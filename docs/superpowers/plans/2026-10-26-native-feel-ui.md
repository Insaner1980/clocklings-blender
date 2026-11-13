# Clocklings Native-Feel UI Implementation Plan

> **For implementation:** Work through the checkboxes in order after an explicit implementation request. The `superpowers:executing-plans` skill can guide execution. This document is a planning deliverable, not permission to implement, commit, push, or publish.

**Date:** 26 October 2026

**Status:** Implemented on 26 October 2026; completed checks and remaining verification limits are recorded in `docs/implementation/STATUS.md`. The original design was grounded in the repository and the owner's 19 screenshots (`clocklings-screenshots2`). All design decisions are resolved (section 8).

**Goal:** Make Clocklings look and feel like a native mobile application for children instead of a web page in an Android shell, while keeping its cheerful pastel identity, fonts, companions, and clock pedagogy.

**Architecture:** Keep Compose, Navigation 3, manual `AppServices`, Preferences DataStore, and the language-neutral clock domain. The work is a presentation-layer change: theme tokens, shared components in `ui/components/`, screen layouts, navigation transitions, and haptics. No new dependencies.

**Tech stack inspected:** Kotlin 2.4.21, Compose BOM 2026.09.00, Activity Compose 1.13.0, Navigation 3 1.2.0, minSdk 26, compileSdk/targetSdk 37. `NavDisplay` in the cached `navigation3-ui` 1.2.0 artifact exposes `sharedTransitionScope`, `predictivePopTransitionSpec`, and `LocalNavAnimatedContentScope`. Check the exact parameter order against that artifact before wiring.

**Related documents:** [AGENTS.md](../../../AGENTS.md), [implementation status](../../implementation/STATUS.md), [product proposal](2026-10-20-public-clock-learning-app.md), [companion wardrobes design](../specs/2026-10-22-companion-wardrobes-design.md). Owner instructions take precedence.

## Contents

