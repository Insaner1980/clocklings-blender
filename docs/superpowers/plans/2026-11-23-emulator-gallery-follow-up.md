# Clocklings emulator gallery follow-up (23 November 2026) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a planning deliverable. It is not permission to commit, push, open a pull request or publish.

**Goal:** Fix every defect the 23 November emulator gallery review found, including the small ones, without reopening earlier owner decisions.

**Architecture:** Presentation changes in existing Compose screens, one new lesson caption string in seven languages, one shared system-bar helper, and fixes to the device capture harness. No new dependencies, no migration of stored progress, no change to clock mathematics.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric JVM tests, Compose instrumentation on the dedicated emulator, Android string resources in seven languages.

**Spec:** The review given to the owner on 23 November 2026 (in Finnish, in the session that wrote this plan). After the review, the owner asked for every finding to be fixed, including the smallest. Evidence: `reports/clocklings-emulaattori-kuvagalleria-2026-11-23/` (gitignored, local checkout only), called **GALLERY** below. It has five configurations: `411dp-100`, `411dp-200`, `320dp-100`, `320dp-200` and `411dp-100-dark`. `GALLERY/411dp-100/x.png` means `reports/clocklings-emulaattori-kuvagalleria-2026-11-23/411dp-100/screens/screens--x.png`.

