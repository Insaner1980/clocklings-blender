"""Build an editable unicorn and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_unicorn_layers.py -- --rebuild --preview
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
SOURCE = ROOT / "art/blender/unicorn.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-unicorn/render"
REGIONS = {
    "head": ((22, 0, 179, 191), (0, 0)),
    "body": ((63, 123, 185, 285), (475, 0)),
    "tail": ((151, 138, 221, 233), (0, 577)),
    "eyes": ((40, 99, 119, 136), (214, 577)),
}
COLORS = {"original": "F8F1F3", "blue": "86BDED", "green": "94C9A1", "purple": "C5A1DD"}

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



def lock(name, controls, width, depth, thickness, mat, group):
    """A continuous, softly tapered hair lock with a rounded cross-section."""
    rings, sides = 100, 48
    a, b, c, d = [np.array(p, dtype=float) for p in controls]
    vertices, faces = [], []
    for row in range(rings + 1):
        t = row / rings
        center = (1-t)**3*a + 3*(1-t)**2*t*b + 3*(1-t)*t*t*c + t**3*d
        tangent = 3*(1-t)**2*(b-a) + 6*(1-t)*t*(c-b) + 3*t*t*(d-c)
        normal = np.array((tangent[1], -tangent[0])) / np.linalg.norm(tangent)
        taper = max(0.001, math.sin(math.pi*t)**0.5)
        for i in range(sides):
            angle = i * math.tau / sides
            x, y = center + normal * width * taper * math.cos(angle)
            vertices.append(point(x, y, depth + thickness*taper*math.sin(angle)))
    for row in range(rings):
        for i in range(sides):
            u, v = row*sides+i, row*sides+(i+1)%sides
            faces.append((u, u+sides, v+sides, v))
    faces.extend((tuple(reversed(range(sides))), tuple(rings*sides+i for i in range(sides))))
    return mesh_form(name, vertices, faces, mat, group)


def hoof(name, rect, depth, mat):
    x0, y0, x1, y1 = rect
    cx, radius = (x0+x1)/2, (x1-x0)/2
    profiles = [(y0,0.7),(y0+2,0.85),(y1-4,1),(y1-1,0.97),(y1,0.87)]
    count = 64
    vertices = [point(cx+radius*scale*math.cos(i*math.tau/count),y,
                      depth+11.5*scale*math.sin(i*math.tau/count))
                for y,scale in profiles for i in range(count)]
    faces = []
    for row in range(len(profiles)-1):
        for i in range(count):
            a,b=row*count+i,row*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    faces.extend((tuple(reversed(range(count))),tuple((len(profiles)-1)*count+i for i in range(count))))
    return mesh_form(name,vertices,faces,mat,"body")


def horn(gold, groove):
    rings, sides = 80, 64
    vertices, faces = [], []
    for row in range(rings + 1):
        t = row/rings
        radius = max(0.08, 12*(1-t)**0.85)
        for i in range(sides):
            angle = i*math.tau/sides
            vertices.append(point(79+radius*math.cos(angle), 62-57*t, -5+radius*math.sin(angle)))
    for row in range(rings):
        for i in range(sides):
            a, b = row*sides+i, row*sides+(i+1)%sides
            faces.append((a, a+sides, b+sides, b))
    faces.extend((tuple(reversed(range(sides))), tuple(rings*sides+i for i in range(sides))))
    mesh_form("Golden tapered horn", vertices, faces, gold, "head")
    points, radii = [], []
    for t in np.linspace(0.08, 0.95, 110):
        radius = 12*(1-t)**0.85
        angle = t*math.tau*4.2
        points.append((79+radius*math.cos(angle), 62-57*t, -5+radius*math.sin(angle)))
        radii.append(1-0.65*t)
    line("Golden spiral ridge", points, 1.0, groove, "head", radii)


def build():
    scene = bpy.data.scenes.new("Clocklings unicorn")
    bpy.context.window.scene = scene
    coat = material("Unicorn coat", COLORS["original"], 0.38)
    muzzle_mat = material("Cream muzzle", "FBE7BE", 0.4)
    pink = material("Rose mane", "F29BC7", 0.3)
    blue = material("Sky mane", "8FC3ED", 0.3)
    purple = material("Lilac mane and hooves", "BC88DD", 0.3)
    inner = material("Pink inner ears", "EE91AF", 0.45)
    cheek = material("Rosy cheeks", "F3A1AB", 0.5)
    nostril = material("Peach nostrils", "D6947C", 0.46)
    gold = material("Golden horn", "FFD06D", 0.28)
    groove = material("Spiral gold", "E9AF42", 0.32)
    ink = material("Deep plum eyes and smile", "28182F", 0.2)
    iris = material("Violet iris", "673985", 0.2)
    white = material("Eye whites", "FFF8FC", 0.28)
    glint = material("Eye catchlight", "FFFFFF", 0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    shader.inputs["Emission Strength"].default_value = 0.3

    # Three intertwined strands make one curled pastel tail behind the rump.
    lock("Tail lilac crest", [(157,174),(172,139),(211,151),(207,193)], 11, 19, 10, purple, "tail")
    lock("Tail pink curl", [(160,174),(211,145),(182,240),(216,205)], 13, 10, 11, pink, "tail")
    lock("Tail blue curl", [(159,174),(199,169),(171,216),(199,226)], 11, -1, 10, blue, "tail")

    # Weld the chest, neck and four legs into a smooth continuous coat.
    components = [
        oval("Torso volume", 128, 193, 8, 41, 29, 25, coat, "body"),
        oval("Chest volume", 104, 190, -3, 23, 32, 23, coat, "body"),
        oval("Neck volume", 101, 163, -2, 19, 36, 18, coat, "body"),
    ]
    for name, pts, radius in [
        ("Far front leg", [(95,198,18),(90,227,18),(82,265,18)], 10),
        ("Far hind leg", [(145,201,26),(143,231,26),(136,260,26)], 9),
        ("Near front leg", [(111,198,-13),(108,231,-13),(106.5,272,-13)], 11),
        ("Near hind leg", [(153,199,-1),(160,230,-1),(167,267,-1)], 11),
    ]:
        obj = line(name, pts, radius, coat, "body", [1.2,0.75,0.9])
        bpy.context.view_layer.objects.active = obj
        obj.select_set(True)
        bpy.ops.object.convert(target="MESH")
        components.append(bpy.context.object)
        bpy.ops.object.select_all(action="DESELECT")
    bpy.ops.object.select_all(action="DESELECT")
    for obj in components:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = components[0]
    bpy.ops.object.join()
    body = bpy.context.object
    body.name = "Continuous body neck and four legs"
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    remesh = body.modifiers.new("Continuous sculpted coat", "REMESH")
    remesh.mode = "VOXEL"
    remesh.voxel_size = 0.018
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth = body.modifiers.new("Soft sculpt transitions", "SMOOTH")
    smooth.factor = 0.8
    smooth.iterations = 20
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    for face in body.data.polygons:
        face.use_smooth = True
    for name, rect, depth in [
        ("Far front hoof", (69,256,95,275),18),
        ("Far hind hoof", (124,251,148,268),26),
        ("Near front hoof", (93,261,120,281),-13),
        ("Near hind hoof", (154,257,180,276),-1),
    ]:
        hoof(name, rect, depth, purple)

    # Locks behind the head continue down the neck as in the reference.
    lock("Mane lilac back", [(125,61),(160,60),(172,87),(154,106)], 13, 14, 13, purple, "head")
    lock("Mane blue fall", [(131,69),(185,83),(151,153),(130,177)], 16, 1, 14, blue, "head")
    lock("Mane pink neck curl", [(139,128),(152,154),(118,169),(116,186)], 11, -10, 10, pink, "head")
    cushion("Left pointed ear", [(43,80),(34,49),(38,34),(55,43),(69,64)], 1, 10, coat, "head")
    cushion("Left pink inner ear", [(45,69),(41,46),(45,43),(59,59)], -8.5, 3, inner, "head")
    cushion("Right pointed ear", [(117,64),(130,38),(138,35),(145,54),(143,76)], 2, 11, coat, "head")
    cushion("Right pink inner ear", [(126,64),(134,44),(138,43),(139,68)], -9, 3, inner, "head")
    head = oval("Single rounded head",86,108,0,60,54,39,coat,"head")
    muzzle = oval("Soft cream muzzle",78,145,-34,29,18,14,muzzle_mat,"head")
    bpy.context.view_layer.update()
    horn(gold, groove)
    lock("Swept rose forelock",[(35,90),(28,60),(59,51),(77,63)],13,-21,11,pink,"head")
    lock("Rose forehead curl",[(73,59),(76,73),(62,82),(70,99)],9,-33,8,pink,"head")
    lock("Lilac forelock",[(82,63),(94,45),(115,55),(124,76)],13,-18,12,purple,"head")
    bpy.context.view_layer.update()
    for name,x,y,rx,ry in [("Far",55,118,10.2,12.7),("Near",102,116,11.5,13.5)]:
        depth = front_depth(head,x,y,0.8)
        toy_eye(oval, material, name + " unicorn eye", x, y, depth - 1, rx + 0.6, ry + 0.8, "eyes", "8953A6")
    for name,x,y,rx,ry in [("Far",44,134,7.2,5.6),("Near",119,135,9,6.6)]:
        surface_mark(name+" rosy cheek",head,x,y,rx,ry,cheek,"head")
    for name,points in [("Far",[(49,91),(54,87),(60,87)]),("Near",[(98,84),(105,84),(111,88)])]:
        line(name+" gentle brow",[(x,y,front_depth(head,x,y,0.6)) for x,y in points],1.5,purple,"head",[0.4,1,0.35])
    for x in (67.5,89):
        surface_mark("Small nostril "+str(x),muzzle,x,141,2.3,2.7,nostril,"head")
    points = [(66,151),(72,154.5),(79,155.5),(86,154.5),(91,150.5)]
    line("Friendly curved smile",[(x,y,front_depth(muzzle,x,y,0.6)) for x,y in points],1.1,ink,"head",[0.6,1,1,1,0.6])
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
    scene.view_settings.exposure = -0.85
    # Save only this character's scene, leaving the live session's other scenes untouched.
    bpy.data.libraries.write(str(SOURCE), {scene}, fake_user=True, compress=True)
    return scene



def render_layers(scene):
    geometry = [o for o in scene.objects if o.get("layer")]
    for color, hex_color in COLORS.items():
        shader = next(n for n in bpy.data.materials["Unicorn coat"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        atlas = np.zeros((866, 845, 4), dtype=np.float32)
        for group, (rect, origin) in REGIONS.items():
            for obj in geometry:
                obj.visible_camera = True
                obj.hide_render = obj["layer"] != group
            path = SCRATCH / f"{color}_{group}.png"
            scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            rendered = bpy.data.images.load(str(path), check_existing=False)
            pixels = read_layer_pixels(rendered, 720, 900)
            bpy.data.images.remove(rendered)
            x0,y0,x1,y1 = [v*3 for v in rect]
            outside = pixels[:,:,3].copy()
            outside[y0:y1,x0:x1] = 0
            assert outside.max() < 0.01, f"Clipped {color}/{group}"
            cropped = pixels[y0:y1,x0:x1]
            assert cropped[:,:,3].max() > 0.9, f"Empty {color}/{group}"
            ax,ay = origin
            atlas[ay:ay+y1-y0,ax:ax+x1-x0] = cropped
        image = bpy.data.images.new(f"Unicorn {color}",width=845,height=866,alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"unicorn_blender_{color}.png")
        image.file_format = "PNG"
        image.save()
        bpy.data.images.remove(image)
        print(f"ATLAS_COMPLETE {color}",flush=True)

if __name__ == "__main__":
    SCRATCH.mkdir(parents=True, exist_ok=True)
    if "--rebuild" in sys.argv:
        scene = build()
        if bpy.app.background:
            # Turn the scene-only library into a normal editable startup file.
            bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
            scene = bpy.data.scenes["Clocklings unicorn"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings unicorn"]
        bpy.context.window.scene = scene
        configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)", flush=True)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)


