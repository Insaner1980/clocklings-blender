# Screenshot Review 6 Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the 23 findings from the owner's review of the `clocklings-screenshots6` set (61 Finnish screenshots, 2026-10-30 17:31 to 17:41), so that the clock always shows every numeral a lesson talks about, game feedback is unambiguous, and the wardrobe, Opi path, Pelaa yhdessä, Minuuttihaaste, kellotyylit and home screens look consistent.

**Architecture:** Most fixes are local to one composable or one constant. The clock fix (A1) adds a pure geometry helper to `ClockSpec` and one extra draw pass in `ClockFace`. Game outcomes (B1) add one list to `GameUiState` and an optional parameter to the shared `ProgressDots`. Wardrobe thumbnails (E1 to E3) change how `CompanionView` fits a crop and let rigs define per-item crops. Everything else is copy, constants and layout.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas drawing, `TextMeasurer`), Navigation 3, Preferences DataStore, JUnit4 + Robolectric unit tests, instrumented gallery tests on the dedicated emulator.

**Spec:** this document, sections 1 to 3 (finding index, dropped findings, owner decisions). Source review: the Finnish review message of 2026-10-30 for `C:\Users\EmmaH\Downloads\clocklings-screenshots6`. Earlier decisions that still apply: `docs/superpowers/plans/2026-10-27-screenshot-review-fixes.md` sections 1 and 2, and `docs/implementation/STATUS.md`.

## Global Constraints

- Follow `AGENTS.md` in the repository root. It overrides anything here.
- Work only in `C:\Dev\Clocklings`. Never touch `C:\Dev\Sateenkaarikello-Android`.
- Branch `codex/localization-settings` has a large amount of uncommitted owner work. Do not reset, stash, checkout, clean or discard anything. Edit on top of the working tree. Do not commit, push or open a PR. Every "Commit" step in the usual plan template is replaced by "leave the change uncommitted".
- Every user-visible string change goes into all three languages: `app/src/main/res/values/` (English source), `values-fi/`, `values-sv/`. Finnish and Swedish must read naturally, not word for word.
- Never use `·`, `•` or ` | ` as a separator in UI text or in this project's docs. Never add a coloured vertical stripe to the left edge of a card, row or panel.
- Use theme tokens (`SkColors`, `SkDimens`, `SkShapes`, `SkType`, `PersonalizationColors`). A new value becomes a token in the theme file, never an inline literal.
- No duplicated logic (3+ lines twice means extract). Delete code and string resources that become unused in the same change, in all three languages, and grep tests for their ids.
- Identifiers and new code comments in English. Existing Finnish comments may stay.
- Gradle only from the repository root with `--no-daemon`. Unit tests: `./gradlew --no-daemon :app:testDebugUnitTest`. Build: `./gradlew --no-daemon :app:assembleDebug`.
- Device checks only on the dedicated test emulator described in the project memory note `clocklings-validation-setup` (emulator-5590). Never a physical device or another running emulator.
- Changed UI is checked at 320 dp width, at 200% font scale, and both combined.
- The owner runs `lc` and `sc`. Do not run them and do not claim they passed.
- The owner answered all decisions D-1 to D-6 on 2026-10-30 by accepting every recommended option (section 3). Implement exactly the accepted options; the rejected alternatives are kept only as a record. Task E7 is skipped because of D-6.

## Review Focus

These conditions are implied by the findings but easy to miss. Each one has a test or a screenshot check in the task named after it.

1. **24-hour mode and help marks on the clock (A1):** redrawing numerals above the minute hand must not hide the inner 13 to 23 ring, the "yli/vaille" half colours or a lesson's yellow number glow. Screenshot check in A1 step 7.
2. **Reveal paths in the game (B1):** a SET task that ends in the answer being shown, and a READ task with one wrong answer, must produce the right outcome dot. Unit tests in B1 step 1 cover first try, hint and reveal for both task types.
3. **Long labels at 200% font scale on 320 dp (D1, B3):** the new full-width bottom-bar buttons in Pelaa yhdessä and Minuuttihaaste, including Swedish "Ge till din kompis", wrap at spaces and never clip. Screenshot check in D1 step 6 and B3 step 6.
4. **Reduced motion (B4):** with system animations off, confetti is either absent or fades, and clearing it on a new game task never crashes. Unit test in B4 step 1, device check in B4 step 6.
5. **Renamed and longer item names (E5):** new names fit the wardrobe tiles at 320 dp and 200% with the shared per-grid text size from the earlier task B7. Unit test for unique names per companion in E5 step 1, screenshot check in E5 step 5.

---

## 1. Finding index

Every finding from the review maps to exactly one task, a dropped entry, or a decision. Finding numbers are the ones in the review message.

1. Minute hand hides the numeral it points at: **A1**.
2. Game progress dots do not show right or wrong: **B1**.
3. Same title three times on the Opi path: **C1**.
4. Completed play node still says "Aloita pelaaminen!": **C1**.
5. Pelaa yhdessä main button below the fold, answer layout differs from Tutki: **D1**.
6. Half-width, start-aligned primary buttons: **D1** (Pelaa yhdessä), **B3** (Minuuttihaaste). The lesson navigation part is dropped (section 2).
7. Neck thumbnails show a floating mouth and other slots' items: **E1**, **E2**.
8. Accessory thumbnails too small to see the item: **E3**.
9. Doll's tap hint sits under "Päähineet": **E4**.
10. Same item has different names across companions, "Kukka korvan takana" on the dinosaur: **E5**.
11. "3 / 15" badge and "3 / 7" pills read as progress, not as a threshold: **E7**, skipped by decision D-6 (no change).
12. Kellotyylit: selected style not visible, zoom thumbnails show a lone "0" and a cut "2": **F1**.
13. Home: large empty area above the menu, hanger button covers the companion: **G1**.
14. Unicorn's near-white colour swatch disappears: **E6**.
15. Dragon's two purples and dinosaur's two greens look alike: **E6**.
16. Tutki help halves have no legend: dropped (section 2).
17. Tarkat minuutit tick highlights barely visible: **A2**.
18. Minuuttihaaste: live-looking clock before start, "Tasot" label, result layout: **B3**.
19. Empty space under the answer card in games: **B2**.
20. Settings remarks: dropped (section 2).
21. Lesson nodes ahead look like open nodes: dropped (section 2).
22. Confetti stars left on screen on the next question: **B4**.
23. Half-faded text under the top bar while scrolling: **H1**.

## 2. Dropped findings (do NOT implement)

