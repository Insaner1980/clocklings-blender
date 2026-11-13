"""Editable accessories from the owner's polished cat/dog/dragon/dinosaur sheet.

Build with render_reference_accessories.build(kind) through Blender MCP.
Coordinates use the existing character's 240 x 300 orthographic fitting grid.
"""
import math
import render_reference_accessories as art

from render_reference_accessories import (
    item, oval, tube, pillow, plate, box, ring, star, bow, flower, leaf,
    GOLD, WHITE, BLUE, GREEN, RED, PINK,
)

TEAL = '35B8A5'
NAVY = '326BB9'
YELLOW = 'FFD52C'


def cone(name,x,y,r,h,color,depth=-65,tilt=0):
    vertices=[]
    segments=64
    for step in range(17):
        t=step/16
        radius=max(.15,r*(1-t))
        for k in range(segments):
            a=k*math.tau/segments
            vertices.append(art.HELP.point(x+tilt*t+radius*math.cos(a),y-h*t,
                                          depth+radius*.65*math.sin(a)))
    faces=[tuple(reversed(range(segments)))]
    for step in range(16):
        for k in range(segments):
            n=(k+1)%segments; start=step*segments
            faces.append((start+k,start+n,start+segments+n,start+segments+k))
    faces.append(tuple(range(16*segments,17*segments)))
    return art.tag(art.HELP.mesh_form(name,vertices,faces,art.mat(color),'accessory'))


def dots(points, color=WHITE, radius=2, depth=-86):
    for x,y in points:
        oval('Raised round fabric spot',x,y,radius,radius,color,depth,.65)


def scarf(x,y,color,stripe,scale=1):
    # A continuous padded collar with the striped end hanging on the right.
    s=scale
    pillow('Soft hanging scarf',[(x+10*s,y+6*s),(x+33*s,y+4*s),(x+37*s,y+64*s),
        (x+14*s,y+67*s)],color,-72,5*s)
    for dy in (18,35,52):
        box('Horizontal scarf stripe',x+25*s,y+dy*s,24*s,7*s,stripe,-79,1.3*s,1*s)
    for dx in (16,21,26,31,36):
        tube('Knitted scarf fringe',[(x+dx*s,y+62*s),(x+dx*s,y+72*s)],1.2*s,color,-75)
    tube('Padded scarf around neck',[(x-43*s,y),(x-26*s,y+7*s),(x+23*s,y+7*s),(x+43*s,y)],9*s,color,-68)
    for dx in (-32,-10,12,33):
        tube('Vertical scarf stripe',[(x+dx*s,y+1*s),(x+dx*s,y+12*s)],3.2*s,stripe,-78)


def necklace(x,y,width,colors,pearls=11,r=5):
    for k in range(pearls):
        t=k/(pearls-1)
        oval('Rounded necklace bead',x-width/2+t*width,y+15*math.sin(t*math.pi),r,r,
             colors[k%len(colors)],-73,r*.85)


def wreath(x,y,width,colors,r=10):
    tube('Flower crown green stem',[(x-width/2,y+10),(x,y-6),(x+width/2,y+10)],2.5,GREEN,-64)
    for k in range(7):
        t=k/6; xx=x-width/2+t*width; yy=y+10-16*math.sin(t*math.pi)
        leaf(xx-5,yy+4,3,9,-35,-66)
        flower(xx,yy,r,colors[k%len(colors)],-74)


def medal(x,y,color=RED,r=15):
    tube('Left medal ribbon',[(x-30,y-39),(x-14,y-16),(x,y)],4,color,-70)
    tube('Right medal ribbon',[(x+29,y-39),(x+15,y-15),(x,y)],4,color,-70)
    ring('Medal attachment',x,y-2,3,4,1.5,GOLD,-78)
    oval('Gold medal disc',x,y+15,r,r,GOLD,-78,4)
    ring('Medal embossed rim',x,y+15,r*.77,r*.77,1.2,'FFE68B',-83)
    star('Medal embossed star',x,y+15,r*.52,'FFDA59',-84)


def cape(id,x,y,color,trim,stars=False):
    item(id,layer='back')
    for side in (-1,1):
        outline=[(x+side*25,y),(x+side*42,y+13),(x+side*75,y+117),
                 (x+side*47,y+121),(x+side*20,y+110),(x+side*10,y+20)]
        pillow('Flowing royal cape',outline,color,32,5)
        tube('Cape gold hem',outline[1:5],3,trim,24)
        if stars:
            for dx,dy,r in [(43,36,8),(51,68,7),(62,96,9)]:
                star('Golden cape star',x+side*dx,y+dy,r,GOLD,20)
    item(id)
    for side in (-1,1):
        outline=[(x+side*25,y+9),(x+side*43,y+20),(x+side*75,y+117),
                 (x+side*47,y+121),(x+side*25,y+109),(x+side*15,y+25)]
        pillow('Front folded cape panel',outline,color,-62,5)
        tube('Front cape hem',outline[1:5],2.5,trim,-70)
        if stars:
            for dx,dy,r in [(38,40,8),(49,72,7),(61,99,9)]:
                star('Visible golden cape star',x+side*dx,y+dy,r,GOLD,-73)
    tube('Cape folded collar',[(x-36,y+1),(x-20,y+11),(x,y+15),(x+20,y+11),(x+36,y+1)],7,color,-70)
    oval('Round gold cape clasp',x,y+18,9,9,GOLD,-80,4)


