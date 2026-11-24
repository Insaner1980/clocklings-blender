# Clocklings child clarity review (22 November 2026) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a planning deliverable. It is not permission to commit, push, open a pull request or publish.

**Goal:** Make the learning path, practice and result screens clearer and more consistent for a child, based on the 22 November Pixel 9 gallery review.

**Architecture:** Presentation and copy changes in the existing Compose screens and string resources, plus one small view-model action (lesson save retry). Content and ordering changes to the curriculum wait for the owner decisions in section "Owner decisions". No new dependencies, no migration of stored progress.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric JVM tests, Android string resources in seven languages.

**Spec:** The review given to the owner on 22 November 2026 (in Finnish, in the session that wrote this plan). Evidence: `reports/clocklings-pixel9-kuvagalleria-2026-11-22/` (gitignored, local checkout only), called **GALLERY** below. `GALLERY/screens/x.png` means `reports/clocklings-pixel9-kuvagalleria-2026-11-22/screens/x.png`.

**Starting revision:** `ce7e88185f7251b1a62275aeadf5be9ae15c583b` on `codex/visual-follow-up-2026-11-17`, with the uncommitted work of the [21 November simplification plan](2026-11-21-behavior-preserving-simplifications.md) in the working tree. Keep that work.

## Global Constraints

- Read `AGENTS.md`, `docs/implementation/STATUS.md`, the [round 2 plan](2026-11-17-visual-follow-up-round-2.md) and this plan first. All repository rules apply.
- Work on a `codex/` branch. Do not commit, push or open a pull request unless the owner asks separately. The commit steps below apply only after that request.
- Follow the repository's `sonar analyze secrets` instruction before reading source.
- Seven languages: `values`, `values-fi`, `values-sv`, `values-de`, `values-fr`, `values-es`, `values-pt-rBR`. Every new string, and every string whose meaning changes, needs all seven. A Finnish-only wording fix that keeps the meaning changes only `values-fi`.
- Identifiers, filenames and code comments in English. UI copy in resources.
- Use theme tokens (`SkColors`, `SkDimens`, `SkType`, `SkShapes`). Add a token instead of an inline value.
- No middle-dot or pipe separators in UI, no coloured edge stripes.
- Never hide, collapse or remove answer options or the child's own picks (owner rejected D2 on 17 November).
- Keep the 24-hour "to"/"past" pills beside the day-part chip (D4, approved 17 November). Do not remove them or move them back into the dial.
- Within one task the practice clock may shrink but never grow (round 2, R2). Do not change `FeedbackClockMin`, `FitClockMin` or `QuizClockMin`.
- Do not change companion artwork, Blender rigs or narration recordings (`res/raw*`). A string that has a narration recording (`Sound.entries` with `narration = true`, for example `voice_result_1`) must not change its wording, because the recording would no longer match.
- Run Gradle only from this repository with `--no-daemon`. Use only the dedicated `ClocklingsReview20261030` emulator.
- The owner runs `lc` and `sc`. Report in Finnish at the end.

## Review Focus

1. **Long two-line answer options in 24-hour mode at level 5** (Finnish, German): after C1 all three options must be fully visible at default size before answering, and at 200 % font they may scroll but all three stay in the list. Test in C1.
2. **The clock never grows within one READ task** after C1 changes the fit mode: initial, hint and reveal sizes must be non-increasing, and the next task starts at its own fitted size. Test in C1.
3. **Lesson save retry tapped twice, or tapped while the save is still pending:** only one write, no duplicate "correct" analytics count, the button stays disabled while pending. Test in C3.
4. **A lesson quiz completed as the last lesson before a level:** the action label must say "Start playing", not "Done", and must start the game exactly as before. Test in C4.
5. **Locked practice rows in the free version:** the new practice title must still read as locked and keep the "Full version" supporting text and semantics. Test in C2.

---

## Owner decisions

These change curriculum content, recorded voice or purchase behaviour. Do not implement them until the owner answers. Each has a recommendation.

### D5. Lesson order: "Yli ja vaille" before counting by fives

