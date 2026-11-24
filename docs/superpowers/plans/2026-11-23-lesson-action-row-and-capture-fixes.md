# Clocklings lesson action row and capture fixes (23 November 2026) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a planning deliverable. It is not permission to commit, push, open a pull request or publish.

**Goal:** Fix the remaining findings from the review of the 23 November feedback-visibility gallery:
- The "Seuraava" label breaks mid-word on lesson 6 page 1 at 320 dp / 200 %.
- Four capture fixtures do not show real states.
- The effect of Android's per-app expanded dark theme switch on the splash is still unverified.

**Architecture:** One presentation fix in `LessonScreens.kt`, fixture and capture fixes in the tracked gallery harness, and one device check recorded in `STATUS.md`. No new dependencies, strings, migrations or clock-mathematics changes.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric JVM tests, Compose instrumentation on the owner's Pixel 9 (Android 17, three-button navigation).

**Spec:** The review given to the owner on 23 November 2026, in Finnish, in the session that wrote this plan. Evidence: `reports/clocklings-pixel9-feedback-visibility-2026-11-23/` (gitignored), called **GALLERY**. `GALLERY/<config>/x` means `.../<config>/screens--x.png`.

**Starting revision:** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` on `codex/visual-follow-up-2026-11-17`. The working tree holds the uncommitted work up to and including the [G1–G7 plan](2026-11-23-feedback-visibility-and-light-only.md). Keep all of it.

## Global Constraints

- Read `AGENTS.md`, `docs/implementation/STATUS.md`, the [G1–G7 plan](2026-11-23-feedback-visibility-and-light-only.md) and this plan first. All repository rules apply.
- Work on a `codex/` branch. Do not commit, push or open a pull request unless the owner asks separately. The commit steps below apply only after that request.
- Follow the repository's `sonar analyze secrets` instruction before reading source.
- Identifiers, filenames and code comments in English.
- Use theme tokens. No middle-dot or pipe separators in UI, no coloured edge stripes.
- Keep every earlier decision, in particular:
  - D2: never hide answer options;
  - R2: the practice clock never grows within a task, and no clock minimum changes;
  - E1: no clock movement during a lesson demo;
  - P1: the tall-layout hint rule;
  - G4: on lesson 6 page 1, keep the 272 dp dial and the playback buttons in the action row at 411 dp / 100 %;
  - L1: the app is light only.
- Run Gradle only from this repository with `--no-daemon`. Device captures run on the owner's Pixel 9. Do not change its notification settings; banners in shots are not defects. Restore every phone setting you change.
- The owner runs `lc` and `sc`. Report in Finnish at the end.

## Review Focus

1. **H1 at the boundary between the tall and non-tall layouts.** On the same page, the playback buttons must not jump between the panel and the action row during the demo. The placement depends only on the layout, not on the demo state.
2. **H1 in all seven languages.** The German, French and Portuguese labels for "Seuraava" are longer than the Finnish one.

---

### Task H1: Lesson 6 page 1 uses the action-row playback only in the tall layout

**Problem.** G4 moved the speaker and replay buttons of lesson `viisi` page 1 into the bottom action row in every layout (`ui/lesson/LessonScreens.kt`, the `actions` lambda: `if (page != null && (compact || firstFiveMinutePage))`, and the panel: `if (!compact && !firstFiveMinutePage)`). The 272 dp minimum it protects (`SkDimens.FiveMinuteDemoClockMin`) is passed as `tallFitMin`, so it only applies in the tall layout.

At 320 dp / 200 %, the two round buttons leave the primary button too narrow. Its `WordSafeText` label reaches its minimum scale and breaks "Seuraa / va" (GALLERY `320dp-200/lesson-06-viisi-page-1-p0`). At 411 dp / 200 % and 320 dp / 100 % the row still fits.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt`
- Possibly modify: `app/src/main/java/fi/clocklings/ui/components/ClockTaskScaffold.kt`. `LocalTallClockTask` is provided only inside the content lambda (around line 126). If the `actions` lambda needs the same value, expose it the smallest way: pass the tall flag to `actions`, or provide the local around both.
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt` and the P7 `WordBreakRegressionTest`

- [ ] **Step 1: Write the failing tests.**
  - `fiveMinutePagePlaybackFollowsLayout`. At 411 dp / 100 % on the Pixel 9 viewport, the playback buttons are in the action row (G4). At 411 dp / 200 %, 320 dp / 100 % and 320 dp / 200 %, they are inside the panel, as on every other lesson page.
  - Extend `noMidWordBreaksAtLargeFont` with every lesson page's action-row labels ("Seuraava", "Kokeile", "Valmis", "Aloita pelaaminen!", "Seuraava oppitunti", "Yritä uudelleen"). Cover lesson 6 page 1, in all seven languages, in all four width and font combinations.
- [ ] **Step 2: Run them; the 320 dp / 200 % cases fail.**

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.ReviewRegressionTest' --tests 'fi.clocklings.ui.WordBreakRegressionTest'
```

- [ ] **Step 3: Gate the placement on the tall layout.** Replace `firstFiveMinutePage` in both conditions with `firstFiveMinutePage && tall`, where `tall` is the scaffold's tall-layout flag (`LocalTallClockTask` or the value passed in Step 1's interface change). Do not change `tallClockMin`.
- [ ] **Step 4: Run the tests, the G4 test `everyLessonPageShowsPlaybackAtDefaultSize`, the E1 demo-stability tests and `ClockTaskScaffoldTest`; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks): message "Näytä viiden minuutin sivun toistopainikkeet toimintorivillä vain väljässä näkymässä".

### Task H2: Lesson fixtures show the answered state

