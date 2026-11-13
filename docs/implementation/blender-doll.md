# Blender doll

The ninth companion, `HUMAN`, uses an editable Blender model and a transparent
layer atlas. The source references are `Sunny-haired girl doll turnaround sheet-2.png`
and `Clocklings Girl in Turquoise Overalls-8.png` from the owner's reference folder.
The front view preserves the existing 200 x 340 doll coordinate system.

The model includes the rounded face, brown eyes, side-swept fringe, tied pigtails,
yellow dress with white collar and orange hem, cream shoes, and turquoise overalls.
It also includes the existing golden, pink-curly and rainbow hair choices,
polka-dot, star and rainbow dresses, red shoes and purple boots.

## Files and reproduction

- `art/blender/doll.blend`: editable scene and all modeled variants.
- `tools/blender/render_doll_layers.py`: model generation and layer rendering.
- `app/src/main/res/drawable-nodpi/doll_blender.png`: 2048 x 1664 RGBA atlas.
- `app/src/main/java/fi/clocklings/ui/doll/BlenderDollRig.kt`: Compose layer rig.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- `reports/blender-doll/`: final device verification images.

Run from this repository:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_doll_layers.py
```

Add `-- --rebuild` to recreate the model. Add `-- --preview` to render the saved
default outfit, or `-- --preview --overalls` for the reference overalls.
Intermediate files use `.scratch/blender-doll/render`.

The 23 layers retain separate eyes and left-arm motion. Whole-body sway,
breathing, jump, outfit spin and reduced motion use the existing animation code.
All 18 wardrobe IDs, item crops, unlock rules and saved choices remain unchanged.
The doll continues to have fixed colors rather than a body-color selector.
Existing headwear, wings, strawberry bag and roller skates reuse the vector
artwork extracted from `DollRig`; the legacy vector rig still uses those helpers.
This is fixed-view layered artwork, not a real-time 3D engine.

The shared selector adds only `HUMAN`. The eight previous characters' 55 model,
render script, atlas and individual rig files matched their task-start SHA-256
hashes after integration. No dependency, toolchain, application identity or
release-signing configuration changed.

## Validation

All 23 layer crop and opacity checks passed on 12 November 2026. The default
and overalls previews and the complete atlas were visually inspected.

The final command passed:

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:ktlintCheck :app:detekt :app:lintDebug
```

All 514 JVM tests passed with no failures, errors or skips. The two new pixel
tests cover independent blinking/waving, all paid wardrobe choices, and complete
replacement of the dress hem by overalls. An initial assertion incorrectly
expected a transparent screenshot background; it now compares against the actual
background and verifies the probe intersects the default hem. The first full run
also failed the unchanged car wheel-hub and unicorn hoof pixel assertions. Both
passed unchanged in the final full run; their first failures' cause was not established.

Ktlint, detekt and Android lint passed. Lint retained six warnings in unchanged
code/configuration: one SVG string warning, four version suggestions, and one
SharedPreferences KTX suggestion. No dependency upgrade or test-framework migration
was made.

The dedicated API 26 emulator `ClocklingsBlenderDoll26` (`emulator-5596`) passed
seven instrumentation test executions at 420 dpi:

| Configuration | Checks | Result |
| --- | --- | --- |
| 411 dp, 100% font | Outfit persistence, motion, 18-item gallery | 3 passed |
| 320 dp, 100% font | Outfit persistence and scrolling | 1 passed |
| 411 dp, 200% font | Outfit persistence and scrolling | 1 passed |
| 320 dp, 200% font | Outfit persistence and scrolling | 1 passed |
| 411 dp, 100% font, reduced motion | Spin/jump/idle remain still | 1 passed |

The persistence test changes all hair styles, equips overalls, crown, roller skates
and strawberry bag, recreates the activity, and verifies the saved choices. It
also checks that the doll has no body-color selector. The four wardrobe captures,
gallery, motion strip and normal debug companion-picker screenshot were visually
reviewed. At 320 dp / 200% font, the existing star badge overlaps part of the crown;
the shared wardrobe layout was not changed. Other content remains scrollable.

The APK identifies as `fi.clocklings.blender`, version `1.0` (1), launcher
`Clocklings Blender`, and is 45,358,337 bytes. Its SHA-256 is
`EE4742671774F3ECD2CA9847AF9170FA77979E0D269F56D21EE55EECE1F12695`.
The editable model SHA-256 is
`AEA35181EB474E8CCA8708F47A370D2294A7A546FC068F2B4E86AB530267C554`.

No physical-device, API 36, real Play purchase, TalkBack, owner-run `lc`/`sc`,
or release APK validation is claimed. No commit or push was made.

## Cleanup

The dedicated emulator and its recorded children (24544, 39012, 37204, 37284)
exited, and `adb devices` was empty. Gradle, Blender and Android CLI commands
exited. No desktop capture session was started. Seven final PNGs are retained
as visual verification evidence.

Automatic command approval rejected removal of `.scratch/blender-doll` and
`C:\Users\EmmaH\.android\avd\ClocklingsBlenderDoll26.ini` with `blocked by policy`.
Both task-owned paths remain. Their resolved paths, ancestors and scratch contents
were checked for reparse points; none were present. Scratch contains approximately
9.71 GB of emulator data, temporary renders and logs. Neither path contains the
retained APK, source atlas, editable model or final verification evidence.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\blender-doll' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsBlenderDoll26.ini' -Force
```
