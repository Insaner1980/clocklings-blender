# Companion Wardrobes and Vinyl Artwork Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give all six companions one shaded vinyl-toy look, idle and celebration animation, and a doll-style wardrobe with 15 star-unlocked items each.

**Architecture:** A pure-Kotlin wardrobe catalog covers every companion. Progress stores one outfit and one name per companion. A shared `DrawScope` shading toolkit draws every companion, item and the doll on Compose `Canvas` in fixed design grids. One `CompanionView` entry point animates every companion, and one `WardrobeScreen` replaces `DollScreen` for all companions.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Navigation 3 1.2.0, Preferences DataStore 1.2.1, JUnit 4. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-22-companion-wardrobes-design.md`

## Global Constraints

- Work only in `C:\Dev\Clocklings`, on branch `codex/companions-clock-themes`. `C:\Dev\Sateenkaarikello-Android` is read-only.
- **No commits, pushes or pull requests.** Each task ends with a checkpoint report. The owner commits when they ask for it.
- No new dependencies, raster artwork or 3D engine. Artwork is code-drawn on Compose `Canvas`.
- Identifiers, filenames and code comments in English. UI copy only in Android string resources, English only in this plan. Finnish and Swedish follow in the later localization pass.
- No `·`, `•` or ` | ` separators in any UI text. No colored edge stripes on cards, rows or panels.
- Colors only from theme files (`PersonalizationColors`, `SkDollColors`, `SkColors`). Light and shade tones come from the toolkit, never hardcoded per shape.
- Delete dead code in the same task that makes it dead. Grep every caller when moving or renaming.
- Gradle only from the repository root with `--no-daemon`: `.\gradlew.bat <task> --no-daemon`.
- Device checks only on the dedicated AVD `ClocklingsFoundation20261020` (serial `emulator-5590`). Set `ANDROID_SERIAL=emulator-5590` for every instrumented run. Never touch `emulator-5586` or a physical device. Restore display and font overrides afterwards.
- Do not claim `lc` or `sc` passed. The owner runs them.
- Star ceiling stays `Wardrobe.MAX_STARS = 15`. Each animal kind: one free item (star 0) plus one item for each star 1 to 15.
- Shading recipe (from the approved sample): radial gradient centered at 36% x / 30% y of the shape bounds, radius 0.78 of the larger side. Stops: light (base lerped 0.45 toward white) at 0, base at 0.55, shade (base lerped 0.30 toward `SkColors.Ink`) at 1. Specular: white, alpha 0.55 at the center fading to 0.
- Idle motion values (from the approved sample): breathe period 3200 ms with squash 0.012, head tilt amplitude 3 degrees with period 4800 ms, part swing amplitude 9 degrees with period 2400 ms. The blink timing stays as in the current `DollMotion`.

## Review Focus

1. **Companion switched after a game:** the result screen must show unlocks for the companion selected when the result is displayed, and `NONE` shows none. Pinned in Task 9 (`visibleUnlocks` test).
2. **An id from another companion saved in a slot** (for example `cat-head-bow` under `outfit_dog_head`): reading must leave that slot empty, not crash and not show the cat item on the dog. Pinned in Task 4.
3. **Legacy accessory flag together with a saved outfit:** the saved outfit wins, and the legacy flag only applies when the kind has no outfit keys at all. Pinned in Task 4.
4. **Reduced motion while a new item is revealed:** the new outfit must appear immediately, with no spin. Pinned in Task 2 (`revealsNewOutfit` test).
5. **Editing one companion's name:** other companions' names, including the doll's existing name, must not change. Pinned in Task 3 and Task 4.

---

### Task 1: Vinyl shading toolkit and art pigments

**Files:**
- Create: `app/src/main/java/fi/clocklings/ui/art/VinylShading.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/theme/PersonalizationColors.kt`
- Test: `app/src/test/java/fi/clocklings/ui/art/VinylTonesTest.kt`

**Interfaces:**
- Produces:
  - `@Immutable data class VinylTones(val light: Color, val base: Color, val shade: Color)`
  - `fun vinylTones(base: Color): VinylTones` (the recipe in Global Constraints)
  - `fun vinylBrush(bounds: Rect, tones: VinylTones): Brush`: radial gradient as above. Callers build it once per color and part, never per frame.
  - `fun DrawScope.vinylOval(bounds: Rect, brush: Brush)`
  - `fun DrawScope.vinylPath(path: Path, brush: Brush)`
  - `fun DrawScope.vinylStroke(path: Path, brush: Brush, width: Float)` (round cap)
  - `fun DrawScope.specular(bounds: Rect, rotationDeg: Float = 0f)`
  - `fun DrawScope.contactShadow(bounds: Rect)`: radial fade from `PersonalizationColors.ContactShadow` (alpha 0.28) to transparent
  - `fun DrawScope.groundShadow(bounds: Rect)`: radial fade from `SkColors.Ink` alpha 0.22 to transparent
  - `fun DrawScope.glossyEye(center: Offset, radiusX: Float, radiusY: Float, blink: Float, tones: VinylTones)`: when `blink >= 0.7f`, draws a closed line as the current `eyes()` does. Otherwise a gradient eye with a large highlight (radius 0.38 x radiusX at +0.33 rx, -0.4 ry) and a small one (radius 0.17 x radiusX at -0.28 rx, +0.45 ry).
  - New `PersonalizationColors` pigments: `ContactShadow`, `EyeInk`, `Knit`, `Teal`, `Gold`, `Ruby`, `Pearl`, `Denim`, `Leaf`, `Sky`, `Sun`, `Steel`. Each is one base color; tones come from `vinylTones`.

- [ ] **Step 1: Write the failing test**

```kotlin
class VinylTonesTest {
    @Test fun `light is brighter and shade darker than base`() {
        val t = vinylTones(Color(0xFFEE8C3E))
        assertTrue(t.light.luminance() > t.base.luminance())
        assertTrue(t.shade.luminance() < t.base.luminance())
        assertEquals(Color(0xFFEE8C3E), t.base)
    }
    @Test fun `light mixes 45 percent toward white`() {
        assertEquals(lerp(Color.Black, Color.White, 0.45f), vinylTones(Color.Black).light)
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "fi.clocklings.ui.art.VinylTonesTest"`
Expected: FAIL, unresolved reference `vinylTones`.

- [ ] **Step 3: Implement `VinylShading.kt` and the pigments.** Use `androidx.compose.ui.graphics.lerp` and `Brush.radialGradient` with explicit `center` and `radius` computed from `bounds`.

- [ ] **Step 4: Run the test and confirm it passes**

Same command. Expected: PASS.

- [ ] **Step 5: Checkpoint.** Report the files changed and the test result. Do not commit.

---

### Task 2: Shared companion pose and motion

**Files:**
- Create: `app/src/main/java/fi/clocklings/ui/companion/CompanionPose.kt` (moved from `ui/doll/DollMotion.kt`, which is deleted)
- Modify: every user of `DollPose`, `DollMotion`, `DollCelebration`, `CelebrationFrame` (grep `ui/doll`, `ui/appearance`, `ui/game`, `ui/intro`, `ui/home`, tests)
- Test: move `app/src/test/java/fi/clocklings/ui/doll/DollMotionTest.kt` (if present) to `app/src/test/java/fi/clocklings/ui/companion/CompanionMotionTest.kt` and extend it

**Interfaces:**
- Produces:
  - `@Immutable data class CompanionPose(blink, swayDeg, lift, armDeg, turn, val breathe: Float = 0f, val headTiltDeg: Float = 0f, val partDeg: Float = 0f)`. The first five fields keep their current meaning and defaults.
  - `object CompanionMotion` with every current `DollMotion` member unchanged, plus `const val BREATHE_SQUASH = 0.012f`, `HEAD_TILT_DEG = 3f`, `PART_SWING_DEG = 9f`, and `idle(nowMs)` also filling `breathe = sin(2πt/3200)`, `headTiltDeg = -HEAD_TILT_DEG * (1 - cos(2πt/4800)) / 2`, `partDeg = PART_SWING_DEG * (1 - cos(2πt/2400)) / 2`.
  - `data class CelebrationFrame` unchanged. During the jump phase, `partDeg` is `2 * PART_SWING_DEG * |sin(2πk)|` so that tails and wings flap.
  - `@Immutable data class CompanionCelebration(val newOutfit: Outfit?, val jump: Boolean, val delayMs: Long = 0L)` with `spin` as now.
  - `fun CompanionMotion.revealsNewOutfit(celebration: CompanionCelebration?, elapsedMs: Long, reduceMotion: Boolean): Boolean`: true when `celebration?.spin == true` and either `reduceMotion` or the frame at `elapsedMs - delayMs` has `showNewOutfit`. This is the logic currently inline in `LivelyDoll`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `idle breathing, tilt and swing stay in range`() {
    for (t in 0L..10_000L step 50) {
        val p = CompanionMotion.idle(t)
        assertTrue(p.breathe in -1f..1f)
        assertTrue(p.headTiltDeg in -3f..0f)
        assertTrue(p.partDeg in 0f..9f)
    }
}
@Test fun `idle starts at rest`() {
    val p = CompanionMotion.idle(0L)
    assertEquals(0f, p.breathe, 1e-4f); assertEquals(0f, p.headTiltDeg, 1e-4f); assertEquals(0f, p.partDeg, 1e-4f)
}
@Test fun `reduced motion reveals the new outfit immediately`() {
    val c = CompanionCelebration(newOutfit = Outfit.Default, jump = false, delayMs = 1_100L)
    assertTrue(CompanionMotion.revealsNewOutfit(c, elapsedMs = 0L, reduceMotion = true))
    assertFalse(CompanionMotion.revealsNewOutfit(c, elapsedMs = 0L, reduceMotion = false))
    assertTrue(CompanionMotion.revealsNewOutfit(c, elapsedMs = 1_100L + 300L, reduceMotion = false))
}
@Test fun `no reveal without a new outfit`() {
    assertFalse(CompanionMotion.revealsNewOutfit(CompanionCelebration(null, jump = true), 5_000L, true))
}
```

Keep every existing `DollMotion` test, renamed to `CompanionMotion`.

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "fi.clocklings.ui.companion.CompanionMotionTest"`
Expected: FAIL, unresolved references.

- [ ] **Step 3: Move and extend.** Make `LivelyDoll` call `revealsNewOutfit`. Do not change any drawing in this task.

- [ ] **Step 4: Run all unit tests and the debug build**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Checkpoint.** Report the changes. Do not commit.

---

### Task 3: Domain model for per-companion wardrobes

**Files:**
- Modify: `app/src/main/java/fi/clocklings/domain/Wardrobe.kt`, `app/src/main/java/fi/clocklings/domain/Progress.kt`, `app/src/main/java/fi/clocklings/domain/Personalization.kt`
- Create: `app/src/main/java/fi/clocklings/domain/CompanionItems.kt`
- Modify callers: `ui/doll/Doll.kt` (`o.hair` becomes `o[Slot.HAIR]` and so on), `ui/AppViewModel.kt`, `ui/home/HomeScreen.kt`, `ui/game/ResultScreen.kt`, `ClocklingsApp.kt`, `ui/appearance/*`
- Test: `app/src/test/java/fi/clocklings/domain/WardrobeTest.kt`, `app/src/test/java/fi/clocklings/domain/CompanionItemsTest.kt`, `app/src/test/java/fi/clocklings/domain/ProgressTest.kt`

**Interfaces:**
- Produces:
  - `enum class Slot(val optional: Boolean) { HAIR(false), DRESS(false), SHOES(false), HEAD(true), NECK(true), EXTRA(true) }`
  - `val CompanionKind.slots: List<Slot>`: HUMAN gives `[HAIR, DRESS, SHOES, HEAD, EXTRA]`, NONE gives empty, and the animals give `[HEAD, NECK, EXTRA]`.
  - `data class Item(val id: String, val companion: CompanionKind, val slot: Slot, val star: Int)`
  - `data class Outfit(val items: Map<Slot, String> = emptyMap())` with `operator fun get(slot: Slot): String?`, `fun with(slot: Slot, id: String?): Outfit` (null on a required slot keeps the current item), `fun isOn(item: Item): Boolean`, `companion object { val Default: Outfit /* the doll's current default */; fun default(kind: CompanionKind): Outfit }`. Animals and NONE get `Outfit()`.
  - `object Wardrobe`: `MAX_STARS = 15`, `items: List<Item>` (doll items with `companion = HUMAN` in their current order, then `AnimalItems`), `item(id)`, `itemOrNull(id): Item?`, `itemsFor(kind)`, `itemsFor(kind, slot)`, `freeItems(kind): List<Item>`, `newlyUnlocked(kind, before, after)`, `lockedCount(kind, totalStars)`, `toggle(outfit, item)` (rule unchanged).
  - `internal val AnimalItems: List<Item>` in `CompanionItems.kt`. Built with a private helper `ladder(kind, free: Pair<String, Slot>, vararg unlocks: Pair<String, Slot>)` that gives the free item star 0 and each unlock its 1-based position as its star.
  - `Progress`: replace `outfit` and `dollName` with `val outfits: Map<CompanionKind, Outfit> = emptyMap()` and `val names: Map<CompanionKind, String> = emptyMap()`, plus `fun outfitFor(kind): Outfit` (`outfits[kind] ?: Outfit.default(kind)`), `fun nameFor(kind): String`, and `fun withOutfit(kind, outfit): Progress` and `fun withName(kind, name): Progress`. Both drop the map entry when the value equals the default (`Outfit.default(kind)` or `""`), as `Personalization.withStyle` does.
  - `CompanionStyle(val color: CompanionColor = ORIGINAL)`: the `accessory` field is removed.
  - `AppViewModel`: `toggleItem(kind: CompanionKind, item: Item)`, `setName(kind: CompanionKind, name: String)` and `setColor(kind: CompanionKind, color: CompanionColor)`. `setDollName` is removed.

Item ids use the form `<kind>-<slot>-<name>`, all lowercase. Free item first, then stars 1 to 15 in order:

- `cat`: free `cat-neck-scarf`. Then `cat-head-bow`, `cat-extra-yarn`, `cat-neck-bell-collar`, `cat-head-bobble-hat`, `cat-extra-glasses`, `cat-neck-bow-tie`, `cat-head-sailor-hat`, `cat-extra-fish-backpack`, `cat-neck-ruffle`, `cat-head-flower-crown`, `cat-neck-pearls`, `cat-extra-wand`, `cat-neck-cape`, `cat-head-crown`, `cat-extra-wings`
- `dog`: free `dog-neck-bandana`. Then `dog-head-cap`, `dog-extra-ball`, `dog-neck-bone-collar`, `dog-head-sun-hat`, `dog-extra-frisbee`, `dog-neck-bow-tie`, `dog-head-party-hat`, `dog-extra-boots`, `dog-neck-lei`, `dog-head-aviator`, `dog-extra-backpack`, `dog-neck-medal`, `dog-head-helmet`, `dog-extra-raincoat`, `dog-neck-cape`
- `robot`: free `robot-head-antenna`. Then `robot-neck-bow-tie`, `robot-extra-wrench`, `robot-head-propeller`, `robot-neck-tie`, `robot-extra-heart-eyes`, `robot-head-headphones`, `robot-neck-spring`, `robot-extra-star-eyes`, `robot-head-chef-hat`, `robot-neck-lights`, `robot-extra-pet`, `robot-head-star-antennas`, `robot-neck-medal`, `robot-head-astronaut`, `robot-extra-jetpack`
- `dragon`: free `dragon-neck-pendant`. Then `dragon-head-flower-crown`, `dragon-extra-coins`, `dragon-neck-bow`, `dragon-head-wizard-hat`, `dragon-extra-book`, `dragon-neck-scarf`, `dragon-head-tiara`, `dragon-extra-shield`, `dragon-neck-chain`, `dragon-head-knight-helmet`, `dragon-extra-star-cloud`, `dragon-neck-sparkles`, `dragon-neck-cape`, `dragon-head-crown`, `dragon-extra-wings`
- `dinosaur`: free `dinosaur-extra-backpack`. Then `dinosaur-head-flower`, `dinosaur-extra-bucket`, `dinosaur-neck-scarf`, `dinosaur-head-cap`, `dinosaur-extra-balloon`, `dinosaur-neck-bow-tie`, `dinosaur-head-party-hat`, `dinosaur-extra-umbrella`, `dinosaur-neck-lei`, `dinosaur-head-hard-hat`, `dinosaur-extra-kite`, `dinosaur-neck-binoculars`, `dinosaur-neck-medal`, `dinosaur-head-explorer-hat`, `dinosaur-extra-skateboard`

The slot is the second id segment.

- [ ] **Step 1: Write the failing tests**

`CompanionItemsTest`:

```kotlin
private val animals = listOf(CAT, DOG, ROBOT, DRAGON, DINOSAUR)
@Test fun `every animal has one free item and one item per star`() = animals.forEach { kind ->
    val items = Wardrobe.itemsFor(kind)
    assertEquals(16, items.size)
    assertEquals(1, items.count { it.star == 0 })
    assertEquals((1..15).toList(), items.filter { it.star > 0 }.map { it.star }.sorted())
}
@Test fun `animal items use only the animal slots and match their ids`() = animals.forEach { kind ->
    Wardrobe.itemsFor(kind).forEach {
        assertTrue(it.slot in kind.slots)
        assertEquals(it.slot.name.lowercase(), it.id.split("-")[1])
        assertTrue(it.id.startsWith(kind.name.lowercase() + "-"))
    }
}
@Test fun `ids are unique across all companions`() =
    assertEquals(Wardrobe.items.size, Wardrobe.items.map { it.id }.toSet().size)
@Test fun `newly unlocked is per companion`() {
    assertEquals(listOf("cat-head-bow", "cat-extra-yarn"), Wardrobe.newlyUnlocked(CAT, 0, 2).map { it.id })
    assertEquals(listOf("robot-neck-bow-tie"), Wardrobe.newlyUnlocked(ROBOT, 0, 1).map { it.id })
    assertTrue(Wardrobe.newlyUnlocked(NONE, 0, 15).isEmpty())
}
@Test fun `locked count is per companion`() {
    assertEquals(15, Wardrobe.lockedCount(DOG, 0)); assertEquals(0, Wardrobe.lockedCount(DOG, 15))
}
```

`WardrobeTest`: keep every current test, adapted to `Wardrobe.itemsFor(HUMAN)`, `newlyUnlocked(HUMAN, …)`, `lockedCount(HUMAN, …)` and `o[Slot.HAIR]`. Change `only head and extra are optional` to `setOf(HEAD, NECK, EXTRA)`.

`ProgressTest` (Review Focus 5):

```kotlin
@Test fun `names and outfits are kept per companion`() {
    val p = Progress().withName(HUMAN, "Ruusa").withName(CAT, "Miuku")
        .withOutfit(CAT, Outfit().with(Slot.HEAD, "cat-head-bow"))
    assertEquals("Ruusa", p.nameFor(HUMAN)); assertEquals("Miuku", p.nameFor(CAT)); assertEquals("", p.nameFor(DOG))
    assertEquals(Outfit.Default, p.outfitFor(HUMAN))
    assertEquals("cat-head-bow", p.outfitFor(CAT)[Slot.HEAD])
}
@Test fun `default values are not stored`() {
    assertEquals(Progress(), Progress().withName(CAT, "").withOutfit(DOG, Outfit()))
}
```

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "fi.clocklings.domain.*"`
Expected: FAIL, unresolved references.

- [ ] **Step 3: Implement the domain changes and update callers.** In the UI, use `outfitFor(LocalPersonalization.current.companion)` where `progress.outfit` was used, and `nameFor(HUMAN)` where `dollName` was used. Removing `CompanionStyle.accessory` makes the accessory chip in `AppearanceScreen` dead. Delete the chip, `accessoryName()` and the `accessory_*` strings in this task. The color chips stay until Task 7.

- [ ] **Step 4: Run all unit tests and the build**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL. `ProgressRepositoryTest` may still fail until Task 4. If so, report it and leave it for Task 4.

- [ ] **Step 5: Checkpoint.** Report the changes. Do not commit.

---

### Task 4: Persistence for outfits, names and colors

**Files:**
- Modify: `app/src/main/java/fi/clocklings/data/ProgressRepository.kt`, `app/src/main/java/fi/clocklings/data/PersonalizationPreferences.kt`
- Test: `app/src/test/java/fi/clocklings/data/ProgressRepositoryTest.kt`, `app/src/androidTest/java/fi/clocklings/PersonalizationStorageTest.kt`

**Interfaces:**
- Consumes: Task 3 `Progress`, `Outfit`, `Wardrobe.itemOrNull`, `Wardrobe.freeItems`, `CompanionKind.slots`.
- Keys:
  - Human outfit: unchanged `outfit_<slot lowercase>`. Human name: unchanged `doll_name`.
  - Animal outfit: `outfit_<kind lowercase>_<slot lowercase>`. The empty string means nothing worn.
  - Animal name: `companion_name_<kind lowercase>`.
  - Legacy, read only: boolean `companion_<KIND>_accessory` (uppercase kind, as written today).
- Reading an animal outfit: if none of the kind's outfit keys exist and the legacy flag is true, wear `Wardrobe.freeItems(kind).single()`. Otherwise, for each slot, keep the id only if `itemOrNull(id)?.let { it.companion == kind && it.slot == slot }`. Otherwise the slot is empty.
- `writePersonalization` stops writing the accessory key, and `readPersonalization` stops reading that key while retaining the other appearance settings. `ProgressRepository.readAnimalOutfit` retains the read-only legacy accessory fallback described above, only when no outfit keys exist.

- [ ] **Step 1: Write the failing tests** (add to `ProgressRepositoryTest`; update `progress round trips` to use `outfits` and `names`)

```kotlin
@Test fun `animal outfits and names round trip`() = scope.runTest {
    val p = Progress()
        .withOutfit(CAT, Outfit().with(Slot.HEAD, "cat-head-crown").with(Slot.NECK, "cat-neck-scarf"))
        .withOutfit(ROBOT, Outfit().with(Slot.EXTRA, "robot-extra-jetpack"))
        .withName(CAT, "Miuku").withName(HUMAN, "Ruusa")
    repository.updateProgress { p }
    assertEquals(p, repository.progress.first())
}
@Test fun `an id from another companion is ignored`() = scope.runTest {
    store.edit { it[stringPreferencesKey("outfit_dog_head")] = "cat-head-bow" }
    assertEquals(Outfit(), repository.progress.first().outfitFor(DOG))
}
@Test fun `legacy accessory wears the free item only without a saved outfit`() = scope.runTest {
    store.edit {
        it[booleanPreferencesKey("companion_DOG_accessory")] = true
        it[booleanPreferencesKey("companion_CAT_accessory")] = true
        it[stringPreferencesKey("outfit_cat_head")] = "cat-head-bow"
    }
    val p = repository.progress.first()
    assertEquals("dog-neck-bandana", p.outfitFor(DOG)[Slot.NECK])
    assertEquals(null, p.outfitFor(CAT)[Slot.NECK])
    assertEquals("cat-head-bow", p.outfitFor(CAT)[Slot.HEAD])
}
@Test fun `reset clears every outfit and name but keeps colors`() = scope.runTest {
    repository.updateSettings { it.copy(personalization = it.personalization.withStyle(CAT, CompanionStyle(CompanionColor.BLUE))) }
    repository.updateProgress { it.withOutfit(CAT, Outfit().with(Slot.HEAD, "cat-head-bow")).withName(CAT, "Miuku") }
    repository.resetProgress()
    assertEquals(Progress(), repository.progress.first())
    assertEquals(CompanionColor.BLUE, repository.settings.first().personalization.styleFor(CAT).color)
}
```

Keep `unknown outfit ids fall back to defaults`, adapted to `outfitFor(HUMAN)[Slot.DRESS]`.

- [ ] **Step 2: Run the tests and confirm they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "fi.clocklings.data.ProgressRepositoryTest"`
Expected: FAIL.

- [ ] **Step 3: Implement the reading and writing.** Writing a `Progress` writes every animal kind's slot keys and name keys, using the empty string for none. That way reset empties them, and the legacy flag can no longer apply after the first write.

- [ ] **Step 4: Run the tests and the build**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Checkpoint.** Do not commit.

---

### Task 5: Item and slot names

**Files:**
- Create: `app/src/main/res/values/companion_items.xml`
- Modify: `app/src/main/java/fi/clocklings/ui/Texts.kt` (`slotName`, `ItemNames`), `app/src/main/res/values/strings.xml` (`slot_neck`)
- Test: `app/src/test/java/fi/clocklings/ui/ItemNamesTest.kt`

**Interfaces:**
- Produces: `slotName(Slot.NECK) == R.string.slot_neck` ("Neckwear"). For every id, `itemName(id)` returns `R.string.item_<id with '-' replaced by '_'>`.
- English names, exactly as in spec section 4. Examples: `item_cat_neck_scarf` "Striped scarf", `item_cat_extra_yarn` "Yarn ball", `item_robot_extra_heart_eyes` "Heart-eyes screen", `item_dinosaur_head_flower` "Flower behind the ear", `item_dog_neck_bandana` "Bandana", `item_robot_head_antenna` "Antenna", `item_dragon_neck_pendant` "Pendant", `item_dinosaur_extra_backpack` "Backpack".

- [ ] **Step 1: Extend the failing test**

```kotlin
@Test fun `every item has its own name`() {
    val ids = Wardrobe.items.map { itemName(it.id) }
    assertEquals(Wardrobe.items.size, ids.toSet().size)
}
```

(It already exists. It fails now because `getValue` throws for the new ids.)

- [ ] **Step 2: Run the test and confirm it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "fi.clocklings.ui.ItemNamesTest"`
Expected: FAIL with `NoSuchElementException`.

- [ ] **Step 3: Add the 80 strings and map entries.**

- [ ] **Step 4: Run all unit tests and the build.** Expected: PASS.

- [ ] **Step 5: Checkpoint.** Do not commit.

---

### Task 6: Companion rendering framework

**Files:**
- Create: `app/src/main/java/fi/clocklings/ui/companion/CompanionView.kt` (moved from `ui/appearance/CompanionView.kt`; `LivelyDoll.kt` merges into it and is deleted)
- Create: `app/src/main/java/fi/clocklings/ui/companion/CompanionRig.kt`
- Create: `app/src/main/java/fi/clocklings/ui/companion/LegacyAnimalRig.kt` (temporary adapter around the existing `CompanionArt.kt`, deleted in Task 12)
- Modify callers: `ui/appearance/AppearanceScreen.kt`, `ui/home/HomeScreen.kt`, `ui/game/ResultScreen.kt`, `ui/intro/IntroScreen.kt`

**Interfaces:**
- Produces:
  - `internal enum class ItemLayer { BEHIND, FRONT, OVER_EARS }`
  - `internal enum class ArtPart { BODY, HEAD, TAIL, WINGS, HAND }`
  - `internal class ItemArt(val part: ArtPart, val layer: ItemLayer, val draw: DrawScope.() -> Unit)`
  - `internal interface CompanionRig { val width: Float; val height: Float; fun crop(slot: Slot): Rect; fun full(): Rect; fun DrawScope.draw(color: CompanionColor, outfit: Outfit, pose: CompanionPose) }`. The draw happens in design-grid coordinates. Each rig draws its parts and calls `items(outfit, part, layer)` at every part and layer point, inside that part's transform.
  - `internal fun rigFor(kind: CompanionKind): CompanionRig?`: HUMAN gives `DollRig` (wraps the existing `Doll` drawing and `DollParts` crops), NONE gives null, and the animals give `LegacyAnimalRig(kind)` until their own task.
  - `@Composable fun CompanionView(kind: CompanionKind, outfit: Outfit, modifier: Modifier = Modifier, color: CompanionColor = LocalPersonalization.current.styleFor(kind).color, celebration: CompanionCelebration? = null, animated: Boolean = true, cropSlot: Slot? = null)`. One frame clock, reduced-motion handling, confetti sparkle on reveal, and `testTag("companion_${kind.name}")`, as `LivelyDoll` does now for the doll.
  - `LegacyAnimalRig` draws the old art and maps "free item worn" to the old `accessory = true`. Its crop for every slot is the full grid.

- [ ] **Step 1: Move and merge without visual change.** No new test: behavior is covered by Task 2's `revealsNewOutfit` tests and the existing `AppFlowTest`.

- [ ] **Step 2: Run unit tests, the build and the existing instrumented flow**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`, then with `ANDROID_SERIAL=emulator-5590`: `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=fi.clocklings.AppFlowTest`
Expected: BUILD SUCCESSFUL, and every AppFlowTest test passes.

- [ ] **Step 3: Checkpoint.** Do not commit.

---

### Task 7: Wardrobe screen, picker cleanup and navigation

**Files:**
- Create: `app/src/main/java/fi/clocklings/ui/companion/WardrobeScreen.kt` (from `ui/doll/DollScreen.kt`, which is deleted; `ItemTile` moves here)
- Modify: `app/src/main/java/fi/clocklings/Screen.kt` (`Doll` becomes `Wardrobe`, registration included), `ClocklingsApp.kt`, `ui/appearance/AppearanceScreen.kt`, `ui/appearance/AppearanceNames.kt`, `res/values/strings.xml`
- Test: `app/src/androidTest/java/fi/clocklings/AppFlowTest.kt`

**Interfaces:**
- Consumes: Task 3 `AppViewModel.toggleItem/setName/setColor`, Task 6 `CompanionView`, `rigFor`.
- Produces:
  - `@Composable fun WardrobeScreen(kind: CompanionKind, progress: Progress, color: CompanionColor, onBack: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun ItemTile(item: Item, outfit: Outfit, color: CompanionColor, modifier: Modifier = Modifier, on: Boolean = false, onClick: (() -> Unit)? = null)`: renders `CompanionView(item.companion, outfit.with(item.slot, item.id), color = color, animated = false, cropSlot = item.slot)`.
- Screen layout, top to bottom: stage (large `CompanionView` and name field bound to `nameFor(kind)` and `setName(kind, …)`, max 16 characters). Then the info text (`doll_info` and `doll_all_open` with `lockedCount(kind, total)`). For animals only, a **Color** section (`R.string.companion_color`) with four selectable tiles, each a full `CompanionView` in that color with `testTag("color_<COLOR>")`. Then one section per `kind.slots`, using the existing grid, tiles, locked tiles and optional hint.
- Picker: delete `CompanionControls` (the color chips). Every kind except NONE shows `SkButton(R.string.dress_doll)`, which opens `Screen.Wardrobe`.
- Item grid: three columns, as now. Two columns when `maxWidth < 360.dp` or `fontScale > SkDimens.MAX_ROW_FONT_SCALE`, so that long names such as "Firefighter helmet" do not split words.

- [ ] **Step 1: Write the failing instrumented test** in `AppFlowTest`

```kotlin
@Test fun animalWardrobeWearsItemsAndColors() {
    // Seed 3 stars, then choose the cat in the picker.
    click("Choose an outfit")
    onNodeWithText("Striped scarf").performClick(); assertWearing("Striped scarf")
    onNodeWithText("Striped scarf").performClick(); assertNotWearing("Striped scarf")
    onNodeWithTag("color_BLUE").performClick(); onNodeWithTag("color_BLUE").assertIsSelected()
    onNodeWithContentDescription("Locked. Stars needed: 4").assertExists()   // the bobble hat
    onAllNodesWithText("Bandana").assertCountEquals(0)                       // no stray chip
}
```

Use the test file's existing helpers for seeding and clicking; add `assertWearing` and `assertNotWearing` helpers that look for the "Wearing" pill in the item's tile.

- [ ] **Step 2: Run it and confirm it fails**

Run with `ANDROID_SERIAL=emulator-5590`: `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=fi.clocklings.AppFlowTest`
Expected: FAIL at `animalWardrobeWearsItemsAndColors`.

- [ ] **Step 3: Implement the screen, picker and navigation.**

- [ ] **Step 4: Run unit tests, the build and AppFlowTest.** Expected: all pass. Then take screenshots of the picker and the cat wardrobe at default size, 320 dp, 200% font and both together, and inspect them.

- [ ] **Step 5: Checkpoint.** Report test results and screenshot paths. Do not commit.

---

### Task 8: Cat artwork and items (owner checkpoint)

**Files:**
- Create: `app/src/main/java/fi/clocklings/ui/companion/CatRig.kt`, `app/src/main/java/fi/clocklings/ui/companion/CatItems.kt`
- Modify: `rigFor` (CAT gives `CatRig`)
- Test: `app/src/androidTest/java/fi/clocklings/GalleryTest.kt`, `app/src/androidTest/java/fi/clocklings/CompanionArtCoverageTest.kt` (new)

**Interfaces:**
- Consumes: Task 1 toolkit, Task 2 pose, Task 6 `CompanionRig`, `ItemArt`, `ArtPart`, `ItemLayer`.
- Produces: `internal object CatRig : CompanionRig` (grid 240 x 300) and `internal val CatItemArt: Map<String, ItemArt>`, keyed by every id in `Wardrobe.itemsFor(CAT)`.
- Geometry: follow the approved sample. The sample's SVG is the source of truth for the body: tail, body, chest, paws with toe lines, contact shadow, ears with pink inner ears, head, forehead stripes, muzzle, cheeks, glossy eyes, nose, mouth and whiskers. The bobble hat, scarf, crown and bell collar items also come from the sample. The sample is reproduced in the "Cat reference geometry" appendix below.
- Motion mapping: body breathes around (120, 280), the head tilts around (120, 160), the tail rotates by `partDeg` around (168, 250), and eyes blink.
- Item placement: HEAD items go in the head transform. Hats use `OVER_EARS` and are drawn before the ears. NECK items go in the body transform between body and head. EXTRA: yarn ball (BODY, FRONT, held between the paws), glasses (HEAD, FRONT), fish backpack (BODY, BEHIND), wand (HAND, FRONT, at the right paw), wings (BODY, BEHIND).
- Crops: HEAD `Rect(30, 10, 210, 180)`, NECK `Rect(40, 130, 200, 240)`, EXTRA is the full grid.
- Brushes are cached per `CompanionColor` in the rig. No `Brush` is allocated inside `draw` for body parts.

- [ ] **Step 1: Write the failing instrumented coverage test**

```kotlin
@Test fun everyCatItemHasArtwork() =
    assertEquals(Wardrobe.itemsFor(CompanionKind.CAT).map { it.id }.toSet(), CatItemArt.keys)
```

- [ ] **Step 2: Add `GalleryTest.companionInEveryItem(kind)` for CAT.** It renders the cat in each of the four colors, then once with each item (16 tiles) at 90 x 112 dp, and saves `cat-colors.png` and `cat-items.png`.

- [ ] **Step 3: Implement `CatRig` and `CatItems`.**

- [ ] **Step 4: Run the tests, then pull and inspect the gallery**

Run with `ANDROID_SERIAL=emulator-5590`: `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=fi.clocklings.CompanionArtCoverageTest,fi.clocklings.GalleryTest`, then `adb -s emulator-5590 exec-out run-as fi.clocklings cat files/gallery/cat-items.png > reports/companions/cat-items.png` (and the same for `cat-colors.png`).
Expected: tests pass. Inspect both images at full size: no clipped parts, items sit on the body, and whiskers and highlights stay readable at tile size.

- [ ] **Step 5: Measure frame time.** Run the app on the emulator on Home with the cat. Run `adb -s emulator-5590 shell dumpsys gfxinfo fi.clocklings` after 10 seconds idle. Record the janky-frame percentage and the 90th percentile in the report.

- [ ] **Step 6: OWNER CHECKPOINT. Stop here.** Send the gallery images and wardrobe screenshots, and ask the owner to approve the cat on the emulator before Tasks 9 to 12. Do not commit.

---

### Task 9: Dog artwork, items and result unlocks

**Files:**
- Create: `ui/companion/DogRig.kt`, `ui/companion/DogItems.kt`
- Modify: `rigFor`, `ui/game/ResultScreen.kt`, `ui/home/HomeScreen.kt`, `ui/intro/IntroScreen.kt`, `ClocklingsApp.kt`
- Test: `CompanionArtCoverageTest`, `GalleryTest`, `app/src/test/java/fi/clocklings/ui/game/VisibleUnlocksTest.kt` (new)

**Interfaces:**
- Produces: `DogRig`, `DogItemArt` (same contract as Task 8). `internal fun visibleUnlocks(before: Int, after: Int, companion: CompanionKind): List<Item>` in `ResultScreen.kt` returns `Wardrobe.newlyUnlocked(companion, before, after)`, which is empty for NONE.
- Dog look: floppy dark ears (the right ear swings by `partDeg`), a dark patch over the right eye, a cream muzzle with a large glossy nose and a pink tongue, a cream belly, and a tail that wags by `partDeg`.
- Integration: results show the current companion's unlocks with `ItemTile` and spin them on. The Home card uses `home_doll_hint` for every companion except NONE; delete `home_friend_hint` once unused. The Home and intro companions use `nameFor` and `outfitFor` of the current kind.

- [ ] **Step 1: Write the failing tests**

```kotlin
class VisibleUnlocksTest {
    @Test fun `unlocks follow the companion shown`() {
        assertEquals(listOf("dog-head-cap"), visibleUnlocks(0, 1, DOG).map { it.id })
        assertEquals(listOf("cat-head-bow"), visibleUnlocks(0, 1, CAT).map { it.id })
        assertTrue(visibleUnlocks(0, 15, NONE).isEmpty())
    }
}
```

Add `everyDogItemHasArtwork` to `CompanionArtCoverageTest`.

- [ ] **Step 2: Run and confirm they fail.**
- [ ] **Step 3: Implement the dog and the integration.**
- [ ] **Step 4: Run unit tests, the build, the coverage test, the gallery and AppFlowTest. Inspect `dog-colors.png` and `dog-items.png`.**
- [ ] **Step 5: Checkpoint.** Do not commit.

---

### Task 10: Robot artwork and items

**Files:** `ui/companion/RobotRig.kt`, `ui/companion/RobotItems.kt`, `rigFor`, tests as in Task 8.

**Interfaces:** `RobotRig`, `RobotItemArt`.
- Look: rounded teal head with a dark glossy face screen, glowing eyes and smile, side ear discs, a neck joint, a body with a cream chest panel and two colored buttons, jointed dark arms with round hands, and short legs with rounded feet. The free antenna bounces by `partDeg`. While worn, `robot-extra-heart-eyes` and `robot-extra-star-eyes` replace the screen's eyes, and blinking dims them instead of closing them.
- Crops: HEAD `Rect(30, 0, 210, 160)`, NECK `Rect(40, 120, 200, 230)`, EXTRA is the full grid.

- [ ] **Step 1: Add `everyRobotItemHasArtwork` and confirm it fails.**
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Run the coverage test and the gallery, and inspect `robot-colors.png` and `robot-items.png`.**
- [ ] **Step 4: Checkpoint.** Do not commit.

---

### Task 11: Dragon artwork and items

**Files:** `ui/companion/DragonRig.kt`, `ui/companion/DragonItems.kt`, `rigFor`, tests as in Task 8.

**Interfaces:** `DragonRig`, `DragonItemArt`.
- Look: round head with cream horns and small frilled ears, membrane wings that flap by `partDeg`, a cream belly with plate lines, a tail with a gold diamond tip, and pink cheeks. `dragon-extra-wings` replaces the dragon's own wings while worn. Capes use `BEHIND`.
- Crops: HEAD `Rect(30, 10, 210, 180)`, NECK `Rect(40, 130, 200, 240)`, EXTRA is the full grid.

- [ ] **Step 1: Add `everyDragonItemHasArtwork` and confirm it fails.**
- [ ] **Step 2: Implement.**
- [ ] **Step 3: Run the coverage test and the gallery, and inspect.**
- [ ] **Step 4: Checkpoint.** Do not commit.

---

### Task 12: Dinosaur artwork and items, legacy art removal

**Files:** `ui/companion/DinosaurRig.kt`, `ui/companion/DinosaurItems.kt`, `rigFor`, tests as in Task 8. Delete `ui/appearance/CompanionArt.kt`, `ui/companion/LegacyAnimalRig.kt`, and any `PersonalizationColors` pigment that is no longer referenced (grep each one).

**Interfaces:** `DinosaurRig`, `DinosaurItemArt`.
- Look: upright standing dinosaur with a big round head and a long snout, spikes down the back, darker spots, a cream belly, short arms, and a tail that swings by `partDeg`.
- Spin reveal: if the gallery or emulator shows the horizontal flip looking wrong for the side tail, the dinosaur's rig skips `turn` and the reveal is a hop with a sparkle. Record which one was chosen in the report.
- Crops: HEAD `Rect(60, 20, 230, 180)`, NECK `Rect(60, 130, 210, 240)`, EXTRA is the full grid.

- [ ] **Step 1: Add `everyDinosaurItemHasArtwork` and confirm it fails.**
- [ ] **Step 2: Implement, then delete the legacy files and grep for leftovers.**

Run: `rg -n "CompanionArt\b|LegacyAnimalRig|accessory" app/src`
Expected: no `CompanionArt` or `LegacyAnimalRig` references in main sources. The documented read-only `companion_<KIND>_accessory` compatibility key and its regression tests remain valid matches; do not remove that fallback.

- [ ] **Step 3: Run unit tests, the build, the coverage test and the gallery.** Expected: all pass.
- [ ] **Step 4: Checkpoint.** Do not commit.

---

### Task 13: Doll shading

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/doll/Doll.kt`, `app/src/main/java/fi/clocklings/ui/theme/SkDollColors.kt`
- Test: `GalleryTest.dollInEveryItem` (existing)

**Interfaces:** `DollRig` from Task 6 is unchanged. Geometry and drawing order in `Doll.kt` are unchanged.
- Replace flat fills with `vinylBrush` fills, cached per item. Swap the eyes to `glossyEye`. Use the toolkit's cheeks, specular on the hair and dress, a contact shadow under the head, and a ground shadow under the feet. Pigments stay in `SkDollColors`.

- [ ] **Step 1: Capture the before image.** Run `GalleryTest.dollInEveryItem` and save `reports/companions/doll-before.png`.
- [ ] **Step 2: Implement the shading.**
- [ ] **Step 3: Capture the after image** (`doll-after.png`). Compare: every item still sits where it did, and nothing is clipped.
- [ ] **Step 4: Checkpoint.** Do not commit.

---

### Task 14: Final validation and documentation

**Files:**
- Modify: `docs/implementation/STATUS.md` only; keep the durable architecture record inside this repository

- [ ] **Step 1: Run the full suites**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`, then with `ANDROID_SERIAL=emulator-5590`: `.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon`
Expected: BUILD SUCCESSFUL, every test passes.

- [ ] **Step 2: Capture device screenshots.** Capture Home, the picker, every companion's wardrobe, and a result with an unlock, each at default size, 320 dp, 200% font and both together, under `reports/companions/`. Inspect each one for clipping, word splitting and overlap. Restore the emulator's density and font scale afterwards, and stop the processes this task started.

- [ ] **Step 3: Update STATUS.md.** Add a "Companion wardrobes and vinyl artwork — 22 October 2026" section with the owner decisions, the new structure (`ui/art`, `ui/companion`, per-companion outfits and names), the checks actually run, screenshot paths and remaining gaps (Finnish and Swedish names, owner-run `lc`/`sc`). Record the new architecture in the same repository document; external personal memory updates are not part of this plan.

- [ ] **Step 4: Final report to the owner** in Finnish: what was implemented, which checks were run, and any unresolved points. Do not commit.

---

## Appendix: Cat reference geometry

This is the approved sample's cat in the 240 x 300 grid, as SVG. Gradients map to `vinylBrush` with the Global Constraints recipe. `url(#spec)` maps to `specular`, `url(#ao)` to `contactShadow` and `url(#ground)` to `groundShadow`.

```
ground   ellipse cx120 cy284 rx86 ry11
tail     stroke 24 round: M166 252 C214 262 224 218 206 198 C192 182 196 166 212 164   (tip in cream: M206 198 C192 182 196 166 212 164)
body     ellipse cx120 cy214 rx60 ry62;  chest cream ellipse cx120 cy218 rx33 ry45;  spec ellipse cx98 cy186 rx11 ry6
paws     ellipses cx94/146 cy270 rx24 ry15;  toe lines M88 276 L88 268, M100.., M140.., M152..  width 2.5, stripe tone, alpha 0.7
ao       ellipse cx120 cy160 rx54 ry15
ears     M62 104 L56 40 Q58 28 70 34 L108 72 Z  and  M178 104 L184 40 Q182 28 170 34 L132 72 Z
inner    M70 88 L67 49 L97 76 Z  and  M170 88 L173 49 L143 76 Z   (pink)
head     ellipse cx120 cy110 rx76 ry58;  spec ellipse cx90 cy76 rx24 ry10 rotated -18
stripes  M104 58 L108 76, M120 55 L120 74, M136 58 L132 76   width 7 round
muzzle   cream ellipse cx120 cy140 rx37 ry23;  cheeks ellipses cx76/164 cy132 rx12 ry7, Cheek alpha 0.35
eyes     glossyEye centers (92,112) and (148,112), radii 10.5 x 13.5
nose     M111 128 Q120 122 129 128 Q125 135 120 136 Q115 135 111 128 Z  (eye tones), highlight ellipse cx117 cy127 rx3 ry1.6 white 0.6
mouth    M120 136 L120 141 M108 140 Q114 147 120 141 Q126 147 132 140   width 2.5
whiskers M86 137 L50 130, M86 144 L50 147, M154 137 L190 130, M154 144 L190 147   width 1.6, alpha 0.8
bobble   crown M58 96 Q60 40 120 38 Q180 40 182 96 Q120 80 58 96 Z;  band M54 100 Q120 80 186 100 L184 86 Q120 66 56 86 Z;  pom circle (120,34) r15;  hat replaces the stripes
scarf    M62 156 Q120 182 178 156 L176 176 Q120 202 64 176 Z;  tail M138 178 L160 180 L168 220 L146 214 Z;  fringe lines at y214 to 228
crown    M86 62 L90 26 L105 44 L120 18 L135 44 L150 26 L154 62 Q120 54 86 62 Z;  gems (120,50) r5, (101,54) and (139,54) r3.5
collar   M64 158 Q120 180 176 158 L174 170 Q120 194 66 170 Z;  bell circle (120,190) r11 with slit M114 193 L126 193
```