- **Finding 16 (Tutki half colours have no legend):** `ClockFace.clockZoneLabels()` deliberately returns `null` for Finnish and Swedish because those languages refer to the half hour on either side of the face, so a fixed "yli"/"vaille" label would teach something wrong. No change.
- **Finding 20 (settings):** the off-state switch colours (E1), the help-marks wording (E2) and the read-aloud note below the sound card (E3) were all owner decisions in the 2026-10-27 plan and are implemented. The bulb in Tutki and games toggles the same global `Settings.help` (dropped finding 10 of that plan). No change.
- **Finding 21 (nodes ahead look open):** all lessons and levels are intentionally open (dropped finding 31 of the 2026-10-27 plan). No change.
- **Finding 6, lesson navigation part:** when the quiz is not yet answered, the lone "Edellinen" button stays half width on the start side. That was owner decision B2.3 of the 2026-10-27 plan. No change.

## 3. Owner decisions (answered 2026-10-30)

All six decisions were answered by the owner on 2026-10-30: every recommended option is accepted.

### D-1. Opi path naming (finding 3, task C1)

Cause: decision 2.3 of the 2026-10-27 plan made every level name equal to a lesson title, and decision 2.1 added a chapter header and a play node titled with the level name. Result, for example in the first chapter: header "Tasatunnit", lesson 3 "Tasatunnit", play node "Tasatunnit". Every chapter has this repetition, because every chapter contains a lesson named like the level.

- **Option A (accepted by the owner, 2026-10-30):** remove the chapter header text. Chapters stay recognisable by their level colour (decision 2.4) and by the play node that closes them. The play node title becomes a new string `lesson_practise`: en "Practise", fi "Harjoittele", sv "Öva". The level name stays in the play node's accessibility description. This reverses item 1 of decision 2.1; the owner approved the reversal.
- **Option B (rejected):** keep the headers. The play node title becomes `lesson_practise` as in A, and the five lessons whose titles equal their level name get new titles (`l_tasan_title`, `l_puoli_title`, `l_vartti_title`, `l_viisi_title`, `l_minuutit_title`, 15 strings). The owner supplies or approves the 15 new titles.

### D-2. Meaning of game progress dots (finding 2, task B1)

Today a done dot takes the next rainbow colour (`SkColors.Rainbow7`), so the first done dot is always red, even after a correct answer.

- **Accepted by the owner (2026-10-30):** in games only (lessons keep rainbow page dots), a done dot shows how the task went. First try: `SkColors.Good` fill with a white `SkIcons.Check`. Right after a hint: `SkColors.GoldDeep` fill with a white check. Answer shown after two mistakes: `SkColors.DotEmpty` fill with a 2 dp `SkColors.InkSoft` border and no icon. No red anywhere.
- **Rejected alternative:** keep rainbow colours, but start the sequence at the green index of `Rainbow7` so no done dot is ever red, and add nothing else.

### D-3. What an item thumbnail shows (finding 7, task E2)

Today a thumbnail shows the companion wearing the current outfit plus the item, so a neck thumbnail also shows the carrot, the coin bag or the ball of yarn.

- **Accepted by the owner (2026-10-30):** a thumbnail shows only the item on the companion. The doll keeps its required clothes (hair, dress, shoes) because it cannot be bare.
- **Rejected alternative:** keep the current outfit and only fix the crop (E1).

### D-4. Item names (finding 10, task E5)

Accepted renames. Everything not listed stays.

- Bow ties, `item_{cat,dog,robot,dinosaur}_neck_bow_tie`: fi "Rusetti kaulaan" becomes "Rusettisolmuke". en "Bow tie" and sv "Fluga" stay.
- Plain scarves, `item_dragon_neck_scarf` and `item_dinosaur_neck_scarf`: fi "Huivi" becomes "Kaulahuivi" (matches en "Scarf"). sv "Halsduk" stays.
- Neckerchiefs, `item_unicorn_neck_scarf` (en "Neckerchief") and `item_dog_neck_bandana`: fi both become "Kaulaliina". en stays ("Neckerchief", "Bandana"). sv stays unless the owner gives new names.
- `item_dinosaur_head_flower`: en "Flower behind the ear" becomes "Head flower", fi "Kukka korvan takana" becomes "Kukka päässä", sv "Blomma bakom örat" becomes "Blomma på huvudet". The dinosaur has no visible ears.

### D-5. Companion colour options that look alike (finding 15, task E6 step 5)

`PersonalizationColors.DragonPurple` (`0xFF8D68C8`) next to the dragon's own `Dragon` (`0xFFAE94DC`), and `DinosaurGreen` (`0xFF4E9C86`) next to `Dinosaur` (`0xFF86B98C`), read as the same colour on the swatch row.

- **Accepted by the owner (2026-10-30):** keep the `CompanionColor` enum and its labels. Require a CIE76 colour difference of at least 25 between any two options of one companion (a unit test in E6 enforces it). Darken the failing override colours until the test passes, then show the owner a screenshot of both swatch rows and both companions before finishing.
- **Rejected alternative:** the owner picks new hex values directly.

### D-6. Star threshold display (finding 11, task E7)

Decision 2.2 of the 2026-10-27 plan chose "current / needed" pills ("3 / 7") and the header badge uses the same format ("3 / 15"). In the review both read like progress bars toward something.

- **Accepted by the owner (2026-10-30):** no change. The explanation line "Tähdet eivät kulu..." under the stage already explains it. Skip E7.
- **Rejected alternative:** locked pills show only how many stars are missing: new plurals `doll_stars_missing`, en "%1$d more star" / "%1$d more stars", fi "Vielä %1$d tähti" / "Vielä %1$d tähteä", sv "%1$d stjärna till" / "%1$d stjärnor till". The header badge keeps "3 / 15".

---

## 4. Tasks

Do the phases in order. Each task must compile and pass unit tests before the next starts.

### Phase A: Clock face

### Task A1: The numeral under the minute hand stays readable

Findings: 1. Screenshots 173606, 173643, 173711, 173830, 173846, 173927, 174002, 174112 (12, 6, 9 and 3 hidden or split).

Facts (ClockSpec units, 400 x 400 face, centre 200): `NUMBER_RADIUS = 110`, `NUMBER_FONT = 39`, so a numeral spans roughly radius 90 to 130. `MINUTE_LENGTH = 150` must stay, because the minute hand has to reach the minute ticks. So the shaft always crosses the numeral it points at, and `drawHands` paints it on top. Shortening the hand is not allowed.

