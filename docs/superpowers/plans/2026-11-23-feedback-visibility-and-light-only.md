# Clocklings feedback visibility and light-only app (23 November 2026) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a planning deliverable. It is not permission to commit, push, open a pull request or publish.

**Goal:** Fix every remaining finding from the review of the Pixel 9 follow-up gallery:
- Make the child's feedback visible without scrolling in every layout.
- Remove the ambiguous back arrow.
- Polish the last layout details.
- Keep Clocklings in its own light colours everywhere, including the splash screen, when Android's system dark theme is on.

**Architecture:** Presentation changes in existing Compose screens, one new icon token, one theme and starting-window investigation, a gallery regression check and a recorded owner decision. No new dependencies, no migration of stored progress, no change to clock mathematics or copy.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric JVM tests, Compose instrumentation on the owner's Pixel 9 (Android 17, three-button navigation).

**Spec:** The review given to the owner on 23 November 2026, in Finnish, in the session that wrote this plan. Evidence: `reports/clocklings-pixel9-gallery-follow-up-2026-11-23/` (gitignored), called **GALLERY**. `GALLERY/<config>/x` means `.../<config>/screens--x.png`. Device measurements are in `GALLERY/<config>/measure.txt`.

**Starting revision:** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` on `codex/visual-follow-up-2026-11-17`. The working tree holds the uncommitted work up to and including the [P1–P9 plan](2026-11-23-pixel9-gallery-follow-up.md). Keep all of it.

## Owner decision recorded by this plan

**L1. Clocklings has no dark mode.** When Android's system dark theme or expanded dark theme is on, the app keeps all of its own colours. This covers backgrounds, panels, text, the clock, the companions, the status bar and navigation bar styling, dialogs, and the splash screen. Do not add a dark palette, `DayNight` themes or `isSystemInDarkTheme()` branches. System surfaces the app does not draw (the keyboard, notifications, the notification shade) are out of scope.

Write L1 into `docs/implementation/STATUS.md` as an owner decision dated 23 November 2026.

## Global Constraints

- Read `AGENTS.md`, `docs/implementation/STATUS.md`, the [P1–P9 plan](2026-11-23-pixel9-gallery-follow-up.md) and this plan first. All repository rules apply.
- Work on a `codex/` branch. Do not commit, push or open a pull request unless the owner asks separately. The commit steps below apply only after that request.
- Follow the repository's `sonar analyze secrets` instruction before reading source.
- Identifiers, filenames and code comments in English. UI copy in resources; this plan adds no copy.
- Use theme tokens (`SkColors`, `SkDimens`, `SkType`, `SkShapes`, `SkMotion`, `SkIcons`). Add a token instead of an inline value.
- No middle-dot or pipe separators in UI, no coloured edge stripes.
- Never hide, collapse or remove answer options or the child's own picks (D2 rejected).
- Within one task the practice clock may shrink but never grow (R2). Do not change `FitClockMin`, `FeedbackClockMin` or `QuizClockMin`, and do not add a smaller clock minimum.
- Keep D6, E1 (no clock movement during a lesson demo), C9, and P1's tall-layout rule (the hint stays in the question line at 411 dp and 100 %).
- Scrolling into view uses the existing `bringFeedbackIntoView` in `ui/components/FocusScroll.kt`: once per feedback state, and a drag by the child takes priority. Do not add a second mechanism.
- Do not change companion artwork, Blender rigs or narration recordings (`res/raw*`).
- Run Gradle only from this repository with `--no-daemon`. Device captures run on the owner's Pixel 9. Do not change its notification settings; banners in shots are not defects.
- The owner runs `lc` and `sc`. Report in Finnish at the end.

## Review Focus

1. **G1 and G2 when the child has scrolled up to the clock before answering again.** A second wrong pick must bring the new hint into view once, and must not keep pulling the view down while the child scrolls.
2. **G1 and G2 at 320 dp / 200 %**, where the hint may be taller than the space below the clock. The hint's top must be visible. The child can then scroll for the rest.
3. **G6 in the timed Minute Challenge.** The scroll must not delay the next question or steal input from the next tap.
4. **G7** must not change anything in the normal (light system theme) launch. Compare the light gallery before and after.

---

### Task G1: Practice hint is scrolled into view below the options

**Problem.** In the non-tall layouts (411 dp / 200 %, 320 dp / 100 %, 320 dp / 200 %), P1 puts the READ hint below the options and never brings it into view. In GALLERY `411dp-200/practice-read-hint-p0`, `320dp-100/practice-read-hint-p0` and `320dp-200/practice-read-hint-p0`, only the companion's head shows at the bottom edge. `GameAnswerPanel` (`ui/game/GameScreen.kt`, around line 223) passes `scrollToMessage = !read || state.feedback == GameFeedback.SaveFailed`, which is `false` for a READ hint. The reveal and praise already bring the correct option into view; the hint was left out.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

- [ ] **Step 1: Write the failing test** `readHintBelowOptionsIsBroughtIntoView`.
  - Configurations: 411 dp / 200 %, 320 dp / 100 % and 320 dp / 200 %, in Finnish.
  - Pick the visible wrong option, then assert that the top of the hint text node is inside the viewport above the bottom fade.
  - At 411 dp and 100 %, assert that the hint is still in the question line (P1).
- [ ] **Step 2: Run it; the three non-tall cases fail.**

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.ReviewRegressionTest'
```

