"""Render the approved Blender robot into the app's 240 x 300 animation grid.

Run with Blender --background --threads 8 --python-exit-code 1 --python this_file.
The original scene stays unchanged. PNGs are packed into one atlas per shell color.
"""

from pathlib import Path
import math
import sys

import bpy
import numpy as np
sys.path.insert(0, str(Path(__file__).resolve().parent))
from export_wardrobe import read_layer_pixels
from reference_finish import polish_robot


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/blender/robot.blend"
OUTPUT = ROOT / "art/blender/atlases"
SCRATCH = ROOT / ".scratch/blender-integration/render"
# Grid rectangle followed by its pixel origin in the 896 x 640 atlas.
REGIONS = {
    "head": ((30, 42, 210, 148), (0, 0)),
    "body": ((68, 136, 172, 232), (544, 0)),
    "legs": ((70, 202, 170, 284), (244, 322)),
    "arm_left": ((32, 144, 88, 233), (548, 322)),
    "arm_right": ((152, 144, 208, 233), (720, 322)),
    "eyes": ((80, 75, 160, 110), (0, 322)),
    "key": ((149, 160, 223, 227), (0, 432)),
}
COLORS = {
    "original": ((0.26, 0.65, 0.68), (0.13, 0.32, 0.35)),
    "blue": ((0.24, 0.48, 0.78), (0.12, 0.25, 0.43)),
    "green": ((0.40, 0.68, 0.43), (0.21, 0.37, 0.23)),
    "purple": ((0.61, 0.40, 0.77), (0.33, 0.20, 0.43)),
}


def linear(value):
    return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4


def tint(name, rgb):
    material = bpy.data.materials[name]
    shader = next(node for node in material.node_tree.nodes if node.type == "BSDF_PRINCIPLED")
    shader.inputs["Base Color"].default_value = (*map(linear, rgb), 1)


def descendants(name):
    return set(bpy.data.objects[name].children_recursive)


bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
scene = bpy.context.scene
scene.frame_set(1)
polish_robot(scene)
scene.render.engine = "CYCLES"
scene.cycles.device = "CPU"
scene.cycles.samples = 48
scene.cycles.use_denoising = True
scene.render.resolution_x = 720
scene.render.resolution_y = 900
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = "PNG"
scene.render.image_settings.color_mode = "RGBA"
scene.render.image_settings.color_depth = "8"
camera = scene.camera
camera.location = (0, -18, 2.56)
camera.rotation_euler = (math.pi / 2, 0, 0)
camera.data.type = "ORTHO"
camera.data.sensor_fit = "VERTICAL"
camera.data.ortho_scale = 5.91
bpy.context.view_layer.update()

eyes = descendants("Eye L - blink") | descendants("Eye R - blink")
head = descendants("Head - tilt") - eyes
left_arm = descendants("Arm R - wave")  # Viewer left.
right_arm = descendants("Arm L - wave")
key = descendants("Accessory - wind-up key")
legs = {obj for obj in scene.objects if obj.name.startswith(("Boot ", "Shin ", "Ankle cuff "))}
body = descendants("Body - breathing") - head - eyes - left_arm - right_arm - key
groups = dict(head=head, body=body, legs=legs, arm_left=left_arm, arm_right=right_arm, eyes=eyes, key=key)
geometry = [obj for obj in scene.objects if obj.type in {"MESH", "CURVE"}]
bpy.data.objects["Studio floor"].hide_render = True
OUTPUT.mkdir(parents=True, exist_ok=True)
SCRATCH.mkdir(parents=True, exist_ok=True)
shared = {}

for color, (shell, joints) in COLORS.items():
    tint("Shell - sea glass turquoise", shell)
    tint("Joints - deep teal", joints)
    atlas_pixels = np.zeros((640, 896, 4), dtype=np.float32)
    for part, (rect, origin) in REGIONS.items():
        if part in shared:
            pixels = shared[part]
        else:
            for obj in geometry:
                obj.visible_camera = obj in groups[part]
                # Hidden accessories and eyes must not leave shadows in the base layers.
                obj.hide_render = obj.name == "Studio floor" or (obj in key and part != "key") or (obj in eyes and part != "eyes")
            path = SCRATCH / f"{color}_{part}.png"
            scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            rendered = bpy.data.images.load(str(path), check_existing=False)
            pixels = read_layer_pixels(rendered, 720, 900)
            bpy.data.images.remove(rendered)
            if part in {"eyes", "key"}:
                shared[part] = pixels
        x0, y0, x1, y1 = (value * 3 for value in rect)
        cropped = pixels[y0:y1, x0:x1]
        # Reject accidental clipping of a part before it reaches Android.
        outside = pixels[:, :, 3].copy()
        outside[y0:y1, x0:x1] = 0
        assert outside.max() < 0.01, f"Clipped {color}/{part}"
        assert cropped[:, :, 3].max() > 0.9, f"Empty {color}/{part}"
        ax, ay = origin
        atlas_pixels[ay:ay + y1 - y0, ax:ax + x1 - x0] = cropped
    atlas = bpy.data.images.new(f"Robot {color}", width=896, height=640, alpha=True)
    atlas.colorspace_settings.name = "sRGB"
    atlas.pixels.foreach_set(atlas_pixels[::-1].reshape(-1))
    atlas.filepath_raw = str(OUTPUT / f"robot_blender_{color}.png")
    atlas.file_format = "PNG"
    atlas.save()
    bpy.data.images.remove(atlas)
    print(f"ATLAS_COMPLETE {color}", flush=True)

print("ROBOT_LAYERS_COMPLETE", flush=True)