**Problem.** In `VisualFollowUpRound2CaptureTest.captureLessons` (around lines 385–394), `lesson-save-failure` uses `state.copy(saveFailed = true)` and `lesson-before-game` uses `state.copy(quizDone = true, ...)`, both without `choiceStates`. In the app, both states follow a correct pick, so the correct option is green. The gallery shows two unmarked options (GALLERY `411dp-100/lesson-before-game-p0`, `lesson-save-failure-p0`).

**Files:**
- Modify: `app/src/androidTest/java/fi/clocklings/VisualFollowUpRound2CaptureTest.kt`

- [ ] **Step 1: Derive the state from the view model.**
  - Confirm with `LessonViewModel.pick` which fields a correct pick sets before a save fails (`choiceStates`, `savePending`, `saveFailed`, `quizDone`).
  - Add `choiceStates = mapOf(correct to OptionState.CORRECT)` and any other field the view model sets to both fixtures. `correct` is already computed for `quiz-complete`.
- [ ] **Step 2: Assert the correct option has the correct state before each capture.** Use the existing semantics the quiz tests use for `OptionState.CORRECT`.
- [ ] **Step 3: Run `:app:assembleDebugAndroidTest`; it passes.**

### Task H3: Minute Challenge fixtures show a running timer

**Problem.** In the gallery cell, the challenge fixture uses `deadlineNanos = Long.MAX_VALUE` (around line 530). Both `challenge-running` and `challenge-wrong-answer` therefore show "60 s" (GALLERY `411dp-100/challenge-wrong-answer-p0`, `320dp-200/challenge-running-p0`). A wrong answer during a running challenge never happens at a full 60 s.

**Files:**
- Modify: `app/src/androidTest/java/fi/clocklings/VisualFollowUpRound2CaptureTest.kt`

- [ ] **Step 1: Fix the timer values.** Give the gallery fixtures a fixed remaining time: 59 s for `challenge-running` and 55 s for `challenge-wrong-answer`, as in the earlier galleries.
  - Keep the timer from moving during the capture: freeze the main clock, or use whatever field `ChallengeScreen` reads for the remaining time.
  - Do not change `ChallengeViewModel` for this.
- [ ] **Step 2: Assert the displayed seconds before each capture.**
- [ ] **Step 3: Run `:app:assembleDebugAndroidTest`; it passes.**

### Task H4: The light splash capture waits for the window animation

**Problem.** `captureAndroidSplash` (around line 982) accepts a frame when three sample points, at a quarter of the width and height, equal the background. In the light run it saved a frame taken mid-launch-animation: the Home wallpaper still shows at the left and right edges (GALLERY `411dp-100/startup--01-android-splash.png`, pixel x=5 is #0F0F0F and x=1075 is #E6E4E4 against #FFF5FA).

**Files:**
- Modify: `app/src/androidTest/java/fi/clocklings/VisualFollowUpRound2CaptureTest.kt`

- [ ] **Step 1: Check the edges as well.** Also require pixels at x = 2 and x = width − 3, at a quarter, half and three quarters of the height, to equal the sampled background. Exclude the status bar and navigation bar rows. Keep the existing retry loop.
- [ ] **Step 2: If 20 tries are not enough,** raise the retry count rather than adding a fixed sleep. Record the number of tries used in `measure.txt`.
- [ ] **Step 3: Run `:app:assembleDebugAndroidTest`; it passes.**

### Task H5: Verify Android's per-app expanded dark switch on the splash

**Problem.** L1 says the app stays light. G7 showed that Android 17 darkens the system splash when expanded dark theme is on, and that the app cannot prevent it. Android's accessibility settings let a user switch expanded dark theme off for one app. Its effect on the splash was not tested.

**Files:**
- Modify: `docs/implementation/STATUS.md` (the L1 entry)

- [ ] **Step 1: On the Pixel,** with system dark theme and expanded dark theme on, switch expanded dark theme off for Clocklings in Android's settings. Record the exact settings path you used. Launch the app cold and capture `startup--01-android-splash` with H4's check.
- [ ] **Step 2: Record the result under L1:** the measured splash background with the switch off, and whether Home stays #FFF5FA. Then restore the switch to its original state and verify it.
- [ ] **Step 3: Change no code or theme in this task.**

---

## Order of work

H1 first (the only app change). H2, H3 and H4 are independent harness fixes. H5 last, after H4, so its capture uses the stricter check.

## Validation and acceptance

- [ ] `.\gradlew.bat --no-daemon :app:testDebugUnitTest` passes with zero failures. Record the test count against the 607 in `STATUS.md`.
- [ ] `.\gradlew.bat --no-daemon :app:assembleDebug` and `:app:assembleDebugAndroidTest` pass.
- [ ] `java tools/CheckGalleryColors.java <gallery-directory>` passes for the new gallery.
- [ ] On the Pixel 9, recapture the full gallery in the seven configurations, then check the following:
  - H1: `lesson-06-viisi-page-1-p0` keeps every label whole at `320dp-200`. At `411dp-100` the playback stays in the action row and the dial stays 272 dp.
  - H2: the correct option is green in `lesson-before-game` and `lesson-save-failure`, in every configuration.
  - H3: `challenge-running` shows 59 s and `challenge-wrong-answer` shows 55 s.
  - H4: the light `startup--01-android-splash` has #FFF5FA at both edges.
  - H5: the result is recorded under L1.
- [ ] Restore every phone setting changed for the run and stop every process started for the task.
- [ ] Update `docs/implementation/STATUS.md` with a dated entry. The owner runs `lc` and `sc`.

## Final report (Finnish)

- Implemented tasks H1–H5 and what each changed.
- Checks run and their results. Report what was seen on the device separately from source inspection.
- The H5 settings path and measured splash colour.