**Starting revision:** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` on `codex/visual-follow-up-2026-11-17`. The working tree holds the uncommitted work of the 21 November simplification plan, the 22 November child clarity plan (C1–C9) and the 23 November owner decisions (D5–D10, T19). It also holds the untracked `values-night-v36/themes.xml` and `WindowThemeTest.kt`. Keep all of it.

## Global Constraints

- Read `AGENTS.md`, `docs/implementation/STATUS.md`, the [child clarity plan](2026-11-22-child-clarity-review.md) and this plan first. All repository rules apply.
- Work on a `codex/` branch. Do not commit, push or open a pull request unless the owner asks separately. The commit steps below apply only after that request.
- Follow the repository's `sonar analyze secrets` instruction before reading source.
- Seven languages: `values`, `values-fi`, `values-sv`, `values-de`, `values-fr`, `values-es`, `values-pt-rBR`. Every new string needs all seven.
- Identifiers, filenames and code comments in English. UI copy in resources.
- Use theme tokens (`SkColors`, `SkDimens`, `SkType`, `SkShapes`, `SkMotion`). Add a token instead of an inline value.
- No middle-dot or pipe separators in UI, no coloured edge stripes.
- Never hide, collapse or remove answer options or the child's own picks (D2 rejected on 17 November).
- Within one task the practice clock may shrink but never grow (round 2, R2). Do not change `FitClockMin`, `FeedbackClockMin` or `QuizClockMin`.
- Keep D6: the last paragraph of a lesson page appears only when the demo finishes, and at once with reduced motion or replay. E1 changes only how the space for it is held.
- Keep C9: the disabled lesson "Valmis" stays visible and readable before the answer.
- Do not change companion artwork, Blender rigs or narration recordings (`res/raw*`).
- Run Gradle only from this repository with `--no-daemon`. Use only the dedicated `ClocklingsReview20261030` emulator.
- The owner runs `lc` and `sc`. Report in Finnish at the end.

## Review Focus

1. **Lesson pages with reduced motion and with replay** (E1): both paragraphs and the final caption are visible at once. The reserved space must not leave an empty gap below the text in that state.
2. **Practice hint after two wrong picks** (E9): the second hint must replace the first in the question line, with no growth of the dial between them.
3. **TalkBack on a lesson page during the demo** (E1): the reserved paragraph and the reserved captions must not be read before they become visible.
4. **Tapping a locked companion** (E2) must never change the saved companion, in the free version or in the full version with fewer than three stars. Swiping back to a free companion must still save it as before.
5. **Locked thumbnails in the short-window horizontal row** (E2), not only in the `FlowRow` layout.
6. **The level result with a companion that is wider than tall** (E3): the size cap must apply only to tall companions. The cat and the car must keep their current size.

---

## Owner decisions (approved 23 November 2026)

The owner approved option B for both D11 and D12 on 23 November 2026. Implement them as tasks E9 and E10 below. Options A and C are kept here only as a record.

### D11. Where the practice hint appears after a wrong answer

**Problem.** After a wrong READ answer, the hint bubble is added below the three options. Because the clock fits the remaining height (C1), the dial shrinks from 379.4 dp to 236.2 dp at the hint and to 220.2 dp at the reveal (STATUS, 22 November). The hint text "Katso, mihin kumpikin viisari osoittaa" is shown with the smallest dial. Earlier constraints rule out hiding options (D2) and growing the clock (R2).

**Options.**
- **A. Keep the current layout.** The 220 dp dial is still more than half the screen width at 411 dp.
- **B. Approved.** During READ feedback, show the hint in place of the question line "Mitä kello on?" at the top of the options panel, with the same companion avatar, instead of a separate bubble below the options. The panel then grows only by the extra lines of the hint, so the dial shrinks much less. All three options stay visible. The behaviour stays shrink-only and C1's tests still apply. SET tasks and the Minute Challenge are not changed.

Implementation is task E9.

### D12. What gives way at 320 dp with 200 % font

**Problem.** At 320 dp and 200 % font:
- The lesson quiz options start below the bottom fade before the answer (GALLERY `320dp-200/lesson-05-vartti-quiz-p0`).
- The timed Minute Challenge needs scrolling to reach its third option (`320dp-200/challenge-running-p0`).
- At 411 dp and 200 %, the third lesson quiz option is partly under the fade (`411dp-200/lesson-05-vartti-quiz-p0`).

The clock is already at `FitClockMin`. The biggest remaining cost is the separate title row that `TopBar` (`ui/components/ScreenHeader.kt:57`) uses whenever `maxWidth / fontScale < GridBreakpoint`, which is always at 200 %.

**Options.**
- **A. Keep scrolling** as today. The bottom fade shows that more content follows.
- **B. Approved.** Inside `ClockTaskScaffold` screens only (lesson, practice, challenge), when the layout is stacked and `fontScale > SkDimens.MAX_ROW_FONT_SCALE`, use a header variant:
  - The title shares the row with the back button and wraps under it if needed.
  - It uses a new `SkType.TaskTitleLarge` token, about the size of `SectionTitle`.
  - The trailing item (for example the challenge "7 oikein" pill) moves to the progress row.
  - Other screens keep the current `TopBar`.

  This gives about 40–80 dp back without making the clock smaller.
- **C.** B plus a smaller clock minimum only for stacked layouts at 320 dp width with font scale above 1.3 (new token, for example 128 dp). The clock is then harder to read for exact minutes.

Implementation is task E10. C was not approved: do not add a smaller clock minimum.

---

## Considered and not planned

- **pt-BR "da noite" at 18.15.** The Brazilian Senate style manual defines *noite* as 18–24 h. All seven languages share the 18.00 boundary (`EVENING_FROM = 18`). No change.
- **Finnish "kahtakymmentä yli" next to "seitsemän minuuttia yli".** This is the inherited original-app rule in `FinnishTime.minutesPhrase`. Both forms are in normal use, and no Kielitoimisto guidance makes either wrong. No change.
- **The dark gallery set equals the light set.** This is the intended result of the untracked `values-night-v36/themes.xml` and `WindowThemeTest`. E8 labels it instead of removing it.

---

### Task E1: Lesson demo keeps the clock still when the last paragraph appears

**Problem.** D6 adds the last paragraph and the companion caption only when the demo finishes. No space is reserved for them, so the fitted clock jumps:
- At 100 %, it moves about 80 dp up.
- At 200 %, it halves in size.

Compare `411dp-100/lesson-02-kierros-page-1-demo-start-p0` with `lesson-02-kierros-page-1-p0`, and the same pair at `411dp-200`. The jump happens just as the child watches the hands stop. `LessonScreen` renders `state.visibleParagraphs` and `state.caption` directly (`ui/lesson/LessonScreens.kt:135–177`), and `ClockTaskScaffold` fits the clock to the content height.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt`
- Possibly modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonViewModel.kt` (only to expose the page's full paragraph list and its `DemoStep.Say` texts, if `LessonScreen` cannot read them from `state.lesson.pages[state.page]`)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

- [ ] **Step 1: Write the failing test** `lessonClockStaysStillWhenDemoFinishes`.
  - Render `LessonScreen` for lesson `kierros` page 1 in Finnish twice. First with `demoFinished = false` and `caption = null`, then with `demoFinished = true` and `caption` set to the page's last `DemoStep.Say` text.
  - Assert that the clock node's bounds (top and size) are equal within 0.5 dp.
  - Run it at the default viewport with 100 % font, and at 320 dp with 200 % font. Use `TestViewports.kt` and `TestLocales.kt`, and find the clock's test tag with `git grep -n "testTag" app/src/main/java/fi/clocklings/ui/clock`.
- [ ] **Step 2: Run it and confirm both cases fail.**

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.ReviewRegressionTest'
```

