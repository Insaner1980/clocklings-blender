# Polished accessories and marked corrections, 13 November 2026

The app contains the 64 modeled accessories from the owner's polished reference
sheet. This correction pass rebuilt the nine accessories marked in
`device-gallery-edit.png`, using Blender 5.3 through Blender MCP and Cycles renders.

## Corrected models

| Item | Reference features implemented |
| --- | --- |
| Cat ruffle collar | Connected ivory fabric folds over a pink scalloped frill |
| Cat pearl necklace | Nine graduated ivory pearls on a curved hanging strand |
| Dog frisbee | Solid recessed orange disc with a continuous rounded raised rim |
| Dog backpack | Rounded red shell behind the shoulder, blue straps, gold buckles and side zipper |
| Dog raincoat | Flared front panels, sleeves, rolled hood collar, hem and two blue toggles |
| Dinosaur duck swim ring | Thick yellow tube surrounding the waist, rear occlusion, rounded duck head and orange bill |
| Dinosaur binoculars | Rounded dark blue barrels, large cyan lenses and flat neck straps |
| Dinosaur medal | Plain circular gold face, raised circular border and blue ribbon with white edges |
| Dinosaur ice cream | Tilted round cone with raised waffle lattice, blue lower scoop and pink upper scoop |

The dinosaur's far arm and grip now support the cone without hiding its waffle
surface. The selective renderer reuses existing atlas crops for unaffected items.
All 39 unmarked accessories in the cat, dog and dinosaur atlases were compared
against snapshots from before this correction pass: destination rectangles and
RGBA pixels are identical. The dragon artwork was unchanged.

The accessories fit the application's existing character proportions. The paired
comparison shows the supplied reference crops beside the new Blender renders so
that remaining visual differences can be assessed directly.

## Deliverables

- `app/build/outputs/apk/debug/app-debug.apk`: latest debug APK.
- `art/blender/{cat,dog,dinosaur}_accessories.blend`: updated editable scenes;
  each saved file was reopened or inspected successfully and has 16 item choices.
- `tools/blender/render_polished_accessories.py`: accessory geometry.
- `tools/blender/render_reference_accessories.py`: rendering, atlas packing and
  review composition, including selective reuse of unchanged sprites.
- Corresponding Android PNG atlases, JSON manifests and generated
  `BlenderAccessoryRegions.kt` coordinates.
- `reports/polished-accessories/reference-comparison.png`: nine pairs, reference
  on the left and corrected render on the right. Rows: ruffle/pearls/frisbee,
  backpack/raincoat/swim ring, binoculars/medal/ice cream.
- `reports/polished-accessories/accessory-sheet.png`: all 64 exported appearances.
- `reports/polished-accessories/device-gallery.png`: app captures for cat, dog and
  dinosaur, with the unchanged dragon panel retained from the earlier validation.
- `reports/polished-accessories/device-layouts.png`: actual wardrobe captures.
  Columns: cat, dog, dinosaur. Rows: 411 dp / 100%, 320 dp / 100%, 411 dp / 200%,
  320 dp / 200%. Dog and dinosaur captures use the final APK; cat captures use
  the identical corrected cat assets from the preceding build.

## Validation actually run for this correction

- Eight focused JVM tests passed, zero failures or errors: CatRendering (2),
  DogRendering (1), DinosaurRendering (2), CompanionRendering (3). This includes
  the new regression checking that the gripping paw does not hide the cone,
  existing independent-motion checks and the wardrobe rendering coverage.
- Debug APK and instrumentation APK assembly, ktlint, detekt and Android lint
  passed. Lint retained six existing warnings: TextConcatSpace,
  AndroidGradlePluginVersion, two GradleDependency, NewerVersionAvailable, UseKtx.
- After the final frisbee-rim-only render, both APK assemblies and ktlint passed
  again. The final device checks below used that exact APK.
- Dedicated API 26 AVD `ClocklingsPolishedCorrections26`, serial `emulator-5598`,
  density 420 dpi. The correction pass first passed 21 device tests: twelve
  outfit-selection/recreation tests across the four width/font configurations,
  three complete galleries, and six normal/reduced-motion tests.
- After the final dog/dinosaur geometry refinements, ten additional device tests
  passed: both characters at all four width/font configurations (8), plus their
  final complete galleries (2). The tests explicitly select all nine marked
  accessories across the relevant character suites.
- The final app gallery, paired reference comparison and layout captures were
  visually inspected. Large-font wardrobe content remains reachable by scrolling.
- No physical-device, TalkBack or owner-run `lc`/`sc` checks were performed.
  The full 516-test suite reported for the original accessory implementation was
  not rerun during this correction pass.

APK identity: `fi.clocklings.blender`, launcher `Clocklings Blender`, version 1.0
(1), minimum API 26, target API 37. Final size: 58,092,206 bytes. SHA-256:
`A83AC8D3EEC3FE3B5F783A085457F7415365147206B85C0279379E2755A7C63C`.

No commit, push or pull request was made. Branch: `codex/blender-robot`.

## Cleanup status

The task-owned Blender, renderer and dedicated emulator processes exited; their
recorded PIDs were verified absent. Gradle and instrumentation commands finished.
The pre-existing `emulator-5588` was preserved. No Windows capture session was
started. Two disposable review PNGs created during this pass were removed.

Automatic approval review rejected the remaining cleanup command with only
`blocked by policy`. The task-owned `.scratch/polished-accessories` directory
and `ClocklingsPolishedCorrections26.ini` AVD registration therefore remain.
Their exact paths, ancestor directories and nested entries were checked for
reparse points; none were present. They contain temporary render files, baseline
snapshots, test logs/captures and the dedicated disposable AVD, not deliverables.

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings-Blender-version\.scratch\polished-accessories' -Recurse -Force
Remove-Item -LiteralPath 'C:\Users\EmmaH\.android\avd\ClocklingsPolishedCorrections26.ini' -Force
```
