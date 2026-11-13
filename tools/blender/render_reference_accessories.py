"""Editable, reference-matched accessories for the four characters in 16433.jpg.

Run build(kind) through Blender MCP. Render with render(kind) in Blender, or
--background --python this_file -- bunny (also unicorn, car, doll).
Append --rebuild to reconstruct the editable scene before rendering.
Existing character models and atlases are used unchanged as fitting references.
"""
from pathlib import Path
import importlib
import json
import math
import sys

if __name__ == '__main__':
    sys.modules['render_reference_accessories'] = sys.modules[__name__]

import bpy
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SCRATCH = ROOT / '.scratch/reference-accessories'
OUTPUT = ROOT / 'art/blender/atlases'
sys.path.insert(0, str(Path(__file__).resolve().parent))

PINK = 'F35C98'
BLUE = '63B9EC'
GOLD = 'F8C83D'
CREAM = 'FFF1C9'
WHITE = 'FFFAF0'
GREEN = '75BD66'
RED = 'ED4650'
PURPLE = 'A765CB'
RAINBOW = ['EE546B', 'FFA647', 'FBE052', '6CCB70', '64C7EA', 'A985DE']
SPECS = {}
KIND = ''
ITEM = ''
LAYER = 'front'
PART = 'BODY'
HELP = None
SCENE = None
MATS = {}


def mat(color, metallic=0):
    key = (color, metallic)
    if key not in MATS:
        m = HELP.material('Accessory ' + color, color, 0.3 if metallic else 0.38)
        p = next(n for n in m.node_tree.nodes if n.type == 'BSDF_PRINCIPLED')
        p.inputs['Metallic'].default_value = metallic
        p.inputs['Coat Weight'].default_value = 0.15
        MATS[key] = m
    return MATS[key]


def tag(obj):
    obj['accessory'] = ITEM
    obj['accessory_layer'] = LAYER
    obj['part'] = PART
    obj['layer'] = 'accessory'
    return obj


def item(name, part='BODY', layer='front'):
    global ITEM, PART, LAYER
    ITEM, PART, LAYER = name, part, layer
    SPECS.setdefault(name, {})[layer] = {'part': part}


def oval(name, x, y, rx, ry, color, depth=-70, thickness=5, angle=0):
    obj = tag(HELP.oval(name, x, y, depth, rx, ry, thickness, mat(color), 'accessory'))
    obj.rotation_euler.y = math.radians(angle)
    return obj


def tube(name, pts, r, color, depth=-70, radii=None, angular=False):
    obj=tag(HELP.line(name, [(p[0], p[1], p[2] if len(p) > 2 else depth) for p in pts],
                       r, mat(color), 'accessory', radii))
    if angular:
        for bp in obj.data.splines[0].bezier_points:
            bp.handle_left_type=bp.handle_right_type='VECTOR'
    return obj


def pillow(name, outline, color, depth=-70, thickness=4):
    return tag(HELP.cushion(name, outline, depth, thickness, mat(color), 'accessory'))


def plate(name, outline, color, depth=-70, thickness=3, bevel=1):
    n = len(outline)
    vertices = [HELP.point(x, y, d) for d in (depth-thickness, depth+thickness) for x, y in outline]
    faces = [tuple(reversed(range(n))), tuple(range(n, n*2))]
    faces += [(i, (i+1)%n, (i+1)%n+n, i+n) for i in range(n)]
    obj = tag(HELP.mesh_form(name, vertices, faces, mat(color), 'accessory'))
    mod = obj.modifiers.new('Soft rounded edges', 'BEVEL')
    mod.width = bevel/50
    mod.segments = 3
    mod = obj.modifiers.new('Weighted corner normals', 'WEIGHTED_NORMAL')
    return obj


def box(name, x, y, w, h, color, depth=-70, thickness=4, bevel=2):
    return plate(name, [(x-w/2,y-h/2),(x+w/2,y-h/2),(x+w/2,y+h/2),(x-w/2,y+h/2)],
                 color, depth, thickness, bevel)


def ring(name, x, y, rx, ry, r, color, depth=-70):
    pts = [(x+rx*math.cos(i*math.tau/32), y+ry*math.sin(i*math.tau/32), depth)
           for i in range(33)]
    return tube(name, pts, r, color)


def star(name, x, y, r, color=GOLD, depth=-80, angle=0):
    outline = [(x+(r if i%2==0 else r*.48)*math.cos(-math.pi/2+i*math.pi/5+angle),
                y+(r if i%2==0 else r*.48)*math.sin(-math.pi/2+i*math.pi/5+angle)) for i in range(10)]
    return plate(name, outline, color, depth, r*.19, r*.12)


def bow(x, y, scale, color, tails=True, depth=-75, angle=0):
    def pts(outline):
        a=math.radians(angle)
        return [(x+scale*(px*math.cos(a)-py*math.sin(a)), y+scale*(px*math.sin(a)+py*math.cos(a))) for px,py in outline]
    if tails:
        for side in (-1,1):
            pillow('Satin ribbon tail',pts([(side*2,3),(side*11,10),(side*17,36),(side*8,32),(side*3,35)]),color,depth+2,3*scale)
    for side in (-1,1):
        pillow('Folded satin bow loop',pts([(0,0),(side*15,-11),(side*30,-12),(side*31,5),(side*20,12),(side*5,5)]),color,depth,5*scale)
        tube('Soft ribbon fold',[(px,py,depth-5*scale) for px,py in pts([(side*5,1),(side*16,0),(side*24,-4)])],.7*scale,color)
    oval('Rounded bow knot',x,y,6*scale,7*scale,color,depth-5*scale,4*scale,angle)


def flower(x,y,r,color=PINK,depth=-80,petals=5):
    for k in range(petals):
        a=k*math.tau/petals-math.pi/2
        oval('Rounded flower petal',x+math.cos(a)*r*.62,y+math.sin(a)*r*.62,r*.48,r*.58,color,
             depth,2.5,math.degrees(a)+90)
    oval('Golden flower center',x,y,r*.34,r*.34,GOLD,depth-3,2.2)