**Problem.** Lesson 6 `ylivaille` uses 3.20, 3.40, 3.10 and 3.50 (`ui/lesson/LessonCatalog.kt`, pages 1 to 3) and its quiz is 4.10 ("Kymmentä yli neljä"). Counting the minute hand in fives is taught only in lesson 7 `viisi`. Level 3 "Vartit" (`Levels.all[2]`) only uses minutes 0, 15, 30 and 45, so `ylivaille` does not fit the level it is attached to. The path also shows the game "Vartit" after the lesson "Yli ja vaille", not after the lesson "Vartit" (GALLERY `lesson-path-p1.png`).

**Recommendation.** Move `ylivaille` to the five-minute level, after `viisi`:

```kotlin
val levelLessons = listOf(
    listOf("viisarit", "kierros", "tasan"),
    listOf("puoli"),
    listOf("vartti"),
    listOf("viisi", "ylivaille"),
    listOf("minuutit"),
)
```

This puts the game "Vartit" directly after the lesson "Vartit" and teaches fives before "kahtakymmentä vaille". Stored progress keeps working because `lessonsDone` stores lesson ids. A player who already saw the level 3 intro is not shown `ylivaille` automatically again, which is acceptable because it stays in the path. `viisi` page 3 ("viittä vaille puoli … Nimeä seuraava tunti") then comes before the "name the next hour" explanation; the owner may want that page moved into `ylivaille`. Update `LessonsTest` (`level lessons match the web mapping`, `lesson chapters map every lesson to its practice level`) and `AccessPolicyTest` only if the owner approves.

### D6. Lesson text shown before the demo reaches the described time

**Problem.** Each lesson page shows both paragraphs at once, while the demo moves the hands to the described time 0.6 to 4 seconds later. For example "Tässä sanomme varttia yli kolme" is visible while the clock still shows 3.00 (`LessonCatalog.kt` vartti page 2: `pause(600)`, `move(195, 2500)`). A reading child sees text and clock disagree.

**Recommendation.** Show the last paragraph of a page only after the demo's final `DemoStep.Say`, and show it at once when reduced motion is on or the page is replayed to its end. The first paragraph (the instruction) stays visible from the start.

### D7. Minutes quiz on the smallest clock

**Problem.** The "Tarkat minuutit" quiz asks for 3.07 and requires counting single minute ticks, but quiz clocks fit to `QuizClockMin` (200 dp) and are the smallest clocks in the lesson (GALLERY `lesson-08-minuutit-quiz-p0.png`).

**Recommendation.** Give `Quiz` an optional `under: List<Mark>` like `LessonPage`, and for the `minutit` quiz glow number 1 (`num(1)`), the numbered mark just before the hand, which is exactly the method page 2 teaches. The clock size rules stay unchanged.

### D8. Result title for one star

`result_title_1` is "Jatka tutkimista!", which echoes the separate "Tutki" (free clock) menu. Changing it needs a new Finnish recording of `voice_result_1`. Recommendation: keep it unless a new recording is planned; if so, "Hyvä alku!".

### D9. Purchase product name in Finnish UI

`full_unlock_name` is "Clocklings Full Unlock" in every language (`res/values*/purchases.xml`). Decide whether the parent screen shows a localized name (for example "Clocklings-täysversio") or keeps the store product name.

### D10. Star cost on items that need the full version

In the free wardrobe a locked tile shows both the star cost pill and the "Täysversio" pill (`WardrobeScreen.kt`, `if (needsStars)` and `if (requiresFullVersion)`). Recommendation: show only the "Täysversio" pill when `requiresFullVersion` is true. The parent may value the star count; the owner decides.

Still open from earlier plans and not repeated here: T8 (header star total), T19 gate method (reversed digits are easy for a reading 7-year-old), T30.

---

## Considered and not planned

- **Clock size changes between practice states** (GALLERY `practice-read-*`): this is the approved R2 behaviour (shrink only). No change.
- **"to"/"past" pills only in 24-hour mode:** D4 covers 24-hour mode only. Showing them in 12-hour mode would be a new owner decision; not proposed now.
- **Unlabelled yellow help toggle:** a deliberate design from the 27 October and 14 November plans with a visible on-state. No change.
- **Lesson 9 quiz options use "Seitsemän illalla: 19.00" instead of a time pill:** low value for the effort.

