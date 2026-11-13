# Clocklings testing history

Consolidated on **3 November 2026**. Recorded testing period: **20 October–3 November 2026**, including completed check manifests through **10:57:29** on 3 November. Dates and recorded clock times use Europe/Helsinki unless explicitly stated otherwise.

This is a self-contained account of completed testing, observed failures, retests and remaining gaps. It does not require any external logs, screenshots, traces or other evidence files to understand the results. It preserves their conclusions, not their original binary or visual contents. Deleting the original artifacts will remove the ability to inspect those captures or reanalyse the performance traces.

This document was compiled from existing campaign summaries, test-result records, execution logs and 257 check-run manifests. No application tests, builds, emulator sessions or remote analyses were run by the author to create it. Selected source documents and evidence passed local secret scans during compilation. A separately produced build result appeared during compilation and is included in the cutoff above. Historical results apply to the code and environment tested at the time; they do not establish a fresh pass for every file in the current working tree.

## 1. Result overview

| Area | Latest or most relevant recorded outcome | Important limit |
| --- | --- | --- |
| Complete local JVM/JUnit/Robolectric suite | **343/343 passed**, no failures, errors or skips, after the 2 November edge-case fixes | A subsequent equivalent control-flow/style adjustment was followed by 13/13 targeted tests, not another complete suite |
| Final edge-case correction checks | **13/13 targeted tests**, debug app/test APK builds, ktlint and Detekt passed | These targeted checks followed the final adjustment to the clock drag loop |
| Edge-case correction emulator checks | **12/12 method executions**, representing nine distinct methods, on API 36 | Three multilingual layout executions plus nine normal-size regression methods; not the complete device suite |
| Earlier complete device suite | **83/83 passed** on API 37 after the 26 October UI corrections | Historical build, not the final November build |
| UI review handoff | **45/45 requested instrumentation tests**, 16/16 capture invocations and 186 reviewed screenshots on 1 November | Selected displays and states; no complete spoken accessibility review |
| Local lint/check wrapper | **CLEAN**, 2 November at 21:01:55 start time; ktlint, Detekt and debug Android lint reported zero findings | This run covered debug lint; a separate 29 October full run also covered release lint |
| Security wrapper | **CLEAN**, completed 3 November at 10:40:39; all 11 components clean | Four OWASP suppressions and one MobSF exception remained active; MobSF reported seven advisories |
| OSV dependency scan | **72 packages, zero vulnerabilities**, no active package exceptions, 3 November | Result reflects the database and dependency snapshot used then |
| Sonar | Latest retained command completed successfully on 2 November at 15:15:01 with Quality Gate passed | JVM coverage only; no latest numeric coverage percentage is established by that command log |
| CodeQL | **Blocked/error**, 29 October | Private-repository entitlement could not be verified and the workflow was absent; earlier attempts also failed |
| Physical devices, audible speech and full TalkBack | **Not established** by these campaigns | Emulator rendering, semantics and audio start events are different evidence |
| Publication readiness | **Not established** | These are development checks, not acceptance of a production-signed release or Google Play delivery |

Counts from different dates must not be added together as distinct tests. A method repeated across languages or display sizes can produce several executions. Screenshots and capture invocations are not additional functional test cases. Gradle tasks marked up-to-date or from cache are not fresh executions.

## 2. Test environments and evidence types

The application under test was `fi.clocklings`. Emulator tests used dedicated Clocklings AVDs and synthetic data. These records do not establish testing on a physical phone.

| Environment | Recorded use |
| --- | --- |
| `ClocklingsFoundation20261020`, API 37 | Foundation, personalization, localization, UI, artwork, audio-event and regression campaigns; usually serial `emulator-5590`, sometimes `emulator-5554` |
| `ClocklingsLocaleApi26`, API 26 | Legacy language persistence, resources, settings, reset and recreation; serial `emulator-5592` |
| `ClocklingsLocaleApi33`, API 33 | Android LocaleManager and real Activity/settings tests; serial `emulator-5594` |
| `ClocklingsReview20261030`, API 36, x86_64 | November edge cases and performance; read-only/no-snapshot sessions, serial `emulator-5590` or `emulator-5580` depending on the campaign |
| Host JVM and Robolectric | Domain, repository, coroutine, navigation, rendered Compose, semantics and layout tests; Robolectric SDK 37 is not an API 37 device run |

Common device display cells were 360 dp/100%, 320 dp/100%, 360 dp/200% and 320 dp/200%. Some campaigns used approximately 411 dp instead of 360 dp for the normal-width cell. The November edge-case campaign used 640 dp logical height, with D0 = 360 dp/100%, D1 = 320 dp/100%, D2 = 360 dp/200%, D3 = 320 dp/200%, and D4 = 411 dp/100%. System bars reduce the usable app area.

English, Finnish and shared Swedish were tested where explicitly stated. A single-language gallery is not evidence for every language. Component-local width/font overrides, whole-emulator overrides, Activity recreation, saved Compose state and actual process death are identified separately below.

The 2 November edge-case campaign recorded Java 21.0.12, Gradle 9.8.2 and Maestro 2.12.0. The performance campaign recorded Kotlin/Compose compiler 2.4.21, Compose BOM 2026.09.00, Compose runtime/UI/foundation/animation 1.12.1, Material3 1.4.0 and AGP 9.5.0. These are historical environment details, not a new toolchain audit.

## 3. Foundation, personalization and artwork

### 20 October: independent application and foundation

- The initial renamed baseline passed debug assembly and 124 JVM tests. The completed foundation passed **130 JVM tests**, debug app/test APK builds, ktlint and debug/release Compose stability checks.
- Final API 37 instrumentation passed **20/20** at 360 dp/100%, followed by **9/9** relevant UI tests in each of 320 dp/100%, 360 dp/200% and 320 dp/200%: **47 executions** in total.
- Covered Home text line bounds and word breaks; Together phases, role swapping, hidden answers and recreation; lessons; a complete practice round; customization; every level entry; results; dragging; and challenge controls. Result screens and corrected narrow/large-font layouts were visually reviewed.
- English time fixtures covered minute boundaries 0/1/2/14/15/16/29/30/31/44/45/46/58/59, hour wrap, noon/midnight, numeric readings, all 720 analog values and all 1,440 day values. Generated answer labels were checked for uniqueness.
- Package isolation was tested with synthetic installations: the original and Clocklings packages had different UIDs, and a synthetic file in the original package retained its hash through Clocklings updates, tests, uninstall and reinstall. The historical source APK's freshness was not established.
- The recorded original source inputs and Git state were rechecked without changes. This was preservation evidence, not additional app behavior testing.
- Early lint fixes passed 130 JVM tests and three UI methods in each narrow/large-font cell. A later warning cleanup passed 130 JVM tests, two device recreation tests, strict dependency verification and zero ktlint/Detekt/Android-lint findings. The recorded Gradle version exceeded the then-documented Kotlin full-support table maximum; successful local execution did not establish upstream full-support status.