- [ ] **Step 3: Reserve the paragraph.** Render every paragraph of the page. A paragraph that `visibleParagraphs` does not contain yet gets `alpha 0` and `clearAndSetSemantics { }`. When it becomes visible, fade it in with `animateFloatAsState` using an `SkMotion` duration, or snap with reduced motion (`rememberReduceMotion()`).
- [ ] **Step 4: Reserve the caption.** Collect the texts of the page's `DemoStep.Say` steps. Render them in one `Box` as invisible `CompanionTip`s with cleared semantics, so the box always has the height of the tallest one. Draw the current caption on top. If the page has no `Say` step, reserve nothing.
- [ ] **Step 5: Keep speech unchanged.** `LessonPlayback` must still speak only the visible paragraphs and the current caption.
- [ ] **Step 6: Add** `lessonReservedTextIsSilent`. During the demo, assert that no semantics node contains the last paragraph's text and that it appears after `demoFinished = true`.
- [ ] **Step 7: Run the new tests, `LessonViewModelTest` and the existing D6 tests; all pass.** Find the D6 tests with `git grep -n "demoFinished" app/src/test`.
- [ ] **Step 8: Commit** (only after the owner asks): message "Varaa oppitunnin loppukappaleelle tila, jotta kello ei hyppää".

### Task E2: Tapping a locked companion explains the lock

