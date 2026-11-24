"""Build an editable puppy and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_dog_layers.py -- --rebuild --preview
Omit --preview for the four app atlases. Omit --rebuild to render the edited .blend.
"""

from pathlib import Path
import math
import sys

import bpy
import numpy as np
from mathutils import Vector
sys.path.insert(0, str(Path(__file__).resolve().parent))
from export_wardrobe import read_layer_pixels
from reference_finish import configure, fuse, toy_eye, studio


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/dog.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-dog/render"
REGIONS = {
    "head": ((48, 28, 192, 175), (0, 0)),
    "body": ((38, 148, 202, 286), (436, 0)),
    "tail": ((169, 186, 228, 276), (932, 0)),
    "ear_left": ((18, 26, 95, 169), (0, 448)),
    "ear_right": ((145, 26, 222, 169), (236, 448)),
    "eyes": ((68, 83, 172, 132), (472, 448)),
}
COLORS = {"original": "D99557", "blue": "86BDED", "green": "94C9A1", "purple": "C5A1DD"}

def linear(v):
    return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4


def rgba(hex_color):
    return (*[linear(int(hex_color[i:i + 2], 16) / 255) for i in (0, 2, 4)], 1)


def material(name, color, roughness=0.38):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    shader = next(n for n in mat.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Base Color"].default_value = rgba(color)
    shader.inputs["Roughness"].default_value = roughness
    return mat


def point(x, y, depth):
    return ((x - 120) / 50, depth / 50, (280 - y) / 50)


def finish(obj, name, mat, group):
    obj.name = name
    obj.data.materials.append(mat)
    obj["layer"] = group
    if obj.type == "MESH":
        for face in obj.data.polygons:
            face.use_smooth = True
    return obj


def oval(name, x, y, depth, rx, ry, rz, mat, group):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=64, ring_count=40, location=point(x, y, depth))
    obj = bpy.context.object
    obj.scale = (rx / 50, rz / 50, ry / 50)
    return finish(obj, name, mat, group)


def line(name, points, radius, mat, group, radii=None):
    data = bpy.data.curves.new(name, "CURVE")
    data.dimensions = "3D"
    data.resolution_u = 24
    data.bevel_depth = radius / 50
    data.bevel_resolution = 5
    data.use_fill_caps = True
    spline = data.splines.new("BEZIER")
    spline.bezier_points.add(len(points) - 1)
    for index, (bp, xyz) in enumerate(zip(spline.bezier_points, points)):
        bp.co = point(*xyz)
        bp.handle_left_type = bp.handle_right_type = "AUTO"
        if radii:
            bp.radius = radii[index]
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    return finish(obj, name, mat, group)


def floppy_ear(name, right, mat, group):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=64, ring_count=48)
    obj = bpy.context.object
    for vertex in obj.data.vertices:
        x, depth, z = vertex.co
        center = 43 + 17 * ((z + 1) / 2)
        px = center + x * (24 - 3 * z)
        vertex.co = point(240 - px if right else px, 98 - 54 * z, -14 + depth * 18)
    return finish(obj, name, mat, group)


def head_front(x, y, offset=0):
    return -46 * math.sqrt(max(0.02, 1 - ((x - 120) / 69) ** 2 - ((y - 106) / 63) ** 2)) - offset


def surface_patch(name, outline, project, mat, group):
    # Concentric rings keep the marking on the curved surface between its edges.
    cx = sum(p[0] for p in outline) / len(outline)
    cy = sum(p[1] for p in outline) / len(outline)
    vertices = [point(cx, cy, project(cx, cy))]
    rings = 12
    count = len(outline)
    for ring in range(1, rings + 1):
        for x, y in outline:
            px, py = cx + (x - cx) * ring / rings, cy + (y - cy) * ring / rings
            vertices.append(point(px, py, project(px, py)))
    faces = [(0, i + 1, (i + 1) % count + 1) for i in range(count)]
    for ring in range(rings - 1):
        for i in range(count):
            a, b = 1 + ring * count + i, 1 + ring * count + (i + 1) % count
            faces.append((a, a + count, b + count, b))
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, name, mat, group)


