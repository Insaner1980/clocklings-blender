"""Build the editable doll and render its wardrobe on the existing 200 x 340 grid.

Blender --background --factory-startup --threads 8 --python-exit-code 1
        --python tools/blender/render_doll_layers.py -- --rebuild --preview
Omit --preview to export the app atlas. Omit --rebuild to render the edited model.
"""
from pathlib import Path
import math
import sys
import json
import bpy
import bmesh
import numpy as np
from mathutils import Vector
sys.path.insert(0, str(Path(__file__).resolve().parent))
from reference_finish import toy_eye, studio

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/doll.blend"
OUTPUT = ROOT / "art/blender/atlases/doll_blender.png"
SCRATCH = ROOT / ".scratch/blender-doll/render"
REGIONS = {
    "hair_gold_back": ([27,18,173,216], [0,0]),
    "hair_rainbow_back": ([27,18,173,216], [442,0]),
    "hair_curly_back": ([10,0,190,196], [884,0]),
    "dress_overalls": ([40,135,160,291], [1428,0]),
    "hair_braids_back": ([10,18,190,156], [0,598]),
    "dress_yellow": ([38,132,163,259], [544,598]),
    "dress_dots": ([38,132,163,259], [923,598]),
    "dress_stars": ([38,132,163,259], [1302,598]),
    "dress_rainbow": ([38,132,163,259], [0,1016]),
    "head": ([47,38,153,139], [379,1016]),
    "hair_braids_front": ([40,18,160,108], [701,1016]),
    "hair_gold_front": ([40,18,160,108], [1065,1016]),
    "hair_curly_front": ([37,18,163,108], [1429,1016]),
    "hair_rainbow_front": ([40,18,160,108], [0,1392]),
    "arm_left": ([20,154,71,224], [364,1392]),
    "arm_right": ([129,154,180,224], [521,1392]),
    "shoes_boots": ([54,249,144,315], [678,1392]),
    "legs": ([64,243,133,300], [952,1392]),
    "shoes_white": ([54,280,144,315], [1163,1392]),
    "shoes_red": ([54,280,144,315], [1437,1392]),
    "eye_left": ([63,77,91,109], [1711,1392]),
    "eye_right": ([109,77,137,109], [1799,1392]),
    "neck": ([87,124,113,149], [1887,1392]),
}
DEFAULT = {"head","neck","legs","arm_left","arm_right","eye_left","eye_right",
           "hair_braids_back","hair_braids_front","dress_yellow","shoes_white"}
RAINBOW = ["FF6B6B","FFA94D","FCC419","51CF66","339AF0","845EF7","DA77F2"]

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
    return ((x - 100) / 50, depth / 50, (320 - y) / 50)


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


def cushion(name, outline, depth, bulge, mat, group, center=None):
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
    if center is not None:
        cx,cy=center
    count, rings = len(points), 20
    vertices, faces = [], []
    for side in (-1, 1):
        start = len(vertices)
        vertices.append(point(cx, cy, (depth(cx,cy) if callable(depth) else depth) + side * bulge))
        for ring in range(1, rings + 1):
            t = ring / rings
            for p in points:
                x,y = cx + (p.x-cx)*t, cy + (p.y-cy)*t
                base = depth(x,y) if callable(depth) else depth
                vertices.append(point(x,y,base + side*bulge*math.sqrt(max(0,1-t*t))))
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




def head_surface(x,y):
    ellipse=1-((x-100)/57)**2-((y-79)/56)**2
    return 5-36*math.sqrt(max(0.02,ellipse))