def crown(x,y,w=82,h=45,royal=False):
    if royal:
        oval('Red velvet crown cap',x,y-19,w*.43,27,'D92D49',-65,18)
        for dx in (-w*.28,0,w*.28):
            tube('Arched gold crown rib',[(x+dx*1.4,y-5),(x+dx,y-34),(x,y-43)],2.6,GOLD,-82)
        oval('Gold crown finial',x,y-47,5,5,GOLD,-83,4)
    else:
        outline=[(x-w/2,y),(x-w/2-4,y-h*.88),(x-w*.24,y-h*.45),
                 (x,y-h),(x+w*.24,y-h*.45),(x+w/2+4,y-h*.88),(x+w/2,y)]
        plate('Five-point gold crown',outline,GOLD,-68,5,2)
        for dx,dy in [(-w/2-4,-h*.88),(0,-h),(w/2+4,-h*.88)]:
            oval('Gold crown tip',x+dx,y+dy,4,4,GOLD,-70,3)
    tube('Thick crown band',[(x-w/2,y),(x,y+5),(x+w/2,y)],5,GOLD,-82)
    for dx,c in [(-w*.28,BLUE),(0,RED),(w*.28,BLUE)]:
        oval('Crown jewel gold setting',x+dx,y-7,5.5,7,GOLD,-82,2)
        oval('Crown colored jewel',x+dx,y-7,3.5,5,c,-85,2)


def cap(x,y,w,color,emblem=True,visor=7):
    pillow('Rounded baseball cap dome',[(x-w/2,y),(x-w*.45,y-35),(x-w*.23,y-51),
        (x+w*.20,y-51),(x+w*.43,y-33),(x+w/2,y),(x+w*.25,y),(x,y),(x-w*.25,y)],color,-57,24)
    tube('Cap panel seam',[(x,y-48),(x,y-25),(x,y-1)],.8,'9CC8EE' if color==NAVY else 'FF8D8F',-82)
    oval('Curved baseball cap visor',x+visor,y+3,w*.57,10,color,-81,13,-4)
    oval('Top fabric button',x,y-53,4,3,color,-59,3)
    if emblem:star('Cap front star',x,y-23,12,GOLD if color==NAVY else WHITE,-86)


def party_hat(x,y,color,spot,tilt=0):
    cone('Pointed party hat',x,y,28,68,color,-65,tilt)
    tube('Party hat rolled base',[(x-27,y),(x,y+5),(x+27,y)],2.3,spot,-79)
    dots([(x-10,y-11),(x+12,y-13),(x,y-31),(x+tilt*.6,y-49)],spot,3,-79)
    oval('Party hat pompom',x+tilt,y-70,8,8,spot,-65,7)


def sunhat(x,y,w=85):
    oval('Wide curved straw brim',x,y,w,12,YELLOW,-62,24)
    pillow('Round straw hat dome',[(x-44,y-2),(x-39,y-32),(x-23,y-47),
        (x,y-51),(x+26,y-44),(x+42,y-27),(x+44,y-2)],'FFE663',-59,23)
    tube('Pink satin hat band',[(x-44,y-9),(x,y-2),(x+44,y-9)],5,PINK,-82)
    flower(x+44,y-9,12,PINK,-90)
    leaf(x+57,y-4,4,10,60,-85)


def backpack(x,y,color,flap,width=42,height=67):
    box('Rounded backpack body',x,y,width,height,color,-45,16,9)
    box('Backpack fold-over lid',x,y-height*.4,width+4,18,flap,-60,7,5)
    tube('Backpack carry loop',[(x-10,y-height*.54),(x-9,y-height*.7),(x+9,y-height*.7),(x+10,y-height*.54)],2.5,flap,-45)
    box('Front backpack pocket',x,y+height*.18,width*.75,height*.3,flap,-63,4,5)
    for dx in (-width*.27,width*.27):
        box('Gold backpack buckle',x+dx,y,width*.14,13,GOLD,-69,2,2)


