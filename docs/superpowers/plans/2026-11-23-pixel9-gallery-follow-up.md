# Clocklings Pixel 9 gallery follow-up (23 November 2026) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a planning deliverable. It is not permission to commit, push, open a pull request or publish.

**Goal:** Fix every defect the review of the 23 November Pixel 9 gallery found after E1–E10, including the small ones. This includes the 200 % regressions introduced by E9 and the remaining gaps in E3 and E10.

**Architecture:** Presentation changes in existing Compose screens, one new string and one string change in seven languages, removal of one string, a starting-window theme fix, and fixes to the tracked capture harness. No new dependencies, no migration of stored progress, no change to clock mathematics.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric JVM tests, Compose instrumentation on the owner's Pixel 9 (Android 17, three-button navigation), Android string resources in seven languages.

**Spec:** The review given to the owner on 23 November 2026, in Finnish, in the session that wrote this plan. The owner asked for every finding to be fixed. Evidence:
- **GALLERY** is `reports/clocklings-pixel9-gallery-2026-11-23/` (gitignored, local checkout only). `GALLERY/411dp-100/x` means `.../411dp-100/screens--x.png`.
- `GALLERY/<config>/measure.txt` holds Codex's device measurements.
- `GALLERY/REPORT.md` is Codex's own report.

**Starting revision:** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` on `codex/visual-follow-up-2026-11-17`. The working tree holds the uncommitted work of the earlier plans up to and including [E1–E10](2026-11-23-emulator-gallery-follow-up.md). Keep all of it.

## Global Constraints

- Read `AGENTS.md`, `docs/implementation/STATUS.md`, the [E1–E10 plan](2026-11-23-emulator-gallery-follow-up.md) and this plan first. All repository rules apply.
- Work on a `codex/` branch. Do not commit, push or open a pull request unless the owner asks separately. The commit steps below apply only after that request.
- Follow the repository's `sonar analyze secrets` instruction before reading source.
- Seven languages: `values`, `values-fi`, `values-sv`, `values-de`, `values-fr`, `values-es`, `values-pt-rBR`. Every new string, and every string whose meaning changes, needs all seven. Delete a string in all seven when it becomes unused.
- Identifiers, filenames and code comments in English. UI copy in resources.
- Use theme tokens (`SkColors`, `SkDimens`, `SkType`, `SkShapes`, `SkMotion`). Add a token instead of an inline value.
- No middle-dot or pipe separators in UI, no coloured edge stripes.
- Never hide, collapse or remove answer options or the child's own picks (D2 rejected).
- Within one task the practice clock may shrink but never grow (R2). Do not change `FitClockMin`, `FeedbackClockMin` or `QuizClockMin`, and do not add a smaller clock minimum (D12 option C was not approved).
- Keep D6 (last lesson paragraph after the demo), E1 (no clock movement during a demo) and C9 (readable disabled "Valmis").
- Do not change companion artwork, Blender rigs or narration recordings (`res/raw*`).
- Run Gradle only from this repository with `--no-daemon`. Device captures run on the owner's Pixel 9, as for the 23 November gallery. Do not change the phone's notification settings for captures; a notification banner in a shot is not a defect.
- The owner runs `lc` and `sc`. Report in Finnish at the end.

## Review Focus

1. **P1 rule boundaries.**
   - At 411 dp and 100 % the hint must stay in the question line exactly as E9 made it (dial 302.5 dp at the hint).
   - At 320 dp, or with font scale above 1.3, the old placement below the options returns.
   - The switch must not make the dial grow within one task.
2. **P1 reveal scrolling** must not fight the user. Bring the correct option into view once per feedback change, not on every recomposition, and not while the child is dragging the scroll.
3. **P3 with reduced motion:** the pager scroll and the bring-into-view jump at once.
4. **P4 for every companion kind, including `NONE`,** in the level result. With the Minute Challenge result (E4), the decorative clock must not change size.
5. **P7 `WordSafeText` change** affects every caller. Check that labels that fitted on one line before still do (grep callers and run their tests).

---

## Considered and not planned

- **Notification banners in captures** (`GALLERY/320dp-200/wardrobe-free-p0`): the owner's own device, not a defect.
- **`measure.txt` card clearance 3.4 dp versus REPORT.md 19.4 dp for the doll:** both are right. One is the card's bottom and the other the button's bottom.

---

### Task P1: Practice hint placement depends on available space

**Problem.** E9 put the hint, the reveal and the praise in the question line. Where the dial is already at `FitClockMin`, this hides options and gains nothing:
- `measure.txt` R2 READ: 160 → 160 dp at 320 dp / 200 %, 208.8 → 160 dp at 411 dp / 200 %, 181.3 → 160 dp at 320 dp / 100 %.
- At 411 dp / 200 % and at 320 dp / 100 %, the correct green option is below the fold after the reveal (`GALLERY/411dp-200/practice-read-reveal-p0`, `GALLERY/320dp-100/practice-read-reveal-p0`).
- At 320 dp / 200 % no option is visible after the hint (`GALLERY/320dp-200/practice-read-hint-p0`).

E9 helps only at 411 dp / 100 % (373.0 → 302.5 dp).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt` (`GameAnswerPanel` around line 201, `GameOptions` around line 300)
- Possibly modify: `app/src/main/java/fi/clocklings/ui/components/ClockTaskScaffold.kt` (expose whether the layout is the `tall` branch, line 102)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