def curved_outline(name,outline,surface,mat,group):
    """Tessellate the concave hairline before bending it around the scalp."""
    from mathutils.geometry import interpolate_bezier
    data=bpy.data.curves.new(name+" boundary","CURVE")
    data.dimensions="3D"
    spline=data.splines.new("BEZIER")
    spline.bezier_points.add(len(outline)-1)
    for bp,(x,y) in zip(spline.bezier_points,outline):
        bp.co=(x,y,0)
        bp.handle_left_type=bp.handle_right_type="AUTO"
    spline.use_cyclic_u=True
    border=[]
    for i,bp in enumerate(spline.bezier_points):
        following=spline.bezier_points[(i+1)%len(outline)]
        border.extend(interpolate_bezier(bp.co,bp.handle_right,following.handle_left,following.co,7)[:-1])
    bpy.data.curves.remove(data)
    bm=bmesh.new()
    verts=[bm.verts.new(p) for p in border]
    bm.faces.new(verts)
    bmesh.ops.triangulate(bm,faces=list(bm.faces))
    bmesh.ops.subdivide_edges(bm,edges=list(bm.edges),cuts=12,use_grid_fill=True)
    for v in bm.verts:
        x,y=v.co.x,v.co.y
        v.co=point(x,y,surface(x,y))
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    mesh=bpy.data.meshes.new(name)
    bm.to_mesh(mesh)
    bm.free()
    obj=bpy.data.objects.new(name,mesh)
    bpy.context.collection.objects.link(obj)
    finish(obj,name,mat,group)
    solid=obj.modifiers.new("Solid hair shell","SOLIDIFY")
    solid.thickness=0.025
    solid.offset=1
    return obj