def leaf(x,y,w,h,angle=0,depth=-72):
    return oval('Soft green leaf',x,y,w,h,GREEN,depth,1.8,angle)


def chain(points,color=GOLD,r=2,depth=-72):
    tube('Neck chain',points,r,color,depth)


def heart(x,y,r,color=RED,depth=-80):
    return pillow('Heart pendant',[(x,y-r*.45),(x-r*.6,y-r*.9),(x-r,y-r*.45),
        (x-r*.75,y+r*.3),(x,y+r),(x+r*.75,y+r*.3),(x+r,y-r*.45),(x+r*.6,y-r*.9)],color,depth,r*.34)


def tiara(x,y,w,h,depth=-70):
    tube('Gold tiara curved band',[(x-w/2,y+3),(x,y-2),(x+w/2,y+3)],2.5,GOLD,depth)
    for dx,hh in [(-w*.3,h*.65),(0,h),(w*.3,h*.65)]:
        pts=[(x+dx-w*.13,y),(x+dx-w*.1,y-hh*.55),(x+dx,y-hh),
             (x+dx+w*.1,y-hh*.55),(x+dx+w*.13,y)]
        tube('Gold tiara pointed arch',pts,1.9,GOLD,depth+1)
        oval('Rose tiara jewel',x+dx,y-hh*.64,3.1,4.2,PINK,depth-3,1.8)


def bunny():
    item('bunny-neck-ribbon')
    chain([(93,174),(120,184),(147,174)],BLUE,3,-63)
    bow(120,187,.9,BLUE)
    item('bunny-extra-carrot','HAND')
    pillow('Tapered orange carrot',[(110,219),(131,219),(130,238),(120,266),(110,246)],'F78B25',-62,8)
    for x,y,a in [(116,205,-25),(122,200,15),(129,207,40)]: leaf(x,y,3,13,a,-62)
    for x,y in [(114,230),(124,239),(118,249)]: tube('Carrot crease',[(x-3,y),(x+3,y+1)],.55,'CF641C',-70)
    item('bunny-head-ear-bow','HEAD')
    bow(82,57,.8,PINK,False,-66,-22)
    item('bunny-neck-daisies')
    # The new reference has two rows of ivory pearls rather than large daisies.
    for row in range(2):
        for k in range(10-row):
            t=k/(9-row)
            oval('Ivory necklace pearl',88+64*t,174+row*9+12*math.sin(t*math.pi),4.8,4.4,WHITE,-65,4.3)
    item('bunny-extra-basket')
    pillow('Wicker basket',[(166,244),(210,244),(208,271),(198,278),(177,277),(168,268)],'DFAD39',-65,10)
    for y in range(248,275,6): tube('Woven horizontal wicker',[(168,y),(189,y+3),(209,y)],1.4,'F5CA64',-76)
    for x in range(171,208,6): tube('Woven vertical wicker',[(x,246),(x+1,260),(x,274)],1,'B98A28',-76)
    tube('Arched wicker handle',[(174,245),(175,224),(188,217),(201,224),(202,245)],3,GOLD,-62)
    for x,y,c in [(178,240,'F6E5C1'),(190,237,'AFDDEA'),(201,241,'F5BED6')]: oval('Pastel egg',x,y,6,9,c,-80,5)
    tube('Thick wicker rim',[(165,244),(188,247),(212,244)],2.6,GOLD,-85)
    item('bunny-head-sun-hat','HEAD')
    oval('Wide golden sunhat brim',120,51,75,13,GOLD,-60,20)
    pillow('Domed sunhat crown',[(80,49),(82,21),(97,6),(120,2),(141,9),(157,25),(158,48)],'FBDD55',-56,24)
    tube('Pink hat band',[(82,40),(120,47),(158,40)],4.5,PINK,-79)
    flower(151,47,13,PINK,-86)
    item('bunny-neck-locket')
    chain([(83,174),(101,191),(121,200),(145,181),(154,172)],GOLD,2.8)
    ring('Pendant bail',121,201,3,4,1.2,GOLD,-78)
    heart(121,215,14,RED,-80)
    item('bunny-extra-watering-can')
    # The sheet's eighth bunny has no visible item; retain its existing usable slot.
    oval('Blue watering can body',182,250,18,20,BLUE,-62,13)
    ring('Watering can loop handle',204,244,10,13,2.8,BLUE,-62)
    tube('Upturned watering spout',[(168,255),(154,243),(150,235)],4,BLUE,-62)
    oval('Watering rose',148,232,8,4,'A6DCF5',-62,5,-30)
    item('bunny-head-top-hat','HEAD')
    oval('Black top hat brim',120,44,59,12,'27242B',-64,19)
    box('Black top hat crown',120,9,68,59,'332D32',-56,21,7)
    oval('Silk hat top',120,-20,34,8,'403940',-56,22)
    box('Red satin hat ribbon',120,31,69,13,RED,-78,2,2)
    for x,y,r in [(63,7,8),(181,15,7)]: star('Magician golden sparkle',x,y,r,GOLD,-64)
    item('bunny-neck-bow-tie')
    bow(120,188,.9,RED,False)
    for x,y in [(99,183),(102,194),(110,188),(135,185),(143,192),(140,180)]: oval('White bow polka dot',x,y,1.9,1.9,WHITE,-82,0.7)
    item('bunny-extra-bubbles')
    tube('Golden bubble wand handle',[(145,267),(182,221)],3.2,GOLD,-67)
    ring('Bubble wand round hoop',188,212,13,13,3,GOLD,-67)
    for x,y,r in [(213,203,7),(226,225,9),(216,250,6)]:
        oval('Pearlescent soap bubble',x,y,r,r,'C2E6F6',-68,3)
        ring('Blue bubble rim',x,y,r-.7,r-.7,.7,BLUE,-72)
        tube('Bubble highlight',[(x-r*.45,y),(x-r*.55,y-r*.4),(x,y-r*.65)],1.2,WHITE,-73)
    item('bunny-head-flower-crown','HEAD')
    tube('Flower crown foliage',[(65,66),(85,52),(120,46),(153,51),(174,65)],3,GREEN,-60)
    for x,y,r,c in [(70,62,11,'F9A447'),(91,52,12,GOLD),(121,46,14,PINK),(150,52,11,'A6D86D'),(171,62,10,GOLD)]: flower(x,y,r,c,-72)
    item('bunny-extra-bouquet','HAND')
    tube('Pink magic wand handle',[(116,274),(127,211)],3,PINK,-61)
    star('Golden magic wand star',129,198,21,GOLD,-66)
    bow(121,235,.25,PINK,False,-71)
    item('bunny-neck-cape',layer='back')
    pillow('Blue star cape',[(86,174),(120,180),(156,174),(207,276),(185,286),(120,279),(53,285),(35,277)],'3286CC',15,5)
    for x,y,r in [(66,235,5),(50,267,5),(177,237,6),(190,270,5),(72,266,4)]: star('Cape golden star',x,y,r,GOLD,7)
    item('bunny-neck-cape')
    tube('Sky blue cape collar',[(85,173),(120,186),(155,173)],6,BLUE,-64)
    star('Golden cape clasp',120,188,11,GOLD,-79)
    item('bunny-head-tiara','HEAD')
    tiara(120,54,74,39,-65)
    item('bunny-extra-wings',layer='back')
    for side in (-1,1):
        for y,rx,ry,a in [(214,42,22,-35),(246,39,16,-17),(269,26,10,0)]:
            oval('Rounded blue fairy wing',120+side*(58 if y<260 else 67),y,rx,ry,BLUE,25,8,side*a)