---

### Task C1: Practice READ tasks fit all three answers before answering

**Problem.** `GameScreen` uses `ClockFit.NONE` until feedback appears, with `GAME_CLOCK_HEIGHT_SHARE = 0.58`. In 24-hour mode and at levels 4 and 5 the third option is cut by the bottom fade before the child answers (GALLERY `practice-24h-p0.png`, `practice-level-4-read-p0.png`, `practice-level-5-read-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt:150`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

**Interfaces:**
- Consumes: `ClockFit`, `scaffoldClockSize(...)` held-size rule in `ui/components/ScaffoldClockSizing.kt` (unchanged).
- Produces: nothing new.

- [ ] **Step 1: Write the failing test** `readTaskShowsEveryOptionBeforeAnswering` in `ReviewRegressionTest`. Use the existing viewport helpers in `TestViewports.kt` and locales in `TestLocales.kt`. Render a level 5 READ task in Finnish, 24-hour mode, help on, at the default phone viewport and 100 % font. Assert for each of the three option nodes that its bounds bottom is less than or equal to the top of the bottom action area (or of the scaffold bottom fade when there is no action). Add a second case at 320 dp with 200 % font that asserts only that all three option nodes exist and are scrollable into view.
- [ ] **Step 2: Run it and confirm the default-size case fails.**

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.ReviewRegressionTest'
```

- [ ] **Step 3: Change the fit rule** in `GameScreen.kt` to `clockFit = if (state.feedback != null || state.task.type == TaskType.READ) ClockFit.FIT else ClockFit.NONE`. SET tasks keep the full-size clock because the child drags it.
- [ ] **Step 4: Add the growth guard test** `readClockNeverGrowsWithinATask` next to the existing R2 tests (find them with `git grep -n "never grow" app/src/test`). Measure the clock box before answering, after one wrong pick and after the reveal; assert each size is less than or equal to the previous one.
- [ ] **Step 5: Run both tests and the existing game and scaffold tests; all pass.**

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.ReviewRegressionTest' --tests 'fi.clocklings.ui.game.GameViewModelTest' --tests 'fi.clocklings.ui.components.ClockTaskScaffoldTest'
```

- [ ] **Step 6: Commit** (only after the owner asks): `git add` the two files, message "Sovita lukutehtävän kello niin, että kaikki vastaukset näkyvät".

### Task C2: Practice rows in the learning path get their own title

**Problem.** The path shows the lesson "Tasatunnit" and directly below it the game row "Tasatunnit". Only the star icon tells them apart (GALLERY `lesson-path-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonListScreen.kt` (`LessonPracticeNode`)
- Modify: `app/src/main/res/values*/strings.xml` (seven files)
- Test: `app/src/test/java/fi/clocklings/ui/ScreenInteractionTest.kt`

**Interfaces:**
- Produces: string `path_practice_title` with one argument `%1$s` (level name).

- [ ] **Step 1: Write the failing test** `pathPracticeRowNamesTheGame`: open the learning path in Finnish with full access and assert a node with text "Pelaa: Tasatunnit" exists, and that the lesson row text "Tasatunnit" still exists. Add a free-version case asserting the locked practice row for level 2 has text "Pelaa: Puoli tuntia" and still contains the "Täysversio" supporting text.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Add `path_practice_title`.** Finnish `Pelaa: %1$s`, English `Play: %1$s`. Use each language's existing word for the home tile `menu_pelaa` as the prefix. In `LessonPracticeNode` use it for the visible title and keep `chapterTitle` inside the content description so TalkBack still says the level name.
- [ ] **Step 4: Run the test and `tools/check-localization.ps1`; both pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task C3: Lesson quiz save failure offers a retry button

**Problem.** When saving a finished lesson fails, the quiz shows "Muutosta ei voitu tallentaa. Yritä uudelleen." but the only action is a disabled "Valmis" (GALLERY `lesson-save-failure-p0.png`). The child has to guess that tapping the right answer again retries. Practice and the Minute Challenge show a "Yritä uudelleen" button.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonViewModel.kt` (`pick`, new `retrySave`)
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt` (`LessonNavigation`)
- Test: `app/src/test/java/fi/clocklings/ui/lesson/LessonViewModelTest.kt`

**Interfaces:**
- Produces: `fun retrySave()` on `LessonViewModel`. No-op unless `state.isQuiz && state.saveFailed && !state.savePending`.

- [ ] **Step 1: Write the failing tests** using the failing-repository pattern from `PersistenceFailureTest`:
  - `retrySaveAfterFailureMarksLessonDone`: pick the correct choice with a repository that throws `IOException` once; assert `saveFailed == true`; call `retrySave()`; assert `quizDone == true`, `saveFailed == false` and the stored progress contains the lesson id.
  - `retrySaveIgnoresRepeatedTapsWhilePending`: call `retrySave()` twice before the save completes; assert exactly one repository write and exactly one "correct" analytics count for the lesson.
- [ ] **Step 2: Run them; they fail** because `retrySave` does not exist.
- [ ] **Step 3: Implement `retrySave()`** by re-running the correct-choice branch of `pick` for the index whose choice is correct. Extract that branch into a private function shared by both callers instead of duplicating it.
- [ ] **Step 4: In `LessonNavigation`,** when `state.isQuiz && state.saveFailed`, label the next button `R.string.settings_retry`, enable it, and route it to `retrySave`. Pass the callback through the existing screen parameters the same way `onNext` is passed.
- [ ] **Step 5: Run `LessonViewModelTest` and `PersistenceFailureTest`; all pass.**
- [ ] **Step 6: Commit** (only after the owner asks).

### Task C4: Lesson quiz action says where it leads

**Problem.** After the quiz the button always says "Valmis" (`LessonScreens.kt:303`, `if (state.isQuiz) R.string.done`), even when `nextAction` is `START_GAME` or `NEXT_LESSON` and the tap starts a game or the next lesson (GALLERY `lesson-before-game-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt`
- Test: `app/src/test/java/fi/clocklings/ui/lesson/LessonViewModelTest.kt` or the existing lesson UI test that renders the quiz (find with `git grep -n "LessonQuiz\|lesson_try" app/src/test`)

- [ ] **Step 1: Write the failing test** `finishedQuizActionNamesItsDestination`: complete the last lesson queued before a level and assert the action text is the `lesson_start_game` string; complete a lesson with another lesson queued and assert `lesson_next_lesson`; complete a lesson opened from the path alone and assert `done`. Before the answer the disabled button still reads `done`.
- [ ] **Step 2: Run it; the first two cases fail.**
- [ ] **Step 3: Use `nextLabel(state.nextAction)` when `state.isQuiz && state.quizDone`,** `R.string.done` while the quiz is unanswered, and C3's retry label on save failure.
- [ ] **Step 4: Run the test and `NavigationFlowTest`; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task C5: SET tasks check with "Tarkista", not "Valmis"

**Problem.** In practice SET tasks the primary button "Valmis" checks the answer, also after a hint. In lessons "Valmis" ends the lesson. The same word does two jobs.

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/GameScreen.kt:177`
- Modify: `app/src/main/res/values*/strings.xml` (seven files)
- Test: `app/src/test/java/fi/clocklings/ui/ScreenInteractionTest.kt`

**Interfaces:**
- Produces: string `check_answer`.

- [ ] **Step 1: Write the failing test** `setTaskCheckButtonSaysCheck`: render a SET task in Finnish and assert a button with text "Tarkista" exists and no button with text "Valmis" exists.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Add `check_answer`** (Finnish "Tarkista", English "Check", Swedish "Kolla", German "Prüfen", French "Vérifier", Spanish "Comprobar", Portuguese "Conferir"). Use it in `GameScreen` when `state.showDone`. Keep the confetti anchor on that button. `R.string.done` stays in lessons and elsewhere; grep its other callers and leave them.
- [ ] **Step 4: Run the test, `GameViewModelTest` and `tools/check-localization.ps1`; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task C6: One button order on result screens

**Problem.** The level result puts the primary "Seuraava taso" above the secondary "Pelaa uudelleen" (`ResultScreen.kt:156-170`). The Minute Challenge result and lessons use `ActionPair`, where the primary action is last, so stacked it sits at the bottom (GALLERY `result-3-stars-p0.png` against `challenge-result-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt`
- Test: `app/src/test/java/fi/clocklings/ui/ReviewRegressionTest.kt`

- [ ] **Step 1: Write the failing test** `resultPrimaryActionIsLast`: render a level result with a next level and assert the top of the `next_level` button is below the bottom of the `play_again` button.
- [ ] **Step 2: Run it; it fails.**
- [ ] **Step 3: Swap the two buttons** so "Pelaa uudelleen" (secondary) comes first and "Seuraava taso" (primary) last. Do not change their kinds or the final-level layout with a single "Pelaa uudelleen".
- [ ] **Step 4: Run the test and `VisibleUnlocksTest`; all pass.**
- [ ] **Step 5: Commit** (only after the owner asks).

### Task C7: Copy a child can read

All seven languages unless marked Finnish only. Exact Finnish text is fixed here; other languages carry the same meaning in their own words.

**Files:**
- Modify: `app/src/main/res/values*/strings.xml`
- Modify: callers named below
- Test: `tools/check-localization.ps1`, plus the screens' existing tests

- [ ] **Step 1: Progress save error.** Add `progress_save_error`, Finnish "Tulosta ei voitu tallentaa. Yritä uudelleen." Use it in `GameScreen.kt:250`, `LessonScreens.kt:221` and `ChallengeResultScreen.kt:165`. `settings_save_error` stays for settings, appearance and wardrobe.
- [ ] **Step 2: Level result body, Finnish only.** `result_text`: "%1$s tehty! Ansaitsit %2$d tähden." and "%1$s tehty! Ansaitsit %2$d tähteä." This replaces "Valmista:", which reads as the verb "prepare". The English "%1$s complete!" already reads correctly.
- [ ] **Step 3: Lesson wording, Finnish only (same meaning):**
  - `l_tasan_p2a`: "Katso lyhyttä viisaria. Se näyttää tunnin."
  - `l_puoli_p2a`: "Lyhyt viisari on ohittanut numeron 3, mutta ei ole vielä numeron 4 kohdalla."
  - `l_minuutit_p1b`: "Kahden numeron välillä on 5 minuuttia."
  - `l_minuutit_p2a`: "Etsi numero juuri ennen pitkää viisaria. Laske siitä pienet merkit eteenpäin."
- [ ] **Step 4: `l_ylivaille_p3b`, all seven.** Keep only the first sentence (Finnish "Kun sanot kymmentä vaille, nimeä seuraava tunti."). The removed sentence about counting near the half hour is not shown by that page's demo (3.10, 3.50, 3.05, 3.55).
- [ ] **Step 5: Minute Challenge intro, all seven.** `challenge_intro`, Finnish: "Vastaa niin moneen kuin ehdit. Väärän vastauksen jälkeen peli jatkuu." The heading already says "minuutissa".
- [ ] **Step 6: Home tile subtitle, Finnish only.** `menu_pelaa_sub`: "Lue ja aseta kelloja". The current text auto-shrinks below the other tiles' size (GALLERY `main-home-p0.png`).
- [ ] **Step 7: Play-together reveal text.** Add `ask_revealed_text`, Finnish "Arvasitteko oikein? Vaihtakaa sitten vuoroa." Show it in `AskScreen` in the `AskPhase.REVEALED` phase instead of `ask_small_text`. Add `askRevealShowsFollowUpText` to `AskScreenTest` asserting the new text after reveal and the old text before it.
- [ ] **Step 8: Run `tools/check-localization.ps1`, `AskScreenTest`, `PersistenceFailureTest` and `ExpandedLocaleContentTest`; all pass.** Confirm with `git diff --stat res/raw*` that no recording changed.
- [ ] **Step 9: Commit** (only after the owner asks).

### Task C8: Moon icon reads as a moon

**Problem.** In the day-part chip the crescent is so thin that at chip size it reads as a "(" bracket (GALLERY `lesson-09-vrk-page-1-p0.png`, `lesson-09-vrk-quiz-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/clock/DayPartChip.kt:34`

- [ ] **Step 1: Thicken the crescent.** Change the inner arc of `MoonPath` from `A7 12` to `A3 12` so the widest part is 9 of 24 units (the outer arc and colour stay):

```kotlin
private val MoonPath = PathParser().parsePathString("M3 -12A12 12 0 0 0 3 12A3 12 0 0 1 3 -12Z").toPath()
```

- [ ] **Step 2: Run `ClockDayPartLayoutTest` and `ClockFaceHaloRenderTest`; both pass.** Visual acceptance is on the emulator (see Validation).
- [ ] **Step 3: Commit** (only after the owner asks).

### Task C9: Disabled primary button is readable

**Problem.** The disabled "Valmis" in lesson quizzes is white text on pale lavender, too faint for a child to read (GALLERY `lesson-01-viisarit-quiz-p0.png`).

**Files:**
- Modify: `app/src/main/java/fi/clocklings/ui/theme/SkColors.kt`, `app/src/main/java/fi/clocklings/ui/components/Buttons.kt`
- Test: `app/src/test/java/fi/clocklings/ui/components/SurfaceAccessibilityTest.kt`

**Interfaces:**
- Produces: token `SkColors.PrimaryDisabledContent`.

- [ ] **Step 1: Write the failing test** `disabledPrimaryLabelHasReadableContrast`: assert the sRGB contrast ratio of `SkColors.PrimaryDisabledContent` on the disabled primary fill used in `Buttons.kt` is at least 4.5. Reuse the contrast helper the F4 star-pill check used (find with `git grep -n "contrast" app/src/test`).
- [ ] **Step 2: Run it; it fails** because the token does not exist.
- [ ] **Step 3: Add the token** (a dark ink from the existing palette that reaches 4.5:1) and use it as the disabled content colour of `ButtonKind.PRIMARY` only.
- [ ] **Step 4: Run the test; it passes.**
- [ ] **Step 5: Commit** (only after the owner asks).

---

## Order of work

C1 first (it changes layout that later captures depend on). C2 to C9 are independent of each other, except that C4 builds on C3's retry label in `LessonNavigation`: do C3 before C4. Owner decisions D5 to D10 come after the owner answers, each as its own task with tests written the same way.

## Validation and acceptance

- [ ] `.\gradlew.bat --no-daemon :app:testDebugUnitTest` passes with zero failures. Record the test count against the 534 in `STATUS.md`.
- [ ] `.\gradlew.bat --no-daemon :app:assembleDebug` passes.
- [ ] `tools/check-localization.ps1` reports seven-language parity.
- [ ] On the dedicated `ClocklingsReview20261030` emulator, capture in Finnish and English at default width, 320 dp, 200 % font and 320 dp with 200 %: practice READ level 5 in 24-hour mode before answering (C1), learning path free and full (C2), lesson save failure (C3), last lesson before a game after the quiz (C4), a SET task (C5), level result with three rewards (C6), Minute Challenge intro and play-together reveal (C7), lesson 9 page 1 (C8), an unanswered lesson quiz (C9).
- [ ] Capture lesson pages **after** the demo's final step, not at its start. The 22 November gallery captured start states, so its lesson clocks disagree with the text. The tool that produced that gallery is not in a tracked test; record in the report which capture method was used.
- [ ] Restore emulator settings and stop every process started for the task.
- [ ] Update `docs/implementation/STATUS.md` with a dated entry. The owner runs `lc` and `sc`.

## Final report (Finnish)

- Toteutetut tehtävät C1–C9 ja mitä kukin muutti.
- Ajetut tarkistukset ja niiden tulokset, laitteella nähty näyttö erikseen lähdekoodin tarkastelusta.
- Omistajan päätökset D5–D10 ja aiemmat avoimet T8, T19, T30.
- Mittaukset, joita ei saatu tai jotka jäivät hyväksymisrajan alle.