### 21 October: companions and clock appearances

- **133 JVM tests** passed, together with debug app/test builds, ktlint, Detekt and debug/release stability checks.
- Device results: **18** existing regression tests, **7** new tests at 360 dp/100%, and **6** UI tests in each of the other three display cells: **43 executions**.
- Verified companion/theme selection, no-companion learning/results, independent styles, custom clock controls, activity recreation, real DataStore close/reopen persistence and shared appearances in learning modes.
- Reviewed all six original companions and accessory variants, all eight clock choices, picker/custom controls, and representative 320 dp/200% screens.

### 22–24 October: wardrobes, rendering and duplication fixes

- Final wardrobe implementation passed **158 JVM tests in 23 classes** and the complete **46/46 API 37 instrumentation suite**. Companion artwork coverage independently passed **7/7** after an earlier 1/7 failure during concurrent edits did not recur.
- Device coverage included all original companions and their wardrobes, names, colors, item unlocks, persistence, results, clock themes and speech metadata. Screenshots covered approximately 411 dp/100%, 320 dp/100%, 411 dp/200% and 320 dp/200%. A real dog result animation showed three newly unlocked items.
- Color-layout checks observed a four-tile row from 392 dp at 100% font and a 2×2 layout at 360/380 dp.
- Shared-rendering duplication cleanup passed **158 JVM tests**, builds, ktlint, Detekt and PMD CPD at a **50-token threshold**. Gallery tests passed **13/13 before and 13/13 after**; all **23 captured PNGs were byte-identical**. Two personalization methods passed in each narrow/large-font cell, six executions total.
- Sonar issue corrections separately passed **158 JVM tests**, builds, ktlint, Detekt and CPD. Device gallery checks passed **5/5**, coverage tests **46/46**, and **11 affected PNGs were byte-identical**. Six additional personalization matrix executions passed. Sonar analysis completed with zero open/confirmed issues and Quality Gate OK.
- Intermediate formatting, Detekt and package-installation failures were retained in the original history. They are superseded only by the stated successful retests. A filename containing “final” alone was not treated as proof of success.

### 28–30 October and 1 November: new companions and UI review

- Bunny, unicorn and car rendering/wardrobe validation included a successful two-test rendering follow-up and **252 captured images** across English/Finnish/Swedish and four width/font cells. A Finnish car section-title word break at 320 dp/200% was reproduced, corrected and recaptured. Final local checks passed **220 JVM tests**, debug assembly, ktlint and stability.
- The 29 October wardrobe consistency work passed **226 JVM tests**, assembly, ktlint and stability. Finnish wardrobe captures covered four display cells; Swedish had a normal-width capture. Observed grids used three columns at 411 dp, two at 320 dp or 200% font, and one at 320 dp/200%.
- The 30 October–1 November UI work covered practice outcome indicators, isolated wardrobe item previews, companion color distinctions, current-path semantics, Home balance and challenge start/run/result sizing. Color distances were unit-tested against the selected CIE76 threshold of 25.
- The 1 November handoff passed **306/306 JVM tests**, debug app/test builds, localization parity and stability, plus **45/45 requested device tests** and **16/16 capture invocations**. All **186 screenshots** were recorded as visually inspected across approximately 411 dp/100%, 360×640 dp/100%, 320 dp/100%, 320 dp/200% and approximately 420 dp/200%, including Finnish 24-hour mode with Bold text.
- Verified the challenge record headline and score at 360×640 dp, four Home actions for every companion, unchanged RUN-to-END clock size, reachable large-text bottom actions and a clipped-score confetti fallback. The extreme fallback also had a 320×420 dp/200% Robolectric test.
- Ktlint was still failing at that handoff. The separate 1 November formatting correction resolved **101 findings**, passed final ktlint with zero findings, **307 tests in 49 classes**, builds and stability. `ActionPairTest` exercised remembered slot state, current callbacks and row/column changes at 411/320 dp and 100%/200%, including the combination and return to the original layout. No device suite was rerun for that formatting correction.

## 4. Languages, settings, navigation and accessibility

### 25 October: localization and settings

- **184 JVM tests in 28 suites** passed with builds, ktlint, Detekt, stability, debug lint and debug bundle generation. Subsequent removal of five unused resource keys and an exact version-notice exception brought Android lint to zero errors/warnings.
- Resource checks verified key/type/plural/array/format-argument parity in English, Finnish and Swedish, including negative fixtures for the checker. The recorded inventory was 438 base keys and 437 translatable keys per language. This is structural completeness, not linguistic acceptance.
- API 37 passed **26** final core functional/content/audio/state methods. All **three active-locale methods** passed, including actual Activity replacement during practice, an unfinished lesson quiz, results and a real-time challenge.
- API 33 passed **6/6** real Activity/settings tests using LocaleManager. This did not include manual navigation through Android's Settings application.
- API 26 passed **6/6 Settings tests at 320 dp/200%**, **6/6 AppFlow tests at 360 dp/100%**, and the other **22** earlier content/functional/audio cases. The final Settings tests superseded the earlier large-text copy failures.
- The API 37 clock/lesson layout matrix passed in all three languages at 360/320 dp and 100%/200% font. Final Settings/reset checks passed six methods at normal size and one layout method in each other cell. Assertions covered actual text widths, whole words, ellipses, dialog action bounds and scrolling to the final body line.
- A debug AAB was converted with bundletool 1.18.3 and installed as device-specific APKs. Language splits were disabled; default English, Finnish and Swedish resources were present. The final Settings matrix ran with airplane mode enabled and Wi-Fi disabled, with language switching working without downloads.
- Eight fake-engine speech tests and three offline-voice metadata tests covered initialization, reconfiguration, last-request-wins behavior, error recovery, network/language filtering and stale-request cancellation. They did not establish pronunciation or audible offline playback.
- A bounded TalkBack smoke test observed focus on Home Settings and Back and opened Settings. Complete traversal and spoken output were not established.

### 26–27 October: navigation and UI changes