Fix: after the hands are drawn, draw every numeral that lies under the minute hand again, on top, with a halo in the colour under it. The hand then visibly passes beneath the number.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/clock/ClockSpec.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/clock/ClockFace.kt` (`ClockFace` draw block around lines 144 to 155, `drawNumbers` around 376 to 391, `ClockTextStyles` around 232 to 249)
- Test: `app/src/test/java/fi/clocklings/ui/clock/NumeralCoverTest.kt` (create)

**Interfaces:**
- Produces: `ClockSpec.NUMERAL_COVER_ANGLE: Float = 16f`, `ClockSpec.NUMERAL_HALO: Float = 6f`, `fun ClockSpec.numeralsUnder(handAngle: Float): List<Int>` (hour numerals 1..12 in ascending order whose angle `n * 30` is within `NUMERAL_COVER_ANGLE` degrees of `handAngle`, using `ClockTime.angleDiff`).

- [ ] **Step 1: Write the failing test**

```kotlin
class NumeralCoverTest {
    @Test fun `hand at twelve covers twelve`() = assertEquals(listOf(12), ClockSpec.numeralsUnder(0f))
    @Test fun `hand at six covers six`() = assertEquals(listOf(6), ClockSpec.numeralsUnder(180f))
    @Test fun `hand at nine covers nine`() = assertEquals(listOf(9), ClockSpec.numeralsUnder(270f))
    @Test fun `hand just before twelve covers twelve`() = assertEquals(listOf(12), ClockSpec.numeralsUnder(350f))
    @Test fun `hand halfway between one and two covers both`() = assertEquals(listOf(1, 2), ClockSpec.numeralsUnder(45f))
    @Test fun `hand at minute 3 covers only one`() = assertEquals(listOf(1), ClockSpec.numeralsUnder(18f))
}
```

- [ ] **Step 2: Run it and see it fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.clock.NumeralCoverTest"`
Expected: compilation failure, `numeralsUnder` unresolved.

- [ ] **Step 3: Implement `numeralsUnder` and the two constants in `ClockSpec`**

The 16 degree threshold comes from half the width of "12" (about 22 units at radius 110, about 11 degrees) plus half the minute hand width and its halo (5 + 3 units, about 4 degrees). Write that reasoning as the KDoc of `NUMERAL_COVER_ANGLE`.

- [ ] **Step 4: Run the test and see it pass**

Same command. Expected: 6 tests PASS.

- [ ] **Step 5: Add the halo style and the redraw pass in `ClockFace.kt`**

- In `ClockTextStyles` add `val numberHalo = number.copy(drawStyle = Stroke(width = S.NUMERAL_HALO, join = StrokeJoin.Round))`. The colour is set per draw call.
- Extract the body of `drawNumbers` into `private fun DrawScope.drawNumeral(measurer: TextMeasurer, n: Int, style: TextStyle)` and make `drawNumbers` call it, so the redraw does not duplicate the position maths.
- Add `private fun DrawScope.drawCoveredNumerals(measurer, styles, raw: Float, help: Boolean, under: List<Mark>)`. For each `n` in `S.numeralsUnder(ClockTime.minuteAngle(raw))`: pick the halo colour, draw `drawNumeral(measurer, n, styles.numberHalo.copy(color = halo))`, then `drawNumeral(measurer, n, styles.number)`.
- Halo colour, first match wins: `SkColors.MarkNumber` if `under` contains `Mark.NumberGlow(n)`; the colour of a `Mark.Sector` in `under` whose minute range contains `n * 5` (via `S.markColor`); `SkColors.Yli` when `help` is on and `n` is 1 to 5; `SkColors.Vaille` when `help` is on and `n` is 7 to 11; otherwise `SkColors.Face`. Write this as a small pure function `internal fun ClockSpec.numeralBackground(n: Int, help: Boolean, under: List<Mark>): Color` and add three cases to `NumeralCoverTest` (glow wins, help right half gives `Yli`, plain gives `Face`).
- Call it in the `ClockFace` draw block right after `drawHands(...)` and before the pin circles. Pass the same `help && !mini` flag that `drawHelp` uses.

- [ ] **Step 6: Run all unit tests**

Run: `./gradlew --no-daemon :app:testDebugUnitTest`
Expected: all PASS.

- [ ] **Step 7: Screenshot check on the emulator**

Render with the gallery instrumentation (`GalleryTest`, procedure in memory note `clocklings-validation-setup`): 12:00, 1:30, 4:45, 5:15, 10:50, 11:55 with `HandStyle.ROUND` and `HandStyle.POINTER`; Tutki with help on at 3:00 and 9:45 in 12 h and 24 h mode; lesson Puoli tuntia page 1; lesson Kaksi viisaria page 3 (yellow glows); a mini clock on the Pelaa list at 1:30. Acceptance: every digit of every numeral is fully readable, the hand visibly continues on both sides of the numeral, no white ring shows on a coloured background, the inner 24 h numerals are untouched.

### Task A2: Tarkat minuutit tick highlights are visible

Findings: 17. Screenshot 173954 (two tiny pink dots at minutes 1 and 2).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/clock/ClockSpec.kt` (`GLOW_TICK_RADIUS`)
- Modify: `app/src/main/java/fi/clocklings/ui/clock/ClockFace.kt` (`drawMark`, `Mark.TickGlow` branch)

- [ ] **Step 1: Make the glow larger and outlined**

`GLOW_TICK_RADIUS` from 7f to 10f. In the `Mark.TickGlow` branch draw the existing `SkColors.MarkTick` fill, then a ring `drawCircle(SkColors.Hour, S.GLOW_TICK_RADIUS, at, style = Stroke(S.BUBBLE_STROKE))`.

- [ ] **Step 2: Check overlap**

Neighbouring tick glows are 6 degrees apart at radius 157, about 16 units. With radius 10 two neighbouring glows overlap by about 4 units. That is acceptable (they read as a row of counted minutes); if the screenshot shows a blob, use 9f.

- [ ] **Step 3: Unit tests and screenshot**

Run `./gradlew --no-daemon :app:testDebugUnitTest` (expected PASS). Screenshot lesson Tarkat minuutit page 1 mid-count at 320 dp: each counted minute is visible as a separate highlighted tick.

### Phase B: Game and Minuuttihaaste

### Task B1: Progress dots show how each game task went

**Decision D-2: accepted recommendation.** Findings: 2. Screenshot 173802.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/Display.kt` (`ProgressDots`, around lines 112 to 155)
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt` (`GameUiState`, `taskState`, `advance`)
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt` (call site around line 148)
- Test: `app/src/test/java/fi/clocklings/ui/game/GameViewModelTest.kt`

**Interfaces:**
- Produces: `enum class DotOutcome { FIRST_TRY, WITH_HINT, SHOWN }` in `Display.kt`; `GameUiState.outcomes: List<DotOutcome> = emptyList()` (one entry per finished task, in order); `ProgressDots(..., outcomes: List<DotOutcome>? = null)`. When `outcomes` is null the dots keep today's rainbow behaviour (lessons).
- Internal: `internal fun outcomeOf(taskMistakes: Int): DotOutcome` in `GameViewModel.kt`: 0 gives `FIRST_TRY`, 1 gives `WITH_HINT`, `MAX_TRIES` or more gives `SHOWN`.

- [ ] **Step 1: Write the failing tests** (use the same `FakeServices`, dispatcher rule and seeded `Random` as the existing tests in the file)