1. [Diagnosis](#1-diagnosis)
2. [Design principles](#2-design-principles)
3. [Scope and constraints](#3-scope-and-constraints)
4. [Target design by area](#4-target-design-by-area)
5. [File and interface map](#5-file-and-interface-map)
6. [Implementation work packages](#6-implementation-work-packages)
7. [Validation and acceptance](#7-validation-and-acceptance)
8. [Resolved design decisions](#8-resolved-design-decisions)
9. [Risks](#9-risks)
10. [Sources and evidence limits](#10-sources-and-evidence-limits)

## 1. Diagnosis

The app was ported from the HTML version, and many components still say so in their KDoc (`web: .card`, `web: .btn`, `web: radial-gradient`). The web origin is visible in seven places:

| ID | Observation | Where in code |
| --- | --- | --- |
| D1 | Every surface is the same white rounded card with a hard bottom "ledge", including surfaces that cannot be tapped ("Right now", Settings groups, "The time is", question panels). Children cannot tell what is pressable. | `SkCard` and `Modifier.cardShadow()` in `ui/components/Buttons.kt`; `Panel` in `ui/components/Panels.kt` |
| D2 | Navigation uses bordered text pills: "← Back", "Settings", "Clock styles", "Help on". | `TopBar` in `ui/components/Layout.kt`; `ChipButton`, `HelpChip` |
| D3 | Home is a scrolling landing page: logo, tagline, stacked cards, and a footer link to clock styles. | `ui/home/HomeScreen.kt` |
| D4 | Much reading: intro paragraphs (`koulu_intro`, appearance intros), uppercase eyebrows (`now_eyebrow`, `clock_is`, `level_eyebrow`), and subtitles on every menu tile. | `Eyebrow` in `ui/components/Display.kt`; screens |
| D5 | Choices look like form controls: text chips for colors and shapes, and a "Selected" text line under the chosen card. | `ChoiceChips`, `ChoiceCard` in `ui/appearance/AppearanceScreen.kt` |
| D6 | The dotted background is a CSS pattern drawn on every screen. | `Modifier.dotBackground()` in `ui/theme/Theme.kt` |
| D7 | The system navigation bar has an opaque white scrim, and content stops above it instead of running edge to edge. | `NAV_SCRIM_LIGHT` in `MainActivity.kt`; `safeDrawingPadding()` in `ScreenScaffold` |

Also missing: haptic feedback (no `Haptic` reference anywhere in `main/`), navigation transitions (`NavDisplay` uses defaults), and spring-based press feedback.

What already works and must stay: the Fredoka and Nunito pair, the pastel palette with a color per activity, the vinyl-shaded companions with idle and celebration poses, the to/past clock halves, the minute ring, and the hand colors. `android:enableOnBackInvokedCallback="true"` is already set, so predictive back only needs transitions.

## 2. Design principles

These principles decide ambiguous details during implementation.

1. **Depth means "press me".** A raised surface with a ledge is always tappable. Anything that is not tappable is flat.
2. **Press feedback is physical.** Pressable surfaces sink, shrink slightly, and spring back. The clock ticks under the finger.
3. **Icons and pictures before words.** A child should be able to use every main flow without reading. Text that remains is short. Removed visible text moves into semantics so TalkBack loses nothing.
4. **Places, not pages.** Each activity has its own color world, and moving between them is animated as a journey.
5. **The companion is the host.** The companion greets, hints, and celebrates. When the companion is "Just the clock", the clock takes its place and the current text boxes are the fallback.
6. **Grown-up screens are calm.** Settings follows standard Android list conventions so the playful screens stand out against it.
7. **Motion respects the system.** Every new animation and transition goes through `rememberReduceMotion()` in `ui/Motion.kt` and degrades to an instant change or a short fade.

## 3. Scope and constraints

### 3.1 In scope

- Surface system, buttons, top bar, icons, backgrounds, and motion tokens.
- Layout changes on Home, Learn list, Levels, Explore, Play Together, Game, Result, Lesson, Appearance, Wardrobe, Settings, and Language.
- Edge-to-edge system bars, navigation transitions, shared elements, haptics.
- Clock face depth styling shared by all clock themes.
- Removal of text that becomes redundant, in English, Finnish, and Swedish together.

### 3.2 Out of scope

- Clock mathematics, scoring, rewards, progress identifiers, lesson content, companion artwork and rigs, wardrobe rules.
- New features, new activities, adult controls, profiles, timers, widgets.
- Dark theme, tablet layouts, new dependencies, toolchain or SDK changes.

### 3.3 Constraints carried over

- All colors, sizes, shapes, and motion specs live in `ui/theme/` and `ui/Motion.kt`. Screens never hard-code them.
- No colored edge stripes on cards or rows, no middle-dot or bar separators.
- Touch targets at least 48 dp. Primary child targets at least 64 dp.
- UI copy in Android resources for all three languages. Identifiers and comments in English.
- Delete components, tokens, and string resources in the same change that makes them unused.
- Validate at 320 dp width, 200% font scale, and their combination on the dedicated emulator.

## 4. Target design by area

### 4.1 Surface system (D1)

Two surface types replace the single card:

| Type | Look | Used for |
| --- | --- | --- |
| **Pressable** (`DepthSurface`) | Filled, ledge underneath, sinks and scales to about 0.96 with a spring on press | Primary and secondary buttons, menu tiles, answer options, level nodes, choice tiles |
| **Flat** (`FlatPanel`, replacing `SkCard` and `Panel`) | Tinted fill, no ledge, no shadow, no border | Clock captions, question panels, "The time is", result summary, Settings groups |

`DepthSurface` already implements the ledge and press offset. It gains a `graphicsLayer` scale driven by `animateFloatAsState` with a spring from `Motion.kt`. `cardShadow()` disappears once no flat surface uses it.

Flat panels on the activity color background use a lighter tint of that color, or translucent white, so the clock and text float on the screen instead of sitting in boxes.

### 4.2 Top bar and icons (D2)

- New `RoundIconButton(icon, contentDescription, onClick, selected = null)`: 48 dp circle, white fill at partial opacity, ink icon, press scale. No border.
- New icons in `SkIcons`, as `SvgIcon` paths like the existing ones: `Back` (arrow), `Settings` (gear), `Help` (light bulb, filled when on), `Listen` (speaker), `Shuffle` (random time), `Check` (selected badge), `Palette` (clock styles).
- `TopBar(title, onBack, trailing)` becomes: round back button left, centered title, optional round trailing button. The narrow and large-font fallback that already exists keeps the title on its own row.
- `HelpChip` becomes a light-bulb toggle with `selected` semantics and the existing `help_on`/`help_off` strings as its description.
- Text-labelled action buttons that a child presses often ("Listen", "Random time") get an icon plus a short label.

### 4.3 Edge to edge (D7)

- `MainActivity`: transparent navigation bar style and `window.isNavigationBarContrastEnforced = false` (API 29 and up), keeping dark system icons.
- `ScreenScaffold`: apply status bar and horizontal insets as padding, but add the navigation bar inset as bottom padding inside the scrolling column. Content then scrolls behind the navigation bar and the last item still clears it.
- Check gesture navigation and three-button navigation.

### 4.4 Activity color worlds (D6)

- Remove the dot pattern. The root `Box` in `ClocklingsApp` stops drawing a background.
- New `Section` enum in the theme package (`HOME`, `LEARN`, `EXPLORE`, `PLAY`, `TOGETHER`, `COMPANION`, `SETTINGS`), each with a background color and a soft top-to-bottom gradient. Values derive from the existing `KouluFill`, `VapaaFill`, `PelaaFill`, `KysyFill`, `NukkeFill`, lightened for readability.
- `ScreenScaffold(section = ...)` draws the section background. Levels, Game, Result, and Challenge use `PLAY`. Lesson list and Lesson use `LEARN`. Settings and Language use a calm neutral.
- The transition between sections then reads as moving to another place (see 4.10).

### 4.5 Home (D3, D4)

Target layout, from top to bottom:

1. **Header row:** small rainbow wordmark left, round settings button right. Tagline removed.
2. **Companion stage:** the selected companion large on a soft ground shadow, with a star counter pill (star icon and `3 / 15`). The companion greets on first appearance (existing greeting pose). Tapping the stage opens Appearance. With "Just the clock", the live clock stands on the stage instead.
3. **Live clock (D-1):** the live clock stays, smaller, beside the companion, with the time in words in the companion's speech bubble ("It's thirteen minutes to twelve"). It is no longer a separate card, and the "Right now" eyebrow is removed. With "Just the clock", the live clock is the stage itself and the time in words sits under it.
4. **Activity grid (D-3):** four big pressable tiles in a 2 × 2 grid: Learn, Explore, Play, Play Together. Icon and short title only. The visible subtitles are removed and move into `contentDescription`. The companion tile is removed because the stage replaces it.
5. **Clock styles:** reached from Appearance (it already has the tab) and from a small palette button on the stage. The footer chip is removed.

At default size (360 × 640 dp and up, 100% font) Home fits without scrolling. At 320 dp or 200% font it may scroll, with the grid falling back to one column as today.

### 4.6 Companion as guide

- New `CompanionTip(text, modifier)` component: a small `CompanionView` (head crop or full mini) with a speech bubble. The bubble is a flat surface with a tail. No edge stripe.
- Used for: the Explore legend ("The short red hand shows the hour"), Game feedback ("Try again", "Great"), Play Together instructions, and lesson captions where space allows.
- Correct answers trigger the existing `CompanionCelebration` hop. Wrong answers (D-5) get no new pose: the companion keeps its idle motion and the bubble shows the existing encouraging line. No sad or negative expression, and no rig changes.
- With companion `NONE`, `CompanionTip` renders today's `FeedbackBox` or legend box.
- On small screens the mini companion shrinks first, and the bubble keeps full text width.

### 4.7 Learn list and Levels as paths

Both screens use a winding vertical path (D-2).

- New shared `PathNode` component: a large circular pressable node, an optional mini clock or number inside, a status badge (check for done, stars for levels), and the title below. Nodes sit on a gently winding dotted path drawn behind them.
- **Learn list:** lessons as numbered nodes. The "Completed" pill becomes a check badge. The intro paragraph is removed.
- **Levels:** each level is a node showing its mini clock, three stars under it, the level name, and a small round "watch the lesson" button beside it. Tapping the node starts the level. The minute challenge is a distinct final node (trophy or stopwatch icon).
- The current level is highlighted with a gentle pulse (reduced motion: a static ring).
- Accessibility: each node is one button whose description includes the title, the example ("For example, half past two"), star count, and completion.

### 4.8 Appearance and Wardrobe pickers (D5)

- Tabs "Companions" and "Clock styles" become a two-segment control using the existing `SegmentedControl` without its label.
- **Companions (D-4):** a `HorizontalPager` (Compose Foundation, no new dependency) with the large preview of each companion, and under it a compact horizontal row of small thumbnails (all six companions and "Just the clock") for direct jumps. The current grid is removed. Swiping the pager or tapping a thumbnail selects that companion. The selected thumbnail gets a check badge, and the large preview does a small hop. The "Selected" text line is removed, and the `selected` semantic stays.
- **Clock styles:** the theme grid stays, with check badge selection. The custom clock controls become visual: frame colors as round swatches (reuse the swatch tile from `WardrobeScreen`), frame patterns, numbers, and hands as mini `ClockFace` thumbnails. Text names stay as content descriptions.
- Intro paragraphs are removed. The hand explanation sentence moves into a `CompanionTip` on the custom clock section.

### 4.9 Settings and Language (D2)

- Grouped list style: a section heading, then one flat container with rows separated by thin dividers.
- Rows: the language row shows the title with the current value under it and a chevron, the 12 h / 24 h choice as radio rows, and switches for guides and sound with a one-line supporting text.
- "Reset progress" is a text-style destructive row at the bottom, opening the existing confirmation dialog.
- The calm neutral section background, no ledges, no gold selection rings.
- No adult gate is added. Settings stays reachable as it is today. Adult controls are a separate product topic in STATUS.md and outside this plan.

### 4.10 Motion and transitions

Tokens in `ui/Motion.kt`:

- `PressSpring`: stiffness medium, damping slightly bouncy, for press scale.
- `PopSpring`: bouncier, for badges, stars, and selection.
- `NavEnter`/`NavExit`: slide by a fraction of the width plus fade, about 300 ms equivalent.
- `ReducedFade`: short fade used when reduced motion is on.

Navigation:

- `NavDisplay` gets `transitionSpec`, `popTransitionSpec`, and `predictivePopTransitionSpec`. Forward: the new screen slides in from the end with fade. Back: reverse. Predictive back: the current screen scales down slightly and follows the gesture.
- Wrap `NavDisplay` in `SharedTransitionLayout` and pass its scope as `sharedTransitionScope`. Shared elements:
  - Home live clock → Explore clock (`Modifier.sharedElement` keyed `"clock"`, using `LocalNavAnimatedContentScope.current`).
  - Home companion stage → Appearance preview (key `"companion"`).
  - Level node mini clock → Game clock (key `"level-clock-$level"`).
- Reduced motion: transitions become `ReducedFade`, shared elements are skipped.

### 4.11 Haptics

Through `LocalHapticFeedback.current` and `HapticFeedbackType`:

| Moment | Type |
| --- | --- |
| Clock hand passes a minute step while dragging | `SegmentFrequentTick` (documented for dense choices such as clock minutes) |
| Hand passes a five-minute mark or the hour | `SegmentTick` |
| Correct answer, level complete | `Confirm` |
| Wrong answer | `Reject` |
| Switch toggled in Settings | `ToggleOn` / `ToggleOff` |

Implementation: `ClockState` already reports value changes through `onChange` during drag. Add a drag-only step callback (or compare old and new values inside `ClockFace`) and fire haptics there, not in the domain layer. Haptics follow the system haptics setting only (D-6). The app's sound toggle does not affect them, and no new setting is added.

### 4.12 Clock face depth

The clock is the toy, so it should feel like an object. The depth applies to all eight clock styles and the custom clock through shared drawing (D-7):

- Face: very soft radial gradient (white center, faint tint at the rim) and a thin inner shadow along the rim.
- Frame: subtle bevel highlight on the top half, darker on the bottom half, applied on top of every theme's ring art.
- Glass: one faint specular arc in the upper left, drawn last, never over the hand pivot or numbers' legibility.
- Hands: soft drop shadow offset down, in addition to the current drawing.
- Mini clocks use a reduced version (no glass) to stay crisp at 84 to 120 dp.
- All values are tokens. `RingRevealTest` and clock layout tests must keep passing.

### 4.13 Copy trimming (D4)

| Remove from UI | Keep as semantics | Notes |
| --- | --- | --- |
| `tagline` | App name heading | |
| `now_eyebrow`, `clock_is`, `level_eyebrow`, `challenge_eyebrow`, `ask_*_eyebrow` | Merged into the node or panel description where useful | `Eyebrow` is deleted if unused |
| `koulu_intro` and appearance intro paragraphs | No | Delete strings in en, fi, sv |
| Menu subtitles (`menu_*_sub`, `home_companion_choices`) | Yes, as tile descriptions | |
| `lesson_done` pill text | Yes, in node description | |
| "Selected" text under choices | `selected` semantic only | |
| `back` ("← Back") | Needs a new arrow-free description string `back_description` ("Back", "Takaisin", "Tillbaka") | The arrow must not be read aloud |

Run `tools/check-localization.ps1` after string changes.

## 5. File and interface map

| File | Change |
| --- | --- |
| `MainActivity.kt` | Transparent navigation bar, contrast enforcement off |
| `ClocklingsApp.kt` | No root dot background; `SharedTransitionLayout`; `NavDisplay` transition specs |
| `ui/Motion.kt` | Spring and transition tokens, reduced-motion helpers |
| `ui/theme/SkColors.kt` | Section colors and gradients, bubble, node, badge, bevel tokens; remove `Dots` |
| `ui/theme/SkDimens.kt` | Icon button, path node, stage, bubble sizes; remove dot pattern tokens; KDoc no longer refers to the web version |
| `ui/theme/SkShapes.kt` | Bubble shape, node shape |
| `ui/theme/Theme.kt` | Remove `dotBackground`; add `Section` and `sectionBackground()` |
| `ui/components/Buttons.kt` | `DepthSurface` press scale; remove `cardShadow`, `SkCard`; `BigMenuButton` without subtitle (subtitle to semantics) |
| `ui/components/Panels.kt` | `FlatPanel`; `HelpChip` replaced by help toggle |
| `ui/components/Layout.kt` | `TopBar` with icon buttons; `ScreenScaffold(section)` and inset handling |
| `ui/components/Icons.kt` | New icons |
| `ui/components/Display.kt` | `Eyebrow` removed if unused; option badges |
| New `ui/components/IconButtons.kt` | `RoundIconButton` |
| New `ui/components/CompanionTip.kt` | Companion speech bubble with fallback |
| New `ui/components/PathNode.kt` | Shared node and path drawing for Learn and Levels |
| `ui/home/HomeScreen.kt` | New layout (4.5) |
| `ui/lesson/LessonScreens.kt` | Path list; tip captions |
| `ui/levels/LevelsScreen.kt` | Path layout |
| `ui/freeclock/FreeClockScreen.kt`, `ui/ask/AskScreen.kt`, `ui/game/GameScreen.kt`, `ui/game/ResultScreen.kt`, `ui/challenge/ChallengeScreen.kt` | Flat panels, tips, icon actions, section backgrounds |
| `ui/appearance/AppearanceScreen.kt` | Segmented tabs, pager, swatches, thumbnails, check badges |
| `ui/companion/WardrobeScreen.kt` | Flat stage, check badges, shared swatch tile extracted for reuse |
| `ui/settings/SettingsScreen.kt` | Grouped list |
| `ui/clock/ClockFace.kt`, `ui/clock/ClockState.kt` | Depth drawing; drag step callback for haptics |
| `res/values*/strings.xml` and companions | Remove redundant strings, add descriptions, in en, fi, sv |
| `androidTest/` | Update text-based clicks ("← Back", "Help on", "Settings", "Selected", "Completed") to descriptions or tags; update screenshot tests |
| `docs/implementation/STATUS.md` | Record the change and validation evidence |

## 6. Implementation work packages

Each package ends in a buildable state with unit tests passing. Packages 1 to 4 are the foundation. Packages 5 to 12 depend on them but not on each other unless noted.

### WP0: Baseline

- [ ] Capture before screenshots of all main screens at 360 dp / 100%, 320 dp / 100%, 360 dp / 200%, 320 dp / 200% on the dedicated emulator, using the existing gallery and localization screenshot tests. Partial baseline galleries and the frozen baseline APKs were retained; the original complete baseline run stalled, as recorded in STATUS.md.
- [x] Record current instrumentation results as the regression baseline.

### WP1: Tokens

- [x] Add motion tokens to `Motion.kt` with reduced-motion variants.
- [x] Add section colors, gradients, bubble, node, badge, and bevel tokens to `SkColors`.
- [x] Add sizes to `SkDimens` and shapes to `SkShapes`.
- [x] Add `Section` and `Modifier.sectionBackground(section)` to `Theme.kt`.

### WP2: Surfaces

- [x] Add press scale to `DepthSurface` using `PressSpring`, skipped under reduced motion.
- [x] Add `FlatPanel` and migrate every `SkCard` and `Panel` use. Tappable cards (`DollCard`, `ChoiceCard`) become `DepthSurface` tiles.
- [x] Delete `SkCard`, `Panel`, and `cardShadow` when unused.

### WP3: Top bar and icons

- [x] Add icons to `SkIcons`.
- [x] Add `RoundIconButton`.
- [x] Rewrite `TopBar` and the help toggle. Keep the narrow and large-font fallback.
- [x] Add `back_description` in en, fi, sv. Remove `back` if unused.
- [x] Update instrumentation tests that click "← Back", "Help on", and "Settings".

### WP4: Edge to edge and backgrounds

- [x] Transparent navigation bar and contrast enforcement off in `MainActivity`.
- [x] Inset handling in `ScreenScaffold` so content scrolls behind the navigation bar.
- [x] `ScreenScaffold(section)` draws the section background. Assign a section on every screen.
- [x] Remove `dotBackground`, `Dots`, `DotPatternStep`, and `DotRadius`.
- [x] Check the intro overlay and confetti layers still cover the full window.

### WP5: Home

- [x] Header row with wordmark and settings button.
- [x] Companion stage with star pill, greeting, and tap to Appearance. Clock fallback for `NONE`.
- [x] Smaller live clock beside the companion, time in words in the speech bubble (D-1).
- [x] 2 × 2 activity grid without subtitles, descriptions in semantics.
- [x] Remove the tagline, eyebrows, footer chip, and unused strings.
- [x] Verify no scrolling at 360 × 640 dp / 100% and an acceptable scroll fallback at 320 dp / 200%.

### WP6: Companion tip

- [x] Add `CompanionTip` with the `NONE` fallback to the current boxes.
- [x] Use it in Explore, Game feedback, Play Together, and lesson captions.
- [x] Trigger celebration on correct answers through the existing `CompanionCelebration`.

### WP7: Paths

- [x] Add `PathNode` and path drawing.
- [x] Convert the Learn list. Replace the "Completed" pill with a check badge.
- [x] Convert Levels, including the lesson button and the challenge node.
- [x] Add the current-level pulse with the reduced-motion ring.
- [x] Node semantics include title, example, stars, and completion.

### WP8: Pickers

- [x] Segmented tabs in Appearance.
- [x] Companion pager with the thumbnail row, grid removed, check badges, no "Selected" text (D-4).
- [x] Extract the swatch tile from `WardrobeScreen` and reuse it for frame colors.
- [x] Thumbnails for frame pattern, numbers, and hands.
- [x] Remove intro paragraphs; hand explanation moves into a tip.

### WP9: Settings

- [x] Grouped list layout with headings, dividers, and supporting text.
- [x] Language row with value and chevron. Destructive reset row.
- [x] Keep `SettingsErrorTest` and `LanguageSettingsTest` semantics passing.

### WP10: Transitions and shared elements

- [x] Transition, pop, and predictive pop specs on `NavDisplay`.
- [x] `SharedTransitionLayout` and the three shared elements in 4.10.
- [x] Reduced motion: fade only, no shared elements.
- [x] Check that `rememberViewModelStoreNavEntryDecorator` and saveable state still behave during predictive back cancel.

### WP11: Haptics

- [x] Drag step callback in `ClockState` or `ClockFace`, firing `SegmentFrequentTick` and `SegmentTick`.
- [x] `Confirm` and `Reject` on answers in Game, Lesson quiz, Challenge, and Play Together reveal.
- [x] Toggle haptics on Settings switches.
- [x] Unit test the step detection (which value changes produce which tick) without Android.

### WP12: Clock depth

- [x] Face gradient, rim inner shadow, frame bevel, glass arc, and hand shadow as tokens in `ClockFace`.
- [x] Reduced version for mini clocks.
- [x] Visually check all eight clock styles and the custom clock options at full and mini size.

### WP13: Copy and cleanup

- [x] Remove the strings in 4.13 from en, fi, sv and run `tools/check-localization.ps1`.
- [x] Remove `web:` references from component KDoc where the component no longer mirrors the web version.
- [x] Grep for unused components, tokens, and resources and delete them.
- [x] Update `STATUS.md` and, if architecture notes change, the project memory.

## 7. Validation and acceptance

### 7.1 Required checks

- `:app:testDebugUnitTest` and `:app:assembleDebug` with `--no-daemon`.
- `:app:ktlintCheck`, `:app:detekt`, and the Compose stability checks. Inspect stability baselines before updating them.
- Instrumentation suite on the dedicated emulator at 360 dp / 100%, plus relevant UI tests at 320 dp / 100%, 360 dp / 200%, and 320 dp / 200%.
- After screenshots of every screen in the same four cells, in English, Finnish, and Swedish for Home, Levels, Settings, and one Game screen.
- Reduced motion on: no spring, slide, pulse, or shared element animation.
- Gesture navigation and three-button navigation: content behind the transparent bar, nothing hidden.
- A TalkBack smoke pass over Home, Levels, Appearance, and Settings: every icon button and node is announced with a meaningful description.
- The owner runs `lc` and `sc`.

### 7.2 Acceptance criteria

| ID | Criterion |
| --- | --- |
| A1 | No non-tappable surface has a ledge or shadow. Every tappable primary surface sinks and springs back on press. |
| A2 | No text pill is used for back, settings, help, or clock styles navigation. |
| A3 | Home fits without scrolling at 360 × 640 dp / 100% font. |
| A4 | No screen shows the dot pattern. Each activity has its own background world. |
| A5 | Dragging a clock hand gives a tick per minute step on a device with haptics. |
| A6 | Forward, back, and predictive back navigation are animated, and reduced motion removes the motion. |
| A7 | Selected choices are shown with a badge, not text. Colors are chosen from swatches. |
| A8 | Settings reads as a standard Android settings list. |
| A9 | No word splitting, clipping, overlap, middle-dot or bar separators, or colored edge stripes in any validated cell. |
| A10 | Accessibility descriptions exist for every icon-only control in all three languages. |

## 8. Resolved design decisions

The owner delegated these choices on 26 October 2026. They are final for this plan, and no design question blocks any work package. Change them only on a new owner instruction.

| ID | Question | Decision | Reason | Applies to |
| --- | --- | --- | --- | --- |
| D-1 | Does Home keep the live "Right now" clock? | Yes. It becomes smaller and sits beside the companion, with the time in words in the companion's speech bubble. No separate card, no eyebrow. With "Just the clock", the live clock is the stage. | It shows the child real time every day and is the app's daily hook. Joining it to the companion saves a whole card of height, so Home fits without scrolling. | 4.5, WP5 |
| D-2 | Levels and Learn as a winding path or a horizontal pager? | Winding vertical path for both. | It scrolls naturally, survives 200% font and 320 dp, keeps all levels and progress visible at once, and needs no hidden swipe gesture. | 4.7, WP7 |
| D-3 | Remove the menu tile subtitles? | Yes, from view. They stay as content descriptions. | Icon, color, and a short title are enough for children. TalkBack users keep the full meaning. | 4.5, 4.13, WP5 |
| D-4 | Companion picker: pager only, or pager plus grid? | Pager plus a compact thumbnail row for direct jumps. The current grid is removed. | Swiping big previews feels native and playful, and the thumbnails keep every choice one tap away without a long grid. | 4.8, WP8 |
| D-5 | How does the companion react to a wrong answer? | No new pose. Idle motion continues and the bubble shows the existing encouraging line. | Wrong answers must never feel like punishment. A new pose would need rig work, which is outside this plan. | 4.6, WP6 |
| D-6 | Does the app's sound toggle also mute haptics? | No. Haptics follow the system haptics setting only. | Sound and touch are separate senses; a child playing silently still benefits from ticks. No new setting is needed. | 4.11, WP11 |
| D-7 | Clock depth on all eight styles, or opt-in? | All styles, through shared drawing. Mini clocks use the reduced variant. | The clock family stays consistent, and no new option is added to the picker. | 4.12, WP12 |

## 9. Risks

| Risk | Mitigation |
| --- | --- |
| Large-font and 320 dp layouts break with denser icon rows | Keep the existing `MAX_ROW_FONT_SCALE` fallbacks; validate every package in all four cells |
| Shared element transitions conflict with ViewModel-scoped entries or predictive back cancel | Introduce in WP10 only after the plain transitions work; cover back cancel in a device test |
| Text removal reduces accessibility | Every removed visible label becomes a description; TalkBack smoke pass in 7.1 |
| Haptics feel noisy during fast drags | Fire only on value changes after snapping; throttle to the frame rate if needed |
| Clock depth reduces legibility on mini clocks | Mini variant without glass; visual check at 84 dp |
| Many instrumentation tests click visible text | Update tests in the same package that changes the label; prefer tags and descriptions |
| Scope creep into new features | Section 3.2 is binding; new ideas go to the owner as separate proposals |

## 10. Sources and evidence limits

- Owner screenshots, 26 October 2026: Home, Settings, Appearance (companions and clock styles), custom clock controls, Learn list, Explore, Levels, Game, and Play Together screens.
- Repository inspection: `MainActivity.kt`, `ClocklingsApp.kt`, `ui/theme/`, `ui/components/`, `ui/home/HomeScreen.kt`, `ui/clock/ClockState.kt`, `ui/clock/ClockFace.kt`, screen files, string resources, and instrumentation tests.
- [Navigation 3 UI package reference](https://developer.android.com/reference/kotlin/androidx/navigation3/ui/package-summary): `NavDisplay` transition parameters, `PredictivePopTransitionKey`, `LocalNavAnimatedContentScope`.
- [Navigation 3 destination animations guide](https://developer.android.com/guide/navigation/navigation-3/animate-destinations): wrapping `NavDisplay` in `SharedTransitionLayout` and passing its scope.
- [Compose `HapticFeedbackType` reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType): `SegmentFrequentTick`, `SegmentTick`, `Confirm`, `Reject`, `ToggleOn`, `ToggleOff`.
- The `navigation3-ui` 1.2.0 artifact in the local Gradle cache was checked for `sharedTransitionScope`, `predictivePopTransitionSpec`, and `LocalNavAnimatedContentScope`. Full signatures were not decompiled.
- Not established: how these haptic types feel on the dedicated emulator (emulators usually give no haptic output, so A5 needs a physical device the owner chooses), actual frame performance of the clock depth drawing, and children's comprehension of icon-only navigation.
