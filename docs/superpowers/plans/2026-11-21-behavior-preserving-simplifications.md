# Clocklings behavior-preserving simplifications

**Implementation plan | 21 November 2026**

**Goal.** Make the five improvements identified in the project-wide review while preserving clock answers, localized greetings, character artwork, and the pixels produced by the Blender exporters.

**Starting revision.** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` in `C:\Dev\Clocklings-Blender-version`. The file and symbol names below refer to that revision. The scope comes from the five findings reported to the owner on 21 November. The repository's [working instructions](../../../AGENTS.md) apply throughout.

**Approach.** Reuse the existing Kotlin game state and language default, delete one unused drawing function, and share two existing Python operations. The three Android tasks are independent. The two Blender tasks touch some of the same import lists, so apply them in order. Each task has its own acceptance check.

## Before editing

- [ ] From the repository root, run `git status --short --branch` and `git rev-parse HEAD`. Preserve existing edits. If the checkout has moved since the review, confirm the named callers and tests before applying the corresponding step. Work on a `codex/` feature branch.
- [ ] Follow the repository's `sonar analyze secrets` instruction before reading source, including any source loaded by the Blender verification commands. The Android test commands below use the repository's Gradle wrapper and `--no-daemon`.
- [ ] Run the focused Android test selections in Tasks 1–3 once against the starting code as a baseline. They can share a single Gradle invocation by combining the `--tests` filters. Run the scene-settings probe in Task 4 against the starting code as well. Record pre-existing failures separately so that an affected task can be assessed against its baseline.

## 1. Submit the same clock value that the game displays

**Edit** [GameViewModel.kt](../../../app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt). **Verify with** [GameViewModelTest.kt](../../../app/src/test/java/fi/clocklings/ui/game/GameViewModelTest.kt).

The private `clockValue` field has two writes and one read. Starting a SET task puts the same initial time into `clockValue` and `GameUiState.clock.time`. A drag writes the selected time to both. A reveal or success can move the displayed hands, but those transitions also set `busy = true`, which prevents another submission.

- [ ] Remove the `clockValue` declaration and its assignments in `taskState` and `onClockChange`. In `taskState`, keep `Levels.setTaskStart(task.time)` and the existing `ClockCommand` construction. In `onClockChange`, keep the `interactive` guard and the state update that sets `time = value` and `ms = 0`.
- [ ] In `done()`, take the existing `s = _state.value` snapshot and retain the early return for a busy session or a task other than SET. Read `val submittedTime = s.clock.time` after that guard. Pass it to both `ClockTime.sameHands(submittedTime, s.task.time)` and `Hints.forSet(s.task.time, submittedTime)`. The comparison must still accept identical hand positions twelve hours apart in 24-hour mode.
- [ ] Run the full view-model test class. Check that `recreatedClockUsesTheSameTimeAsTheSubmittedAnswer` passes, along with the cases for the initial SET position, a wrong answer followed by a correct one, the other half of the day, and the reveal after two mistakes. A literal search for `clockValue` in the production file should return zero matches.

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.game.GameViewModelTest'
```

## 2. Use the shared greeting rule for Finnish and Swedish

**Edit** [FinnishTime.kt](../../../app/src/main/java/fi/clocklings/domain/FinnishTime.kt) and [SwedishTime.kt](../../../app/src/main/java/fi/clocklings/domain/SwedishTime.kt). The existing [TimeLanguage.greeting](../../../app/src/main/java/fi/clocklings/domain/TimeLanguage.kt) delegates to `EnglishTime.greeting(total)`. Its return value is a `Greeting` enum. The intro screen uses the selected locale to resolve the greeting text.

- [ ] Delete only the `greeting(total: Int)` override from each of the two objects. Remove the Swedish private constant `LAST_MORNING_HOUR`, whose only caller was the removed override. Other constants, including Swedish `DECIMAL_BASE` and Finnish `LATE_MORNING_FROM`, still have callers.
- [ ] Retain the existing language-specific `dayPart` methods. They distinguish such cases as midnight, noon and late morning, and supply localized phrases used elsewhere.
- [ ] Verify the following reference table. Normalize the input to minutes within a 24-hour day using the existing implementation. The inclusive boundaries matter on both sides of midnight.