```kotlin
@Test fun `correct first answer records FIRST_TRY`() = runTest {
    // pick the right option of task 0, let the success pause pass
    assertEquals(listOf(DotOutcome.FIRST_TRY), vm.state.value.outcomes)
}
@Test fun `one wrong answer then right records WITH_HINT`() = runTest {
    // task 0: pick a wrong option, then the right one, let the pause pass
    assertEquals(listOf(DotOutcome.WITH_HINT), vm.state.value.outcomes)
}
@Test fun `two wrong answers then next records SHOWN`() = runTest {
    // task 0: two wrong options, then vm.next()
    assertEquals(listOf(DotOutcome.SHOWN), vm.state.value.outcomes)
}
@Test fun `set task shown after two mistakes records SHOWN`() = runTest {
    // reach a SET task the same way `second set mistake shows the answer on the clock` does,
    // call done() twice with a wrong value, then next()
    assertEquals(DotOutcome.SHOWN, vm.state.value.outcomes.last())
}
@Test fun `outcomeOf maps mistakes`() {
    assertEquals(DotOutcome.FIRST_TRY, outcomeOf(0))
    assertEquals(DotOutcome.WITH_HINT, outcomeOf(1))
    assertEquals(DotOutcome.SHOWN, outcomeOf(2))
}
```

- [ ] **Step 2: Run them and see them fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.game.GameViewModelTest"`
Expected: compilation failure (`outcomes`, `DotOutcome`, `outcomeOf` unresolved).

- [ ] **Step 3: Implement**

- `taskState(index, mistakes, celebrate, from)` copies `from?.outcomes.orEmpty()` into the new state.
- In `advance()`, before building the next state or calling `finish`, compute `s.outcomes + outcomeOf(s.taskMistakes)` and carry it into the next state (and into the final state before `finish`).
- `ProgressDots`: for `k < current` with `outcomes != null`, draw by `outcomes.getOrNull(k)`: `FIRST_TRY` fill `SkColors.Good` with `SkIcon(SkIcons.Check, ...)` in white centred (icon size `SkDimens.ProgressDot * 0.75f`, add token `SkDimens.ProgressDotIcon = 12.dp`); `WITH_HINT` fill `SkColors.GoldDeep` with the same white check; `SHOWN` fill `SkColors.DotEmpty` with `border(SkDimens.BorderThin, SkColors.InkSoft, CircleShape)`. For `k == current && currentDone`, `PoppingDot` gets `SkColors.Good` instead of the rainbow colour when `outcomes != null` (success is the only path that pops). If `SkIcons.Check` is drawn in ink colour, tint it with `SkColors.Surface` the way `SelectionBadge` does.
- `GameScreen` passes `outcomes = state.outcomes`. `LessonScreen` does not change.

- [ ] **Step 4: Run the tests and see them pass**

Same command. Expected: all `GameViewModelTest` tests PASS, including the existing ones.

- [ ] **Step 5: Screenshot check**

Play Tasatunnit: answer task 1 right, task 2 wrong then right, task 3 wrong twice. Screenshot the dot row at 320 dp: green with check, amber with check, grey ringed, then the current ring.

### Task B2: Less empty space under the answer card in games

Findings: 19. Screenshots 173750, 173830 (about 300 px empty below the card).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt` (`clockHeightShare` at line 137)

- [ ] **Step 1: Add a game-only token**

`const val GAME_CLOCK_HEIGHT_SHARE = 0.58f` in `SkDimens`, with a KDoc saying it is the read/set game screen share and that Ask and Challenge keep `PLAY_CLOCK_HEIGHT_SHARE`.

- [ ] **Step 2: Use it in `GameScreen`**

- [ ] **Step 3: Tune on the emulator**

On a 411 x 914 dp configuration at 100% font: the clock, the dot row, the "Mitä kello on?" card with three options, and the companion hint after a wrong answer all fit without scrolling. Raise the value in 0.02 steps up to at most 0.62 while that holds, or lower it if it fails. On 360 x 640 dp scrolling is allowed but nothing may be clipped. Record the final value and the two screenshots.

- [ ] **Step 4: Unit tests**

Run `./gradlew --no-daemon :app:testDebugUnitTest`. Expected PASS.

### Task B3: Minuuttihaaste start and result screens

Findings: 18, 6. Screenshots 174002, 174112.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/challenge/ChallengeScreen.kt` (START branch around line 140, `ChallengeResult` around 220 to 250, clock around 130)
- Modify: `app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt` (line 131, `levels_short`)
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt`
- Modify: `app/src/main/res/values*/strings.xml` (delete `challenge_again`, `levels_short` if unused)

- [ ] **Step 1: Mark the start clock as not yet in play**

The START phase shows `START_TIME = 45` (12:45), which looks like a question. Wrap the `ClockFace` in the START phase with `Modifier.alpha(SkDimens.INACTIVE_CLOCK_ALPHA)` (new token, `const val INACTIVE_CLOCK_ALPHA = 0.45f`). RUN and END phases are unchanged.

- [ ] **Step 2: Full-width buttons**

"Aloita!" (`challenge_go`) gets `Modifier.fillMaxWidth()`. In `ChallengeResult` replace the `Row` with a `Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM))`: primary button full width, then the back button full width.

- [ ] **Step 3: Clear labels and no duplicate strings**

- Retry/again button: `R.string.play_again` instead of `R.string.challenge_again` (same text in all three languages today). Delete `challenge_again` from all three `strings.xml`.
- Back button: `R.string.back_description` ("Takaisin" / "Back" / "Tillbaka") instead of `R.string.levels_short` ("Tasot"), because the Pelaa screen is no longer titled "Tasot" (task C2 of the 2026-10-27 plan). Do the same in `ResultScreen.kt` line 131. Then grep `levels_short` in `app/src`; if unused, delete it from all three languages.
- Grep `app/src/androidTest` and `app/src/test` for `challenge_again`, `levels_short`, "Tasot", "Levels", "Nivåer" and update the tests.

- [ ] **Step 4: Unit tests**

Run `./gradlew --no-daemon :app:testDebugUnitTest`. Expected PASS (fix `ScreenInteractionTest` or `ChallengeViewModelTest` text lookups if they used the old strings).

- [ ] **Step 5: Instrumented test**

Run `ChallengeScreenTest` on the dedicated emulator. Expected PASS.

- [ ] **Step 6: Screenshots**

Start screen and result screen in fi, en, sv at 320 dp, 200% and both. Buttons full width, labels wrap at spaces, the start clock reads as inactive.

### Task B4: Confetti does not linger into the next question

Findings: 22. Screenshot 173802 (stray stars at the left edge and near the card on question 2).

Cause: particles live `LIFE = 110` frames (about 1.83 s) with full opacity, and are removed only when `life >= LIFE` or they fall below the screen. The game moves on after `SUCCESS_PAUSE_MS = 1500`, so stragglers stay visible on the new question. Landing sparkles start even later (after `FLIGHT_MS = 650`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/ConfettiMath.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/components/Confetti.kt` (particle drawing, `ConfettiState`)
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt`
- Test: `app/src/test/java/fi/clocklings/ui/components/ConfettiMathTest.kt`, `ConfettiStateTest.kt`

**Interfaces:**
- Produces: `fun ConfettiMath.particleAlpha(life: Int, lifeSpan: Int, fadeFrames: Int): Float` (1 until `lifeSpan - fadeFrames`, then linear down to 0 at `lifeSpan`, clamped to 0..1); `fun ConfettiState.clear()` (empties `particles` and `flights`).

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `particle is opaque before the fade`() = assertEquals(1f, ConfettiMath.particleAlpha(64, 110, 30), 0.001f)
@Test fun `particle is half faded mid fade`() = assertEquals(0.5f, ConfettiMath.particleAlpha(95, 110, 30), 0.001f)
@Test fun `particle is invisible at end of life`() = assertEquals(0f, ConfettiMath.particleAlpha(110, 110, 30), 0.001f)
// ConfettiStateTest
@Test fun `clear removes particles and flights`() {
    // burst and flyStar as in the existing tests, then
    state.clear()
    assertFalse(state.isActive)
}
```

