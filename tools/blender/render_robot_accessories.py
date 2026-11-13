"""Build the owner's seven robot reference sheets as editable Blender geometry.

Call build() through Blender MCP, then render() and gallery(). The accessory
atlas uses the existing 240 by 300 robot grid and animation attachment points.
"""
from pathlib import Path
import json
import math
import sys

import bpy
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import render_bunny_layers as geometry
import render_reference_accessories as art
from render_reference_accessories import item, oval, tube, pillow, plate, box, ring, star
from reference_finish import polish_robot

ROOT = Path(__file__).resolve().parents[2]
SCRATCH = ROOT / '.scratch/robot-accessories'
REPORT = ROOT / 'reports/robot-reference-accessories'
SCALE = 5.91 / 300
TEAL = '285A60'
RED = 'E9364E'
GOLD = 'FFC32D'
CREAM = 'FFF0D6'
BLUE = '438FCB'


def point(x, y, depth):
    return ((x - 120) * SCALE, depth / 50, 2.56 + (150 - y) * SCALE)


def ribbon(name, points, width, color, depth=-64):
    """A broad softly beveled ribbon with a rectangular fabric cross-section."""
    left = [(x-width/2, y) for x, y in points]
    right = [(x+width/2, y) for x, y in reversed(points)]
    return pillow(name, left+right, color, depth, 2.2)


def headwear():
    item('robot-head-antenna', 'ANTENNA', 'back')
    oval('Antenna broad teal foot',120,45,18,6,TEAL,-12,11)
    oval('Antenna upper step',120,41,12,5,TEAL,-12,8)
    tube('Antenna teal stem',[(120,41),(120,22)],3.6,TEAL,-12)
    oval('Antenna golden ball',120,16,13,13,GOLD,-12,13)

    item('robot-head-propeller', 'HEAD')
    # A real hemispherical cap, with separate blue and yellow fabric panels.
    for panel in range(8):
        vertices=[]
        for row in range(25):
            phi=(row/24)*math.pi/2
            for col in range(13):
                theta=panel*math.tau/8+col/12*math.tau/8
                vertices.append(point(120+69*math.sin(phi)*math.cos(theta),
                                      54-40*math.cos(phi), -9+35*math.sin(phi)*math.sin(theta)))
        faces=[]
        for row in range(24):
            for col in range(12):
                a=row*13+col
                faces.append((a,a+1,a+14,a+13))
        color=GOLD if panel in (5,6) else BLUE
        art.tag(geometry.mesh_form('Propeller cap fabric panel',vertices,faces,art.mat(color),'accessory'))
    tube('Red curved cap brim',[(52,56,-32),(80,50,-47),(120,48,-50),(160,50,-47),(188,56,-32)],5.5,RED)
    tube('Propeller spindle',[(120,14),(120,7)],3.1,TEAL,-9)
    for side in (-1,1):
        oval('Red rounded propeller blade',120+side*24,5,25,6,RED,-9,4,side*5)
    oval('Golden propeller hub',120,5,6,7,GOLD,-12,6)

    item('robot-head-headphones','HEAD')
    arch=[(27,92,-15),(35,47,-15),(65,23,-15),(120,15,-15),(175,23,-15),(205,47,-15),(213,92,-15)]
    tube('Blue padded headband',arch,9,'3468AC')
    tube('Ivory headband lining',[(34,79,-20),(46,44,-20),(76,27,-20),(120,23,-20),(164,27,-20),(194,44,-20),(206,79,-20)],4,CREAM)
    for x in (28,212):
        oval('Soft ivory ear cushion',x,100,14,30,CREAM,-31,17)
        oval('Coral headphone ear cup',x+(-7 if x<120 else 7),100,16,29,'F46C56',-39,16)
        oval('Golden ear cup button',x+(-10 if x<120 else 10),100,4.5,5,GOLD,-56,2.5)

    item('robot-head-chef-hat','HEAD')
    puffs=[]
    for x,y,rx,ry in [(82,26,25,24),(120,13,29,32),(158,26,25,25)]:
        puffs.append(oval('Chef hat soft crown',x,y,rx,ry,CREAM,-12,23))
    from reference_finish import fuse
    art.tag(fuse([o.name for o in puffs],'Chef hat continuous crown',voxel=.025))
    box('Chef hat broad folded band',120,47,94,23,CREAM,-29,13,7)
    for x in (83,97,111,127,143,157):
        tube('Chef hat soft vertical pleat',[(x,39),(x-1,46),(x,55)],.8,'E5D5BA',-43)

    item('robot-head-star-antennas','ANTENNA','back')
    tube('Twin antenna curved base',[(91,45),(120,43),(149,45)],5,TEAL,-13)
    for side in (-1,1):
        oval('Star antenna mounting dome',120+side*22,43,10,5,TEAL,-13,8)
        tube('Outward leaning antenna stalk',[(120+side*22,42),(120+side*29,27),(120+side*35,19)],3.4,TEAL,-13)
        star('Rounded five point golden antenna',120+side*39,8,19,GOLD,-15,side*.12)

    item('robot-head-astronaut','HEAD')
    dome=oval('Transparent astronaut bubble',120,76,104,96,'BDE9FF',-2,58)
    glass=bpy.data.materials.new('Clear bubble with reflective silhouette')
    glass.use_nodes=True
    nodes=glass.node_tree.nodes
    nodes.clear()
    output=nodes.new('ShaderNodeOutputMaterial')
    transparent=nodes.new('ShaderNodeBsdfTransparent')
    gloss=nodes.new('ShaderNodeBsdfPrincipled')
    gloss.inputs['Base Color'].default_value=geometry.rgba('D3EDFF')
    gloss.inputs['Roughness'].default_value=.12
    gloss.inputs['Metallic'].default_value=.25
    weight=nodes.new('ShaderNodeLayerWeight')
    weight.inputs['Blend'].default_value=.15
    mix=nodes.new('ShaderNodeMixShader')
    glass.node_tree.links.new(weight.outputs['Fresnel'],mix.inputs[0])
    glass.node_tree.links.new(transparent.outputs[0],mix.inputs[1])
    glass.node_tree.links.new(gloss.outputs[0],mix.inputs[2])
    glass.node_tree.links.new(mix.outputs[0],output.inputs['Surface'])
    dome.data.materials.clear()
    dome.data.materials.append(glass)
    tube('Pale blue glass silhouette',[(39,144),(17,105),(20,52),(54,0),(120,-18),(186,0),(220,52),(223,105),(201,144)],1.5,'9CCDDC',-5)
    # Long softbox reflections make the transparent bubble readable over the screen.
    pillow('Glass tall softbox reflection',[(45,43),(53,28),(70,14),(77,12),(69,23),(58,39),(50,50)],'F0F8FF',-47,.2)
    pillow('Glass small softbox reflection',[(75,8),(86,2),(101,-3),(105,-2),(95,5),(81,12)],'F0F8FF',-40,.2)
    # Ring lies horizontally around the neck and retains a real central opening.
    collar=[(120+65*math.cos(a),151+6*math.sin(a),30*math.sin(a)) for a in np.linspace(0,math.tau,65)]
    tube('Ivory helmet collar',collar,5,CREAM)
    box('Teal helmet clasp',120,158,17,14,TEAL,-38,4,4)
    oval('Gold collar fastening',120,158,4.4,4.4,GOLD,-43,2)


