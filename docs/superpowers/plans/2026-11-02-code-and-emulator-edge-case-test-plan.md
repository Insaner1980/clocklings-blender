# Clocklings Code and Emulator Edge-Case Test Plan

**Date:** 2 November 2026.  
**Workspace:** `C:\Dev\Clocklings` only.  
**Planning baseline:** branch `codex/localization-settings`, HEAD `885d97a107a182a40ca8c68efdac1690ea11dbe5`. `PROJECT.md` was already modified and must be preserved. Its embedded HEAD is older than the inspected checkout.  
**Status:** Planning only. No application tests, builds, emulator sessions, or application changes were performed to write this plan. Secret pre-read scans and read-only tool discovery are not application tests.  
**Goal:** Investigate and test the implemented application's realistic boundary conditions using source inspection, deterministic Kotlin tests, and dedicated Android emulators, with reproducible evidence and explicit limits.  
**Architecture:** Reuse the existing domain tests, coroutine test scheduler, Robolectric/Compose tests, injectable `AppServices`, Preferences DataStore boundary, and instrumentation suites. Add local Maestro flows for externally observable user journeys where they provide additional evidence.  
**Tech stack observed:** Kotlin 2.4.21, Compose BOM 2026.09.00, Navigation 3 1.2.0, Preferences DataStore 1.3.0, JUnit 4, Robolectric 4.17, AndroidJUnitRunner, AGP 9.5.0, Gradle 9.8.2; minSdk 26, targetSdk/compileSdk 37, Java bytecode target 17. Preserve these versions.  
**Authority:** Current owner instructions and `AGENTS.md`, followed by current source and the decisions in `docs/implementation/STATUS.md`. The product proposal `docs/superpowers/plans/2026-10-20-public-clock-learning-app.md` is background, not approval to implement its optional features.

> **For the next chat:** Read this plan and the current project instructions before execution. This document does not itself authorize starting tests. Once the owner explicitly asks to execute it, proceed through the bounded tasks below. Testing may add focused test code and Maestro flow files; do not fix application code, alter product behavior, add production test hooks, or change dependencies unless separately requested. If using a plan-execution skill, use `superpowers:executing-plans` and retain these limits. No automatic commits, pushes, pull requests, or delegated agents are required.

## 1. Global constraints and exclusions

- Work only in the Clocklings repository and on a `codex/` feature branch. Inspect branch/status before writing. Preserve all pre-existing changes and evidence.
- Before reading any workspace file, run `sonar analyze secrets <path>` and inspect its result. Directory scans may cover a bounded set of source files. If a secret is reported, do not read the affected content; follow the project stop-and-notify rule. A scanner failure is not clearance to read.
- Run Gradle only from this repository, using its wrapper and `--no-daemon`. The existing technical reference calls for JDK 21 for API 37 Robolectric; verify the local test configuration and selected JDK before execution. Do not change the app's bytecode target.
- Use only explicitly identified dedicated test AVDs and synthetic app data. Never install, clear data, change settings, or execute tests on a physical device or unrelated emulator.
- Do not touch `C:\Dev\Sateenkaarikello-Android`, its Git state, signing material, or installed data. No original-app comparison run is needed here.
- The owner runs `lc` and `sc`. Do not run either wrapper, a broad remote scanner, or a Sonar upload as part of this plan. The mandated deterministic pre-read secret check is a separate operation.
- Do not configure production signing, build a release candidate, publish, upload artifacts, or use Maestro Cloud. Use local debug builds for this campaign.
- No backend, accounts, billing, advertising, sync, notifications, timers/widgets, profile switching, daily streaks, or import/export flows were found in the inspected current implementation. Do not invent tests or implement these features from the proposal.
- Backup is disabled in the manifest and extraction rules. Inspect the packaged declarations; a real cloud/device-transfer exercise is outside this plan.
- No physical acoustics, Bluetooth/headset behavior, real vibration quality, OEM hardware behavior, native-speaker acceptance, child usability study, or Play Console validation is included. Emulator TTS and accessibility checks have the narrower limits stated below.
- Do not fill the host disk or shared emulator storage to provoke an error. Use controlled test-store failures and isolated temporary DataStore files instead.
- Apply the minimal sufficient work rule: extend existing meaningful tests; do not duplicate every passing assertion in several frameworks. Stop when each applicable case has evidence or a specific unresolved classification.

## 2. What the inspected implementation makes relevant

Paths below are relative to the repository. Production Kotlin paths start at `app/src/main/java/fi/clocklings/`; JVM tests at `app/src/test/java/fi/clocklings/`; device tests at `app/src/androidTest/java/fi/clocklings/`.

| Area | Current implementation and invariant | Existing evidence starting points; not newly run |
| --- | --- | --- |
| Clock mathematics | `domain/ClockTime.kt`: periods 720/1440 minutes, positive normalization, rounded snapping, shortest circular movement, minute/hour dragging, equivalent hands across day halves | `domain/ClockTimeTest.kt`, `ui/clock/DragTickTest.kt`, `ClockFaceUpdateTest.kt` |
| Task generation | `domain/Levels.kt`: five levels with 60/30/15/5/1-minute steps; normally ten unique alternating READ/SET tasks; three answers; challenge uses only levels 0–3 | `domain/LevelsTest.kt`, `TimeLanguageTest.kt` |
| Practice | `ui/game/GameViewModel.kt`: first mistake hints, second reveals; success advances after 1,500 ms; final result follows persistence; outcomes recorded once | `ui/game/GameViewModelTest.kt`, `ui/PersistenceFailureTest.kt`, `ui/ScreenInteractionTest.kt` |
| Minute challenge | `ui/challenge/ChallengeViewModel.kt`: 60,000 ms run; 380/1,300 ms answer transitions; monotonic deadline display; record save and retry; no stars | `ui/challenge/ChallengeViewModelTest.kt`, `ui/ChallengeInteractionTest.kt`, device `ActiveLocaleStateTest.kt` |
| Lessons | `domain/Lessons.kt`, `ui/lesson/LessonCatalog.kt`, `LessonViewModel.kt`: eight standard lessons and optional `vrk` day lesson; queued demonstrations and quiz persistence | `domain/LessonsTest.kt`, `ui/lesson/*Test.kt`, device `LocalizationContentTest.kt` |
| Storage | `data/ProgressRepository.kt`: transactional preferences writes, best-only scores, IOException read fallback, reset progress while preserving settings | `data/ProgressRepositoryTest.kt`, `PersonalizationTest.kt`, device `PersonalizationStorageTest.kt`, `LocaleReadFailureTest.kt` |
| Retry state | `ui/RetryableSave.kt`, `ui/companion/WardrobeScreen.kt`, `ui/appearance/AppearanceScreen.kt`: request generations; separate name, color, and item-slot retries | `ui/RetryableSaveTest.kt`, `ReviewRegressionTest.kt`, `PersistenceFailureTest.kt`; recent 1 November status entries |
| Language | `ui/locale/AppLocaleController.kt`, `AppLocale.kt`: API 26–32 preference authority, API 33+ platform authority and legacy handoff; EN/FI/shared SV | `ui/locale/AppLocaleControllerTest.kt`, device `LanguageSettingsTest.kt`, `ActiveLocaleStateTest.kt` |
| Navigation/restoration | `ClocklingsApp.kt`, `Screen.kt`, `Navigator.kt`: serializable back stack, saveable UI state, per-entry ViewModels, stale-callback guards | `ui/NavigationFlowTest.kt`, `LocalAppFlowTest.kt`; device `NativeFeelUiTest.kt` |
| Explore/Together | `ui/freeclock/FreeClockScreen.kt`, `ui/ask/AskScreen.kt`: five-minute dragging, random time, SET/ASK/REVEALED phases, answer hidden from semantics, no progress rewards | `ui/LocalAppFlowTest.kt`, device `AppFlowTest.kt`, `ClockSurfaceStateTest.kt` |
| Personalization | `domain/Personalization.kt`: nine companions plus NONE; eight themes; custom clock 4 palettes × 3 frames × 2 numeral styles × 2 hands; independent colors/outfits/names | Domain wardrobe/color/item tests, device `PersonalizationFlowTest.kt`, `CompanionArtCoverageTest.kt` |
| Names | `ui/companion/NameField.kt` limits input with `take(16)`; `WardrobeScreen.kt` keeps a draft and trims the saved value | `ui/ReviewRegressionTest.kt`; Unicode and interruption boundaries need specific mapping |
| Audio | `audio/Speech.kt`: asynchronous offline voice selection and request invalidation; `MainActivity.onPause()` stops speech/SFX | `audio/SpeechTest.kt`, `SfxTest.kt`, `SoundTest.kt`, `ToneSynthTest.kt`; device `audio/OfflineVoiceTest.kt` checks metadata, not actual speech |
| Layout/motion | Shared scaffold, pinned actions, clock fitting, answer-height reservation, reduced-motion behavior, introduction overlay | `ScreenshotReviewUiTest.kt`, `ui/challenge/RunOptionReservationTest.kt`, `AnswerRoomCostTest.kt`, device screenshot and intro suites |
| Home time | `ui/home/HomeScreen.kt`: time polling while STARTED, immediate refresh on return; local time used for display | `ui/home/HomeTimeTest.kt`, `HomeStageMathTest.kt` |

