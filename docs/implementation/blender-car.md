# Blender car

The eighth Blender companion follows the owner's
`Friendly orange sports car character sheet-9.png`,
`Orange sports car expression sheet-11.png` and existing `09-car.png` app
screenshot. The low orange sports car has a rounded body, raked cabin, blue
glazing, oval mirrors, rear spoiler, headlight eyes, a smile, dark intakes and
cream five-spoke rims in dark plum tires.

The lower shell is built from longitudinal cross sections. After the owner's
marked-image correction, the entire cabin was rebuilt from curved panels with
shared three-dimensional boundary curves: a taller windscreen, larger side
window, narrow roof rim and rounded roof. The cabin and lower body are joined
and voxel-remeshed. The mirrors have larger rounded housings with stems rooted
in the cabin surface. The spoiler sits behind the cabin with revised geometry
and supports. Darker curved glass replaces the old shallow pane and bright
horizontal reflection band.

The wheel arches are actual mesh cuts. Glass and headlight surfaces follow the
body, and the rims have separate ring, spoke and hub meshes. Wheel geometry,
headlight eyes, smile, item anchors and all Kotlin code are unchanged by the
cabin correction.
The fixed three-quarter camera fits the established 240 x 300 design grid.

## Deliverables and reproduction

- `art/blender/car.blend`: normal editable Blender 5.3 file with one
  `Clocklings car` scene and 47 named objects, meshes, curves, materials,
  an orthographic camera and studio lighting.
- `tools/blender/render_car_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/car_blender_*.png`: four 715 x 765 RGBA atlases
  in original orange, blue, green and purple.
- `BlenderCarRig.kt`: body, wheels and two independent pupil layers.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-car/`: device screenshots and accessory/motion galleries.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_car_layers.py
```

This renders the saved model. Add `-- --rebuild` to recreate the editable model,
or `-- --rebuild --preview` for a full render. Temporary output uses
`.scratch/blender-car/render`.

The car retains its body-rock motion around the wheels, blinking, whole-body
sway, breathing, jump and outfit spin. All 16 existing accessories and their
IDs, roof/bumper/hub anchors and thumbnail crops are reused. A wider full-art
frame includes the roof flag, rocket flame and snowplow. The original vector
car rig and item artwork are unchanged. This is layered fixed-view artwork,
not a real-time 3D engine.

Only CAR is added to the shared rig selector. The seven previous characters,
application identity, debug preview setup, dependencies and Kotlin/Compose
toolchain are preserved.

## Cabin correction validation on 2026-11-12

After the cabin rebuild, the following Gradle command completed successfully:
`./gradlew.bat --no-daemon :app:testDebugUnitTest --tests fi.clocklings.ui.companion.BlenderCarRenderingTest :app:assembleDebug :app:assembleDebugAndroidTest`.
Both targeted JVM pixel tests passed with zero failures, errors or skips.
They cover body rock/blink with stationary wheel hubs and replacement sport
rims. The original car integration passed 512 JVM tests, ktlint, detekt and
lint before this artwork correction; those broader checks were not rerun.
Application source and dependency configuration did not change in this correction.

The dedicated API 26 emulator `ClocklingsBlenderCar26` (`emulator-5596`)
passed seven instrumentation test executions:

| Configuration | Checks | Result |
| --- | --- | --- |
| 411 dp, 100% font | Color/outfit persistence, motion, 16-accessory gallery | 3 passed |
| 320 dp, 100% font | Color/outfit persistence and scrolling | 1 passed |
| 411 dp, 200% font | Color/outfit persistence and scrolling | 1 passed |
| 320 dp, 200% font | Color/outfit persistence and scrolling | 1 passed |
| 411 dp, 100% font, reduced motion | Spin/jump/idle remain still | 1 passed |

The four wardrobe captures, accessory gallery, motion strip and normal app
companion-picker screenshot were visually reviewed. Original, blue, green and
purple render atlases passed all 16 layer
crop/opacity checks. The seven previous Blender characters' 48 model, render
script, atlas and rig files match their task-start SHA-256 hashes. The shared
selector changes only by adding CAR.

The APK identifies as `fi.clocklings.blender`, version `1.0` (1), launcher
`Clocklings Blender`, and is 42,811,439 bytes. SHA-256:
`432E8141B7E55709BBF8BF32A4479541168963D2BBE7D7EF603214824A0E7259`.
The editable model SHA-256 is
`F0F88AEE27C610851E0E6B75FEA7930F1735FB011207F946715A26D550192D8C`.

No physical-device testing, `lc` or `sc` run is claimed. No commit, push,
dependency upgrade or release signing change was made.

## Cleanup

The dedicated emulator and all identified task-owned child processes exited.
The Gradle and Blender commands also exited. No desktop capture session was
started. The seven retained PNGs are the final visual verification evidence.

Automatic command approval rejected removal of `.scratch/blender-car` and
`C:\Users\EmmaH\.android\avd\ClocklingsBlenderCar26.ini` with `blocked by policy`.
Both current-task-owned paths remain. Their resolved paths, ancestors and
scratch contents were checked for reparse points; none were present. The
scratch directory contains about 9.75 GB of emulator data, temporary renders
and logs. Neither location contains the retained APK or editable model.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-car' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderCar26.ini' -Force
```