- PR 4 corrections passed **185 JVM tests**, builds, ktlint, Detekt, lint, stability and localization checks. API 37 passed nine methods at normal size and two in each other display cell; API 26 passed nine at 320 dp/200%: **24 executions** total. Covered I/O fallback, stale-error clearing, pending writes and locale restoration.
- Initial native-feel work passed **190 JVM tests** and all **83 distinct instrumentation tests in grouped runs**. One result method overlapped between groups. A later correction round passed **193 JVM tests** and then **83/83** in complete device runs, including a final run after the compact Home adjustment.
- Covered a complete practice/reward/outfit journey, levels, lesson quiz, Together concealment/recreation, every companion including NONE, themes/custom controls, settings/reset persistence, dragging, intros, pager selection/recreation, Home fit and activity-name accessibility descriptions.
- The first native-feel matrix passed 17 targeted methods at 360 dp/100% and 11 in each other cell, with final galleries. Three-button navigation passed **2/2** tests and reset remained above the navigation bar. Reduced-motion behavior passed **3/3** methods with animator scale zero.
- Predictive Back evidence used dispatched back events and cancellation; it does not prove every physical edge gesture. Partial English TalkBack focus/semantics were observed on Home, Levels, Appearance and Settings; spoken traversal in all languages was not verified.
- Earlier baseline and multi-class runs stalled or timed out in locale/challenge tests. Isolated/class retests and later full runs passed, but the original intermittent stall's internal root cause was not proven.
- The evening correction round visually rechecked approximately 411 dp/100% and 320 dp/200%. It did not rerun 320 dp/100% or 360 dp/200% for that exact round. An unrelated app briefly took foreground on the dedicated emulator; later interaction was guarded by application focus.
- The 27 October screenshot correction work passed **217 JVM tests**, builds, ktlint and stability. **49 distinct device tests had passing evidence after targeted reruns**, not one clean combined 49-test run. The first combined run passed 41/49. A three-test screenshot/readability matrix passed in five display cells, and **25/25 targeted capture runs** regenerated affected learning/wardrobe states. Lesson replay and all 60 pointed-hand minute positions were checked separately.
- Two visual observations remained at that checkpoint: English 24-hour day-part/help-label overlap and a split English five-minute level title at 320 dp/200%. Later clock/UI work exists, but this consolidation does not establish a specific final closure for those exact observations.
- PR 5 plural corrections passed **218 JVM tests**, assembly, ktlint, Detekt, zero-issue lint and localization parity. The initially failing regression then passed for unlock thresholds one/two in all languages and all four Robolectric display cells. This is not spoken TalkBack evidence.

### 1 November: save-error and retry review corrections

- Initial corrections passed **19/19 targeted JVM tests**, assembly, ktlint, Detekt and stability. Four new regressions first failed before the fixes. Covered pinned save-error bars, retained failed-name drafts, appearance retries, Together scroll reset and lesson-hint precedence.
- Follow-up operation-scoping corrections passed **12/12 targeted tests** and the same build/style/stability checks. Independent name/color/item failures stayed retryable; unrelated success did not erase them, and superseded request completions did not restore stale errors.
- Per-slot wardrobe retry correction passed **6/6 targeted tests** and the same checks. Head and neck failures remained independently retryable after a successful name write.
- These were Robolectric checks, including English/Finnish/Swedish at 411/320 dp and 100%/200%. The full JVM/device suites were not rerun for each correction. An intermediate local lint attempt was stopped after more than ten minutes, and no local lint pass was claimed for those specific review rounds. Later dated lint results are separate.
- Recorded GitHub Android checks passed for preceding review heads. A remote Codex review hit its usage limit and was not a completed clean review.

## 5. Edge-case campaign and verified fixes, 2 November

The campaign classified **88 case groups**. Classification does not mean every proposed subcase was executed. The ledger contained 437 rows: 325 PASS, six historical FAIL, 69 NOT_RUN, 28 BLOCKED, five DECISION_NEEDED, three STATIC_ONLY and one NOT_APPLICABLE. These are evidence/coverage rows, not 437 independent tests. The five confirmed defects below subsequently received passing correction evidence; historical failure rows were retained.

### Execution results

| Stage | Recorded result |
| --- | --- |
| Initial full JVM baseline | 330/330 passed, freshly executed |
| Added targeted edge tests | 62 executed: 58 passed, four failed |
| Full JVM suite before fixes | 343 executed: 339 passed, four failed; no skips |
| Original API 36 campaign | 68 method executions: 65 passed, two failed on the same Swedish word break, one test-helper failure; 47 distinct methods represented |
| Functional subset | Initially 40 passed plus one helper failure; corrected helper retest passed |
| Selected D1/D2/D3 layout methods | 11 passed and one Swedish D3 failure; controlled rerun repeated that failure |
| D4 selected methods | 4/4 passed |
| Reduced motion, all three animation scales zero | 6/6 passed |
| Local Maestro journeys | Six journeys eventually passed within their stated scope; one unexplained Together interaction remains noted below |
| After production fixes | 343/343 JVM tests passed; after the final drag-loop style adjustment, 13/13 targeted tests plus builds, ktlint and Detekt passed |
| Correction emulator verification | 12/12 method executions passed, covering nine distinct methods |

The API 26/33/37 images required by this campaign were unavailable. Only API 36 was run. The older October API 26/33/37 results remain valid historical evidence, but do not fill the November campaign's missing cells.

### Five reproduced defects and correction evidence

| Case | Reproduced behavior | Verified correction |
| --- | --- | --- |
| CH-03: deadline ordering | An answer after 60,001 ms could score if the timer coroutine had not yet run | Answers check the monotonic deadline directly; both answer-first and timer-first boundary regressions pass |
| CH-05: delayed saved-best load | Stored best 10 plus new score 1 could be announced as a new record when the initial read was delayed; durable best stayed 10 | Compare within the same persisted-progress update; delayed initial emission no longer lowers the visible best or announces a false record |
| CK-06: drag interactivity | A drag started while interactive continued changing the clock after interaction was disabled | Check current interactivity for each pointer event; the original regression passes |
| PS-05: name truncation | Fifteen ASCII characters plus an emoji could leave an unpaired UTF-16 surrogate at the 16-unit cutoff | Avoid cutting a surrogate pair; both fitting and over-boundary emoji cases and ordinary name limits pass |
| UI-01 / LO-06: Swedish settings layout | `24-timmarsläge` split before its final letter at 320 dp/200% | Supporting text gets the full row width at large font scale; the original three-language layout regression and Swedish visual check pass |

The correction matrix ran the settings layout method in all three languages at 320 dp/100%, 360 dp/200% and 320 dp/200%. On the final APK, normal-size regression passed all six LanguageSettings methods, two ClockSurfaceState methods and one SettingsError method. The settings matrix preceded the final equivalent drag-loop style adjustment; settings code did not change afterwards. Maestro was not rerun for these fixes.

### Executed functional coverage