def unicorn():
    item('unicorn-neck-lei')
    for x,y,c in [(85,177,GOLD),(97,186,GOLD),(111,188,PINK),(126,179,GOLD)]: flower(x,y,7,c,-55)
    item('unicorn-head-horn-rings','HEAD')
    for x,y,rx,c in [(79,39,12,PINK),(79,27,9,'A7D6F4'),(79,15,6,PINK)]:
        oval('Pastel horn ring',x,y,rx,3.5,c,-20,7)
    item('unicorn-extra-cloud')
    pieces=[box('Soft ivory cloud cushion',124,285,128,19,WHITE,-38,10,7)]
    for x,y,rx in [(67,281,15),(89,278,18),(115,280,20),(142,278,18),(169,281,20)]:
        pieces.append(oval('Cloud scallop',x,y,rx,13,WHITE,-43,11))
    from reference_finish import fuse
    tag(fuse([o.name for o in pieces],'Continuous soft cloud cushion',voxel=.018))
    item('unicorn-neck-stars')
    chain([(84,168),(86,184),(102,195),(124,177)],GOLD,2,-57)
    star('Large gold star necklace',101,193,18,GOLD,-65)
    item('unicorn-head-flower-crown','HEAD')
    tube('Flower crown band',[(30,84),(50,68),(83,64),(114,68),(143,80)],2.5,GREEN,-43)
    for x,y,r,c in [(29,83,11,PINK),(49,71,13,PINK),(80,69,12,GOLD),(108,72,14,PINK),(136,84,11,'B9D954')]: flower(x,y,r,c,-51)
    item('unicorn-extra-balloon')
    tube('Golden star wand stem',[(80,267),(29,198)],2.7,'CB9634',-47)
    star('Golden star wand',26,187,20,GOLD,-50)
    item('unicorn-neck-scarf')
    pillow('Pale blue triangular neckerchief',[(80,168),(99,179),(130,169),(105,214),(88,190)],'A8D9F1',-54,3.5)
    tube('Folded scarf upper seam',[(82,171),(102,183),(129,172)],2.5,'DBF0F5',-59)
    star('Tiny scarf star',101,191,6,GOLD,-61)
    item('unicorn-head-ribbons','HEAD')
    bow(56,72,.36,'F6A0C4',True,-49,-15)
    for x,y in [(141,135),(143,147),(139,159)]: oval('White mane bead',x,y,4.5,4.5,WHITE,-37,4)
    item('unicorn-extra-boots')
    for x,y,w,h in [(82,265,26,23),(136,259,25,22),(106.5,271,29,25),(167,266,29,24)]:
        box('Blue ankle boot',x,y,w,h,BLUE,-42,10,5)
        tube('Cream boot cuff',[(x-w/2,y-h/2),(x,y-h/2+2),(x+w/2,y-h/2)],2.7,CREAM,-53)
        tube('Cream boot sole',[(x-w/2,y+h/2),(x,y+h/2+1),(x+w/2,y+h/2)],2.3,CREAM,-53)
        star('Boot sparkle',x,y,5,GOLD,-55)
    item('unicorn-neck-rainbow-scarf')
    box('Knitted striped scarf body',100,212,25,61,GOLD,-51,4,3)
    for i,c in enumerate([PINK,GOLD,BLUE,GREEN,GOLD,PINK,BLUE,GREEN]):
        box('Rainbow knitted stripe',100,184+i*7.5,25,7,c,-56,1.2,1)
    for x in [90,96,102,108]: tube('Scarf fringe',[(x,242),(x-1,252)],1.5,GOLD,-54)
    item('unicorn-head-butterfly','HEAD')
    for side in (-1,1):
        oval('Butterfly upper wing',135+side*8,69,9,12,PINK,-33,3,side*-25)
        oval('Butterfly lower wing',135+side*7,80,7,6,'F69AC5',-34,3,side*20)
        oval('Butterfly wing white dot',135+side*9,67,2,2,WHITE,-38,1)
    oval('Butterfly center',135,75,2.5,10,'E966AD',-39,3)
    item('unicorn-extra-rainbow',layer='back')
    for i,c in enumerate(RAINBOW):
        r=98-i*10
        tube('Rounded rainbow band',[(124+r*math.cos(t),279-r*math.sin(t),28+i*.1) for t in np.linspace(0,math.pi,40)],5.6,c)
    item('unicorn-neck-moon')
    chain([(81,170),(87,186),(103,199),(122,177)],GOLD,2.1,-58)
    tube('Golden crescent pendant',[(106,198),(95,200),(92,209),(96,218),(105,218),(111,212)],3.8,GOLD,-65,
         [0.25,0.8,1,1,.7,.2])
    item('unicorn-head-crown','HEAD')
    tube('Golden circlet base',[(45,80),(79,73),(116,80)],2.4,GOLD,-49)
    tube('Three open crown points',[(47,78),(43,50),(66,65),(79,43),(93,65),(116,51),(112,79)],2.5,GOLD,-47,angular=True)
    tube('Crossed diadem gold band',[(47,78),(79,60),(112,79)],1.7,GOLD,-50,angular=True)
    for x,y in [(43,50),(79,43),(116,51)]: oval('Crown gold tip',x,y,3,3,GOLD,-47,3)
    oval('Pink crown jewel',79,66,4.5,6,PINK,-52,2.5)
    item('unicorn-neck-cape',layer='back')
    outline=[(113,169),(160,171),(183,204),(195,265),(168,270),(146,261),(132,221)]
    pillow('Purple royal cape',outline,'67358D',28,6)
    tube('Royal cape gold hem',[(148,261),(168,268),(191,264)],3,GOLD,18)
    item('unicorn-neck-cape')
    bow(101,185,.45,GOLD,False,-53)
    item('unicorn-extra-wings')
    for x,y,rx,ry,a in [(169,149,44,10,-43),(175,162,40,10,-33),(175,176,33,10,-22),(169,187,24,9,-10)]:
        oval('Layered blue flight feather',x,y,rx,ry,'7CC6F2',-34,6,a)
    oval('Soft wing shoulder',143,187,18,14,BLUE,-41,7,25)


