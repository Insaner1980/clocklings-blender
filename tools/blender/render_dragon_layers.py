"""Build an editable dragon and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_dragon_layers.py -- --rebuild --preview
Omit --preview for the four app atlases. Omit --rebuild to render the edited .blend.
"""

from pathlib import Path
import math
import sys

import bpy
import bmesh
import numpy as np
from mathutils import Vector
sys.path.insert(0, str(Path(__file__).resolve().parent))
from reference_finish import toy_eye, studio


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/dragon.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-dragon/render"
REGIONS = {
    "head": ((20, 42, 220, 164), (0, 0)),
    "body": ((56, 132, 240, 282), (604, 0)),
    "wing_left": ((0, 134, 100, 216), (1160, 0)),
    "wing_right": ((140, 134, 240, 216), (1160, 250)),
    "limbs": ((56, 158, 186, 286), (0, 456)),
    "horns": ((70, 12, 170, 65), (394, 456)),
    "eyes": ((68, 79, 173, 124), (698, 456)),
}
COLORS = {"original": "A77BDD", "blue": "86BDED", "green": "94C9A1", "orange": "F4B26A"}

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


def mesh_form(name, vertices, faces, mat, group):
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    return finish(obj, name, mat, group)


def cushion(name, outline, depth, bulge, mat, group):
    # Smooth closed membranes with real volume, not a flat image plane.
    from mathutils.geometry import interpolate_bezier
    curve = bpy.data.curves.new(name + " outline", "CURVE")
    curve.dimensions = "3D"
    spline = curve.splines.new("BEZIER")
    spline.bezier_points.add(len(outline) - 1)
    for bp, (x, y) in zip(spline.bezier_points, outline):
        bp.co = (x, y, 0)
        bp.handle_left_type = bp.handle_right_type = "AUTO"
    spline.use_cyclic_u = True
    points = []
    for i, bp in enumerate(spline.bezier_points):
        following = spline.bezier_points[(i + 1) % len(outline)]
        points.extend(interpolate_bezier(bp.co, bp.handle_right, following.handle_left, following.co, 9)[:-1])
    bpy.data.curves.remove(curve)
    cx = sum(p.x for p in points) / len(points)
    cy = sum(p.y for p in points) / len(points)
    count, rings = len(points), 20
    vertices, faces = [], []
    for side in (-1, 1):
        start = len(vertices)
        vertices.append(point(cx, cy, depth + side * bulge))
        for ring in range(1, rings + 1):
            t = ring / rings
            for p in points:
                vertices.append(point(cx + (p.x - cx) * t, cy + (p.y - cy) * t, depth + side * bulge * math.sqrt(max(0, 1 - t * t))))
        faces.extend((start, start + i + 1, start + (i + 1) % count + 1) for i in range(count))
        for ring in range(rings - 1):
            for i in range(count):
                u, v = start + 1 + ring * count + i, start + 1 + ring * count + (i + 1) % count
                faces.append((u, u + count, v + count, v))
    obj = mesh_form(name, vertices, faces, mat, group)
    # Join the coincident perimeter of the front and back surfaces.
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=0.00001)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(obj.data)
    bm.free()
    return obj


def horn(name, right, mat):
    vertices, faces = [], []
    rings, count = 64, 64
    for ring in range(rings + 1):
        t = ring / rings
        radius = max(0.12, 15 * (1 - t) ** 0.85)
        for groove in (0.36, 0.68):
            radius *= 1 - 0.10 * math.exp(-((t - groove) / 0.025) ** 2)
        cx, y = 92 - 13 * t, 53 - 34 * t
        for angle in np.linspace(0, 2 * math.pi, count, endpoint=False):
            x = cx + radius * math.cos(angle)
            vertices.append(point(240 - x if right else x, y, 7 - 7 * t + radius * math.sin(angle)))
    for ring in range(rings):
        for i in range(count):
            u, v = ring * count + i, ring * count + (i + 1) % count
            faces.append((u, u + count, v + count, v))
    faces.extend([tuple(range(count - 1, -1, -1)), tuple(rings * count + i for i in range(count))])
    return mesh_form(name, vertices, faces, mat, "horns")