- [ ] **Step 2: Run them and see them fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.components.*"`
Expected: compilation failure.

- [ ] **Step 3: Implement**

- `particleAlpha` in `ConfettiMath`. New constant `FADE_FRAMES = 30` next to `LIFE` in `ConfettiState.Companion`. Multiply every particle's draw colour alpha by `particleAlpha(p.life, LIFE, FADE_FRAMES)`.
- `ConfettiState.clear()`.
- In `GameScreen`, `LaunchedEffect(state.taskIndex) { LocalConfetti.current?.clear() }` (read `LocalConfetti.current` outside the effect). Do not clear on the first composition if that would remove nothing important; clearing an empty state is harmless either way.

- [ ] **Step 4: Run tests**

Same command, then the full `:app:testDebugUnitTest`. Expected PASS.

- [ ] **Step 5: Screenshot**

Answer task 1 correctly in Tasatunnit; capture the first frame of task 2. No confetti or sparkle on screen.

- [ ] **Step 6: Reduced motion**

With the emulator's animator duration scale set to 0 (`adb -s emulator-5590 shell settings put global animator_duration_scale 0`), repeat step 5, then restore the previous value. No crash, no stray particles.

### Phase C: Opi path

### Task C1: Play node title and label

**Decision D-1: accepted recommendation.** Findings: 3, 4. Screenshots 173542, 173553, 173600.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt` (`LessonListScreen`, lines 87 to 154)
- Modify: `app/src/main/res/values*/strings.xml`
- Test: `app/src/test/java/fi/clocklings/ui/ScreenInteractionTest.kt` (or the existing lesson list test there)

- [ ] **Step 1: Write the failing test**

Compose `LessonListScreen` with `progress.stars[0] = 3` and all lessons of level 0 done. Assert: a node with text `lesson_practise` exists; within the level-0 play node the text `play_again` is shown and `lesson_start_game` is not; for level 1 (0 stars) `lesson_start_game` is shown. Also assert that no `SectionTitle` heading with the text `level_tasatunnit` exists.

- [ ] **Step 2: Run and see it fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.ScreenInteractionTest"`
Expected: FAIL.

- [ ] **Step 3: Implement**

- Add `lesson_practise` (en "Practise", fi "Harjoittele", sv "Öva").
- Play node `title` becomes `stringResource(R.string.lesson_practise)`. Its accessibility description becomes `"$chapterTitle. $practise. $action. $starDescription"`.
- Badge action text: `if (stars > 0) stringResource(R.string.play_again) else stringResource(R.string.lesson_start_game)`.
- Delete the chapter header `Text` (line 112) and the `chapterTitle` lookup for the `vrk` chapter if it becomes unused. Keep `chapterTitle` for the play node description.
- Update `docs/implementation/STATUS.md` decisions: one sentence: chapter headers removed, play node "Harjoittele" (D-1, reverses item 1 of decision 2.1 of the 2026-10-27 plan).

- [ ] **Step 4: Run tests**

Same command, then the full unit suite. Expected PASS.

- [ ] **Step 5: Screenshots**

Opi path top and bottom in fi, en, sv; 320 dp and 200%. No title appears twice in a row.

### Phase D: Pelaa yhdessä

### Task D1: Actions in a bottom bar, answer laid out like Tutki

Findings: 5, 6. Screenshots 173643, 173651, 173711, 173718.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/ask/AskScreen.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/freeclock/FreeClockScreen.kt` (`ExplorePanel`, lines 110 to 140)
- Modify: `app/src/main/java/fi/clocklings/ui/components/Display.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ScreenInteractionTest.kt`, `app/src/androidTest/java/fi/clocklings/AppFlowTest.kt`

**Interfaces:**
- Produces: `@Composable fun TimeReadout(time: Int, h24: Boolean, speechAvailable: Boolean, onSpeak: (String) -> Unit, modifier: Modifier = Modifier)` in `Display.kt`: a `Row` with a weighted `Column` (words in `SkType.Words`, `DigitalPill`) and, when `speechAvailable`, a large `RoundIconButton(SkIcons.Listen, listen)` on the end that calls `onSpeak(spokenTime(res, time, h24))`. Exactly the layout `ExplorePanel` has today.

- [ ] **Step 1: Write the failing test**

In `ScreenInteractionTest`, compose `AskScreen`: assert the node with text `ask_go` is displayed without scrolling (use `assertIsDisplayed()` on a 360 x 640 dp test configuration); tap it; assert `ask_reveal` is displayed; tap it; assert `ask_new` and the digital time are displayed.

- [ ] **Step 2: Run and see it fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.ScreenInteractionTest"`
Expected: FAIL on the first `assertIsDisplayed` (the button is below the fold).

- [ ] **Step 3: Implement**

- Move the Row from `ExplorePanel` into `TimeReadout`; `ExplorePanel` calls `TimeReadout(time, h24, speechAvailable, app::speak)`.
- `AskAnswer` becomes `TimeReadout(time, h24, speechAvailable, app::speak)`; delete the old body.
- `AskScreen` passes `bottomBar = { AskActions(phase, ...) }` to `ScreenScaffold`. `AskActions` shows one full-width button per phase: SET `ask_go` (PRIMARY, `SkIcons.Pass`), ASK `ask_reveal` (PRIMARY, keeps the confetti anchor and the haptic/sound/confetti code now in `AskQuestion`), REVEALED `ask_new` (GREEN). Remove those buttons from `AskSetup` and `AskQuestion`; the "Satunnainen aika" button stays in the panel.
- The answer (`TimeReadout`) stays inside the panel under the companion tip.

- [ ] **Step 4: Run tests**

Same command, then the full unit suite. Expected PASS.

- [ ] **Step 5: Instrumented test**

Run `AppFlowTest` on the dedicated emulator. Update any lookups that scrolled to `ask_go`. Expected PASS.

- [ ] **Step 6: Screenshots**

All three phases in fi, en, sv at 320 dp, 200% and both. The phase action is always visible without scrolling, labels wrap at spaces, the revealed answer matches the Tutki layout (speaker on the end of the words row).