- [ ] **Step 3: Bring the hint into view.** For `GameFeedback.HintText` below the options, pass `scrollToMessage = true`. Pass the panel's `ScrollState` so a drag takes priority. Keep the key as the feedback text, so a second, different hint scrolls once more. Reveal and praise stay as P1 made them (the correct option is brought into view, not the message).
- [ ] **Step 4: Run the test, `revealBringsCorrectOptionIntoView`, `readClockNeverGrowsWithinATask` and `GameViewModelTest`; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks): message "Vieritä harjoituksen vihje näkyviin".

### Task G2: Lesson quiz feedback is scrolled into view

**Problem.** In `LessonQuiz` (`ui/lesson/LessonScreens.kt`, around lines 290–310), the hint, the "Oikein! Oppitunti … suoritettu" message and the save error are `CompanionTip`s with no bring-into-view. They are off-screen in the same three configurations as in G1 (GALLERY `411dp-200/lesson-01-viisarit-quiz-hint-p0`, `-quiz-complete-p0`, `lesson-save-failure-p0`, and the `320dp-*` equivalents).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt` or the existing lesson quiz UI test (find with `git grep -ln "lesson_learned" app/src/test`)

- [ ] **Step 1: Write the failing test** `lessonQuizFeedbackIsBroughtIntoView` in the three non-tall configurations and at 411 dp / 100 %. Check each of these states and assert that the top of the message text is inside the viewport above the fade:
  - after a wrong pick (hint);
  - after the correct pick (completion message);
  - after a storage failure (save error).
- [ ] **Step 2: Run it; the non-tall cases fail.**
- [ ] **Step 3: Add `Modifier.bringFeedbackIntoView(...)`** to the three `CompanionTip`s. Key it by the message state (hint shown, quiz done, save failed) and pass the screen's scroll state so a drag takes priority. Do not move the messages and do not change their wording.
- [ ] **Step 4: Run the test, `LessonViewModelTest`, `PersistenceFailureTest` and the C3 and C4 tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task G3: The compact previous-page button uses a back chevron

**Problem.** At large font, the compact previous-page button (`LessonScreens.kt`, around line 413) uses `SkIcons.Back`, the same arrow as the header's leave-lesson button. Two identical arrows with different meanings sit on the same screen (GALLERY `320dp-200/lesson-09-vrk-page-3-p0`, `411dp-200/lesson-01-viisarit-page-2-p0`). The Seuraava button uses `SkIcons.Chevron` (›).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/components/Icons.kt` (next to `Chevron`, line 99)
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt`
- Test: the P5 compact-navigation test (find with `git grep -n "compactStart\|RoundIconButton" app/src/test`)

**Interfaces:**
- Produces: `SkIcons.ChevronBack`, the mirror of `Chevron`: `inkIcon("M15 5l-7 7 7 7")`.

- [ ] **Step 1: Extend the P5 test.** Assert that the compact previous button's icon is `SkIcons.ChevronBack`, that its content description is still the previous-page string, and that the header back button still uses `SkIcons.Back`.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Add the token and use it in `compactStart`.** The header is unchanged.
- [ ] **Step 4: Run the test; it passes.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task G4: Lesson "Viiden minuutin välein" page 1 fits at default size

**Problem.** P2 gives this page a 272 dp dial minimum (`SkDimens.FiveMinuteDemoClockMin`, used at `LessonScreens.kt:154`). On the Pixel at 411 dp / 100 %, the speaker and replay buttons are pushed under the bottom fade after the demo (GALLERY `411dp-100/lesson-06-viisi-page-1-p0`). Every other lesson page fits.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt` (`FiveMinuteDemoClockMin`)
- Test: the P2 test `fiveMinutePageOneDialNotSmallerThanPageTwo` and a new fit test in `ReviewRegressionTest`