**Problem.** In the free version, the dimmed companions do nothing when tapped: `CompanionThumbnails.kt:69` passes `enabled = access.companion(option, progress.totalStars)` to `PressSurface`. The requirement text "Edellyttää täysversiota ja 3 ansaittua tähteä." and the Vanhemmille button appear only after swiping the big pager to that companion (`AppearanceScreen.kt:169–171`). The same happens in the full version with fewer than three stars. The gallery has no capture of a locked preview.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/appearance/CompanionThumbnails.kt`
- Test: the appearance UI test that already uses `choose_companion_` tags (`ScreenshotReviewUiTest.kt`, `ReviewRegressionTest.kt` or `LocalAppFlowTest.kt`; pick the one that renders `AppearanceScreen` with an `AccessPolicy`)

- [ ] **Step 1: Write the failing tests.**
  - `lockedCompanionTapShowsRequirement`: use free access with the saved companion `CAT`. Click `choose_companion_DOG`, then assert:
    - the text `companion_access_requirement` and the `parent_title` button are displayed;
    - the stored personalization still has `CAT`.
  - `lockedCompanionTapInFullVersionWithoutStars`: use full access with 0 stars, and assert the requirement text is shown and no `parent_title` button is shown.
- [ ] **Step 2: Run them; they fail** because the thumbnail is disabled.
- [ ] **Step 3: Keep every thumbnail clickable.** Remove `enabled` from the thumbnail's `PressSurface`, so a tap always scrolls the pager to that companion. `CompanionPagerState` already refuses to save a locked companion; do not change it.
- [ ] **Step 4: Keep the locked look.**
  - Draw the art of a locked thumbnail at `SkColors.DISABLED_CONTROL_ALPHA`.
  - Add the same `SkIcons.Lock` the pager uses (`AppearanceScreen.kt:412`) in a corner of the thumbnail. Use a size token, not the pager's `SkDimens.Star` if that is too big for `ThumbnailMin`.
  - Add `stateDescription = stringResource(R.string.full_version_label)` for locks that need the full version. Use the requirement string for locks that need stars in the full version.
- [ ] **Step 5: Run the tests and the existing appearance and pager tests; all pass.** Find them with `git grep -ln "rememberCompanionPager\|choose_companion_" app/src/test`.
- [ ] **Step 6: Commit** (only after the owner asks): message "Näytä lukitun kaverin ehto napautuksesta".

### Task E3: Level result fits the reward card with a tall companion

**Problem.** With the doll and two or three rewards, the "Valitse asu" button sits under the bottom fade at 411 dp and 100 % (`411dp-100/result-3-stars-p0`, `result-2-stars-p0`). The cat versions fit. The doll is drawn at `SkDimens.DollResult` width (128 dp) with its tall aspect ratio (`ResultScreen.kt:123–128`). Round 2 already recorded this as open.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt`, `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

**Interfaces:**
- Produces: token `SkDimens.ResultCompanionMaxHeight`.

- [ ] **Step 1: Write the failing test** `tallCompanionResultFitsRewardAction`.
  - Render a 3-star level result with the doll (`CompanionKind.HUMAN`) and three new rewards at the default viewport and 100 % font.
  - Assert that the bottom of the `dress_doll` button is above the top of the bottom actions minus `SkDimens.ScrollFade`.
  - Add a cat case that asserts its rendered companion size equals today's size.
- [ ] **Step 2: Run it; the doll case fails.**
- [ ] **Step 3: Cap the height.** Limit the result companion to `ResultCompanionMaxHeight` and keep its aspect ratio, so tall companions get narrower and wide ones are unchanged. Set the token to the cat's current rendered height at `DollResult` width, or lower if the test needs it, and record the value.
- [ ] **Step 4: Run the test, `VisibleUnlocksTest` and the F1 reward-card tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task E4: Minute Challenge result

**Problem 1.** At 320 dp and 200 %, the score number is half hidden under the action buttons (`320dp-200/challenge-result-p0`). The companion slot comes before `ChallengeScore` (`ChallengeResultScreen.kt:97–112`).

**Problem 2.** With no companion, the score panel is centred, and the upper third of the screen is empty (`411dp-100/challenge-no-companion-p0`). Elsewhere, "no companion" is shown as a clock: the picker thumbnail (`CompanionThumbnails.kt:126`) and Home's `ClockOnlyStage`.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/challenge/ChallengeResultScreen.kt`
- Possibly move: the decorative preview time constant `PREVIEW_TIME` from `ui/appearance/` to a shared place, if reused
- Test: the existing challenge result test (find with `git grep -ln "ChallengeResultScreen" app/src/test`)

- [ ] **Step 1: Write the failing tests.**
  - `challengeScoreVisibleInShortViewport`: at 320 dp and 200 %, assert that the score number node is fully above the bottom actions.
  - `challengeResultWithoutCompanionShowsClock`: with `CompanionKind.NONE`, assert that a decorative clock exists above the score panel and has no semantics.
- [ ] **Step 2: Run them; they fail.**
- [ ] **Step 3: Score first when space is short.** When `viewport < SkDimens.CompactTaskHeight`, place `ChallengeScore` before the companion slot. Keep the current order otherwise.
- [ ] **Step 4: Clock without a companion.** With `CompanionKind.NONE`, put a decorative `ClockFace(..., mini = true)` in the companion slot at `SkDimens.ChallengeCompanionHeight`, with `clearAndSetSemantics { }`. Use the same decorative time as the picker. Remove the `CenterVertically` branch, because the layout is then the same as with a companion.
- [ ] **Step 5: Run the tests and the existing challenge tests; all pass.**
- [ ] **Step 6: Commit** (only after the owner asks).

### Task E5: Name field cursor does not cut the placeholder