def neckwear():
    item('robot-neck-bow-tie')
    for side in (-1,1):
        pillow('Folded red bow wing',[(120,155),(120+side*28,141),(120+side*33,145),
            (120+side*32,170),(120+side*25,173),(120,161)],RED,-51,7)
        tube('Bow fabric crease',[(120+side*6,157),(120+side*17,157),(120+side*25,152)],.7,'C92C42',-58)
    outline=[(120+9*math.cos(i*math.tau/6),158+9*math.sin(i*math.tau/6)) for i in range(6)]
    plate('Golden hexagonal bow fastener',outline,GOLD,-61,3,1)
    ring('Inset bolt circle',120,158,4.5,4.5,.65,'CE8B16',-65)

    item('robot-neck-cape',layer='back')
    # A complete flared cloth shell with waved hem and a separate gold inner lining.
    for lining in (False,True):
        vertices=[]
        for row in range(31):
            t=row/30
            for col in range(65):
                a=-math.pi*.56+col/64*math.pi*1.12
                radius=32+55*t
                x=120+radius*math.sin(a)
                depth=12+(19+20*t)*math.cos(a)
                depth+=2*math.cos(a*7)*t
                y=148+112*t+3*math.cos(a*7)*t*t
                vertices.append(point(x,y,depth+(.8 if lining else 0)))
        faces=[]
        for row in range(30):
            for col in range(64):
                i=row*65+col
                faces.append((i,i+1,i+66,i+65))
        art.tag(geometry.mesh_form('Cape gold lining' if lining else 'Red flowing cape',vertices,faces,art.mat(GOLD if lining else RED),'accessory'))
    # Rolled edges expose the gold lining from the front beside the feet.
    for side in (-1,1):
        pillow('Golden lining visible at cape hem',[(120+side*57,220),(120+side*84,254),(120+side*75,263),(120+side*48,257)],GOLD,-2,4)
        pillow('Red folded cape outer edge',[(120+side*31,148),(120+side*47,162),
            (120+side*80,244),(120+side*84,257),(120+side*74,254),(120+side*57,218)],RED,-6,4)
    item('robot-neck-cape')
    tube('Folded scarlet cape collar',[(84,146),(100,153),(120,156),(140,153),(156,146)],5.5,RED,-49)
    oval('Round golden cape clasp',120,157,7,7,GOLD,-59,3)

    item('robot-neck-medal')
    ribbon('Left royal blue medal ribbon',[(91,145),(102,174),(117,198)],9,'315DCA',-52)
    ribbon('Right royal blue medal ribbon',[(149,145),(138,174),(123,198)],9,'315DCA',-53)
    ring('Medal attachment loop',120,199,3,5,1.4,GOLD,-58)
    outline=[]
    for i in range(120):
        a=i*math.tau/120
        r=16+1.4*math.cos(a*12)
        outline.append((120+r*math.cos(a),217+r*math.sin(a)))
    plate('Scalloped gold medal',outline,GOLD,-59,3,1)
    ring('Embossed medal circle',120,217,12,12,.7,'FFE17D',-63)
    star('Raised medal star',120,217,9,'FFDA60',-64)