- [ ] **Step 1: Write the failing test** `everyLessonPageShowsPlaybackAtDefaultSize`. Use the Pixel 9 three-button-navigation viewport at 411 dp and 100 % (as in P3 and P4). For every page of every lesson after the demo, assert that the speaker and replay buttons are fully above the fade.
- [ ] **Step 2: Run it; the `viisi` page 1 case fails.**
- [ ] **Step 3: Lower the token** to the largest value that passes this test, but no lower than the page 2 dial minus one `SkType.LessonText` line height. That keeps the P2 test green. Record the value. If no value satisfies both tests, keep 272 dp and show the playback row in the bottom action area for this page, as the compact layout already does. Report which path you took.
- [ ] **Step 4: Run the tests and the E1 demo-stability tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task G5: The car is not visibly smaller on the result screen

**Problem.** On the 3-reward result, the car looks clearly smaller than the other companions (GALLERY `411dp-100/result-3-stars-car-p0` against `result-3-stars-bunny-p0`). P4 gives the car and the dog a 118.5 dp slot, because their two-line reward labels take more height (`measure.txt`). The car's art also fills less of the shared animal grid (`companionAspectRatio` in `CompanionRig.kt:167` uses one grid for every animal).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt` (P4's measured layout)
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt` (`everyCompanionResultFitsRewardAction`)

- [ ] **Step 1: Measure first.** In a capture or a JVM render, find the car's and the cat's visible art bounds: the non-transparent pixels inside the companion slot at the same slot height. If the car's visible width is at least 80 % of the cat's, record the numbers and stop here; there is nothing to fix.
- [ ] **Step 2: If it is smaller,** give `CAR` a result-only display scale. Use the ratio of the visible widths, capped so the slot still fits the P4 rule. Put it in one `when (kind)` next to the result layout, not in the rig, and do not change the art.
- [ ] **Step 3: Extend** `everyCompanionResultFitsRewardAction` to assert that the car case still fits. Run it with `VisibleUnlocksTest`; all pass.
- [ ] **Step 4: Commit** (only after the owner asks).

### Task G6: Minute Challenge shows the correct option after a wrong answer

**Problem.** At 320 dp / 200 %, after a wrong answer the correct option gets a check mark, its label wraps to two lines, and the third option's top falls 9.5 dp below the fade (`measure.txt` `challenge-wrong-answer`; GALLERY `320dp-200/challenge-wrong-answer-p0`). The child may not see which answer was right before the next question.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/challenge/ChallengeScreen.kt` (option list around lines 220–250)
- Test: the challenge screen test (find with `git grep -ln "ChallengeScreen(" app/src/test`)

- [ ] **Step 1: Write the failing test** `challengeWrongAnswerShowsCorrectOption` at 320 dp / 200 %. After a wrong pick, the option marked correct is fully inside the viewport above the fade. Repeat with the correct option as the third one.
- [ ] **Step 2: Run it; the third-option case fails.**
- [ ] **Step 3: Bring the correct option into view** on the wrong-answer state with `bringFeedbackIntoView`, keyed by the question index, the same way P1 does for practice. The next question resets the scroll as today.
- [ ] **Step 4: Run the test and the challenge tests, including the timer tests; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task G7: Clocklings stays light under the system dark theme, including the splash

**Problem.** The owner confirmed that the app has no dark mode (L1). In the dark gallery (GALLERY `411dp-100-dark`) every app screen matches the light set except for notification banners. The exception is the Android splash, which shows #11060C with a light icon circle (`startup--01-android-splash.png`). P8 concluded that Android 17's `SplashscreenContentDrawer` darkens the starting window from the expanded dark theme setting, independently of the app theme. Google's Android 16 QPR2 announcement also says the system darkens app splash screens. No app-side opt-out has been found yet; users can switch expanded dark theme off per app in system settings.