def car():
    item('car-front-plate')
    box('Yellow number plate',169,268,62,20,GOLD,-88,2.5,3)
    data=bpy.data.curves.new('Raised plate digits','FONT')
    data.body='012'; data.align_x='CENTER'; data.size=.30; data.extrude=.003
    obj=bpy.data.objects.new('012 plate lettering',data); SCENE.collection.objects.link(obj)
    obj.location=HELP.point(169,274,-92); obj.rotation_euler=(math.pi/2,0,0)
    obj.data.materials.append(mat('35302E')); tag(obj)
    for x in (142,196): oval('Plate screw',x,268,1.2,1.2,'8E772C',-93,1)
    hubs=[(21.7,228.4,8.7,23),(75.2,246.1,14,23.1)]
    item('car-wheels-sport','WHEELS')
    for x,y,rx,ry in hubs:
        oval('Blue sport wheel',x,y,rx*1.2,ry*1.03,'399ED1',-90,4)
        ring('Sport rim rounded edge',x,y,rx*1.05,ry*.9,1.3,BLUE,-95)
        oval('Sport hub center',x,y,rx*.5,ry*.52,'55BCE9',-96,2)
    item('car-roof-suitcase')
    box('Red rounded roof suitcase',99,137,70,34,RED,-26,15,5)
    box('Suitcase lid seam',99,137,71,2,'C52B3C',-42,1,1)
    for x in (77,120): box('Ivory luggage strap',x,136,6,35,CREAM,-43,1.8,1)
    tube('Suitcase handle',[(90,119),(90,111),(108,111),(108,119)],2.8,CREAM,-27)
    tube('Roof rack',[(59,157),(97,162),(137,153)],2.4,'637580',-27)
    item('car-front-sunglasses')
    for x,y,w,h,a in [(128,232,48,28,-4),(205,228,39,25,5)]:
        lens=oval('Black rounded sunglasses lens',x,y,w/2,h/2,'272734',-95,4,a)
        ring('Thick sunglasses frame',x,y,w/2,h/2,2.7,'382D32',-98)
        tube('Lens soft glint',[(x-8,y-6),(x+3,y-8)],1,'566072',-100)
    tube('Sunglasses bridge',[(150,230),(170,225),(185,228)],3.2,'302A2C',-97)
    tube('Left glasses arm',[(103,229),(91,222)],2.5,'302A2C',-91)
    item('car-wheels-offroad','WHEELS')
    for x,y,rx,ry in hubs:
        oval('Chunky purple tire',x,y,rx*1.9,ry*1.37,'674187',-78,8)
        for k in range(12):
            a=k*math.tau/12
            oval('Raised tire tread block',x+rx*1.7*math.cos(a),y+ry*1.2*math.sin(a),3,4.2,'8358A5',-87,3,math.degrees(a))
        oval('Offroad cream hub',x,y,rx*.78,ry*.73,CREAM,-90,3)
        for k in range(5):
            a=k*math.tau/5
            oval('Hub recess',x+rx*.45*math.cos(a),y+ry*.46*math.sin(a),rx*.16,ry*.14,'54414E',-94,1)
    item('car-model-race')
    # Thin painted bands follow the hood; they are not raised ornaments.
    body=next(o for o in SCENE.objects if o.name.startswith('Rounded sports body'))
    for x in (141,158):
        tag(HELP.patch('White painted racing stripe',body,[(x,201),(x+9,201),(x+25,262),(x+16,262)],mat(WHITE),'accessory',.2))
    smile=next(o for o in SCENE.objects if o.name.startswith('Friendly curved smile')).copy()
    smile.data=smile.data.copy(); SCENE.collection.objects.link(smile)
    del smile['fitting_reference']; tag(smile)
    smile.hide_set(False)
    for x in (98,114): plate('White roof racing stripe',[(x-8,155),(x+2,156),(x+12,161),(x+1,161)],WHITE,-20,.7,.5)
    for x in (110,126): plate('White windscreen racing stripe',[(x,164),(x+7,165),(x+29,200),(x+20,199)],WHITE,-65,.2,.2)
    item('car-roof-surfboard')
    oval('Yellow surfboard rail',104,151,91,12,GOLD,-19,9)
    oval('Green surfboard deck',104,148,87,9,'71C97B',-27,3)
    tube('Surfboard center stripe',[(36,148),(104,144),(171,147)],2.4,'AFE5A1',-31)
    for x in (73,131): tube('Surfboard roof support',[(x,158),(x,167)],2,'56717E',5)
    item('car-front-bull-bar')
    tube('Steel bull bar',[(112,270),(113,258),(168,261),(222,254),(222,267)],3.5,'C2CDD0',-87)
    for x,y in [(120,263),(215,260)]: oval('Yellow front rally lamp',x,y,7,8,GOLD,-92,4)
    item('car-wheels-flames','WHEELS')
    for x,y,rx,ry in hubs:
        oval('Fiery hub outer red',x,y,rx*1.1,ry*.99,RED,-93,4)
        oval('Fiery hub orange',x,y,rx*.75,ry*.8,'FF9F29',-98,3)
        oval('Fiery hub yellow core',x,y,rx*.42,ry*.55,'FFE651',-102,2.8)
    item('car-roof-flag')
    tube('Checkered flag pole',[(102,159),(108,87)],1.7,'505058',-12)
    plate('White waving flag',[(109,86),(127,83),(151,87),(149,112),(128,108),(107,111)],WHITE,-17,1,.5)
    for row in range(4):
        for col in range(6):
            if (row+col)%2==0:
                x=109+col*6.5; y=87+row*5.7+math.sin(col)*1.7
                box('Black flag check',x+3,y+2.5,6.3,5.5,'302D32',-19,0.4,.1)
    item('car-model-fire-engine')
    tube('Ladder left rail',[(32,228),(57,127)],3,'98A9B7',-72)
    tube('Ladder right rail',[(48,233),(73,132)],3,'B6C4CC',-72)
    for i in range(7):
        y=139+i*13
        x=54-(y-139)*.25
        tube('Steel ladder rung',[(x,y),(x+16,y+5)],2.2,'D4DDE0',-75)
    oval('Fire engine red beacon',99,155,10,8,RED,-35,7)
    item('car-front-plow')
    plate('Yellow snowplow blade',[(116,264),(222,260),(230,288),(110,293)],GOLD,-97,4,2)
    for x in (122,148,174,200): plate('Black diagonal hazard stripe',[(x,264),(x+12,263),(x+1,289),(x-12,290)],'292C31',-102,.5,.3)
    tube('Steel plow lower lip',[(111,294),(230,289)],2.2,'9AA8AE',-102)
    item('car-roof-taxi')
    pillow('Golden taxi roof sign',[(70,159),(75,139),(83,132),(116,134),(127,158)],GOLD,-24,8)
    for row in range(2):
        for col in range(6):
            if (row+col)%2==0: box('Taxi black check',82+col*6,141+row*6,5,5,'323034',-34,0.4,.3)
    item('car-wheels-gold','WHEELS')
    for x,y,rx,ry in hubs:
        oval('Gold disc rim',x,y,rx*1.1,ry,GOLD,-93,5)
        ring('Gold raised rim edge',x,y,rx*.93,ry*.85,1.3,'FFE778',-99)
        oval('Gold wheel hub',x,y,rx*.45,ry*.5,'E7AA25',-100,2)
    item('car-roof-rally-lights')
    tube('Roof rally light rail',[(62,154),(128,154)],3,'7F8E9C',-26)
    for x in (68,86,104,122):
        oval('Rally lamp housing',x,145,9,10,'858894',-28,6)
        oval('Golden rally lamp lens',x,145,7.3,8.2,GOLD,-35,2.7)
        oval('Lamp glass glint',x-2,142,2,2,'FFF4C7',-38,1)
    item('car-model-rocket',layer='back')
    for outline in [[(35,202),(5,148),(9,140),(45,174)],[(69,173),(52,125),(59,120),(89,161)]]:
        pillow('Red rocket tail fin',outline,RED,23,5)
    item('car-model-rocket')
    oval('Steel rocket nozzle',13,224,12,16,'748899',-63,12)
    pillow('Rocket orange flame',[(-1,211),(-15,218),(-7,224),(-19,229),(-1,237),(8,226)],'FF9133',-63,5)
    pillow('Rocket yellow core',[(0,218),(-9,224),(1,231),(7,225)],GOLD,-69,3)
    plate('Door lightning bolt',[(58,209),(48,222),(55,222),(51,234),(67,217),(59,217),(64,208)],GOLD,-77,1,.7)