| Minute of day | Expected greeting |
|---|---|
| 0–299 | `NIGHT` |
| 300–599 | `MORNING` |
| 600–1079 | `DAY` |
| 1080–1319 | `EVENING` |
| 1320–1439 | `NIGHT` |

- [ ] Run the three existing classes below. In [TimeLanguageTest.kt](../../../app/src/test/java/fi/clocklings/domain/TimeLanguageTest.kt), `greetingsKeepTeachingBoundariesAcrossAllLanguages` checks the transitions, `everyFullDayMinuteHasDistinctContextAndNormalizesAcrossDays` exercises day offsets for every minute, and `dayPartNamesCoverEveryCategoryBoundaryAndAdjacentMinutes` guards the localized distinctions. The review separately compared the three source rules at all 1,440 minute values.

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.domain.TimeLanguageTest' --tests 'fi.clocklings.domain.FinnishTimeTest' --tests 'fi.clocklings.domain.SwedishTimeTest'
```

## 3. Remove the unused tail-drawing function

**Edit** [CompanionRig.kt](../../../app/src/main/java/fi/clocklings/ui/companion/CompanionRig.kt). **Verify with** [CompanionRenderingTest.kt](../../../app/src/test/java/fi/clocklings/ui/CompanionRenderingTest.kt).

- [ ] Run `git grep -n -w drawTailedBody -- '*.kt' '*.java'`. At the reviewed revision its only match is the protected declaration in the internal `AnimalRig` class. If a real caller has appeared, reassess the deletion against that caller.
- [ ] Delete `protected fun DrawScope.drawTailedBody(...)` and its now-unused `androidx.compose.ui.graphics.drawscope.rotate` import. Preserve `drawBodyWithAccessories` and the `rotate(pose.swayDeg, feet)` call inside `withTransform` in `posedBody`. That call uses the transform receiver's own rotation method.
- [ ] Repeat the symbol search. Zero matches and Git exit code 1 are expected. Compile the debug app and run the existing rendering class, including `everyCompanionAndWearableProducesVisibleDistinctArtwork`. Compilation checks the import removal. The rendering test exercises the active paths for characters and clothes.

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest --tests 'fi.clocklings.ui.CompanionRenderingTest'
```

## Blender file map for Tasks 4 and 5

Each row is one body-layer exporter under `tools/blender/`. The line numbers identify the existing implementation. Width and height describe an individual rendered frame before packing it into an atlas.

| Exporter | Task 4 settings | Task 5 pixel read | Frame width × height |
|---|---|---:|---|
| [render_bunny_layers.py](../../../tools/blender/render_bunny_layers.py) | `configure` at 352 | 389 | 720 × 900 |
| [render_car_layers.py](../../../tools/blender/render_car_layers.py) | `configure` at 472 | 501 | 720 × 900 |
| [render_cat_layers.py](../../../tools/blender/render_cat_layers.py) | `configure` at 259 | 292 | 720 × 900 |
| [render_dinosaur_layers.py](../../../tools/blender/render_dinosaur_layers.py) | `configure` at 347 | 381 | 720 × 900 |
| [render_dog_layers.py](../../../tools/blender/render_dog_layers.py) | `configure` at 226 | 259 | 720 × 900 |
| [render_dragon_layers.py](../../../tools/blender/render_dragon_layers.py) | `configure` at 279 | 313 | 720 × 900 |
| [render_unicorn_layers.py](../../../tools/blender/render_unicorn_layers.py) | `configure` at 365 | 395 | 720 × 900 |
| [render_doll_layers.py](../../../tools/blender/render_doll_layers.py) | Local `configure` at 563, distinct frame size | 593 | 600 × 1020 |
| [render_robot_layers.py](../../../tools/blender/render_robot_layers.py) | Top-level setup at 57, 48 samples and explicit 8-bit output | 106 | 720 × 900 |