def cat():
    item('cat-neck-scarf'); scarf(120,169,RED,WHITE,.95)
    item('cat-head-bow','HEAD'); bow(145,47,1.0,'F04B85',False,-72,-8)
    item('cat-extra-yarn')
    oval('Blue yarn ball',105,255,34,32,'45B9E7',-75,27)
    for angle in (-.65,.6):
        for k in range(-5,6):
            offset=k*4.5
            reach=math.sqrt(30**2-offset**2)
            points=[]
            for j in range(13):
                t=-1+2*j/12
                u=offset+4*(1-t*t); v=t*reach
                dx=u*math.cos(angle)-v*math.sin(angle)
                dy=u*math.sin(angle)+v*math.cos(angle)
                depth=-75-27*math.sqrt(max(.015,1-(dx/34)**2-(dy/32)**2))-.8
                points.append((105+dx,255+dy,depth))
            tube('Individual curved yarn winding',points,.65,'6CC9E8')
    tube('Loose curling yarn',[(127,271),(153,278),(161,286),(181,286),(191,280)],2.5,'45B9E7',-81)
    item('cat-neck-bell-collar')
    tube('Red padded collar',[(75,167),(101,175),(139,175),(165,167)],8,'E93249',-65)
    ring('Bell hanging loop',120,180,4,5,1.8,GOLD,-77)
    oval('Golden spherical bell',120,195,12,13,GOLD,-78,10)
    tube('Bell lower slit',[(113,201),(120,204),(127,201)],1,'8B571E',-89)
    oval('Bell sound hole',120,203,2,2,'755123',-90,1)
    item('cat-head-bobble-hat','HEAD')
    pillow('Blue knitted beanie dome',[(48,78),(53,48),(75,26),(120,20),(164,31),(186,52),
        (191,78),(155,78),(120,78),(85,78)],'50AEE4',-55,26)
    tube('Broad white knitted brim',[(46,78),(79,72),(120,70),(161,72),(193,78)],10,WHITE,-80)
    for x in range(55,191,9):tube('Fine rib on white hat brim',[(x,69+abs(x-120)*.08),(x,80+abs(x-120)*.08)],.45,'DFE9ED',-91)
    oval('Red bobble pompom',120,8,19,19,'F33250',-54,17)
    item('cat-extra-glasses','HEAD')
    for x in (87,154):ring('Thick blue round glasses',x,111,29,29,4.5,'2384CC',-86)
    tube('Glasses nose bridge',[(114,110),(120,106),(126,110)],3,'2384CC',-87)
    tube('Left glasses temple',[(59,110),(41,104)],3,'2384CC',-78)
    tube('Right glasses temple',[(182,110),(199,104)],3,'2384CC',-78)
    for x in (69,136):tube('Glasses rim gleam',[(x,99),(x+5,92),(x+12,89)],1.2,'91DCF8',-91)
    item('cat-neck-bow-tie'); bow(120,180,.8,TEAL,False,-76)
    dots([(101,174),(99,186),(109,182),(136,172),(140,185),(132,183)],YELLOW,2.4,-85)
    item('cat-head-sailor-hat','HEAD')
    pillow('White sailor cap',[(53,73),(50,29),(77,17),(121,20),(168,16),(190,29),
        (187,73),(152,73),(120,73),(88,73)],WHITE,-56,20)
    tube('Navy sailor cap band',[(54,70),(120,75),(187,70)],6,NAVY,-79)
    tube('White sailor cap piping',[(54,60),(120,65),(187,60)],2.1,WHITE,-82)
    ring('Anchor top ring',120,32,4,4,1.7,NAVY,-79)
    tube('Anchor shank',[(120,36),(120,54)],2.3,NAVY,-80)
    tube('Anchor arms',[(106,46),(109,54),(120,58),(131,54),(134,46)],2.1,NAVY,-80)
    tube('Anchor crossbar',[(113,42),(127,42)],1.8,NAVY,-80)
    item('cat-extra-fish-backpack')
    tube('Fish carrying strap',[(57,191),(68,182),(74,195)],3,'258BC4',-38)
    oval('Blue fish body',42,230,20,44,'35BCEB',-67,13,7)
    pillow('Fish tail fin',[(35,267),(21,284),(42,280),(54,286),(50,265)],'269BDB',-66,4)
    pillow('Fish side fin',[(35,235),(14,225),(13,242),(34,246)],'299FDD',-83,3)
    for y in (228,242,255):tube('Fish curved scale detail',[(31,y),(42,y+4),(52,y)],1.3,'8ADCF2',-82)
    oval('Fish white eye',39,204,7,8,WHITE,-80,3)
    oval('Fish black pupil',40,204,3.5,4,'23364A',-84,2)
    oval('Fish eye glint',39,202,1.2,1.4,WHITE,-86,1)
    item('cat-neck-ruffle')
    tube('Pink gathered collar binding',[(71,166),(92,180),(120,185),(149,180),(169,166)],6,'F87CA8',-67)
    for k in range(9):
        a=math.pi*k/8
        x=120-49*math.cos(a); y=165+23*math.sin(a)
        angle=math.radians(30*math.cos(a))
        def fold(points):
            return [(x+dx*math.cos(angle)-dy*math.sin(angle),
                     y+dx*math.sin(angle)+dy*math.cos(angle)) for dx,dy in points]
        pillow('Soft pink scalloped fabric fold',fold([(-11,-4),(11,-4),(13,7),
            (7,16),(0,19),(-8,15),(-13,6)]),'F87CA8',-72,4)
        pillow('Ivory folded ruffle edge',fold([(-11,-10),(11,-10),(12,-1),
            (7,7),(0,9),(-8,6),(-12,-1)]),'FFF5E6',-79,3)
    item('cat-head-flower-crown','HEAD'); wreath(120,50,130,[PINK,WHITE,'F7CE49','8DCCEE'],11)
    item('cat-neck-pearls')
    for k in range(9):
        a=math.pi*k/8
        radius=6.5+3*math.sin(a)
        oval('Graduated ivory pearl',120-46*math.cos(a),167+26*math.sin(a),
             radius,radius,'FFF3DB',-73,radius*.95)
    item('cat-extra-wand')
    tube('Purple wand handle',[(65,270),(29,197)],3.4,'9476C0',-66)
    star('Rounded gold magic star',27,190,27,GOLD,-70)
    item('cat-neck-cape'); cape('cat-neck-cape',120,166,'3D74CC',GOLD,True)
    item('cat-head-crown','HEAD'); crown(120,49,85,48)
    item('cat-extra-wings',layer='back')
    for side in (-1,1):
        oval('Sky blue butterfly upper wing',120+side*76,182,45,28,'54C5EC',25,8,side*-28)
        oval('Pink butterfly lower wing',120+side*66,227,31,26,'F78DB9',25,7,side*35)
        for dx,dy,rx,ry in [(80,175,18,10),(94,190,8,5)]:
            oval('Pale butterfly wing inset',120+side*dx,dy,rx,ry,'C7F0F5',14,2,side*-30)
        tube('Lower pink wing swirl',[(120+side*45,221),(120+side*72,216),(120+side*83,230),(120+side*69,241)],2,'FFC4DA',15)


