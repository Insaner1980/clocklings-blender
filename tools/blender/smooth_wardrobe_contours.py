"""Fair the existing painted model in camera space while preserving its UVs and motion.

blender -b art/blender/cat.blend -P tools/blender/smooth_wardrobe_contours.py
Then render the saved scene with export_wardrobe.py and publish with optimize_atlases.py.
Pass -- <output.blend> to save a preview copy instead of the current scene.
"""
import json
import sys
from pathlib import Path

import bpy
import numpy as np
from mathutils.kdtree import KDTree


def rim_loops(mesh, count):
    """Follow the front triangles around each boundary, even when two rings touch."""
    lengths = np.empty(len(mesh.polygons), np.int32)
    starts = np.empty_like(lengths)
    indices = np.empty(len(mesh.loops), np.int32)
    mesh.polygons.foreach_get('loop_total', lengths)
    mesh.polygons.foreach_get('loop_start', starts)
    mesh.loops.foreach_get('vertex_index', indices)
    triangles = indices[starts[lengths == 3, None] + np.arange(3)]
    triangles = triangles[np.all(triangles < count, axis=1)]
    if not len(triangles):
        return

    # Painted shells have corresponding front/back triangles and four-sided walls.
    walls = indices[starts[lengths == 4, None] + np.arange(4)]
    if not len(walls) or not np.all(walls[:, :2] + count == walls[:, 3:1:-1]):
        return
    following = {}
    for a, b, c in triangles.tolist():
        following[a, b] = c
        following[b, c] = a
        following[c, a] = b
    boundary = {edge for edge in following if edge[::-1] not in following}
    while boundary:
        start = next(iter(boundary))
        edge = start
        ring = []
        while edge in boundary:
            boundary.remove(edge)
            a, b = edge
            ring.append(a)
            c = following[a, b]
            while (c, b) in following:
                c = following[c, b]
            edge = b, c
        if edge != start:
            raise ValueError(f'Unclosed painted boundary in {mesh.name}')
        if len(ring) >= 5:
            yield np.asarray(ring)


