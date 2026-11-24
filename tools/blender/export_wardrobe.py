"""Render the edited .blend model through its orthographic wardrobe camera.

blender -b art/blender/cat.blend -P tools/blender/export_wardrobe.py
Then run optimize_atlases.py to publish the PNG masters as lossless Android WebP.
The layout, original color and dye weights travel with the editable model.
"""
import json
import sys
import tempfile
from pathlib import Path

import bpy
import numpy as np


def linear(rgb):
    return np.where(rgb <= 0.04045, rgb / 12.92, ((rgb + 0.055) / 1.055) ** 2.4)


def dye(color, tone):
    for material in bpy.data.materials:
        if material.library or 'wardrobe_dye' not in material:
            continue
        nodes = material.node_tree.nodes
        shader = next(n for n in nodes if n.type == 'BSDF_PRINCIPLED')
        original = np.array(material['wardrobe_color'])
        mix = material['wardrobe_dye']
        if color:
            srgb = np.array(list(bytes.fromhex(color))) / 255
            pigment = linear(srgb)
            # Keep dark ear markings, light foot pads and the shine of the original finish.
            strength = material['wardrobe_value']
            rgb = original[:3] * (1-mix) + pigment * strength * mix
        else:
            rgb = original[:3]
        shader.inputs['Base Color'].default_value = (*rgb, 1)
        tint = nodes.get('Wardrobe print color')
        if tint:
            tint.inputs[0].default_value = mix if color else 0
            tint.inputs[2].default_value = (*rgb, 1)
        fur_pigment = nodes.get('Wardrobe fur pigment')
        if fur_pigment:
            fur_pigment.outputs[0].default_value = (*rgb, 1)
        value = nodes.get('Wardrobe pigment value')
        if value:
            value.inputs[2].default_value = (*tone, 1)


def finish_silhouette(pixels, sigma):
    """Antialias a solid painted silhouette without painting across the space between limbs."""
    alpha = pixels[:, :, 3]
    radius = int(np.ceil(sigma * 3))
    weights = np.exp(-.5 * (np.arange(-radius, radius + 1) / sigma)**2)
    weights /= weights.sum()
    # Filter coverage, then recover a crisp subpixel edge from the half-coverage contour.
    coverage = alpha
    for axis in (0, 1):
        pad = [(0, 0), (0, 0)]
        pad[axis] = (radius, radius)
        padded = np.pad(coverage, pad)
        coverage = np.zeros_like(alpha)
        for i, weight in enumerate(weights):
            window = [slice(None), slice(None)]
            window[axis] = slice(i, i + alpha.shape[axis])
            coverage += weight * padded[tuple(window)]
    target = np.clip((coverage - .5) * (sigma * 2.8) + .5, 0, 1)
    # Stretch existing edge paint into newly covered pixels instead of sampling the transparent background.
    rows, cols = np.nonzero(target > alpha + .04)
    opaque = alpha >= .82
    offsets = [(y, x) for y in range(-radius, radius + 1) for x in range(-radius, radius + 1)]
    for dy, dx in sorted(offsets, key=lambda p: p[0]**2 + p[1]**2):
        if rows.size == 0:
            break
        sy = np.clip(rows + dy, 0, alpha.shape[0] - 1)
        sx = np.clip(cols + dx, 0, alpha.shape[1] - 1)
        hit = opaque[sy, sx]
        pixels[rows[hit], cols[hit], :3] = pixels[sy[hit], sx[hit], :3]
        rows, cols = rows[~hit], cols[~hit]
    pixels[:, :, 3] = target
    pixels[target == 0] = 0
    return pixels


def read_layer_pixels(image, width, height):
    values = np.empty(len(image.pixels), dtype=np.float32)
    image.pixels.foreach_get(values)
    return values.reshape(height, width, 4)[::-1]


