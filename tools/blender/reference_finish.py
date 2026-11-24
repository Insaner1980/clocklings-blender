"""Shared surface finishing for the reference-matched, layered toy models."""

import bpy


def fuse(names, name, voxel=0.024):
    """Join intersecting forms into a single soft surface within one animation layer."""
    objects = [bpy.context.scene.objects[n] for n in names]
    layer = objects[0].get("layer")
    material = objects[0].data.materials[0]
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = objects[0]
    bpy.ops.object.convert(target="MESH")
    bpy.ops.object.join()
    obj = bpy.context.object
    obj.name = name
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    remesh = obj.modifiers.new("Continuous toy surface", "REMESH")
    remesh.mode = next(i.identifier for i in remesh.bl_rna.properties["mode"].enum_items if i.identifier == "VOXEL")
    remesh.voxel_size = voxel
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth = obj.modifiers.new("Soft transitions", "SMOOTH")
    smooth.factor = 0.8
    smooth.iterations = 6
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    obj.data.materials.clear()
    obj.data.materials.append(material)
    if layer is not None:
        obj["layer"] = layer
    for face in obj.data.polygons:
        face.use_smooth = True
    return obj


def toy_eye(oval, material, name, x, y, depth, rx, ry, group, iris_color="79503D"):
    """A shallow glossy eye with a warm lower iris and a fine sclera crescent."""
    edge = material(name + " soft outline", "35252D", 0.28)
    white = material(name + " warm sclera", "FFF5EB", 0.3)
    iris = material(name + " shaded iris", iris_color, 0.19)
    nodes = iris.node_tree.nodes
    shader = next(n for n in nodes if n.type == "BSDF_PRINCIPLED")
    coords = nodes.new("ShaderNodeTexCoord")
    separate = nodes.new("ShaderNodeSeparateXYZ")
    ramp = nodes.new("ShaderNodeValToRGB")
    base = tuple(shader.inputs["Base Color"].default_value)
    ramp.color_ramp.elements[0].position = 0.03
    ramp.color_ramp.elements[0].color = base
    ramp.color_ramp.elements[1].position = 0.68
    ramp.color_ramp.elements[1].color = (0.009, 0.004, 0.009, 1)
    iris.node_tree.links.new(coords.outputs["Generated"], separate.inputs[0])
    iris.node_tree.links.new(separate.outputs["Z"], ramp.inputs[0])
    iris.node_tree.links.new(ramp.outputs[0], shader.inputs["Base Color"])
    shader.inputs["Coat Weight"].default_value = 0.25
    shader.inputs["Coat Roughness"].default_value = 0.12
    shader.inputs["Specular IOR Level"].default_value = 0.25
    glint = material(name + " catchlight", "FFFFFF", 0.2)
    gloss = next(n for n in glint.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    gloss.inputs["Emission Color"].default_value = (1, 1, 1, 1)
    gloss.inputs["Emission Strength"].default_value = 0.25
    oval(name + " outline", x, y, depth, rx, ry, 2.3, edge, group)
    oval(name + " sclera", x, y, depth - 1.1, rx - 0.75, ry - 0.75, 2.2, white, group)
    oval(name + " iris", x - rx * 0.07, y + ry * 0.025, depth - 2.8,
         rx * 0.87, ry * 0.925, 2.5, iris, group)
    oval(name + " main catchlight", x - rx * 0.32, y - ry * 0.43, depth - 5.2,
         rx * 0.27, ry * 0.25, 0.55, glint, group)
    oval(name + " small catchlight", x + rx * 0.32, y + ry * 0.53, depth - 5.0,
         rx * 0.105, ry * 0.09, 0.35, glint, group)


def configure(scene):
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 40
    scene.cycles.use_denoising = True
    scene.render.resolution_x = 720
    scene.render.resolution_y = 900
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 8


def studio(scene):
    """Soft broad highlights without the old white band across every forehead."""
    for obj in scene.objects:
        if obj.type == "LIGHT":
            if "key" in obj.name.lower():
                obj.data.energy = 650
                obj.data.size = 5.0
            elif "fill" in obj.name.lower():
                obj.data.energy = 320
                obj.data.size = 5.0
            elif "rim" in obj.name.lower():
                obj.data.energy = 170
                obj.data.size = 4.0
    background = next(n for n in scene.world.node_tree.nodes if n.type == "BACKGROUND")
    background.inputs["Color"].default_value = (1, 0.94, 0.88, 1)
    background.inputs["Strength"].default_value = 0.3
    scene.view_settings.exposure = -0.55


def polish_robot(scene):
    """Keep the approved mechanical rig while softening its hands and face finish."""
    for side in ("L", "R"):
        if scene.objects.get("Thumb " + side):
            fuse(["Mitten " + side, "Thumb " + side], "Mitten " + side, voxel=0.016)
        scene.objects["Eye " + side].scale = (0.166, 0.023, 0.257)
    for name, values in {
        "Face - luminous mint": {
            "Base Color": (0.10, 0.80, 0.44, 1),
            "Emission Color": (0.08, 0.80, 0.40, 1),
            "Emission Strength": 0.45,
        },
        "Screen - midnight blue": {"Roughness": 0.32, "Specular IOR Level": 0.22},
        "Key - honey gold": {"Metallic": 0.32, "Roughness": 0.25},
    }.items():
        shader = next(n for n in bpy.data.materials[name].node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        for key, value in values.items():
            shader.inputs[key].default_value = value
    studio(scene)
    scene.view_settings.exposure = -0.35
    scene.view_settings.view_transform = "Standard"