| Area | What the recorded tests exercised |
| --- | --- |
| Clock mathematics and input | Normalization/readings/angles in both periods, snap boundaries, analog equivalence, representative dragging and accessible actions, command updates and disabled-interaction regression |
| Task generation and rewards | Seeded level generation, distinct valid choices across languages, READ/SET behavior, mistake/star rules, best-only progress and companion unlock catalogs |
| Practice | Duplicate/late callbacks, hints/reveals, outcomes, round completion, failed-save retry and no premature saved result |
| Minute challenge | Duplicate answers, timer/deadline ordering, delayed best read, retries, recreation, background returns and three real timed runs |
| Lessons | All nine independent numeric/language quiz fixtures, queues, page/replay cancellation, hints, persistence failure/retry and practice links |
| Storage | Defaults, invalid IDs and numeric clamps, legacy outfits, independent retry operations, serialized transforms/reset, cancellation, real store reopen and isolated corrupt-file behavior |
| Localization | All-minute wording/context fixtures, answer-label uniqueness, resource parity, regional fallback, actual locale replacement and active-state preservation |
| Navigation and Together | Main destinations, stale/duplicate callbacks, back cancellation, hidden answers, recreation, role swapping and repeated journeys |
| Personalization | Every companion, no-companion mode, themes/custom options, own styles, wardrobe slots, names, persistence and selection semantics |
| UI/accessibility | Selected text layouts, reachable dialogs/actions, roles/headings/selected states, hidden answers, reduced motion and observed dark-system behavior |
| Audio | Fake-engine/voice metadata, request cancellation and recovery; offline launch and Listen availability observed, not audible output |
| Lifecycle/package | Activity replacement, five retained-task process deaths, force-stop/relaunch, same-APK reinstall and static debug-package inspection |

### Real lifecycle and persistence observations

For process-death checks, the app was backgrounded, its retained task identified, the app process killed, the old PID confirmed gone, and the observed task card reopened. A new PID and unchanged real Preferences DataStore checksum were confirmed in every case.

| State before process death | Observed return |
| --- | --- |
| Explore at 6:15 | Same time and route |
| Together ASK | Answer remained hidden; no numeric answer or Listen action in the hierarchy |
| Lesson quiz | Lesson start page |
| Practice READ question | Same practice route with a new question |
| Running challenge | Challenge START |

Exact continuation of a transient lesson/question/challenge after process death remains a product decision. These observations did not show loss of committed data. Saved Result process death and interruption immediately around a deliberately held commit were not tested.

Force-stop plus explicit launch produced a new PID, Home and unchanged stored data. Reinstalling the same debug APK with `install -r` also preserved the data checksum; this is not an upgrade from an older version. The attempted Recents-card removal did not establish that the task disappeared and therefore remains blocked.

The failed-name recreation test used the real WardrobeScreen/AppViewModel in a test ComponentActivity with an injected IOException store. Actual `recreate()` and platform-locale Activity replacement retained the unsaved draft and unchanged empty committed name, but error/retry counts changed from one to zero. Whether unsaved error/retry state should survive is unresolved. This test is not evidence for the entire MainActivity route or process death.

### Maestro, timing, keyboard and offline limits

- The six local journeys were navigation, together, learning-navigation, personalization, settings-persistence and repeated-navigation. Final bounded coverage included **20 navigation cycles**, **10 Together turns**, fresh synthetic name/color persistence and settings/reset persistence.
- Together initially remained REVEALED after Swap; an unchanged controlled ten-turn rerun passed. The missed interaction remains unexplained, not a proven application bug or an unconditional clean result.
- Three real zero-answer minute challenges ended without a new record or stored-data change. One run returned from background before and after expiry. Observed collection durations of 77/67/68 seconds included automation overhead and are not precise timer measurements. Lock/unlock was not tested.
- Earlier Maestro selector/scroll errors and a SaveFailureUiTest unconditional-scroll helper error were test-harness problems. Corrected runs retained visibility/click assertions. A name erase attempt left a trailing character, and hideKeyboard navigated away; the successful fresh-field path does not prove all replacement-text/IME cases.
- At 320 dp/200%, tapping the name field opened real Gboard. A name was saved and scrolled into view, but subsequent automation changed IME state. Simultaneous field/keyboard reachability and the complete Done/Back sequence remain unverified.
- Offline cold start and Explore worked with Wi-Fi/mobile data off. GoogleTtsService and Listen were present and Listen was tapped; audible playback and stop behavior were not observed. A system-dark screenshot retained the app's light surface and readable system icons.
- Headless emulator launches crashed on the Windows host with 0xc0000005. A full Qt/host-GPU configuration with Vulkan disabled worked. These were host-emulator crashes, not demonstrated Android app crashes. Maestro class-loading failures were bypassed by using the installed JDK 21; their root cause was not established.
- Retained Android buffers showed no matching app crash/ANR for the bounded repeat campaign. No long-duration soak or memory-leak guarantee follows from that result.

Static inspection of the tested debug APK found package `fi.clocklings`, locales en/fi/sv, backup disabled, cleartext disabled, cloud/device-transfer exclusions and no declared uses-permission entries. Exported surfaces included the launcher, debug Compose components and a DUMP-protected profile-installer receiver. This does not establish release-package behavior or an actual backup/transfer test.

## 6. Performance and audio observations

### Debug-emulator measurements, 23 and 28 October

Ten-second Home idle samples after gfxinfo reset were recorded as follows. These are debug emulator observations, not physical-device benchmarks or controlled proof of improvement between dates.

| Scene | Frames | Reported janky frames | p50 / p90 / p95 |
| --- | ---: | ---: | --- |
| Doll with star dress, 23 October | 612 | 1.80% | 30 / 36 / 44 ms |
| Dinosaur with hat/neck item, 23 October | 618 | 0.32% | 26 / 31 / 32 ms |
| Unicorn, 28 October | 613 | 0.49% | 24 / 27 / 28 ms |
| Car, 28 October | 614 | 0.16% | 19 / 26 / 28 ms |
| Dinosaur, 28 October | 613 | 0.16% | 26 / 29 / 30 ms |

Three 23 October debug-emulator cold starts after force-stop reported TotalTime of **3,754, 2,846 and 2,777 ms**.

### 1–2 November: actual minute-update profiling

Four approximately 185-second Perfetto captures compared three real minute changes before/after the correction, with no companion and with the cat. The dedicated API 36 emulator used 1080×2400, 420 dpi, approximately 411 dp, font 1.0, two CPU cores, host GPU and Vulkan disabled. No concurrent build ran during capture.

Both APKs used the R8-minified release variant, temporarily made shell-profileable and signed with a local test key. DEBUGGABLE was verified absent. These were measurement builds, not production-signed releases.

| No-companion Home, total across three updates | Before | After |
| --- | ---: | ---: |
| Drawn frames | 6, two per update | 3, one per update |
| Layout intervals | 3 | 3 |
| Main-thread doFrame duration | 53.20 ms | 28.56 ms |
| RenderThread DrawFrames duration | 97.58 ms | 35.51 ms |
| Main-thread CPU in update windows | 36.37 ms | 32.62 ms |
| RenderThread CPU in update windows | 50.25 ms | 30.10 ms |
| App Deadline Missed | 3 | 3 |
| Frames between updates | 0 | 0 |

