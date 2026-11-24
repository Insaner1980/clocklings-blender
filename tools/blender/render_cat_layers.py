"""Build an editable kitten and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_cat_layers.py -- --rebuild --preview
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
SOURCE = ROOT / "art/blender/cat.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-cat/render"
REGIONS = {
    "head": ((30, 48, 210, 170), (0, 0)),
    "body": ((40, 146, 200, 286), (544, 0)),
    "tail": ((170, 148, 235, 278), (1028, 0)),
    "paws": ((72, 170, 170, 286), (0, 424)),
    "eyes": ((70, 88, 172, 132), (300, 424)),
    "ears": ((40, 18, 202, 91), (610, 424)),
    "stripes": ((94, 50, 146, 84), (300, 560)),
}
COLORS = {"original": "EFA04C", "blue": "86BDED", "green": "94C9A1", "purple": "C5A1DD"}


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


def ear(name, outline, depth, mat):
    # Round the silhouette as well as the front edge, like a molded vinyl ear.
    data = bpy.data.curves.new(name, "CURVE")
    data.dimensions = "2D"
    data.fill_mode = "BOTH"
    data.resolution_u = 24
    data.extrude = 4 / 50
    data.bevel_depth = 2.5 / 50
    data.bevel_resolution = 6
    spline = data.splines.new("BEZIER")
    rounded = []
    for i, vertex in enumerate(outline):
        for neighbor in (outline[i - 1], outline[(i + 1) % len(outline)]):
            rounded.append(tuple(vertex[j] * 0.92 + neighbor[j] * 0.08 for j in (0, 1)))
    spline.bezier_points.add(len(rounded) - 1)
    for bp, (x, y) in zip(spline.bezier_points, rounded):
        bp.co = ((x - 120) / 50, (280 - y) / 50, 0)
        bp.handle_left_type = bp.handle_right_type = "AUTO"
    spline.use_cyclic_u = True
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    obj.rotation_euler[0] = math.pi / 2
    obj.location.y = depth / 50
    return finish(obj, name, mat, "ears")


def head_front(x, y, offset=0):
    return -46 * math.sqrt(max(0.02, 1 - ((x - 120) / 76) ** 2 - ((y - 110) / 58) ** 2)) - offset


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
    scene = bpy.data.scenes.new("Clocklings kitten")
    bpy.context.window.scene = scene
    fur = material("Cat fur", COLORS["original"])
    stripe = material("Cat stripes", "A85F30", 0.42)
    cream = material("Warm cream", "FFE5B7", 0.4)
    pink = material("Inner ears", "F18C99", 0.46)
    cheek = material("Peach cheeks", "F6A071", 0.48)
    cocoa = material("Cocoa face", "583024", 0.4)
    eye = material("Deep brown eyes", "281B21", 0.16)
    white = material("Eye white", "FFF5EB", 0.22)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    oval("Seated pear body", 120, 217, 5, 49, 63, 29, fur, "body")
    for x in (67, 173):
        oval(f"Haunch {x}", x, 249, 1, 23, 26, 23, fur, "body")
        oval(f"Back paw {x}", x, 270, -12, 18, 12, 19, fur, "body")
        for dx in (-5, 5):
            line(f"Back toes {x} {dx}", [(x + dx, y, -12 - 19 * math.sqrt(1 - (dx / 18) ** 2 - ((y - 270) / 12) ** 2) - 0.2) for y in (265, 270, 276)], 0.6, stripe, "body", [0.5, 1, 0.5])

    # The chest marking follows the body surface; it is not a floating cream sphere.
    outline = []
    count = 64
    for i in range(count):
        a = 2 * math.pi * i / count
        y = 215 + 42 * math.sin(a)
        width = 24 * (1 - 0.42 * math.sin(a))
        x = 120 + width * math.cos(a)
        outline.append((x, y))
    surface_patch("Cream chest", outline, lambda x, y: 5 - 29 * math.sqrt(max(0.01, 1 - ((x - 120) / 49) ** 2 - ((y - 217) / 63) ** 2)) - 0.3, cream, "body")

    for x, angle in ((97, -7), (143, 7)):
        limb = oval(f"Front leg {x}", x, 223, -19, 14, 47, 14, fur, "paws")
        limb.rotation_euler[1] = math.radians(angle)
        oval(f"Front paw {x}", x, 269, -30, 19, 15, 19, fur, "paws")
        fuse([f"Front leg {x}", f"Front paw {x}"], f"Soft front leg and paw {x}")
        for dx in (-6, 6):
            line(f"Front toes {x} {dx}", [(x + dx, y, -30 - 19 * math.sqrt(1 - (dx / 20) ** 2 - ((y - 272) / 12) ** 2) - 0.2) for y in (267, 272, 278)], 0.65, stripe, "paws", [0.5, 1, 0.5])

    tail = line("Curled tail", [(180, 262, 15), (205, 246, 15), (214, 219, 12), (204, 190, 10), (212, 174, 9), (220, 171, 9)], 10, fur, "tail", [1, 1, 1, 1, 1.12, 0.85])
    bpy.ops.object.select_all(action="DESELECT")
    tail.select_set(True)
    bpy.context.view_layer.objects.active = tail
    bpy.ops.object.convert(target="MESH")
    tail = bpy.context.object
    tail.data.materials.append(cream)
    for face in tail.data.polygons:
        y = sum(280 - tail.data.vertices[v].co.z * 50 for v in face.vertices) / len(face.vertices)
        face.material_index = int(y < 192)
        face.use_smooth = True
    oval("Tail tip round end", 220, 171, 9, 8.6, 8.6, 8.6, cream, "tail")

    ear("Left ear", [(54, 84), (55, 29), (99, 64)], 0, fur)
    ear("Right ear", [(141, 64), (185, 29), (186, 84)], 0, fur)
    ear("Left inner ear", [(62, 72), (62, 40), (87, 64)], -7, pink)
    ear("Right inner ear", [(153, 64), (178, 40), (178, 72)], -7, pink)
    oval("Round kitten head", 120, 110, 0, 76, 58, 46, fur, "head")
    oval("Cream muzzle", 120, 141, -43, 40, 21, 15, cream, "head")
    for x in (66, 174):
        outline = [(x + 10.5 * math.cos(a), 134 + 6 * math.sin(a)) for a in np.linspace(0, math.tau, 64, endpoint=False)]
        surface_patch(f"Cheek {x}", outline, lambda px, py: head_front(px, py, 0.3), cheek, "head")
    for x in (90, 152):
        d = head_front(x, 110, 1)
        toy_eye(oval, material, f"Kitten eye {x}", x, 110, d - 1, 14.8, 17.2, "eyes")
        brow = [(x - 8, 87, head_front(x - 8, 87, 1)), (x, 83, head_front(x, 83, 1)), (x + 8, 85, head_front(x + 8, 85, 1))]
        line(f"Brow {x}", brow, 2.1, stripe, "head", [0.35, 1, 0.35])
    for x in (104, 120, 136):
        outline = [(x + 4.3 * math.cos(a), 67.5 + 12 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 64, endpoint=False)]
        surface_patch(f"Forehead stripe {x}", outline, lambda px, py: head_front(px, py, 0.15), stripe, "stripes")
    nose = oval("Velvet nose", 120, 131, -58, 6, 4, 3.5, cocoa, "head")
    for vertex in nose.data.vertices:
        vertex.co.x *= 1 + 0.4 * vertex.co.z
    line("Smile center", [(120, 133, -58), (120, 141, -58)], 0.9, cocoa, "head")
    for sign in (-1, 1):
        line(f"Smile {sign}", [(120, 141, -58), (120 + sign * 8, 145, -58), (120 + sign * 15, 140, -56.5)], 1, cocoa, "head")
        for index in (0, 1):
            line(f"Whisker {sign} {index}", [(120 + sign * 42, 137 + index * 7, -45), (120 + sign * 62, 133 + index * 13, -44), (120 + sign * 84, 133 + index * 17, -40)], 0.75, cocoa, "head", [1, 0.9, 0.55])

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
        shader = next(n for n in bpy.data.materials["Cat fur"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        stripe = next(n for n in bpy.data.materials["Cat stripes"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        stripe.inputs["Base Color"].default_value = tuple(v * 0.40 for v in rgba(hex_color)[:3]) + (1,)
        atlas = np.zeros((776, 1232, 4), dtype=np.float32)
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
        image = bpy.data.images.new(f"Cat {color}", width=1232, height=776, alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"cat_blender_{color}.png")
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
            scene = bpy.data.scenes["Clocklings kitten"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings kitten"]
        bpy.context.window.scene = scene
        configure(scene)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)