def doll():
    item('head-rusetti','HEAD')
    bow(134,39,.62,RED,False,-43,-12)
    item('head-kukat','HEAD')
    tube('Flower crown green base',[(44,56),(64,33),(98,24),(133,32),(157,52)],3,GREEN,-28)
    for x,y,r,c in [(46,49,11,PINK),(68,31,12,GOLD),(96,25,12,PINK),(128,30,12,'C78DDE'),(151,46,10,PINK)]: flower(x,y,r,c,-35)
    item('extra-strawberry-bag')
    tube('Golden crossbody bag strap',[(136,147),(112,181),(77,218)],2.3,GOLD,-32)
    heart(69,224,19,RED,-40)
    for x,y,a in [(56,211,-35),(65,208,-15),(74,208,20),(83,212,40)]: leaf(x,y,4,7,a,-49)
    for x,y in [(59,220),(69,218),(79,221),(64,229),(75,230),(69,238)]: oval('Cream strawberry seed',x,y,1.5,2.3,CREAM,-48,0.8)
    item('head-kruunu','HEAD')
    plate('Gold three point crown',[(75,40),(75,10),(87,26),(99,3),(112,26),(125,10),(125,40)],GOLD,-22,6,2)
    tube('Rounded gold crown band',[(76,39),(100,42),(124,39)],3.2,GOLD,-29)
    for x,y in [(76,10),(99,3),(125,10)]: oval('Crown ball tip',x,y,3.5,3.5,GOLD,-24,3.5)
    for x,y,c in [(85,33,PINK),(100,30,'61BCDF'),(115,33,PINK)]: oval('Crown colored gemstone',x,y,2.5,3,c,-30,1.5)
    item('extra-siivet',layer='back')
    for side in (-1,1):
        oval('Lavender upper butterfly wing',100+side*60,181,30,46,'B48AE0',20,7,side*-25)
        oval('Lavender lower butterfly wing',100+side*54,222,23,24,'CAA1EB',20,6,side*35)
        tube('Butterfly raised wing vein',[(100+side*35,211),(100+side*64,184),(100+side*71,155)],1.5,'D7B9F1',11)
    item('shoes-roller-skates','WHEELS')
    for x in (76.7,120.9):
        box('Teal skate ankle boot',x,292,34,32,'48C7C1',-9,12,8)
        oval('Rounded teal skate toe',x,306,20,11,'55CBC6',-14,14)
        tube('Gold skate ankle strap',[(x-14,288),(x,291),(x+14,288)],3,GOLD,-23)
        box('Golden skate chassis',x,317,42,6,GOLD,-13,9,2)
        for dx in (-14,14):
            oval('Golden roller wheel',x+dx,322,7,8,GOLD,-24,5)
            oval('Turquoise roller hub',x+dx,322,2.5,3,'3CA8AB',-30,1.5)