### Phase E: Wardrobe

### Task E1: Thumbnail crops grow downwards, not upwards

Findings: 7 (first part). Screenshots 173119, 173207, 173223, 173249, 173303, 173334, 173356 (a mouth, tongue or snout line floating at the top of neck thumbnails).

Cause: `CompanionView.kt` `Rect.grownTo(canvas)` grows a wide crop to the square window symmetrically, so a neck crop (for example dog `Rect(66, 158, 174, 238)`, 108 x 80) gains 14 units above its top edge, where the mouth is.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/companion/CompanionView.kt` (`grownTo`, lines 86 to 95)
- Modify: the `neckCrop` of each rig if needed: `BunnyRig.kt`, `CatRig.kt`, `DinosaurRig.kt`, `DogRig.kt`, `DragonRig.kt`, `RobotRig.kt`, `UnicornRig.kt`
- Test: `app/src/test/java/fi/clocklings/ui/companion/CropFitTest.kt` (create)

**Interfaces:**
- Produces: `internal fun Rect.grownTo(canvas: Size): Rect` (visibility from private to internal). Vertical growth keeps `top` and extends `bottom`; horizontal growth stays centred.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `wide crop grows downwards only`() =
    assertEquals(Rect(66f, 158f, 174f, 266f), Rect(66f, 158f, 174f, 238f).grownTo(Size(76f, 76f)))
@Test fun `tall crop still grows on both sides`() =
    assertEquals(Rect(-50f, 0f, 150f, 200f), Rect(0f, 0f, 100f, 200f).grownTo(Size(100f, 100f)))
```

- [ ] **Step 2: Run and see it fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.companion.CropFitTest"`
Expected: compilation failure (private), then FAIL on the first test.

- [ ] **Step 3: Implement** the top-anchored branch.

- [ ] **Step 4: Run the test and the whole suite.** Expected PASS (`CompanionRenderingTest` included).

- [ ] **Step 5: Tune neck crop tops**

Capture every NECK thumbnail of all seven animals with `PersonalizationGalleryTest`. Acceptance: no part of the mouth, tongue, whiskers or snout outline is visible, and every neck item is complete (pendants, cape clasps, scarf ends). For the robot, the dark bottom edge of the head plate may show as a thin band of at most 4 dp; if it is thicker, move `RobotRig.neckCrop.top` down. Change a rig's `neckCrop.top` only when the acceptance fails, and keep each rig's existing comment true. Also re-check HEAD thumbnails, which now show a little more chin instead of empty space above.

### Task E2: A thumbnail shows only its own item

**Decision D-3: accepted recommendation.** Findings: 7 (second part).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/domain/Wardrobe.kt` (`Outfit`)
- Modify: `app/src/main/java/fi/clocklings/ui/companion/ItemArtWindow.kt`
- Test: `app/src/test/java/fi/clocklings/domain/WardrobeTest.kt`

**Interfaces:**
- Produces: `fun Outfit.previewFor(item: Item): Outfit` = the required (non-optional) slots of this outfit plus `item.slot to item.id`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun `preview keeps required clothes and only the previewed item`() {
    val outfit = Outfit(mapOf(Slot.HAIR to "hair-letit", Slot.DRESS to "dress-perus", Slot.SHOES to "shoes-tossut",
        Slot.HEAD to "head-rusetti"))
    val item = Wardrobe.items.first { it.id == "extra-laukku" }
    val preview = outfit.previewFor(item)
    assertEquals(setOf(Slot.HAIR, Slot.DRESS, Slot.SHOES, Slot.EXTRA), preview.items.keys)
    assertEquals("extra-laukku", preview[Slot.EXTRA])
}
@Test fun `animal preview contains only the item`() {
    val item = Wardrobe.items.first { it.id == "cat-neck-scarf" }
    assertEquals(mapOf(Slot.NECK to "cat-neck-scarf"), Outfit(mapOf(Slot.EXTRA to "cat-extra-yarn")).previewFor(item).items)
}
```

The doll ids above are the real ones from `Wardrobe.kt` (`head-rusetti`, `extra-laukku`).

- [ ] **Step 2: Run and see it fail**

Run: `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.domain.WardrobeTest"`

- [ ] **Step 3: Implement** `previewFor`; in `ItemArtWindow` replace `outfit.with(item.slot, item.id)` with `outfit.previewFor(item)`.

- [ ] **Step 4: Run tests.** Expected PASS.

- [ ] **Step 5: Screenshots** of the bunny, dragon and cat wardrobes: neck thumbnails show no carrot, coin bag or yarn.

### Task E3: Accessory thumbnails zoom to the item

Findings: 8. Screenshots 173127, 173215, 173230, 173256, 173319, 173342, 173410, 173433 (frisbee, ball, wand, heart eyes, bag hard to see).

Today `AnimalRig.crop(slot)` returns the whole 240 x 300 grid for EXTRA, and the car's MODEL slot does the same.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/companion/CompanionRig.kt` (`CompanionRig`, `AnimalRig`)
- Modify: every rig with EXTRA items: `BunnyRig.kt`, `CatRig.kt`, `DinosaurRig.kt`, `DogRig.kt`, `DragonRig.kt`, `RobotRig.kt`, `UnicornRig.kt`, and `ui/doll/DollRig.kt` for the doll's extras
- Modify: `app/src/main/java/fi/clocklings/ui/companion/CompanionView.kt` (`cropSlot` parameter), `ItemArtWindow.kt`
- Test: `app/src/test/java/fi/clocklings/ui/CompanionRenderingTest.kt`

**Interfaces:**
- Produces: `fun CompanionRig.crop(item: Item): Rect` with default implementation `crop(item.slot)`; `AnimalRig` gains a constructor parameter `itemCrops: Map<String, Rect> = emptyMap()` and overrides `crop(item)` as `itemCrops[item.id] ?: crop(item.slot)`. `CompanionView`'s `cropSlot: Slot?` becomes `cropItem: Item?` (grep all callers; today only `ItemArtWindow`).

- [ ] **Step 1: Write the failing test**

In `CompanionRenderingTest`: for every `Item` in `Wardrobe.items` with `slot == Slot.EXTRA`, let `rig = rigFor(item.companion)!!` (`CompanionRig.kt` line 195) and assert `rig.crop(item) != rig.full()`. The doll is included (`rigFor(HUMAN)` is `DollRig`, whose `crop(slot)` delegates to `DollParts.crop`); give `DollRig` its own `crop(item)` override with crops for `extra-laukku` and `extra-siivet`.

- [ ] **Step 2: Run and see it fail.**

- [ ] **Step 3: Implement the interface and one crop per EXTRA item**

Start each crop from the item's drawing bounds in its `*Items.kt` file (the design-grid coordinates of the item's paths), add 12 units of margin on every side, and make it no smaller than 120 x 120 units so the companion stays recognisable. Items that sit far from the body (balloon, kite, wings, rainbow) need a crop that includes the nearest part of the companion.