def build():
    # Run in a new scene, preserving any scene already open in the MCP session.
    scene = bpy.data.scenes.new("Clocklings puppy")
    bpy.context.window.scene = scene
    fur = material("Dog fur", COLORS["original"])
    marking = material("Dog markings", "7A4428", 0.42)
    cream = material("Warm cream", "FFE5BD", 0.4)
    pink = material("Pink tongue", "EF788D", 0.4)
    cheek = material("Peach cheeks", "EA936F", 0.48)
    cocoa = material("Cocoa face", "583024", 0.4)
    eye = material("Deep brown eyes", "281B21", 0.16)
    white = material("Eye white", "FFF5EB", 0.22)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    oval("Seated pear body", 120, 217, 5, 49, 63, 29, fur, "body")
    for x in (61, 179):
        oval(f"Haunch {x}", x, 248, 1, 21, 27, 23, fur, "body")
        oval(f"Back paw {x}", x, 268, -12, 16, 12, 19, fur, "body")
        for dx in (-5, 5):
            line(f"Back toes {x} {dx}", [(x + dx, y, -12 - 19 * math.sqrt(1 - (dx / 16) ** 2 - ((y - 268) / 12) ** 2) - 0.2) for y in (264, 269, 274)], 0.6, marking, "body", [0.5, 1, 0.5])

    outline = []
    for a in np.linspace(0, 2 * math.pi, 64, endpoint=False):
        y = 211 + 41 * math.sin(a)
        x = 120 + 25 * (1 - 0.32 * math.sin(a)) * math.cos(a)
        outline.append((x, y))
    surface_patch("Cream chest", outline, lambda x, y: 5 - 29 * math.sqrt(max(0.01, 1 - ((x - 120) / 49) ** 2 - ((y - 217) / 63) ** 2)) - 0.3, cream, "body")

    for x, angle in ((94, -5), (146, 5)):
        limb = oval(f"Front leg {x}", x, 223, -19, 15, 47, 14, fur, "body")
        limb.rotation_euler[1] = math.radians(angle)
        oval(f"Front paw {x}", x, 269, -30, 20.5, 15, 19, fur, "body")
        fuse([f"Front leg {x}", f"Front paw {x}"], f"Soft puppy leg and paw {x}")
        for dx in (-6, 6):
            line(f"Front toes {x} {dx}", [(x + dx, y, -30 - 19 * math.sqrt(1 - (dx / 21.5) ** 2 - ((y - 272) / 12) ** 2) - 0.2) for y in (267, 272, 278)], 0.65, marking, "body", [0.5, 1, 0.5])

    line("Upturned tail", [(180, 259, 15), (207, 242, 15), (215, 218, 12), (206, 201, 10)], 10, fur, "tail", [1, 1, 0.95, 0.8])
    oval("Rounded tail tip", 206, 201, 10, 8, 8, 8, fur, "tail")
    floppy_ear("Left floppy ear", False, marking, "ear_left")
    floppy_ear("Right floppy ear", True, marking, "ear_right")

    oval("Round puppy head", 120, 106, 0, 69, 63, 46, fur, "head")
    patch = [(152 + 26 * math.cos(a), 94 + 37 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 80, endpoint=False)]
    surface_patch("Right eye patch", patch, lambda x, y: head_front(x, y, 0.25), marking, "head")
    oval("Cream muzzle", 120, 141, -40, 39, 23, 19, cream, "head")
    for x in (72, 168):
        outline = [(x + 10 * math.cos(a), 132 + 6.5 * math.sin(a)) for a in np.linspace(0, math.tau, 64, endpoint=False)]
        surface_patch(f"Cheek {x}", outline, lambda px, py: head_front(px, py, 0.3), cheek, "head")
    for x in (88, 152):
        d = head_front(x, 108, 1)
        toy_eye(oval, material, f"Puppy eye {x}", x, 108, d - 1, 14.8, 17.2, "eyes", "805134")
        brow = [(x - 8, 80, head_front(x - 8, 80, 1)), (x, 77, head_front(x, 77, 1)), (x + 8, 79, head_front(x + 8, 79, 1))]
        line(f"Brow {x}", brow, 2.1, marking, "head", [0.35, 1, 0.35])

    oval("Tongue", 124, 151.5, -58, 7.5, 8, 3, pink, "head")
    line("Tongue crease", [(124, 150, -61), (124, 156, -61)], 0.55, cocoa, "head", [0.7, 0.3])
    oval("Soft triangular nose", 120, 128, -59, 12, 7, 6, cocoa, "head")
    for vertex in bpy.context.object.data.vertices:
        vertex.co.x *= 1 + 0.35 * vertex.co.z
    line("Smile center", [(120, 132, -60), (120, 141, -59)], 1, cocoa, "head")
    for sign in (-1, 1):
        line(f"Smile {sign}", [(120, 141, -59), (120 + sign * 9, 146, -59), (120 + sign * 18, 140, -57)], 1.2, cocoa, "head", [0.9, 1, 0.5])

    camera_data = bpy.data.cameras.new("Front orthographic camera")
    camera = bpy.data.objects.new(camera_data.name, camera_data)
    scene.collection.objects.link(camera)
    camera.location = (0, -18, 2.6)
    camera.rotation_euler = (math.pi / 2, 0, 0)
    camera_data.type = "ORTHO"
    camera_data.sensor_fit = "VERTICAL"
    camera_data.ortho_scale = 6
    scene.camera = camera
    for name, loc, power, size in [("Large warm key", (-4, -6, 8), 650, 5), ("Soft fill", (4, -4, 4), 350, 5), ("Top rim", (1, 3, 7), 500, 4)]:
        data = bpy.data.lights.new(name, "AREA")
        data.energy = power
        data.shape = "DISK"
        data.size = size
        obj = bpy.data.objects.new(name, data)
        scene.collection.objects.link(obj)
        obj.location = loc
        obj.rotation_euler = (Vector((0, 0, 2.8)) - obj.location).to_track_quat("-Z", "Y").to_euler()
    scene.world = bpy.data.worlds.new("Soft studio")
    scene.world.use_nodes = True
    background = next(n for n in scene.world.node_tree.nodes if n.type == "BACKGROUND")
    background.inputs["Color"].default_value = (0.75, 0.8, 1, 1)
    background.inputs["Strength"].default_value = 0.28
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.exposure = -0.5
    configure(scene)
    studio(scene)
    # Save only this character's scene, leaving the live session's other scenes untouched.
    bpy.data.libraries.write(str(SOURCE), {scene}, fake_user=True, compress=True)
    return scene