The extra draw was removed. Deadline misses remained, so the result does not prove all jank was eliminated. Duration includes waiting and is not CPU time; differing clock text, warmup and emulator scheduling prevent interpreting the ratios as a general speedup.

| Cat scene / phase | Frame-duration p95 | Main CPU ms/s | RenderThread CPU ms/s |
| --- | ---: | ---: | ---: |
| Before / update windows, 37 frames | 57.65 ms | 185.79 | 538.28 |
| Before / between updates, 9,736 frames | 44.46 ms | 121.88 | 430.64 |
| After / update windows, 44 frames | 41.17 ms | 161.93 | 581.09 |
| After / between updates, 10,317 frames | 39.29 ms | 136.32 | 467.14 |

Cat animation kept drawing between minute changes. The small sample does not establish a general animation improvement; between-update maxima remained 230.26 and 147.40 ms. No app GC was observed in the no-companion captures. Cat captures had 23/22 concurrent GC intervals, none within the defined update windows. Allocated bytes and the precise cause of the animation spikes were not measured.

Capture limitations: each trace reported 37 ftrace setup notices and a no-flush configuration warning. Checked baseline no-companion overrun counters were zero. Its last frame lacked a FrameTimeline record, so the six-frame baseline also used doFrame/DrawFrames correlation and gfxinfo. FrameTimeline statistics use available records only.

Validation passed **17 focused JVM/Robolectric tests**, including polling pause/resume, immediate return update, one-draw clock updates, stage geometry and layout regressions. The R8 APK was visually/UI-tree checked in English with cat and NONE at approximately 411 dp/100%, 320 dp/100%, 411 dp/200% and 320 dp/200%. All four menu choices were reachable, with scrolling where needed. Separate performance gains from retained command identity and lifecycle-aware polling were not measured.

### Recorded sound effects

- Initial sound integration passed **196 JVM tests**, assembly, ktlint, Detekt, lint and stability. API 37 audio-service events showed cat/dog selection starts and success followed by cat celebration; no Sfx warnings. The sounds were not listened to.
- Later companion voice checks observed one SoundPool start per selected companion, eight starts including repeated dragon/dinosaur selections. Taps were about four seconds apart, so interruption of overlapping voices was not established by that run.
- Recorded normalization measurements were approximately −20.3 to −19.75 LUFS, with the highest true peak −1.48 dBTP. These measurements do not establish subjective quality.
- The later bunny/car/unicorn integration and quieter equip sound passed **226 JVM tests**, assembly and ktlint. The bunny measured −19.87 LUFS; equip measured −29.65 LUFS, ten decibels quieter than its preceding version. This round was not played on the emulator or listened to by the implementer.
- One intermediate 218-test audio-change run encountered a Windows DataStore rename IOException that also reproduced without the audio changes. Stability also failed on a previously missing ScaffoldBottomBar baseline entry. Those failures must not be described as a clean audio-change suite; later dated successful suites are separate evidence.

## 7. Code quality, dependencies, security and review

### Latest recorded local check runs

Times below are run start times, except the separately stated Sonar completion time. CLEAN means the recorded tool's configured scope completed without blocking findings; it is not proof that the app has no vulnerabilities.

| Check | Date/time | Recorded result and qualifications |
| --- | --- | --- |
| Android project security checks | 3 November 09:38 | Project Semgrep and MobSF clean; seven MobSF advisories and one exception |
| Dependency checks | 3 November 10:04 | Gradle verification, OSV and OWASP clean; four OWASP suppressions |
| Secret scans | 3 November 10:07 | Gitleaks, TruffleHog, Semgrep secrets, Gitleaks history and TruffleHog history clean |
| DeepSec | 3 November 10:12 | Clean; zero active exported findings |
| MobSF standalone | 3 November 10:20 | Clean with seven advisories and one exception |
| OSV standalone | 3 November 10:24 | Clean; the later combined scan recorded 72 packages and zero vulnerabilities |
| Google Android Security Lints | 3 November 10:25 | Debug lint: zero findings and zero toolchain diagnostics |
| Combined security check | 3 November 10:36 | All 11 components clean; completed 10:40:39 |
| Lint wrapper | 2 November 21:01 | Ktlint, Detekt and debug lint clean; input fingerprint stable during the run |
| Compose Rules | 2 November 13:33 | Ktlint and Detekt rule checks clean |
| Compose stability | 2 November 13:35 | No disallowed regression; input fingerprint stable during the run |
| PMD CPD | 2 November 13:42 | Clean |
| Debug build wrapper | 3 November 10:56 | Successful; all 38 actionable tasks up-to-date; input fingerprint stable during the run |
| Unit-test wrapper | 2 November 15:07 | Unit tests clean; instrumentation explicitly NOT_RUN |
| Sonar command | 2 November, completed 15:15 | Build/analysis and Quality Gate passed; JVM-only coverage |
| Dependabot | 29 October 03:29 | Configuration check passed and no open alerts were recorded at that time |
| CodeQL | 29 October 03:29 | Error/blocked; not a completed analysis |

The 257 manifests record 19 Android-security runs, 14 build runs, one CodeQL attempt, 16 Compose Rules runs, 20 stability runs, 17 DeepSec runs, one Dependabot check, 17 dependency runs, 15 Google-security-lint runs, 27 lint runs, 15 MobSF runs, 16 OSV runs, 34 CPD runs, 16 secret-scan runs, 15 combined-security runs and 14 unit-test-wrapper runs. These repeat checks overlap in scope and are not distinct app tests.

Historical manifest states were **236 CLEAN, 18 FINDINGS and three ERROR**. Findings occurred in Android security, dependencies, lint, stability and CPD; errors occurred in CodeQL, DeepSec and lint. Every command category except CodeQL has a later recorded CLEAN result. Raw failed attempts outside the manifest system also existed and are described in the relevant campaign sections.

### Security limits and exceptions

- The combined security run covered Gradle dependency verification, OSV, OWASP, working-tree/history secret checks, Kotlin Semgrep, project Semgrep and MobSF. It did not include CodeQL or a full manual security audit.
- OWASP used four existing package/CVE suppressions. MobSF used the existing targetSdk exception and retained seven informational/advisory groups. A clean result does not mean no exceptions were used.
- OSV reported zero vulnerabilities before and after filtering, with no active package exceptions.
- The 29 October DeepSec campaign warned about weak Kotlin matcher coverage. Zero exported findings does not establish comprehensive Kotlin security coverage.
- The 29 October full lint run passed ktlint, Detekt and debug/release lint with zero findings. Its full security run passed all 11 components. The attempted full instrumentation command was stopped at the owner's request to free RAM and produced no accepted complete-suite result.
- CodeQL history included an unavailable Android SDK installation, strict dependency-verification failures, and a GitHub integration-permission error during SARIF processing/upload. Reaching analysis or generating SARIF is not a completed clean CodeQL result. The later entitlement/workflow blocker remained.