**Problem.** When the empty name field gets focus, the cursor blinks in the middle of the centred placeholder "Valitse nimi" (`411dp-100/wardrobe-name-entry`). `NameField.kt:115–122` draws the placeholder whenever the field is empty, under a centred input.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/companion/NameField.kt`
- Test: the existing name field test (find with `git grep -ln "doll_name_placeholder" app/src/test`)

- [ ] **Step 1: Write the failing test** `namePlaceholderHidesWhileFocused`.
  - The placeholder text is shown while the field is unfocused and empty, and is not shown after focus.
  - The field's accessibility label is still present in both states.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Track focus.** Use the field's `MutableInteractionSource` with `collectIsFocusedAsState()`, and draw the placeholder only when the field is empty and not focused.
- [ ] **Step 4: Run the test and the wardrobe name tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task E6: Lesson "Viiden minuutin välein" explains 60 and 0

**Problem.** On page 1 the demo reaches number 12 and says "Numero 12 tarkoittaa 60 minuuttia" with a "60" bubble (`LessonCatalog.kt:390–398`). Every other clock with minute numbers shows "0" at the top, so the two numbers disagree without explanation.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonCatalog.kt`
- Modify: `app/src/main/res/values*/strings.xml` (seven files)
- Test: `app/src/test/java/fi/clocklings/ui/lesson/LessonCatalogTest.kt` or the existing lesson content test (find with `git grep -ln "say_number_minutes\|LessonCatalog" app/src/test`)

**Interfaces:**
- Produces: string `say_number_full_hour`, with no arguments.

- [ ] **Step 1: Write the failing test** `fiveMinuteLessonExplainsFullHour`: the last `DemoStep.Say` of page 1 of lesson `viisi` uses `R.string.say_number_full_hour`, and the steps for 1 to 11 still use `say_number_minutes`.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Add the string.**
  - Finnish: "Numero 12 tarkoittaa 60 minuuttia eli koko tuntia. Sitten minuutit alkavat taas nollasta."
  - English: "Number 12 marks 60 minutes, a whole hour. Then the minutes start again from 0."
  - Other languages: the same meaning, worded like that language's `say_number_minutes`.

  Use it for `n == 12` in the demo loop and keep the "60" bubble. Captions are not narrated (`say_number_minutes` has no `Sound` entry); confirm with `git grep -n "say_number" app/src/main/java/fi/clocklings/audio`.
- [ ] **Step 4: Run the test, `tools/check-localization.ps1` and `ExpandedLocaleContentTest`; all pass.** E1 reserves the caption space from the longest `Say` text, so check that this page's clock does not move after E1.
- [ ] **Step 5: Commit** (only after the owner asks).

### Task E7: Capture host matches the app's system bars

**Problem.** In almost every gallery shot the status-bar icons are white on the light app background. The capture host is a plain `ComponentActivity` (`ScreenshotReview6GalleryTest.kt:1422–1424`). `MainActivity` calls `enableEdgeToEdge` with `SystemBarStyle.light(...)`. Only `startup--03-live-home` shows the real dark icons.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/MainActivity.kt`
- Create: `app/src/main/java/fi/clocklings/ui/SystemBars.kt` (or a top-level function in an existing `ui` file)
- Modify: `app/src/androidTest/java/fi/clocklings/ScreenshotReview6GalleryTest.kt`

- [ ] **Step 1: Extract** `fun ComponentActivity.applyClocklingsSystemBars()` with the exact `enableEdgeToEdge(...)` call from `MainActivity`. Call it from `MainActivity` and from `launchHost()` in `scenario!!.onActivity { ... }` before `setContent`. This keeps one source of truth.
- [ ] **Step 2: Run `:app:testDebugUnitTest` (`WindowThemeTest` included) and `:app:assembleDebugAndroidTest`; both pass.**
- [ ] **Step 3: Commit** (only after the owner asks).

### Task E8: Gallery fixtures and the untracked harness

**Problems.**
- `practice-24h` is titled "Tasatunnit" because the fixture reuses level 0. Its options 14.30, 15.30 and 16.30 belong to "Puoli tuntia" (`VisualFollowUpRound2CaptureTest.kt:528–538`, `gameState` uses `level = 0`).
- `companion-picker-locked` shows a locked robot as the selected companion with a disabled "Valitse asu". The real app never selects a locked companion (`AccessPolicy.displayedCompanion`).
- The 23 November gallery contains captures (`companion-picker-*`, `companion-home-*`, `*-demo-start`, the `411dp-100-dark` set, `parent-store-*` and others) that no tracked test produces. `git grep -n "companion-picker"` finds nothing. The next gallery cannot be reproduced.
- The dark set looks like a duplicate without an explanation.

**Files:**
- Modify: `app/src/androidTest/java/fi/clocklings/VisualFollowUpRound2CaptureTest.kt`
- Modify or create: the gallery index generator, if one is tracked; otherwise document the index format in the report

- [ ] **Step 1: Fix `practice-24h`.** Set `level = 1` in the `dayTask` copy so the title matches the half-hour options.
- [ ] **Step 2: Find the harness that produced the 23 November gallery** (Codex's own capture code from that run). Move it into tracked androidTest code, inside `VisualFollowUpRound2CaptureTest` under `cell == "gallery"` or as a sibling class that reuses `ScreenshotReview6GalleryTest.launchHost()`. Do not duplicate helpers that already exist there. If the code cannot be found, write the missing captures again from the GALLERY file names.
- [ ] **Step 3: Replace the locked picker capture with real states.**
  - `companion-picker-locked-free`: free access, saved `CAT`, pager scrolled to `ROBOT`, showing the requirement text and the Vanhemmille button.
  - `companion-picker-locked-stars`: full access, 0 stars, pager on `ROBOT`.
  - `companion-picker-locked-tap`: free access, after clicking `choose_companion_DOG` (E2).
- [ ] **Step 4: Label the dark set.** In the gallery index, title the `411dp-100-dark` section "Järjestelmän tumma tila: sovellus pysyy vaaleana (odotettu)". Keep the set, because it guards `values-night-v36` and Force Dark.
- [ ] **Step 5: Run `:app:assembleDebugAndroidTest`; it passes.** The device run is in Validation.
- [ ] **Step 6: Commit** (only after the owner asks).

### Task E9: Practice hint replaces the question line (D11 B)

**Problem.** See D11. The hint bubble below the options makes the fitted READ dial shrink from 379.4 dp to 236.2 dp.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt` (`GameFeedbackMessage` around line 210, the feedback block at lines 242–300, the READ question line near `ReadOptions`)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

