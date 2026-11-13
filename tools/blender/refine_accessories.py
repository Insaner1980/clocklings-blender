"""Reference-driven geometry corrections, applied to the existing editable scenes.

Open art/blender/<kind>_accessories.blend in one Blender MCP call, then call
refine(kind) in the next call. Render with this script's command-line entry.
Unchanged
accessories are retained as geometry and copied pixel-for-pixel by the packer.
"""
import importlib
import json
import math
from pathlib import Path
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))

import bpy
import render_reference_accessories as art
from render_reference_accessories import item, oval, tube, pillow, plate, box, ring, star
from reference_finish import fuse

ROOT = Path(__file__).resolve().parents[2]
SCRATCH = ROOT / '.scratch/accessory-refinement'
REPORT = ROOT / 'reports/accessory-refinement'
GOLD, CREAM, PINK = 'F8C83D', 'FFF1D3', 'F35C98'
CHANGED = {
    'cat': ['cat-neck-scarf', 'cat-extra-yarn'],
    'dragon': ['dragon-neck-scarf', 'dragon-extra-coins', 'dragon-extra-book', 'dragon-extra-star-cloud'],
    'dinosaur': ['dinosaur-neck-scarf', 'dinosaur-extra-backpack', 'dinosaur-extra-bucket'],
    'bunny': ['bunny-extra-wings', 'bunny-extra-watering-can'],
    'unicorn': ['unicorn-neck-rainbow-scarf', 'unicorn-head-horn-rings', 'unicorn-extra-cloud', 'unicorn-extra-wings'],
    'doll': ['extra-strawberry-bag', 'shoes-roller-skates', 'extra-siivet'],
    'car': ['car-wheels-sport', 'car-roof-taxi', 'car-front-bull-bar'],
    'robot': ['robot-neck-cape'],
}