### Sonar workflow and coverage history

- The 25 October wrapper repair tested live output, nonzero exit, timeout/child cleanup and stale/missing receipt rejection in PowerShell 7 and Windows PowerShell 5.1. All four scenarios passed in each shell. The earlier 3,600-second timeout had completed 43 of 44 started device methods and was not counted as a pass.
- The repaired real combined-coverage run passed **72/72 device tests** and received server Quality Gate OK. JVM tasks were up-to-date in that run.
- On 27 October the ordinary Sonar workflow changed to JVM coverage without emulator commands. Wrapper tests passed in both shells, including with an unrelated Android serial present. The first JVM-only upload correctly failed the coverage gate at **26.7% versus 80%**; the other five conditions passed.
- After adding local rendered tests, **210 JVM tests** passed. The next server analysis passed all gate conditions: **89.6% new-code coverage**, 93.95% new-line coverage, 74.96% new-branch coverage and 86.2% overall coverage. Denominators, exclusions and gate thresholds were not relaxed.
- The 29 October server analysis independently confirmed Quality Gate OK, new-code coverage **87.9%**, new duplication **0.0%**, reviewed new security hotspots **100%**, and A reliability/security/maintainability ratings. The interrupted instrumentation attempt was not included as completed coverage.
- The 2 November command log confirms another successful JVM-only gate. It does not provide a replacement numeric percentage for the earlier metrics above.

### Toolchain and dependency validation

- The 24 October local Android-check command completed successfully. Local tooling's final package audit recorded zero vulnerabilities across 309 total dependencies. Earlier remote build and CodeQL failures remain separate from that local pass.
- The 25 October Compose stability correction passed **184 JVM tests**; both debug/release inventories recorded the repository type stable and zero UNKNOWN/UNSTABLE entries.
- The 26–27 October CPD/Detekt corrections passed their scoped unit/build/style checks; the text-concatenation lint correction passed ktlint, Detekt and debug lint. The 29 October Detekt verification log completed unit tests, assembly, ktlint/Detekt and stability successfully.
- AGP 9.5.0 final validation completed debug assembly, unit tests and debug lint successfully. Release-unit-test task configuration was repaired so both stability variants could run without disabling checks.
- CI verification repair checked previously trusted but missing public keys against fresh artifacts/signatures. A Java 21 CI-equivalent run passed **220 tests in 36 suites**, assembly, ktlint, Detekt and zero-issue lint; the follow-up also passed stability. Java 17 had failed because the Robolectric API 37 sandbox required Java 21; app bytecode still targeted Java 17.
- Guava dependency validation passed unit tests and assembly; the corresponding OSV snapshot scanned 71 packages with zero vulnerabilities. Later 72-package results are recorded separately.
- Retained DataStore dependency-verification material contained signed module metadata for annotation, collection, Okio and tracing dependencies. Artifact/signature files alone are not an independent completed runtime-test result.
- The local DeepSec `qs` repair to 6.17.0 had two comma-array regressions fail before and pass after, six workspace tests pass, successful frozen-lockfile installation and a clean package audit. This tooling dependency was not part of the Android application.

### Whole-project CodeRabbit review, 28–29 October

The review covered **259 maintained text files** in batches of 126 and 133, with input hashes and file-completion lists matched. The first attempt failed to resolve its comparison branch and was retried explicitly; it was not counted as a completed review.

Of 25 initial issues, 18 were verified and fixed, three were duplicates, one was a false positive and three were valid optional suggestions outside scope. The correction review covered all 22 changed/new files and raised three further items: an optional refactor, a duplicate and a documentation issue that was corrected.

Validation passed **226 JVM tests in 37 suites**, app/test builds, ktlint, Detekt, Android lint, stability and localization. Device fixture tests passed **16/16**. A separate error/navigation method passed **36/36 language/display/surface combinations** across three languages, two widths and two font scales. A real lesson-navigation word break was first reproduced by a failing regression, then corrected. A separate paragraph-width overflow false positive was resolved by checking visible lines, height, ellipses and word boundaries instead of skipping the assertion.

Storage failures used synthetic IOExceptions through real ViewModels/repository paths, with actual Compose error/retry rendering on the emulator. No physical full-disk scenario, complete device suite or spoken TalkBack audit was performed. Review completion is not proof of absence of defects.

## 8. Remaining decisions and verification gaps

1. **Current-build platform coverage:** the November edge-case campaign did not run its required API 26, 33 and 37 cells. Earlier platform results are historical. A complete all-screen/all-language matrix was not established for the final code.
2. **Accessibility:** full audible TalkBack traversal in all three languages, focus recovery across every dialog/navigation state, physical gesture navigation and hardware-keyboard traversal remain unverified. Automated semantics and partial focus observations do not close this gap.
3. **Speech and haptics:** real-engine pronunciation, audible offline playback, stop/recovery behavior and physical haptic feel remain unverified. Audio metadata and SoundPool start events are narrower evidence.
4. **Lifecycle decisions:** exact in-progress session continuation after process death and persistence of unsaved draft error/retry state need an explicit product contract. Durable data preservation was tested separately.
5. **Persistence/interleavings:** pending commits combined with recreation, route departure, reset, process death, duplicate retry and late callbacks were not exhaustively exercised. No physical full-disk test or old-version upgrade was completed.
6. **IME and task removal:** the narrow/large-font full keyboard interaction chain and actual Recents-task removal remain blocked/unproven.
7. **Repeated interactions:** the first missed Together Swap remains unexplained despite a passing unchanged rerun. No long soak, comprehensive timing-race proof or general leak/performance guarantee was established.
8. **Rendering/performance:** the extra Home draw was removed, but deadline misses and animated-companion spikes remain. No physical-device benchmark or allocation attribution was completed. The two 27 October English layout observations lack specific final closure in this consolidation.
9. **Security:** CodeQL remained blocked. Existing OWASP/MobSF exceptions and DeepSec Kotlin-coverage limits qualify clean scanner outcomes.
10. **Release acceptance:** production signing, a final release-wide package/backup/upgrade check, actual Play delivery and native-speaker wording acceptance are not established here. A debug bundle and test-signed R8 profiling APK do not constitute a production release.

The following appendix preserves the detailed unexecuted edge-case limits so a future test task does not accidentally treat partial coverage as complete. The five fixed defects are already resolved as described in section 5; their remaining variants are still distinguished from the passing regressions.

## Appendix A. Detailed edge-case coverage limits

