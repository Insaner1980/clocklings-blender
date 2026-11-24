"""Build an editable dinosaur and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_dinosaur_layers.py -- --rebuild --preview
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
from export_wardrobe import read_layer_pixels
from reference_finish import configure, toy_eye, studio


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/dinosaur.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-dinosaur/render"
REGIONS = {
    "head": ((60, 30, 228, 156), (0, 0)),
    "body": ((38, 10, 194, 286), (508, 0)),
    "tail": ((4, 208, 104, 280), (980, 0)),
    "near_arm": ((94, 153, 149, 211), (0, 384)),
    "far_arm": ((158, 161, 192, 207), (169, 384)),
    "fist": ((180, 181, 213, 214), (275, 384)),
    "eye_near": ((103, 75, 138, 113), (0, 562)),
    "eye_far": ((151, 62, 178, 97), (109, 562)),
}
COLORS = {"original": "7DBE74", "blue": "86BDED", "purple": "C5A1DD", "orange": "F4B26A"}

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


def joined_form(name, forms, mat, group):
    parts = [oval(name, *form, mat, group) for form in forms]
    bpy.ops.object.select_all(action="DESELECT")
    for obj in parts:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = parts[0]
    bpy.ops.object.join()
    obj = bpy.context.object
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.remesh_voxel_size = 0.025
    bpy.ops.object.voxel_remesh()
    smooth = obj.modifiers.new("Soft joined contours", "SMOOTH")
    smooth.factor, smooth.iterations = 1, 45
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    for face in obj.data.polygons:
        face.use_smooth = True
    return obj


def front_depth(obj, x, y, offset=0.4):
    inverse = obj.matrix_world.inverted()
    origin = inverse @ Vector(point(x, y, -500))
    direction = inverse.to_3x3() @ Vector((0, 1, 0))
    hit, location, _, _ = obj.ray_cast(origin, direction)
    assert hit, f"Surface miss at {obj.name}: {x}, {y}"
    return (obj.matrix_world @ location).y * 50 - offset


def surface_mark(name, obj, x, y, rx, ry, mat, group):
    vertices = [point(x, y, front_depth(obj, x, y))]
    faces, count, rings = [], 64, 10
    for ring in range(1, rings + 1):
        for i in range(count):
            angle = i * math.tau / count
            px, py = x + rx * ring / rings * math.cos(angle), y + ry * ring / rings * math.sin(angle)
            vertices.append(point(px, py, front_depth(obj, px, py)))
    faces.extend((0, 1 + i, 1 + (i + 1) % count) for i in range(count))
    for ring in range(rings - 1):
        for i in range(count):
            u, v = 1 + ring * count + i, 1 + ring * count + (i + 1) % count
            faces.append((u, u + count, v + count, v))
    return mesh_form(name, vertices, faces, mat, group)


def rounded_head(mat):
    # One continuous surface avoids dents where overlapping ellipsoids would meet.
    rings, segments = 96, 128
    vertices = [point(128, 38, 0)]
    faces = []
    for ring in range(1, rings):
        theta = math.pi * ring / rings
        y = 94 - 56 * math.cos(theta)
        radius = math.sin(theta)
        extension = 36 * math.exp(-((y - 110) / 32) ** 4)
        for segment in range(segments):
            angle = math.tau * segment / segments
            right = (1 + math.cos(angle)) / 2
            x = 128 + 17 * ((y - 38) / 112) ** 2 + radius * (53 * math.cos(angle) + extension * right)
            depth = radius * (43 * math.sin(angle) - 14 * math.exp(-((y - 110) / 30) ** 2) * right * right)
            vertices.append(point(x, y, depth))
    bottom = len(vertices)
    vertices.append(point(145, 150, 0))
    faces.extend((0, 1 + i, 1 + (i + 1) % segments) for i in range(segments))
    for ring in range(rings - 2):
        for i in range(segments):
            u = 1 + ring * segments + i
            v = 1 + ring * segments + (i + 1) % segments
            faces.append((u, u + segments, v + segments, v))
    last = 1 + (rings - 2) * segments
    faces.extend((last + i, bottom, last + (i + 1) % segments) for i in range(segments))
    return mesh_form("Continuous rounded skull and muzzle", vertices, faces, mat, "head")


def build():
    # Preserve every scene already open in the live Blender session.
    scene = bpy.data.scenes.new("Clocklings dinosaur")
    bpy.context.window.scene = scene
    skin = material("Dinosaur skin", COLORS["original"])
    marking = material("Dinosaur markings", "588551", 0.42)
    cream = material("Warm cream", "FBE7BE", 0.4)
    cheek = material("Peach cheek", "F4B18A", 0.45)
    dark = material("Dark eyes and smile", "211B19", 0.2)
    iris = material("Warm brown iris", "604835", 0.24)
    white = material("Eye white", "FFF5EB", 0.22)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    tail = joined_form("Curved tapered tail", [
        (76, 250, 13, 25, 20, 18),
        (54, 253, 13, 26, 15, 14),
        (29, 248, 13, 19, 8, 8),
    ], skin, "tail")
    for index, outline in enumerate([
        [(69, 240), (53, 217), (47, 223), (47, 246)],
        [(39, 244), (27, 232), (23, 236), (23, 248)],
    ]):
        cushion(f"Tail plate {index}", outline, 11, 5, marking, "tail")
    for x, y, rx, ry in [(64.8, 240.7, 6.5, 5.5), (48.2, 246.8, 5, 4.2)]:
        surface_mark(f"Tail spot {x}", tail, x, y, rx, ry, marking, "tail")

    for index, triangle in enumerate([
        [(114, 48), (101, 18), (92, 45)],
        [(99, 56), (64, 43), (83, 86)],
        [(91, 82), (48, 96), (91, 130)],
        [(103, 130), (58, 141), (96, 185)],
        [(98, 177), (57, 189), (85, 226)],
    ]):
        outline = []
        for i, vertex in enumerate(triangle):
            for neighbor in (triangle[i - 1], triangle[(i + 1) % 3]):
                outline.append(tuple(vertex[j] * 0.9 + neighbor[j] * 0.1 for j in (0, 1)))
        cushion(f"Rounded back plate {index}", outline, 14, 5, marking, "body")
    body = joined_form("Body and broad neck", [
        (130, 215, 5, 54, 64, 33),
        (130, 154, 9, 30, 32, 27),
        (97, 238, 7, 24, 32, 24),
        (157, 238, 7, 24, 32, 24),
    ], skin, "body")
    oval("Cream belly", 147.5, 213, -22, 28.5, 51, 18, cream, "body")
    for x, y, rx, ry in [(88.6, 188.4, 5, 7), (87.2, 214.2, 6, 10)]:
        surface_mark(f"Body spot {y}", body, x, y, rx, ry, marking, "body")

    for name, x, y, depth, rx, ry in [
        ("Near foot", 101.6, 268, -18, 26.7, 14.5),
        ("Far foot", 159.6, 265.6, -3, 27.6, 13.2),
    ]:
        oval(name, x, y, depth, rx, ry, 20, skin, "body")
    for x, y, rx, ry, depth in [
        (96.2, 277, 6, 5.6, -35), (110.3, 276.6, 6, 5.6, -35), (122, 276, 5.4, 5.2, -32),
        (167.5, 274.4, 5.5, 5.2, -21), (178.8, 273, 5, 4.8, -19), (186, 272.1, 3, 4, -12),
    ]:
        oval(f"Cream toe {x}", x, y, depth, rx, ry, 4, cream, "body")

    near_arm = oval("Soft near forearm and paw", 121, 183, -31, 13, 23, 13, skin, "near_arm")
    near_arm.rotation_euler.y = math.radians(-43)
    far_arm = oval("Soft far forearm and paw", 174, 183, -22, 11, 20, 12, skin, "far_arm")
    far_arm.rotation_euler.y = math.radians(17)
    fist = joined_form("Ice cream holding fist", [
        (197.4, 193.8, -35, 10.4, 8, 9),
        (192.8, 202, -35, 9.4, 7.4, 9),
    ], skin, "fist")

    head = rounded_head(skin)
    surface_mark("Peach cheek", head, 115.5, 116.6, 11.5, 7.5, cheek, "head")
    for name, x, y, rx, ry, group in [
        ("Near", 120, 93, 12.5, 13.8, "eye_near"),
        ("Far", 164.6, 78.5, 8, 12, "eye_far"),
    ]:
        d = front_depth(head, x, y)
        toy_eye(oval, material, name + " dinosaur eye", x, y, d - 1, rx + 0.6, ry + 0.5, group, "795539")
    for name, points in [
        ("Near brow", [(101.5, 76), (107, 68), (116, 65.5)]),
        ("Far brow", [(151.5, 57), (157, 55), (163, 59)]),
    ]:
        line(name, [(x, y, front_depth(head, x, y, 0.9)) for x, y in points], 1.7, marking, "head", [0.35, 1, 0.35])
    for x, y, rx, ry in [(192.2, 95.3, 3.5, 4), (207.5, 93, 2.2, 3.6)]:
        surface_mark(f"Nostril {x}", head, x, y, rx, ry, marking, "head")
    points = []
    for t in np.linspace(0, 1, 13):
        x = (1-t)**2*144.5 + 2*(1-t)*t*168.5 + t*t*202.5
        y = (1-t)**2*119.5 + 2*(1-t)*t*139.5 + t*t*114.5
        points.append((x, y, front_depth(head, x, y, 0.8)))
    line("Friendly smile", points, 1.2, dark, "head")
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
    fist.hide_render = True
    # Save only this character's scene, leaving the live session's other scenes untouched.
    bpy.data.libraries.write(str(SOURCE), {scene}, fake_user=True, compress=True)
    return scene


def render_layers(scene):
    geometry = [o for o in scene.objects if o.get("layer")]
    for color, hex_color in COLORS.items():
        shader = next(n for n in bpy.data.materials["Dinosaur skin"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        for name, factor in (("Dinosaur markings", 0.7),):
            shader = next(n for n in bpy.data.materials[name].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
            shader.inputs["Base Color"].default_value = tuple(linear(int(hex_color[i:i + 2], 16) / 255 * factor) for i in (0, 2, 4)) + (1,)
        atlas = np.zeros((832, 1284, 4), dtype=np.float32)
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
        image = bpy.data.images.new(f"Dinosaur {color}", width=1284, height=832, alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"dinosaur_blender_{color}.png")
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
            scene = bpy.data.scenes["Clocklings dinosaur"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings dinosaur"]
        bpy.context.window.scene = scene
        configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)", flush=True)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)