def soft_cheek(name,x,y,head,skin):
    vertices,faces,colors = [],[],[]
    count,rings=64,16
    for ring in range(rings+1):
        t=ring/rings
        for i in range(count):
            a=i*math.tau/count
            px,py=x+7.2*t*math.cos(a),y+5.2*t*math.sin(a)
            vertices.append(point(px,py,front_depth(head,px,py,0.15)))
            amount=0.24*(1-t*t)**2
            pink=rgba("F5847D")
            colors.append(pink[:3]+(amount,))
    for ring in range(rings):
        for i in range(count):
            a,b=ring*count+i,ring*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    mat=skin.copy()
    mat.name=name+" soft blush"
    attr=mat.node_tree.nodes.new("ShaderNodeVertexColor")
    attr.layer_name="Blush"
    shader=next(n for n in mat.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    mat.node_tree.links.new(attr.outputs["Color"],shader.inputs["Base Color"])
    mat.node_tree.links.new(attr.outputs["Alpha"],shader.inputs["Alpha"])
    obj=mesh_form(name,vertices,faces,mat,"head")
    obj.visible_shadow=False
    data=obj.data.color_attributes.new(name="Blush",type="FLOAT_COLOR",domain="POINT")
    for entry,color in zip(data.data,colors):
        entry.color=color


def rainbow_material(name,top,bottom):
    mat=material(name,"FF6B6B",0.32)
    nodes=mat.node_tree.nodes
    tex=nodes.new("ShaderNodeNewGeometry")
    split=nodes.new("ShaderNodeSeparateXYZ")
    mapping=nodes.new("ShaderNodeMapRange")
    mapping.inputs["From Min"].default_value=(320-bottom)/50
    mapping.inputs["From Max"].default_value=(320-top)/50
    ramp=nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.interpolation="LINEAR"
    stops=list(reversed(RAINBOW))
    ramp.color_ramp.elements[0].color=rgba(stops[0])
    ramp.color_ramp.elements[1].color=rgba(stops[-1])
    for index,color in enumerate(stops[1:-1],1):
        ramp.color_ramp.elements.new(index/6).color=rgba(color)
    links=mat.node_tree.links
    links.new(tex.outputs["Position"],split.inputs["Vector"])
    links.new(split.outputs["Z"],mapping.inputs["Value"])
    links.new(mapping.outputs["Result"],ramp.inputs["Fac"])
    shader=next(n for n in nodes if n.type=="BSDF_PRINCIPLED")
    links.new(ramp.outputs["Color"],shader.inputs["Base Color"])
    return mat


def hair_style(style,mat,tie):
    back,front="hair_"+style+"_back","hair_"+style+"_front"
    oval(style+" back crown",100,74,13,55.5,49.5,26,mat,back)
    if style=="braids":
        outline=[(35,93),(45,97),(51,108),(58,116),(57,130),(48,142),(43,150),
                 (31,146),(21,137),(15,124),(17,110),(25,99)]
        for side in (1,-1):
            tuft=oval(("Left" if side==1 else "Right")+" soft pigtail",
                      34 if side==1 else 166,120,9,20,28,18,mat,back)
            for vertex in tuft.data.vertices:
                vertex.co.x *= 0.88 + 0.12 * vertex.co.z
            tuft.rotation_euler.y=math.radians(-12*side)
            x=44.5 if side==1 else 155.5
            band=oval(("Left" if side==1 else "Right")+" pink hair tie",x,97.2,-6,6.7,12,5,tie,back)
            band.rotation_euler.y=math.radians(12*side)
    elif style in ("gold","rainbow"):
        cushion(style+" long flowing hair",
                [(46,67),(66,37),(100,28),(137,39),(155,70),(156,122),(162,174),
                 (163,201),(143,209),(117,211),(99,207),(76,213),(51,209),(37,200),(41,143)],
                18,15,mat,back)
    else:
        for index,(x,y,rx,ry) in enumerate([(54,56,26,26),(42,96,24,24),(44,134,22,22),
                                          (54,168,18,18),(146,56,26,26),(158,96,24,24),
                                          (156,134,22,22),(146,168,18,18),(100,36,34,34)]):
            oval(f"Pink rounded curl {index+1}",x,y,14,rx,ry,16,mat,back)
    curved_outline(style+" continuous swept fringe",
            [(50,103),(45,85),(44,65),(49,49),(63,35),(82,26),(100,24),(120,28),
             (133,37),(146,46),(153,63),(154,84),(149,103),(144,89),(136,70),
             (128,56),(115,56),(104,62),(92,65),(80,66),(72,70),(64,82),(57,91),(56,104)],
            head_surface,mat,front)
    if style != "curly":
        for index, points in enumerate([
            [(48,79),(57,58),(81,42),(112,35)],
            [(55,89),(66,67),(87,54),(119,38)],
        ]):
            line(style + f" sculpted swept lock {index}",
                 [(x,y,head_surface(x,y)-0.2) for x,y in points],
                 2.8,mat,front,[0.1,0.9,1,0.1])
    if style=="curly":
        for x in (50,150):
            oval("Pink temple curl "+str(x),x,84,-8,10.5,11,8,mat,front)


def garment(name,mat,group,hem=None):
    profiles=[(140,23,12),(148,27,15),(166,30,18),(190,39,23),(220,51,27),
              (236,55.5,29),(241,57.5,30),(245,59,30),(248,59,30)]
    vertices,faces=[],[]
    count=96
    for y,rx,rz in profiles:
        for i in range(count):
            a=i*math.tau/count
            vertices.append(point(100+rx*math.cos(a),y-6*math.sin(a),7+rz*math.sin(a)))
    for row in range(len(profiles)-1):
        for i in range(count):
            a,b=row*count+i,row*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    faces.extend((tuple(reversed(range(count))),tuple((len(profiles)-1)*count+i for i in range(count))))
    obj=mesh_form(name,vertices,faces,mat,group)
    bevel=obj.modifiers.new("Soft fabric rim","BEVEL")
    bevel.width=0.025
    bevel.segments=3
    if hem:
        obj.data.materials.append(hem)
        for face in obj.data.polygons:
            if face.index//count==len(profiles)-4:
                face.material_index=1
    return obj


def collar(target,mat,group):
    for side in (1,-1):
        outline=[(99.7,139),(87,137),(77,140),(78,148),(84,154),(92,154),(99.7,148)]
        if side==-1: outline=[(200-x,y) for x,y in outline]
        cushion(("Left" if side==1 else "Right")+" Peter Pan collar "+group,outline,-13,2.6,mat,group)


def decorate_dress(target,group,white,gold,stars=False):
    bpy.context.view_layer.update()
    for row,y in enumerate(range(165,244,17)):
        for x in range(51+(row%2)*8,153,17):
            try: depth=front_depth(target,x,y,0.45)
            except AssertionError: continue
            if stars:
                outline=[(x+(3.8 if i%2==0 else 1.65)*math.cos(-math.pi/2+i*math.pi/5),
                          y+(3.8 if i%2==0 else 1.65)*math.sin(-math.pi/2+i*math.pi/5)) for i in range(10)]
                points=[]
                for px,py in outline:
                    try: points.append(point(px,py,front_depth(target,px,py,0.3)))
                    except AssertionError: break
                if len(points)==10:
                    mesh_form(f"Fabric star {x} {y}",points,[tuple(range(10))],gold if row%2 else white,group)
            else:
                oval(f"Fabric polka dot {x} {y}",x,y,depth,2.5,2.5,0.35,white,group)


def overalls(teal,cream,gold,cuff):
    group="dress_overalls"
    cushion("Cream shirt body",[(75,140),(100,136),(125,140),(140,165),(128,191),(72,191),(60,166)],7,13,cream,group)
    for x in (63,137):
        sleeve=oval("Cream short sleeve "+str(x),x,160,5,16,16,12,cream,group)
        sleeve.rotation_euler.y=math.radians(25 if x<100 else -25)
    outline=[(78,171),(122,171),(130,184),(138,208),(136,225),(100,235),
             (64,225),(62,208),(70,184)]
    bib=cushion("Teal bib and waist",outline,0,18,teal,group)
    pieces=[bib]
    for x in (76,124):
        pieces.append(shoe_volume("Rounded overall leg "+str(x),x,
                      [(182,5,5),(196,16,13),(215,22,18),(233,23,17),(260,22,16),(280,21,15)],teal,group))
    body=join_smooth(pieces,"Seamless teal overalls")
    for x,sign in [(77,1),(123,-1)]:
        line("Teal shoulder strap "+str(x),[(x,141,-10),(x+4*sign,152,-15),(x+7*sign,173,-17)],
             5.6,teal,group)
        oval("Gold bib button "+str(x),x+7*sign,173,-22,6.2,6.2,2.4,gold,group)
    bpy.context.view_layer.update()
    pocket=cushion("Raised rounded overall pocket",[(82,190),(118,190),(123,195),(122,213),(116,219),(86,219),(78,213),(77,196)],
                   lambda x,y:front_depth(body,x,y,0.4),1.1,cuff,group)
    for x in (76,124):
        shoe_volume("Turned overall cuff "+str(x),x,
                    [(272,21,14),(274,23,17),(283,23,17),(286,21,14)],cuff,group)


def shoe_volume(name,x,profiles,mat,group):
    count=64
    vertices=[point(x+rx*math.cos(i*math.tau/count),y,rz*math.sin(i*math.tau/count))
              for y,rx,rz in profiles for i in range(count)]
    faces=[]
    for row in range(len(profiles)-1):
        for i in range(count):
            a,b=row*count+i,row*count+(i+1)%count
            faces.append((a,a+count,b+count,b))
    faces.extend((tuple(reversed(range(count))),tuple((len(profiles)-1)*count+i for i in range(count))))
    return mesh_form(name,vertices,faces,mat,group)


def shoes(style,mat,sole,skin):
    group="shoes_"+style
    for x in (76.7,120.9):
        if style=="boots":
            cushion("Boot shaft "+str(x),[(x-13,262),(x+13,262),(x+13,300),(x-14,300)],3,9,mat,group)
            cushion("Boot cuff "+str(x),[(x-16,259),(x+16,259),(x+16,268),(x-16,268)],0,9,sole,group)
        dome=[(y,19.7*math.sqrt(max(0.0001,1-((y-301)/18)**2)),
                 16*math.sqrt(max(0.0001,1-((y-301)/18)**2))) for y in np.linspace(283,301,32)]
        shoe_volume(style+" domed shoe "+str(x),x,dome+[(306.5,19.5,15.8)],mat,group)
        shoe_volume(style+" flat rounded sole "+str(x),x,[(306.3,19.5,15.8),(308,19.8,16),
                    (310.5,19,15),(311.2,17,13.5)],sole,group)
        if style=="red":
            line("Red instep strap "+str(x),[(x-12,289,-10),(x,293,-17),(x+12,289,-10)],1.7,sole,group)


def join_smooth(objects,name):
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    bpy.context.view_layer.objects.active=objects[0]
    bpy.ops.object.convert(target="MESH")
    bpy.ops.object.join()
    obj=bpy.context.object
    obj.name=name
    remesh=obj.modifiers.new("Seamless skin","REMESH")
    remesh.mode="VOXEL"
    remesh.voxel_size=0.015
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth=obj.modifiers.new("Soft palm and thumb","SMOOTH")
    smooth.factor=0.6
    smooth.iterations=6
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    for face in obj.data.polygons:
        face.use_smooth=True
    return obj


def arm(side,skin):
    group="arm_left" if side==1 else "arm_right"
    def x(value): return value if side==1 else 200-value
    limb=line(group+" tapered limb",[(x(59),165,2),(x(50),184,1),(x(35),204,0)],
              7.3,skin,group,[1,0.95,0.85])
    palm=oval(group+" palm",x(33),208,0,8.5,11,7,skin,group)
    thumb=oval(group+" thumb",x(43),209,-1,4.1,6.7,5,skin,group)
    join_smooth([limb,palm,thumb],group+" smooth mitten hand")


def build():
    scene=bpy.data.scenes.new("Clocklings doll")
    bpy.context.window.scene=scene
    skin=material("Warm peach skin","FFD8B6",0.43)
    brown=material("Chestnut brown hair","98501F",0.29)
    gold_hair=material("Golden hair","F2B53A",0.31)
    pink=material("Pink curls","F06595",0.33)
    rainbow_hair=rainbow_material("Rainbow hair",24,212)
    tie=material("Pink hair bands","F34478",0.26)
    white=material("Warm ivory","FFF7E8",0.32)
    sole=material("Soft shoe sole","DED6D1",0.48)
    red=material("Red shoes","E03131",0.29)
    red_deep=material("Red shoe strap","BA252B",0.38)
    violet=material("Purple boots","9775FA",0.31)
    lavender=material("Lavender boot cuffs","E5DBFF",0.42)
    yellow=material("Yellow dress","FFD43B",0.36)
    hem=material("Golden hem","FAB005",0.35)
    dots=material("Pink polka-dot dress","F783AC",0.38)
    blue=material("Blue star dress","4DABF7",0.37)
    gold=material("Golden buttons and stars","FCC419",0.3)
    teal=material("Turquoise overalls","2FB8AE",0.39)
    cuff=material("Teal cuffs and pocket","38C3B6",0.39)
    cream=material("Cream overall shirt","FCEFCB",0.5)
    rainbow_dress=rainbow_material("Rainbow dress",140,254)
    ink=material("Eyelashes and pupils","24150F",0.22)
    iris=material("Chocolate brown irises","62331D",0.2)
    lip=material("Rose smile","C83248",0.32)
    glint=material("Eye catchlights","FFFFFF",0.15)
    shader=next(n for n in glint.node_tree.nodes if n.type=="BSDF_PRINCIPLED")
    shader.inputs["Emission Color"].default_value=(1,1,1,1)
    shader.inputs["Emission Strength"].default_value=0.3

    oval("Small neck",100,137,4,10,10,10,skin,"neck")
    for x in (80,117.5):
        line("Rounded lower leg "+str(x),[(x,246,7),(x,293,6)],13,skin,"legs")
    arm(1,skin)
    arm(-1,skin)
    head=oval("Round doll face",100,89,0,50,47,25,skin,"head")
    for x in (52,148):
        oval("Small ear "+str(x),x,99,1,4.5,9,7,skin,"head")
    bpy.context.view_layer.update()
    soft_cheek("Left peach cheek",66,105,head,skin)
    soft_cheek("Right peach cheek",134,105,head,skin)
    oval("Small button nose",100,106,front_depth(head,100,106,1),3.0,2.5,2.3,skin,"head")
    smile=[(89.5,113.5),(94,117.5),(100,119),(106,117.5),(109.5,113.5)]
    line("Friendly rose smile",[(x,y,front_depth(head,x,y,0.55)) for x,y in smile],1.35,lip,"head",[0.5,1,1,1,0.5])
    for label,x in [("Left",77),("Right",123.1)]:
        group="eye_left" if x<100 else "eye_right"
        depth=front_depth(head,x,92.4,0.8)
        toy_eye(oval, material, label + " doll eye", x, 92.4, depth, 9.2, 11.6, group, "885336")
        sign=-1 if x<100 else 1
        line(label+" outer eyelashes",[(x+sign*7,90,depth-1),(x+sign*10.5,89,depth-0.8),(x+sign*12,86.5,depth)],
             0.8,ink,group,[1,0.7,0.1])
        brow=[(x-6.5,76),(x-1,72.8),(x+5,74)]
        line(label+" arched brow",[(px,py,front_depth(head,px,py,0.5)) for px,py in brow],
             1.4,brown,"head",[0.25,1,0.2])
    for style,mat in [("braids",brown),("gold",gold_hair),("curly",pink),("rainbow",rainbow_hair)]:
        hair_style(style,mat,tie)
    for style,mat in [("yellow",yellow),("dots",dots),("stars",blue),("rainbow",rainbow_dress)]:
        group="dress_"+style
        dress=garment(style+" rounded A-line dress",mat,group,hem if style=="yellow" else None)
        for x in (64.5,135):
            oval(style+" puff sleeve "+str(x),x,156,5,13,13,11,mat,group)
        if style in ("yellow","dots"):
            collar(dress,white,group)
        if style in ("dots","stars"):
            decorate_dress(dress,group,white,gold,stars=style=="stars")
        if style=="stars":
            line("Gold waist ribbon",[(66,177,-3),(100,182,-17),(134,177,-3)],3,gold,group)
    overalls(teal,cream,gold,cuff)
    shoes("white",white,sole,skin)
    shoes("red",red,red_deep,skin)
    shoes("boots",violet,lavender,skin)

    camera_data=bpy.data.cameras.new("Front orthographic camera")
    camera=bpy.data.objects.new(camera_data.name,camera_data)
    scene.collection.objects.link(camera)
    camera.location=(0,-18,3.0)
    camera.rotation_euler=(math.pi/2,0,0)
    camera_data.type="ORTHO"
    camera_data.sensor_fit="VERTICAL"
    camera_data.ortho_scale=6.8
    scene.camera=camera
    for name,loc,power,size in [("Large soft key",(-4,-6,8),650,5),("Gentle fill",(4,-4,4),250,5),("Hair rim",(1,3,7),450,4)]:
        data=bpy.data.lights.new(name,"AREA")
        data.energy=power
        data.shape="DISK"
        data.size=size
        obj=bpy.data.objects.new(name,data)
        scene.collection.objects.link(obj)
        obj.location=loc
        obj.rotation_euler=(Vector((0,0,3))-obj.location).to_track_quat("-Z","Y").to_euler()
    scene.world=bpy.data.worlds.new("Soft doll studio")
    scene.world.use_nodes=True
    background=next(n for n in scene.world.node_tree.nodes if n.type=="BACKGROUND")
    background.inputs["Color"].default_value=(0.75,0.8,1,1)
    background.inputs["Strength"].default_value=0.28
    scene.view_settings.view_transform="Standard"
    scene.view_settings.exposure=-0.5
    scene["wardrobe_layers"]=json.dumps({group:list(rect) for group,(rect,_) in REGIONS.items()})
    configure(scene)
    studio(scene)
    show_layers(scene,DEFAULT)
    bpy.data.libraries.write(str(SOURCE),{scene},fake_user=True,compress=True)
    return scene


def configure(scene):
    scene.render.engine="CYCLES"
    scene.cycles.device="CPU"
    scene.cycles.samples=40
    scene.cycles.use_denoising=True
    scene.render.resolution_x=600
    scene.render.resolution_y=1020
    scene.render.resolution_percentage=100
    scene.render.film_transparent=True
    scene.render.image_settings.file_format="PNG"
    scene.render.image_settings.color_mode="RGBA"
    scene.render.threads_mode="FIXED"
    scene.render.threads=8


def show_layers(scene,groups):
    for obj in scene.objects:
        if obj.get("layer"):
            obj.hide_render=obj["layer"] not in groups
            obj.hide_set(obj["layer"] not in groups)


def render_layers(scene):
    atlas=np.zeros((1664,2048,4),dtype=np.float32)
    for group,(rect,origin) in REGIONS.items():
        show_layers(scene,{group})
        path=SCRATCH/(group+".png")
        scene.render.filepath=str(path)
        bpy.ops.render.render(write_still=True)
        rendered=bpy.data.images.load(str(path),check_existing=False)
        pixels=np.asarray(rendered.pixels[:],dtype=np.float32).reshape(1020,600,4)[::-1].copy()
        bpy.data.images.remove(rendered)
        x0,y0,x1,y1=[v*3 for v in rect]
        outside=pixels[:,:,3].copy()
        outside[y0:y1,x0:x1]=0
        assert outside.max()<0.01,f"Clipped {group}"
        cropped=pixels[y0:y1,x0:x1]
        assert cropped[:,:,3].max()>0.9,f"Empty {group}"
        ax,ay=origin
        atlas[ay:ay+y1-y0,ax:ax+x1-x0]=cropped
        print("LAYER_COMPLETE",group,flush=True)
    image=bpy.data.images.new("Doll wardrobe atlas",width=2048,height=1664,alpha=True)
    image.colorspace_settings.name="sRGB"
    image.pixels.foreach_set(atlas[::-1].reshape(-1))
    image.filepath_raw=str(OUTPUT)
    image.file_format="PNG"
    image.save()
    bpy.data.images.remove(image)
    print("ATLAS_COMPLETE doll",flush=True)


if __name__=="__main__":
    SCRATCH.mkdir(parents=True,exist_ok=True)
    if "--rebuild" in sys.argv:
        scene=build()
        if bpy.app.background:
            bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
            scene=bpy.data.scenes["Clocklings doll"]
            bpy.context.window.scene=scene
            for other in list(bpy.data.scenes):
                if other!=scene and not other.objects:
                    bpy.data.scenes.remove(other)
            bpy.context.preferences.filepaths.save_version=0
            bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE))
    else:
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
        scene=bpy.data.scenes["Clocklings doll"]
        bpy.context.window.scene=scene
        configure(scene)
    print(f"SCENE_READY {scene.name}: {len(scene.objects)} objects, {len(bpy.data.scenes)} scene(s)",flush=True)
    if "--preview" in sys.argv:
        preview=DEFAULT.copy()
        if "--overalls" in sys.argv:
            preview.remove("dress_yellow")
            preview.add("dress_overalls")
        show_layers(scene,preview)
        scene.render.filepath=str(SCRATCH/("overalls-preview.png" if "--overalls" in sys.argv else "preview.png"))
        bpy.ops.render.render(write_still=True)
    else:
        render_layers(scene)