Case prefixes: CK = clock input/math; DG = generation/rewards; GM = practice game; CH = challenge; LS = lessons; ST = storage; LO = localization; NV = navigation; LC = lifecycle; TG = Together; PS = personalization; UI = layout/accessibility; AU = audio; TM = time; RP = repeat testing; PK = packaging. These 77 rows describe the November campaign's remaining scope or evidence boundary, not new defects. They do not negate earlier explicitly dated checks, such as October three-button navigation or the 1 November Bold-text captures. Historical failure wording has been updated only where the recorded correction retested it.

| Case | Remaining scope or evidence boundary |
| --- | --- |
| AU-03 | Actual offline cold launch and Explore/Listen UI observed with GoogleTtsService. Actual audio output and stop on background remain unverified. |
| AU-04 | Host delayed/failed resource opening passes. Real muted/background SFX output not audibly verified. |
| CH-01 | 379/380ms correct-answer duplicate boundary covered; wrong-answer exact 1299/1300ms and rapid UI double-start not separately executed. |
| CH-02 | Timer-first expiry and retained-deadline tests pass; explicit 59999ms accepted answer plus pending-next cancellation tuple not separately executed. |
| CH-03 | Both deterministic deadline event orders now pass. Real-device timer-starvation reproduction was not attempted. |
| CH-04 | Actual locale replacement around 20 s and ordinary background before/after deadline executed. Lock/unlock variant not run. |
| CH-05 | Stored best 10/new score 1 with delayed initial load now passes. Scores 0/10/11 and every release ordering were not separately enumerated. |
| CH-06 | Save-failure/retry unit assertions and visible retry component pass; held retry plus replay race and double retry not all exercised. |
| CH-07 | Three real 60-second runs with replay completed. Leave/reopen during exact 380/1,300 ms answer delay not exercised externally. |
| CH-08 | Robolectric longest-answer/reservation/reduced-motion assertions pass. Device gallery is partial visual evidence, not all language/mode/END combinations. |
| CK-03 | Exact half-turn/near-tie pairs and bidirectional device wrap are not exhaustively exercised. |
| CK-05 | Rendered center/radius/angular boundary and overlapping-pointer hit selection are not exhaustively exercised. |
| CK-06 | Disabled-interactivity drag regression now passes. Multi-pointer cancellation, release outside, navigation and incoming-command variants remain unexecuted. |
| CK-07 | A-to-B-to-C animation supersession and period changes during an active animation lack a dedicated executed race assertion. |
| CK-08 | Representative accessible increment executed; both boundary decrements and help-off combinations not all executed. |
| DG-03 | Existing seeded generation passes; explicit Explore boundary-current 0/5/period-minus-5 tuples not all added. |
| DG-04 | STATIC: makeTasks stops after 500 draws and returns accumulated tasks; game uses tasks.size. Options have 300-draw fallback. Explore/challenge retries have no guard. No infinite RNG test; finite scripted fallback experiment not run, no ordinary-user defect established. |
| DG-05 | Existing mistake/star and best-only assertions pass; full cross-product of every prior best and repeated equal reward not separately enumerated. |
| DG-06 | Threshold catalog and newly-unlocked unit assertions plus one device reward path pass; every T-1/T/T+1 item tuple not separately enumerated. |
| GM-02 | Existing hint/reveal/outcome assertions pass; all repeated disabled-option/Next callback orders not separately exercised. |
| GM-03 | SET hints, equivalent day-half and device practice exercised; repeated Done in every busy/reveal state not separately enumerated. |
| GM-04 | Ten-task completion and outcome aggregation pass in existing tests; all three full-round mistake-pattern sequences not separately repeated. |
| GM-05 | Fail/retry and result-after-save tests pass; held final commit combined with double retry plus actual activity replacement not exercised. |
| GM-06 | Leaving during held final save and delayed success then immediately opening identical level was not independently exercised. |
| GM-07 | Result action callbacks and device replay covered; complete real navigation cross-product for last level/NONE not executed. |
| LC-01 | Intro semantics/skip/timeline covered; Back/background during each intro subphase not all enumerated. |
| LC-02 | Actual ActivityScenario and locale recreation named tests pass; every phase including pending-save/failed-draft not covered. StateRestorationTester rows do not count as activity recreation. |
| LC-03 | Home lifecycle unit polling and actual background returns covered. Audible stop and held-save lock/unlock not independently observed. |
| LC-04 | Actual platform locale replacement while name save is failed was executed with ComponentActivity identity assertion. Held pending commit and other operations remain unexecuted. |
| LC-05 | Five retained-task process deaths executed. Saved Result phase and held pre-/post-commit interruption not run. Exact in-progress recovery is a decision gap. |
| LC-06 | Force-stop/relaunch and explicit fresh-data cold launch pass. Task-removal attempt BLOCKED because retained task disappearance was not established. |
| LO-03 | Supported/fallback regional unit rules and actual platform relaunch pass; every actual device primary/secondary locale list not installed/tested. |
| LO-04 | Host legacy/platform authority tests pass; actual API26 and API33 images unavailable. API36 platform evidence is supplemental only. |
| LO-05 | Actual locale replacement during practice, result, quiz, challenge, hidden Together and reset was exercised. Failed-name draft behavior was characterized separately; held pending writes and other failed operations remain incomplete. |
| LO-06 | Resource parity script and runtime resource/plural tests pass; not a native-speaker wording acceptance review. |
| LS-01 | Existing lesson queue tests pass; full mapping cross-product of every done/intro/mode combination not independently enumerated. |
| LS-03 | Demo cancellation and language tests pass; device narration while leaving/replaying not audibly verified. |
| LS-04 | Known authored quiz and wrong/correct path pass. Held commit plus repeated Next/correct and queued-last-lesson transition not all executed together. |
| LS-05 | Failure/retry and hint-precedence assertions pass; actual background/recreation while commit is held not executed. |
| LS-06 | Intro-marker write failure/skip/exit/reset sequence not executed; desired marker timing remains product characterization, not a repair request. |
| LS-07 | Lesson mapping, grouped labels and localized independent quiz fixtures pass; all live learning-path scroll positions/modes not captured. |
| NV-01 | Duplicate/stale callback tests and Maestro main destinations pass. No unbounded click stress. |
| NV-02 | System Back plus instrumentation dispatcher predictive-cancel pass. Real edge-gesture commit/cancel and three-button navigation not both verified. |
| NV-03 | Practice-origin host tests and lesson Maestro path pass; full Result replay/next/dress navigation sequence not repeated externally. |
| PK-01 | STATIC packaged debug APK: fi.clocklings, en/fi/sv locale config, backup and cleartext disabled, no uses-permission declarations. Exported launcher/debug Compose components and DUMP-protected profile receiver identified. No release/R8 or transfer execution claim. |
| PS-01 | Every companion/NONE instrumented; pager and thumbnails, representative recreation and Maestro switch/relaunch executed. This covers the named flows, not every combination of state and lifecycle. |
| PS-02 | Registry/required/optional/domain coverage and free device item path pass; every locked threshold has no device exercise. |
| PS-03 | Color/domain/retry and representative device persistence pass; every offered color on every companion not manually exercised. |
| PS-04 | Eight theme device checks pass; custom extremes represented. All 48 serialized combinations and every learning-mode render not separately enumerated. |
| PS-05 | Ordinary UTF-16 boundaries and emoji cutoff now pass. Pasted newlines, rapid edit/trim and malformed-glyph persistence on emulator remain unverified. Grapheme-count policy is unresolved. |
| PS-06 | Fresh-field Maestro ordinary name/relaunch passed. D3 real keyboard opened; automated text/scroll/Back observations did not establish the complete simultaneous IME reachability contract. Failed-draft/late companion callback variants remain unexecuted. |
| PS-07 | Robolectric word/lock description and API36 D4 picker/style checks pass; all 0/15-star first/last grid endpoints in every cell not executed. |
| RP-01 | 20 navigation cycles, 10 Together turns (controlled rerun after unexplained first Swap miss), 3 complete timed challenges, name input and background returns executed. No app crash/ANR match in retained log buffers; no unbounded soak or performance guarantee. |
| RP-02 | Conditional memory-trend investigation not triggered: no reproducible accumulating app leak/hang. Emulator native crashes and Maestro tap issue are separately documented. |
| ST-01 | Empty/invalid IDs and negative/out-of-range scores covered; full malformed typed-key cross-product not exhaustively generated. |
| ST-02 | Legacy and explicit-empty outfit unit paths pass; every required-human/optional invalid field permutation not separately enumerated. |
| ST-03 | IOException fallback, non-I/O propagation and real isolated corrupt-file write failure covered. Wrong-typed key matrix not exhaustively run; no full-disk simulation. |
| ST-04 | Committed settings failure/retry and local restart tested; every held write/re-entry/unrelated completion permutation not exercised. |
| ST-05 | Reset scope/cancel/failure/double submission covered by named tests and Maestro. Full combined all-fields plus held double-confirm fixture not separately run. |
| ST-06 | Independent name/color/item failures and supersession tests pass; simultaneous two-slot failures with all old-callback completion orders not enumerated. |
| ST-07 | Failed name tested across actual ComponentActivity recreation and platform locale replacement: draft retained, error/retry lost, committed value unchanged. Color/item/theme, route leave/re-entry and process relaunch with failed draft remain unexecuted. |
| ST-08 | Serialized disjoint transforms followed by reset pass. Pre-reset held transform resuming after reset at a user-reachable UI boundary not reproduced. |
| ST-09 | CancellationException propagation covered by existing test. Each operation before/after real commit and screen replacement not separately run. |
| ST-10 | Real temporary DataStore close/reopen passed plus installed-app committed name/settings relaunch and five store-hash-preserving process deaths, as detailed in section 5. |
| ST-11 | Same-build reinstall passed. Optional historical upgrade skipped because no verified compatible old artifact was selected. |
| TG-01 | Representative gesture/random/mode/locale coverage passes; all midnight/help/mode combinations not repeated on device. |
| TG-02 | Hidden hierarchy/semantics, ordinary transitions and process-death concealment pass. The disabled-interactivity drag regression was fixed; full multi-touch Together reproduction was not run. |
| TG-03 | Ten turns pass on controlled rerun; first unexplained missed Swap remains intermittent evidence. All D3 phase recreation/scroll reset permutations not separately run. |
| TM-01 | Lifecycle polling tested with 12:00/12:01/12:15; phrase/day-part boundary suites pass. Exact injected 11:59 and 23:59 lifecycle transitions not added. |
| TM-02 | STATIC: challenge uses System.nanoTime/deadlineNanos and coroutine delay, not wall time. Retained deadline host assertion executed; emulator wall-clock jump not performed. |
| UI-01 | Selected API 36 D1-D3 text/reachability checks ran; the Swedish D3 settings failure was fixed and retested in all three languages. Full required API 26/33/37 language/screen coverage remains incomplete. |
| UI-02 | Reset bounds and retry surfaces have selected device coverage; the later correction completed the three-language settings/reset layout method. Cutout/tall-system-bar variants and the full cross-product remain untested. |
| UI-03 | Named semantics/role/hidden-answer assertions pass. Full focus traversal after every dialog/navigation not established by these tests. |
| UI-04 | TalkBack package installed but actual announcements are not observable through current test evidence. No screen-reader pass claimed from semantics or Maestro. |
| UI-05 | Six device methods with scales 0 pass plus host reduced-motion tests. Not every screen/gesture/correct-answer combination repeated with motion disabled. |
| UI-06 | Host bold-measurement key and actual system-dark Explore visual check pass. System bold-text rendering not executed. |
| UI-07 | Portrait package declaration inspected; actual permitted multiwindow/rotation and hardware-keyboard traversal not executed. |

