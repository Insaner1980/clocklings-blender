"""Build the car wardrobe on the photographed three-quarter view's 240 x 300 grid."""
import math

import bpy


def build(art, selected=None):
    """Keep the same authoring geometry for the full rebuild and partial refinements."""
    oval, tube, plate = art.oval, art.tube, art.plate
    charcoal, steel, silver = '30394A', '718399', 'D9E7EB'
    gold, amber, cream = 'F7BD40', 'D38426', 'FFEEBA'
    teal, red = '41ADAB', 'D84943'

    def add(name, part='BODY', side='front'):
        key = 'car-' + name
        if selected is not None and key not in selected:
            return False
        art.item(key, part, side)
        return True

    def panel(name, points, color, depth, bevel=.8, thickness=.9):
        return plate(name, points, color, depth, thickness, bevel)

    def link(name, start, end, radius, color, depth):
        return tube(name, [start, end], radius, color, depth)

    def fitting(x, y, width=4, depth=-27):
        panel('Rubber mounting foot', [(x-width,y-1),(x+width-1,y-2),(x+width+1,y),(x-width+1,y+2)],
              charcoal, depth, .6, 1.3)

    def lettering(name, text, x, y, size, color, depth, tilt=0):
        data = bpy.data.curves.new(name, 'FONT')
        data.body = text
        data.align_x = 'CENTER'
        data.size = size / 50
        data.extrude = .003
        obj = bpy.data.objects.new(name, data)
        art.SCENE.collection.objects.link(obj)
        obj.location = art.HELP.point(x,y,depth)
        obj.rotation_euler = (math.pi/2,math.radians(tilt),0)
        data.materials.append(art.mat(color))
        art.tag(obj)

    if add('front-plate'):
        # A drilled frame follows the bumper's rising right edge.
        panel('Rounded enamel plate holder', [(139,257),(200,254),(201,267),(140,272)], charcoal,-88,2.1,2.4)
        panel('Honey yellow registration plate', [(141,258),(198,255.3),(198.8,266),(141.5,269.8)],
              gold,-92,1.4,1.3)
        lettering('Raised registration digits', '012',170,266.2,14,'29374A',-94.3,-3.4)
        for x,y in [(145,264.3),(194.5,261.4)]:
            oval('Recessed plate fastener',x,y,1,1,'7D652F',-94.2,.5)

    # Both tyres are shown in perspective; radial profiles and spokes use the same
    # ellipse as the rim underneath. Their depths give the lip, barrel and hub separate highlights.
    hubs = [(21.4,228.3,9.15,18.8),(75.65,246.0,13.05,23.4)]

    def wheel_face(x,y,rx,ry,angle,r):
        return (x+rx*r*math.cos(angle)-.42*r*math.sin(angle),y+ry*r*math.sin(angle))

    def turning(name, hub, profile, color, base=-90, divisions=72):
        verts, faces = [], []
        for radius,depth in profile:
            for i in range(divisions):
                x,y = wheel_face(*hub,i*math.tau/divisions,radius)
                verts.append(art.HELP.point(x,y,base+depth))
        for j in range(len(profile)):
            other=(j+1)%len(profile)
            for i in range(divisions):
                k=(i+1)%divisions
                faces.append((j*divisions+i, j*divisions+k, other*divisions+k,other*divisions+i))
        return art.tag(art.HELP.mesh_form(name,verts,faces,art.mat(color),'accessory'))

    def vane(name, hub, angle, contour, color, depth):
        points=[wheel_face(*hub,angle+math.radians(a),r) for r,a in contour]
        panel(name,points,color,depth,.5,.7)

    for style in ('sport','gold','flames','offroad'):
        if not add('wheels-'+style,'WHEELS'):
            continue
        for x,y,rx,ry in hubs:
            hub=(x,y,rx,ry)
            if style=='offroad':
                # Narrow shoulders stay round between blocks. The chevrons follow the tyre,
                # including the projected width of each lug on the far wheel.
                turning('Grape rubber sidewall',hub,[(.69,0),(1.14,-3),(1.32,-2),(1.48,1),(1.44,5),(.69,5)],'584567')
                for i in range(16):
                    vane('Moulded chevron tread',hub,math.tau*i/16,
                         [(1.13,-5),(1.31,-8),(1.46,-3),(1.44,3),(1.30,-1),(1.13,3)],'80698F',-93.4)
                style_hub=(x,y,rx*.76,ry*.76)
                rim,spokes,cap='B8AB90','F5DFC0','BDB8A4'
            else:
                style_hub=hub
                rim,spokes,cap={'sport':('B4DBE2','51BDE9','D1E6EF'),
                                'gold':('FFDF85','EBB841','FFE9A4'),
                                'flames':('BA6930','F28B36','FFE1A0')}[style]
            xx,yy,ww,hh=style_hub
            oval('Recessed graphite wheel barrel',xx,yy,ww*.97,hh*.98,'283242',-92,1.2)
            turning('Machined outer rim',style_hub,[(.84,1),(.86,-1.8),(.90,-3),(.98,-2.2),(1,0),(.98,2)],rim,-94)
            if style=='flames':
                for i in range(5):
                    a=i*math.tau/5
                    vane('Swept copper flame',style_hub,a,[(.16,-20),(.48,-22),(.77,-6),(.92,20),(.66,9),(.72,28),(.38,13),(.16,7)],'F06A32',-97)
                    vane('Golden flame tongue',style_hub,a,[(.19,-9),(.42,-11),(.69,0),(.78,11),(.51,4),(.39,6),(.2,4)],'FFD25D',-98.4)
            else:
                count={'sport':5,'gold':7,'offroad':5}[style]
                for i in range(count):
                    a=i*math.tau/count-.5*math.pi
                    shape=([(.15,-23),(.82,-10),(.92,-8),(.91,5),(.77,4),(.26,19)]
                           if style=='sport' else [(.16,-21),(.92,-8),(.94,4),(.65,7),(.19,19)])
                    vane('Forged alloy spoke',style_hub,a,shape,spokes,-97)
            oval('Raised hub collar',xx,yy,ww*.27,hh*.26,cap,-99.2,1.3)
            oval('Inset axle cap',xx,yy,ww*.13,hh*.13,'4D6874' if style=='sport' else 'A57A42',-101,.8)

    if add('model-race'):
        # Stop at the glass seal, eye sockets and grille, following the visible hood crown.
        for dx in (0,14):
            panel('Ivory roof racing stripe',
                  [(97+dx,156),(101+dx,155.1),(107+dx,155),(117+dx,158.8),(110+dx,160.3)],
                  'FFF4DD',-65,.28,.38)
            panel('Ivory bonnet racing stripe',
                  [(125+dx,199.9),(132+dx,199.7),(144+dx,208.4),(154+dx,218.4),
                   (163+dx,231.8),(162+dx,234),(155+dx,234.3),(149+dx,224.7),(138+dx,212.7)],
                  'FFF4DD',-84,.45,.55)
        # Same perspective as the side intake.
        panel('Driver door race roundel',[(49,206.8),(56,208),(60.3,213),(59.6,222.5),(54.5,225),
              (49,222.3),(46.4,216),(46.7,210.7)],'FFF0D6',-80,1.35,1)
        panel('Driver number seven',[(49,211),(57.6,212.6),(57.2,214.3),(52.3,220.5),(50,219.5),
              (54.8,214.3),(49.5,213.3)],'43536A',-82,.14,.22)

    if add('model-fire-engine'):
        # The back support lands on the spoiler and the forward one on the roof.
        for bx,by,topx,topy in [(49,171.4,46.5,152.4),(86,161.5,83,157.2)]:
            fitting(bx,by,5.1,-29)
            panel('Cast ladder carrier',[(bx-3,by),(bx-1,by-2.2),(topx-2.1,topy+.2),
                  (topx+1.7,topy-.2),(bx+2.8,by-2),(bx+4,by-.2)],'667B87',-32,.65,1.35)
            link('Ladder saddle pad',(topx-3.7,topy-.15),(topx+4,topy+.8),1.5,charcoal,-34)
            oval('Carrier fixing bolt',bx,by-2.2,.95,1.1,silver,-34,.65)
        # The far rail sits both higher and farther right in the car's perspective.
        p0,p1=(18,149),(98,159)
        q0,q1=(27,141.5),(107,151.5)
        link('Far aluminium ladder rail',q0,q1,2.0,steel,-32)
        for i in range(9):
            t=.035+i*.116
            p=(p0[0]+80*t,p0[1]+10*t)
            q=(q0[0]+80*t,q0[1]+10*t)
            link('Ladder cross tread',p,q,1.25,silver,-34)
        link('Near aluminium ladder rail',p0,p1,2.05,'B8CBD2',-37)
        link('Recess in ladder rail',(19,149.5),(97,159.2),.58,'748899',-39)
        for x,y in (p0,p1,q0,q1):
            oval('Ladder protective cap',x,y,1.9,2.1,charcoal,-40,.9)
        fitting(128.5,154.8,8)
        def beacon_lathe(name, profile, color):
            vertices,faces=[],[]
            segments=48
            for radius,level in profile:
                for i in range(segments):
                    angle=math.tau*i/segments
                    vertices.append(art.HELP.point(128.5+radius*math.cos(angle),
                                       level-radius*.13*math.sin(angle),-41+radius*math.sin(angle)))
            for row in range(len(profile)-1):
                for i in range(segments):
                    a=row*segments+i;b=row*segments+(i+1)%segments
                    faces.append((a,b,b+segments,a+segments))
            faces.extend((tuple(range(segments-1,-1,-1)),tuple(range(len(vertices)-segments,len(vertices)))))
            art.tag(art.HELP.mesh_form(name,vertices,faces,art.mat(color),'accessory'))
        beacon_lathe('Alloy beacon plinth',[(7.3,154),(7.8,153.5),(7.8,152),(7,151.1)],steel)
        beacon_lathe('Ruby rotating beacon dome',[(6.6,151.5),(6.8,150.5),(6.5,144.8),
                       (6,143.5),(4.5,142.6),(.9,142.1)],'E64A45')
        for dx in (-3.8,-1.8,2.4):
            front=-41-math.sqrt(6.65**2-dx**2)
            tube('Prismatic beacon lens rib',[(128.5+dx,145.5),(128.5+dx,149.9)],
                 .32,'F66D56',front-.2,[.55,1])

    if add('roof-suitcase'):
        for x,y in [(69,164),(122,156.5)]:
            fitting(x,y)
        link('Luggage carrier rail',(63,162.9),(130,153.1),1.65,steel,-32)
        panel('Vintage suitcase left gusset',[(62,130),(70,125),(76,153),(67,159.6)],'963D38',-33,1.7,3)
        panel('Vintage suitcase lid',[(62.6,129),(71,122.5),(128,117.3),(132,122.7),(76,131.8)],'F67758',-35,1.5,3)
        panel('Red leather suitcase',[(68,130),(131.5,122.5),(133,151.9),(69.8,159.2)],red,-39,2,3.1)
        link('Lid welting',(69.7,141.4),(130.8,134.5),.8,'A84038',-43)
        for x,y in [(79,128.5),(117.5,124)]:
            panel('Leather luggage belt',[(x,y),(x+4,y-.4),(x+5.7,y+28.9),(x+1.5,y+29.5)],'F5D2A0',-44,.7,.9)
            panel('Brass luggage clasp',[(x-.4,y+10),(x+5.3,y+9.4),(x+5.6,y+14),(x,y+14.6)],gold,-46,.55,.45)
        tube('Wrapped leather handle',[(88,124),(89,119),(99,117),(105,117),(107,122)],1.7,'76463A',-37)
        for x,y in [(71.7,155.5),(129.8,149)]:
            oval('Brass corner stud',x,y,1.35,1.4,gold,-44,1)

    if add('roof-surfboard'):
        for bx,by,topx,topy in [(74,165.2,71.8,150.2),(118.8,157,118.3,152.8)]:
            fitting(bx,by,5.1,-27)
            panel('Board carrier tower',[(bx-2.9,by),(bx-1.9,by-1.5),(topx-2.1,topy-.2),
                  (topx+1.7,topy+.2),(bx+2.3,by-1.5),(bx+3.3,by-.2)],'617984',-29,.65,1.2)
            link('Padded board saddle',(topx-4,topy-.2),(topx+4,topy+.8),1.8,charcoal,-31)
        # Lens-shaped rails and an asymmetric nose give the board its tip and rocker.
        outline=[(24,142.5),(36,136.7),(60,136),(101,140),(146,146),(163,148.7),
                 (166,151),(159,155.4),(136,157.4),(103,154.9),(62,150.8),(35,146.7)]
        art.pillow('Honey surfboard rail',outline,gold,-34,3.3)
        upper=[(x, y-2) for x,y in outline]
        art.pillow('Seafoam surfboard deck',upper,'4DB5A8',-38,2.6)
        tube('Cream board stringer',[(28,141.3),(59,141.9),(101,146.4),(142,151.5),(161,151.7)],
             1,'FFECBD',-41.8,[.2,1,1,1,.15])
        for x,y in [(68,141.1),(120,148.2)]:
            link('Cinch strap across surfboard',(x+5,y-4),(x-1,y+8),1.45,'405B68',-43)

    if add('roof-flag'):
        fitting(100,158.3,5.2)
        tube('Tapered graphite flag staff',[(100,157),(102.5,127),(105,99)],1.2,charcoal,-34,[1.3,1,.75])
        oval('Brass flag staff finial',105.1,97.9,1.8,1.9,gold,-37,1.45)
        # Contiguous quads share each wave and silhouette, so the checker print follows the cloth.
        def fabric(u,v):
            wave=math.sin(u*math.tau*1.12)
            return (105.1+46*u-1.55*v,v*(24-4*u)+99+1.5*u-3.2*wave,-35-3.2*wave-v*.5)
        cols,rows=36,4
        verts=[art.HELP.point(*fabric(i/cols,j/rows)) for j in range(rows+1) for i in range(cols+1)]
        faces=[(j*(cols+1)+i,j*(cols+1)+i+1,(j+1)*(cols+1)+i+1,(j+1)*(cols+1)+i)
               for j in range(rows) for i in range(cols)]
        flag=art.tag(art.HELP.mesh_form('Woven chequered pennant',verts,faces,art.mat('FFEFCC'),'accessory'))
        flag.data.materials.append(art.mat('334052'))
        for f in flag.data.polygons:
            row,col=divmod(f.index,cols)
            f.material_index=(row+col//6)%2
        hem=flag.modifiers.new('Double-faced flag cloth','SOLIDIFY')
        hem.thickness=.009
        for h in (.14,.86):
            x,y,z=fabric(0,h)
            link('Flag sleeve binding',(x-.8,y),(x+1.4,y+.25),.7,gold,z-1.3)

    if add('roof-taxi'):
        panel('Taxi sign rubber shoe',[(75,157.5),(125,151),(131,154.8),(79,162.3)],charcoal,-32,1.2,2)
        panel('Golden cab sign side',[(73.6,155.9),(78.6,141.7),(84.8,139.4),(82.8,157.2)],amber,-34,1.1,1.6)
        panel('Golden cab light box',[(78.5,157.8),(82.6,141),(119,136.2),(127.8,151.2)],gold,-38,1.6,2.4)
        lettering('Taxi sign lettering','TAXI',103,151.4,12,'303949',-41.2,-7.5)

    if add('roof-rally-lights'):
        for x,y in [(71.5,163.7),(125,156)]:
            fitting(x,y)
            link('Rally light bracket',(x,y-1),(x-1,y-6),1.7,charcoal,-30)
        link('Roof light crossbar',(65,158.7),(130,150.1),2,steel,-32)
        for i in range(4):
            x,y=70.4+17.35*i,150.6-2.24*i
            oval('Graphite rally lamp housing',x,y,8.7,8.3,charcoal,-36,5)
            oval('Raised alloy lamp bezel',x+.45,y-.2,7.5,7.1,'BEC9C7',-40,2.6)
            oval('Amber driving lens',x+.75,y-.05,6.2,5.85,gold,-43.1,2.5)
            tube('Fluted lens highlight',[(x-2.8,y-2.8),(x-.8,y-3.6),(x+1,y-3.7)],.65,cream,-45.9)

    if add('front-sunglasses'):
        left=[(104,223),(114,220),(135,219.6),(146,222),(148,225),(145,235),
              (139,240.6),(123,241.6),(112,238),(106.6,231)]
        right=[(188.8,222),(198,217.5),(216.2,215.9),(223.6,218.8),(224.3,224),
               (221,233),(214.4,236.2),(200.4,237.9),(193.2,235),(189.6,229.5)]
        tube('Folded glasses arm',[(105.1,226),(99,222),(94,218.6)],1.85,charcoal,-90)
        tube('Raised saddle bridge',[(146.8,226.9),(157.5,224.5),(176.8,223.7),(190.2,225.2)],2.2,'3C3944',-96)
        for points in (left,right):
            panel('Espresso acetate lens frame',points,'39323C',-97,2,2)
            cx=sum(x for x,y in points)/len(points)
            cy=sum(y for x,y in points)/len(points)
            lens=[(cx+(x-cx)*.83,cy+(y-cy)*.78) for x,y in points]
            panel('Smoked blue sunglass lens',lens,'29394F',-100,1.6,1.25)
        for x,y in [(114,225),(198,222.7)]:
            link('Reflection in polished lens',(x,y+2.8),(x+10.7,y-1.8),.8,'A3B9C3',-102)

    if add('front-bull-bar'):
        for x,y in [(123,262.6),(211.6,257.2)]:
            fitting(x,y,4.7,-79)
            link('Bumper guard upright',(x,y+3.4),(x-1.2,y-3.6),1.6,charcoal,-85)
        tube('Bent alloy bumper hoop',[(112,265),(113.5,257),(122,253.8),(150,255.4),
             (177,254.9),(210,250.8),(220,251.4),(223,258)],2.05,silver,-92)
        for x,y in [(124.7,259.1),(211.8,255.1)]:
            oval('Metal fog light casing',x,y,6.2,6.4,charcoal,-94,3.2)
            oval('Amber fog light',x+.1,y-.4,4.8,4.9,gold,-97.4,2.2)
            oval('Fog lens glint',x-1,y-2,1.35,.95,cream,-99.9,.45)

    if add('front-plow'):
        for x,y in [(133,263.6),(204,259.3)]:
            link('Snow blade suspension arm',(x,y-1.6),(x-2.5,y+9),2,charcoal,-85)
        panel('Gold curved snow blade',[(117,266.8),(151,268),(187,265.7),(221.5,260.3),
              (227,278),(202,283.3),(164,287),(110,285.2)],gold,-95,2.0,2.8)
        for x,y in [(126,267.9),(149,268.6),(173,267.6),(197,264.9)]:
            panel('Inlaid snow blade warning stripe',[(x,y),(x+7.1,y-.25),(x-.3,y+16.7),(x-7.4,y+16.5)],
                  '414650',-98.5,.5,.45)
        tube('Steel curved scraper edge',[(110.6,285),(139,286.9),(166,286.9),(197,284),(226,278.5)],1.7,steel,-100)
        for x,y in [(116,285.1),(143,286.7),(180,285.7),(219,280.1)]:
            oval('Scraper lip rivet',x,y,.95,.75,silver,-101.7,.42)

    if add('model-rocket',side='back'):
        for x,y,scale in [(4.7,183.5,1),(47,166.7,.82)]:
            def fin(points):
                return [(x+u*scale,y+v*scale) for u,v in points]
            panel('Swept enamel rocket fin',fin([(1,-27),(4,-26),(22,-13),(33,-4),
                  (36,4),(31,8),(14,3),(8,-4)]),red,20,2.0,2.3)
            panel('Inset fin flashes',fin([(6,-21),(8,-21),(26,-7),(27,-4),(23,-5)]),amber,16.6,.65,.65)
    if add('model-rocket'):
        panel('Rocket body mounting plate',[(14,190),(29,188),(35,191.4),(32.5,198),(17.8,201.6)],
              charcoal,-76,1.1,2.7)
        panel('Rocket exhaust barrel',[(3.4,188),(14,187.5),(26.1,191),(24,198.2),(11.6,199.3),(1,196)],
              steel,-81,1.55,3)
        oval('Rocket exhaust rim',4.2,192.2,3.8,6.7,'C6D4DB',-85.2,1.8,-12)
        # Taper the plumes towards the rear, clear of the rear wheel face.
        panel('Orange thrust plume',[(-15,183.9),(-6.6,185.1),(-8.2,181.7),(2.5,188.7),(4,191.6),
              (2.8,195.7),(-1.5,196.7),(-12.8,196.9),(-8.2,193.1),(-18.6,190.2),(-10.4,190)],
              'F18439',-88,.9,1.2)
        panel('Hot thrust core',[(-9.5,188.5),(-1.6,189.4),(-3.2,187.6),(3.8,191.2),
              (3.1,194.7),(-5.6,194.4),(-3.2,192.5)],'FFDA76',-91,.65,.6)
        panel('Enamel lightning badge',[(52.6,207.7),(58.7,208.8),(54.3,215.2),(59.8,215.4),
              (49.6,226.6),(52.5,218.1),(47.3,217.7)],gold,-78,.5,.8)