def render_tile(scene, camera, frame, rectangle, scale, temp):
    x, y, w, h = rectangle
    left, top, right, bottom, cx, cz, units = frame
    pad = 2
    camera.location.x = cx + (x + w/2 - (left+right)/2) * units
    camera.location.z = cz - (y + h/2 - (top+bottom)/2) * units
    camera.data.ortho_scale = (h + 2*pad) * units
    scene.render.resolution_x = (w + 2*pad)*scale
    scene.render.resolution_y = (h + 2*pad)*scale
    scene.render.filepath = str(temp)
    bpy.ops.render.render(write_still=True, scene=scene.name)
    image = bpy.data.images.load(str(temp), check_existing=False)
    try:
        pixels = read_layer_pixels(image, *image.size)
        p = pad*scale
        cropped = pixels[p:-p, p:-p].copy()
        outside = pixels[:, :, 3].copy()
        outside[p:-p, p:-p] = 0
        if cropped[:, :, 3].max() < .1 or outside.max() > .08:
            raise ValueError(f'Empty or clipped sprite at {rectangle}: {outside.max():.3f}')
        return cropped
    finally:
        bpy.data.images.remove(image)


def export(scene, destination, selected=None):
    spec = json.loads(scene['wardrobe_export'])
    if 'wardrobe_rest_frame' in scene:
        scene.frame_set(scene['wardrobe_rest_frame'])
    camera = scene.camera
    objects = [obj for obj in scene.objects if obj.type not in {'LIGHT', 'CAMERA'}]
    # Android draws each piece separately in wardrobe order. The fitting key only
    # offsets overlapping surfaces when the complete toy is displayed in Blender.
    for obj in objects:
        shapes = getattr(obj.data, 'shape_keys', None)
        if shapes and not obj.library:
            fit = shapes.key_blocks.get('Wardrobe assembly fit')
            if fit:
                fit.value = 0
    scene.render.resolution_percentage = 100
    scene.render.use_border = False
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.image_settings.color_depth = '8'
    scene.render.film_transparent = True
    scene.render.use_sequencer = False
    scene.render.use_compositing = False
    destination.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=destination, prefix='wardrobe-') as directory:
        for suffix,color in spec['colors'].items():
            if selected is not None and suffix != selected:
                continue
            dye(color, spec.get('tones', {}).get(suffix, [1, 1, 1]))
            width, height = spec['size']
            atlas = np.zeros((height, width, 4), dtype=np.float32)
            for key, tile in spec['regions'].items():
                for obj in objects:
                    obj.hide_render = obj.get('atlas_layer') != key
                mask_name = tile.get('overlap_mask')
                gates = [m.node_tree.nodes['Overlap strength'].outputs[0] for m in bpy.data.materials
                         if mask_name and not m.library and m.get('wardrobe_overlap') == mask_name]
                strengths = [gate.default_value for gate in gates]
                try:
                    for gate in gates:
                        gate.default_value = 0
                    pixels = render_tile(scene, camera, spec['frame'], tile['rect'], 3, Path(directory)/'tile.png')
                finally:
                    for gate, strength in zip(gates, strengths):
                        gate.default_value = strength
                if any(obj.type == 'MESH' and obj.data.uv_layers.get('Studio paint') is not None
                       for obj in objects if obj.get('atlas_layer') == key):
                    fine_detail = 'accessories' in spec['name'] or 'eye' in key or key in ('stripes', 'horns')
                    pixels = finish_silhouette(pixels, 1.05 if fine_detail else 2.8)
                if mask_name:
                    # Use the packed camera-space paint mask directly to avoid stochastic transparency noise.
                    mask = bpy.data.images[mask_name]
                    assert tuple(mask.size) == (pixels.shape[1], pixels.shape[0])
                    coverage = np.empty(len(mask.pixels), np.float32)
                    mask.pixels.foreach_get(coverage)
                    pixels[:, :, 3] *= 1 - coverage.reshape(pixels.shape)[::-1, :, 0]
                    pixels[pixels[:, :, 3] == 0] = 0
                x, y = tile['source']
                h, w = pixels.shape[:2]
                atlas[y:y+h, x:x+w] = pixels
            name = spec['name'] + ('_'+suffix if suffix else '')
            image = bpy.data.images.new(name, width=width, height=height, alpha=True)
            image.colorspace_settings.name = 'sRGB'
            image.pixels.foreach_set(np.ascontiguousarray(atlas[::-1]).ravel())
            image.filepath_raw = str(destination / (name+'.png'))
            image.file_format = 'PNG'
            image.save()
            bpy.data.images.remove(image)
            print('WARDROBE_EXPORTED', name, flush=True)


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[2]
    args = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
    export(bpy.context.scene, Path(args[0]).resolve() if args else root/'art/blender/atlases',
           args[1] if len(args)>1 else None)