The other classified groups were AU-01, AU-02, CK-01, CK-02, CK-04, DG-01, DG-02, GM-01, LO-01, LO-02, LS-02. They have named passing assertions in the recorded ledger without a separate residual-limit row. That absence does not certify every possible variant. Section 5 summarizes their executed scope.

The blocked platform matrix comprised API 26 D0/D3, API 33 D0, and API 37 D0/D1/D2/D3/D4, each in English, Finnish and Swedish: 24 blocked cells. The other four blocked ledger rows concerned spoken TalkBack, audible offline speech/stop, the actual D3 keyboard workflow and confirmed Recents-card removal. Five decision rows described the three transient session restarts and the failed-name draft/retry observation under storage and lifecycle headings.

## Appendix B. Record interpretation and retention

Repeated scanner outputs, source snapshots, downloaded public dependency metadata, temporary test fixtures, build copies and capture attempts have been consolidated into the results above. Their existence is not counted as successful testing. Earlier red tests, tool errors, incomplete runs and superseded observations remain distinguished from successful retests. Original images and traces are not embedded in this text.

Historical campaigns generally recorded restoring emulator settings and stopping their own processes. Some earlier cleanup attempts were blocked by automatic approval review and left temporary files/directories. This document does not claim those old leftovers were removed. The consolidation itself starts no emulator, build daemon, watcher or server and deletes no pre-existing files.

Only this Markdown document is the deliverable of the consolidation. It stands on its own after removal of the original evidence directory. No links, relative evidence paths or instructions to open those artifacts are required.

**Retention update, 3 November 2026:** The owner retained the `C:\Dev\Clocklings\reports\clock-performance` directory, including `C:\Dev\Clocklings\reports\clock-performance\clock-performance.apk`. This APK is the test-signed performance measurement build described in section 6, not a production release. This summary remains self-contained; the retained directory provides the original performance artifacts for later inspection.
