# Reference accessories, 13 November 2026

The owner's `16433.jpg` is the visual reference for the bunny, unicorn, car and
doll wardrobes. The new geometry was built in Blender 5.3 through Blender MCP.
Cycles rendered the transparent sprites locally. No generative image service,
external asset library or paid generation was used.

## Delivered artwork

- `art/blender/{bunny,unicorn,car,doll}_accessories.blend`: four editable scenes
  including the existing character as a fitting reference. Each scene previews
  its first accessory; the other choices are named mesh/curve objects, hidden
  until selected by their `accessory` property.
- `tools/blender/render_reference_accessories.py`: model generation, rendering,
  packing and reference contact-sheet compositing.
- Four `*_accessories_blender.png` atlases in `app/src/main/res/drawable-nodpi`,
  JSON coordinate manifests beside the models, and generated Kotlin sprite
  coordinates in `BlenderAccessoryRegions.kt`.
- `reports/reference-accessories/{bunny,unicorn,car,doll}.png`: all 66 wardrobe
  appearances composited from the exported app images in resting layer order.
- `app/build/outputs/apk/debug/app-debug.apk`: the installable debug application.

There are 54 newly modeled choices: 16 bunny, 16 unicorn, 16 car and six doll
accessories. The doll's 12 existing Blender hair, dress and shoe choices are
retained. Existing character models, body atlases, item IDs, unlock order,
saved outfits, toolchain and application identity are preserved.

The reference's pearl necklace replaces the bunny's daisy artwork. Its pink
star wand replaces the bouquet, and the unicorn's gold star wand replaces the
star balloon. Their displayed names are updated in all seven languages while
their saved IDs remain compatible. The eighth bunny in the image has no visible
accessory; its existing watering-can choice is retained as a modeled blue can.
This is an interpretation of missing reference detail.

## Integration and visual bounds

The four Blender rigs draw the new sprites instead of the corresponding vector
accessories, including rear cape and wing layers. Headwear follows head tilt;
wheel overlays follow the wheel layer. Bunny paws close over held objects.
The doll's skates replace its shoes. Thumbnail crops grow to include accessory
geometry. Car and doll full frames include the tallest/widest new pieces.

Render checks require every layer to be nonempty with no pixels clipped at the
image edge. Visual review corrected the cloud's lower contour, car camera scale,
painted race stripes, crown points, upward unicorn feathers and doll crown margin.
The race stripes conform to the car surface, retaining its smile above the paint.

This is modeled, fixed-view artwork fitted to the existing characters. It is not
a pixel-identical reproduction of the supplied image. Existing character
proportions and camera views remain those of this experiment.

## Reproduction

Run from this repository, replacing `bunny` with `unicorn`, `car` or `doll`:

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe' --background --factory-startup --threads 8 --python-exit-code 1 --python tools/blender/render_reference_accessories.py -- bunny
```

Append `--rebuild` to regenerate geometry first; otherwise the saved model is
rendered. Temporary renders use `.scratch/reference-accessories`. The renderer
writes the matching atlas, JSON manifest and Kotlin coordinates. Model generation
may also be called as `build('bunny')` from this module in Blender MCP.

The Blender MCP viewport preview stalled once. The task-owned process was stopped
and restarted; saved models and PNGs were used for the remaining visual review.
No Windows desktop capture session was started.

## Validation

- `:app:testDebugUnitTest`: 515 tests executed, zero failures, errors or skips.
  The added pixel regression checks that a modeled tiara follows head tilt while
  rear wings remain attached to the body. Existing tests cover every wardrobe
  choice, independent motion, held-carrot occlusion and all four unicorn boots.
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintCheck` and
  `:app:detekt` passed. The first gate found formatting and complexity issues
  in the new code; these were corrected before the successful full run.
- `:app:lintDebug` passed with six existing warnings: AndroidGradlePluginVersion,
  two GradleDependency, NewerVersionAvailable, TextConcatSpace and UseKtx.
- Dedicated API 26 AVD `ClocklingsReferenceAccessories26`, serial
  `emulator-5598`, 420 dpi: 28 instrumentation test executions passed.

| Device configuration | Checks | Passed |
| --- | --- | --- |
| 411 dp / 100% font | Outfit selection and recreation, all four characters | 4 |
| 320 dp / 100% font | Outfit selection, scrolling and recreation | 4 |
| 411 dp / 200% font | Outfit selection, scrolling and recreation | 4 |
| 320 dp / 200% font | Outfit selection, scrolling and recreation | 4 |
| 411 dp / 100% font | Complete four wardrobe galleries | 4 |
| Normal system animation | Spin, jump and idle | 4 |
| Reduced system animation | Static spin, jump and idle | 4 |

The four device-layout composites and complete device gallery were visually
reviewed. At large fonts the existing wardrobe scrolls; the tested selections
remain reachable. Evidence is in `reports/reference-accessories/device-*.png`.
The 54 accessory choices produced 57 nonempty, unclipped front/rear sprites.
All four saved Blender files reopened successfully with their expected 16/16/16/6
choice counts. The three renamed resource labels are present exactly once and
nonempty in all seven release language resource files.

The APK identifies as `fi.clocklings.blender`, launcher `Clocklings Blender`,
version 1.0 (1), 48,883,145 bytes. SHA-256:
`33C3C2A83EEF59C508A159644A2C041D456C33FB2D318C8970A3265704C3E6BD`.

No physical-device, API 36, TalkBack, real Play purchase, release APK, or owner-run
`lc`/`sc` validation is claimed. No commit, push or pull request was made.

## Cleanup

The dedicated emulator and both task Blender sessions exited. Their identified
processes and descendants were absent, and `adb devices` was empty. Temporary
Gradle/Blender commands finished. The owned `.scratch/reference-accessories`
directory, dedicated AVD registration and new Python bytecode were removed after
checking exact paths and reparse-point boundaries. Existing scratch directories
from other work were preserved. The four models, four atlases, coordinate files,
APK, nine review PNGs and this note are retained.