def dog():
    item('dog-neck-bandana')
    pillow('Red triangular bandana',[(74,167),(120,179),(166,167),(144,204),(120,220),(96,202)],'E5344E',-68,7)
    tube('Bandana rolled neck edge',[(74,166),(120,176),(166,166)],4.5,'F14C60',-77)
    dots([(89,177),(109,184),(133,183),(151,175),(102,197),(122,202),(141,194)],WHITE,2.6,-80)
    item('dog-head-cap','HEAD'); cap(120,54,110,NAVY)
    item('dog-extra-ball')
    oval('Lime tennis ball',120,257,33,32,'95CD2C',-76,28)
    for side in (-1,1):tube('Curved ivory tennis seam',[(120+side*19,230),(120+side*14,242),(120+side*20,260),(120+side*13,283)],1.8,WHITE,-104)
    item('dog-neck-bone-collar')
    tube('Turquoise dog collar',[(77,170),(120,178),(163,170)],8,TEAL,-65)
    ring('Bone pendant ring',120,185,3,4,1.5,GOLD,-80)
    box('Gold bone center',120,199,28,8,GOLD,-78,4,2)
    for x in (105,135):
        for y in (194,203):oval('Rounded bone end',x,y,6,6,GOLD,-78,5)
    item('dog-head-sun-hat','HEAD'); sunhat(120,46,88)
    item('dog-extra-frisbee','HEAD')
    oval('Solid orange flying disc underside',120,165,40,14,'F85D21',-80,8)
    oval('Recessed orange disc surface',120,158,33,9,'E95018',-85,2)
    rim=tube('Continuous rounded disc rim',[(120+37*math.cos(a),160+12*math.sin(a),
        -85-7*math.sin(a)) for a in [i*math.tau/40 for i in range(40)]],6.5,'FF762C')
    rim.data.splines[0].use_cyclic_u=True
    item('dog-neck-bow-tie'); bow(120,186,.82,NAVY,False,-76)
    dots([(99,178),(99,190),(109,185),(138,179),(141,190),(132,185)],GOLD,2.6,-85)
    item('dog-head-party-hat','HEAD'); party_hat(126,50,PINK,YELLOW,4)
    item('dog-extra-boots')
    for x in (98,144):
        box('Yellow rain boot shaft',x,252,33,36,YELLOW,-78,12,7)
        oval('Rounded yellow boot toe',x+2,274,22,12,YELLOW,-85,15)
        tube('Raised rain boot rim',[(x-16,235),(x,238),(x+16,235)],3,'FFE775',-90)
        tube('Amber boot sole',[(x-19,280),(x,284),(x+21,280)],2.8,'E9A819',-96)
    item('dog-neck-lei')
    for k in range(9):
        t=k/8;flower(72+96*t,178+20*math.sin(t*math.pi),11,[PINK,'FDE36A','FF9D49',WHITE][k%4],-78)
    item('dog-head-aviator','HEAD')
    oval('Olive explorer hat brim',120,60,85,12,'718945',-63,25)
    pillow('Olive explorer hat dome',[(65,58),(72,28),(92,7),(123,2),(152,14),(175,37),
        (177,58),(150,58),(120,58),(90,58)],'91A257',-58,24)
    tube('Gold explorer hat band',[(66,51),(120,45),(175,51)],4,GOLD,-82)
    tube('Leather goggle strap',[(76,26),(120,14),(166,26)],4,'AC7736',-77)
    for x in (94,147):
        oval('Sky blue goggle lens',x,22,17,16,'67D7E7',-85,6)
        ring('Gold goggle frame',x,22,18,17,3,GOLD,-90)
        tube('Goggle glass highlight',[(x-8,16),(x-3,12),(x+4,12)],1.5,'DAF9F8',-93)
    tube('Goggles nose bridge',[(112,24),(121,19),(129,24)],2,GOLD,-89)
    item('dog-extra-backpack',layer='back')
    pillow('Rounded red backpack shell',[(27,184),(36,170),(63,168),(86,179),(91,210),
        (83,260),(67,274),(37,273),(25,258),(24,220)],'EE3F4B',-16,22)
    tube('Backpack side panel piping',[(68,176),(72,200),(68,238),(63,268)],1.5,'C72E3C',-40)
    tube('Backpack top seam',[(32,185),(49,179),(67,182)],1.1,'FF6B6B',-39)
    box('Backpack brass zipper teeth',34,219,4,32,'F9BA2D',-40,1.5,1)
    ring('Long brass zipper pull',35,225,4,8,1.8,GOLD,-45)
    item('dog-extra-backpack')
    for side in (-1,1):
        x=120+side*37
        pillow('Curved blue backpack shoulder strap',[(x-side*4,169),(x+side*8,173),
            (x+side*16,214),(x+side*14,237),(x+side*4,238),(x+side*4,214)],'2C88C9',-66,3)
        box('Gold shoulder strap buckle',x+side*8,205,12,10,GOLD,-74,2,2)
        box('Blue buckle center',x+side*8,205,5,5,'2C88C9',-77,1,1)
    item('dog-neck-medal'); medal(120,190,RED,19)
    item('dog-head-helmet','HEAD')
    oval('Red firefighter helmet brim',120,58,90,12,'DA2D3A',-68,24)
    pillow('Firefighter helmet dome',[(54,53),(64,16),(87,-2),(120,-11),(153,-2),(177,19),
        (186,53),(150,53),(120,53),(90,53)],'EE3848',-58,26)
    tube('Raised central helmet ridge',[(120,-8),(120,17),(120,50)],4,'FF6267',-87)
    for x in (78,162):tube('Fire helmet side ridge',[(x,17),(x-3,39)],3,'FF5662',-79)
    plate('Golden firefighter shield',[(104,15),(120,20),(136,15),(135,38),(120,50),(105,38)],GOLD,-92,3,2)
    plate('Shield pale inset',[(111,24),(120,27),(129,24),(128,36),(120,42),(112,36)],'FFF09B',-96,1,1)
    item('dog-extra-raincoat')
    for side in (-1,1):
        pillow('Raincoat long sleeve',[(120+side*29,177),(120+side*48,187),
            (120+side*66,233),(120+side*62,244),(120+side*46,246),(120+side*33,215)],'FFCD24',-61,7)
        tube('Raincoat sleeve cuff',[(120+side*48,241),(120+side*63,238)],2.4,'EAA51B',-70)
        pillow('Flared raincoat front panel',[(120+side*3,185),(120+side*25,172),
            (120+side*42,189),(120+side*58,250),(120+side*56,263),
            (120+side*31,266),(120+side*5,263),(120+side*3,240)],'FFD22C',-73,7)
        tube('Rounded raincoat lower hem',[(120+side*5,261),(120+side*31,265),
            (120+side*55,260)],3.3,'F3B01E',-81)
        pillow('Soft folded yellow hood collar',[(120+side*5,178),(120+side*30,163),
            (120+side*39,174),(120+side*24,184),(120+side*9,190)],'FFD945',-80,3)
        tube('Hood collar rolled edge',[(120+side*6,185),(120+side*22,178),(120+side*35,173)],2.5,'EAB025',-85)
    for x in (117,123):tube('Raincoat fastening cord',[(x,194),(x,250)],1.2,'DBA53A',-84)
    for y in (211,242):
        box('Rounded blue raincoat toggle',120,y,34,10,'3178BA',-87,3.5,4)
        tube('Toggle central notch',[(121,y-3),(121,y+3)],.6,'245C96',-91)
    item('dog-neck-cape'); cape('dog-neck-cape',120,171,'E83B50',GOLD)