## 4. Share the identical render settings used by seven characters

**Edit** [reference_finish.py](../../../tools/blender/reference_finish.py) and the seven exporters with a 720 × 900 `configure` function in the table. All seven already import from `reference_finish`.

- [ ] Move the existing `configure(scene)` body from the bunny exporter to `reference_finish.py`, next to `studio(scene)`. Keep the twelve assignments in their present order. Each value is pinned by the probe below.
- [ ] Replace each of the seven local definitions with `from reference_finish import configure` as part of its existing import. Keep the name `configure` available at module scope. [render_reference_accessories.py](../../../tools/blender/render_reference_accessories.py) calls `HELP.configure(SCENE)` after selecting a character module dynamically. [render_robot_accessories.py](../../../tools/blender/render_robot_accessories.py) calls the bunny module's `geometry.configure(scene)`.
- [ ] Run the same probe before and after the change. It imports the seven modules and the doll under their normal names, calls their public functions on fresh in-memory Blender scenes and checks all twelve settings. Importing the robot body exporter starts a render at module scope, so the probe covers its Task 5 wiring by source inspection instead.

Run the following from the repository root in PowerShell. Reuse `$blenderExe` for Task 5. A successful run prints `PASS: 8 scenes, 12 settings each` and exits with code 0.

```powershell
$blenderExe = 'C:\Program Files\Blender Foundation\Blender 5.3\blender.exe'
$sceneProbe = @'
import importlib
import sys
from pathlib import Path
import bpy

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path("tools/blender").resolve()))
names = ("bunny", "car", "cat", "dinosaur", "dog", "dragon", "unicorn", "doll")
for name in names:
    module = importlib.import_module(f"render_{name}_layers")
    scene = bpy.data.scenes.new(f"check-{name}")
    try:
        module.configure(scene)
        render = scene.render
        width, height = (600, 1020) if name == "doll" else (720, 900)
        actual = (render.engine, scene.cycles.device, scene.cycles.samples,
                  scene.cycles.use_denoising, render.resolution_x, render.resolution_y,
                  render.resolution_percentage, render.film_transparent,
                  render.image_settings.file_format, render.image_settings.color_mode,
                  render.threads_mode, render.threads)
        expected = ("CYCLES", "CPU", 40, True, width, height, 100, True,
                    "PNG", "RGBA", "FIXED", 8)
        assert actual == expected, (name, actual, expected)
    finally:
        bpy.data.scenes.remove(scene)
print("PASS: 8 scenes, 12 settings each")
'@
& $blenderExe --background --factory-startup --disable-autoexec --python-exit-code 1 --python-expr $sceneProbe
if ($LASTEXITCODE -ne 0) { throw 'Blender scene settings check failed' }
```

- [ ] Verify the final definitions using `git grep -n '^def configure(scene)' -- tools/blender/reference_finish.py 'tools/blender/render_*_layers.py'`. Expect one shared definition and one doll definition. Review the diff to confirm the robot's sampling, camera and output setup were outside this edit.

## 5. Read each Blender frame directly into its NumPy buffer

**Edit** [export_wardrobe.py](../../../tools/blender/export_wardrobe.py) and the pixel-read site in each of the nine exporters in the table. The existing `render_tile` already demonstrates direct reads with `image.pixels.foreach_get`. Its module can be imported without invoking its command-line entry point.

- [ ] Extract the following small function in `export_wardrobe.py` immediately before `render_tile`. The caller supplies the expected width and height. The returned array is an RGBA `float32` view with shape `(height, width, 4)` and top row first. The NumPy allocation owns the data and remains valid when the caller removes the Blender image.

```python
def read_layer_pixels(image, width, height):
    values = np.empty(len(image.pixels), dtype=np.float32)
    image.pixels.foreach_get(values)
    return values.reshape(height, width, 4)[::-1]
```

