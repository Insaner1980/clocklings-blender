"""Build an editable bunny and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_bunny_layers.py -- --rebuild --preview
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
SOURCE = ROOT / "art/blender/bunny.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-bunny/render"
REGIONS = {
    "head": ((48, 28, 194, 178), (0, 0)),
    "body": ((46, 154, 207, 287), (442, 0)),
    "ear_left": ((0, 42, 112, 232), (0, 454)),
    "ear_right": ((133, 42, 240, 214), (340, 454)),
    "arms": ((68, 164, 175, 253), (665, 454)),
    "eyes": ((67, 91, 175, 135), (665, 725)),
}
COLORS = {"original": "F2CFA2", "blue": "86BDED", "green": "94C9A1", "purple": "C5A1DD"}

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


def ear_center(t, right):
    if right:
        if t < 0.6:
            controls = [(154, 66), (187, 47), (191, 90), (203, 128)]
            u = t / 0.6
        else:
            controls = [(203, 128), (215, 166), (232, 164), (210, 201)]
            u = (t - 0.6) / 0.4
    else:
        controls = [(97, 64), (36, 38), (2, 158), (35, 215)]
        u = t
    a, b, c, d = [np.array(p, dtype=float) for p in controls]
    center = (1-u)**3*a + 3*(1-u)**2*u*b + 3*(1-u)*u*u*c + u**3*d
    tangent = 3*(1-u)**2*(b-a) + 6*(1-u)*u*(c-b) + 3*u*u*(d-c)
    normal = np.array((tangent[1], -tangent[0])) / np.linalg.norm(tangent)
    return center, normal


def ear_point(t, angle, right, offset=0):
    center, normal = ear_center(t, right)
    taper = math.sin(math.pi * t) ** 0.3
    root = min(1, t / 0.35)
    width = 23 * taper * (0.65 + 0.35 * root * root * (3 - 2 * root))
    x, y = center + normal * width * math.cos(angle)
    depth = -18 + 13 * taper * math.sin(angle) - offset
    return point(x, y, depth)


def lop_ear(name, right, fur, pink, group):
    rings, segments = 128, 64
    vertices = [ear_point(0, 0, right)]
    for ring in range(1, rings):
        for i in range(segments):
            vertices.append(ear_point(ring / rings, math.tau * i / segments, right))
    bottom = len(vertices)
    vertices.append(ear_point(1, 0, right))
    faces = [(0, 1 + i, 1 + (i + 1) % segments) for i in range(segments)]
    for ring in range(rings - 2):
        for i in range(segments):
            a, b = 1 + ring * segments + i, 1 + ring * segments + (i + 1) % segments
            faces.append((a, a + segments, b + segments, b))
    start = 1 + (rings - 2) * segments
    faces.extend((start + i, bottom, start + (i + 1) % segments) for i in range(segments))
    mesh_form(name + " soft lop ear", vertices, faces, fur, group)
    # A smooth rose patch follows the front of the curved ear with a fur border.
    vertices, faces = [], []
    rows, columns = 100, 32
    for row in range(rows + 1):
        s = row / rows
        t = 0.20 + 0.75 * s
        half = 0.95 * math.sin(math.pi * s) ** 0.55
        for col in range(columns + 1):
            angle = 4.8 + half * (2 * col / columns - 1)
            vertices.append(ear_point(t, angle, right, 0.12))
    for row in range(rows):
        for col in range(columns):
            a = row * (columns + 1) + col
            faces.append((a, a + columns + 1, a + columns + 2, a + 1))
    mesh_form(name + " pink inner ear", vertices, faces, pink, group)


def build():
    scene = bpy.data.scenes.new("Clocklings bunny")
    bpy.context.window.scene = scene
    fur = material("Bunny fur", COLORS["original"])
    pale = material("Bunny pale fur", "F9E7D1", 0.42)
    arms = material("Bunny arms", COLORS["original"], 0.4)
    nodes = arms.node_tree.nodes
    texture = nodes.new("ShaderNodeTexCoord")
    separate = nodes.new("ShaderNodeSeparateXYZ")
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.interpolation = "EASE"
    ramp.color_ramp.elements[0].position = 0.1
    ramp.color_ramp.elements[0].color = rgba("F9E7D1")
    ramp.color_ramp.elements[1].position = 0.48
    ramp.color_ramp.elements[1].color = rgba(COLORS["original"])
    shader = next(n for n in nodes if n.type == "BSDF_PRINCIPLED")
    arms.node_tree.links.new(texture.outputs["Generated"], separate.inputs[0])
    arms.node_tree.links.new(separate.outputs["Z"], ramp.inputs[0])
    arms.node_tree.links.new(ramp.outputs[0], shader.inputs["Base Color"])
    foot = material("Bunny foot fur", "F5DAB6", 0.43)
    crease = material("Bunny creases", "A58D6E", 0.46)
    pink = material("Pink inner ears and pads", "EE9189", 0.5)
    nose = material("Pink nose and tongue", "EF8FA1", 0.3)
    cheek = material("Rosy cheeks", "F3A1AB", 0.47)
    cocoa = material("Cocoa smile", "4B2428", 0.42)
    eye = material("Deep plum eyes", "261C2A", 0.16)
    iris = material("Warm plum iris", "513C4D", 0.22)
    white = material("Eye white and teeth", "FFF5EB", 0.25)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    oval("Round cotton tail", 186.2, 238.8, 17, 16.5, 23, 16, fur, "body")
    oval("Soft seated body", 121.4, 222, 6, 56, 59, 34, fur, "body")
    oval("Pale chest", 121.4, 200, -22, 24, 26, 12, pale, "body")
    for name, x, y, rx, ry, pads in [
        ("Left", 76.8, 259.8, 26.3, 23.3, [(81.5,268.5,12,9.6),(60.1,258.4,4.6,4.6),(68.4,248.8,5,5),(82.1,248.8,4.7,4.7)]),
        ("Right", 164.5, 259.5, 27.3, 23.5, [(163,268,12,9.6),(162.5,248.8,4.7,4.7),(176,249.7,5,5),(183.6,258.2,4.6,4.6)]),
    ]:
        paw = oval(name + " seated foot", x, y, -19, rx, ry, 19, foot, "body")
        bpy.context.view_layer.update()
        for i, (px, py, prx, pry) in enumerate(pads):
            surface_mark(name + f" sole pad {i}", paw, px, py, prx, pry, pink, "body")

    for name, x, angle, fingers in [
        ("Left", 103, -17, [(103.9,230,105.6,238),(112,229,114,237)]),
        ("Right", 139.8, 17, [(138.9,230,137.2,238),(130,229,128,237)]),
    ]:
        arm = oval(name + " continuous arm and paw", x, 212, -27, 16.5, 35, 15, arms, "arms")
        arm.rotation_euler[1] = math.radians(angle)
        bpy.context.view_layer.update()
        for i, (x0,y0,x1,y1) in enumerate(fingers):
            points = [(x0+(x1-x0)*t,y0+(y1-y0)*t) for t in np.linspace(0,1,6)]
            line(name + f" finger {i}", [(px,py,front_depth(arm,px,py,0.55)) for px,py in points], 0.55, crease, "arms", [0.45,0.8,1,1,0.8,0.3])

    cushion("Tall forehead tuft", [(102,62),(110,44),(129,36),(121,49),(119,62)], 0, 8, fur, "head")
    cushion("Small forehead tuft", [(117,61),(125,47),(142,44),(133,55),(130,63)], 0, 7, fur, "head")
    head = oval("Single rounded head", 120.5, 112.5, 0, 70, 61, 44, fur, "head")
    bpy.context.view_layer.update()
    muzzle = oval("Pale rounded muzzle", 121.8, 142.6, -39, 26.4, 20.6, 12, pale, "head")
    bpy.context.view_layer.update()
    for x,y in [(73.6,135.8),(167.5,134.8)]:
        surface_mark(f"Rosy cheek {x}", head, x, y, 12, 8, cheek, "head")
    for x,y in [(87.2,113),(153.9,112.5)]:
        depth = front_depth(head,x,y)
        toy_eye(oval, material, f"Bunny eye {x}", x, y, depth - 1, 15.5, 17, "eyes", "725069")
    for i, points in enumerate([[(78.5,84.5),(86,78),(95.5,76)],[(145,76),(154,78),(161,84.5)]]):
        line(f"Soft brow {i}",[(x,y,front_depth(head,x,y,0.7)) for x,y in points],1.7,crease,"head",[0.35,1,0.5])

    # The mouth sits on the muzzle surface, with a tongue and two separate front teeth.
    outline = [(105.5,139.5),(113,142),(121.5,143),(130,142),(137.5,139.5),(133,150),(121.5,154.5),(110,150)]
    cushion("Open happy smile", outline, -50, 1.7, cocoa, "head")
    oval("Small pink tongue",121.5,151.7,-52,7.5,2.5,1.1,nose,"head")
    for x in (118.25,125.25):
        bpy.ops.mesh.primitive_cube_add(size=2, location=point(x,145.2,-52.5))
        tooth = bpy.context.object
        tooth.scale = (3.2/50,1.4/50,3/50)
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
        bevel = tooth.modifiers.new("Soft tooth corners", "BEVEL")
        bevel.width, bevel.segments = 0.7/50, 4
        finish(tooth,f"Front tooth {x}",white,"head")
    line("Philtrum",[(121.8,134,-51),(121.8,138,-52),(121.7,141,-52)],0.85,cocoa,"head")
    cushion("Soft heart nose",[(114.5,128.8),(117.5,126.5),(121.8,126),(127,126.8),(129.5,129),(126,133),(121.8,135),(117,133)],-52,3,nose,"head")
    oval("Nose glint",119.5,128.5,-55,2.6,1.1,0.5,glint,"head")
    lop_ear("Left",False,fur,pink,"ear_left")
    lop_ear("Right",True,fur,pink,"ear_right")
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
        ramp = next(n for n in bpy.data.materials["Bunny arms"].node_tree.nodes if n.type == "VALTORGB")
        ramp.color_ramp.elements[0].color = tuple(linear(int(hex_color[i:i + 2],16)/255*0.5+0.5) for i in (0,2,4)) + (1,)
        ramp.color_ramp.elements[1].color = rgba(hex_color)
        shader = next(n for n in bpy.data.materials["Bunny fur"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        for name, white_mix, factor in (("Bunny pale fur", 0.5, 1), ("Bunny foot fur", 0.22, 1), ("Bunny creases", 0, 0.68)):
            shader = next(n for n in bpy.data.materials[name].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
            shader.inputs["Base Color"].default_value = tuple(linear((int(hex_color[i:i + 2], 16) / 255 * (1 - white_mix) + white_mix) * factor) for i in (0, 2, 4)) + (1,)
        atlas = np.zeros((1028, 992, 4), dtype=np.float32)
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
        image = bpy.data.images.new(f"Bunny {color}", width=992, height=1028, alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"bunny_blender_{color}.png")
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
            scene = bpy.data.scenes["Clocklings bunny"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings bunny"]
        bpy.context.window.scene = scene
        configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)", flush=True)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)
