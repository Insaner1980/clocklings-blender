# Blender bunny

The sixth Blender companion follows the owner's
`Cream lop-eared bunny character sheet-5.png` and existing `07-bunny.png` app
screenshot: warm cream fur, asymmetric long lop ears with pink interiors,
a forehead tuft, glossy plum eyes, rosy cheeks, a pale muzzle, two front teeth,
a seated body, softly pale paws, pink sole pads and a round tail.

The robot, cat, dog, dragon and corrected dinosaur models, twenty atlases,
cat/dog/dragon/dinosaur rigs and five render scripts retain their pre-task
SHA-256 hashes. The shared rig selector adds only BUNNY; the robot drawing
function is otherwise byte-for-byte unchanged. No dependencies, toolchain,
application identity or debug preview initialization changed.

## Deliverables and reproduction

- `art/blender/bunny.blend`: normal Blender 5.3 file with one `Clocklings bunny`
  scene and 54 named objects, editable meshes, curves, materials, camera and lights.
- `tools/blender/render_bunny_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/bunny_blender_*.png`: four 992 x 1028 RGBA atlases.
- `BlenderBunnyRig.kt`: six layers on the existing 240 x 300 design grid.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-bunny/`: device screenshots and accessory/motion galleries.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_bunny_layers.py
```

This renders the saved model. Add `-- --rebuild` to replace it from the script,
or `-- --rebuild --preview` for a full preview. Disposable intermediate renders
use `.scratch/blender-bunny/render`.

The head, eyes, body, arms and two separate ears retain head tilt, blinking,
outward ear sway, breathing, jump and outfit transitions. The paws are redrawn
over held items only below design y = 224, preserving visible carrot leaves,
flowers and the watering-can handle. Cape and fairy wings retain their rear
layers. All 16 existing vector accessories, IDs and crops are reused. Four
colors remain available: original cream, blue, green and purple. The full frame
includes 80 design units above the character for tall headwear below the star
badge. This is fixed-view layered artwork, not a real-time 3D engine.

## Verification on 2026-11-12

The final Gradle invocation passed with `--no-daemon`:

```text
:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
:app:ktlintCheck :app:detekt :app:lintDebug
```

All 508 JVM tests passed with zero failures, errors or skips. The two new bunny
rendering tests cover head tilt, both ears, blinking, unchanged resting paws and
held-carrot occlusion. Lint retains six existing warnings; there are no new lint
errors. No dependency or toolchain changes were made.

Seven instrumentation test executions passed on the dedicated API 26 AVD
`ClocklingsBlenderBunny26` (`emulator-5592`). The wardrobe flow passed at all four
width/font combinations: 411 dp / 100%, 320 dp / 100%, 411 dp / 200% and
320 dp / 200%. It changes colors, equips a top hat and carrot, recreates the
activity and checks retained selections. Two further executions cover the
16-item gallery and normal animation; one covers disabled system animations.
Device screenshots of all four layouts, all accessories and motion frames were
visually inspected. Normal launcher startup retained the robot preview and
15 stars, and selecting Bunny displayed the new cream model.

All 24 atlas regions passed crop/opacity checks. The saved Blender file was
reopened and checked for its one scene and 54 objects. The 34 protected
pre-existing character files matched their baseline SHA-256 hashes after the
final build. Removing the single new BUNNY selector line reproduces the previous
`BlenderRobotRig.kt` source exactly after newline normalization.

The debug APK identifies itself as `fi.clocklings.blender` with the launcher
label `Clocklings Blender`. Its SHA-256 is
`E358912FF267080CBF614CAB7A8300F2AB187A2CDD6CBF6A9C6C0ED9C03EEF21`.
The bunny model SHA-256 is
`E5578C8ED5A5EFAE7407C3EDA074137E518F3D0226B3D52CDC40F7C55B7A9959`.

API 36, physical devices, TalkBack, real Play purchases and release signing were
not tested. The owner's `lc` and `sc` checks were not run. The original Clocklings
projects and their installed application data were not modified.

## Cleanup

The task's Blender process, temporary Gradle processes, dedicated emulator and
its remaining shutdown helpers exited. The final process check found none of
their identified PIDs or descendants; `adb devices` was empty. No Windows
screen-capture session was used.

Automatic approval review rejected the bounded `Remove-Item` cleanup command
with `blocked by policy`. The task-owned scratch directory and its AVD
registration therefore remain. Their paths and ancestors were checked and no
reparse points were found in the scratch tree. The following commands remove
only those disposable artifacts, preserving the APK, model and seven evidence
images:

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-bunny' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderBunny26.ini' -Force
```