def head_front(x, y, offset=0):
    return -44 * math.sqrt(max(0.02, 1 - ((x - 120) / 66.5) ** 2 - ((y - 102.3) / 56.7) ** 2)) - offset


def build():
    # Run in a new scene, preserving any scene already open in the MCP session.
    scene = bpy.data.scenes.new("Clocklings dragon")
    bpy.context.window.scene = scene
    fur = material("Dragon skin", COLORS["original"])
    wing = material("Dragon wings", "8C67BA", 0.42)
    crease = material("Dragon creases", "62408E", 0.44)
    cream = material("Warm cream", "FBE7BE", 0.4)
    plate = material("Plate grooves", "CDA773", 0.46)
    gold = material("Golden tail tip", "F2B83C", 0.36)
    cheek = material("Pink cheeks", "F3A1AB", 0.45)
    eye = material("Deep violet eyes", "241A30", 0.16)
    iris = material("Violet iris", "7044A7", 0.23)
    white = material("Eye white", "FFF5EB", 0.22)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    for right in (False, True):
        side = "Right" if right else "Left"
        x = lambda value: 240 - value if right else value
        group = "wing_right" if right else "wing_left"
        outline = [(90, 178), (65, 148), (44, 142), (22, 148), (5, 164), (19, 174), (27, 184), (30, 198), (47, 194), (62, 204), (72, 184)]
        cushion(side + " membrane wing", [(x(px), py) for px, py in outline], 16, 5, wing, group)
        line(side + " leading wing edge", [(x(px), py, 11) for px, py in [(78, 163), (65, 149), (44, 144), (23, 149), (6, 164)]], 2.4, fur, group, [1, 1, 1, 0.8, 0.3])
        for target in ((30, 198), (62, 204)):
            line(side + f" wing rib {target}", [(x(71), 158, 10), (x((71 + target[0]) / 2), (158 + target[1]) / 2, 10), (x(target[0]), target[1] - 1, 14)], 1.35, crease, group, [1, 1, 0.4])
        frill = [(73, 64), (42, 50), (43, 63), (26, 75), (38, 86), (37, 99), (56, 101), (65, 85)]
        cushion(side + " ear frill", [(x(px), py) for px, py in frill], 4, 6, wing, "head")
        for target in ((41, 53), (29, 75), (39, 98)):
            line(side + f" frill ridge {target}", [(x(66), 78, -3), (x((66 + target[0]) / 2), (78 + target[1]) / 2, -2), (x(target[0]), target[1], 3)], 1.4, fur, "head", [1, 0.85, 0.2])
        horn(side + " cream horn", right, cream)

    line("Curving tail", [(166, 258, 15), (192, 252, 15), (209, 238, 12), (218, 215, 9)], 13, fur, "body", [1.25, 1, 0.7, 0.4])
    cushion("Golden leaf tail tip", [(215, 218), (208, 207), (214, 195), (230, 187), (234, 184), (236, 190), (234, 205), (226, 215), (220, 220)], 7, 7, gold, "body")
    line("Tail leaf vein", [(216, 217, -1), (224, 203, -1), (233, 186, 5)], 0.8, plate, "body", [0.5, 1, 0.3])
    oval("Pear dragon body", 120, 215, 5, 49, 63, 32, fur, "body")
    oval("Rounded neck", 120, 163, 10, 31, 29, 25, fur, "body")
    for x in (82, 158):
        oval(f"Rounded haunch {x}", x, 240, 5, 23, 33, 25, fur, "body")
    oval("Cream plated belly", 120, 213, -20, 33, 51, 17, cream, "body")
    for y0 in (177, 201, 224, 244):
        width = 33 * math.sqrt(max(0.01, 1 - ((y0 - 213) / 51) ** 2)) * 0.97
        points = []
        for dx in np.linspace(-width, width, 9):
            y = y0 + 4 * (1 - (dx / width) ** 2)
            d = -20 - 17 * math.sqrt(max(0.01, 1 - (dx / 33) ** 2 - ((y - 213) / 51) ** 2)) - 0.2
            points.append((120 + dx, y, d))
        line(f"Belly plate {y0}", points, 0.8, plate, "body")

    for right in (False, True):
        x = lambda value: 240 - value if right else value
        side = "Right" if right else "Left"
        hand = oval(side + " soft forearm and paw", x(90), 183, -30, 12.5, 23, 14, fur, "limbs")
        hand.rotation_euler[1] = math.radians(28 if right else -28)
        for dx in (-3, 3):
            line(side + f" finger crease {dx}", [(x(93 + dx), 192, -42), (x(96 + dx), 197, -39.5)], 0.4, crease, "limbs", [0.6, 0.2])
        oval(side + " foot", x(87), 270, -15, 25.5, 13, 23, fur, "limbs")
        for dx in (-11, 0, 11):
            oval(side + f" cream claw {dx}", x(87 + dx), 277, -34, 4.2, 5.2, 5, cream, "limbs")

    oval("Round dragon head", 120, 102.3, 0, 66.5, 56.7, 44, fur, "head")
    oval("Rounded snout", 120, 122, -41, 24, 15, 10, fur, "head")
    for x in (74.7, 166.1):
        oval(f"Pink cheek {x}", x, 121.6, head_front(x, 121.6, 1), 10.5, 7, 2.5, cheek, "head")
    for x in (89.4, 151.9):
        d = head_front(x, 100.4, 1)
        toy_eye(oval, material, f"Dragon eye {x}", x, 100.4, d - 1, 13.8, 16.2, "eyes", "8D58AE")
        points = [(x - 9, 77, head_front(x - 9, 77, 1)), (x, 71, head_front(x, 71, 1)), (x + 9, 75, head_front(x + 9, 75, 1))]
        line(f"Brow {x}", points, 2.1, crease, "head", [0.4, 1, 0.4])
    for x in (112, 129.5):
        oval(f"Nostril {x}", x, 118.6, -50.7, 2.6, 2.2, 0.7, crease, "head")
    line("Friendly smile", [(102, 129, -48), (120.8, 138, -42), (139.5, 129, -47)], 1.2, eye, "head", [0.7, 1, 0.7])

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