def render_layers(scene):
    geometry = [o for o in scene.objects if o.get("layer")]
    for color, hex_color in COLORS.items():
        shader = next(n for n in bpy.data.materials["Dog fur"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        marking = next(n for n in bpy.data.materials["Dog markings"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        marking.inputs["Base Color"].default_value = rgba("7A4428") if color == "original" else tuple(linear(int(hex_color[i:i + 2], 16) / 255 * 0.66) for i in (0, 2, 4)) + (1,)
        atlas = np.zeros((880, 1112, 4), dtype=np.float32)
        for group, (rect, origin) in REGIONS.items():
            for obj in geometry:
                # Render complete parts: occlusion from another layer would leave dark
                # silhouettes when Compose moves the part or redraws ears over a hat.
                obj.visible_camera = True
                obj.hide_render = obj["layer"] != group
            path = SCRATCH / f"{color}_{group}.png"
            scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            rendered = bpy.data.images.load(str(path), check_existing=False)
            pixels = read_layer_pixels(rendered, 720, 900)
            bpy.data.images.remove(rendered)
            x0, y0, x1, y1 = [v * 3 for v in rect]
            outside = pixels[:, :, 3].copy()
            outside[y0:y1, x0:x1] = 0
            assert outside.max() < 0.01, f"Clipped {color}/{group}"
            cropped = pixels[y0:y1, x0:x1]
            assert cropped[:, :, 3].max() > 0.9, f"Empty {color}/{group}"
            ax, ay = origin
            atlas[ay:ay + y1 - y0, ax:ax + x1 - x0] = cropped
        image = bpy.data.images.new(f"Dog {color}", width=1112, height=880, alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"dog_blender_{color}.png")
        image.file_format = "PNG"
        image.save()
        bpy.data.images.remove(image)
        print(f"ATLAS_COMPLETE {color}", flush=True)


if __name__ == "__main__":
    SCRATCH.mkdir(parents=True, exist_ok=True)
    if "--rebuild" in sys.argv:
        scene = build()
        if bpy.app.background:
            # Turn the scene-only library into a normal editable startup file.
            bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
            scene = bpy.data.scenes["Clocklings puppy"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings puppy"]
        bpy.context.window.scene = scene
        configure(scene)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)


