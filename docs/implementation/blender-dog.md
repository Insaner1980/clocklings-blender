# Blender dog

The third Blender companion follows the owner's caramel puppy reference sheet
and existing dog screenshot: long chocolate floppy ears, a right-eye patch,
cream muzzle and chest, glossy eyes, pink tongue, seated paws and an upturned tail.
The original robot and cat models, eight atlases, cat rig and both rendering
scripts have unchanged SHA-256 hashes. The shared rig selector adds only DOG;
robot drawing code, cat drawing code, other companions and preview setup remain
unchanged. No dependencies or toolchain versions changed.

## Deliverables and reproduction

- `art/blender/dog.blend`: normal Blender 5.3 file, one `Clocklings puppy` scene,
  49 named objects, editable meshes/curves, materials, camera and studio lights.
- `tools/blender/render_dog_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/dog_blender_*.png`: four 1112 x 880 RGBA atlases.
- `BlenderDogRig.kt`: six layers on the existing 240 x 300 design grid.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-dog/`: seven inspected screenshots, including all 16 items.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_dog_layers.py
```

This renders the saved model. Add `-- --rebuild` to replace it from the script,
or `-- --rebuild --preview` for one complete preview. Intermediate renders use
`.scratch/blender-dog/render`.

The body, head, eyes, tail and two ears are separate layers. Compose retains head
tilt, blinking, right-ear movement, tail wagging, breathing, sway, jump and outfit
transitions. Reduced motion uses the existing system setting. Four colors and
all 16 existing vector accessories retain their IDs and wardrobe crops. The full
frame adds 64 design units above the model to keep tall hats below the star badge
at narrow widths and large font sizes. This is fixed-view layered artwork.

## Verification on 12 November 2026

1. Full JVM suite: 503 passed, no failures, errors or skips. This includes all
   companion colors/items, independent dog head/ear/tail/blink movement, existing
   robot/cat rendering, shared motion and debug preview state.
2. `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintCheck`,
   `:app:detekt` and `:app:lintDebug` passed. Lint has the same six existing
   warnings: four dependency suggestions, SVG text concatenation and the debug
   preview SharedPreferences call.
3. Seven device test executions passed on dedicated API 26 AVD
   `ClocklingsBlenderDog26`, serial `emulator-5592`. Wardrobe flow passed at
   411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale
   (1080/840 pixels at 420 dpi). Each changes all colors, equips party hat and
   frisbee, recreates the activity and checks retained color/frisbee selection.
   All four final wardrobe images were inspected; controls remain scrollable.
4. The 16-item/four-color gallery and animation strip were inspected. Normal
   animation changes across spin, jump and idle; animator duration scale 0 keeps
   identical pixels. Hats, boots, backpack, cape and mouth-held frisbee fit.
5. Normal debug launch showed the unchanged robot preview and 15 stars. The dog
   was then selected through the real picker; `dog-in-app.png` records the result.
   APK identity is `fi.clocklings.blender`, launcher label `Clocklings Blender`.
6. All 24 layer bounds/opacity assertions passed. The saved model reopened in
   Blender with its single puppy scene. All 13 recorded robot/cat file hashes
   matched their pre-task values.

APK SHA-256:
`7EB0AE944876F0522E3E0CDABF291F85DA4C20A4E5C49ED8268DB345F2A1C39C`.

Physical devices, API 36, TalkBack, real Play purchases, release builds and
owner-run `lc`/`sc` were not tested. No commit, push or pull request was made.

## Cleanup limitation

Blender PID 39928 and dedicated emulator/helper PIDs 39788, 29012 and 13152
were stopped and verified absent; `adb devices` was empty. Gradle exited, and
remaining Java processes predate this task. No Windows desktop capture session
was used. The Android layout helper stopped with its emulator.

Automatic approval review rejected removal of the task-owned scratch directory
and AVD registration with `blocked by policy`. These contain only this task's
disposable emulator, intermediate renders and logs. Exact paths, ancestors and
the recursive scratch inventory were checked for reparse points. Existing build
directories and earlier tasks' files were preserved. The model, APK and seven
verification images were confirmed present separately.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-dog' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderDog26.ini' -Force
```