def configure(scene):
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 40
    scene.cycles.use_denoising = True
    scene.render.resolution_x = 720
    scene.render.resolution_y = 900
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 8


def render_layers(scene):
    geometry = [o for o in scene.objects if o.get("layer")]
    for color, hex_color in COLORS.items():
        shader = next(n for n in bpy.data.materials["Dragon skin"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        for name, factor in (("Dragon wings", 0.84), ("Dragon creases", 0.60)):
            shader = next(n for n in bpy.data.materials[name].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
            shader.inputs["Base Color"].default_value = tuple(linear(int(hex_color[i:i + 2], 16) / 255 * factor) for i in (0, 2, 4)) + (1,)
        atlas = np.zeros((844, 1464, 4), dtype=np.float32)
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
            pixels = np.asarray(rendered.pixels[:], dtype=np.float32).reshape(900, 720, 4)[::-1].copy()
            bpy.data.images.remove(rendered)
            x0, y0, x1, y1 = [v * 3 for v in rect]
            outside = pixels[:, :, 3].copy()
            outside[y0:y1, x0:x1] = 0
            assert outside.max() < 0.01, f"Clipped {color}/{group}"
            cropped = pixels[y0:y1, x0:x1]
            assert cropped[:, :, 3].max() > 0.9, f"Empty {color}/{group}"
            ax, ay = origin
            atlas[ay:ay + y1 - y0, ax:ax + x1 - x0] = cropped
        image = bpy.data.images.new(f"Dragon {color}", width=1464, height=844, alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"dragon_blender_{color}.png")
        image.file_format = "PNG"
        image.save()
        bpy.data.images.remove(image)
        print(f"ATLAS_COMPLETE {color}", flush=True)


if __name__ == "__main__":
    if "--rebuild" in sys.argv:
        scene = build()
        if bpy.app.background:
            # Turn the scene-only library into a normal editable startup file.
            bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
            scene = bpy.data.scenes["Clocklings dragon"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.context.scene
        if "--preview" in sys.argv or "wardrobe_export" not in scene:
            configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)", flush=True)
    if "--preview" in sys.argv:
        SCRATCH.mkdir(parents=True, exist_ok=True)
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    elif "wardrobe_export" in scene:
        from export_wardrobe import export
        export(scene, OUTPUT)
    else:
        SCRATCH.mkdir(parents=True, exist_ok=True)
        render_layers(scene)