**Files:**
- Possibly modify: `app/src/main/res/values-v31/themes.xml`, `app/src/main/res/values-night-v36/themes.xml`, `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/fi/clocklings/ui/WindowThemeTest.kt`; a new device check in the capture harness

- [ ] **Step 1: Read the sources and record the exact condition.** In AOSP `SplashscreenContentDrawer.java` on `android17-release` (the file P8 cited), find the code path that darkens the background. Write down every condition it checks: theme attributes, window flags, splash style, target SDK, per-app setting. Quote the relevant lines in the final report. Also read:
  - [Android 16 QPR2 Beta 1](https://android-developers.googleblog.com/2025/08/android-16-qpr2-beta-1-is-here.html)
  - [Android 16 QPR2 release](https://developer.android.com/blog/posts/android-16-qpr-2-is-released)
  - [Android dark theme accessibility help](https://support.google.com/accessibility/android/answer/6151800?hl=en)
  - [Splash screen guide](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [ ] **Step 2: Try only documented or source-backed options on the Pixel,** one at a time, with expanded dark theme on. For each, record the measured splash background, and check that the normal light launch is unchanged.
  - Examples, only if Step 1 shows they matter: the starting theme's `isLightTheme` value in `values-night-v36`, `windowSplashScreenBackground` or icon-background attributes, the splash style passed by the launcher, or a manifest attribute.
  - Do not use reflection, hidden APIs, or a fake dark splash.
- [ ] **Step 3: If an option keeps #FFF5FA,** apply it, extend `WindowThemeTest` to lock it in, and keep `forceDarkAllowed=false` and every other window attribute unchanged.
- [ ] **Step 4: If no option works,** change nothing in the theme. Write in `STATUS.md` under L1 that the system splash is darkened by Android under expanded dark theme and cannot be controlled by the app, with the source lines. Note that a user can switch it off per app in Android's dark theme settings.
- [ ] **Step 5: Add a light-only regression check to the capture harness.** For every screen captured in both `411dp-100` and `411dp-100-dark`, compare the app area: exclude the status bar, the navigation bar, and any notification banner region (detect it, or limit the comparison to below the app's top bar). Fail the check on any colour difference above a small tolerance. `startup--01-android-splash` is the only allowed exception, and only if Step 4 applied.
- [ ] **Step 6: Commit** (only after the owner asks).

---

## Order of work

G7 Step 1 first, because its result decides whether anything in the theme changes. Then G1 and G2 together (same mechanism), then G6, which uses the same mechanism in the challenge. G3, G4 and G5 are independent of each other.

## Validation and acceptance

- [ ] `.\gradlew.bat --no-daemon :app:testDebugUnitTest` passes with zero failures. Record the test count against the 602 in `STATUS.md`.
- [ ] `.\gradlew.bat --no-daemon :app:assembleDebug` and `:app:assembleDebugAndroidTest` pass.
- [ ] `tools/check-localization.ps1` reports seven-language parity (no strings change, but run it).
- [ ] On the Pixel 9, recapture the full gallery in the seven configurations of the 23 November follow-up gallery, then check the following:
  - G1: `practice-read-hint` in the three non-tall configurations shows the hint text. At `411dp-100` the hint is still in the question line, with the dial at about 302 dp.
  - G2: `lesson-01-viisarit-quiz-hint`, `-quiz-complete` and `lesson-save-failure` show their message in every configuration.
  - G3: the compact previous button shows ‹, and the header still shows ←.
  - G4: `lesson-06-viisi-page-1` at `411dp-100` shows the speaker and replay buttons. Record the dial size.
  - G5: the car's visible width compared with the cat's in `result-3-stars-*`.
  - G6: `challenge-wrong-answer` at `320dp-200` shows the correct option.
  - G7: the light-only comparison of `411dp-100` against `411dp-100-dark` passes, and the measured splash background is recorded.
- [ ] Restore every phone setting changed for the run (density, font scale, dark theme, expanded dark theme, animation scale, USB wake) and stop every process started for the task.
- [ ] Update `docs/implementation/STATUS.md` with a dated entry and the L1 decision. The owner runs `lc` and `sc`.

## Final report (Finnish)

- Implemented tasks G1–G7 and what each changed.
- Checks run and their results. Report what was seen on the device separately from source inspection.
- Every measurement in the Validation list, and any that fell below the acceptance threshold.
- For G7: the source lines and documentation used, every option tried with its measured result, and the final outcome.