**Interfaces:**
- Produces: a way for the content lambda to know the scaffold uses the tall layout. Pass it the same way `compact` is passed to `content`, or with a small `CompositionLocal` defined next to `ClockTaskScaffold`. Choose the smaller change.

- [ ] **Step 1: Write the failing tests.**
  - `hintStaysInQuestionLineOnTallLayout`: at the default viewport and 100 %, the hint replaces the question line. This keeps the existing E9 test.
  - `hintGoesBelowOptionsWhenClockIsAtMinimum`: at 320 dp with 100 %, and at 411 dp with 200 %, the question text stays and the hint is below the last option.
  - `revealBringsCorrectOptionIntoView`: in all four width and font combinations, after `GameFeedback.RevealRead` and after `Praise`, the correct option node is fully inside the viewport above the bottom fade.
- [ ] **Step 2: Run them; the second and third fail.**
- [ ] **Step 3: Use the question-line placement only in the tall layout.** Use the old placement (below the options inside `GameAnswerPanel`) otherwise. SET tasks and `SaveFailed` are unchanged.
- [ ] **Step 4: Bring the correct option into view** on `RevealRead` and `Praise`. Reuse `bringFeedbackIntoView` from `ui/components/FocusScroll.kt` (keyed by the feedback) rather than adding a second mechanism.
- [ ] **Step 5: Run the tests, `readClockNeverGrowsWithinATask`, C1's `readTaskShowsEveryOptionBeforeAnswering` and `GameViewModelTest`; all pass.**
- [ ] **Step 6: Commit** (only after the owner asks): message "Näytä vihje vaihtoehtojen alla, kun kellolla ei ole tilaa".

### Task P2: Lesson "Viiden minuutin välein" page 1 keeps a readable dial

**Problem.** E1 reserves space for the tallest caption of a page. E6 made the `n == 12` caption four lines long, so lesson 6 page 1 now has the smallest dial of all lesson pages at 411 dp / 100 %. The minute bubbles 5–60 the child counts are tiny (`GALLERY/411dp-100/lesson-06-viisi-page-1-p0`). Compare it with page 2 of the same lesson.

**Fix.** Keep the explanation, but move it into the page's final paragraph, which is reserved anyway, and give the last caption the same length as the others.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonCatalog.kt:398`
- Modify: `app/src/main/res/values*/strings.xml` (seven files: `l_viisi_p1b` changed, `say_number_full_hour` deleted)
- Test: the E6 test `fiveMinuteLessonExplainsFullHour` and the E1 tests in `ReviewRegressionTest`

- [ ] **Step 1: Change the test.**
  - Rename `fiveMinuteLessonExplainsFullHour` to `fiveMinuteLessonExplainsFullHourInParagraph`.
  - All twelve `Say` steps of page 1 use `say_number_minutes`.
  - `l_viisi_p1b` contains the full-hour sentence in every language.
  - Add `fiveMinutePageOneDialNotSmallerThanPageTwo`: at the default viewport and 100 %, the page 1 dial box is at least the page 2 dial box minus one `SkType.LessonText` line height.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Change the content.**
  - Use `say_number_minutes` for `n == 12` again.
  - Finnish `l_viisi_p1b`: "Numero 1 tarkoittaa 5 minuuttia, numero 2 tarkoittaa 10 minuuttia ja niin edelleen. Numerossa 12 tunti on täynnä, ja minuutit alkavat taas nollasta."
  - English: "Number 1 marks 5 minutes, number 2 marks 10, and so on. At number 12 the hour is full, and the minutes start again from 0."
  - Other languages: the same added sentence in their own words.
  - Delete `say_number_full_hour` in all seven files.