- [ ] **Step 4: Run tests.** Expected PASS.

- [ ] **Step 5: Screenshot acceptance**

Capture every EXTRA thumbnail with `PersonalizationGalleryTest`, locked (grey) and unlocked. Acceptance: the whole item is visible; the item spans at least 35% of the window's width or height; part of the companion is visible so the child sees whose item it is. Leave MODEL (car transformations) on the full grid.

### Task E4: One tap hint, in the same place for every companion

Findings: 9. Screenshots 173410, 173425, 173433.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/companion/WardrobeScreen.kt` (lines 120 to 140)
- Modify: `app/src/main/res/values*/strings.xml`

- [ ] **Step 1: Add a doll-specific hint**

New string `doll_optional_hint_partial`: en "Tap an item to put it on. Hats and extras come off when you tap them again.", fi "Pue asuste napauttamalla. Päähineen ja lisävarusteen voi riisua napauttamalla uudelleen.", sv "Tryck på en sak för att ta på den. Huvudbonader och tillbehör tas av om du trycker igen."

- [ ] **Step 2: Show the hint once, above the first section**

Remove `hintSlot` and the per-section hint. Right where the animals' hint is today (line 126), show `doll_optional_hint` when `kind.slots.all { it.optional }`, otherwise `doll_optional_hint_partial`.

- [ ] **Step 3: Unit tests and screenshots**

Full unit suite PASS; update `PersonalizationGalleryTest` or `LocalizationContentTest` lookups if they searched the old position. Doll and cat wardrobes in fi, en, sv.

### Task E5: Item names

**Decision D-4: accepted recommendation.** Findings: 10.

**Files:**
- Modify: `app/src/main/res/values/companion_items.xml`, `values-fi/companion_items.xml`, `values-sv/companion_items.xml`
- Test: `app/src/test/java/fi/clocklings/ui/ItemNamesTest.kt`

- [ ] **Step 1: Write the failing guard test**

`names are unique within each companion in every language`: for each locale in en, fi, sv (Robolectric `@Config(qualifiers = ...)` or `createConfigurationContext`), group `Wardrobe.items` by `companion` and assert the resolved names in each group are distinct. This passes today; it guards the renames.

- [ ] **Step 2: Apply the approved renames from D-4.**

- [ ] **Step 3: Run** `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.ItemNamesTest"`. Expected PASS.

- [ ] **Step 4: Grep tests** for the old names ("Rusetti kaulaan", "Kukka korvan takana", "Flower behind the ear", "Blomma bakom örat") and update them.

- [ ] **Step 5: Screenshots** of the renamed tiles in all three languages at 320 dp and 200%. Names fit with the shared per-grid size.

### Task E6: Colour swatches

Findings: 14, 15. Screenshots 173109, 173207, 173241.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/SwatchTile.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/theme/PersonalizationColors.kt` (step 5 only)
- Test: `app/src/test/java/fi/clocklings/ui/art/VinylTonesTest.kt` (or a new `SwatchColorsTest.kt` next to it)

**Interfaces:**
- Produces: `internal fun swatchNeedsOutline(color: Color, background: Color = SkColors.Bg): Boolean`, true when the WCAG contrast ratio `(max(L1, L2) + 0.05) / (min(L1, L2) + 0.05)` of the two colours' `Color.luminance()` values is below `SWATCH_MIN_CONTRAST = 1.3f`. Use Compose's `androidx.compose.ui.graphics.luminance`; `androidx.core` is not a direct dependency of the app and must not be added.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `near white unicorn swatch needs an outline`() = assertTrue(swatchNeedsOutline(PersonalizationColors.Unicorn))
@Test fun `blue swatch does not`() = assertFalse(swatchNeedsOutline(PersonalizationColors.Blue))
@Test fun `options of one companion are clearly different`() {
    for (kind in CompanionKind.entries.filter { it != CompanionKind.HUMAN && it != CompanionKind.NONE }) {
        val colors = CompanionColor.entries.map { PersonalizationColors.companion(kind, it) }
        for (i in colors.indices) for (j in i + 1 until colors.size) {
            assertTrue("$kind $i $j", deltaE76(colors[i], colors[j]) >= 25.0)
        }
    }
}
```

`deltaE76(a, b)` is a test helper: convert both colours with `color.convert(ColorSpaces.CieLab)` (Compose `androidx.compose.ui.graphics.colorspace`), read L, a and b from `red`, `green` and `blue` of the converted colour, and return the Euclidean distance. `PersonalizationColors.companion(kind, color)` is the real function (`PersonalizationColors.kt` line 93). `CompanionKind` is `HUMAN, CAT, DOG, ROBOT, DRAGON, DINOSAUR, BUNNY, UNICORN, CAR, NONE`; the doll (`HUMAN`) has no colour options and `NONE` has no art, so the filter above is correct.

- [ ] **Step 2: Run** `./gradlew --no-daemon :app:testDebugUnitTest --tests "fi.clocklings.ui.art.*"`. Expected: the outline tests fail to compile; record which kind pairs fail the ΔE test.

- [ ] **Step 3: Implement the outline**

In `SwatchTile`, when `swatchNeedsOutline(color)` and not `selected`, draw `Box(Modifier.matchParentSize().border(SkDimens.BorderThin, SkColors.InkSoft.copy(alpha = 0.35f), SkShapes.Pill))`. Add the colour as a token `SkColors.SwatchOutline` instead of the inline `copy`.

- [ ] **Step 4: Run the outline tests.** Expected PASS.

- [ ] **Step 5: Fix the palette**

Change only the override colours of the pairs that failed (expected `DragonPurple`, `DinosaurGreen`), darkening them in small steps until the ΔE test passes. Show the owner the swatch rows and the recoloured dragon and dinosaur before finishing.

- [ ] **Step 6: Full suite and screenshots** of the unicorn, dragon and dinosaur swatch rows.

### Task E7: Locked items show stars missing