def dragon():
    item('dragon-neck-pendant')
    necklace(120,156,84,[GOLD],13,3.5)
    plate('Gold diamond pendant frame',[(120,180),(137,195),(120,221),(103,195)],GOLD,-78,4,2)
    plate('Turquoise diamond pendant',[(120,185),(131,196),(120,213),(109,196)],'40C6DC',-83,3,1)
    item('dragon-head-flower-crown','HEAD'); wreath(120,49,121,[PINK,WHITE,YELLOW],10)
    item('dragon-extra-coins')
    pillow('Rounded red treasure pouch',[(99,231),(141,231),(151,253),(144,278),(98,278),(90,256)],'EA3144',-78,18)
    tube('Gold pouch gathered neck',[(99,232),(120,237),(141,232)],4,GOLD,-98)
    bow(120,235,.28,GOLD,False,-100)
    star('Gold pouch star',120,259,15,GOLD,-98)
    for x,y,count in [(106,221,3),(128,219,4),(75,280,2),(163,282,2)]:
        for k in range(count):
            oval('Stacked gold coin',x,y-k*4,12,3.8,GOLD,-81,7)
            tube('Bright coin edge',[(x-8,y-k*4),(x,y-k*4+2),(x+8,y-k*4)],.7,'FFECA0',-89)
    item('dragon-neck-bow'); bow(120,176,1.08,'F584AF',True,-74)
    item('dragon-head-wizard-hat','HEAD')
    oval('Blue wizard hat brim',120,51,78,12,'397BD1',-67,24)
    pillow('Bent blue wizard hat cone',[(73,46),(84,1),(104,-35),(137,-45),(153,-34),
        (134,-21),(144,8),(166,47),(140,47),(120,47),(95,47)],'4389DF',-58,23)
    tube('Gold wizard hat band',[(75,35),(120,43),(163,35)],5,GOLD,-83)
    for x,y,r in [(98,14,10),(125,-15,9),(141,17,7)]:star('Wizard hat gold star',x,y,r,GOLD,-83)
    item('dragon-extra-book')
    plate('Red spellbook cover',[(70,236),(116,240),(122,246),(129,240),(174,236),(176,278),
        (126,283),(120,287),(114,283),(68,278)],RED,-75,5,2)
    for side in (-1,1):
        pillow('Open ivory book pages',[(120,246),(120+side*43,235),(120+side*46,273),
            (120+side*8,277),(120,282)],'FFF0CA',-84,4)
        for y in (249,256,263,270):
            tube('Fine printed book line',[(120+side*8,y+3),(120+side*23,y),(120+side*36,y-1)],.6,'CDAA6E',-90)
    tube('Spellbook central binding',[(120,247),(120,281)],1.3,'CEAC70',-91)
    tube('Red bookmark ribbon',[(145,275),(146,288)],2,RED,-88)
    item('dragon-neck-scarf'); scarf(120,159,TEAL,YELLOW,.93)
    item('dragon-head-tiara','HEAD')
    tube('Tiara gold band',[(73,52),(120,48),(167,52)],4,GOLD,-78)
    for dx,h in [(-28,25),(0,47),(28,25)]:
        plate('Pointed tiara setting',[(120+dx-15,48),(120+dx,48-h),(120+dx+15,48)],GOLD,-73,3,1)
        plate('Tiara colored inset',[(120+dx-7,43),(120+dx,53-h),(120+dx+7,43)],PINK if dx==0 else BLUE,-78,2,1)
    item('dragon-extra-shield')
    outline=[(32,188),(61,181),(89,188),(87,238),(62,261),(36,240)]
    pillow('Gold shield rim',outline,GOLD,-78,7)
    pillow('Blue shield face',[(39,194),(61,188),(82,194),(80,234),(62,252),(43,235)],'329DD9',-87,4)
    star('Shield central gold star',61,215,19,GOLD,-94)
    item('dragon-neck-chain')
    necklace(120,156,84,[GOLD],15,4.5)
    ring('Heavy gold medal bail',120,179,4,5,2,GOLD,-79)
    oval('Gold dragon medallion',120,197,20,21,GOLD,-79,6)
    ring('Medallion raised inner rim',120,197,14,15,1.4,'FFE786',-87)
    item('dragon-head-knight-helmet','HEAD')
    pillow('Steel knight helmet dome',[(62,64),(66,32),(88,11),(121,5),(154,20),(176,47),
        (177,67),(145,59),(120,55),(90,58)],'8C9DA9',-61,23)
    tube('Steel helmet central ridge',[(122,8),(118,29),(120,54)],3,'D0D9DD',-84)
    tube('Dark lifted visor',[(64,64),(90,55),(120,52),(151,58),(177,69)],7,'526775',-84)
    for x in (81,101,121,142,161):box('Visor narrow ventilation slot',x,62+abs(x-120)*.09,3,9,'263F4D',-92,1,1)
    tube('Red helmet feather stem',[(130,13),(136,-13),(143,-29)],3,'B82A36',-57)
    pillow('Red knight helmet plume',[(133,3),(126,-20),(139,-36),(160,-38),(177,-25),
        (179,-9),(169,-21),(152,-17),(147,4)],'E53748',-53,7)
    item('dragon-extra-star-cloud')
    for x,y,rx,ry in [(52,273,29,19),(82,264,27,28),(119,269,31,24),(153,264,29,28),(184,274,28,19)]:
        oval('Soft sky blue cloud lobe',x,y,rx,ry,'74CAF1',-82,14)
    for x,y,r in [(75,269,15),(132,263,9),(182,273,8)]:star('Gold cloud star',x,y,r,GOLD,-101)
    item('dragon-neck-sparkles')
    necklace(120,157,86,[GOLD],13,3.2)
    for x,y,r,c in [(88,172,7,WHITE),(104,181,6,BLUE),(120,185,10,'49C8E6'),(139,178,6,BLUE),(153,167,7,WHITE)]:
        plate('Faceted jewel necklace',[(x,y-r),(x+r*.75,y),(x,y+r),(x-r*.75,y)],c,-81,3,1)
    item('dragon-neck-cape'); cape('dragon-neck-cape',120,155,'DC3646',GOLD)
    item('dragon-head-crown','HEAD'); crown(120,50,96,45,True)
    item('dragon-extra-wings','WINGS','back')
    for side in (-1,1):
        pts=[(120+side*38,170),(120+side*72,145),(120+side*116,142),
             (120+side*102,171),(120+side*99,203),(120+side*76,187),(120+side*54,200)]
        pillow('Pink dragon wing membrane',pts,'F293C3',25,4)
        tube('Blue dragon wing frame',pts[:3],4,'58BAE1',17)
        tube('Blue scalloped wing border',pts[2:]+[pts[0]],2.8,'58BAE1',17)
        for end in (pts[3],pts[4],pts[5],pts[6]):
            tube('Blue wing finger',[(120+side*67,158),end],1.8,'71CEEE',16)
        oval('Gold wing tip',pts[2][0],pts[2][1],3,3,GOLD,15,2)