def build(kind):
    global KIND, HELP, SCENE, SPECS, MATS
    KIND=kind; SPECS={}; MATS={}
    HELP=importlib.import_module('render_'+kind+'_layers')
    if kind in ('cat','dog'):
        geometry=importlib.import_module('render_dinosaur_layers')
        HELP.mesh_form=geometry.mesh_form
        HELP.cushion=geometry.cushion
    name='Reference accessories '+kind
    old=bpy.data.scenes.get(name)
    if old:
        for obj in list(old.objects): bpy.data.objects.remove(obj,do_unlink=True)
        bpy.data.scenes.remove(old)
    with bpy.data.libraries.load(str(ROOT/'art/blender'/f'{kind}.blend'),link=False) as (source,target):
        source_name={'cat':'kitten','dog':'puppy'}.get(kind,kind)
        target.scenes=[next(n for n in source.scenes if n.lower()=='clocklings '+source_name)]
    SCENE=target.scenes[0]; SCENE.name=name
    bpy.context.window.scene=SCENE
    for obj in SCENE.objects:
        if obj.type not in ('CAMERA','LIGHT'):
            obj.hide_render=True
            obj.hide_set(True)
            obj['fitting_reference']=True
    if kind in ('cat','dog','dragon','dinosaur'):
        getattr(importlib.import_module('render_polished_accessories'),kind)()
    else:
        globals()[kind]()
    HELP.configure(SCENE)
    SCENE.cycles.samples=32
    SCENE.render.threads=8
    SCENE['reference']='16433.jpg supplied by owner, 13 November 2026'
    if kind in ('cat','dog','dragon','dinosaur'):
        SCENE['reference']='Clocklings polished 3D accessory sheet.png supplied by owner'
    SCENE['accessory_specs']=json.dumps(SPECS)
    SCENE['character']=kind
    # Extend the fixed-view camera upwards for the magician hat and sideways for rockets.
    if kind in ('cat','dog','dragon','dinosaur'): frame=(-12,-84,252,310)
    elif kind=='doll': frame=(-4,-8,204,344)
    elif kind=='bunny': frame=(-12,-60,252,300)
    elif kind=='car': frame=(-24,80,248,304)
    else: frame=(0,-20,240,304)
    SCENE['frame']=list(frame)
    x0,y0,x1,y1=frame
    SCENE.camera.location=HELP.point((x0+x1)/2,(y0+y1)/2,-900)
    SCENE.camera.data.ortho_scale=(y1-y0)/50
    SCENE.render.resolution_x=(x1-x0)*3
    SCENE.render.resolution_y=(y1-y0)*3
    SCENE.camera.rotation_euler=(math.pi/2,0,0)
    # Keep all accessory choices editable in one scene; only one is shown at a time.
    preview(SCENE,next(iter(SPECS)))
    path=ROOT/'art/blender'/f'{kind}_accessories.blend'
    bpy.data.libraries.write(str(path),{SCENE},fake_user=True,compress=True)
    print('ACCESSORIES_BUILT',kind,len(SPECS),len([o for o in SCENE.objects if o.get('accessory')]),flush=True)
    return SCENE


def preview(scene,id):
    for obj in scene.objects:
        if obj.type in ('CAMERA','LIGHT'): continue
        if obj.get('fitting_reference'):
            visible=scene['character']!='doll' or obj.get('layer') in importlib.import_module('render_doll_layers').DEFAULT
        else: visible=obj.get('accessory')==id
        obj.hide_render=not visible
        obj.hide_set(not visible)


def save_image(path,pixels):
    h,w=pixels.shape[:2]
    img=bpy.data.images.new(path.stem,width=w,height=h,alpha=True)
    img.colorspace_settings.name='sRGB'
    img.pixels.foreach_set(pixels[::-1].astype(np.float32).reshape(-1))
    img.filepath_raw=str(path); img.file_format='PNG'; img.save()
    bpy.data.images.remove(img)