Do not use old test totals as an expected current pass count. The status file contains dated runs and later scoped corrections; some later corrections were only tested with selected Robolectric suites. Enumerate current tests at execution time. Gallery captures and skipped tests are not passing functional tests.

## 3. Review focus: first investigations

These are hypotheses to test, not confirmed bugs.

1. **Challenge deadline versus delayed timer dispatch:** `pick()` checks phase/busy, while the displayed deadline uses `nowNanos()`. Explore a late answer when monotonic time has crossed the deadline but the timer coroutine has not executed. See CH-03.
2. **Delayed initial challenge-best load:** initialization reads progress asynchronously. Explore answering/ending while that first emission is delayed, with an existing higher best. See CH-05.
3. **Retry continuity across recreation:** retry closures are remembered in composition, while the name draft is saveable. Explore failed writes followed by activity/locale recreation and screen re-entry. See ST-07 and LC-04. Do not promise durable draft recovery without an approved contract.
4. **Gesture cancellation and changing interactivity:** the clock drag loop checks interactivity at gesture start. Explore cancellation, phase changes, and incoming clock commands during a gesture. See CK-06.
5. **Saved navigation versus non-durable ViewModels:** restored routes do not prove exact in-progress game/challenge recovery after process death. Test durable-data integrity separately and characterize session recovery. See LC-05.

## 4. Test layers and Maestro's role

| Label | Method | Suitable proof | Limit |
| --- | --- | --- | --- |
| S | Static source/resource/package inspection | Wiring, supported domains, permissions, missing test mapping | Does not prove execution |
| J | JUnit/domain/coroutine tests | Exhaustive small input domains, exact timing and race ordering, injected I/O errors | Fakes do not prove Android disk or lifecycle behavior |
| R | Robolectric/Compose tests | Semantics, layout assertions, callbacks and saved-state behavior on host | Does not establish emulator/device rendering or process death |
| E | Android instrumentation on a dedicated AVD | Android resources, real DataStore, activity recreation, Compose input and geometry | Does not prove physical hardware behavior |
| M | Local Maestro on the dedicated AVD | Installed-app navigation, settings, persistence relaunches, scrolling, language journeys | Cannot directly inject repository errors or prove internal state/timing |
| A | Explicitly targeted ADB/emulator orchestration | Actual background process kill, configuration changes, install/relaunch, logs | Must validate the target and distinguish each lifecycle operation |

### 4.1 Maestro installation observed during planning

- `Get-Command maestro` resolved to `C:\maestro\bin\maestro.bat`.
- `maestro --version` returned **2.12.0**. `--help` and `test --help` were read; no flow, hierarchy query, device connection, or test was started.
- Local help supports explicit `--device`, `test`, `hierarchy`, `check-syntax`, JUnit output, `--test-output-dir`, and `--debug-output`. Verify help again only if the installation changes.
- Source search found Compose `testTag` usages but no `testTagsAsResourceId` enabling statement. Do not assume those tags are selectable by Maestro as IDs.