def surface(name, mapping, colors, rows=64, columns=16, stripe_count=8):
    """Continuous rounded fabric with woven-in colors rather than raised bars."""
    vertices = [art.HELP.point(*mapping(col / columns, row / rows))
                for row in range(rows + 1) for col in range(columns + 1)]
    faces = []
    for row in range(rows):
        for col in range(columns):
            n = row * (columns + 1) + col
            faces.append((n, n + 1, n + columns + 2, n + columns + 1))
    obj = art.tag(art.HELP.mesh_form(name, vertices, faces, art.mat(colors[0]), 'accessory'))
    for color in colors[1:]:
        obj.data.materials.append(art.mat(color))
    for face in obj.data.polygons:
        face.material_index = int((face.index // columns + .5) / rows * stripe_count) % len(colors)
    solid = obj.modifiers.new('Soft fabric thickness', 'SOLIDIFY')
    solid.thickness = .035
    bevel = obj.modifiers.new('Soft fabric edge', 'BEVEL')
    bevel.width = .018
    bevel.segments = 3
    return obj


def scarf(x, y, colors, s=1):
    def tail(u, v):
        return (x + (17 + u * 24 + 4 * math.sin(v * 3)) * s,
                y + (7 + v * 67 + 2 * math.sin(u * math.pi)) * s,
                -78 - 3 * math.sin(u * math.pi) - 2 * math.sin(v * 5))
    surface('Continuous striped wool scarf tail', tail, colors, stripe_count=8)
    def collar(u, v):
        return (x + (v - .5) * 88 * s, y + (9 * math.sin(v * math.pi) + (u - .5) * 15) * s,
                -73 - 8 * math.sin(u * math.pi))
    surface('Continuous striped wool neck wrap', collar, colors, stripe_count=10)
    for k in range(8):
        xx, yy, dd = tail(k / 7, 1)
        tube('Soft wool tassel', [(xx, yy, dd), (xx - s, yy + 7 * s, dd)], .65 * s, colors[1])
    for k in range(9):
        u = (k + .5) / 9
        pts = [tail(u, v / 20) for v in range(21)]
        tube('Subtle knitted rib', [(a, b, c - .22) for a, b, c in pts], .18, colors[0])


def wings(cx, cy, color, edge, depth=23, scale=1):
    for side in (-1, 1):
        def pts(values):
            return [(cx + side * x * scale, cy + y * scale) for x, y in values]
        upper = [(26, 10), (48, -28), (86, -43), (98, -29), (84, 2), (48, 29)]
        lower = [(32, 20), (67, 13), (88, 34), (72, 59), (49, 58), (27, 35)]
        for outline in (upper, lower):
            pillow('Sculpted butterfly wing', pts(outline), color, depth, 5)
            tube('Fine wing perimeter', pts(outline + [outline[0]]), 1.4, edge, depth - 2)
        for path in [[(28, 23), (52, 3), (81, -28)], [(28, 23), (57, 29), (72, 48)]]:
            tube('Curved wing vein', pts(path), 1.3, edge, depth - 6)
        for xx, yy, rr in [(79, -17, 5), (66, -7, 3.5), (72, 37, 4)]:
            px, py = pts([(xx, yy)])[0]
            oval('Pearl wing marking', px, py, rr * scale, rr * scale, CREAM, depth - 7, 1)


def cat():
    item('cat-neck-scarf'); scarf(120, 169, ['E84254', CREAM], .95)
    item('cat-extra-yarn')
    oval('Soft blue yarn ball', 105, 255, 33, 32, '45B9E7', -75, 27)
    # Closely laid parallel strands wrap around the sphere in two broad bundles.
    for angle, span in [(-.42, range(-12, 13)), (.65, range(-4, 5))]:
        for k in span:
            offset = k * 2.3
            reach = math.sqrt(max(0, 30 ** 2 - offset ** 2))
            points = []
            for j in range(33):
                t = -1 + 2 * j / 32
                u, v = offset + 2 * (1 - t*t), t * reach
                dx = u * math.cos(angle) - v * math.sin(angle)
                dy = u * math.sin(angle) + v * math.cos(angle)
                d = -75 - 27 * math.sqrt(max(.01, 1 - (dx/33)**2 - (dy/32)**2)) - .6
                points.append((105 + dx, 255 + dy, d))
            tube('Close wrapped wool strand', points, .72, '65C4E7')
    tube('Loose yarn curl', [(125, 273), (148, 278), (159, 285), (179, 284), (190, 277)], 2, '45B9E7', -81)


def dragon():
    item('dragon-neck-scarf'); scarf(120, 159, ['35B8A5', 'FFD52C'], .93)
    item('dragon-extra-coins')
    pillow('Rounded treasure pouch', [(99, 231), (141, 231), (151, 254), (142, 278), (99, 278), (90, 254)], 'E83F54', -78, 18)
    oval('Dark opening inside pouch', 120, 229, 21, 7, '92283A', -81, 10)
    for x, y, count in [(107, 232, 3), (129, 231, 4), (77, 280, 2), (162, 282, 2)]:
        for k in range(count):
            oval('Coin resting in treasure', x, y-k*3.6, 11, 3.4, GOLD, -83, 7)
            tube('Coin rim gleam', [(x-8, y-k*3.6), (x, y-k*3.6+2), (x+8, y-k*3.6)], .55, 'FFECA0', -90)
    tube('Gathered pouch rim', [(98, 231), (106, 237), (120, 240), (134, 237), (142, 231)], 3.4, GOLD, -99)
    for side in (-1, 1):
        tube('Pouch drawstring loop', [(120, 240), (120+side*12, 234), (120+side*16, 241), (120, 241)], 1.5, GOLD, -101)
        tube('Pouch hanging drawstring', [(120, 240), (120+side*8, 251)], 1.4, GOLD, -101)
    star('Pouch embossed star', 120, 262, 12, GOLD, -98)
    item('dragon-extra-book')
    plate('Rounded red book cover', [(68, 235), (113, 239), (120, 245), (128, 239), (173, 235), (176, 276), (126, 283), (120, 286), (113, 283), (65, 276)], 'D83D55', -76, 4, 2)
    for side in (-1, 1):
        def page(u, v):
            return (120 + side * (3 + u*47), 244 + v*34 - math.sin(u*math.pi)*8,
                    -85 - math.sin(u*math.pi)*5)
        surface('Gently arched ivory pages', page, [CREAM], stripe_count=1)
        for i in range(3):
            tube('Layered page edges', [(120+side*4, 279+i*.7), (120+side*27, 271+i*.7), (120+side*49, 278+i*.7)], .45, 'D8BF8A', -85)
        for y in (252, 259, 266):
            tube('Fine printed line', [(120+side*10, y), (120+side*26, y-4), (120+side*42, y-1)], .35, 'AE9163', -91)
    tube('Book center binding', [(120, 246), (120, 282)], 1, 'C4A36B', -86)
    plate('Satin bookmark', [(140, 271), (145, 271), (148, 289), (143, 287), (140, 290)], PINK, -92, .4, .4)
    item('dragon-extra-star-cloud')
    parts = [oval('Cloud lobe', x, y, rx, ry, '7BCEF1', -82, 14) for x,y,rx,ry in
             [(52,273,28,17),(82,265,28,25),(119,270,32,21),(153,265,29,25),(183,274,28,17)]]
    art.tag(fuse([o.name for o in parts], 'Continuous blue cloud', voxel=.018))
    for x,y,r in [(75,269,13),(132,265,8),(182,274,7)]: star('Cloud golden star',x,y,r,GOLD,-100)


def dinosaur():
    item('dinosaur-neck-scarf'); scarf(139,154,['EF433D','FFD52C'],.88)
    item('dinosaur-extra-backpack')
    box('Soft yellow backpack shell',54,197,52,79,'F9C62E',-45,17,11)
    pillow('Rounded backpack flap',[(29,166),(39,159),(69,159),(79,167),(75,182),(53,186),(31,180)],'FFDA50',-64,4)
    tube('Backpack carry handle',[(42,158),(42,148),(65,148),(66,158)],3,'E6A82A',-46)
    box('Red rounded front pocket',54,216,39,25,'EF4552',-67,5,6)
    tube('Pocket top seam',[(38,209),(54,211),(70,209)],.65,'FF9290',-73)
    box('Pocket gold clasp',54,213,7,11,GOLD,-76,2,2)
    tube('Fitted shoulder strap',[(81,157),(86,176),(88,198),(84,216)],4,'E0AA29',-61)
    ring('Strap buckle',87,191,4,6,1.4,GOLD,-67)
    item('dinosaur-extra-bucket')
    # A round tapered cup instead of an extruded trapezoid.
    vertices=[]; faces=[]
    for j in range(25):
        t=j/24; radius=21-7*t
        for k in range(65):
            a=k*math.tau/64
            vertices.append(art.HELP.point(211+radius*math.cos(a),234+47*t,-73+radius*.65*math.sin(a)))
    for j in range(24):
        for k in range(64):
            n=j*65+k; faces.append((n,n+1,n+66,n+65))
    art.tag(art.HELP.mesh_form('Round tapered blue drink cup',vertices,faces,art.mat('49B1E6'),'accessory'))
    oval('White fitted drink lid',211,234,23,5,CREAM,-74,14)
    tube('Bent coral straw',[(215,231),(218,211),(221,205),(230,205)],3,'F24955',-77)
    star('Cup golden star',211,257,12,GOLD,-89)


def bunny():
    item('bunny-extra-wings',layer='back'); wings(120,211,'72BFEA','B1E2F7',scale=.97)
    item('bunny-extra-watering-can')
    oval('Blue watering can vessel',183,251,18,19,'60BCE6',-62,13)
    oval('Watering can dark opening',182,234,10,3,'347DA3',-65,5)
    ring('Rounded watering handle',204,247,10,14,2.8,'60BCE6',-62)
    tube('Long curved spout',[(169,258),(157,250),(150,238)],3.8,'60BCE6',-62)
    oval('Watering rose',148,236,8,4,'A6DCF5',-65,5,-30)
    for dx,dy in [(-3,-1),(1,-2),(4,0),(0,1)]: oval('Rose water hole',148+dx,236+dy,.6,.6,'3982A6',-70,.4)


def unicorn():
    item('unicorn-neck-rainbow-scarf')
    colors=['F47CA9','F9D34A','73C6EB','8BC47E']
    surface('Continuous rainbow knit scarf',lambda u,v:(87+u*25+3*math.sin(v*4),181+v*64,-55-3*math.sin(u*math.pi)),colors,stripe_count=9)
    surface('Rainbow scarf neck wrap',lambda u,v:(80+v*46,171+math.sin(v*math.pi)*10+u*9,-55-3*math.sin(u*math.pi)),colors,stripe_count=5)
    for x in range(89,113,4):tube('Rainbow scarf soft fringe',[(x,244),(x-1,252)],.8,'F9D34A',-57)
    item('unicorn-head-horn-rings','HEAD')
    for y,rx,c in [(39,8.8,PINK),(28,6.7,'A7D6F4'),(18,4.6,PINK)]:
        ring('Fitted pastel horn band',79,y,rx,2.4,1.7,c,-18)
    item('unicorn-extra-cloud')
    parts=[oval('Ivory cloud cushion lobe',x,y,rx,ry,'FFFAEF',-43,12) for x,y,rx,ry in
           [(69,281,17,13),(92,279,23,15),(121,280,25,14),(150,279,24,15),(175,282,17,12)]]
    art.tag(fuse([o.name for o in parts],'Soft connected ivory cloud',voxel=.014))
    item('unicorn-extra-wings')
    for x,y,rx,ry,a in [(168,148,42,9,-43),(174,160,40,9,-33),(176,172,34,9,-22),(170,183,26,9,-10)]:
        oval('Sculpted blue flight feather',x,y,rx,ry,'8DCFF1',-34,5,a)
        aa=math.radians(a)
        tube('Subtle feather shaft',[(x+u*math.cos(aa),y+u*math.sin(aa),-39) for u in (-rx*.7,0,rx*.7)],.35,'B9E5F5')
    for x,y,a in [(145,178,-40),(155,184,-25),(165,187,-12)]:oval('Layered small cover feather',x,y,17,8,'68B9E5',-42,4,a)


def doll():
    item('extra-strawberry-bag')
    surface('Pink crossbody shoulder strap',lambda u,v:(136-61*v+(u-.5)*5,146+73*v,-37-2*math.sin(v*math.pi)),[PINK],stripe_count=1)
    pillow('Plump strawberry shoulder bag',[(51,213),(63,208),(78,209),(88,216),(84,234),(70,246),(57,237)],'EF4053',-42,9)
    tube('Strawberry bag stitched edge',[(52,216),(56,234),(70,244),(83,231),(86,217)],.6,'C92E47',-49)
    for x,y,a in [(55,212,-50),(63,213,-25),(70,215,0),(78,213,25),(85,212,50)]:
        oval('Strawberry leafy flap',x,y,5,9,'78BD45',-53,2.4,a)
    for x,y in [(59,223),(70,224),(81,223),(64,233),(76,234)]:
        pillow('Teardrop strawberry seed',[(x,y-2.7),(x-1.8,y+.6),(x,y+2),(x+1.8,y+.6)],CREAM,-53,1)
    for x in (52,87):ring('Pink strap attachment',x,215,3,4,1.4,PINK,-49)
    item('shoes-roller-skates','WHEELS')
    for x in (76.7,120.9):
        parts=[box('Skate ankle',x,293,33,31,'43BDB9',-9,11,8),
               oval('Skate rounded toe',x,307,20,10,'43BDB9',-16,14)]
        art.tag(fuse([o.name for o in parts],'Continuous teal skate boot',voxel=.012))
        ring('Soft rolled ankle cuff',x,278,15,3,2.2,'6CD3CD',-10)
        pillow('Wide coral skate strap',[(x-15,288),(x+13,286),(x+16,294),(x-13,298)],'FA806D',-23,2)
        tube('Coral strap seam',[(x-12,291),(x+12,289)],.45,'FFC0A5',-26)
        box('Ivory skate outsole',x,315,41,7,CREAM,-15,14,3)
        box('Teal undercarriage',x,321,30,5,'237C80',-11,9,2)
        for dx in (-13,13):
            tube('Skate wheel axle',[(x+dx,323,-11),(x+dx,323,-25)],1.5,'B29148')
            oval('Golden quad wheel',x+dx,325,7,8,GOLD,-23,5)
            ring('Recessed wheel hub rim',x+dx,325,2.7,3,.8,'E6A629',-28)
            oval('Wheel axle cap',x+dx,325,1.3,1.5,CREAM,-29,1)
    item('extra-siivet',layer='back'); wings(100,181,'B89AE3','DBC6F3',depth=20,scale=.77)


def car():
    hubs=[(21.7,228.4,8.7,23),(75.2,246.1,14,23.1)]
    item('car-wheels-sport','WHEELS')
    for x,y,rx,ry in hubs:
        oval('Sport alloy dark recess',x,y,rx*1.08,ry*.97,'255D7E',-91,3)
        ring('Blue sport rim',x,y,rx*.98,ry*.89,1.8,'58BDEC',-96)
        for k in range(5):
            a=k*math.tau/5
            tube('Sculpted sport wheel spoke',[(x,y),(x+rx*.8*math.cos(a),y+ry*.77*math.sin(a))],1.7,'76CEF2',-96)
        oval('Sport central hub',x,y,rx*.28,ry*.23,'B2E5F7',-99,2)
    item('car-roof-taxi')
    plate('Rounded trapezoid taxi sign',[(71,158),(77,138),(119,138),(126,158)],GOLD,-24,7,3)
    box('Taxi sign dark foot',99,161,59,5,'52616D',-24,6,2)
    for row in range(2):
        for col in range(7):
            if (row+col)%2==0:box('Taxi checker marking',81+col*5.7,144+row*5.5,5.5,5.4,'34323A',-32,.35,.3)
    item('car-front-bull-bar')
    tube('Continuous steel bumper bar',[(111,270),(113,257),(166,257),(221,253),(224,267)],3.2,'C9D7DC',-87,angular=True)
    for x,y in [(121,263),(214,260)]:
        oval('Steel front lamp housing',x,y,9,10,'7D8B97',-91,5)
        oval('Warm front lamp lens',x,y,7,8,GOLD,-97,2.5)
        oval('Front lamp gleam',x-2,y-3,2,2,CREAM,-100,.8)


def robot():
    item('robot-neck-cape',layer='back')
    pillow('Continuous raspberry cape backing',[(83,147),(120,157),(157,147),(207,252),(189,267),(121,260),(51,267),(33,252)],'D93650',30,5)
    for side in (-1,1):
        outline=[(120+side*29,151),(120+side*46,168),(120+side*82,251),
                 (120+side*72,264),(120+side*47,251),(120+side*35,206)]
        pillow('Gold lining inside cape fold',outline,'F2BE39',12,5)
        front=[(120+side*29,149),(120+side*45,164),(120+side*74,229),
               (120+side*81,253),(120+side*66,242),(120+side*48,214),(120+side*34,181)]
        pillow('Flowing raspberry cape side',front,'EA405B',1,7)
        tube('Cape folded hem',front[2:5],1.3,'F86D78',-5)
    item('robot-neck-cape')
    for side in (-1,1):
        pillow('Broad folded cape collar',[(120,154),(120+side*18,151),(120+side*36,143),(120+side*39,152),(120+side*18,164),(120,161)],'EA405B',-48,3.5)
    oval('Golden cape clasp',120,159,7,7,GOLD,-58,3)


def refine(kind):
    """Replace only reviewed accessories, retaining the original scene and rig."""
    scene=bpy.data.scenes['Reference accessories '+kind]
    bpy.context.window.scene=scene
    if kind=='robot':
        robot_module=importlib.import_module('render_robot_accessories')
        helper=importlib.import_module('render_bunny_layers')
        helper.point=robot_module.point
    else:
        # Reset the shared bunny coordinate helper after a robot fitting session.
        helper=importlib.reload(importlib.import_module('render_'+kind+'_layers'))
        if kind in ('cat','dog'):
            geometry=importlib.import_module('render_dinosaur_layers')
            helper.mesh_form=geometry.mesh_form; helper.cushion=geometry.cushion
    art.HELP=helper; art.SCENE=scene; art.MATS={}
    art.SPECS=json.loads(scene['accessory_specs'])
    for obj in list(scene.objects):
        if obj.get('accessory') in CHANGED[kind]: bpy.data.objects.remove(obj,do_unlink=True)
    for key in CHANGED[kind]:art.SPECS[key]={}
    globals()[kind]()
    scene['accessory_specs']=json.dumps(art.SPECS)
    scene['refinement_reference']='Downloads/clocklings-reference, reviewed 13 November 2026'
    scene.cycles.samples=64
    if kind=='robot':importlib.import_module('render_robot_accessories').preview(CHANGED[kind][0])
    else:art.preview(scene,CHANGED[kind][0])
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/blender'/f'{kind}_accessories.blend'),compress=True)
    print('REFINED',kind,len(CHANGED[kind]),flush=True)


def render(kind):
    # Keep every disposable render in this task's owned directory.
    art.render(kind,CHANGED[kind],scratch=SCRATCH/'render')


def gallery(kind):
    directory=SCRATCH/'gallery'
    directory.mkdir(parents=True,exist_ok=True)
    if kind=='robot':
        module=importlib.import_module('render_robot_accessories')
        module.REPORT=directory
        module.gallery()
    else:
        art.gallery(kind,directory=directory)


if __name__=='__main__':
    import sys
    args=sys.argv[sys.argv.index('--')+1:]
    if '--gallery' in args:
        gallery(args[0])
    else:
        render(args[0])
