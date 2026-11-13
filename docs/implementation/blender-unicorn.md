# Blender unicorn

The seventh Blender companion follows the owner's
`White unicorn character reference sheet-8.png` and existing `08-unicorn.png`
app screenshot. It has a pearl-white coat, a cream muzzle, a smiling face,
glossy violet eyes, pink cheeks, pointed ears, a golden spiral horn, three pastel
mane and tail strands, four legs and violet hooves.

The body, neck and four legs are joined and voxel-remeshed into one smooth coat.
The head, eyes and tail remain independent animation layers. The existing
240 x 300 design grid, wardrobe anchors, item IDs and all 16 vector accessories
are reused. Horn rings align to the golden horn, boots cover the four original
hoof rectangles, and the cape, rainbow and wings retain their front/rear layers.
Head tilt, blinking, tail sway, breathing, jump and outfit spin use the existing
Compose motion. This is fixed-view layered artwork, not a real-time 3D engine.

## Deliverables and reproduction

- `art/blender/unicorn.blend`: normal editable Blender 5.3 file, one
  `Clocklings unicorn` scene, 45 named objects, meshes, curves, materials,
  orthographic camera and studio lights.
- `tools/blender/render_unicorn_layers.py`: model generation and layer rendering.
- `app/src/main/res/drawable-nodpi/unicorn_blender_*.png`: four RGBA atlases,
  845 x 866 pixels, in original white, blue, green and purple.
- `BlenderUnicornRig.kt`: four atlas layers and existing wardrobe rendering.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-unicorn/`: device screenshots and accessory/motion galleries.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_unicorn_layers.py
```

This renders the saved model. Add `-- --rebuild` to recreate the model from the
script, or `-- --rebuild --preview` to create a full-character preview. Temporary
render files use `.scratch/blender-unicorn/render`.

Only UNICORN is added to the shared rig selector. The six earlier characters,
their render scripts, models and atlases are preserved, as are the original
unicorn vector rig and accessories. The Kotlin/Compose architecture, toolchain,
dependencies, application identity and debug preview initialization are unchanged.

## Verification on 2026-11-12

The final Gradle invocation passed in 4 minutes 29 seconds with `--no-daemon`:

```text
:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
:app:ktlintCheck :app:detekt :app:lintDebug
```

All 510 JVM tests passed with zero failures, errors or skips. The two new tests
verify head tilt, tail sway and blinking independently of resting hooves, return
to the resting pose, and sparkle boots covering all four hooves. Lint retained
the six existing warnings: AndroidGradlePluginVersion, two GradleDependency,
NewerVersionAvailable, TextConcatSpace and UseKtx.

Seven instrumentation test executions passed on dedicated API 26 AVD
`ClocklingsBlenderUnicorn26` (`emulator-5594`). The wardrobe flow passed at
411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale.
It changes colors, equips horn rings and sparkle boots, recreates the activity
and checks retained color and boot selection. The remaining executions cover
all 16 accessories, normal outfit spin/jump/idle motion and disabled system
animations. The four wardrobe screenshots and accessory/motion galleries were
visually inspected, including horn-ring alignment and all four boots.

Normal launcher startup retained the robot preview and 15 stars. Selecting
Unicorn displayed the new white model; `unicorn-in-app.png` records that view.

All 16 atlas regions passed opacity and crop checks after expanding the initial
tail crop to include its lilac crest. Blender reopened the saved file and
reported one scene and 45 objects. All 41 protected files for the robot, cat,
dog, dragon, corrected dinosaur and bunny retained their baseline SHA-256 hashes.
Removing the single new UNICORN selector line reproduces the previous shared
`BlenderRobotRig.kt` source after newline normalization.

APK metadata confirms `fi.clocklings.blender` and launcher label
`Clocklings Blender`. APK SHA-256:
`9D344063283CAA144BDC4638E5A6BC17E7B25535938172854481A63ED0BA9866`.
Model SHA-256:
`9FB248BCB847887978DDFCFE4F6839D754FDD640BF629136A9CFD6ACC52C4A31`.

No API 36 device, physical device, TalkBack, real Play purchase or release signing
was tested. The owner's `lc` and `sc` checks were not run. The original projects
and their installed application data were not modified. No commit or push was made.

## Cleanup

The temporary Blender commands and Gradle processes exited. The dedicated
emulator was shut down, `adb devices` was empty, and the final process check found
none of its identified PIDs or shutdown helpers. No Windows screen-capture
session was used.

Automatic approval review rejected the bounded cleanup command with
`blocked by policy`. The task-owned scratch directory and its AVD registration
remain. Their exact paths and ancestors were checked; no reparse points were
found in the scratch tree. The following removes only these disposable artifacts,
preserving the model, APK, source assets and seven verification images:

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-unicorn' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderUnicorn26.ini' -Force
```