- [ ] **Step 1: Write the failing test** `readHintReplacesQuestionLine`.
  - Render a level 1 READ task in Finnish, pick a wrong option, and assert:
    - the hint text is displayed;
    - the `question_read` text ("Mitä kello on?") is not displayed;
    - the hint's top is above the top of the first option.
  - Before any answer, the question text is displayed as today.
- [ ] **Step 2: Extend** `readClockNeverGrowsWithinATask`. At the default viewport and 100 % font, the hint-state dial must be at least 300 dp, and the sizes must still never increase from initial to hint, reveal and praise.
- [ ] **Step 3: Run both; they fail.**
- [ ] **Step 4: Move the message.** For READ tasks with `HINT`, `RevealRead` or `Praise` feedback, draw the `CompanionTip` (same avatar, same text, same `celebrate`) in the question line's place at the top of the options panel. Remove the separate bubble below the options for those states.
  - Keep `SaveFailed` and all SET-task feedback where they are now.
  - Do not change the Minute Challenge.
  - Keep the confetti anchor and the speech of the hint unchanged.
- [ ] **Step 5: Run the tests together with C1's `readTaskShowsEveryOptionBeforeAnswering`, `GameViewModelTest` and `ClockTaskScaffoldTest`; all pass.** If 300 dp cannot be reached without breaking C1, use the measured value and report it.
- [ ] **Step 6: Commit** (only after the owner asks): message "Näytä harjoituksen vihje kysymyksen paikalla".

### Task E10: Compact task header at large font (D12 B)

**Problem.** See D12. At 200 % font, `TopBar` always puts the title on its own row (`ScreenHeader.kt:57`). At 320 dp this pushes lesson quiz options and the Minute Challenge's third option below the fold.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/ScreenHeader.kt`, `app/src/main/java/fi/clocklings/ui/components/ClockTaskScaffold.kt`, `app/src/main/java/fi/clocklings/ui/theme/SkType.kt`
- Modify: the `topBar` callers in `ui/lesson/LessonScreens.kt`, `ui/game/GameScreen.kt` and the challenge running screen (find with `git grep -n "TopBar(" app/src/main/java/fi/clocklings/ui`)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

**Interfaces:**
- Produces: token `SkType.TaskTitleLarge`. Either a `TaskTopBar` composable or a parameter on `TopBar`; choose the smaller change. Do not duplicate the back-button and heading semantics.