def dinosaur():
    item('dinosaur-extra-backpack')
    backpack(54,197,YELLOW,'E9AD28',52,81)
    box('Red backpack pocket',54,213,39,23,RED,-67,5,4)
    box('Gold pocket clasp',55,208,8,11,GOLD,-75,2,2)
    tube('Backpack shoulder strap',[(82,154),(86,183),(91,206)],4,'E1AA29',-62)
    item('dinosaur-head-flower','HEAD')
    leaf(86,61,7,18,-45,-72);leaf(96,45,6,16,20,-72)
    flower(87,67,24,'F47AA6',-82)
    item('dinosaur-extra-bucket')
    plate('Blue tapered drink cup',[(190,233),(232,233),(227,281),(198,281)],'49B1E6',-75,12,3)
    oval('White drink lid',211,232,24,6,WHITE,-77,13)
    tube('Bent red drinking straw',[(215,230),(219,207),(227,204),(234,207)],4,'F24955',-78)
    star('Gold drink cup star',211,258,14,GOLD,-91)
    item('dinosaur-neck-scarf'); scarf(139,154,'EF433D',YELLOW,.88)
    item('dinosaur-head-cap','HEAD'); cap(126,55,112,'ED3F52',visor=26)
    item('dinosaur-extra-balloon')
    tube('Fine balloon string',[(175,204),(208,164),(218,130),(216,85)],1.1,'735C52',-58)
    oval('Glossy red balloon',217,48,28,36,'EA2943',-58,23,-8)
    plate('Balloon tied neck',[(215,80),(209,91),(222,91)],'CB273B',-60,3,1)
    oval('Balloon soft highlight',207,33,5,10,'FF8797',-80,1,-20)
    item('dinosaur-neck-bow-tie'); bow(141,164,.86,'EE394A',False,-76,5)
    dots([(120,157),(120,169),(130,163),(159,159),(165,172),(154,168)],WHITE,2.3,-86)
    item('dinosaur-head-party-hat','HEAD')
    hat=cone('Teal striped party hat',141,51,28,73,TEAL,-65,-2)
    hat.data.materials.append(art.mat(YELLOW))
    for face in hat.data.polygons:
        if 1 <= face.index <= 16*64 and (face.index-1)//64 in (2,3,7,8,12,13):
            face.material_index=1
    oval('Pink party pompom',139,-24,10,10,PINK,-64,8)
    item('dinosaur-extra-umbrella',layer='back')
    tube('Umbrella straight shaft',[(132,-13),(132,157),(145,168),(155,155)],3,GOLD,28)
    item('dinosaur-extra-umbrella')
    for panel in range(8):
        vertices=[art.HELP.point(126,-42,-58)]
        for row in range(1,17):
            t=row/16
            for col in range(13):
                u=col/12; a=(panel+u)*math.tau/8
                y=-42+65*t*t-10*math.sin(math.pi*u)*t**8
                vertices.append(art.HELP.point(126+98*t*math.cos(a),y,-58+43*t*math.sin(a)))
        faces=[(0,1+col,2+col) for col in range(12)]
        for row in range(15):
            for col in range(12):
                n=1+row*13+col;faces.append((n,n+1,n+14,n+13))
        obj=art.tag(art.HELP.mesh_form('Curved umbrella canopy panel',vertices,faces,
            art.mat(YELLOW if panel%2==0 else 'F45C64'),'accessory'))
        solid=obj.modifiers.new('Fabric canopy thickness','SOLIDIFY');solid.thickness=.025
        a=panel*math.tau/8
        tube('Umbrella canopy seam',[(126+98*t*math.cos(a),-42+65*t*t,-59+43*t*math.sin(a))
             for t in (0,.25,.5,.75,1)],.8,GOLD)
    oval('Umbrella top finial',126,-44,4,7,RED,-62,3)
    tube('Umbrella visible hooked handle',[(139,157),(139,177),(150,183),(160,174),(160,166)],3,GOLD,-78)
    item('dinosaur-extra-swim-ring',layer='back')
    tube('Inflatable ring behind the waist',[(137+54*math.cos(a),220+14*math.sin(a),-27-50*math.sin(a))
        for a in [math.pi+i*math.pi/24 for i in range(25)]],18,'FFD32A',25)
    item('dinosaur-extra-swim-ring')
    tube('Thick inflatable ring in front of the waist',[(137+54*math.cos(a),220+14*math.sin(a),-27-50*math.sin(a))
        for a in [-.12+i*(math.pi+.24)/28 for i in range(29)]],18,'FFD32A',-79)
    oval('Inflatable duck rising neck',187,212,23,25,'FFD32A',-80,20,-15)
    oval('Inflatable duck round head',190,188,24,24,'FFDA38',-82,22)
    oval('Orange duck upper bill',214,194,18,7,'F4771D',-102,10)
    oval('Orange duck lower bill',214,201,15,4.5,'F38D26',-103,8)
    tube('Duck bill smile',[(207,199),(216,200),(225,198)],.9,'CB511A',-112)
    oval('Duck eye ivory surround',197,183,6.2,7.4,'FFF1BA',-104,2)
    oval('Duck black eye',198,183,4.2,5.2,'233A36',-107,2)
    oval('Duck eye glint',197,181,1.4,1.7,WHITE,-110,1)
    item('dinosaur-head-hard-hat','HEAD')
    oval('Golden hardhat brim',135,51,72,11,'FDC323',-68,23)
    pillow('Yellow safety helmet dome',[(76,48),(82,14),(104,-6),(135,-10),(169,3),(190,23),
        (192,46),(160,47),(130,48),(100,48)],YELLOW,-59,26)
    tube('Hardhat raised center rib',[(132,-7),(131,13),(133,44)],4,'FFE976',-87)
    for x in (101,165):tube('Hardhat side rib',[(x,5),(x-5,22),(x-3,45)],2.2,'FFE779',-81)
    item('dinosaur-extra-kite')
    tube('Kite string',[(172,192),(124,142),(76,87),(56,17)],1.1,'9B7943',-55)
    cx=55;cy=23
    for pts,c in [([(cx,cy-42),(cx-31,cy),(cx,cy)],'F8D93D'),
                  ([(cx,cy-42),(cx+32,cy),(cx,cy)],'58CFB5'),
                  ([(cx-31,cy),(cx,cy+43),(cx,cy)],'28B69F'),
                  ([(cx+32,cy),(cx,cy+43),(cx,cy)],'FDD13D')]:
        plate('Colorful diamond kite panel',pts,c,-60,2,1)
    tube('Kite central spar',[(55,-19),(55,66)],.8,'D4B84C',-64)
    tube('Kite curling tail',[(55,66),(49,91),(56,113),(49,140)],1,'A07450',-60)
    bow(51,92,.3,RED,False,-66,10);bow(53,122,.3,RED,False,-66,-10)
    item('dinosaur-neck-binoculars')
    pillow('Left flat binocular neck strap',[(99,149),(106,148),(114,169),(125,181),
        (119,187),(107,173)],'428DB5',-65,1.8)
    pillow('Right flat binocular neck strap',[(163,148),(170,149),(164,174),(152,187),
        (146,181),(157,168)],'428DB5',-65,1.8)
    box('Binocular center hinge',133,186,16,15,'356987',-76,7,5)
    for x in (115,151):
        oval('Rounded dark blue binocular barrel',x,186,16,24,'2C6E91',-76,13,-8)
        oval('Deep blue circular lens housing',x,198,19,20,'245777',-88,7)
        oval('Convex cyan binocular glass',x,198,13.5,15,'55BBEA',-96,4)
        tube('Glass crescent highlight',[(x-8,196),(x-5,189),(x+3,187)],1.6,'B3E8FB',-101)
    oval('Binocular focus wheel',133,180,5,6,'294E69',-82,5)
    item('dinosaur-neck-medal')
    for points in ([(99,149),(136,188),(128,198),(91,155)],
                   [(162,149),(169,154),(143,199),(132,190)]):
        plate('White edged medal ribbon',points,'F3FAF6',-67,1.8,1.3)
    for points in ([(98,150),(134,188),(129,193),(95,154)],
                   [(163,151),(166,154),(142,194),(137,190)]):
        plate('Sky blue medal ribbon center',points,'62CDEB',-70,1,1)
    ring('Small gold medal bail',136,192,4,5,1.8,'F8BC25',-78)
    oval('Round gold winner medal',136,211,24,25,'FDC72D',-79,6)
    oval('Smooth gold medal face',136,211,20,21,'FFD640',-85,2.4)
    ring('Embossed circular medal border',136,211,20.5,21.5,1.4,'FFE36A',-88)
    item('dinosaur-extra-ice-cream','HAND')
    cone('Round tapered waffle cone',191,176,20,-43,'E7AF59',-76,-18)
    for direction in (-1,1):
        for k in range(12):
            points=[]
            for j in range(33):
                t=.02+.91*j/32; a=k*math.tau/12+direction*4*t; r=20*(1-t)+.4
                points.append((191-18*t+r*math.cos(a),176+43*t,-76+r*.65*math.sin(a)))
            tube('Raised diamond waffle lattice',points,.65,'F4C579')
    oval('Blue ice cream lower scoop',193,169,24,13,'76CFE4',-77,19,-18)
    oval('Pink strawberry ice cream dome',199,145,24,22,'F885B4',-77,21,-18)
    oval('Soft strawberry scoop lower edge',195,161,23,7,'F885B4',-78,18,-15)
    item('dinosaur-extra-skateboard')
    tube('Skateboard silver axle',[(54,284),(187,284)],3,'89A2A8',-62)
    for x in (63,179):
        oval('Gold skateboard wheel',x,293,12,12,GOLD,-78,8)
        oval('Orange wheel bearing',x,293,4,4,'DC8D26',-87,2)
    pillow('Upturned turquoise skateboard',[(29,268),(43,277),(99,280),(172,280),(205,272),
        (218,264),(217,279),(201,289),(58,290),(35,281)],TEAL,-67,6)
    tube('Gold skateboard deck edge',[(33,279),(69,287),(177,287),(209,277)],1.5,'F2C459',-74)