**SKIPPED. Do not implement.** The owner accepted D-6 "no change". Findings: 11. The steps below are kept only as a record of the rejected alternative.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/companion/WardrobeScreen.kt` (`LockedTile` pill)
- Modify: `app/src/main/res/values*/strings.xml` (plurals `doll_stars_missing`)

- [ ] **Step 1:** Add the plurals from D-6. Pill text: `pluralStringResource(R.plurals.doll_stars_missing, item.star - total, item.star - total)`. Keep the star icon and `PillKind.MUTED`. Keep the accessibility text `doll_item_locked` unchanged.
- [ ] **Step 2:** Update gallery test lookups that expect "3 / 7".
- [ ] **Step 3:** Unit suite PASS; screenshots in three languages at 320 dp and 200%.

### Phase F: Kellotyylit

### Task F1: Selected style is named, zoom thumbnails show whole numerals

Findings: 12. Screenshots 173509, 173517, 173527.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/appearance/AppearanceScreen.kt` (`ClockChoices`, lines 158 to 189)
- Modify: `app/src/main/java/fi/clocklings/ui/appearance/ZoomedClock.kt`
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt` (`ZoomWindowHeight`, `ZoomClock`, `ZoomClockShift`)

- [ ] **Step 1: Name the selected style under the big preview**

Under the large `ClockFace` add `Text(stringResource(clockThemeName(appearance.clock.theme)), style = SkType.SectionTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())`. The preview's `stateDescription` already carries the theme name; mark the new text with `clearAndSetSemantics {}` so TalkBack does not read it twice.

- [ ] **Step 2: Fix the zoom window**

Today `ZOOM_WINDOW_ASPECT = 1.2f` makes the window wide enough to show the right half of "10" (a lone "0") and the left part of "2". Tune `ZOOM_WINDOW_ASPECT`, `SkDimens.ZoomWindowHeight`, `SkDimens.ZoomClock` and `SkDimens.ZoomClockShift` so that the window shows the frame, the complete "11", "12" and "1", and the hand tips at `ZOOM_TIME` (11:05), and no part of "10" or "2". Keep the window at least `SkDimens.ThumbnailMin` high and wide. If 11:05 hides a hand tip behind the window edge after the change, pick another time that keeps both tips inside and update the KDoc of `ZOOM_TIME`.

- [ ] **Step 3: Unit suite PASS, then screenshots** of the custom clock options (frame, numerals, hands) at 320 dp and 411 dp and 200%. Acceptance as in step 2.

### Phase G: Home

### Task G1: Stage fills the space, hanger beside the companion

Findings: 13. Screenshot 173534.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/home/HomeScreen.kt` (`FillHeightLayout` lines 123 to 157, `CompanionStage` 318 to 360, `HomeCompanion` 445 to 461)
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt` (`HomeStageMaxHeight`)

- [ ] **Step 1: Centre the stage in its slot and let it grow**

`FillHeightLayout` measures the stage at `contentHeight` (capped by `HomeStageMaxHeight = 300.dp`) but reserves `stageHeight`, and places it at the top, so on tall phones the leftover sits between the ground and the menu. Place it at `y + (stageHeight - contentHeight) / 2`, and raise `HomeStageMaxHeight` to 360.dp.

- [ ] **Step 2: Move the hanger out of the companion's bounds**

Take the `RoundIconButton(SkIcons.Hanger, ...)` out of `HomeCompanion` and place it in `CompanionStage`, aligned `BottomStart`, with start padding equal to `SkDimens.GapS` + the companion box width + `SkDimens.GapS` and bottom padding `SkDimens.GapS`, so it sits on the ground just right of the companion. Keep the `NONE` companion rule (no hanger) and the content description `menu_nukke`.

- [ ] **Step 3: Acceptance on the emulator**

411 x 914 dp at 100%: the gap between the bottom of the ground and the top of the menu tiles is at most 32 dp. 360 x 640 dp: all four menu tiles fully visible without scrolling at 100% font. At 320 dp and 200%: the hanger overlaps neither the companion, the speech bubble nor the clock. Check every companion kind (the car and the doll are the widest and tallest).

- [ ] **Step 4: Unit suite PASS; run `NativeFeelUiTest` and `AppFlowTest` on the emulator.** Expected PASS.

### Phase H: Shell

### Task H1: Scrolled content does not leave half-faded text under the top bar

Findings: 23. Screenshots 173119, 173127, 173207, 173517.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/Layout.kt` (`ScreenScaffold`, top fade around lines 147 to 153)
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt`

- [ ] **Step 1: Taller fade with an opaque start**

New token `ScrollFadeTop = 28.dp`. Replace the top fade's `Brush.verticalGradient(listOf(section.color, transparent))` with stops `0f to section.color`, `0.45f to section.color`, `1f to section.color.copy(alpha = 0f)` and height `ScrollFadeTop`. Leave the bottom fade as it is.

- [ ] **Step 2: Screenshots** of the wardrobe and Kellotyylit scrolled so a section title sits just under the top bar. No glyph is visible cut in half at less than about 40% opacity; the first fully readable line starts below the fade.

- [ ] **Step 3: Unit suite PASS.**

---

## 5. Validation

Run in this order and report results exactly (pass/fail, counts, failure output):

1. `./gradlew --no-daemon :app:testDebugUnitTest`
2. `./gradlew --no-daemon :app:assembleDebug`
3. Instrumented tests on the dedicated emulator only: `AppFlowTest`, `ChallengeScreenTest`, `GalleryTest`, `PersonalizationGalleryTest`, `ScreenshotReviewGalleryTest`, `NativeFeelUiTest`, `ClockSurfaceStateTest`, `LocalizationContentTest`.
4. Stability baseline: if `:app:stabilityCheck` fails because of new composable parameters (`ProgressDots.outcomes`, `TimeReadout`, `CompanionView.cropItem`), run `:app:stabilityDump`, confirm no new unstable parameter, and report it.
5. Screenshots for the owner, in fi, en and sv where text changed, each at 320 dp, 200% font and both: every screen listed in the tasks' screenshot steps. Store them where the memory note `clocklings-validation-setup` says gallery images go and list the folder in the report.
6. Report device evidence separately from source inspection. Stop every emulator and process you started; never stop anything you did not start. Restore `animator_duration_scale` if B4 step 6 changed it.

## 6. Report to the owner

In Finnish. Per task id (A1 to H1): what was implemented (E7: skipped by decision D-6). Checks actually run with results. Screenshot folder. Any value tuned on the device (B2 share, E1 crop tops, E3 crops, F1 zoom values, G1 placement) with its final number. Open questions. Do not commit.

## 7. Checklist

- [ ] A1 numerals under the minute hand redrawn with matching halo
- [ ] A2 larger, outlined tick highlights
- [ ] B1 game dots show first try, hint, shown
- [ ] B2 game clock share tuned
- [ ] B3 Minuuttihaaste: inactive start clock, full-width buttons, `play_again` and `back_description`, unused strings deleted
- [ ] B4 confetti fades and clears on a new task
- [ ] C1 chapter headers removed, play node "Harjoittele", "Pelaa uudelleen" after stars
- [ ] D1 Pelaa yhdessä bottom-bar actions, shared `TimeReadout`
- [ ] E1 crops grow downwards, neck crops tuned
- [ ] E2 thumbnails show only their item
- [ ] E3 per-item EXTRA crops
- [ ] E4 one tap hint above the sections
- [ ] E5 item renames
- [ ] E6 swatch outline, palette ΔE
- [x] E7 skipped (D-6: no change)
- [ ] F1 selected style named, zoom window shows whole numerals
- [ ] G1 home stage centred, hanger beside the companion
- [ ] H1 taller opaque-start top fade
- [ ] `docs/implementation/STATUS.md` decisions updated with D-1 to D-6 (all recommendations accepted 2026-10-30)
- [ ] Section 5 validation completed and reported