- [ ] In `render_tile` use `pixels = read_layer_pixels(image, *image.size)` in place of its original three read/reshape lines. Preserve its independent cropped copy, padding, alpha checks and `finally` cleanup. This keeps the existing direct reader and its nine new consumers on one implementation.
- [ ] Import `read_layer_pixels` from `export_wardrobe` in each body exporter after the local module path has been set. Replace the old `np.asarray(rendered.pixels[:], ...)[::-1].copy()` expression with `read_layer_pixels(rendered, 720, 900)`, using `read_layer_pixels(rendered, 600, 1020)` for the doll. Each script retains its existing `bpy.data.images.remove(rendered)` call. The fixed dimensions preserve the original reshape contract.
- [ ] Preserve the independent `outside = pixels[:, :, 3].copy()` used for clipping assertions. Clearing that alpha scratch array must have no effect on the source image. Preserve the robot's `shared` entries for `eyes` and `key` across color passes, the per-character clipping and empty-layer checks, the crop coordinates, channel order, and the final atlas write orientation.
- [ ] Search `git grep -n -F 'rendered.pixels[:]' -- 'tools/blender/render_*_layers.py'` for zero matches. Then use `git grep -n -F 'read_layer_pixels(rendered,' -- 'tools/blender/render_*_layers.py'` to verify one call in each of the nine files and the dimensions listed in the table. Accessory-only exporters have separate pipelines.
- [ ] Run the probe below after the helper exists. It compares the actual helper against the old expression, exercises both supported frame sizes and both Blender buffer types, and compares the arrays again after releasing each Blender image. The nonuniform RGBA fixture exposes row reversals and channel swaps. Comparison as `uint32` checks every bit of each `float32` channel.

```powershell
$pixelProbe = @'
import sys
from pathlib import Path
import bpy
import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path("tools/blender").resolve()))
from export_wardrobe import read_layer_pixels

for width, height in ((720, 900), (600, 1020)):
    for floating in (False, True):
        image = bpy.data.images.new("compare-rgba", width, height,
                                    alpha=True, float_buffer=floating)
        try:
            source = np.random.default_rng(19).random(width * height * 4, dtype=np.float32)
            image.pixels.foreach_set(source)
            old = np.asarray(image.pixels[:], dtype=np.float32).reshape(height, width, 4)[::-1].copy()
            new = read_layer_pixels(image, width, height)
            assert new.shape == old.shape and new.dtype == old.dtype
            assert np.array_equal(new.view(np.uint32), old.view(np.uint32))
        finally:
            bpy.data.images.remove(image)
        assert np.array_equal(new.view(np.uint32), old.view(np.uint32))
        print(f"PASS: {width}x{height}, float_buffer={floating}, {new.size} channels")
'@
& $blenderExe --background --factory-startup --disable-autoexec --python-exit-code 1 --python-expr $pixelProbe
if ($LASTEXITCODE -ne 0) { throw 'Blender pixel comparison failed' }
```

**Acceptance.** Four PASS lines, process exit code 0, and the expected nine call sites. For a 720 × 900 frame the probe compares 2,592,000 channels per case. For 600 × 1020 it compares 2,448,000. These checks establish exact pixel equivalence for the tested inputs. Timing and peak memory measurements are separate from that result.

## Final verification and handoff

- [ ] Run the full Android debug unit suite and build the debug APK from the repository root. Confirm both Gradle tasks succeed. Record the test count and any skipped cases from the actual run. If a result is cached, identify it as cached in the handoff.

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

- [ ] Review `git diff --check`, `git diff --stat` and `git status --short` against the starting state. The intended implementation touches four Kotlin production files and eleven Blender Python files. Check especially for accidental changes to `art/blender`, packaged Android drawables and test baselines.
- [ ] The five tasks require no layout or resource edits. If a UI change becomes necessary, treat it as a scope change and apply the project's UI verification on the dedicated test emulator at 320 dp width, 200% font scale, and both together. Report device evidence separately from JVM checks.
- [ ] Report actual Android and Blender results and any gaps. The review had already observed equal scene settings and four bit-identical Blender comparisons using the proposed read pattern. Those observations are a baseline. The implementation is accepted against the edited code and the checks above. The Blender probes exit naturally and write no character assets.