def fair_ring(points, width):
    closed = np.vstack([points, points[0]])
    arc = np.r_[0, np.cumsum(np.linalg.norm(np.diff(closed, axis=0), axis=1))]
    if arc[-1] < 2:
        return np.zeros_like(points)
    step = arc[-1] / max(16, int(np.ceil(arc[-1] / .35)))
    offsets = np.arange(round(arc[-1] / step)) * step
    line = np.column_stack([np.interp(offsets, arc, col) for col in closed.T])

    # Arc-length convolution treats long and short mesh edges equally. The broader
    # kernel compensates for shrinkage on rounded cheeks, buttons and wheel arches.
    def blur(radius):
        spread = np.arange(-int(np.ceil(3 * radius / step)), int(np.ceil(3 * radius / step)) + 1)
        weights = np.exp(-.5 * (spread * step / radius)**2)
        return sum(w * np.roll(line, j, axis=0) for w, j in zip(weights / weights.sum(), spread))

    radius = min(width, arc[-1] / 16)
    smooth = (4 * blur(radius) - blur(2 * radius)) / 3
    shift = smooth - line
    # Keep the tips of strongly projecting features such as whiskers and horns.
    span = min(len(line) // 5, max(1, round(2.3 * radius / step)))
    ahead = np.roll(line, -span, axis=0) - line
    behind = np.roll(line, span, axis=0) - line
    cosine = (ahead * behind).sum(axis=1) / np.maximum(
        .00001, np.linalg.norm(ahead, axis=1) * np.linalg.norm(behind, axis=1)
    )
    limit = np.where(cosine > .45, .38, width * .72)
    shift *= np.minimum(1, limit / np.maximum(.00001, np.linalg.norm(shift, axis=1)))[:, None]
    return np.column_stack([
        np.interp(arc[:-1], np.r_[offsets, arc[-1]], np.r_[col, col[0]]) for col in shift.T
    ])


def rim_samples(obj, units):
    mesh = obj.data
    count = len(mesh.vertices) // 2
    positions = np.empty((len(mesh.vertices), 3), np.float32)
    mesh.vertices.foreach_get('co', positions.ravel())
    matrix = np.asarray(obj.matrix_world)
    world = (positions @ matrix[:3, :3].T + matrix[:3, 3])[:, [0, 2]] / units
    for loop in rim_loops(mesh, count):
        points = np.vstack([world[loop], world[loop[0]]])
        arc = np.r_[0, np.cumsum(np.linalg.norm(np.diff(points, axis=0), axis=1))]
        offsets = np.arange(0, arc[-1], .6)
        yield np.column_stack([np.interp(offsets, arc, col) for col in points.T])


def smooth_object(obj, spec, width, neighbors):
    left, top, right, bottom, cx, cz, units = spec['frame']
    mesh = obj.data
    count = len(mesh.vertices) // 2
    loops = list(rim_loops(mesh, count))
    if not loops:
        return False
    basis = np.empty((2 * count, 3), np.float32)
    mesh.vertices.foreach_get('co', basis.ravel())
    matrix = np.array(obj.matrix_world)[:3, :3]
    xy = (basis[:count] @ matrix.T)[:, [0, 2]] / units
    shifts = np.zeros((count, 2))
    samples = np.zeros(count)
    for ring in loops:
        np.add.at(shifts, ring, fair_ring(xy[ring], width))
        np.add.at(samples, ring, 1)
    rim = np.flatnonzero(samples)
    shifts[rim] /= samples[rim, None]
    # Carry nearby paint and interior triangles along with the border. Moving
    # only the rim allows triangles just inside a notch to stick out past it.
    radius = width * 1.6
    inside = np.flatnonzero(samples == 0)
    for block in np.array_split(inside, 1 + len(inside) // 256):
        delta = xy[block, None] - xy[rim]
        dist2 = (delta * delta).sum(axis=2)
        weights = np.exp(-dist2 / (2 * radius**2)) / np.maximum(.01, dist2)
        shifts[block] = (weights @ shifts[rim]) / np.maximum(weights.sum(axis=1, keepdims=True), 1.e-20)
        shifts[block] *= np.exp(-dist2.min(axis=1) / (2 * radius**2))[:, None]
    if neighbors:
        # At moving joints independently rounding both fitted pieces can cut a
        # hole. Preserve their common edge, feathering into the surrounding arc.
        tree = KDTree(sum(len(points) for points in neighbors))
        for points in neighbors:
            for point in points:
                tree.insert((point[0], point[1], 0), 0)
        tree.balance()
        origin = np.asarray(obj.matrix_world.translation)[[0, 2]] / units
        proximity = np.array([tree.find((x, z, 0))[2] for x, z in xy + origin])
        joint_weight = np.clip((proximity - 1.8) / 4.0, 0, 1)
        if spec['name'] == 'bunny_blender' and obj['atlas_layer'] in ('head', 'ear_left', 'ear_right'):
            # The hanging ears swing away from the cheeks during a hop. Only
            # their attachment at the crown is a fitted joint in all poses.
            y = (top + bottom) / 2 + cz / units - (xy[:, 1] + origin[1])
            joint_weight = np.maximum(joint_weight, np.clip((y - 63) / 12, 0, 1))
        shifts *= joint_weight[:, None]
    if spec['name'] == 'cat_blender' and obj['atlas_layer'] in ('ears', 'head'):
        # The whiskers project beyond the cheek and the ears meet the crown
        # edge to edge. Retain the painted tips and the two joining vertices.
        origin = obj.matrix_world.translation
        x = xy[:, 0] + (left + right) / 2 + (origin.x - cx) / units
        y = -xy[:, 1] + (top + bottom) / 2 + (cz - origin.z) / units
        def outside_rect(x0, y0, x1, y1):
            return np.clip(np.maximum.reduce([x0 - x, x - x1, y0 - y, y - y1]) / 4, 0, 1)
        protect = outside_rect(65, 45, 98, 59) * outside_rect(140, 45, 171, 59)
        if obj['atlas_layer'] == 'head':
            protect *= outside_rect(32, 123, 54, 144) * outside_rect(185, 123, 209, 144)
        shifts *= protect[:, None]
    planar = np.tile(shifts * units, (2, 1))
    local = np.column_stack((planar[:, 0], np.zeros(2 * count), planar[:, 1])) @ np.linalg.inv(matrix).T
    for shape in (mesh.shape_keys.key_blocks if mesh.shape_keys else []):
        points = np.empty_like(basis)
        shape.data.foreach_get('co', points.ravel())
        shape.data.foreach_set('co', (points + local).astype(np.float32).ravel())
    mesh.vertices.foreach_set('co', (basis + local).astype(np.float32).ravel())
    mesh.update()
    return True


def main():
    scene = bpy.context.scene
    spec = json.loads(scene['wardrobe_export'])
    models = []
    painted = [o for o in scene.objects if o.get('atlas_layer') and o.type == 'MESH'
               and not o.library and o.data.uv_layers.get('Studio paint')]
    rims = {}
    if 'accessories' not in spec['name']:
        rims = {o.name: list(rim_samples(o, spec['frame'][-1])) for o in painted}
    for obj in painted:
        layer = obj['atlas_layer']
        # The blink uses two already faired, narrow ink strokes.
        if layer == 'eyelids' or obj.data.get('wardrobe_contour_faired'):
            continue
        detail = 'accessories' in spec['name'] or 'eye' in layer or layer in ('horns', 'stripes')
        neighbors = [p for name, rings in rims.items() if name != obj.name for p in rings]
        if smooth_object(obj, spec, 2.0 if detail else 5.4, neighbors):
            obj.data['wardrobe_contour_faired'] = True
            models.append(obj.name)
    if models:
        args = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
        output = str(Path(args[0]).resolve()) if args else bpy.data.filepath
        bpy.context.preferences.filepaths.save_version = 0
        bpy.ops.wm.save_as_mainfile(filepath=output, compress=True, relative_remap=False)
    print('Faired painted shells:', len(models), ', '.join(models), flush=True)


if __name__ == '__main__':
    main()