Maestro can use the rendered text and accessibility metadata of Compose screens. First inspect the actual external hierarchy on the authorized test emulator, then choose unique selectors from the current English/Finnish/Swedish resources. Use per-language selectors and explicit screen assertions; avoid coordinates for ordinary buttons. Existing Compose tests remain the better tool for exact clock gestures and internal semantics. [Maestro Compose documentation](https://docs.maestro.dev/get-started/supported-platform/android/jetpack)

Compose tags require explicit exposure to become external resource IDs. Do not change production semantics merely to enable a Maestro flow; use current descriptors or record the blocked selector. Such a source change needs separate authorization. [Android Compose interoperability](https://developer.android.com/develop/ui/compose/testing/interoperability)

Maestro's default `launchApp` stops/restarts the app. Use `stopApp: false` for a foreground-return case. Use `clearState: false` for persistence tests; a clean-data launch belongs only to a named fresh-install fixture. Neither a normal restart nor `clearState` proves OS process-death restoration. [Maestro launchApp reference](https://docs.maestro.dev/reference/commands-available/launchapp)

### 4.2 Minimal proposed Maestro files

Create these only during authorized execution, and only where they supply evidence not already adequately obtained:

| Proposed file | Journey and case mapping |
| --- | --- |
| `.maestro/edge-cases/navigation.yaml` | Home → each main destination → Back; Settings → Language → Back twice; repeated entry/return; NV-01/02 |
| `.maestro/edge-cases/settings-persistence.yaml` | Set sound/help/12–24 h/language; restart without clearing; verify; cancel reset; confirm reset only with owned synthetic data; ST-04/05, LO-03 |
| `.maestro/edge-cases/together.yaml` | Set/randomize → ask → verify concealed readout → reveal → swap, including scroll reset; TG-02/03 |
| `.maestro/edge-cases/personalization.yaml` | Companion/color/name/theme selection, switching away/back, keyboard dismissal, retained selection after restart; PS-01/03/05 |
| `.maestro/edge-cases/learning-navigation.yaml` | Enter/skip automatic lesson, complete a known authored lesson quiz, reach practice, exit/back; LS-01/04, NV-03 |

Derive exact UI labels from scanned resource files at execution time. Do not guess names, test tags, or answer locations. Random practice answers require a deliberate oracle; do not automate guessing by reading a hidden correct-answer semantic or hard-code a random option index. Full scored rounds are already better suited to seeded Kotlin/instrumentation tests.

Keep shared flow setup minimal. Each flow documents starting locale/data, whether it resets anything, expected final state, and which display cells it supports. Prefer bounded waits for a specific element/state. Do not mask intermittent failures with unlimited retries or use long fixed sleeps for every animation. Validate syntax before running newly authored flows. No Studio/MCP server or continuous watcher is needed for CLI execution.

## 5. Bounded emulator and configuration matrix

### 5.1 Device selection

1. Inventory installed AVDs and connected devices without modifying them. Identify each target by AVD name, serial, API, and emulator ownership.
2. Historical names such as `ClocklingsFoundation20261020`, `ClocklingsLocaleApi26`, `ClocklingsLocaleApi33`, and `ClocklingsReview20261030` are discovery hints, not verified current targets. Do not assume their old serials, API levels, availability, or ownership.
3. Prefer one dedicated emulator at a time to limit memory pressure. A missing API image is a specific blocked cell; do not silently substitute another API or upgrade the project.
4. Record the initial display size/density, font scale, locale, navigation mode and animation scales before changing them. Restore them in a finally/teardown step. Record task-started PIDs/handles and stop only owned processes.

### 5.2 Display cells

Widths below are effective Android dp widths, not raw pixels. Configure and verify `screenWidthDp`, font scale, and usable viewport; a cell name alone is not evidence. Record actual height and system insets.

| Cell | Width and font | Required content |
| --- | --- | --- |
| D0 | 360 × 640 dp, 100% | Standard full journeys; all three languages; challenge headline/score and bottom actions |
| D1 | 320 dp width, 100% | Reachability and word/answer layout on all main screens |
| D2 | 360 dp width, 200% | Large-font layout independent of narrow-width effects |
| D3 | 320 dp width, 200% | Combined worst required cell; all main screens and error/reset/keyboard states |
| D4 | About 411 dp width, 100% | Companion picker and multi-column wardrobe boundaries; normal Home layout |
| D5 | 320 × 420 dp, 200%, R initially | Known constrained-height score/confetti fallback; add emulator evidence only if R cannot resolve the issue |

- **API 37:** main functional emulator campaign, D0–D3 in EN/FI/SV; D4 for grid/pager coverage. Run 12/24-hour variants where the clock/readout changes.
- **API 26:** legacy locale/storage/lifecycle and core learning smoke at D0; Settings/reset/keyboard/retry and longest time labels at D3 in EN/FI/SV.
- **API 33:** platform locale authority/handoff/external changes, recreation, settings and core launch/learning smoke at D0. Add D3 only for an API-specific layout finding.
- Test normal and disabled animations on API 37. Test gesture Back and three-button Back where the image supports them. No exhaustive cross-product of all themes, APIs, languages and fonts is required: use full finite domain coverage in J, then targeted UI representatives.
- The manifest requests portrait. Verify behavior when the system changes configuration; do not invent a requirement for a new landscape design. If the AVD permits a resized/multi-window layout despite that request, test no-crash/reachable-actions behavior and record the actual viewport.

## 6. Detailed case catalog

Each ID is a reportable case group. Record its subcases/parameters; passing one subcase does not pass the entire row. **P0** means prioritize because of data integrity, scoring, or blocked learning; **P1** means required functional boundary; **P2** means bounded robustness/compatibility. These are execution priorities, not findings' severities.

Expected outcomes below distinguish established behavior from a candidate invariant. Where a stronger product promise is unresolved, collect observations and report a decision gap instead of silently implementing it.

### 6.1 Clock mathematics, commands and gestures

Owners: `domain/ClockTime.kt`, `ui/clock/ClockState.kt`, `ClockFace.kt`, `DragTick.kt`. Reuse `ClockTimeTest`, `DragTickTest`, `ClockFaceUpdateTest`, `RingRevealTest`, `NumeralCoverTest`, device `LintRegressionTest` and `ClockSurfaceStateTest`.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| CK-01 P1 | For periods 720 and 1440, normalize -1441, -1440, -721, -720, -1, 0, 1, 719, 720, 721, 1439, 1440, 1441; enumerate all valid minutes for readings/angles | Value stays within period; 12 h zero reads hour 12, 24 h zero hour 0; minute 0–59; hour angle includes minute progress | J |
| CK-02 P1 | Each step 1/5/15/30/60: test rounding immediately below, at, above half-step and period end, including negative raw times | Matches documented rounding rule and wraps; no out-of-period result; no step drift | J |
| CK-03 P1 | Circular movements 719↔0, 1439↔0 and exact half-turn; drag minute hand both directions over twelve; choose 24 h hour-hand candidate at tie/near-tie | Shortest route, documented forward tie; correct hour/day-half transition; no skipped hour on ordinary continuous drag | J + E representative |
| CK-04 P1 | 03:30 and 15:30; 00:00 and 12:00; compare unequal minute positions | `sameHands` accepts equal analog positions; READ day-context remains distinguishable in 24 h mode | J, linked GM-03/LO-02 |
| CK-05 P1 | Touch center; just below/at/above center exclusion radius 16, hour selection radius 112, angular selection boundary 40 degrees in model coordinates; overlapping hands at twelve | Deterministic hand selection matching source; center does not begin a drag; usable gestures at rendered sizes | J + E |
| CK-06 P0 | Start drag then cancel touch, add/remove second pointer, release outside clock, navigate away, or switch interactive to false; send a new command while dragging | No crash/stuck interaction; visible/readout time agrees after completion; hidden/noninteractive phase cannot keep accepting input. Characterize any mismatch before naming a defect | R/E |
| CK-07 P1 | Send A→B→C animated commands before earlier settle; send same target with different command IDs; change period mid-animation; reduced motion on/off | Latest command wins; superseded animation does not call stale settle action; final raw/value agree; no duplicate reward/sound triggered by settle | J/R |
| CK-08 P1 | Use accessible increment/decrement at 0 and period−step; execute in both modes and with help off | Wraps one configured step, readout and semantics update; actions absent when noninteractive; no dependence on dragging | R/E |

Only feed invalid period/step/NaN inputs if a real call path can produce them. Do not demand a general-purpose arithmetic API redesign for unreachable values.

### 6.2 Task generation, language-independent scoring and unlocks

Owners: `domain/Levels.kt`, `Progress.kt`, `Wardrobe.kt`, `CompanionItems.kt`. Reuse existing domain suites and `ui/game/VisibleUnlocksTest.kt`.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| DG-01 P0 | All five levels × both modes × seeds 0–99; inspect every generated task | Ten unique in-range times; READ/SET alternates starting READ; minutes fit level step; SET start differs in hand position from target | J |
| DG-02 P0 | Every valid step-aligned target for each level and period, seeds 0/1/17; build options | Exactly three unique numeric values, correct one exactly once, valid range and step; distinct displayed answer labels in each language | J |
| DG-03 P1 | Repeated seeded random Explore and challenge generation; boundary current values 0, 5, period−5; challenge previous value | New time differs from previous; Explore uses 5-minute increments; challenge uses first four levels only | J |
| DG-04 P2 | Inspect 500-draw/300-draw guards and retry-based generators; use a finite scripted repeated RNG sequence that eventually yields new values | Determine guard/fallback behavior and whether a shorter list is accepted by consumers. Report reachability; do not run an infinite constant-RNG test in-process or classify artificial RNG failure as ordinary user reproduction | S/J bounded |
| DG-05 P0 | Mistake counts 0,1,2,3,4,20; existing best 0/1/2/3, replay with lower/equal/higher stars | Stars 3/3/2/2/1/1; best never decreases; total is sum of five bests, max 15 for valid persisted progress | J |
| DG-06 P0 | Each wardrobe item's threshold T: total T−1/T/T+1 within valid bounds; totals 0 and 15; replay already-best level | Unlock exactly at threshold; stars never spent; new-unlock list only crosses new thresholds and matches chosen companion | J + E one threshold |

### 6.3 Practice round state machine

Owners: `ui/game/GameViewModel.kt`, `GameScreen.kt`, `ResultScreen.kt`. Existing `GameViewModelTest`, `PersistenceFailureTest`, `ScreenInteractionTest`, `ActiveLocaleStateTest` are the starting coverage.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| GM-01 P0 | Correct READ answer twice before scheduler advances; press again during 1,499 ms pause; advance to 1,500 ms and drain scheduled work | One success, one celebration, one advance, one outcome; no skipped task | J |
| GM-02 P0 | Wrong READ option twice, then another wrong; repeat Next; press correct answer after reveal | Repeated disabled wrong option adds no mistake; first error hints, second reveals; Next advances once; busy/revealed answers cannot rescore | J/R |
| GM-03 P0 | SET: wrong minutes, right minutes/wrong hour, correct hands, other 24 h half; repeated Done | Hint targets correct component; equivalent hands accepted; first/second error rules match READ; controls disable after success/reveal | J/E |
| GM-04 P0 | Complete ten tasks with all-first-try, one corrected error, and two-error reveals; inspect after each task and final result | One outcome per completed task, correct dot type, no premature result, correct aggregate mistakes/stars | J |
| GM-05 P0 | Hold final DataStore write; double-tap/recreate while pending; fail, retry twice, then succeed | No result or durable reward before commit; one final outcome; retry keeps round and no duplicate progress; terminal result corresponds to saved data | J + R/E |
| GM-06 P1 | Leave during success delay, second-error reveal, and final-save wait; open a new round of same level | Old job cannot navigate new route or consume a new round; fresh round has new identity and clean transient state. Committed writes remain valid if commit already occurred | J/R/E |
| GM-07 P1 | On Result replay, next level, wardrobe, and Back, including last level and NONE companion | Correct destination, no invalid next-level index, no duplicate result route or reward; result preview does not equip items | R/E |

### 6.4 Minute challenge

Owners: `ui/challenge/ChallengeViewModel.kt`, `ChallengeScreen.kt`, `AnswerRoom.kt`. Reuse `ChallengeViewModelTest`, `ChallengeInteractionTest`, `AnswerRoomCostTest`, `RunOptionReservationTest`, `PersistenceFailureTest`, device `ChallengeScreenTest` and `ActiveLocaleStateTest`.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| CH-01 P0 | Start, immediately repeat start through UI; answer correct/wrong twice; advance 379/380 ms or 1,299/1,300 ms | One visible run start per action; busy answer ignored; correct +1, wrong +0; exactly one next question after appropriate delay | J/R |
| CH-02 P0 | Observe at 0, 59,999, 60,000, 60,001 ms with clock tied to test scheduler; answer just before expiry and pending next-question callback | Remaining fraction in [0,1]; ends once, pending question canceled, no post-end score; no stars/lesson/wardrobe mutation | J |
| CH-03 P0 | Advance injected monotonic clock beyond deadline without dispatching timer coroutine; submit answer; then dispatch timer. Also reverse callback order at the exact boundary | Candidate invariant: answers after displayed deadline must not gain extra time/score. Record both event orders and any mismatch between visible expiry and accepted answer; do not hide it with sleeps | J; E corroboration if reproducible |
| CH-04 P0 | Background/lock for less than remaining duration, then beyond deadline; change locale/recreate at ~20 s | Activity recreation does not restart deadline; foreground result/countdown agrees with elapsed run; no extra minute from return. Process death separately LC-05 | E/A |
| CH-05 P0 | Seed previous best 10; delay first repository emission; start and finish with 0/1/10/11; release load before/after answer/end | Saved best never decreases; displayed previous/new record is truthful; lower score not announced as a new saved record | J |
| CH-06 P0 | New-record write fails; double Retry; attempt Play again while pending; success and failure on retry; best-equal run | Failure does not claim saved record; retry preserves score and serializes save; replay cannot race pending save; equal best is not a new record | J/R/E synthetic |
| CH-07 P1 | Retry/replay after END, leave during 380/1,300 ms delay, reopen Challenge; repeat 3 complete runs | Fresh score/deadline per new run; old timer cannot end new run; old question job cannot update a departed screen | J/E |
| CH-08 P1 | Longest answers across languages/12–24 h, bold text, D0–D3, score scrolled/clipped at END, normal/reduced motion | Answer room fits; RUN→END clock size stays stable; D0 headline/score visible without scroll, D3 actions reachable and content scrollable; confetti anchored or safe fallback | R/E |

### 6.5 Lessons and automatic introductions

Owners: `domain/Lessons.kt`, `ui/lesson/*`, `AppViewModel.startLevel`, `Navigator.startLevel`. Reuse lesson/catalog tests and device English/localization content tests.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| LS-01 P1 | Each level, no lessons done / some / all; intro shown/not shown; 24 h on/off and `vrk` done/not done | Queue matches level mapping and existing once-shown rule; optional day lesson appears only as defined; no duplicated IDs | J |
| LS-02 P0 | Every one of nine quizzes in EN/FI/SV; independently specify expected numeric answer and reference-hour phrase | Exactly one correct choice; word/clock/numeric answer agree; shuffled order does not change correctness | J/E |
| LS-03 P1 | Replay repeatedly; Next/Previous during each demo move; change language during demo; leave screen | Canceled demo cannot overwrite new page; replay restores start; overlays/captions reset correctly; old speech stops | J/R/E |
| LS-04 P0 | Wrong option twice, then correct twice; Next before answer, during pending save, after success; traverse queued last lesson into game | Wrong disabled choice counted once; cannot leave unfinished/pending quiz via Next; completion stored once; correct terminal route | J/R/E/M authored quiz |
| LS-05 P0 | Correct-answer write fails; choose a wrong answer then retry correct; fail/succeed; background/recreate at pending point | Quiz remains retryable; hint outranks stale error; no completion before saved; no stale save callback applies to unrelated lesson state | J/R/E |
| LS-06 P1 | Start-level intro marker write fails; skip intro; exit before quiz; re-enter; reset progress then re-enter | Observe current marker semantics and error visibility; progress remains coherent. Do not change when intros are marked shown without a product decision | J/E |
| LS-07 P1 | Learning path in 12/24 h, completed/incomplete state, all three languages | Eight standard lessons grouped under five practice levels; day lesson optional; current practice labels and accessible level identity preserved | R/E |

### 6.6 Storage, settings, resets and retry isolation

Owners: `data/*`, `ui/AppViewModel.kt`, `RetryableSave.kt`, Settings/Appearance/Wardrobe screens. Reuse repository, settings, persistence and recent review tests before extending them.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| ST-01 P0 | Empty store; missing keys; stars −1/0/3/4; negative best; unknown theme/companion/color/outfit IDs; item belonging to another companion/slot | Defaults/clamps match current parser, other valid fields preserved, no cross-companion item; invalid offered color falls back to Original | J |
| ST-02 P1 | Earlier animal accessory flag with no outfit keys, then explicit empty/saved outfit; human required/optional invalid items; old dragon purple/dinosaur green | Legacy fallback only when intended; explicit empty outfit wins; required human defaults retained, optional invalid removed | J |
| ST-03 P0 | Read IOException then fresh collector/reopen; non-I/O error; malformed preferences file in a test-owned store; correctly typed versus wrong-typed keys | Distinguish parser sanitization, I/O fallback and corruption handling. No false claim of successful persistence. Non-I/O propagation is currently intentional; characterize UI failure separately | J + E isolated file |
| ST-04 P0 | Toggle sound/help/mode, hold write, repeat input; fail and retry; leave/reopen Settings; unrelated write succeeds/fails | Committed settings remain on failure; pending actions cannot duplicate; errors/retries correspond to their operation; re-entry has no stale unrelated error | J/R/E/M |
| ST-05 P0 | Seed stars, best, lessons, intros, every companion name/outfit, colors/theme/mode/audio/help/language. Cancel reset; dismiss/back; confirm twice; fail/retry | Cancel changes nothing. Successful reset clears progress/names/outfits and retains settings/colors/theme/language. Failed reset keeps prior data and offers recovery; one successful action | J/E/M |
| ST-06 P0 | Fail name, color, head and neck/other independent slots; allow unrelated name/appearance success; retry each; replace failed request within same slot; release old callbacks in both orders | All independent failed operations remain retryable; new same-operation choice supersedes old failure; old completion cannot restore stale retry; correct requested values committed | J/R; E visible bar |
| ST-07 P0 | Failed name/color/item/theme then locale change, activity recreation, navigate away/back, or process relaunch; compare draft, saved values and retry visibility | No falsely displayed saved value; record whether retry/draft survives each boundary. Persistent unsaved-draft recovery is a decision gap unless already specified; data loss of committed values is a defect | R/E/A |
| ST-08 P0 | Suspend one write, issue disjoint name/settings/progress transforms, release in controlled order; reset interleaved with pending writes where user-reachable | No lost unrelated fields; final state follows serialized accepted operations. A pre-reset pending update must not silently resurrect cleared progress without being reported/investigated | J |
| ST-09 P0 | Cancel test-owned coroutine during write; cancel before/after commit; repeat for game, lesson, settings | Cancellation is not presented as ordinary IOException; no success before commit; new screen not permanently stuck on stale pending state | J/R |
| ST-10 P0 | Close/cancel the scope owning real temporary DataStore, reopen same file; app restart after committed settings and progress | Exact committed data survives; no simultaneous active stores for same file; clean teardown; real Android disk proof distinct from in-memory tests | E/M |
| ST-11 P2 | Install the same freshly built debug APK over itself without data clear; optional older synthetic fixture only if a verified compatible artifact already exists | Same-build reinstall preserves committed data. Label it reinstall, not genuine old-version upgrade; no download/build of historical versions required | A/E |

### 6.7 Localization and regional boundaries

Owners: `domain/AppLanguage.kt`, `TimeLanguage.kt`, `EnglishTime.kt`, `FinnishTime.kt`, `SwedishTime.kt`, `ui/locale/*`, resource directories. Reuse existing exhaustive language suites; inspect assertions rather than adding duplicates.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| LO-01 P0 | All 720 analog minutes and 1,440 full-day values, each language; explicit minutes 0/1/2/14/15/16/20/21/29/30/31/39/40/44/45/46/58/59; noon/midnight/hour wrap | Nonempty distinct appropriate labels, correct reference hour, day context only where known; match independently authored fixtures, not the same formatter twice | J |
| LO-02 P0 | Each generated answer tuple in DG-02 in EN/FI/SV, both modes; 03:30 vs 15:30 | No indistinguishable options; numeric/phrase/day-part agreement; Finnish/Swedish half-hour reference and English past/to rules remain distinct | J/E representative |
| LO-03 P1 | Device-language mode, explicit EN/FI/SV, unsupported primary locale with supported secondary, unsupported-only fallback; sv-SE/sv-FI, en-US/en-GB | Correct first supported/fallback language; one Swedish content variant; selected mode persists across relaunch without changing progress | J/E/M |
| LO-04 P0 | API 26 legacy language write/read failure; API 33+ empty/existing platform choice with legacy preference; cleanup failure then external platform selection | Platform authority wins where applicable; handoff not repeatedly overwriting external choice; failures recover without resetting learning data | J/E |
| LO-05 P0 | Switch language during wrong-answer feedback, unfinished lesson quiz, saved Result, running Challenge, hidden Together, reset dialog and failed draft | Question/choice identity, score and timer preserved across activity recreation; hidden answer remains hidden; no stale-language narration; dialog dismissal does not reset data | E; R race details |
| LO-06 P1 | Resource parity script; resolve plurals 0/1/2/3/15 and format placeholders for stars, roles, item locks/errors | No missing translation masked by fallback, incompatible formatter arguments, untranslated runtime key or crash; exact copy matches current resources | S + existing script/J/E |

### 6.8 Navigation, lifecycle and process death

Owners: `ClocklingsApp.kt`, `Navigator.kt`, `Screen.kt`, `MainActivity.kt`, feature ViewModels/saveable state. Reuse `NavigationFlowTest`, `LocalAppFlowTest`, `NativeFeelUiTest`, `ActiveLocaleStateTest`.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| NV-01 P0 | Open each Home destination; rapid double click; invoke captured old callback after destination leaves; Settings→Language→Back→Back | One destination added per accepted action; stale callbacks ignored; Back returns one level and never removes root | R + M/E |
| NV-02 P1 | Back button, system Back, predictive-back cancel/commit where available; cancel while Explore value or game question visible | Cancel preserves current state; committed Back pops correctly; no hidden destination remains interactive | E/A |
| NV-03 P1 | Start practice from Learn and Levels; skip queued lessons; result replay/next/dress/back | Returns to correct origin; no duplicate practice entries or completed game reappearing behind Result | R/E/M |
| LC-01 P1 | First launch intro: skip rapidly, press Back, background/return, recreate, animations disabled | Intro finishes or skips once; covered Home not clickable/focusable; no repeated sound/celebration after recreation | R/E |
| LC-02 P0 | Recreate on every meaningful phase: game READ/SET/hint/reveal, lesson page/quiz/pending save, challenge START/RUN/END, Together SET/ASK/REVEALED, personalization, Settings | Retained state consistent with existing contract; no duplicate save/reward; clock/readout and phase agree | E |
| LC-03 P1 | Home/background/lock/unlock while speaking, animating, awaiting save; return twice | Audio stops on pause; no duplicate loops; Home refreshes immediately; active screen resumes safely | E/A; J HomeTime |
| LC-04 P0 | Locale-triggered activity replacement during pending and failed saves; compare activity identity, not only resources | Actual replacement verified; operation outcome attached to correct state; characterize any lost transient retry state via ST-07 | E |
| LC-05 P0 | Background app from Explore, hidden Together, mid-game, lesson quiz, Challenge RUN and saved Result; kill only app process; reopen retained task from Recents | Verify old PID disappeared/new PID appears. Committed data preserved, no fabricated completion/reward or hidden-answer leak. Exact in-progress ViewModel recovery is characterization/decision-gap, not an assumed promise | A + E/M observation |
| LC-06 P1 | Separately force-stop/relaunch, remove task/relaunch, and cold-launch after synthetic data clear | Distinct outcomes logged; committed data survives non-clearing operations; clear fixture returns defaults. Never count these as LC-05 | A/M |

**Process-death procedure:** Save a compact synthetic state summary first. Send the app to Home/background and wait for the activity to stop. Capture its PID and retained task. Use a targeted `adb -s <serial> shell am kill fi.clocklings` only on the verified emulator. Confirm process disappearance; if it remains, the kill case did not run. Reopen the same task using Recents and capture new PID, route, visible state and durable data. Do not replace this with `ActivityScenario.recreate()`, `StateRestorationTester`, “Don't keep activities”, `force-stop`, or Maestro's default restart. An in-app instrumentation test cannot by itself continue reporting after its own process is killed; use an external orchestrator and observation after relaunch.

### 6.9 Explore and Together

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| TG-01 P1 | Explore drag/randomize repeatedly at day boundary; help on/off; supported in-composition mode change and re-entry after Settings change | Clock/readout agree, new random value differs; no progress change; 12/24 adjustment follows source. Differentiate re-entry from retaining a live screen | J/R/E |
| TG-02 P0 | Together SET→ASK→REVEALED; inspect external hierarchy and Compose semantics in ASK; attempt clock input and speech before reveal | Answer absent from text, content/state descriptions and listen controls until revealed; clock noninteractive in ASK; no score or progress saved | R/E/M |
| TG-03 P1 | Reveal from scrolled D3 content; swap twice; repeated clicks; locale/recreate in each phase | Answer brought into view; swap returns top and alternates players 1/2 once; time/phase survives supported recreation; old speech stopped | R/E/M |

### 6.10 Personalization, wardrobe and names

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| PS-01 P1 | Iterate HUMAN/CAT/DOG/ROBOT/DRAGON/DINOSAUR/BUNNY/UNICORN/CAR/NONE; select via pager and thumbnails; switch away/back | Correct selected identity and artwork, independent state; NONE removes artwork without losing access or learning progress | J/R/E/M |
| PS-02 P1 | All item IDs mapped to correct companion/slot/art registry; optional item tap twice, required human item tap twice; locked item below threshold | Registry complete; optional removal allowed, required clothing retained, locked item not equipped; no cross-companion art | J/E |
| PS-03 P1 | Each offered color; former dragon PURPLE and dinosaur GREEN stored values; failed selection then another success | Offered swatch/preview/saved value agree; obsolete choice resolves Original; failure doesn't masquerade as saved choice; ST-06 isolation | J/R/E/M |
| PS-04 P1 | Eight themes; enumerate 48 custom option combinations at model/serialization level; render representative extremes and both hand styles in every learning mode | Choices persist independently; numerals/hand roles/time geometry unchanged; theme cannot change answer correctness | J/R/E |
| PS-05 P1 | Name lengths 0/1/15/16/17, spaces only, leading/trailing spaces, Å/Ä/Ö, combining marks, emoji near cutoff, pasted newline, rapid edits and delete | Established limit: `take(16)` uses UTF-16 units and saved value is trimmed. Verify no malformed/truncated surrogate rendering, crash or cross-companion overwrite; flag Unicode character-count policy separately | R/E; M ordinary text |
| PS-06 P1 | Edit with keyboard open at D3, press IME Done/Back, scroll, switch companion, recreate, fail name save then change color/slot | Field and recovery actions reachable; keyboard does not trap user; latest draft/saved distinction clear; previous companion not renamed by late callback | R/E/M |
| PS-07 P1 | Total stars 0/15, long FI/SV labels, first/last grid item, D0–D4; selected item preview/cropped art | Labels/actions reachable without word clipping; appropriate columns; equipped/locked states identified beyond color; no required full gallery capture | R/E |

### 6.11 Accessibility, layout and motion

Owners: shared components, all screens, `ui/intro/*`, `ui/Motion.kt`, confetti/clock rendering. Reuse existing layout, surface-accessibility, introduction, confetti and screenshot-review tests.

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| UI-01 P0 | D0–D3 × EN/FI/SV: Home, Learn, lesson page/quiz/hint, Levels, game READ/SET/reveal/save failure, Result, challenge START/RUN/END/failure, Explore, Together phases, Appearance/custom, Wardrobe, Settings/Language/reset | Every essential control reachable; no clipped answer/button label, overlap or off-screen pinned actions; long content scrolls. Assert actual text line and control bounds, not just node existence | R/E; M journeys |
| UI-02 P1 | D0 challenge END, D3 reset/error actions, D3 keyboard, tall system bars/cutout where supported; scroll top/bottom | Insets respected; bottom actions actionable; obscured content can be scrolled; score/clock requirements from CH-08 maintained | E |
| UI-03 P0 | Semantic traversal: Home activity names, selected tabs/radios, disabled wrong answers, reset dialog, locked items, clock custom actions, hidden answers and intro overlay | Correct labels/roles/states; hidden answer and covered content not exposed; focus not trapped/lost after navigation/dialog dismissal | R/E |
| UI-04 P1 | On an AVD with an already available screen reader, traverse Home→Explore→Settings→reset cancel; adjust clock using accessible actions; hidden Together answer | Verify actual emulator focus traversal and announcements if observable. If service/observation unavailable, mark blocked; semantics/Maestro success is not a full screen-reader pass | E/A |
| UI-05 P1 | Set system animation scales to disabled; intro, lesson replay, clock command/drag snap, companion selection, correct answer, Result and challenge END | All states complete without waiting forever on animation; no repeated confetti on layout/recreation; learning remains usable without motion | J/R/E |
| UI-06 P1 | Switch system light/dark; enable bold text if image supports it; recreate at critical layout | Existing always-light app stays legible with usable system bars; bold text changes answer measurement cache correctly; no new theme design required | R/E |
| UI-07 P2 | One permitted resized/large-window configuration; rotation request on portrait app; hardware keyboard focus if emulator supports it | No crash, inaccessible essential action, or corrupted saved state; record restrictions actually imposed by system | E/A |

Screenshots must be inspected; successfully saving an image is not a visual pass. Keep selected normal/critical-cell evidence and failure captures, not a duplicate full wardrobe gallery in every matrix cell.

### 6.12 Speech, sound, time and bounded repeated use

| ID / priority | Setup and exact exercise | Expected result / evidence | Method |
| --- | --- | --- | --- |
| AU-01 P1 | Fake engine absent/failing/late/synchronous init; no voice, wrong language, network-required, missing-data voice; EN/FI/SV variants | Speech unavailable until eligible voice/rate set; learning remains usable; no automatic playback of early queued text | J existing SpeechTest |
| AU-02 P0 | Queue speech then stop, language switch, refresh or shutdown; stale/current utterance errors; blocked engine call | Old requests do not speak in new language/screen; stale error cannot disable new utterance; caller/main thread not blocked by engine | J |
| AU-03 P1 | Installed emulator voice present/absent; launch offline, use Listen, navigate/background/return; toggle SFX off/on | UI availability agrees with usable installed voice; stop/refresh behavior works; no-network learning succeeds. Record voice/engine and observed evidence, not claims about physical pronunciation quality | E/A |
| AU-04 P1 | SFX resource open failure or delay, rapid sound requests, mute and background | No crash/main-thread resource blockage; mute/pause honored; no false pass based only on FakeServices that does not implement real mute policy | J + E |
| TM-01 P1 | Inject Home times 11:59→12:00 and 23:59→00:00; stop/start lifecycle; move wall clock/timezone on owned AVD only if needed | Home refreshes at next active poll and immediately on resume; displayed greeting/context correct; no writes to scores | J/R, E optional |
| TM-02 P1 | Inject wall-clock jump during challenge separately from monotonic time progression | Challenge duration unaffected by wall-clock timezone/date changes; do not add calendar/streak tests for absent features | J/S, E corroboration |
| RP-01 P2 | One bounded cycle: 20 Home↔Explore/Appearance/Settings navigations, 10 Together turns, 3 complete challenge runs, name editing and background returns | No accumulating duplicate destinations, observers, sounds, timers or persistent hang; relevant app log has no crash/ANR | E/M |
| RP-02 P2 | If repeated-use behavior suggests a leak/hang, compare settled app-process memory and capture narrow relevant diagnostics before/after | Reproducible trend/failure tied to actions; no universal FPS/memory promise from emulator; do not start a separate optimization campaign | E/A conditional |
| PK-01 P1 | Inspect debug packaged manifest/resources after build: identity, locale set, backup exclusions, exported components and permission declarations | `fi.clocklings`, EN/FI/SV packaged; no unintended runtime permission/network component; source and merged package agree | S/package |

## 7. Execution tasks and commands for the next chat

### Task 1 — Reconcile baseline and create the coverage ledger

- [ ] Re-read applicable instructions after secret scans; inspect branch/status/HEAD and relevant changes since this plan.
- [ ] Check current source for the implementations named above. Retire a case only with a recorded reason; do not treat stale document numbers as current evidence.
- [ ] Map each case to existing test method(s), missing assertions, and the minimum extra test/flow needed. Label existing coverage as source-inspected until actually executed.
- [ ] Create one `reports/edge-cases/REPORT.md` and one `reports/edge-cases/coverage.csv` only if these paths do not already belong to other work. Otherwise select one clearly owned alternate directory once. Keep a short artifact/process ledger in the report.
- [ ] Record tool versions, emulator targets, and planned matrix cells. No production-file changes.

### Task 2 — Deterministic tests first

- [ ] Run the existing full JVM suite once as a current baseline; build the debug app and test APK. Do not run emulator tests concurrently with this memory-heavy task.
- [ ] Extend the existing owning test files for uncovered cases. Prefer local helpers, `MainDispatcherRule`, `FakeServices`, `InMemoryPreferencesStore`, deferred writes and controlled emissions. The in-memory store already serializes updates with a mutex.
- [ ] Keep real file-store reopen/corruption tests on Android: the existing in-memory helper documents Windows JVM atomic-rename limitations.
- [ ] Use the injected `Random` and `nowNanos` seams; do not refactor production APIs to test CH-03/05. Test callback order explicitly and distinguish injected time from coroutine virtual time.
- [ ] Run only the affected test classes after each test addition. A failing test reproducing an application defect remains a finding; do not edit application code or weaken the assertion to make it pass.
- [ ] Once test additions are final, run the full JVM suite once more if any test code changed. If none changed and the baseline is current, do not repeat it unnecessarily.

Illustrative PowerShell commands, executed only after authorization and environment verification:

```powershell
Set-Location -LiteralPath 'C:\Dev\Clocklings'
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon

# Targeted rerun example; change the class filter to the owning test.
.\gradlew.bat :app:testDebugUnitTest --tests 'fi.clocklings.ui.challenge.ChallengeViewModelTest' --rerun-tasks --no-daemon
```

Inspect actual JUnit XML under `app/build/test-results/testDebugUnitTest` and the Gradle task outcome. `UP-TO-DATE` is cached evidence, not a new execution. If fresh baseline execution is required and the task is cached, force the test task to execute and state that explicitly. Do not use `clean` or erase old reports as a shortcut.

### Task 3 — Dedicated emulator baseline and Maestro pilot

- [ ] Select/launch the verified dedicated emulator and record its process handle, original settings, installed package baseline, API and serial. Use hidden process windows for background helpers.
- [ ] Install current debug/test APKs only to that serial. Record APK SHA-256 and source HEAD plus dirty diff scope.
- [ ] Run a small existing functional instrumentation class first. If it fails to connect or initialize, resolve that environmental blocker before running the full matrix.
- [ ] Inspect Maestro hierarchy on that same serial; create one navigation flow with actual selectors. Validate syntax using installed CLI help and then run locally.
- [ ] Confirm output is routed to the owned report directory. Maestro must not auto-select a different device or upload the APK.

```powershell
# Replace this only after verifying AVD name, ownership and API.
$testSerial = '<verified-dedicated-emulator-serial>'
adb -s $testSerial install -r app/build/outputs/apk/debug/app-debug.apk
adb -s $testSerial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s $testSerial shell am instrument -w -r `
  -e class fi.clocklings.AppFlowTest `
  fi.clocklings.test/androidx.test.runner.AndroidJUnitRunner

maestro --device $testSerial hierarchy
maestro --device $testSerial test .maestro/edge-cases/navigation.yaml `
  --format JUNIT --output reports/edge-cases/maestro-navigation.xml `
  --test-output-dir reports/edge-cases/maestro-navigation `
  --debug-output reports/edge-cases/maestro-debug
```

Verify built APK paths and instrumentation component before using these examples. ADB command exit alone does not establish a pass: require instrumentation completion, reported test count, and no failure/crash markers. Maestro flow success must include its explicit assertions and correct target/cell.

### Task 4 — Functional, storage and lifecycle emulator cases

- [ ] Execute existing functional classes in explicit groups. Useful starting groups: `AppFlowTest`, `ChallengeScreenTest`, `LanguageSettingsTest`, `ActiveLocaleStateTest`, `PersonalizationFlowTest`, `PersonalizationStorageTest`, `LocaleReadFailureTest`, `SettingsErrorTest`, `SaveFailureUiTest`, `ClockSurfaceStateTest`, `IntroOverlayTest`, `ComposeStabilityRegressionTest`.
- [ ] Inspect each class's current setup, filters and assertions first. A class name does not mean it covers every row assigned to its area.
- [ ] Add narrowly scoped instrumentation cases only where current J/R tests cannot prove Android behavior. Keep failure injection in test source and test-owned stores; do not add production debug menus, seed intents, network controls, or database backdoors.
- [ ] Execute LC-05 externally, separate from instrumentation recreation. Test pre-commit and post-commit interruption using synthetic fixtures; retain one clear before/after record for each relevant phase.
- [ ] Run Maestro journeys from section 4.2 with defined starting data. Keep tests sequential on one emulator. Avoid duplicate full scored journeys where existing instrumentation already gives the needed proof.
- [ ] Do not run gallery classes blindly in the standard suite. `ScreenshotReview6GalleryTest` requires a `cell` argument and may skip without it; explicitly selected gallery methods are evidence capture, not automatic visual acceptance.

### Task 5 — Display/language matrix and Android-version boundaries

- [ ] Run required D0–D3 API 37 cells in EN/FI/SV; include 24 h longest labels and error/result states, not only happy screens.
- [ ] Run the defined API 26 and API 33 subsets. Do not expand every case into the full cross-product.
- [ ] Verify effective dp/font values from the test environment. Do not use Maestro's web-only `--screen-size` option to configure Android size.
- [ ] Inspect screenshots for clipping/overlap plus functional assertions for actual reachability. Report R and E results separately even when labels match.
- [ ] Execute reduced-motion and emulator accessibility checks; mark unavailable service/observation as blocked rather than a semantic-test pass.
- [ ] Restore each emulator's original settings before stopping or returning it.

### Task 6 — Targeted static/package checks and handoff

- [ ] After scanning its inputs, run `tools/check-localization.ps1` without `-SelfTest` to validate current authored resources when covering LO-06.
- [ ] If Kotlin tests were added/edited, run the applicable test-source style checks, ordinarily `:app:ktlintCheck` and `:app:detekt`, with `--no-daemon`. No automatic baseline update, check suppression, dependency upgrade, or broad unrelated cleanup.
- [ ] Perform PK-01 on the built debug package; do not present it as R8/production release validation.
- [ ] Review changed files and whitespace; ensure production sources and the pre-existing `PROJECT.md` modification were not altered by this campaign.
- [ ] Complete the report/coverage ledger, clean owned disposable artifacts, verify retained evidence/deliverable paths, and stop owned temporary processes.
- [ ] Deliver a concise Finnish summary of actual checks, confirmed findings and gaps. Stop; do not commit/push/create a PR or automatically fix findings.

## 8. Fault-injection and oracle rules

1. **Independent oracle:** For language and scoring, specify expected values independently from the implementation. Calling the same formatter to generate both expected and actual is not a meaningful correctness test.
2. **Deterministic concurrency:** Use `CompletableDeferred`, controlled Flow emissions and the existing coroutine scheduler to expose pending states before releasing writes. An immediate fake throw only tests the failure branch, not a race.
3. **Visible and durable state:** For every save case, assert repository contents, visible state and action availability. Successful UI feedback alone does not prove persistence; a repository assertion alone does not prove usable retry.
4. **Injection limits:** IOException tests prove app handling of that exception. They do not prove behavior on a physically full disk, hardware corruption or all Android filesystem failures. Corrupt only a known synthetic file owned by the test.
5. **No implicit state restoration promise:** Exact continuation after process death is not established by the current ViewModels. Record restart/restoration behavior; require durable-data integrity and no fabricated scores regardless. Escalate desired draft/session continuity as a product decision if not already specified.
6. **Reachability:** Out-of-range level/quiz indexes and impossible enum values require a reachable stored/navigation/input route before being reported as user-facing bugs. Model-level invalid-input experiments remain labeled robustness observations.
7. **Bounded runs:** Reproduction gets one controlled rerun to confirm determinism, then a targeted investigation. Preserve the first failure. Do not rerun indefinitely until green or count a flaky retry as an unqualified pass.
8. **Evidence validity:** A test timeout, driver crash, unsupported AVD image or locale service absence is not proof of an application defect. Preserve enough evidence to distinguish test environment from app behavior.

## 9. Reporting format and completion criteria

### 9.1 Coverage ledger

Use one row per case/subcase and configuration that materially changes the assertion:

```text
case_id,subcase,priority,source_owner,test_method_or_flow,method,api,avd,language,width_dp,height_dp,font_scale,clock_mode,status,evidence,notes
```

Statuses:

- `PASS`: specified assertion executed successfully on the identified baseline/cell.
- `FAIL`: reproducible app behavior violates an established invariant, with actual/expected evidence.
- `BLOCKED`: required environment/tool/oracle unavailable; name the exact blocker.
- `NOT_RUN`: planned, not executed; include reason.
- `STATIC_ONLY`: source conclusion without runtime proof.
- `DECISION_NEEDED`: observed behavior whose desired product contract is unresolved.
- `NOT_APPLICABLE`: source establishes no relevant implemented path; explain why.

Existing source coverage is a mapping field, not a `PASS` status. Count distinct methods, parameterized cases and repeated matrix executions separately. Report skipped tests explicitly.

### 9.2 Findings

For each confirmed failure record:

1. Case ID and concise user-visible consequence.
2. Source HEAD/dirty state, APK hash, framework, API/AVD, locale, display/font and clock mode.
3. Minimal initial fixture and exact action/callback order.
4. Expected contract and its authority; actual visible and stored result.
5. Reproduction command or exact test/flow/method and evidence path.
6. Whether reproduced twice and whether it also occurs through a real emulator UI path.
7. Impact: crash/blocked learning, lost or duplicated data, incorrect teaching/scoring, inaccessible control, or minor display issue.
8. Proposed smallest fix area only; no repair performed unless separately authorized.

### 9.3 Completion

The campaign is complete when every applicable case is classified, all P0 cases have executed evidence or a named blocker/decision gap, the defined emulator matrix is accounted for, and failures are reproducible enough to fix later. A completed campaign can contain failures; it is not a claim that the application is defect-free or release-ready. Do not claim “all Android edge cases tested.”

Retain the plan, added focused tests/flows, one report, one ledger, minimal structured test results and decisive screenshots/log excerpts. No duplicate APK copies, archives or full repeated galleries are required. Preserve the latest build only where needed as the tested deliverable; record its actual path and hash.

Before ending, check all task-created files and processes. Remove only positively identified owned disposable outputs, checking resolved paths and reparse points before recursive operations. Preserve shared caches, pre-existing reports, user changes and unrelated processes. Do not kill all Java/ADB/emulator processes or stop a shared ADB server. If a specific owned leftover must remain, name it and explain why.

## 10. Ready-to-use instruction for the next chat

The owner can provide this file with the following scope:

> Execute `docs/superpowers/plans/2026-11-02-code-and-emulator-edge-case-test-plan.md` against the current Clocklings checkout. First reconcile the baseline and existing coverage. Use deterministic Kotlin tests and explicitly identified dedicated emulators, including local Maestro where the plan assigns it. You may add focused tests and local Maestro flows, but do not fix or refactor application code, add production test hooks, change dependencies, commit, push, or create a pull request. Preserve existing work. Report actual results, reproducible findings, and unverified cases in Finnish. Clean up only your own disposable artifacts and processes.

Providing this file as context without an execution request is not authorization to run that instruction.
