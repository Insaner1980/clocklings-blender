# Clocklings Blender project reference

This guide describes the independent Android checkout used to develop Clocklings with Blender artwork. Use it to locate the code behind a behavior, prepare review questions, choose tests, and design UI changes against the current product.

**Source snapshot:** 15 November 2026, Europe/Helsinki. Branch `codex/modern-android-ui`, HEAD `3d3587642aefc91847e811fdf4142a1f07c037c3`, with existing local changes and untracked files. The source descriptions include the local modern Android UI and build configuration. A checkout of HEAD alone can differ. The dated check results below apply to the snapshots identified in their reports.

## Contents

- [Identity and scope](#identity-and-scope)
- [Architecture and startup](#architecture-and-startup)
- [Navigation and screen contracts](#navigation-and-screen-contracts)
- [Learning, clock behavior and rewards](#learning-clock-behavior-and-rewards)
- [State, storage and recovery](#state-storage-and-recovery)
- [Access, billing and privacy](#access-billing-and-privacy)
- [UI design and window behavior](#ui-design-and-window-behavior)
- [Companions and Blender assets](#companions-and-blender-assets)
- [Localization, speech and audio](#localization-speech-and-audio)
- [Build, tooling and release](#build-tooling-and-release)
- [Tests and evidence](#tests-and-evidence)
- [Review question and test planning guide](#review-question-and-test-planning-guide)

## Identity and scope

| Property | Current reference |
| --- | --- |
| Workspace | `C:\Dev\Clocklings-Blender-version`. The related `C:\Dev\Clocklings` and `C:\Dev\Sateenkaarikello-Android` projects are read-only source material. |
| Product and launcher | Product **Clocklings**, launcher **Clocklings Blender**, intended Play title **Clocklings: Learn to Tell Time**. |
| Package and version | Android application ID `fi.clocklings.blender`, Kotlin namespace `fi.clocklings`, version code 1, version name `1.0`. Use the application ID for ADB, signing/Play configuration and device automation. |
| Runtime | Android API 26 minimum, compile and target API 37. A single Compose activity with an authored light theme, edge-to-edge layout, resizable windows and no fixed orientation. |
| Product | Analog time teaching, guided practice, free clock exploration, turn-taking, a minute challenge, local rewards, companion customization, appearance settings and an adult area. |
| Publication | All agreed features and all seven release languages belong to a single complete release. Curriculum mapping, Teacher Approved work and an MVP release are excluded by the owner. Production signing is unconfigured. |
| Sources of authority | Current owner instructions and `AGENTS.md` govern the task. Code and configuration establish implementation. Historical records establish the evidence they explicitly report. The product proposal includes exploratory features such as profiles, timers, routines and widgets. |

See [AGENTS.md](AGENTS.md), [implementation status](docs/implementation/STATUS.md), the [product proposal](docs/superpowers/plans/2026-10-20-public-clock-learning-app.md), the [Blender implementation note](docs/implementation/blender-version.md), the [modern Android UI plan](docs/superpowers/plans/2026-11-14-modern-android-ui.md) and [app build configuration](app/build.gradle.kts). The UI plan is local and was untracked at this snapshot. The verification and benchmark reports under `reports/` are local and Git-ignored.

## Architecture and startup

| Layer | Responsibilities and entry points |
| --- | --- |
| Android entry | `MainActivity` installs edge-to-edge content and forwards resume/pause to the application container. The authored manifest declares language configuration, optional touchscreen and package visibility for TTS. |
| Composition root | `ClocklingsApplication` lazily creates `AppContainer`, which implements `AppServices` for progress/settings, purchases, SFX and speech. This is manual dependency injection. Host tests can substitute the preference store and purchase service. |
| App shell | `ClocklingsApp` resolves language, publishes localized resources and effective personalization, observes app state with lifecycle awareness, hosts confetti and sets up Navigation 3, including shared and predictive back transitions. |
| App state | `AppViewModel` exposes progress, settings, purchase state, speech availability and write actions. The root waits for progress/settings and initial ownership loading before constructing the navigation content. |
| Feature state | Lesson, Game and Challenge have entry-scoped ViewModels. Explore, Together, modal UI and various visual controls also use Compose state. Inspect the specific holder when reasoning about recovery. |
| Domain and I/O | `domain/` holds clock arithmetic, lesson models, task generation, access policy, outfits and language rules. `data/` owns Preferences DataStore codecs and writes. `audio/` and `billing/` isolate platform operations. |
| Modules | `:app` contains Android behavior, assets and tests. `:benchmark` captures and measures startup for the app's performance variants. `.deepsec/` belongs to local development tooling. |

Entry points are [MainActivity.kt](app/src/main/java/fi/clocklings/MainActivity.kt), [ClocklingsApplication.kt](app/src/main/java/fi/clocklings/ClocklingsApplication.kt), [ClocklingsApp.kt](app/src/main/java/fi/clocklings/ClocklingsApp.kt), [AppViewModel.kt](app/src/main/java/fi/clocklings/ui/AppViewModel.kt) and [settings.gradle.kts](settings.gradle.kts).

**Startup sequence.** The app establishes the locale and settings, waits for initial local state, prepares Home and overlays the greeting. The overlay hides underlying Home semantics/input while active. It can be skipped, and its completion is saveable across recreation. The animated timeline ends at 1,250 ms. Reduced motion uses a 400 ms still timeline. The delayed greeting can continue on Home and is cancelled when the root is no longer on Home. `ReportDrawnWhen` waits for ready root content and the completed overlay. First display, usable Home and the fixed intro duration are distinct measurements. [IntroScreen.kt](app/src/main/java/fi/clocklings/ui/intro/IntroScreen.kt), [IntroTimeline.kt](app/src/main/java/fi/clocklings/ui/intro/IntroTimeline.kt), [ClocklingsApp.kt](app/src/main/java/fi/clocklings/ClocklingsApp.kt)

**Debug fixture.** The debug manifest selects `BlenderPreviewApplication`. First initialization runs asynchronously. It writes three stars per level and selects ROBOT in separate DataStore updates, then records `blender_preview / initialized` in private SharedPreferences. The marker preserves later choices across launches. The debug purchase provider independently reports full ownership. Test applications can supply different stores, progress and entitlements. A clean release uses the production application and verified Play purchases. [Debug manifest](app/src/debug/AndroidManifest.xml), [BlenderPreviewApplication.kt](app/src/debug/java/fi/clocklings/BlenderPreviewApplication.kt), [debug purchase provider](app/src/debug/java/fi/clocklings/billing/AppPurchases.kt), [release purchase provider](app/src/release/java/fi/clocklings/billing/AppPurchases.kt)

## Navigation and screen contracts

`Screen` has 14 typed keys registered in `ScreenSavedState`. The stack has Home at its base. `Navigator` filters restricted lessons, levels and Challenge on restoration and route changes, checks the current destination before acting on stale callbacks, and stops speech before changing destinations. Saveable state and ViewModel store decorators scope state to back-stack entries. An entry key can be restored independently of its transient lesson, game or challenge state. [Screen.kt](app/src/main/java/fi/clocklings/Screen.kt), [Navigator.kt](app/src/main/java/fi/clocklings/Navigator.kt), [ClocklingsApp.kt](app/src/main/java/fi/clocklings/ClocklingsApp.kt)

| Displayed screen | Key | Behavior and source |
| --- | --- | --- |
| Home | `Home` | Learn, Practise, Explore, Together, Outfits, Personalize and Settings entry points. The companion stage opens Personalize. With a companion present, Outfits opens Wardrobe directly. A clock-only stage supports the no-companion choice. Live time follows the device clock. [HomeScreen.kt](app/src/main/java/fi/clocklings/ui/home/HomeScreen.kt), [HomeStage.kt](app/src/main/java/fi/clocklings/ui/home/HomeStage.kt) |
| Learn | `LessonList` | Five chapters with practice rows and a final 24-hour lesson. First rows act as headings, current rows expose state and unavailable items expose disabled state. [LessonListScreen.kt](app/src/main/java/fi/clocklings/ui/lesson/LessonListScreen.kt) |
| Lesson | `Lesson(queue, thenLevel)` | Explanation pages, clock demo, replay/listen, quiz and persisted completion. A level-start queue advances into the game when finished. [LessonScreens.kt](app/src/main/java/fi/clocklings/ui/lesson/LessonScreens.kt) |
| Practise levels | `Levels` | Five practice levels with clock previews, lesson shortcuts and the Minute challenge entry. Commercial access is applied before restricted ViewModels are created. [LevelsScreen.kt](app/src/main/java/fi/clocklings/ui/levels/LevelsScreen.kt) |
| Game | `Game(level, round)` | Alternating read and set tasks. The round key separates replays of the same level. Successful completion replaces Game with Result. [GameScreen.kt](app/src/main/java/fi/clocklings/ui/game/GameScreen.kt), [GameViewModel.kt](app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt) |
| Result | `Result(level, stars, previousBest, starsBefore)` | Shows earned stars and newly eligible outfit items. Next level and replay follow access; the close action returns to Levels. Dress opens Wardrobe from the result when available. [ResultScreen.kt](app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt) |
| Explore | `FreeClock` | Drag or adjust the hands, choose supported precision, randomize and read the current time. Help and 12/24-hour preferences affect presentation. [FreeClockScreen.kt](app/src/main/java/fi/clocklings/ui/freeclock/FreeClockScreen.kt) |
| Together | `Ask` | One player sets the clock, then hands it over. The next player guesses, reveals and swaps roles. [AskScreen.kt](app/src/main/java/fi/clocklings/ui/ask/AskScreen.kt) |
| Minute challenge | `Challenge` | Timed reading round with start, active and result states, best score and retryable saving. [ChallengeScreen.kt](app/src/main/java/fi/clocklings/ui/challenge/ChallengeScreen.kt) |
| Outfits | `Wardrobe` | Displayed companion, name, colors, equipped items, star thresholds and section navigation. The no-companion state has no outfit catalog. [WardrobeScreen.kt](app/src/main/java/fi/clocklings/ui/companion/WardrobeScreen.kt) |
| Personalize | `Personalize` | Companion/clock tabs, companion pager, clock preview and custom clock choices. Includes a route to the wardrobe. [AppearanceScreen.kt](app/src/main/java/fi/clocklings/ui/appearance/AppearanceScreen.kt) |
| Settings and Language | `Settings`, `Language` | Effects, teaching help, 12/24-hour display, language, speech status, Parents entry and confirmed learning reset. [SettingsScreen.kt](app/src/main/java/fi/clocklings/ui/settings/SettingsScreen.kt) |
| Parents | `Parent` | Reverse-code gate followed by purchase/restore information, status and actions. Entrances are wired from Settings and the relevant restricted learning and personalization surfaces. [ParentScreen.kt](app/src/main/java/fi/clocklings/ui/parent/ParentScreen.kt) |

Practice starts enqueue outstanding standard lesson introductions and insert the 24-hour lesson when required by the current settings and intro state. Starting from the level list schedules a write of the intro-shown markers and returns the queue immediately. Quiz completion has its own write and success gate. Opening an individual lesson from Learn supplies its own queue. `replace` gives game-to-result and lesson-to-game transitions a clean back target. `popTo(Levels)` returns to an existing Levels entry or creates it directly above Home. Test these cases separately from back on a lesson opened directly from Learn. [Lessons.kt](app/src/main/java/fi/clocklings/domain/Lessons.kt), [Navigator.kt](app/src/main/java/fi/clocklings/Navigator.kt), [ClocklingsApp.kt](app/src/main/java/fi/clocklings/ClocklingsApp.kt)

## Learning, clock behavior and rewards

### Clock and teaching primitives

- Clock values are minutes. `ClockTime` owns normalization, hand equivalence, angles, snap steps, wrap and hand movement. A 12-hour cycle is 720 minutes and a 24-hour cycle is 1,440 minutes. A 24-hour label can represent the same hand position as its 12-hour counterpart. Locale rules and themed paint are separate from this arithmetic. [ClockTime.kt](app/src/main/java/fi/clocklings/domain/ClockTime.kt)
- `ClockState` owns displayed time, active animation and motion commands. `ClockFace` connects drag, keyboard and accessibility actions to the state and draws the face. `ClockSpec` owns design-grid geometry, including number visibility when the minute hand crosses a numeral, and `ClockThemeArt` owns themed artwork. Test wrap, negative and over-period inputs, wheel/hand angle boundaries, cancellation and programmatic commands independently from locale phrases. [ClockState.kt](app/src/main/java/fi/clocklings/ui/clock/ClockState.kt), [ClockFace.kt](app/src/main/java/fi/clocklings/ui/clock/ClockFace.kt), [ClockSpec.kt](app/src/main/java/fi/clocklings/ui/clock/ClockSpec.kt), [ClockThemeArt.kt](app/src/main/java/fi/clocklings/ui/clock/ClockThemeArt.kt)
- Seven presets are Classic, Rainbow, Space, Forest, Ocean, Garden and Workshop. Rainbow is the default. Custom offers four palettes (Blue, Green, Purple, Coral), three frames (Round, Scalloped, Blocks), two numeral styles (Rounded, Plain) and two hand styles (Round, Pointer). Changes to appearance retain time geometry. [Personalization.kt](app/src/main/java/fi/clocklings/domain/Personalization.kt), [AppearanceScreen.kt](app/src/main/java/fi/clocklings/ui/appearance/AppearanceScreen.kt)

### Learning chapters and lesson state

| Practice level index / minute step | Associated lessons and stable IDs |
| --- | --- |
| 0 / 60 | Hands `viisarit`, full revolution `kierros`, whole hours `tasan` |
| 1 / 30 | Half hours `puoli` |
| 2 / 15 | Quarter hours `vartti`, past/to `ylivaille` |
| 3 / 5 | Five-minute increments `viisi` |
| 4 / 1 | Exact minutes `minuutit` |
| Optional final lesson | Day and 24-hour clock `vrk`, no additional game level |

`LessonCatalog` maps stable IDs to localized pages, a demo timeline and quiz content. Demo language is part of the teaching contract. Half-hour explanations in Finnish, Swedish and German refer to the upcoming hour. English, French, Spain Spanish and Brazilian Portuguese refer to the current hour. The five-minute demo reaches the intended position before showing its label. Replay cancels the current demo and stops speech. A language update reconstructs localized content and retains active quiz choice state. A correct answer starts saving, and completion actions wait for successful persistence. An IO failure restores the pre-answer state and permits retry by answering again. [Lessons.kt](app/src/main/java/fi/clocklings/domain/Lessons.kt), [LessonCatalog.kt](app/src/main/java/fi/clocklings/ui/lesson/LessonCatalog.kt), [LessonViewModel.kt](app/src/main/java/fi/clocklings/ui/lesson/LessonViewModel.kt)

### Practice, challenge and Together

- **Practice.** A round targets ten distinct times with alternating READ and SET tasks. READ targets three shuffled choices, including plausible nearby-hour, mirrored-minute and swapped-hand mistakes. In 24-hour mode choices can use the same hand position in the other half of the day. The bounded random generation loops can return shorter results with adversarial random sources. A first mistake gives a hint. A second reveals the answer and enables Next. SET checks hand positions modulo 720 minutes. Correct answers advance after a 1,500 ms pause. Examine command identity and retained time when rotating during dragging or feedback. [Levels.kt](app/src/main/java/fi/clocklings/domain/Levels.kt), [Hints.kt](app/src/main/java/fi/clocklings/domain/Hints.kt), [GameViewModel.kt](app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt)
- **Stars and rewards.** Up to one mistake earns three stars, up to three earns two, and any greater count earns one. Each level retains its best result, giving a total of at most 15. The same DataStore transform captures the old total/level best and writes the best-preserving result. Navigation waits for a successful write. Save failure preserves the completed round and offers Retry. Reward previews are limited to the displayed companion and currently eligible items whose thresholds were crossed. A preview can dress the companion temporarily. Equipping remains a wardrobe action and never spends stars. [Progress.kt](app/src/main/java/fi/clocklings/domain/Progress.kt), [GameViewModel.kt](app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt), [ResultScreen.kt](app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt)
- **Minute challenge.** A run uses a 60-second monotonic deadline and questions from levels 0–3. It rejects late picks even if the timer has yet to dispatch. A correct answer advances after 380 ms, a wrong one after 1,300 ms. Best score is checked against persisted progress when writing a candidate record. Failed saves reach the result with Retry, and restarting cancels pending work tied to the prior run. Challenge records are separate from stars and outfit rewards. [ChallengeViewModel.kt](app/src/main/java/fi/clocklings/ui/challenge/ChallengeViewModel.kt)
- **Together.** SET, ASK and REVEALED are distinct phases. Five-minute hand controls are active in SET. ASK hides the readout, answer semantics and Listen, and rejects stale setter actions. Reveal restores the answer. Swap keeps the selected time and changes the setter once. Spoken guesses are a social interaction, with no microphone capture or grading. [AskScreen.kt](app/src/main/java/fi/clocklings/ui/ask/AskScreen.kt)

## State, storage and recovery

### State lifetime

| Owner | Lifetime and recovery |
| --- | --- |
| Application / DataStore | Learning progress, lesson/intro markers, names, outfits, settings and appearance survive normal restart. Verified ownership has its own store. |
| Platform locale or legacy language preference | API 33+ uses application locales. API 26–32 uses the local language preference. Migration is coordinated separately from app startup's ordinary read fallback. |
| Navigation stack | Typed route keys and their serialized payloads remain separate from live screen state. Access is checked when restoring restricted routes. |
| Entry ViewModels | Practice round, lesson demo/quiz and challenge deadline survive activity recreation while their owner survives. A recreated process can restore the route without restoring arbitrary in-memory work. |
| Compose saveable state | Used for selected UI state such as Explore/Together time and phase, introduction completion and parts of scroll/name state. Inspect individual keys before claiming process restoration. |
| Composition-only state | Adult-gate success/code, transient dialogs and some appearance retry callbacks are scoped to the active composition. |
| Pending writes | `AppViewModel`, the feature ViewModels and `RetryableSave` guard repeated actions. Appearance retry tracks the current request. Name retry can be reconstructed from its saveable draft. A pending write and its recovery UI are separate from a committed update. |

Sources are [AppViewModel.kt](app/src/main/java/fi/clocklings/ui/AppViewModel.kt), [RetryableSave.kt](app/src/main/java/fi/clocklings/ui/RetryableSave.kt), [NameField.kt](app/src/main/java/fi/clocklings/ui/companion/NameField.kt), [AppLocaleController.kt](app/src/main/java/fi/clocklings/ui/locale/AppLocaleController.kt) and the feature sources in the screen table.

### Preferences contracts

`ProgressRepository` transforms the current model inside DataStore `edit`. The application delegates to the private store named `clocklings`. Changes to persistent IDs require considering existing installs. [ProgressRepository.kt](app/src/main/java/fi/clocklings/data/ProgressRepository.kt), [PersonalizationPreferences.kt](app/src/main/java/fi/clocklings/data/PersonalizationPreferences.kt)

| Key or key family | Meaning and compatibility behavior |
| --- | --- |
| `stars_0` … `stars_4` | Per-level best. Read values default to zero and are clamped to 0–3. |
| `challenge_best` | Nonnegative challenge best, defaults to zero. |
| `lessons_done`, `intro_shown` | Separate string sets. Completing a lesson and marking its automatic introduction are different events. |
| `sound`, `help`, `h24` | Default to true, true and false. |
| `app_language` | Legacy language tag on API 26–32. Absent means device choice. The modern handoff can import or clear this key. |
| `outfit_<slot>`, `doll_name` | Human outfit and custom name. Required hair, dress and shoe slots have default items. |
| `outfit_<kind>_<slot>`, `companion_name_<kind>` | Non-human outfits and names. Kind and slot suffixes are lowercase. Unknown or wrong-kind/slot item IDs are filtered. An explicit empty optional slot stays empty. |
| `companion_<KIND>_accessory` | Older per-kind Boolean compatibility input. Only used when that kind has no explicit outfit keys. |
| `companion`, `companion_<KIND>_color` | Saved selection and per-kind color. Unavailable color values fall back to Original. Access can independently affect the displayed companion. |
| `clock_theme`, `clock_palette`, `clock_frame`, `clock_numerals`, `clock_hands` | Appearance enums. Defaults are Rainbow and the custom configuration Blue/Round/Rounded/Round. |

On an initial learning-store `IOException`, the read flow can emit defaults and keeps retrying while collected. Retry delays are 1, 2, 4, 8, 16 and then 30 seconds. After a successful read, another failure leaves the last known state visible. A one-shot `first()` can return the startup fallback and end collection. The language handoff uses a direct read so an unreadable store cannot be interpreted as an absent locale choice. Non-IO errors from the ordinary read flow propagate. [ProgressRepository.kt](app/src/main/java/fi/clocklings/data/ProgressRepository.kt), [AppLocaleController.kt](app/src/main/java/fi/clocklings/ui/locale/AppLocaleController.kt)

A confirmed learning reset clears stars, challenge best, lesson and intro markers, names and outfits. It preserves appearance, sound, help, 12/24-hour mode, locale and purchase ownership. A saved paid companion can temporarily display as Human after the reset while its selection and color remain stored. Stored item validation and eligibility checks are separate. The app checks eligibility when displaying/selecting an item, and the repository validates its ID and slot when reading. [ProgressRepository.kt](app/src/main/java/fi/clocklings/data/ProgressRepository.kt), [AccessPolicy.kt](app/src/main/java/fi/clocklings/domain/AccessPolicy.kt), [AppViewModel.kt](app/src/main/java/fi/clocklings/ui/AppViewModel.kt)

## Access, billing and privacy

### What access changes

| Feature | Free | Full ownership |
| --- | --- | --- |
| Lessons and practice | Whole-hour chapter and level 0, plus the optional 24-hour lesson | All lessons and five practice levels |
| Explore, Together and clock styles | Available | Available |
| Minute challenge | Restricted | Available |
| Companions | Human, Cat and None | Remaining companions become eligible at three earned stars |
| Outfit catalog | Human/Cat items with thresholds up to three stars, subject to earning each threshold | All content subject to companion eligibility and each item's earned threshold |
| Progress | Best practice results, local lesson completion and local customization | Same star and outfit rules; ownership itself awards no progress |

`AccessPolicy` governs destination filtering, selection actions and displayed saved values. If ownership or stars change, display falls back to an eligible companion or outfit. Saved selections can reappear when eligible again. `AppViewModel` rechecks item identity, kind and current stars inside the progress write. A finished or active route has its own lifetime. Validate denial during selection, new navigation and restoration independently from entitlement changes during an already-running round. [AccessPolicy.kt](app/src/main/java/fi/clocklings/domain/AccessPolicy.kt), [Navigator.kt](app/src/main/java/fi/clocklings/Navigator.kt), [AppViewModel.kt](app/src/main/java/fi/clocklings/ui/AppViewModel.kt)

### Purchase and ownership service

The production product is the permanent in-app purchase `clocklings_full_unlock`. The release factory uses the Android application's package name for receipt binding. The checked resource is `UNCONFIGURED`. Its older comment names `fi.clocklings`; configuring a future purchase for this version requires the licensing configuration for `fi.clocklings.blender`. Debug ownership and signed fake receipts are suitable for local coverage. A live Play transaction, current Console state and production signing are unverified here. [billing.xml](app/src/main/res/values/billing.xml), [PlayPurchases.kt](app/src/main/java/fi/clocklings/billing/PlayPurchases.kt), [PurchaseUiState.kt](app/src/main/java/fi/clocklings/billing/PurchaseUiState.kt), [monetization decision](docs/implementation/MONETIZATION.md)

| Stage | Contract |
| --- | --- |
| Parent gate | Generates a four-digit code and requires its reverse with numeric input. Gate success lasts for the current composition. It is an adult-area usability gate. |
| Buy availability | Requires a configured verifier, available product/price and an eligible idle state. Play gateway selects a single permanent base Buy offer and never consumes the unlock. |
| Receipt verification | Validate the exact JSON bytes and RSA signature using the Play receipt protocol. Require the expected package, sole product, nonblank token, quantity one and signed completed wire state. Reconcile callback state, product and token with signed values. |
| Callback and query | A successful launch alone grants nothing. A completed callback leads to an authoritative ownership query. When unowned, the expected verified token must still be present. A transient or invalid response preserves prior verified access. A confirmed absence removes entitlement. |
| Grant and acknowledgement | Write signed evidence before publishing ownership and acknowledging. Acknowledgement failure can leave verified access with a pending status. Retry and Restore reconcile independently of learning data. |
| Foreground and background | The service serializes reconciliation. Ordinary refresh work is cancelled when backgrounded. A launched Play flow can retain its connection for the callback and process buffered results on return. Foreground retries wait 0, 2, 10 and 60 seconds for retryable status. |
| Durable cache | `clocklings_purchase` stores `verified_receipt`, `verified_signature` and `acknowledged`. A cached grant is revalidated at load. Receipt origin and current Play ownership are distinct. If durable revocation fails, old signed evidence can persist until reconciliation. |

Implementation is in [ParentScreen.kt](app/src/main/java/fi/clocklings/ui/parent/ParentScreen.kt), [LocalPurchases.kt](app/src/main/java/fi/clocklings/billing/LocalPurchases.kt), [PurchaseOwnership.kt](app/src/main/java/fi/clocklings/billing/PurchaseOwnership.kt), [LocalPurchaseVerifier.kt](app/src/main/java/fi/clocklings/billing/LocalPurchaseVerifier.kt), [OwnershipStore.kt](app/src/main/java/fi/clocklings/billing/OwnershipStore.kt) and [PlayBillingGateway.kt](app/src/main/java/fi/clocklings/billing/PlayBillingGateway.kt).

### Privacy and platform surface

The launcher activity is the app-authored exported entry. The manifest disables cleartext traffic and backup, references cloud/transfer exclusions, makes touch optional and declares the TTS service query. Billing dependencies contribute to the merged manifest. Inspect the actual artifact for its final permission and component inventory. Clocklings has no account or general-purpose runtime backend in this implementation. Names, progress and ownership evidence live in the Android app sandbox. Clearing app data can remove local learning data. Restoring ownership queries Play for a purchase. Local learning data has its own store and no in-app restore flow. [Main manifest](app/src/main/AndroidManifest.xml), [data extraction rules](app/src/main/res/xml/data_extraction_rules.xml), [ClocklingsApplication.kt](app/src/main/java/fi/clocklings/ClocklingsApplication.kt)

## UI design and window behavior

### Foundations and shared components

The 14 November UI uses flat section tints, start-aligned headings and violet pill actions. `Primary` is `#6741D9` with white content. `UiInkSoft` is the supporting UI text color. Clock and character drawing have their own color and geometry contracts, including artwork consumers of `InkSoft`. Fredoka is the display family and Nunito is the body family. Use established tokens for component variants, shape, spacing and type. Project guidance excludes decorative colored edge stripes and middle-dot UI separators. [Theme.kt](app/src/main/java/fi/clocklings/ui/theme/Theme.kt), [SkColors.kt](app/src/main/java/fi/clocklings/ui/theme/SkColors.kt), [SkShapes.kt](app/src/main/java/fi/clocklings/ui/theme/SkShapes.kt), [SkDimens.kt](app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt), [Type.kt](app/src/main/java/fi/clocklings/ui/theme/Type.kt)

| Component group | Implementation contract |
| --- | --- |
| Actions | `PressSurface`, `SkButton`, `RoundIconButton` and `ActionPair` own flat press/focus treatment, roles, icon descriptions and layout. Action pairs can stack when labels cannot fit. |
| Shell and content | `ScreenScaffold` and `TopBar` handle reading width, insets, scrollable content, headers and bottom actions. Use the available viewport and keep focus/IME visibility when changing placement. |
| Learning rows | `PathRow` replaces the old trail. Step, current and practice treatments share a component. Chapter headings and current rows remain labelled/focusable buttons where applicable. Current state is a localized state description. Unavailable rows expose disabled state. Large text can move indicators below the title. Levels keep the 56 dp mini clock. |
| Progress and answers | `ProgressSegments` exposes a localized numeric task position and keeps the decorative segment row out of semantics. It distinguishes first-try, hinted and shown outcomes. `OptionButton` and `ReadOptions` own numeric and word labels, state, feedback and disabled interaction. Review accessible answer text, the visible error/correct treatment and the flying-star target as separate behaviors. |
| Panels and selection | `FlatPanel`, pills, bubbles, `SwatchTile`, `SelectionBadge` and the Settings controls share the token system. Color and 12/24-hour choices expose selection states. |
| Section navigation | `SectionChips` uses tab selection semantics. Wardrobe compares measured heading and viewport coordinates, clamps the target, selects the last section at the bottom, and supports a changing viewport or scrolling header. Reduced motion snaps. |

Sources include [Buttons.kt](app/src/main/java/fi/clocklings/ui/components/Buttons.kt), [IconButtons.kt](app/src/main/java/fi/clocklings/ui/components/IconButtons.kt), [Layout.kt](app/src/main/java/fi/clocklings/ui/components/Layout.kt), [PathRow.kt](app/src/main/java/fi/clocklings/ui/components/PathRow.kt), [Display.kt](app/src/main/java/fi/clocklings/ui/components/Display.kt), [OptionButton.kt](app/src/main/java/fi/clocklings/ui/components/OptionButton.kt), [SectionChips.kt](app/src/main/java/fi/clocklings/ui/components/SectionChips.kt) and [WardrobeSections.kt](app/src/main/java/fi/clocklings/ui/companion/WardrobeSections.kt).

### Resizing, text, input and motion

- Base tokens include a 16 dp gutter, 640 dp reading width, 1,120 dp wide content cap, 720 dp side-by-side threshold, 400 dp visual pane cap, 360 dp controls budget and 360 dp minimum height for pinned chrome. Components also consider available width after padding, font scale and content. The general adaptive pane check needs both 720 dp and space for 320 dp of visuals, 360 dp multiplied by font scale for controls, and the pane gap. Task-specific layouts also consider height and their own visual floor. [SkDimens.kt](app/src/main/java/fi/clocklings/ui/theme/SkDimens.kt), [AdaptiveContent.kt](app/src/main/java/fi/clocklings/ui/components/AdaptiveContent.kt), [ClockTaskScaffold.kt](app/src/main/java/fi/clocklings/ui/components/ClockTaskScaffold.kt)
- Clock tasks size or reserve space according to the active phase and available viewport. Compact Game uses an answer panel with narrower padding and retains inline Next during feedback. Challenge ranks candidate labels and measures six actual rows to reserve the tallest three. Preparation runs in frame-sized chunks and RUN can measure synchronously when preparation is incomplete. Its cache holds 16 configurations indexed by language, mode, density, font scale, font weight adjustment and width. Long feedback and rewards can scroll, especially at 200% text. [GameScreen.kt](app/src/main/java/fi/clocklings/ui/game/GameScreen.kt), [AnswerRoom.kt](app/src/main/java/fi/clocklings/ui/challenge/AnswerRoom.kt), [ScaffoldClockSizing.kt](app/src/main/java/fi/clocklings/ui/components/ScaffoldClockSizing.kt)
- Home keeps clipped scenery/ripple separate from the character layer, so animation and accessories can extend within the stage design. The direct Outfits action and the companion's Personalize action must retain separate targets. Result prioritizes Next level, falls back to Play again when available, and provides a return action when neither is available. One and multiple rewards get different wrapping layouts. [HomeStage.kt](app/src/main/java/fi/clocklings/ui/home/HomeStage.kt), [HomeStageMath.kt](app/src/main/java/fi/clocklings/ui/home/HomeStageMath.kt), [ResultScreen.kt](app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt)
- `NameField` retains a per-companion draft, caps the draft at 16 UTF-16 code units while avoiding a split surrogate pair, and uses the platform text-editing path. Validate real selection, typing, editing, IME inset and focus behavior when touching it. For disabled controls, modal focus and keyboard-only paths, assertions should cover actionable semantics and rendered focus when appropriate. [NameField.kt](app/src/main/java/fi/clocklings/ui/companion/NameField.kt), [FocusScroll.kt](app/src/main/java/fi/clocklings/ui/components/FocusScroll.kt)
- Reduced motion follows the system animator scale at zero. The app uses shorter or still alternatives for navigation, intro, clock/companion movement and supported celebrations. Static artwork, final clock values and task actions remain available. Verify each surface's chosen behavior, particularly if new effects or scrolling are introduced. [Motion.kt](app/src/main/java/fi/clocklings/ui/Motion.kt), [NavigationMotion.kt](app/src/main/java/fi/clocklings/ui/NavigationMotion.kt), [CompanionView.kt](app/src/main/java/fi/clocklings/ui/companion/CompanionView.kt)

For a changed UI, include 320 dp at normal scale, 200% at a representative width and 320 dp with 200% together. Use measured available width. Add short, wide, rotated, resized, multi-window, IME or long-translation cells when the change reaches those branches. Inspect natural wrapping, reachable actions, contrast, focus order, TalkBack semantics and touch/keyboard/mouse parity relevant to the feature. Recorded platform semantics and human TalkBack listening are separate forms of evidence. [AGENTS.md](AGENTS.md), [approved UI plan and mockups](docs/superpowers/plans/2026-11-14-modern-android-ui.md), [14 November verification](reports/modern-android-ui/VERIFICATION.md)

## Companions and Blender assets

### Catalog, customization and drawing

| Kind | Wardrobe slots | Color choices |
| --- | --- | --- |
| Human (doll) | Hair, dress/overalls, shoes, head, extra. Hair, dress and shoes are required. | Original, Blue, Green, Purple |
| Cat, Dog, Robot, Bunny, Unicorn | Head, neck, extra | Original, Blue, Green, Purple |
| Dragon | Head, neck, extra | Original, Blue, Green, Orange |
| Dinosaur | Head, neck, extra | Original, Blue, Purple, Orange |
| Car | Roof, front, wheels, model | Original, Blue, Green, Purple |
| None | No wardrobe slots or rendered rig | No visible color choices |

The catalog has 146 items, with 18 for the doll and 16 per other rendered companion. Persistent item IDs, slots and star thresholds belong to `Wardrobe` and `CompanionItems`. A required item can be replaced. A selected optional item can be removed by tapping it again. A thumbnail combines applicable required clothes with the preview item and omits other optional items. `ItemArtWindow` uses the companion rig and item-specific crop, so catalog, art position and thumbnail geometry can fail independently. [Wardrobe.kt](app/src/main/java/fi/clocklings/domain/Wardrobe.kt), [CompanionItems.kt](app/src/main/java/fi/clocklings/domain/CompanionItems.kt), [Personalization.kt](app/src/main/java/fi/clocklings/domain/Personalization.kt), [ItemArtWindow.kt](app/src/main/java/fi/clocklings/ui/companion/ItemArtWindow.kt)

Android renders fixed-view layers of the Blender models on a Compose canvas. All nine drawn companions use Blender rigs. `rememberCompanionRig` dispatches by companion and color. `CompanionRig` and `AnimalRig` define the current drawing, crop and design-grid contracts. The animal grid is 240 × 300, while the doll has its own grid. Attachment regions identify a body part and behind/front/screen layer so accessories move with their owning part. Robot screen replacements, doll required clothes and car transformations use their own rig branches. Older copied notes can mention legacy Canvas helpers whose responsibilities are now in the Blender rigs. [CompanionView.kt](app/src/main/java/fi/clocklings/ui/companion/CompanionView.kt), [CompanionRig.kt](app/src/main/java/fi/clocklings/ui/companion/CompanionRig.kt), [BlenderRobotRig.kt](app/src/main/java/fi/clocklings/ui/companion/BlenderRobotRig.kt), [BlenderDollRig.kt](app/src/main/java/fi/clocklings/ui/doll/BlenderDollRig.kt), [BlenderCarRig.kt](app/src/main/java/fi/clocklings/ui/companion/BlenderCarRig.kt), [BlenderAccessoryRegions.kt](app/src/main/java/fi/clocklings/ui/companion/BlenderAccessoryRegions.kt)

### Asset production and fidelity

1. Editable base and accessory scenes and accessory metadata live in `art/blender/`. Character-specific scripts in `tools/blender/` render layered PNG masters. The Blender implementation and accessory notes identify the corresponding scenes, script invocations and evidence.
2. The 42 PNG masters consist of four color atlases for each of eight non-human companions, one doll body atlas and nine accessory atlases. The 42 matching lossless WebP resources are loaded from `drawable-nodpi`. The original PNG directory is included only in the Android test assets for decoder comparison.
3. `optimize_atlases.py` checks RGBA equivalence and keeps dimensions. Its `--check` mode compares existing published files without rewriting them. `BlenderAtlas` uses full-size decoding, straight RGBA followed by Android's premultiplication, and a weak bitmap cache by resource ID. Tests for this path should distinguish file equivalence from Android decoder output.
4. Accessory loading considers both the current and incoming outfit for a celebration. Normal poses include movement such as squash, sway, blink, jump and turn where supported. A reduced-motion verification should inspect final pose/content as well as the absence of time-varying movement. Check stage clipping, full figure, individual worn items, slot previews, layered silhouettes and recreation.

[Blender implementation and reproduction](docs/implementation/blender-version.md), [reference accessories](docs/implementation/blender-reference-accessories.md), [accessory refinement](docs/implementation/blender-polished-accessories.md), [scene and master directory](art/blender), [renderer scripts](tools/blender), [WebP publisher](tools/blender/optimize_atlases.py), [BlenderAtlas.kt](app/src/main/java/fi/clocklings/ui/companion/BlenderAtlas.kt), [BlenderAccessories.kt](app/src/main/java/fi/clocklings/ui/companion/BlenderAccessories.kt)

Use the published-resource check from the repository root when verifying the atlas pipeline. The documented reproduction environment uses Blender 5.3 for rendering and Pillow 13.0.0 for publishing. Render only the affected assets when changing a model.

```powershell
uv run --with pillow==13.0.0 python tools/blender/optimize_atlases.py --check
```

## Localization, speech and audio

### Locale selection and time wording

| Language | App tag / resource location | Teaching or speech detail |
| --- | --- | --- |
| English | `en` / `values`, `raw` | Shared past/to forms including quarter past and half past. |
| Finnish | `fi` / `values-fi`, `raw-fi` | Upcoming hour at half past, near-half forms and dot-separated numeric time. |
| Shared Swedish | `sv` / `values-sv`, `raw-sv` | One content variant, upcoming hour at half past, dot separator. |
| German | `de` / `values-de`, `raw-de` | `halb` names the upcoming hour. The :25/:35 forms refer to that half hour. |
| French | `fr` / `values-fr`, `raw-fr` | Half past names the current hour. Test noon/midnight agreement and next-hour day-part wording. |
| Spain Spanish | `es-ES` / `values-es`, `raw-es` | `la una` versus `las`, `y media`, and `Es`/ `Son` in complete spoken sentences. |
| Brazilian Portuguese | `pt-BR` / `values-pt-rBR`, `raw-pt-rBR` | Device `pt` and `pt-PT` select this content. Optional TTS requires an installed offline Brazilian voice. |

`AppLanguage` resolves by primary language and falls back to English if the ordered device tags have no match. The app offers a device choice and explicit languages. API 33+ reads/writes Android application locales and migrates the old preference if appropriate. An existing Android choice remains authoritative. A legacy read failure can require Retry when the choice is unknown, or leave a known choice usable while cleanup is pending. API 26–32 retains the DataStore path. `AppLocale` provides consistent context/configuration/resources and language selection for the UI. [AppLanguage.kt](app/src/main/java/fi/clocklings/domain/AppLanguage.kt), [locales_config.xml](app/src/main/res/xml/locales_config.xml), [AppLocale.kt](app/src/main/java/fi/clocklings/ui/locale/AppLocale.kt), [AppLocaleController.kt](app/src/main/java/fi/clocklings/ui/locale/AppLocaleController.kt)

`TimeLanguage` and its seven formatters own analog phrases, digital formatting, day parts and greetings. `Texts` connects resource strings/plurals, hints, speech and generated time phrases to screens. Analog phrasing can name the following hour even while a digital value or day label refers to the current time. Review noon/midnight, exact minutes, :15/:30/:45, :25/:35, 12/24-hour labels, the following-hour day-part boundary and singular/plural speech. Test the same time through practice options, lesson content, Explore, accessibility and speech as relevant. [TimeLanguage.kt](app/src/main/java/fi/clocklings/domain/TimeLanguage.kt), [Texts.kt](app/src/main/java/fi/clocklings/ui/Texts.kt), [LessonCatalog.kt](app/src/main/java/fi/clocklings/ui/lesson/LessonCatalog.kt), [resource parity script](tools/check-localization.ps1)

### Three playback paths

| Path | Trigger and recovery |
| --- | --- |
| Effects and companion voices | `Sound` maps recorded clips to a shared `SoundPool`. POP uses `ToneSynth`/AudioTrack. The sound preference and foreground state gate playback. The Settings preview can bypass the disabled preference while the app is resumed. Unready clips can be skipped. |
| Packaged narrator | 14 narrator lines in each of seven languages. The triggers cover greetings, READ/SET instructions, two hand hints, results/reward and challenge start/end. Language changes select the recorded bank. Navigation or newer speech can cancel earlier narration. The 116 packaged OGGs include 98 localized recordings and 18 shared clips. |
| Installed offline TTS | `Speech` selects an available offline voice and handles on-demand Listen. Language switching and shutdown invalidate queued requests using generations. Work runs through a serialized IO dispatcher. An already executing blocking engine call can delay other work. Normal learning remains usable without a matching voice. |

Audio review should include state change during initialization, stale language requests, failed loads, overlapping cues, preference changes and backgrounding. Practice hint narration follows the wrong-answer effect after 1,250 ms and is cancellable by subsequent task/answer work. Challenge ends with its recorded cue and delays the fanfare by 1,500 ms. Results can play an unlock sound followed by reward narration and companion voice. Check screen-specific order when modifying timing. Actual playback and pronunciation require device listening evidence. [Sound.kt](app/src/main/java/fi/clocklings/audio/Sound.kt), [Sfx.kt](app/src/main/java/fi/clocklings/audio/Sfx.kt), [Speech.kt](app/src/main/java/fi/clocklings/audio/Speech.kt), [ToneSynth.kt](app/src/main/java/fi/clocklings/audio/ToneSynth.kt), [GameViewModel.kt](app/src/main/java/fi/clocklings/ui/game/GameViewModel.kt), [ChallengeViewModel.kt](app/src/main/java/fi/clocklings/ui/challenge/ChallengeViewModel.kt), [ResultScreen.kt](app/src/main/java/fi/clocklings/ui/game/ResultScreen.kt)

## Build, tooling and release

### Configured versions and variants

| Component | Current workspace configuration |
| --- | --- |
| Build | Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, built-in Android Kotlin, Compose and serialization plugins |
| JVM | App/benchmark Java compatibility 17. CI uses JDK 21 for the Robolectric API 37 runtime. |
| UI and state | Compose BOM 2026.09.00, Activity Compose 1.13.0, Lifecycle 2.11.0, Navigation 3 1.2.1, DataStore 1.3.0, coroutines 1.11.0, kotlinx.serialization 1.13.0 |
| Integrations and tests | Play Billing 9.1.0, Robolectric 4.17, JUnit 4.13.2, AndroidX Benchmark/Baseline Profile 1.5.0 |
| App variants | `debug`, unsigned production `release` with R8 and resource shrinking, `benchmarkRelease` and `nonMinifiedRelease` with debug-key signing for local performance/profile work |
| Verification | Gradle wrapper checksum, dependency-verification metadata/keyring, Android lint, ktlint with Compose rules, Detekt, stability snapshots and OWASP Dependency-Check |

These are configured values, including working-tree version changes. [version catalog](gradle/libs.versions.toml), [wrapper properties](gradle/wrapper/gradle-wrapper.properties), [app build](app/build.gradle.kts), [benchmark build](benchmark/build.gradle.kts), [root build](build.gradle.kts)

The benchmark module targets `fi.clocklings.blender`. It exercises cold launch to usable Home with no precompilation or the packaged Baseline Profile. Regular builds consume the checked-in profile. Generation is explicit, and the generator collects a Baseline Profile without a separate Startup Profile. Shrunk startup, preview behavior and production purchase verification depend on variant. Record the tested artifact and signing purpose when benchmarking. [benchmark sources](benchmark/src/main/java/fi/clocklings/benchmark), [checked-in Baseline Profile](app/src/release/generated/baselineProfiles/baseline-prof.txt), [shorter startup results](reports/fast-startup/RESULTS.md)

### Commands and scope

From this repository root, use Gradle with `--no-daemon`. Select the smallest appropriate checks. The snippets are examples of existing entry points.

```powershell
./gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug
./gradlew.bat --no-daemon :app:testDebugUnitTest --tests 'fi.clocklings.ui.companion.WardrobeSectionTest'
./gradlew.bat --no-daemon :app:ktlintCheck :app:detekt :app:lintDebug
./gradlew.bat --no-daemon :app:stabilityCheck
./tools/check-localization.ps1
```

Device validation requires a positively identified dedicated emulator for this project. Set its serial only after verifying the target and device selection. The example class checks Android atlas decoding. Choose the class and any arguments according to the change.

```powershell
$env:ANDROID_SERIAL = '<verified dedicated emulator serial>'
./gradlew.bat --no-daemon :app:assembleDebugAndroidTest
./gradlew.bat --no-daemon :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=fi.clocklings.BlenderAssetIntegrityDeviceTest'
```

| Tool or gate | Source and evidence boundary |
| --- | --- |
| GitHub Actions | The workflow requests JDK 21 and Android SDK 37, then runs JVM tests, debug assembly, ktlint, Detekt and debug lint on push, PR and manual dispatch. Inspect the actual run for its revision and result. |
| Local Android checks | PowerShell wrappers delegate to the shared Windows `Android-check` installation with project ID `clocklings`. `config/android-check.json` lists modules and task selections. The owner runs `lc` and `sc`. |
| Compose stability | The analyzer and compiler use the same stability configuration. Compare all affected variant snapshots. The local configuration realizes host-test resources for all variants to support Compose Preview tasks. |
| Dependency and security checks | Dependency-Check scans debug and release runtime classpaths and uses the configured threshold and suppressions. Semgrep and scanner exceptions are scoped inputs. Review exact rules, artifact, scope and expiry when interpreting a report. |
| Sonar | Root Gradle configuration uses debug assembly and JVM JaCoCo XML coverage. The wrapper offers `-PlanOnly`. An authorized upload uses `-AllowExternalUpload`. Check the matching analysis receipt and Quality Gate. Local deterministic `sonar analyze secrets` is the separate pre-read check. |
| DeepSec | `tools/ds.ps1` delegates to the shared runner. `.deepsec/` is the Node/pnpm scanner workspace. Distinguish discovery, processing, revalidation and report export, and use existing approvals/flags for requested external analysis. |

The app disables language splitting for bundles. The build uses a Windows resource-packaging lock to serialize selected resource tasks. CI and local runs can therefore exercise different build-host paths.

Sources are [CI workflow](.github/workflows/android-checks.yml), [local check config](config/android-check.json), [check exceptions](config/check-exceptions.json), [stability config](config/compose-stability.conf), [stability snapshots](app/stability), [dependency suppressions](config/dependency-check-suppressions.xml), [Sonar config](sonar-project.properties), [Sonar wrapper](tools/sonar.ps1), [wrapper test](tools/test-sonar.ps1), [DeepSec wrapper](tools/ds.ps1) and [DeepSec workspace](.deepsec).

Inspect the active branch and preserve local changes. Follow the project's Git and release rules in AGENTS. Debug, test-signed benchmark and unsigned release artifacts have distinct uses. Validate manifest merging, saved-state reconstruction after R8, signing, package-specific billing and store requirements on the authorized release path. The default debug artifact path is `app/build/outputs/apk/debug/app-debug.apk`. [AGENTS.md](AGENTS.md), [app build](app/build.gradle.kts), [R8 rules](app/proguard-rules.pro)

## Tests and evidence

### Source inventory and fixtures

The app has 127 main Kotlin files, two debug Kotlin files and one release Kotlin file. The JVM test tree contains 113 Kotlin files, with 516 source lines declaring `@Test` in 104 files. Instrumentation contains 70 Kotlin and two Java files, with 228 Kotlin `@Test` declarations in 60 files. These are source counts including untracked Kotlin files. Helpers, Android SDK matrices, conditional cases and selected invocations affect execution counts. The `:benchmark` module has a separate test entry point. [JVM sources](app/src/test/java/fi/clocklings), [instrumentation sources](app/src/androidTest/java/fi/clocklings), [benchmark sources](benchmark/src/main/java/fi/clocklings/benchmark)

Host coverage includes pure JUnit, coroutine tests, Robolectric and Compose interaction/rendering. `TestClocklingsApplication`, `InMemoryPreferencesStore` and fake services/purchases support controlled host conditions. The Android runner uses `ClocklingsTestApplication` with controllable full/free ownership. It bypasses the ordinary debug application's first-run preview seeding. A clean launch of the regular debug APK is the appropriate surface for verifying preview initialization. [Host test application](app/src/test/java/fi/clocklings/TestClocklingsApplication.kt), [in-memory store](app/src/test/java/fi/clocklings/data/InMemoryPreferencesStore.kt), [Android runner](app/src/androidTest/java/fi/clocklings/ClocklingsTestRunner.kt)

### Test selection by change

| Change | Relevant host tests | Android or other evidence when needed |
| --- | --- | --- |
| Clock arithmetic, snapping and input | [ClockTimeTest](app/src/test/java/fi/clocklings/domain/ClockTimeTest.kt), [DragTickTest](app/src/test/java/fi/clocklings/ui/clock/DragTickTest.kt), [ClockKeyboardTest](app/src/test/java/fi/clocklings/ui/clock/ClockKeyboardTest.kt), [ClockStateOwnershipTest](app/src/test/java/fi/clocklings/ui/clock/ClockStateOwnershipTest.kt) | [ClockSurfaceStateTest](app/src/androidTest/java/fi/clocklings/ClockSurfaceStateTest.kt), [InputDeviceTest](app/src/androidTest/java/fi/clocklings/InputDeviceTest.kt) |
| Clock rendering and teaching overlays | [NumeralCoverTest](app/src/test/java/fi/clocklings/ui/clock/NumeralCoverTest.kt), [ClockFaceHaloRenderTest](app/src/test/java/fi/clocklings/ui/clock/ClockFaceHaloRenderTest.kt), [RingRevealTest](app/src/test/java/fi/clocklings/ui/clock/RingRevealTest.kt) | [ScreenshotReviewGalleryTest](app/src/androidTest/java/fi/clocklings/ScreenshotReviewGalleryTest.kt) |
| Lesson queue, demo, quiz and retry | [LessonsTest](app/src/test/java/fi/clocklings/domain/LessonsTest.kt), [LessonCatalogTest](app/src/test/java/fi/clocklings/ui/lesson/LessonCatalogTest.kt), [LessonViewModelTest](app/src/test/java/fi/clocklings/ui/lesson/LessonViewModelTest.kt) | [PendingLessonDeviceTest](app/src/androidTest/java/fi/clocklings/PendingLessonDeviceTest.kt), [LocalizationContentTest](app/src/androidTest/java/fi/clocklings/LocalizationContentTest.kt) |
| Practice, save and reward | [LevelsTest](app/src/test/java/fi/clocklings/domain/LevelsTest.kt), [GameViewModelTest](app/src/test/java/fi/clocklings/ui/game/GameViewModelTest.kt), [StorageReadRecoveryTest](app/src/test/java/fi/clocklings/ui/StorageReadRecoveryTest.kt), [VisibleUnlocksTest](app/src/test/java/fi/clocklings/ui/game/VisibleUnlocksTest.kt) | [AppFlowTest](app/src/androidTest/java/fi/clocklings/AppFlowTest.kt), [PendingWriteExitDeviceTest](app/src/androidTest/java/fi/clocklings/PendingWriteExitDeviceTest.kt), [SaveFailureUiTest](app/src/androidTest/java/fi/clocklings/SaveFailureUiTest.kt) |
| Challenge timing and answer room | [ChallengeViewModelTest](app/src/test/java/fi/clocklings/ui/challenge/ChallengeViewModelTest.kt), [RunOptionReservationTest](app/src/test/java/fi/clocklings/ui/challenge/RunOptionReservationTest.kt), [ChallengeInteractionTest](app/src/test/java/fi/clocklings/ui/ChallengeInteractionTest.kt) | [ChallengeScreenTest](app/src/androidTest/java/fi/clocklings/ChallengeScreenTest.kt) |
| Together phase and restoration | [AskScreenTest](app/src/test/java/fi/clocklings/ui/AskScreenTest.kt), [ScreenStateRestorationTest](app/src/test/java/fi/clocklings/ui/ScreenStateRestorationTest.kt) | [Part03DeviceTest](app/src/androidTest/java/fi/clocklings/Part03DeviceTest.kt) |
| Navigation and route access | [NavigationFlowTest](app/src/test/java/fi/clocklings/ui/NavigationFlowTest.kt), [NavigationStateTest](app/src/test/java/fi/clocklings/ui/NavigationStateTest.kt), [AccessNavigationTest](app/src/test/java/fi/clocklings/ui/AccessNavigationTest.kt) | [ActivityRetentionDeviceTest](app/src/androidTest/java/fi/clocklings/ActivityRetentionDeviceTest.kt) |
| DataStore, migration, reset and failed writes | [ProgressRepositoryTest](app/src/test/java/fi/clocklings/data/ProgressRepositoryTest.kt), [OutfitMigrationTest](app/src/test/java/fi/clocklings/data/OutfitMigrationTest.kt), [PersistenceFailureTest](app/src/test/java/fi/clocklings/ui/PersistenceFailureTest.kt), [RetryableSaveTest](app/src/test/java/fi/clocklings/ui/RetryableSaveTest.kt) | [Part04StorageDeviceTest](app/src/androidTest/java/fi/clocklings/Part04StorageDeviceTest.kt), [RetryActivityRecreationTest](app/src/androidTest/java/fi/clocklings/RetryActivityRecreationTest.kt) |
| Entitlement, receipt and adult area | [AccessPolicyTest](app/src/test/java/fi/clocklings/domain/AccessPolicyTest.kt), [RewardAccessActionsTest](app/src/test/java/fi/clocklings/ui/RewardAccessActionsTest.kt), [LocalPurchaseVerifierTest](app/src/test/java/fi/clocklings/billing/LocalPurchaseVerifierTest.kt), [LocalPurchasesTest](app/src/test/java/fi/clocklings/billing/LocalPurchasesTest.kt), [PurchaseRecoveryBoundariesTest](app/src/test/java/fi/clocklings/billing/PurchaseRecoveryBoundariesTest.kt), [ParentScreenTest](app/src/test/java/fi/clocklings/ui/ParentScreenTest.kt) | [MonetizationDeviceTest](app/src/androidTest/java/fi/clocklings/MonetizationDeviceTest.kt), [ReceiptStorageDeviceTest](app/src/androidTest/java/fi/clocklings/ReceiptStorageDeviceTest.kt). Live Play is a separate boundary. |
| Locale handoff and wording | [AppLocaleControllerTest](app/src/test/java/fi/clocklings/ui/locale/AppLocaleControllerTest.kt), [ExpandedLocaleContentTest](app/src/test/java/fi/clocklings/ui/ExpandedLocaleContentTest.kt), [language formatter tests](app/src/test/java/fi/clocklings/domain) | [LanguageSettingsTest](app/src/androidTest/java/fi/clocklings/LanguageSettingsTest.kt), [LocalizationContentTest](app/src/androidTest/java/fi/clocklings/LocalizationContentTest.kt), [structural parity](tools/check-localization.ps1) |
| Audio and startup | [SfxFailureTest](app/src/test/java/fi/clocklings/audio/SfxFailureTest.kt), [SfxNarrationTest](app/src/test/java/fi/clocklings/audio/SfxNarrationTest.kt), [SpeechTest](app/src/test/java/fi/clocklings/audio/SpeechTest.kt), [NarrationResourcesTest](app/src/test/java/fi/clocklings/audio/NarrationResourcesTest.kt), [IntroTimelineTest](app/src/test/java/fi/clocklings/ui/intro/IntroTimelineTest.kt), [BlenderPreviewTest](app/src/test/java/fi/clocklings/BlenderPreviewTest.kt) | [OfflineVoiceTest](app/src/androidTest/java/fi/clocklings/audio/OfflineVoiceTest.kt), [IntroOverlayTest](app/src/androidTest/java/fi/clocklings/IntroOverlayTest.kt), [startup benchmarks](benchmark/src/main/java/fi/clocklings/benchmark) |
| Screen layout, insets and semantics | [ScreenInteractionTest](app/src/test/java/fi/clocklings/ui/ScreenInteractionTest.kt), [AdaptiveContentTest](app/src/test/java/fi/clocklings/ui/components/AdaptiveContentTest.kt), [ClockTaskScaffoldTest](app/src/test/java/fi/clocklings/ui/components/ClockTaskScaffoldTest.kt), [ScaffoldInsetsTest](app/src/test/java/fi/clocklings/ui/components/ScaffoldInsetsTest.kt), [SurfaceAccessibilityTest](app/src/test/java/fi/clocklings/ui/components/SurfaceAccessibilityTest.kt) | [AuditCompletionDeviceTest](app/src/androidTest/java/fi/clocklings/AuditCompletionDeviceTest.kt), [AdaptiveWindowDeviceTest](app/src/androidTest/java/fi/clocklings/AdaptiveWindowDeviceTest.kt), [MultiWindowDeviceTest](app/src/androidTest/java/fi/clocklings/MultiWindowDeviceTest.kt), [NativeFeelUiTest](app/src/androidTest/java/fi/clocklings/NativeFeelUiTest.kt) |
| Wardrobe, name and IME | [WardrobeSectionTest](app/src/test/java/fi/clocklings/ui/companion/WardrobeSectionTest.kt), [WardrobeNavigationTest](app/src/test/java/fi/clocklings/ui/companion/WardrobeNavigationTest.kt), [NameFieldEdgeCaseTest](app/src/test/java/fi/clocklings/ui/companion/NameFieldEdgeCaseTest.kt) | [BlenderWardrobeDeviceTest](app/src/androidTest/java/fi/clocklings/BlenderWardrobeDeviceTest.kt), [NameEditingDeviceTest](app/src/androidTest/java/fi/clocklings/NameEditingDeviceTest.kt), [DeviceInputVerificationTest](app/src/androidTest/java/fi/clocklings/DeviceInputVerificationTest.kt), [NativeProviderDeviceTest](app/src/androidTest/java/fi/clocklings/NativeProviderDeviceTest.kt) |
| Artwork and motion | [BlenderAtlasRenderingTest](app/src/test/java/fi/clocklings/ui/companion/BlenderAtlasRenderingTest.kt), [BlenderAccessoryLoadingTest](app/src/test/java/fi/clocklings/ui/companion/BlenderAccessoryLoadingTest.kt), [CompanionMotionTest](app/src/test/java/fi/clocklings/ui/companion/CompanionMotionTest.kt), per-rig tests | [BlenderAssetIntegrityDeviceTest](app/src/androidTest/java/fi/clocklings/BlenderAssetIntegrityDeviceTest.kt), per-companion `Blender*DeviceTest` and `Blender*MotionDeviceTest`, [BlenderMotionScenario](app/src/androidTest/java/fi/clocklings/BlenderMotionScenario.kt) |

**Device setup and capture.** Some suites depend on explicit instrumentation arguments, language, font scale, window shape, motion scale or installed/offered providers. `NativeProviderDeviceTest` opts in via `nativeProviders=true` and expects the configured IME for the relevant cases. Inspect each fixture before using it, since provider, clipboard, dimensions or app-data settings can be observable outside the test activity. A gallery is visual evidence at its actual configuration. It needs a comparison target and an interaction test when the claim is about behavior. Follow the project rules for restoring owned device settings and closing capture/temporary processes. [AGENTS.md](AGENTS.md), [ScreenshotReviewGalleryTest.kt](app/src/androidTest/java/fi/clocklings/ScreenshotReviewGalleryTest.kt), [NativeProviderDeviceTest.kt](app/src/androidTest/java/fi/clocklings/NativeProviderDeviceTest.kt)

**Copied Maestro journeys.** The six YAML files still target `fi.clocklings`. One personalization scenario starts with `clearState: true`. They are references for journey intent and need explicit retargeting and fixture review before use with the Blender variant. Their force-stop/relaunch steps cover relaunch behavior. Restored process state requires a separate lifecycle scenario. [Maestro files](.maestro/edge-cases), [personalization scenario](.maestro/edge-cases/personalization.yaml)

### Dated verification and remaining boundaries

| Record | What it establishes |
| --- | --- |
| Modern Android UI, 14 November | The report records a fresh JVM suite of 522 passes, then a focused row retest after the final Boolean extraction. Debug and test assembly, ktlint, Detekt, debug lint, resource parity and all four variant stability checks passed at their recorded checkpoints. |
| UI emulator, 14 November | Dedicated API 36 emulator. Five sizes at 100% and 200% for ten flows: 360 × 800, 320 × 800, 800 × 900, 1,200 × 600 and 1,000 × 360 dp. Final 320 × 800 and 1,000 × 360 at 200% were repeated. The report includes resize/rotation/recreation, keyboard, platform semantics, the seven-language compact controls and a 782-image checked gallery. |
| Artwork protection, 14 November | 154 protected file hashes matched. Android compared 42 PNG/WebP atlases. The asset check plus eight normal-motion tests passed, and eight reduced-motion tests passed. Robot color/outfit/recreation was checked separately. |
| Blender import, 12 November | The Blender note reports minimum API 26 robot wardrobe and recreation checks at normal and 200% text, including 320 dp, and separate first-launch preview verification. |
| Startup optimization, 13 November | In the paired API 32 cold-start experiment with the packaged profile, usable Home median moved from 5,656.2 ms to 3,276.1 ms across seven accepted samples per APK. The report fixes the device, artifact and conditions. |
| Inherited 10 November review | Eight ledgers cover questions 001–640 with their own checks and continuations. They describe the earlier Clocklings baseline. Use their findings and coverage to choose a check against the current Blender code. |

See [modern UI verification](reports/modern-android-ui/VERIFICATION.md), [implementation status](docs/implementation/STATUS.md), [Blender import and API 26 validation](docs/implementation/blender-version.md), [atlas optimization](reports/blender-optimization/RESULTS.md), [paired startup measurements](reports/fast-startup/RESULTS.md) and [testing history](TESTING_HISTORY.md).

Inherited review ledgers by topic are [01 setup and overall quality](docs/implementation/2026-11-10-part-01-review.md), [02 clock and teaching logic](docs/implementation/2026-11-10-part-02-review.md), [03 learning and activity behavior](docs/implementation/2026-11-10-part-03-review.md), [04 navigation and persistence](docs/implementation/2026-11-10-part-04-review.md), [05 billing, security and privacy](docs/implementation/2026-11-10-part-05-review.md), [06 UI and artwork](docs/implementation/2026-11-10-part-06-review.md), [07 audio, localization and teaching language](docs/implementation/2026-11-10-part-07-review.md) and [08 coverage and release](docs/implementation/2026-11-10-part-08-review.md).

**Current documentation verification.** The update checked local source/configuration, file paths, inventories, Markdown links, the document diff and the deterministic secrets scan for the source reviewed. The seven-language structural parity command passed. Application build, JVM/instrumentation runs, benchmark reruns, SonarCloud analysis and DeepSec execution were outside this documentation update.

**Open evidence boundaries.** The modern UI record leaves manual TalkBack listening, native-speaker listening and physical hardware unverified. Live Play transactions, current production Console configuration, release signing and publication require their own evidence. The current local record supplies no fresh Chromebook/Googlebook runtime or owner `lc`/`sc` result.

## Review question and test planning guide

A self-contained review question identifies the current branch or diff, entry point, observable behavior and inputs. Ask the reviewer to trace the owner, caller and effect, including the guard and recovery path. Findings should give a concrete trigger and impact with current source evidence. Record the test selection, artifact and environment used. When working through a numbered review, preserve the source numbering and record the last completed item and any unresolved item.

| Review area | Concrete question to adapt |
| --- | --- |
| Clock and scoring | Can a drag or keyboard change near 11:59/12:00 or 23:59/00:00 choose the wrong period, or can a subsequent animation overwrite it? How do SET equivalence and the displayed 24-hour text differ? |
| Lesson and quiz | If language changes during an active demo or after a wrong quiz answer, what restarts, what stays selected, and which condition confirms the lesson was saved? |
| Completion races | Can a double submission advance twice, append a duplicate outcome, or create Result before the star update? Does the same write provide the previous best and the after-total inputs if the read path falls back? |
| Challenge | Does a click at the deadline get rejected before the timer dispatch? Can an old advance, failed record write or delayed end sound affect the next run? |
| Navigation | What happens on restored paid routes after access is lost? Check direct Outfits versus companion tap, game replacement, Result-to-Levels fallback and system back from the resulting stack. |
| Storage | Can explicitly removed optional items come back through legacy decoding? Does resetting progress preserve language, appearance and ownership? Can fallback reads obscure an already committed value? |
| Purchase verification | Does any malformed, stale, pending, wrong-package or wrong-product evidence grant ownership? What changes when a live query fails, absence is confirmed, or cache removal fails? |
| Adaptive UI | At 320 dp and 200%, are long lesson labels readable, answer and result actions reachable, and clock previews intact? What happens in a short wide window, with keyboard focus and docked IME? |
| Semantics | Do current/locked rows retain their state, role and action boundaries? Can Together leak the answer through semantics or an old action? Does a clicked Wardrobe tab correspond to its section after resizing? |
| Blender asset change | Does a changed sprite region follow the intended part across motion, crop correctly in the wardrobe, load for the incoming outfit and survive Android premultiplication? Which sources or atlases are affected? |
| Localization and audio | Does the wording name the right hour and day part? Can narration from the previous task, screen or locale play after switching? Does the UI still work if audio or TTS never becomes ready? |
| Evidence and release | Which result actually used the Blender application ID, intended source variant, build and emulator? Which claims require Android runtime, signed release, live Play, actual listening or manual visual review? |

For a test plan, start with a deterministic domain or state test where it can prove the behavior. Use Compose for actions, semantics and layout contracts. Use Android instrumentation for native decoders, focus, viewport, providers and recreation where those differences matter. Report source inspection, execution results and visual or listening checks by their actual coverage. For UI changes, the project requires 320 dp, 200% font and their combination. [AGENTS.md](AGENTS.md)
