# Blender dragon

The fourth Blender companion follows the owner's lavender dragon reference sheet
and existing dragon screenshot: violet skin, cream ridged horns,
ear frills, glossy violet eyes, a rounded snout, pink cheeks, membrane wings,
short arms, a plated cream belly, clawed feet and a golden leaf-shaped tail tip.

The robot, cat and dog models, twelve atlases, cat/dog rigs and three render
scripts retain their pre-task SHA-256 hashes. The shared rig selector adds only
DRAGON; the robot drawing function is otherwise byte-for-byte unchanged. Other
companions, application identity and debug preview initialization are unchanged.
No dependencies or toolchain versions changed.

## Deliverables and reproduction

- `art/blender/dragon.blend`: normal Blender 5.3 file with one `Clocklings dragon`
  scene, 73 named objects, editable meshes/curves, materials, camera and studio lights.
- `tools/blender/render_dragon_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/dragon_blender_*.png`: four 1464 x 844 RGBA atlases.
- `BlenderDragonRig.kt`: seven layers on the existing 240 x 300 design grid.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-dragon/`: device screenshots and accessory/motion galleries.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_dragon_layers.py
```

This renders the saved model. Add `-- --rebuild` to replace it from the script,
or `-- --rebuild --preview` for one full preview. Intermediate renders use
`.scratch/blender-dragon/render`.

Separate head, horns, eyes, body, limbs and left/right wing layers retain head
tilt, blinking, wing flapping, breathing, sway, jump and outfit transitions.
Sparkle wings replace both original wings and flap around the existing shoulder
pivots. The knight helmet lets the horns emerge above it. Held items render
behind the hands; the royal cape renders behind the body. Four supported colors
(original lavender, blue, green, orange) and all 16 existing vector accessories
retain their IDs and wardrobe crops. The full frame includes 64 design units
above the character to leave room for tall headwear below the star badge.
This is fixed-view layered artwork, not a real-time 3D engine.

Visual refinements rounded the shoulders, extended the wing roots behind the
body and widened the neck so neckwear rests against the body during head tilt.

## Verification on 12 November 2026

1. Final full JVM suite: 505 passed, zero failures, errors or skips. This includes
   every companion's colors/items, independent dragon head/wing/blink motion,
   both replacement wings moving without moving the face, previous Blender
   companions, shared motion and debug preview state.
2. Final debug app/test assembly, `ktlintCheck`, `detekt` and `lintDebug` passed.
   Lint retains six existing warnings: four dependency suggestions, SVG text
   concatenation and the unchanged preview SharedPreferences call.
3. Seven final device test executions passed on dedicated API 26 AVD
   `ClocklingsBlenderDragon26`, serial `emulator-5592`: wardrobe flow at
   411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale
   (1080/840 pixels at 420 dpi). Each changes all four colors, equips the knight
   helmet and sparkle wings, recreates the activity and checks retained color
   and wing selection. All four wardrobe screenshots were inspected; large-font
   controls remain reachable by scrolling.
4. The final 16-item/four-color gallery and animation strip were inspected.
   Normal spin, jump and idle produce different frames; animator duration scale
   0 keeps identical pixels. Only the selected wing pair renders. Helmets/horns,
   hats, held book/shield, necklaces, cape, coin pouch and cloud remain visible.
5. Normal debug launch selected the existing robot preview with 15 stars. The
   dragon was then selected through the actual picker. APK metadata identifies
   `fi.clocklings.blender`, launcher label `Clocklings Blender`.
6. All 28 final layer bounds/opacity assertions passed. The rebuilt model opened
   as one dragon scene with 73 objects. All 20 recorded robot/cat/dog file hashes
   matched their pre-task values. The robot source also matched after excluding
   the single new DRAGON selector line.

The first full JVM run had one failure in the unchanged robot's strict chest
pixel comparison. That test passed in isolation without changes, then passed in
the final full 505-test run. The transient cause was not established; no test
assertion was weakened and no robot artwork or drawing behavior was changed.

Final APK SHA-256:
`BB72A9E3BF6103C8A30E773EB0823A5475A250B3E0AEFC5034CD42A3E779F75F`.

Physical devices, API 36, TalkBack, real Play purchases, release builds and
owner-run `lc`/`sc` were not tested. No commit, push or pull request was made.

## Cleanup limitation

Task-owned Blender PID 18128 and emulator/helper PIDs 21104, 16688 and 39112
were stopped and verified absent; `adb devices` was empty. Background render
and Gradle sessions exited. Remaining Java processes predate this task. No
Windows desktop capture session was used; the Android layout helper stopped
with its emulator.

Automatic approval review rejected removal of the task-owned scratch directory
and AVD registration with `blocked by policy`. Their exact paths, ancestors and
recursive scratch inventory were checked for reparse points. They contain this
task's disposable emulator, intermediate renders and logs. Earlier tasks' files
and existing build directories were preserved. The model, final APK and seven
verification images remain in their delivery locations.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-dragon' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderDragon26.ini' -Force
```
