"""Build an editable car and render layers on the existing 240 x 300 app grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_car_layers.py -- --rebuild --preview
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
from reference_finish import configure, studio


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/car.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-car/render"
REGIONS = {
    "body": ((3, 150, 240, 279), (0, 0)),
    "wheels": ((2, 191, 226, 283), (0, 391)),
    "eye_left": ((100, 215, 147, 245), (0, 671)),
    "eye_right": ((191, 212, 224, 242), (145, 671)),
}
COLORS = {"original": "F07A2E", "blue": "86BDED", "green": "94C9A1", "purple": "C5A1DD"}

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



def car_point(u, v, height):
    """Three-quarter vehicle coordinates fitted to the existing app anchors."""
    x = 119.45 + 26.75*u + 71*v
    y = 234.75 + 8.85*u - 2.5*v - height
    depth = -35*u + 12*v + 0.18*height
    return point(x, y, depth)


def shell(name, profiles, mat):
    rows, sides = 100, 80
    values = np.array(profiles)
    vertices, faces = [], []
    for row in range(rows + 1):
        u = values[0,0] + (values[-1,0]-values[0,0])*row/rows
        index = min(len(values)-2,max(0,np.searchsorted(values[:,0],u)-1))
        distance = values[index+1,0]-values[index,0]
        t = (u-values[index,0])/distance
        before, after = max(0,index-1), min(len(values)-1,index+2)
        slope0 = (values[index+1,1:]-values[before,1:])/(values[index+1,0]-values[before,0])
        slope1 = (values[after,1:]-values[index,1:])/(values[after,0]-values[index,0])
        width, low, high = ((2*t**3-3*t*t+1)*values[index,1:] + (t**3-2*t*t+t)*distance*slope0
                            + (-2*t**3+3*t*t)*values[index+1,1:] + (t**3-t*t)*distance*slope1)
        for i in range(sides):
            angle = i*math.tau/sides
            cosine, sine = math.cos(angle), math.sin(angle)
            v = width * math.copysign(abs(cosine)**0.65, cosine)
            height = (low+high)/2 + (high-low)/2*math.copysign(abs(sine)**0.65,sine)
            vertices.append(car_point(u,v,height))
    for row in range(rows):
        for i in range(sides):
            a,b = row*sides+i,row*sides+(i+1)%sides
            faces.append((a,a+sides,b+sides,b))
    faces.extend((tuple(reversed(range(sides))),tuple(rows*sides+i for i in range(sides))))
    return mesh_form(name,vertices,faces,mat,"body")


def arch_cut(body, name, x, y, rx, ry):
    sides = 96
    vertices = [point(x+rx*math.cos(i*math.tau/sides),y+ry*math.sin(i*math.tau/sides),depth)
                for depth in (-200,200) for i in range(sides)]
    faces = [(i,(i+1)%sides,(i+1)%sides+sides,i+sides) for i in range(sides)]
    faces.extend((tuple(reversed(range(sides))),tuple(sides+i for i in range(sides))))
    cutter = mesh_form(name,vertices,faces,body.data.materials[0],"temporary")
    bpy.context.view_layer.objects.active = body
    boolean = body.modifiers.new(name,"BOOLEAN")
    boolean.operation = "DIFFERENCE"
    boolean.solver = "EXACT"
    boolean.object = cutter
    bpy.ops.object.modifier_apply(modifier=boolean.name)
    bpy.data.objects.remove(cutter,do_unlink=True)


def bezier(points, t):
    a, b, c, d = [np.array(p, dtype=float) for p in points]
    return (1-t)**3*a + 3*(1-t)**2*t*b + 3*(1-t)*t*t*c + t**3*d


def panel(top, bottom, left, right, bulge=0):
    """Curved body panel with shared three-dimensional boundary curves."""
    def sample(u, v):
        corners = ((1-u)*(1-v)*np.array(top[0]) + u*(1-v)*np.array(top[-1])
                   + (1-u)*v*np.array(bottom[0]) + u*v*np.array(bottom[-1]))
        value = ((1-v)*bezier(top,u) + v*bezier(bottom,u)
                 + (1-u)*bezier(left,v) + u*bezier(right,v) - corners)
        value[2] += bulge*math.sin(math.pi*u)*math.sin(math.pi*v)
        return value
    return sample


def straight(a, b):
    return [a, tuple((2*np.array(a)+b)/3), tuple((np.array(a)+2*np.array(b))/3), b]


def cabin_panels(paint):
    # The reference has a tall windscreen, a generous side window and a narrow roof rim.
    front_top = [(87,162,-5),(102,157,1),(128,156,6),(144,158,12)]
    front_bottom = [(92,211,-25),(116,212,-20),(152,209,-10),(176,204,-5)]
    front_left = [(87,162,-5),(86,177,-16),(90,200,-24),(92,211,-25)]
    front_right = [(144,158,12),(159,169,5),(175,193,-3),(176,204,-5)]
    side_top = [(63,164,34),(71,159,20),(80,159,6),(87,162,-5)]
    side_bottom = [(43,205,25),(60,211,8),(78,214,-16),(92,211,-25)]
    side_back = [(63,164,34),(55,176,32),(47,195,29),(43,205,25)]
    roof_back = [(63,164,34),(73,151,43),(101,149,55),(114,155,60)]
    roof_far = [(114,155,60),(125,152,45),(138,154,25),(144,158,12)]
    rear_bottom = [(43,205,25),(64,207,33),(94,205,63),(117,204,72)]
    rear_far = straight(roof_back[-1],rear_bottom[-1])
    far_bottom = straight(rear_bottom[-1],front_bottom[-1])
    front = panel(front_top,front_bottom,front_left,front_right,bulge=-7)
    side = panel(side_top,side_bottom,side_back,front_left,bulge=-3)
    surfaces = [front,side,
                panel(roof_back,front_top,side_top,roof_far),
                panel(roof_back,rear_bottom,side_back,rear_far),
                panel(roof_far,far_bottom,rear_far,front_right),
                panel(rear_bottom,front_bottom,side_bottom,far_bottom)]
    vertices, faces, steps = [], [], 40
    for surface in surfaces:
        start = len(vertices)
        for row in range(steps+1):
            for col in range(steps+1):
                vertices.append(point(*surface(col/steps,row/steps)))
        for row in range(steps):
            for col in range(steps):
                a = start+row*(steps+1)+col
                faces.append((a,a+1,a+steps+2,a+steps+1))
    obj = mesh_form("Reference-shaped curved cabin",vertices,faces,paint,"body")
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.remove_doubles(bm,verts=bm.verts,dist=0.00001)
    bmesh.ops.recalc_face_normals(bm,faces=bm.faces)
    bm.to_mesh(obj.data)
    bm.free()
    return obj,front,side


def glazing(name, target, surface, mat, margins, tilt=0):
    u0,v0,u1,v1 = margins
    radius = 0.065
    arcs = []
    for cx,cy,start in [(u0+radius,v0+radius,math.pi),
                        (u1-radius,v0+radius,1.5*math.pi),
                        (u1-radius,v1-radius,0),
                        (u0+radius,v1-radius,0.5*math.pi)]:
        arcs.append([np.array((cx+radius*math.cos(angle),cy+radius*math.sin(angle)))
                     for angle in np.linspace(start,start+math.pi/2,12)])
    border = []
    for index, arc in enumerate(arcs):
        border.extend(arc)
        end = arcs[(index+1)%len(arcs)][0]
        border.extend(arc[-1]+(end-arc[-1])*t for t in np.linspace(0,1,25)[1:-1])
    center = np.array(((u0+u1)/2,(v0+v1)/2))
    def projected(uv):
        u,v = uv
        return surface(u,v-tilt*u*(v-v0)/(v1-v0))
    x,y,_ = projected(center)
    vertices = [point(x,y,front_depth(target,x,y,0.5))]
    faces, rings, count = [],30,len(border)
    for ring in range(1,rings+1):
        for uv in border:
            x,y,_ = projected(center+(uv-center)*ring/rings)
            vertices.append(point(x,y,front_depth(target,x,y,0.5)))
    faces.extend((0,i+1,(i+1)%count+1) for i in range(count))
    for ring in range(rings-1):
        for i in range(count):
            a,b = 1+ring*count+i,1+ring*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    return mesh_form(name,vertices,faces,mat,"body")


def patch(name, target, outline, mat, group, offset=0.2):
    # Rounded polygon corners preserve straight glass edges without Bezier overshoot.
    corners = []
    for i, p in enumerate(outline):
        p = np.array(p,dtype=float)
        previous = np.array(outline[i-1])-p
        following = np.array(outline[(i+1)%len(outline)])-p
        start = p+previous*min(0.28,2.8/np.linalg.norm(previous))
        end = p+following*min(0.28,2.8/np.linalg.norm(following))
        corners.append([(1-t)**2*start+2*(1-t)*t*p+t*t*end for t in np.linspace(0,1,7)])
    border = []
    for i, corner in enumerate(corners):
        border.extend(corner)
        start, end = corner[-1],corners[(i+1)%len(corners)][0]
        border.extend(start+(end-start)*t for t in np.linspace(0,1,max(2,int(np.linalg.norm(end-start))))[1:-1])
    center = np.mean(border,axis=0)
    vertices = [point(*center,front_depth(target,*center,offset))]
    faces, count, rings = [],len(border),30
    for ring in range(1,rings+1):
        for p in border:
            x,y = center+(p-center)*ring/rings
            vertices.append(point(x,y,front_depth(target,x,y,offset)))
    faces.extend((0,1+i,1+(i+1)%count) for i in range(count))
    for ring in range(rings-1):
        for i in range(count):
            a,b = 1+ring*count+i,1+ring*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    return mesh_form(name,vertices,faces,mat,group)


def wheel(name, x, y, rx, ry, hx, hy, hrx, hry, depth, rubber, cream, inset):
    oval(name+" rounded tire",x,y,depth,rx,ry,12,rubber,"wheels")
    oval(name+" dark rim recess",hx,hy,depth-12,hrx,hry,1.5,inset,"wheels")
    vertices, faces = [], []
    segments, tube = 96, 16
    for i in range(segments):
        a = i*math.tau/segments
        for j in range(tube):
            b = j*math.tau/tube
            vertices.append(point(hx+hrx*(0.94+0.07*math.cos(b))*math.cos(a),
                                  hy+hry*(0.94+0.07*math.cos(b))*math.sin(a),
                                  depth-14+1.3*math.sin(b)))
    for i in range(segments):
        for j in range(tube):
            faces.append((i*tube+j,((i+1)%segments)*tube+j,((i+1)%segments)*tube+(j+1)%tube,i*tube+(j+1)%tube))
    mesh_form(name+" cream rim ring",vertices,faces,cream,"wheels")
    for i in range(5):
        a = -math.pi/2+i*math.tau/5
        outline = []
        for radius,half in [(0.16,-0.10),(0.91,-0.11),(0.91,0.11),(0.16,0.10)]:
            outline.append((hx+hrx*(radius*math.cos(a)-half*math.sin(a)),
                            hy+hry*(radius*math.sin(a)+half*math.cos(a))))
        vertices = [point(x,y,z) for z in (depth-15,depth-14) for x,y in outline]
        faces = [(0,1,2,3),(7,6,5,4),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)]
        spoke = mesh_form(name+f" spoke {i+1}",vertices,faces,cream,"wheels")
        bevel = spoke.modifiers.new("Rounded alloy edges","BEVEL")
        bevel.width = 0.016
        bevel.segments = 3
    oval(name+" rounded hub",hx,hy,depth-16,hrx*0.25,hry*0.25,1.3,cream,"wheels")


def build():
    scene = bpy.data.scenes.new("Clocklings car")
    bpy.context.window.scene = scene
    paint = material("Car paint",COLORS["original"],0.27)
    paint_shader = next(n for n in paint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    paint_shader.inputs["Coat Weight"].default_value = 0.25
    paint_shader.inputs["Coat Roughness"].default_value = 0.2
    rubber = material("Plum rubber","493A52",0.6)
    dark = material("Deep vent and rim gaps","2B2034",0.45)
    glass = material("Blue tinted glass","3D4E73",0.18)
    glass_shader = next(n for n in glass.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    glass_shader.inputs["Specular IOR Level"].default_value = 0.5
    cream = material("Cream alloy rims","FBE7BE",0.32)
    white = material("Warm headlight whites","FFF1D2",0.3)
    ink = material("Deep plum pupils and smile","271C2D",0.17)
    iris = material("Violet iris","59406B",0.2)
    glint = material("White catchlight","FFFFFF",0.12)
    shader = next(n for n in glint.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value = (1,1,1,1)
    shader.inputs["Emission Strength"].default_value = 0.3

    oval("Far front tire under bumper",207,261,9,13,15,13,rubber,"wheels")
    wheel("Rear",27,230,17,29,21.7,228.4,8.7,23,19,rubber,cream,dark)
    wheel("Front",81,245.5,24,33,75.2,246.1,14,23.1,-38,rubber,cream,dark)

    body = shell("Rounded sports body",[
        (-1.65,0.65,-6,18),(-1.50,0.90,-13,27),(-1.20,1.0,-14,35),
        (-0.8,1.0,-14,33),(0,1.0,-15,34),(0.7,1.0,-16,40),
        (1.2,0.98,-17,42),(1.52,0.96,-18,37),(1.64,0.90,-17,27),
    ],paint)
    cabin, windscreen, side_window = cabin_panels(paint)
    bpy.ops.object.select_all(action="DESELECT")
    body.select_set(True)
    cabin.select_set(True)
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.join()
    remesh = body.modifiers.new("Continuous body and cabin","REMESH")
    remesh.mode = "VOXEL"
    remesh.voxel_size = 0.016
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth = body.modifiers.new("Smooth sculpted panels","SMOOTH")
    smooth.factor = 0.8
    smooth.iterations = 8
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    arch_cut(body,"Rear wheel arch",27,232,16,31)
    arch_cut(body,"Front wheel arch",81,247,23,35)
    for face in body.data.polygons:
        face.use_smooth = True
    bpy.context.view_layer.update()

    glazing("Tall curved windshield",body,windscreen,glass,(0.045,0.065,0.965,0.76),tilt=0.1)
    glazing("Large curved side window",body,side_window,glass,(0.11,0.13,0.89,0.72))
    patch("Near side air vent",body,[(45,209),(50,211),(52,229),(49,226),(46,218)],dark,"body",0.3)
    patch("Left bumper intake",body,[(111,250),(123,252),(133,256),(126,262),(108,261)],dark,"body",0.3)
    patch("Right bumper intake",body,[(204,249),(213,246),(216,253),(208,256),(201,253)],dark,"body",0.3)
    points = [(155.5,244.5),(163,248.5),(173,250),(181,247.8),(186,244.2)]
    line("Friendly curved smile",[(x,y,front_depth(body,x,y,0.6)) for x,y in points],1.2,ink,"body",[0.6,1,1,1,0.6])

    line("Rear spoiler left support",[(31,187,56),(33,203,56)],2.8,paint,"body")
    line("Rear spoiler right support",[(89,177,68),(90,191,68)],2.5,paint,"body")
    cushion("Rear spoiler wing",[(15,185),(25,181),(70,176),(113,177),(104,181),(30,188)],65,3,paint,"body")
    line("Near mirror stalk",[(81,198,front_depth(body,81,198,-1)),(76,192,-44),(70,186,-49)],3.2,paint,"body")
    oval("Near oval mirror",69,185,-54,14.5,8.7,7.5,paint,"body")
    line("Far mirror stalk",[(165,191,front_depth(body,165,191,-1)),(174,184,-18),(178,180,-22)],2.3,paint,"body")
    oval("Far oval mirror",178,179,-27,11,7.2,6,paint,"body")

    for name,outline,x,y,rx,ry,group in [
        ("Near",[(124+19*math.cos(a),230+11.2*math.sin(a)-1.8*math.cos(a)) for a in np.linspace(0,math.tau,64,endpoint=False)],127,231,10.3,9.8,"eye_left"),
        ("Far",[(207.5+12*math.cos(a),229+10*math.sin(a)-1.2*math.cos(a)) for a in np.linspace(0,math.tau,64,endpoint=False)],206.5,229,7.6,8.2,"eye_right"),
    ]:
        patch(name+" almond headlight",body,outline,white,"body",0.5)
        depth = front_depth(body,x,y,1.4)
        oval(name+" eye iris",x,y,depth-0.5,rx,ry,1.3,iris,group)
        oval(name+" pupil",x,y-0.4,depth-1.5,rx*0.76,ry*0.78,0.8,ink,group)
        oval(name+" large sparkle",x-2.4,y-3.4,depth-2.4,2.3,2.5,0.4,glint,group)
        oval(name+" small sparkle",x+2.8,y+3.6,depth-2.3,1.0,1.0,0.3,glint,group)
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
        shader = next(n for n in bpy.data.materials["Car paint"].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        shader.inputs["Base Color"].default_value = rgba(hex_color)
        atlas = np.zeros((765, 715, 4), dtype=np.float32)
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
        image = bpy.data.images.new(f"Car {color}",width=715,height=765,alpha=True)
        image.colorspace_settings.name = "sRGB"
        image.pixels.foreach_set(atlas[::-1].reshape(-1))
        image.filepath_raw = str(OUTPUT / f"car_blender_{color}.png")
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
            scene = bpy.data.scenes["Clocklings car"]
            bpy.context.window.scene = scene
            for other in list(bpy.data.scenes):
                if other != scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version = 0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene = bpy.data.scenes["Clocklings car"]
        bpy.context.window.scene = scene
        configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)", flush=True)
    if "--preview" in sys.argv:
        scene.render.filepath = str(SCRATCH / "preview.png")
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)


