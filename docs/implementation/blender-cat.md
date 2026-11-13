# Blender cat

The second Blender companion follows the owner's orange tabby kitten reference:
a seated body, oversized rounded head, cream muzzle and chest, forehead stripes,
glossy brown eyes, pink ears and a curled cream-tipped tail. It replaces only the
cat's renderer in this independent application. The approved robot model and
atlases, other characters, application identity and debug preview initialization
remain unchanged. No dependency or toolchain changes were needed.

## Files and reproduction

- Editable model: `art/blender/cat.blend`, a normal Blender 5.3 file with a single
  `Clocklings kitten` scene, 55 named objects and nine materials.
- Script: `tools/blender/render_cat_layers.py`.
- Four RGBA atlases: `app/src/main/res/drawable-nodpi/cat_blender_*.png`.
- Integration: `BlenderCatRig.kt`, selected by `rememberCompanionRig`.
- Installable debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Device evidence: `reports/blender-cat/`.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_cat_layers.py
```

This renders the editable model. Add `-- --rebuild` to regenerate the model from
the script, or `-- --rebuild --preview` to regenerate it and inspect one full
render before creating atlases. Rebuilding intentionally replaces `cat.blend`.
Intermediate files use `.scratch/blender-cat/render` and can be removed after use.

## Rendering and compatibility

Seven layers use the existing 240 x 300 design grid: head, eyes, ears, stripes,
body, front legs/paws and tail. Head tilt and blinking are independent of the
body; tail rotation points inward so its tip stays within the available width.
The body retains the shared breathing, sway, jump and outfit transition. As in
the existing seated cat, the front legs stay seated instead of using the robot's
arm-wave motion. Reduced motion continues through the existing system setting.

All four supported colors, 16 item IDs, selection identity, wardrobe crops and
saved progress are unchanged. Existing vector accessories follow the established
anchors: hats cover stripes and let ears emerge above the hat band, glasses sit
over the eyes, the wand goes behind the front paws, and cape/backpack/wings retain
their rear layers. This remains fixed-camera artwork, not a real-time 3D engine.
The full-character frame adds 48 design units above the model so the existing
star badge does not cover headwear at 320 dp and 200% font scale. Item thumbnail
crops and the surrounding screens are unchanged.

Each part is rendered without the other parts. The first device screenshots
exposed black occlusion marks from invisible geometry; rendering complete parts
removed those marks, including when ears are redrawn over a hat. The script checks
all 28 rendered regions for clipped or empty content.

## Verification

Completed on 12 November 2026:

1. Full JVM suite: 502 tests passed, zero failures/errors/skips. After the final
   rendering and framing refinements, all 19 targeted tests passed again:
   `CompanionRenderingTest`, `BlenderCatRenderingTest`, `BlenderRobotRenderingTest`,
   `CompanionMotionTest` and `BlenderPreviewTest`. These cover every companion's
   colors/items, independent head/tail/blink movement, motion and preview state.
2. Debug app/test APK assembly, `ktlintCheck` and `detekt` passed. `lintDebug`
   passed with six existing warnings: one SVG path concatenation, four dependency
   update suggestions and the unchanged debug preview's SharedPreferences call.
   No dependency upgrades were made. The last change after lint was only the
   full-character frame's top coordinate; assembly, static style checks, targeted
   JVM tests and the entire device matrix were repeated after that change.
3. The dedicated API 26 emulator `ClocklingsBlenderCat26`, serial `emulator-5592`,
   passed the wardrobe flow at 411 dp / 100%, 320 dp / 100%, 411 dp / 200% and
   320 dp / 200% (1080/840 pixels at 420 dpi). Every run changes all four colors,
   equips the hat/glasses, recreates the activity and checks retained selection.
   All four final screenshots were visually inspected. Large-font controls remain
   reachable by scrolling.
4. Device motion checks passed with animator duration scale 1 and 0. Normal
   animation changes the outfit, jumps and resumes idle; reduced motion keeps
   identical pixels across those timestamps. The normal frame strip and the
   16-item/four-color gallery were also visually inspected. There were seven
   passing device test executions in the final matrix, across three test methods.
5. A normal debug launch, outside instrumentation, showed the existing robot
   preview and 15 stars. The cat was then selected through the real picker and
   captured in `cat-in-app.png`. APK metadata confirms `fi.clocklings.blender`,
   launcher label `Clocklings Blender`. Release initialization was not changed.
6. All 28 layer bounds/opacity assertions passed. The editable file was reopened
   in Blender and contained its single kitten scene. Robot model SHA-256 remained
   `19C8EF068531C7E46CF35E98201BB0F8F3AADED943288368DC872E7306F7E409`.

Final APK SHA-256:
`739309C7DF49E79A0C647E286CA33F7B326619658C522BDFEF7CA7D49A40AF52`.

The retained screenshots are `cat-in-app.png`, four `wardrobe-<width>-<font>.png`
files, `accessories.png` (wardrobe order, four items per row) and `motion.png`
(initial, outfit spin, jump, idle). Earlier failed render attempts and screenshots
are not deliverables.

API 36 was not retried after the previously reported host QEMU crashes. Physical
devices, real Play purchases, TalkBack, release signing/builds and owner-run
`lc`/`sc` remain unverified. No commit, push or pull request was made; the branch
remains `codex/blender-robot`.

## Cleanup limitation

The task-owned Blender process (38168) and dedicated emulator processes (3236,
34636 and 20044) were stopped and verified absent. Gradle validation sessions
exited; the remaining Java processes predate this task. No CUA or Windows desktop
capture session was started. Screenshots came from Blender renders or the Android
emulator; its layout helper stopped with the emulator.

Automatic approval review rejected cleanup with `blocked by policy`, including
separate literal-path commands. The remaining disposable data is the task's
`.scratch/blender-cat` directory (AVD, render intermediates, earlier screenshots
and logs) and its AVD registration. Ancestor paths and the recursive scratch
inventory had no reparse points. Existing Gradle build directories were preserved.
The model, final APK and seven verification images remain in their delivery paths.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-cat' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderCat26.ini' -Force
```