def eyes():
    for shape,color in [('heart','FF729C'),('star','FFD83B')]:
        item('robot-extra-'+shape+'-eyes','HEAD','screen')
        for x in (95,145):
            if shape=='heart':
                outline=[]
                for t in np.linspace(0,math.tau,97)[:-1]:
                    outline.append((x+13*math.sin(t)**3,91-(13*math.cos(t)-5*math.cos(2*t)-2*math.cos(3*t)-math.cos(4*t))*.85))
            else:
                outline=[(x+(14 if i%2==0 else 6.8)*math.cos(-math.pi/2+i*math.pi/5),
                          91+(14 if i%2==0 else 6.8)*math.sin(-math.pi/2+i*math.pi/5)) for i in range(10)]
            obj=plate('Flat luminous '+shape+' eye',outline,color,-69,.08,.06)
            material=bpy.data.materials.get('Flat screen '+color)
            if material is None:
                material=bpy.data.materials.new('Flat screen '+color)
                material.use_nodes=True
                nodes=material.node_tree.nodes
                nodes.clear()
                output=nodes.new('ShaderNodeOutputMaterial')
                emission=nodes.new('ShaderNodeEmission')
                emission.inputs['Color'].default_value=geometry.rgba(color)
                emission.inputs['Strength'].default_value=1.25
                material.node_tree.links.new(emission.outputs[0],output.inputs['Surface'])
            obj.data.materials.clear()
            obj.data.materials.append(material)