- [ ] **Step 4: Run the tests, `tools/check-localization.ps1` and `ExpandedLocaleContentTest`; all pass.** `git grep -n say_number_full_hour` returns nothing.
- [ ] **Step 5: Commit** (only after the owner asks).

### Task P3: Locked companion shows its requirement and action on screen

**Problems.**
- On the Pixel at 411 dp / 100 %, the Vanhemmille button is half under the bottom fade (`GALLERY/411dp-100/companion-picker-locked-free`, `-locked-tap`).
- At 320 dp / 100 % and at 200 %, neither the requirement text nor the button is visible after tapping a locked thumbnail.
- In the full version with fewer than three stars, the text still says "Edellyttää täysversiota ja 3 ansaittua tähteä." although the full version is owned (`AppearanceScreen.kt:170` always uses `companion_access_requirement`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/appearance/AppearanceScreen.kt` (actions around lines 168–172)
- Modify: `app/src/main/java/fi/clocklings/ui/appearance/CompanionThumbnails.kt` (state description of star locks, from E2)
- Modify: `app/src/main/res/values*/purchases.xml` or `strings.xml` (seven files; put it next to `companion_access_requirement`)
- Test: the E2 tests (`lockedCompanionTapShowsRequirement`, `lockedCompanionTapInFullVersionWithoutStars`)

**Interfaces:**
- Produces: string `companion_star_requirement`. Finnish "Ansaitse 3 tähteä, niin saat tämän kaverin." English "Earn 3 stars to unlock this companion."

- [ ] **Step 1: Write the failing tests.**
  - Extend `lockedCompanionTapInFullVersionWithoutStars` to assert `companion_star_requirement` is shown and `companion_access_requirement` is not.
  - Add `lockedCompanionActionVisibleAfterTap`. At 411 dp / 100 % with a viewport the height of the Pixel 9 with three-button navigation (take the window size from `GALLERY/411dp-100` captures), and at 320 dp / 100 %, after clicking `choose_companion_DOG` and the pager settling:
    - the requirement text is fully above the bottom fade;
    - in the free version, the `parent_title` button is also fully above the fade.
- [ ] **Step 2: Run them; they fail.**
- [ ] **Step 3: Choose the text.** Use `companion_star_requirement` when `access.fullAccess`, and `companion_access_requirement` otherwise. Use the same choice for the E2 thumbnail state description.
- [ ] **Step 4: Bring the actions into view** when the settled page is a locked companion. Reuse `bringFeedbackIntoView`, keyed by the settled page.
- [ ] **Step 5: Run the tests and the appearance and pager tests; all pass.** Run `tools/check-localization.ps1`.
- [ ] **Step 6: Commit** (only after the owner asks).

### Task P4: Level result fits the reward card for every companion

**Problem.** E3 capped only the doll (`ResultScreen.kt:125`, `if (kind == CompanionKind.HUMAN)`). On the Pixel with three-button navigation, the cat's card with two or three rewards ends 24.4 dp under the fade at 411 dp / 100 % (`measure.txt` R1 CAT), and "Valitse asu" touches the fade (`GALLERY/411dp-100/result-3-stars-cat-p0`). Other companions were not measured.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt`, `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt` (`tallCompanionResultFitsRewardAction`)

- [ ] **Step 1: Generalise the test** to `everyCompanionResultFitsRewardAction`. Loop over every `CompanionKind` with three rewards, at the Pixel 9 three-button-navigation viewport and 100 %, and assert that the bottom of the `dress_doll` button is above the fade. `NONE` has no rewards card; assert that it does not crash and that the stars, title and actions are visible.
- [ ] **Step 2: Run it; the cat case fails.**
- [ ] **Step 3: Size the companion from the space left over**, not by kind.
  - Measure the reward card and the text (for example with `SubcomposeLayout` or a `BoxWithConstraints` around the column).
  - Give the companion the remaining height, between a new `SkDimens.ResultCompanionMinHeight` and the current size.
  - Remove the `HUMAN` special case and `ResultCompanionMaxHeight` if nothing else uses them.
  - Keep the aspect ratio.
- [ ] **Step 4: Run the test, `VisibleUnlocksTest` and the F1 reward-card tests; all pass.** Record each companion's height at the Pixel viewport.
- [ ] **Step 5: Commit** (only after the owner asks).

### Task P5: Large-font lesson quizzes show their first option

**Problem.** At 320 dp / 200 %, the lesson quizzes 1 (viisarit), 2 (kierros), 6 (viisi), 8 (minuutit) and 9 (vrk) show no option before scrolling. Codex measured only lesson 5 (`measure.txt` "lesson-quiz", +33.2 / −64.9 dp). The two stacked full-width buttons "Edellinen" and "Valmis" take about a quarter of the screen. Long task titles wrap to three lines with the back arrow floating in the middle (`GALLERY/320dp-200/lesson-06-viisi-quiz-p0`, `lesson-08-minuutit-quiz-p0`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt` (`LessonActions`, `LessonNavigation`)
- Modify: `app/src/main/java/fi/clocklings/ui/components/ScreenHeader.kt` (compact task title, around line 75)
- Modify: `app/src/main/java/fi/clocklings/ui/components/WordSafeText.kt` (only if Step 4 needs a line limit in the autosize)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt` (`largeFontTaskShowsFirstOption`)

- [ ] **Step 1: Extend** `largeFontTaskShowsFirstOption` to loop over all nine lesson quizzes in Finnish and German at 320 dp / 200 %. Assert that the first option's top is above the fade and that the title takes at most two lines.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Compact previous action.** When `ActionPair` would stack and the font scale is above `SkDimens.MAX_ROW_FONT_SCALE`, show the previous action as a `RoundIconButton` with `SkIcons.Back`. Give it the content description `R.string.previous` (find the existing "Edellinen" key with `git grep -n ">Edellinen<" app/src/main/res/values-fi`). Put it beside the primary action, which takes the rest of the row.
  - Lesson pages and quizzes only.
  - The primary label and its kind do not change.
  - At other sizes `ActionPair` is unchanged.
- [ ] **Step 4: Two-line compact title.** In the compact task title, align the back button to the top of the row. Limit the title to two lines by letting `WordSafeText` choose a smaller size until both of these hold: no word breaks, and the line count is at most `maxLines`. Pass `maxLines = 2`. Do not truncate.
- [ ] **Step 5: Run the tests, `ClockTaskScaffoldTest`, the TopBar tests and the lesson navigation tests; all pass.** Record each quiz's first-option clearance at 320 dp / 200 %. If a quiz still cannot show the whole first option, record its top clearance and report it.
- [ ] **Step 6: Commit** (only after the owner asks).

### Task P6: Minute Challenge timer keeps its length at large font

**Problem.** With E10's compact header, the "N oikein" pill shares the progress row, and the rainbow timer bar shrinks to about a third of the width at 320 dp / 200 % (`GALLERY/320dp-200/challenge-running-p0`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/challenge/ChallengeScreen.kt:130`
- Test: the challenge screen test (find with `git grep -ln "ChallengeScreen(" app/src/test`)

- [ ] **Step 1: Write the failing test** `challengeTimerKeepsWidthAtLargeFont`. At 320 dp / 200 %, the timer bar is at least half the progress row width. The score node's content description still reads the full `challenge_score` text.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Compact score pill.** In the compact layout only, show the score pill as a check icon plus the number, with `contentDescription` set to `challenge_score`. The full pill stays in all other layouts.
- [ ] **Step 4: Run the test and the challenge tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task P7: No word breaks inside words

**Problem.** At 320 dp / 200 %:
- "Clocklings-täysve / rsio" on the parent screen (`ParentScreen.kt:186` uses plain `Text`).
- "iltapäiväll / ä" in Explore (`ui/components/Display.kt:156` uses plain `Text` for the time words).
- "Tasatun / nit" in the Play level card (`GALLERY/320dp-200/play-levels-p0`). That title already uses `WordSafeText`, but the decorative clock thumbnail and the book button leave too little width, so the text reaches the minimum scale and still breaks.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/parent/ParentScreen.kt:186`, `app/src/main/java/fi/clocklings/ui/components/Display.kt:156`
- Modify: `app/src/main/java/fi/clocklings/ui/components/PathRow.kt` (title width at large font; see `stackShortcut` at line 152)
- Test: a new `WordBreakRegressionTest` in `app/src/test/java/fi/clocklings/ui/`

- [ ] **Step 1: Write the failing test** `noMidWordBreaksAtLargeFont`, at 320 dp / 200 % in all seven languages. For each case, read the text layout and assert that every line ends at a space, a hyphen or the end of the text. Cases:
  - the parent screen title;
  - the Explore time words for 15.00 and 18.15 in 24-hour mode;
  - every Play level card title, including the Minute Challenge.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Use `WordSafeText`** for the parent title and the Explore time words, with `textAlign = TextAlign.Start` where the current text is start-aligned.
- [ ] **Step 4: Give PathRow titles the full width at large font.** When the font scale is above `MAX_ROW_FONT_SCALE`, move the decorative thumbnail above the title row (or omit it). Its semantics are already cleared, so TalkBack output must not change.
- [ ] **Step 5: Run the test, the `PathRow` tests and the learning-path tests; all pass.** Check every other `WordSafeText` caller's existing test still passes.
- [ ] **Step 6: Commit** (only after the owner asks).

### Task P8: Android 17 splash keeps the light background

**Problem.** With Android's expanded dark theme on, the Pixel shows the system splash at #11060C with a light icon circle (`GALLERY/411dp-100-dark/startup--01-android-splash.png`). `values-v31/themes.xml` sets `windowSplashScreenBackground` to `@color/window_background` (#FFF5FA). `values-night-v36/themes.xml` sets `isLightTheme` to `false` to opt out of automatic darkening. REPORT.md records the issue without a cause.

**Files:**
- Modify: `app/src/main/res/values-v31/themes.xml`, `app/src/main/res/values-night-v36/themes.xml` or a new `values-night-v37` qualifier, depending on the documented cause
- Test: `app/src/test/java/fi/clocklings/ui/WindowThemeTest.kt`

- [ ] **Step 1: Research first.** Read the current Android documentation for the expanded dark theme and for `SplashScreen` starting windows on API 36 and 37. Record the source URLs in the final report. Find out whether the system darkens a starting window whose theme reports `isLightTheme=false`, and which attribute keeps the app's own splash colour.
- [ ] **Step 2: Extend `WindowThemeTest`** to assert the starting theme's resolved splash background is #FFF5FA in night mode on SDK 36 (and 37 if Robolectric supports it).
- [ ] **Step 3: Apply the documented fix.** Keep `forceDarkAllowed=false` and the main window's light palette. Do not draw a dark splash on purpose.
- [ ] **Step 4: On the Pixel with expanded dark theme on, capture `startup--01-android-splash`.** Measure the background pixel and restore the phone's dark-theme setting afterwards. If the documentation shows the system colour cannot be controlled, stop and report that with the source instead of a workaround.
- [ ] **Step 5: Commit** (only after the owner asks).

### Task P9: Capture harness shows the real states

**Problems in `VisualFollowUpRound2CaptureTest`:**
- **Lesson quiz fixtures omit `under`.** `captureLessons` (around line 316) builds `LessonUiState` without `under = lesson.quiz.under`, so D7's glow on number 1 never appears in `lesson-08-minuutit-quiz`.
- **`result-final` and `result-fallback`** (around line 833) are captured before `PoppingStar` runs. The screens say "Ansaitsit 3 tähteä" over grey stars.
- **`startup--02-intro`** (around line 891) shows `LoadingScreen`. With `mainClock.autoAdvance = false`, the `show { IntroOverlay }` recomposition has not happened when the shot is taken.
- **`parent-gate-error` at 320 dp / 200 %** shows a typed "0" with no error and the same problem as `parent-gate`.
- **The locked picker captures** have no scroll pages, so the Vanhemmille button is never fully shown.
- **`practice-save-failure`** has no answered option, unlike the real flow, where the correct option is marked before the save fails.
- **Only lesson 5's quiz and the running challenge are measured** for large-font visibility.

**Files:**
- Modify: `app/src/androidTest/java/fi/clocklings/VisualFollowUpRound2CaptureTest.kt`

- [ ] **Step 1: Quiz `under`.** Pass `under = lesson.quiz.under` (and keep `period`) in the lesson quiz fixture.
- [ ] **Step 2: Result stars.** Before capturing `result-final` and `result-fallback`, advance the main clock past `STAR_DELAY_MS + 2 * STAR_STAGGER_MS + POP_MS`, or wait until the stars have popped. Assert the star description node before the shot.
- [ ] **Step 3: Intro.** After `show { IntroOverlay(...) }`, advance one frame and assert the intro greeting text is displayed before `shot("startup--02-intro")`.
- [ ] **Step 4: Gate error.**
  - Enter a wrong answer, submit it, and assert that the error text `R.string.parent_gate_error` (find the key with `git grep -n "Kokeile uutta laskua"`) is displayed before the capture, in every configuration.
  - Assert that the problem text differs from the `parent-gate` capture.
- [ ] **Step 5: Locked pickers.** Use `capturePages` for the three `companion-picker-locked-*` captures. After P3, `-p0` should already show the action.
- [ ] **Step 6: Save failure.** Set `optionStates = mapOf(target to OptionState.CORRECT)` in the `practice-save-failure` fixture.
- [ ] **Step 7: Measurements.**
  - Call `measureOption` for the first option of all nine lesson quizzes.
  - Also measure the correct option in `practice-read-reveal` and the third option in `challenge-wrong-answer`.
  - Add a result-card clearance line for every companion kind.
- [ ] **Step 8: Run `:app:assembleDebugAndroidTest`; it passes.**
- [ ] **Step 9: Commit** (only after the owner asks).

---

## Order of work

P9 first, so every later fix is captured from the real states. P1, then P5 and P6, because all three change the large-font task layout, and the P5 measurements must include P1. P2, P3, P4, P7 and P8 are independent of each other.

## Validation and acceptance

- [ ] `.\gradlew.bat --no-daemon :app:testDebugUnitTest` passes with zero failures. Record the test count against the 593 in `STATUS.md`.
- [ ] `.\gradlew.bat --no-daemon :app:assembleDebug` and `:app:assembleDebugAndroidTest` pass.
- [ ] `tools/check-localization.ps1` reports seven-language parity.
- [ ] On the Pixel 9, recapture the full gallery in all seven configurations of the 23 November gallery (`411dp-100`, `411dp-200`, `320dp-100`, `320dp-200`, `411dp-100-dark`, `411dp-reduced`, `411dp-replay`) and check:
  - P1: at `411dp-100` the hint is in the question line with the dial at about 302 dp. In the other three sizes the hint is below the options, and after the reveal the correct option is fully visible.
  - P2: `lesson-06-viisi-page-1` dial at `411dp-100` compared with page 2, in dp.
  - P3: all three `companion-picker-locked-*` show the requirement, and in the free version the whole Vanhemmille button, at `411dp-100`. The stars state shows the new text.
  - P4: `result-2-stars` and `result-3-stars` for the cat and the doll at `411dp-100` show "Valitse asu" above the fade. Record the clearance for every companion.
  - P5: the first-option clearance of all nine lesson quizzes at `320dp-200`, and titles in at most two lines.
  - P6: `challenge-running` at `320dp-200` with a timer bar at least half the row.
  - P7: no word broken inside in `parent-store-*`, `main-explore-*` and `play-levels-*` at `320dp-200`.
  - P8: `411dp-100-dark/startup--01-android-splash` background colour.
  - P9: D7 glow visible in `lesson-08-minuutit-quiz`, filled stars in `result-final` and `result-fallback`, the intro in `startup--02-intro`, the error state in every `parent-gate-error`, and a marked correct option in `practice-save-failure`.
- [ ] Restore every phone setting changed for the run (density, font scale, dark theme, animation scale) and stop every process started for the task.
- [ ] Update `docs/implementation/STATUS.md` with a dated entry. The owner runs `lc` and `sc`.

## Final report (Finnish)

- Implemented tasks P1–P9 and what each changed.
- Checks run and their results. Report what was seen on the device separately from source inspection.
- Every measurement in the Validation list, and any that fell below the acceptance threshold.
- The documentation sources used in P8.