- [ ] **Step 1: Write the failing test** `largeFontTaskShowsFirstOption` at 320 dp and 200 % font, in Finnish and German (the longest titles):
  - In the lesson quiz before the answer, the first option is fully above the top of the bottom actions minus `SkDimens.ScrollFade`.
  - In the Minute Challenge, the top of the third option is inside the viewport.
  - In each case, the title node is still displayed and is still a heading.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Add the header variant.** Use it only when the scaffold is stacked and `fontScale > SkDimens.MAX_ROW_FONT_SCALE`:
  - The title sits in the back-button row, with `TaskTitleLarge` (about the size of `SectionTitle`), and wraps by whole words under the button if it does not fit.
  - A trailing item (the challenge "N oikein" pill, the practice help toggle) moves into the status/progress row.
  - All other screens and font scales keep the current `TopBar`.
  - Do not change any clock minimum.
- [ ] **Step 4: Run the test, `ClockTaskScaffoldTest`, the TopBar tests and the 200 % layout tests; all pass.** Find the layout tests with `git grep -ln "fontScale = 2" app/src/test`. Record which criteria the header alone meets. If the third challenge option is still partly below the viewport, report the remaining dp. Do not add option C.
- [ ] **Step 5: Commit** (only after the owner asks): message "Tiivistä tehtävänäkymän otsikko suurella fontilla".

---

## Order of work

E7 and E8 first, so that every later fix can be captured with correct system bars and fixtures. E8's `companion-picker-locked-tap` capture works only after E2, so add that capture last. E1 before E6, because E6's caption length feeds E1's reserved space. E2, E3, E4 and E5 are independent. Do E9 after E1–E8 and before E10, because both change the clock fit in task screens and E10's measurements must include E9's layout.

## Validation and acceptance

- [ ] `.\gradlew.bat --no-daemon :app:testDebugUnitTest` passes with zero failures. Record the test count against the 578 in `STATUS.md`.
- [ ] `.\gradlew.bat --no-daemon :app:assembleDebug` and `:app:assembleDebugAndroidTest` pass.
- [ ] `tools/check-localization.ps1` reports seven-language parity.
- [ ] On the dedicated `ClocklingsReview20261030` emulator, recapture the full gallery in all five configurations (`411dp-100`, `411dp-200`, `320dp-100`, `320dp-200`, `411dp-100-dark`) from tracked code only, and check:
  - E1: every `lesson-*-demo-start` capture has the clock in the same place and at the same size as the matching final capture. Measure `lesson-02-kierros-page-1` and `lesson-06-viisi-page-1` at 100 % and 200 % and record the difference in dp.
  - E2: the three `companion-picker-locked-*` captures show the lock icon on the dimmed thumbnails and the requirement text.
  - E3: `result-2-stars` and `result-3-stars` with the doll at `411dp-100` show "Valitse asu" fully above the fade.
  - E4: `challenge-result` at `320dp-200` shows the whole score number, and `challenge-no-companion` shows the decorative clock.
  - E5: `wardrobe-name-entry` shows the cursor without placeholder text.
  - E6: `lesson-06-viisi-page-1` shows the new caption.
  - E7: dark status-bar icons in every capture.
  - E8: `practice-24h` is titled "Puoli tuntia".
  - E9: `practice-read-hint` and `practice-read-reveal` show the hint at the top of the options panel. Record the dial size in dp against 379.4 dp before answering.
  - E10: at `320dp-200` and `411dp-200`, `lesson-*-quiz` and `challenge-running` show the title in the back-button row. Record how far the first quiz option and the third challenge option are from the fade.
- [ ] Check reduced motion separately: a lesson page shows both paragraphs and the caption with no empty reserved gap.
- [ ] Restore emulator settings and stop every process started for the task.
- [ ] Update `docs/implementation/STATUS.md` with a dated entry. The owner runs `lc` and `sc`.

## Final report (Finnish)

- Implemented tasks E1–E10 and what each changed.
- Checks run and their results. Report what was seen on the device separately from source inspection.
- The earlier open decisions T8 and T30.
- Measurements that were not obtained or that fell below the acceptance threshold.

Source for the pt-BR decision: [Senado Federal, Manual de Comunicação, Hora](https://www12.senado.leg.br/manualdecomunicacao/estilos/hora).