def extras():
    item('robot-extra-wrench','HAND')
    # Contiguous open-ended wrench with the handle sloping down towards the mitten.
    outline=[(185,218),(177,216),(176,208),(191,175),(188,165),(190,155),(198,148),
             (202,148),(195,163),(200,169),(207,168),(216,154),(219,157),
             (219,166),(214,175),(205,180),(191,213)]
    plate('Forged steel open jaw wrench',outline,'A9C3D8',-34,3,1.8)
    tube('Inset wrench handle channel',[(183,209),(188,196),(196,180)],2,'648298',-38)
    for obj in bpy.context.scene.objects:
        if obj.get('accessory')=='robot-extra-wrench':
            from mathutils import Matrix, Vector
            pivot=Vector(point(183,213,-34))
            obj.matrix_world=Matrix.Translation(pivot) @ Matrix.Diagonal((1.3,1,1.3,1)) @ Matrix.Translation(-pivot) @ obj.matrix_world

    item('robot-extra-bubble-pack',layer='back')
    oval('Yellow bubble reservoir',49,192,15,28,GOLD,6,13)
    tube('Bubble pack shoulder loop',[(60,205,10),(66,161,12),(77,152,17),(82,196,22)],4,TEAL)
    item('robot-extra-bubble-pack')
    box('Teal bottle retaining strap',48,196,29,9,TEAL,-12,3,2)
    box('Coral bubble bottle cap',49,166,20,9,'F46C56',-8,6,3)
    tube('Curved coral bubble nozzle',[(49,163),(32,159),(27,147),(26,134)],4,'F46C56',-12)
    ring('Open bubble blower hoop',26,117,16,18,4,'F46C56',-12)
    for x,y,r in [(17,85,4),(26,64,7),(10,39,10)]:
        bubble=oval('Pearlescent floating soap bubble',x,y,r,r,'D1DFF7',-12,r)
        p=next(n for n in bubble.data.materials[0].node_tree.nodes if n.type=='BSDF_PRINCIPLED')
        p.inputs['Metallic'].default_value=.25
        tube('Bubble curved highlight',[(x-r*.5,y),(x-r*.5,y-r*.5),(x,y-r*.7)],.65,CREAM,-12-r)

    item('robot-extra-wind-up-key','HAND')
    oval('Teal key mounting socket',168,190,9,13,TEAL,6,5)
    tube('Gold key spindle',[(169,190),(193,190)],4,GOLD,2)
    # Two true holes form the reference's figure-eight key.
    for y in (175,205):
        ring('Open gold winding key loop',200,y,11,15,4.4,GOLD,0)
    box('Key central bridge',200,190,9,13,GOLD,0,4,2)

    item('robot-extra-pet')
    for x in (191,217):
        oval('Pet teal foot',x,280,9,4,TEAL,-16,7)
    box('Pet blue rounded shell',204,257,46,40,'57B4DE',-10,12,12)
    box('Pet midnight screen',204,257,35,27,'16363F',-23,3,8)
    for x in (195,213):
        oval('Pet mint eye',x,254,2.5,4,'76FFD2',-27,1)
        oval('Pet white eye glint',x-.8,252.5,.7,1,CREAM,-28,.4)
    tube('Pet curved mint smile',[(199,261),(204,264),(209,261)],.8,'76FFD2',-27)
    tube('Pet antenna stalk',[(204,237),(205,224)],1.7,TEAL,-10)
    oval('Pet red antenna ball',205,220,4.5,4.5,RED,-10,4.5)

    item('robot-extra-jetpack',layer='back')
    box('Teal backpack body',120,192,78,70,TEAL,27,17,14)
    for x in (35,205):
        oval('Red rounded rocket tank',x,202,14,38,RED,7,13)
        pillow('Ivory tapered rocket nose',[(x-13,174),(x-10,160),(x,146),(x+10,160),(x+13,174)],CREAM,7,10)
        box('Golden rocket tank band',x,188,30,6,GOLD,-6,3,2)
        box('Teal exhaust nozzle',x,240,23,8,TEAL,7,9,2)
        pillow('Orange tapered exhaust flame',[(x-8,246),(x-9,256),(x,277),(x+9,256),(x+8,246)],'FF9B1A',7,5)
        pillow('Bright golden flame core',[(x-4,247),(x-4,256),(x,266),(x+4,256),(x+4,247)],'FFE74C',0,3)
    item('robot-extra-jetpack')
    for side in (-1,1):
        x=120+side*42
        ribbon('Red padded shoulder harness',[(x-side*6,146),(x,164),(x,206),(x-side*5,218)],11,RED,-43)
        box('Gold harness buckle',x,190,15,11,GOLD,-49,2,2)
        box('Buckle inset',x,190,9,5,'BE7920',-52,.5,1)