def render(kind,only=None,scratch=None):
    scene=bpy.data.scenes.get('Reference accessories '+kind)
    if scene is None:
        bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/blender'/f'{kind}_accessories.blend'))
        scene=bpy.data.scenes['Reference accessories '+kind]
    bpy.context.window.scene=scene
    if bpy.app.background:
        for other in list(bpy.data.scenes):
            if other != scene and not other.objects: bpy.data.scenes.remove(other)
        bpy.context.preferences.filepaths.save_version=0
        bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/blender'/f'{kind}_accessories.blend'))
    specs=json.loads(scene['accessory_specs'])
    if scratch is None:
        scratch=ROOT/'.scratch/polished-accessories/render' if kind in ('cat','dog','dragon','dinosaur') else SCRATCH
    directory=scratch/kind; directory.mkdir(parents=True,exist_ok=True)
    frame=scene['frame']; x0,y0,x1,y1=frame
    chunks=[]
    # Reuse untouched sprites directly, even when disposable render files were removed.
    previous={}
    previous_pixels=None
    if only is not None:
        previous=json.loads((ROOT/'art/blender'/f'{kind}_accessories.json').read_text())
        old_image=bpy.data.images.load(str(OUTPUT/f'{kind}_accessories_blender.png'),check_existing=False)
        previous_pixels=np.array(old_image.pixels[:],dtype=np.float32).reshape(old_image.size[1],old_image.size[0],4)[::-1].copy()
        bpy.data.images.remove(old_image)
    for id,layers in specs.items():
        for layer,info in layers.items():
            if only is not None and id not in only:
                old=previous[id][layer]
                info['destination']=old['destination']
                ax,ay=old['source']; _,_,ww,hh=old['destination']
                chunks.append((id,layer,info,previous_pixels[ay:ay+hh*3,ax:ax+ww*3].copy()))
                continue
            path=directory/f'{id}_{layer}.png'
            if only is None or id in only:
                for obj in scene.objects:
                    if obj.type not in ('CAMERA','LIGHT'):
                        obj.hide_render=not(obj.get('accessory')==id and obj.get('accessory_layer')==layer)
                        obj.hide_set(obj.hide_render)
                scene.render.filepath=str(path)
                bpy.ops.render.render(write_still=True,scene=scene.name)
            image=bpy.data.images.load(str(path),check_existing=False)
            pixels=np.array(image.pixels[:],dtype=np.float32).reshape(image.size[1],image.size[0],4)[::-1].copy()
            bpy.data.images.remove(image)
            yy,xx=np.where(pixels[:,:,3]>.005)
            assert len(xx)>20, f'Empty accessory {id}/{layer}'
            assert xx.min()>0 and yy.min()>0 and xx.max()<pixels.shape[1]-1 and yy.max()<pixels.shape[0]-1, f'Clipped {id}'
            left=max(0,int(xx.min()//3)-2); top=max(0,int(yy.min()//3)-2)
            right=min(x1-x0,int(xx.max()//3)+3); bottom=min(y1-y0,int(yy.max()//3)+3)
            crop=pixels[top*3:bottom*3,left*3:right*3]
            info['destination']=[left+x0,top+y0,right-left,bottom-top]
            chunks.append((id,layer,info,crop))
            print('ACCESSORY_RENDERED',kind,id,layer,flush=True)
    width=1536; xpos=ypos=row_height=0
    for id,layer,info,crop in chunks:
        h,w=crop.shape[:2]
        if xpos+w>width: xpos=0; ypos+=row_height+6; row_height=0
        info['source']=[xpos,ypos]
        xpos+=w+6; row_height=max(row_height,h)
    height=ypos+row_height
    atlas=np.zeros((height,width,4),dtype=np.float32)
    for id,layer,info,crop in chunks:
        x,y=info['source']; h,w=crop.shape[:2]
        atlas[y:y+h,x:x+w]=crop
    save_image(OUTPUT/f'{kind}_accessories_blender.png',atlas)
    (ROOT/'art/blender'/f'{kind}_accessories.json').write_text(json.dumps(specs,indent=2)+'\n')
    print('ACCESSORY_ATLAS_COMPLETE',kind,width,height,flush=True)
    write_regions()
    return specs


def write_regions():
    lines=['// Generated by tools/blender/render_reference_accessories.py. Do not hand-edit.',
           '@file:Suppress("MagicNumber")', '', 'package fi.clocklings.ui.companion', '',
           'import androidx.compose.ui.unit.IntOffset', 'import androidx.compose.ui.unit.IntSize', '']
    for kind in ('bunny','unicorn','car','doll','cat','dog','dragon','dinosaur','robot'):
        path=ROOT/'art/blender'/f'{kind}_accessories.json'
        if not path.exists(): continue
        specs=json.loads(path.read_text())
        lines += [f'internal val {kind.title()}AccessoryRegions: Map<String, List<BlenderAccessoryRegion>> =', '    mapOf(']
        for id,layers in specs.items():
            lines += [f'        "{id}" to', '            listOf(']
            for layer,info in layers.items():
                ax,ay=info['source']; x,y,w,h=info['destination']
                lines += ['                BlenderAccessoryRegion(',f'                    ArtPart.{info["part"]},',
                          f'                    ItemLayer.{"BEHIND" if layer=="back" else "SCREEN" if layer=="screen" else "FRONT"},',
                          f'                    IntOffset({ax}, {ay}),',f'                    IntOffset({x}, {y}),',
                          f'                    IntSize({w}, {h}),','                ),']
            lines += ['            ),']
        lines += ['    )', '']
    path=ROOT/'app/src/main/java/fi/clocklings/ui/companion/BlenderAccessoryRegions.kt'
    path.write_text('\n'.join(lines),encoding='utf-8')


def gallery(kind,directory=None):
    """Composite exactly the exported atlas pieces in the application's resting layer order."""
    helper=importlib.import_module('render_'+kind+'_layers')
    specs=json.loads((ROOT/'art/blender'/f'{kind}_accessories.json').read_text())
    frame=(-24,-60,264,310) if kind=='bunny' else ((-24,80,248,310) if kind=='car' else ((0,0,200,340) if kind=='doll' else (0,-20,240,310)))
    if kind in ('cat','dog','dragon','dinosaur'): frame=(-12,-84,252,310)
    x0,y0,x1,y1=frame; w=x1-x0; h=y1-y0
    def read(path):
        image=bpy.data.images.load(str(path),check_existing=False)
        result=np.array(image.pixels[:],dtype=np.float32).reshape(image.size[1],image.size[0],4)[::-1].copy()
        bpy.data.images.remove(image)
        return result
    acc=read(OUTPUT/f'{kind}_accessories_blender.png')
    colors=['original','blue','green','purple']
    if kind=='dragon': colors=['original','blue','green','orange']
    if kind=='dinosaur': colors=['original','blue','purple','orange']
    if kind=='doll':
        ids=['hair-letit','dress-perus','shoes-tossut','dress-pilkku','head-rusetti','shoes-punaiset','hair-pitkat','dress-tahti',
             'head-kukat','extra-strawberry-bag','shoes-saappaat','hair-kiharat','dress-overalls','head-kruunu','extra-siivet',
             'shoes-roller-skates','hair-sateenkaari','dress-sateenkaari']
        atlases=[read(OUTPUT/'doll_blender.png')]*5
    else:
        ids=list(specs)
        atlases=[read(OUTPUT/f'{kind}_blender_{color}.png') for color in colors]
    rows=math.ceil(len(ids)/4)
    canvas=np.zeros((h*rows*3,w*4*3,4),dtype=np.float32)
    canvas[:]=helper.rgba('FFF4F9')
    for index,id in enumerate(ids):
        cell=canvas[(index//4)*h*3:(index//4+1)*h*3,(index%4)*w*3:(index%4+1)*w*3]
        atlas=atlases[index//4]
        def over(pixels,left,top):
            px=(left-x0)*3; py=(top-y0)*3
            hh,ww=pixels.shape[:2]
            tx=max(0,px); ty=max(0,py); xx=min(w*3,px+ww); yy=min(h*3,py+hh)
            if xx<=tx or yy<=ty:return
            src=pixels[ty-py:yy-py,tx-px:xx-px]
            dest=cell[ty:yy,tx:xx]
            a=src[:,:,3:4]
            dest[:,:,:3]=src[:,:,:3]*a+dest[:,:,:3]*(1-a)
        def part(name,clip=None):
            rect,origin=helper.REGIONS[name]
            l,t,r,b=rect; ax,ay=origin
            pixels=atlas[ay:ay+(b-t)*3,ax:ax+(r-l)*3]
            if clip is not None:
                if b<=clip:return
                pixels=pixels[max(0,(clip-t)*3):]; t=max(t,clip)
            over(pixels,l,t)
        def accessory(layer,part_name=None):
            data=specs.get(id,{}).get(layer)
            if data is None or (part_name and data['part']!=part_name):return
            ax,ay=data['source']; l,t,ww,hh=data['destination']
            over(acc[ay:ay+hh*3,ax:ax+ww*3],l,t)
        accessory('back')
        if kind=='bunny':
            part('body');part('arms');accessory('front','HAND');part('arms',224)
            accessory('front','BODY');part('head');part('ear_left');part('ear_right');part('eyes');accessory('front','HEAD')
        elif kind=='unicorn':
            part('tail');part('body');accessory('front','BODY');part('head');part('eyes');accessory('front','HEAD')
        elif kind=='car':
            part('wheels');accessory('front','WHEELS');part('body');part('eye_left');part('eye_right');accessory('front','BODY')
        elif kind=='cat':
            part('tail');part('body');accessory('front','HAND');part('paws');accessory('front','BODY')
            part('ears');part('head');part('stripes');part('eyes');accessory('front','HEAD')
        elif kind=='dog':
            part('tail');part('body');accessory('front','BODY');part('head');part('eyes')
            part('ear_left');part('ear_right');accessory('front','HEAD')
        elif kind=='dragon':
            if id!='dragon-extra-wings':part('wing_left');part('wing_right')
            part('body');accessory('front','HAND');part('limbs');accessory('front','BODY')
            part('horns');part('head');part('eyes');accessory('front','HEAD')
        elif kind=='dinosaur':
            part('tail');part('body');part('near_arm');part('far_arm')
            if id=='dinosaur-extra-ice-cream':part('fist')
            accessory('front','HAND');accessory('front','BODY')
            part('head');part('eye_near');part('eye_far');accessory('front','HEAD')
            if id=='dinosaur-extra-ice-cream':
                data=specs[id]['front']; ax,ay=data['source']; l,t,ww,hh=data['destination']
                end=max(0,min(hh,181-t));over(acc[ay:ay+end*3,ax:ax+ww*3],l,t)
        else:
            hair={'hair-pitkat':'gold','hair-kiharat':'curly','hair-sateenkaari':'rainbow'}.get(id,'braids')
            dress={'dress-pilkku':'dots','dress-tahti':'stars','dress-overalls':'overalls','dress-sateenkaari':'rainbow'}.get(id,'yellow')
            shoe={'shoes-punaiset':'red','shoes-saappaat':'boots'}.get(id,'white')
            part('hair_'+hair+'_back');part('legs')
            if id=='shoes-roller-skates':accessory('front','WHEELS')
            else:part('shoes_'+shoe)
            for name in ['arm_left','arm_right','neck','dress_'+dress,'head','eye_left','eye_right','hair_'+hair+'_front']:part(name)
            accessory('front','HEAD');accessory('front','BODY')
    # A compact review image; the full-resolution atlases remain in Android resources.
    canvas=canvas.reshape(h*rows,3,w*4,3,4).mean(axis=(1,3))
    if directory is None:
        directory=ROOT/('reports/polished-accessories' if kind in ('cat','dog','dragon','dinosaur') else 'reports/reference-accessories')
    directory.mkdir(parents=True,exist_ok=True)
    save_image(directory/f'{kind}.png',canvas)
    print('ACCESSORY_GALLERY_COMPLETE',kind,flush=True)


if __name__=='__main__':
    arguments=sys.argv[sys.argv.index('--')+1:]
    if '--rebuild' in arguments:
        arguments.remove('--rebuild')
        build(arguments[0])
    render(arguments[0],arguments[1:] or None)