def build():
    geometry.point=point
    art.HELP=geometry
    art.SPECS={}
    art.MATS={}
    with bpy.data.libraries.load(str(ROOT/'art/blender/robot.blend'),link=False) as (source,target):
        target.scenes=['Clocklings Robot Studio']
    scene=target.scenes[0]
    scene.name='Reference accessories robot'
    bpy.context.window.scene=scene
    scene.frame_set(1)
    polish_robot(scene)
    excluded=set(bpy.data.objects['Accessory - wind-up key'].children_recursive)
    for obj in scene.objects:
        if obj.type not in ('CAMERA','LIGHT'):
            obj['fitting_reference']=True
            obj['reference_excluded']=obj in excluded or obj.name=='Studio floor'
    headwear()
    neckwear()
    eyes()
    extras()
    geometry.configure(scene)
    scene.cycles.samples=32
    scene['character']='robot'
    scene['reference']='Owner robot sheets 1-7, 13 November 2026. Flat eyes from sheet 7.'
    scene['accessory_specs']=json.dumps(art.SPECS)
    scene['frame']=[-20,-40,260,306]
    scene.camera.location=point(120,133,-900)
    scene.camera.rotation_euler=(math.pi/2,0,0)
    scene.camera.data.type='ORTHO'
    scene.camera.data.sensor_fit='VERTICAL'
    scene.camera.data.ortho_scale=346*SCALE
    scene.render.resolution_x=840
    scene.render.resolution_y=1038
    scene.render.image_settings.color_depth='8'
    scene.render.film_transparent=True
    preview('robot-head-propeller')
    save()
    print('ROBOT_ACCESSORIES_BUILT',len(art.SPECS))
    return scene


def preview(id):
    scene=bpy.data.scenes['Reference accessories robot']
    bpy.context.window.scene=scene
    for obj in scene.objects:
        if obj.type in ('CAMERA','LIGHT'): continue
        visible=(obj.get('fitting_reference') and not obj.get('reference_excluded')) or obj.get('accessory')==id
        if id in ('robot-extra-heart-eyes','robot-extra-star-eyes') and obj.name.startswith(('Eye L','Eye R','Eye sparkle')):
            visible=False
        obj.hide_render=not visible
        obj.hide_set(not visible)


def save():
    scene=bpy.data.scenes['Reference accessories robot']
    bpy.context.window.scene=scene
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/blender/robot_accessories.blend'),compress=True)


def render():
    art.SCRATCH=SCRATCH
    art.render('robot')
    preview('robot-head-propeller')
    save()


def gallery():
    """Check the exact exported sprites against the existing robot atlas."""
    def read(path):
        image=bpy.data.images.load(str(path),check_existing=False)
        pixels=np.array(image.pixels[:]).reshape(image.size[1],image.size[0],4)[::-1].copy()
        bpy.data.images.remove(image)
        return pixels
    base=read(art.OUTPUT/'robot_blender_original.png')
    acc=read(art.OUTPUT/'robot_accessories_blender.png')
    specs=json.loads((ROOT/'art/blender/robot_accessories.json').read_text())
    regions={'head':((30,42,210,148),(0,0)), 'body':((68,136,172,232),(544,0)),
             'legs':((70,202,170,284),(244,322)), 'left':((32,144,88,233),(548,322)),
             'right':((152,144,208,233),(720,322)), 'eyes':((80,75,160,110),(0,322))}
    width,height=280,346
    canvas=np.ones((height*12,width*12,4),dtype=np.float32)
    canvas[:,:,:3]=geometry.rgba('F0EAFB')[:3]
    for index,(id,layers) in enumerate(specs.items()):
        cell=canvas[index//4*height*3:(index//4+1)*height*3,index%4*width*3:(index%4+1)*width*3]
        def over(pixels,x,y):
            x=(x+20)*3; y=(y+40)*3
            h,w=pixels.shape[:2]
            dest=cell[y:y+h,x:x+w]
            alpha=pixels[:,:,3:4]
            dest[:,:,:3]=pixels[:,:,:3]*alpha+dest[:,:,:3]*(1-alpha)
        def part(name):
            (l,t,r,b),(x,y)=regions[name]
            over(base[y:y+(b-t)*3,x:x+(r-l)*3],l,t)
        def accessory(layer,part_name):
            info=layers.get(layer)
            if info and info['part']==part_name:
                x,y=info['source']; l,t,w,h=info['destination']
                over(acc[y:y+h*3,x:x+w*3],l,t)
        accessory('back','BODY')
        part('legs');part('left');accessory('front','HAND');part('right');part('body')
        accessory('front','BODY');accessory('back','ANTENNA');part('head')
        if 'screen' in layers: accessory('screen','HEAD')
        else: part('eyes')
        accessory('front','HEAD')
    REPORT.mkdir(parents=True,exist_ok=True)
    art.save_image(REPORT/'robot-accessories.png',canvas.reshape(height*4,3,width*4,3,4).mean(axis=(1,3)))
    (REPORT/'items.txt').write_text('\n'.join(f'{i+1:02d} {id}' for i,id in enumerate(specs))+'\n')
    print('ROBOT_GALLERY_COMPLETE')


if __name__=='__main__':
    render()
    gallery()
