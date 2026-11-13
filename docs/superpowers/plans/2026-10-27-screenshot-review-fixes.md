# Screenshot Review Fixes: Implementation Plan

Date: 2026-10-27
Source: owner review of the Finnish UI screenshots in the `clocklings-screenshots4` set (18 images, 12:55 to 12:59).
Audience: the implementing agent (Codex). The owner approved every finding below unless it is listed under "Dropped findings".

## 0. Ground rules (read before touching code)

- Follow `AGENTS.md` in the repository root. It overrides anything here.
- Work only in `C:\Dev\Clocklings`. Never touch `C:\Dev\Sateenkaarikello-Android`.
- The current branch is `codex/localization-settings` and the working tree contains a large amount of uncommitted owner work. Do not reset, stash, checkout, clean or discard anything. Make your edits on top of the working tree. Do not commit, push or open a PR.
- Every user-visible string change must be made in all three languages: `app/src/main/res/values/` (English), `values-fi/` (Finnish) and `values-sv/` (Swedish). English is the source; Finnish and Swedish must say the same thing naturally, not word for word.
- Never use `·`, `•` or ` | ` as a separator in UI text. Never add a coloured vertical stripe to the left edge of a card, row or panel.
- Use existing theme tokens (`SkColors`, `SkDimens`, `SkShapes`, `SkType`). If a new value is needed, add it as a token in the theme file, not inline.
- No duplicated logic. If you write the same 3+ lines twice, extract a shared function. Delete code that becomes unused in the same change.
- Keep identifiers and code comments in English. Existing Finnish comments may stay; do not translate them as a side task.
- Run Gradle only from the repository root, with `--no-daemon`.
- The owner runs `lc` and `sc` (lint and security). Do not run them and do not claim they passed.

## 1. Dropped findings (do NOT implement)

These were in the review but turned out not to be defects:

- Finding 4 ("help-marks setting does not match the screens"): the owner toggled the setting manually while taking screenshots. Not a bug.
- Finding 10 ("helper marks differ between challenges"): same cause. The light-bulb button in the game top bar (`HelpToggle` in `ui/components/Panels.kt`) toggles the same global `Settings.help`. Finding 11 below covers the real problem, which is that its state is hard to read.
- Finding 22 ("worn item is not distinguishable"): verified in `ui/companion/WardrobeScreen.kt`. A worn item already gets `SkColors.ItemOnBg`, an `ItemOnBorder` border and a `SelectionBadge`. In the screenshot "Rusetti" was unlocked but not worn. No change.
- Finding 31 ("locked vs open lessons look the same"): all lessons and levels are intentionally open, and nodes ahead of the current one are already faded (`AHEAD_FADE` in `ui/components/PathNode.kt`). No change.
- Finding 36 ("primary button is always pink"): keep it. Pink `ButtonKind.PRIMARY` is the single app-wide primary action colour. Document this in `docs/implementation/STATUS.md` under decisions (one sentence).

## 2. Owner decisions made in this plan

### 2.1 Opi lessons without a matching Pelaa level (finding 9)

Investigation: `domain/Lessons.kt` `Lessons.levelLessons` already groups the 8 lessons under the 5 levels:

- Level 0 Tasatunnit: `viisarit` (Kaksi viisaria), `kierros` (Yksi kierros), `tasan` (Tasatunnit)
- Level 1 Puolet: `puoli` (Puoli tuntia)
- Level 2 Vartit: `vartti` (Vartit), `ylivaille` (Yli ja vaille)
- Level 3 Viiden minuutin välein: `viisi`
- Level 4 Tarkat minuutit: `minuutit`
- Plus `vrk` (24 h lesson, `h24Only`), shown only in 24 h mode.

Every lesson also ends in its own quiz (`Lesson.quiz`). So "Kaksi viisaria", "Yksi kierros" and "Yli ja vaille" are already practised: inside their own quiz, and in the game of the level they belong to (Vartit and Viiden minuutin välein both generate "past" and "to" times).

Decision: do NOT add new levels or exercises. Instead, make the existing structure visible so the child and parent can see which lessons belong to which game level:

1. The Opi path is divided into 5 chapters, one per level, in level order. Each chapter starts with a small chapter header showing the level name (same string as in Pelaa, see 2.3) and uses the level colour (see 2.4) for all its lesson nodes.
2. Lesson nodes keep their own lesson titles ("Kaksi viisaria", "Yksi kierros", ...).
3. After the last lesson node of a chapter, add a "play" node (same `PathNode` component, star art like the Minuuttihaaste node, or a small play icon) titled with the level name and the existing string `lesson_start_game` as its action. Tapping it opens that level's game (`Screen.Game(level, ...)` via the same `playLevel` path that `ClocklingsApp.kt` already uses for the levels screen). This links every lesson group to its practice.
4. The 24 h lesson `vrk` stays where it is today (appended when 24 h mode is on), as its own last chapter without a play node.

Implementation details are in task C1.

### 2.2 What stars mean in the wardrobe (finding 23)

Investigation: `domain/Wardrobe.kt` `Item.star` is a threshold on `Progress.totalStars` ("kokonaistähtimäärä, jolla se avautuu"). `WardrobeScreen` unlocks an item when `item.star <= total`. Stars are never spent. `Progress.totalStars` is the sum of the best result per level (max 3 x 5 = 15 = `Wardrobe.MAX_STARS`).

Problem: the locked tile shows a pill with a star icon and a number ("4"), which looks like a price.

Decision:

1. Replace the price-looking pill on `LockedTile` with a progress pill: star icon plus "current / needed", for example "3 / 4" (use the same format as the existing `home_star_total` string, which renders "3 / 15"). Use `PillKind.MUTED` as today.
2. Under the wardrobe stage (once per screen, directly after `WardrobeStage`, only when `Wardrobe.lockedCount(kind, total) > 0`) show one short line in `SkType.SmallSoft`:
   - en: "Stars are never used up. New clothes open when you have enough stars."
   - fi: "Tähdet eivät kulu. Uusia asuja avautuu, kun tähtiä on tarpeeksi."
   - sv: "Stjärnorna tar aldrig slut. Nya kläder låses upp när du har tillräckligt med stjärnor."
3. Keep the accessibility description of a locked tile, but reword it to match: `doll_item_locked` becomes "Locked. Opens at %1$d stars. You have %2$d." (fi: "Lukittu. Avautuu, kun tähtiä on %1$d. Sinulla on %2$d."; sv: "Låst. Låses upp vid %1$d stjärnor. Du har %2$d."). Update the call site to pass both numbers. Keep `tools:ignore="PluralsCandidate"` if it is already there.

### 2.3 One name per level (finding 6)

Level 1 is called "Puolen tunnin ajat" in Pelaa and the game, but "Puoli tuntia" in Opi. Use the shorter lesson name everywhere: change `level_puolet` to the same text as `l_puoli_title` in all three languages (fi "Puoli tuntia"; check en and sv and copy the lesson title text). Then grep every `level_*` and `l_*_title` string in all three languages and confirm the five level names equal the corresponding lesson title where a lesson of the same topic exists (Tasatunnit, Puoli tuntia, Vartit, Viiden minuutin välein, Tarkat minuutit). Fix any other mismatch the same way.

### 2.4 One colour per level (finding 7)

`ui/theme/SkColors.kt` has two unrelated palettes: `LevelCards` (5 colours, used by `LevelsScreen`) and `LessonNodes` (6 colours cycled by index, used by the Opi list). Result: Tasatunnit is red in Pelaa and yellow in Opi, and so on.

Decision: `SkColors.LevelCards` becomes the single source of truth for level identity. Opi lesson nodes take the colour of the level they belong to (from `Lessons.levelLessons`). The `vrk` lesson uses `SkColors.ChallengeCard`. Delete `SkColors.LessonNodes` once nothing uses it. Implementation in task C1.

## 3. Tasks

Tasks are grouped into phases. Do them in this order; each phase should compile and pass unit tests before the next one starts. Original finding numbers from the review are in brackets.

---

### Phase A: Clock face

#### A1. Numerals collide with hour ticks and hands [3, 18]

Files: `ui/clock/ClockSpec.kt`, `ui/clock/ClockFace.kt`.

Facts (ClockSpec units, 400x400 face, centre 200):
- `NUMBER_RADIUS = 121`, `NUMBER_FONT = 42`. Big ticks span radius 145 to 162 (`TICK_BIG_INNER`, `TICK_OUTER`).
- Two-digit numerals at 10, 11, 1, 2 o'clock reach about radius 148, past `TICK_BIG_INNER`, so they touch the ticks ("\10", "11", "2´" in the screenshots).
- Minute hand: `MINUTE_LENGTH = 150`, `MINUTE_WIDTH = 10`, arrowhead length `1.7 * width = 17`, so with `HandStyle.POINTED` the arrowhead occupies radius 133 to 150 and its 24-unit-wide base sits over the outer half of the numerals. That is why the arrowhead covers 12, 6, 2 and 1 in the screenshots. The comment on `MINUTE_LENGTH` ("the tip knob then sits outside the numerals") is only true for `HandStyle.ROUND`.

Steps:
1. Move the numerals inward and make them slightly smaller: start with `NUMBER_RADIUS = 110f` and `NUMBER_FONT = 39f`. These are starting values; tune them in step 4.
2. Keep `MINUTE_LENGTH = 150f` so the minute hand still reaches the minute ticks (that is a teaching requirement: the minute hand points at the minute ticks).
3. Update `GLOW_NUMBER_RADIUS` if needed so the number glow (lesson mark `Mark.NumberGlow`, drawn at `NUMBER_RADIUS`) still fully surrounds a numeral at the new size. Also check `INNER_24_RADIUS = 84` (24 h inner numbers): with the hour numerals now nearer the centre, the inner 24 h ring must still clear them. If it does not, reduce `INNER_24_RADIUS` and `INNER_24_FONT` together.
4. Acceptance geometry (check with real screenshots, see step 6):
   - No numeral glyph touches a big tick: at least 4 units of free face between the numeral's bounding box and radius 145 for all 12 numerals, including 10, 11 and 12.
   - With `HandStyle.POINTED`, the minute arrowhead does not overlap any numeral glyph at any of the 60 minute positions. If the starting values are not enough, reduce `NUMBER_RADIUS` further (not below 104) before touching the hand.
   - The hour hand tip (`HOUR_LENGTH = 92`) must not overlap a numeral either. Its arrowhead (length 1.7 x 16 = 27) starts at radius 65; with `NUMBER_RADIUS = 110` the inner edge of a numeral is near radius 90. If any overlap remains, shorten `HOUR_LENGTH` to 86 rather than moving the numerals more. The hour hand must stay clearly shorter than the minute hand.
5. Fix the `MINUTE_LENGTH` KDoc so it describes both hand styles truthfully.
6. Verification: render the big clock at 12:00, 12:05, 12:10, ... 12:55 (all twelve 5-minute positions) with both `HandStyle.ROUND` and `HandStyle.POINTED`, and also 10:50 and 11:55 (hands near two-digit numerals). Use the existing gallery instrumentation (`app/src/androidTest/java/fi/clocklings/GalleryTest.kt` and the procedure in the project memory note `clocklings-validation-setup`) on the dedicated test emulator. Also check the mini clocks on the Pelaa level list still look right (they use `MINI_VIEW_*` and round hands).

#### A2. Hands disappear on same-coloured backgrounds [19]

Files: `ui/clock/ClockFace.kt`.

Problem: the blue minute hand on the light-blue "vaille" half (`SkColors.Vaille`) and on the blue quarter sector of the Vartit lesson, and the pink hour hand on the pink quarter, lose contrast.

Steps:
1. In `drawHands`, before `drawHandBody` for each hand, draw a halo: the same hand geometry stroked/filled in `SkColors.Face` (the face colour) about 3 units wider on each side (`HAND_HALO = 3f` in `ClockSpec`). Use the existing `HandGeometry` path so pointed and round hands both get it. Apply the hand's alpha to the halo so dimmed hands stay dimmed.
2. Draw order per hand: shadow, halo, body (shadows of both hands first, as today, then halo+body of hour, then halo+body of minute, so the minute hand stays on top).
3. Do not change mark colours. The halo is the fix.
4. Verify with screenshots: Tutki with help on at 3:45 (minute hand on the blue half), and the Vartit lesson page 1 (quarter sectors).

#### A3. "Red" hour hand is pink [21]

`SkColors.Hour = 0xFFE64980` is pink, and every helper text calls it red. Change the colour word in the copy, not the colour.

Steps:
1. Grep all three languages for the hour-hand colour word: fi `punainen`, en `red`, sv `röd`/`röda`. Known Finnish keys: `legend_hour`, `hint_read_minute_also_hour`, `l_viisarit_p1b`, `l_viisarit_c1`, `l_viisarit_hint`, `custom_clock_hint` (in `personalization.xml`). There may be more; fix every one that refers to the hour hand.
2. Replace with pink: fi `pinkki` (inflect correctly, for example "Lyhyt pinkki viisari", "pinkki viisari"), en `pink`, sv `rosa`.
3. Do not change strings where "red" means something else (for example a red clock theme or red shoes item names).
4. If a quiz uses the colour word as an answer choice (`l_viisarit_c1`, `l_viisarit_c2`), keep the choices parallel ("Lyhyt pinkki viisari" and "Pitkä sininen viisari").

#### A4. Five-minute lesson: the hand does not move to the number being explained [20]

File: `ui/lesson/LessonCatalog.kt`, lesson id `viisi`, page 1 (starts at time 180 = 3:00).

Today the demo glows numbers 1 to 12 and says "Numero n tarkoittaa n*5 minuuttia" (`say_number_minutes`) while the minute hand stays at 12.

Steps:
1. In the page 1 demo loop, before `say(...)` for number n, add `move(180 + n * 5, 600, exact = true)` so the minute hand walks to the number it is talking about, wait with `pause(600)` for the hand to arrive, then keep the glow, bubble and `pause(500)` for reading. `DemoStep.Move` starts an animation without suspending the demo; only explicit pauses consume demo time. The implemented step therefore takes 1,100 ms, intentionally longer than the old 800 ms pause, rather than preserving that old budget.
2. After the loop the clock shows 4:00. That is acceptable (the hour hand has moved one hour, which is correct clock behaviour). If the replay button (`Näytä uudelleen`) restarts the page, confirm it resets to 180; `LessonPage.time` should already handle this. Verify on device.
3. Check page 2 of the same lesson, which already moves the hand; make sure the two pages do not now feel identical. If they do, leave page 2 unchanged anyway; the owner can review.

---

### Phase B: Layout bugs

#### B1. "Satunnainen aika" breaks mid-word in Pelaa yhdessä [1]

File: `ui/ask/AskScreen.kt` (around lines 131 to 150).

Today the random-time and Kysy buttons share a row whenever `adaptiveGridColumns(...) == 2`, and the Finnish label wraps as "Satunnaine / n aika".

Steps:
1. Always stack them: random-time button full width (same as `FreeClockScreen`, which uses `SkButton(stringResource(R.string.random_time), onRandom, Modifier.fillMaxWidth(), icon = SkIcons.Shuffle)`), then the Kysy primary button full width below it. Remove the `BoxWithConstraints` branch and the comment that describes the two-column case.
2. Use the same button size in both screens (drop `small = true` in Ask if FreeClock does not use it, or add it to both; pick whichever keeps the Ask card fitting on a 320 dp wide screen without scrolling at 100% font scale).
3. Give the Kysy button an icon so both buttons have one (see C6).
4. Ask yourself whether `SkButton` can ever break a word: it should not. If `SkButton` text uses plain `Text`, switch its label to the existing `WordSafeText` (already used for item names) so long words shrink or wrap at spaces instead of mid-word. Check all `SkButton` call sites still look correct after that change.

#### B2. Lesson navigation buttons hidden under the system navigation bar [2]

Files: `ui/components/Layout.kt` (`ScreenScaffold`), `ui/lesson/LessonScreens.kt`.

Problem: `Edellinen`/`Seuraava` are the last items inside the scrolling column, so on a normal phone they start under the 3-button navigation bar and the child must scroll to find the main action.

Steps:
1. Add an optional `bottomBar: (@Composable () -> Unit)? = null` parameter to `ScreenScaffold`. When present, render it after the weighted scroll area (outside the scroll), with horizontal padding `SkDimens.Gutter`, top padding `SkDimens.GapS`, and bottom padding equal to the safe-drawing bottom inset plus `SkDimens.GapS`. When a bottom bar is present, the scroll content must not add the bottom inset again (it currently adds `bottom` padding computed from `WindowInsets.safeDrawing`); only the bar handles the inset.
2. Give the bar a background matching the section background so scrolling content passes underneath cleanly, plus a soft top fade (reuse whatever fade the top bar already uses for the clock scrolling under the title; if there is none, a 12 dp vertical gradient from transparent to the section colour is enough, as a token in `SkDimens`).
3. In `LessonScreen`, move the `FlowRow` with `lesson_prev` and the next/primary button out of the `FlatPanel` into `bottomBar`. Lay them out as a `Row` with two `Modifier.weight(1f)` buttons of equal width (finding 28). When the next button is not shown (quiz not done), keep the previous button at half width on the start side, do not stretch it.
4. Check at 320 dp width, 200% font scale and both combined: the two buttons must stay side by side if their labels fit, otherwise stack (previous on top). Use `WordSafeText` behaviour from B1 so long labels like "Seuraava oppitunti" never break mid-word.
5. Other screens do not get a bottom bar in this task.

#### B3. Lesson playback buttons: uneven widths and mixed styles [12, 28]

Files: `ui/lesson/LessonScreens.kt` (`LessonPlayback`), `ui/components/Icons.kt`, `ui/components/IconButtons.kt`, `ui/freeclock/FreeClockScreen.kt`.

Decision: audio and replay are icon actions everywhere (pre-readers recognise icons; it matches the Tutki speaker button and saves vertical space on lesson pages).

Steps:
1. Add `SkIcons.Replay` to `Icons.kt` using the same `inkIcon(...)` path style as the other icons (a circular arrow; 24x24 viewBox like `Listen`).
2. In `LessonPlayback`, replace the two `SkButton`s with two `RoundIconButton`s (large variant, same as the Tutki speaker) in a `Row` with `SkDimens.GapM` spacing, aligned to the start: `SkIcons.Listen` with content description `listen`, and `SkIcons.Replay` with content description `lesson_replay`. Keep the `speechAvailable` condition.
3. Check the Tutki speaker button (`FreeClockScreen`) uses the same component and size after this change, so all audio controls in the app look the same. If other screens have a text "Kuuntele" button (grep `R.string.listen`), convert them too, unless the button is the only action in its context and the text is needed for clarity; list any you keep in your report.

#### B4. Speech-bubble tail floats above short bubbles [13]

File: `ui/components/CompanionTip.kt`, `SpeechBubbleBox` tail drawing, `BubbleTail.START` branch.

Cause: `y = size.height - TipCompanion * HEAD_HEIGHT`. When the bubble is shorter than the companion (one-line captions such as "2 minuuttia" or "Neljä varttia on yksi tunti"), `y` becomes close to or below 0 and the tail is drawn above the bubble.

Steps:
1. Clamp the tail position: `y.coerceIn(cornerRadius + t, size.height - cornerRadius - t)`, where `cornerRadius` is the bubble shape corner in px. If the bubble is so short that the range is empty, use `size.height / 2`.
2. In the non-compact `Row` layout, if the bubble is shorter than the avatar, the tail would now point at the companion's body instead of the head. To keep it pointing at the head, change the row's vertical alignment so that a short bubble is aligned with the head: wrap the bubble in a `Box` with `Modifier.weight(1f)` and align the bubble with a top padding equal to `TipCompanion * (1 - HEAD_HEIGHT) - bubbleHalfHeight`, or simpler: switch the row to `Alignment.Top` and add a top padding to the bubble of `TipCompanion * HEAD_OFFSET_TOP` (new token) so the first line sits level with the head, and draw the tail at that fixed distance from the bubble top instead of from the bottom. Choose the simpler variant that makes the tail touch the bubble edge for 1-line, 2-line and 4-line bubbles.
3. Verify with screenshots of: Tutki legend (long), Pelaa yhdessä (4 lines), Vartit lesson caption (1 line), Tarkat minuutit caption (1 line). All tails must sit on the bubble's left edge and point at the companion's head.

#### B5. Path dots run behind the "Opi" title and behind the book buttons [29, 30]

Files: `ui/components/PathNode.kt`, `ui/components/Layout.kt`, `ui/levels/LevelsScreen.kt`.

Steps:
1. Title overlap (Opi, 125716): the scroll content is visible behind the top bar. Give the top bar area of `ScreenScaffold` the section background plus the same short fade as in B2 (bottom edge of the top bar), so scrolled content passes under it instead of mixing with the title. Apply generally in `ScreenScaffold` so every scrolling screen benefits; check the lesson screens, which already fade the clock under the title, still look the same or better.
2. Book buttons over the dotted path (Pelaa, 125732, rows "Puolen tunnin ajat" and "Viiden minuutin välein"): on rows where the node is on the end side, the stars row and the book button are placed right next to the node, exactly where the dotted connector curves. Fix in `PathNode`: compute the connector from the node edge so it leaves the node on the side away from the text and badge column, or move the badge content (stars + book) to sit under the title text aligned with the title's start edge for end-side nodes, so the connector's corridor stays free. Pick one; the acceptance criterion is that no dot is drawn under or over a button on any row at 320 dp and at 411 dp width.

#### B6. Digital-time chips start at different x positions [27]

File: `ui/components/ReadOptions.kt` (or wherever `OptionButton` lays out the words and the digital chip; grep `OptionButton`).

Steps:
1. Lay out the option as a `Row`: words with `Modifier.weight(1f)`, then the digital chip aligned to the end. All chips in an option list then line up on the right edge.
2. At 200% font scale or 320 dp, if words plus chip do not fit, the words wrap (word-safe), the chip stays at the end, vertically centred.
3. Check every screen that uses the option list: game, lesson quiz, challenge.

#### B7. Long names shrink inconsistently in the wardrobe [17]

File: `ui/companion/WardrobeScreen.kt` (`ItemTileContent`, `LockedTile`), and the `WordSafeText` implementation.

Problem: "Merimieshattu" renders smaller than "Tupsupipo" because `WordSafeText` shrinks a single long word to fit.

Steps:
1. Within one `ItemGrid`, all item names must use the same text size. Implement a shared size: measure all names of the grid with `rememberTextMeasurer` against the tile's text width, take the smallest size any name needs (never below a minimum token, for example `SkType.ItemName` size minus 4 sp, defined in `Type.kt`), and pass that style to every tile in the grid.
2. Alternatively, if `WordSafeText` already supports a fixed style with hyphen-free wrapping at syllable boundaries, use 2-line wrapping without shrinking. Prefer the shared-size approach; it keeps tiles the same height.
3. Verify with the cat (Merimieshattu), the dog and the robot wardrobes at 320 dp and 200% font scale.

#### B8. Number and unit split across lines [32]

Files: string resources in all three languages.

Steps:
1. In every string where a number placeholder or a numeral is followed by a unit word (minuuttia/minuutin/minutes/minuter/tähteä/stars/stjärnor/tunti/hour/timme), replace the space between number and unit with a non-breaking space `\u00A0`. Known keys: `say_number_minutes`, `l_vartti_p*` ("15 minuuttia"), `l_puoli_p1b` ("30 minuuttia"), the plurals used for "2 minuuttia" captions in the `minutit` lesson. Grep `%1$d`, `%2$d` and digits, checking for a following ordinary space in `values*/strings.xml`.
2. Do not use non-breaking spaces anywhere else.

#### B9. Empty space on home, challenge and Ask screens [25, 26]

Files: `ui/home/HomeScreen.kt`, `ui/components/Layout.kt` (`CLOCK_HEIGHT_SHARE`), `ui/game/GameScreen.kt`, `ui/challenge/ChallengeScreen.kt`, `ui/ask/AskScreen.kt`.

Steps:
1. Home (125646): about a third of the screen above the companion is empty sky. Move the stage content up so the companion and clock sit closer to the header (reduce the top spacer or the stage's top offset). Make the companion and the home clock larger if that keeps the four menu tiles fully visible without scrolling on a 360 x 640 dp screen at 100% font scale. Keep the clouds.
2. Game/challenge read screens (125748, 125755, 125827) and Ask (125724): the area below the option card is empty. Let the clock use more height on these screens: either increase `CLOCK_HEIGHT_SHARE` for screens without a bottom bar, or add a per-screen override parameter to `ScreenScaffold`. Target: the clock plus the option card fill the viewport on a 411 x 914 dp phone without scrolling, and nothing is cut off on 360 x 640 dp (scrolling is acceptable there).
3. Do not change lesson screens here; B2 handles them.

---

### Phase C: Consistency

#### C1. Opi path grouped by level, with level colours and play nodes [7, 9]

Files: `ui/lesson/LessonScreens.kt` (lesson list, around line 80 to 110), `ui/lesson/LessonCatalog.kt`, `domain/Lessons.kt`, `ui/theme/SkColors.kt`, `ui/components/PathNode.kt`, `ClocklingsApp.kt`.

Steps:
1. Add a pure function in `domain/Lessons.kt`: `fun levelOf(lessonId: String): Int?` returning the index in `levelLessons` (null for `vrk`). Add a unit test covering all 9 lesson ids.
2. Make sure the lesson list order follows `levelLessons` order (flattened). If `LessonCatalog` order differs, sort by `levelOf` then by position within the level, `vrk` last.
3. Node colour: `SkColors.LevelCards[levelOf(id)]`, or `SkColors.ChallengeCard` for `vrk`. Delete `SkColors.LessonNodes` and its KDoc once unused.
4. Chapter header: before the first lesson of each level, render a small header row (not a path node): the level name from `levelName(index)` in `SkType.SectionTitle` (or the existing eyebrow style), marked as a heading for accessibility. The dotted connector should pass behind or stop at the header and continue below it; keep it simple: end the connector at the last node of a chapter and start a new one at the first node of the next chapter.
5. Play node: after the last lesson of each level chapter add a `PathNode` with star art (reuse the art used for the challenge node in `LevelsScreen`), title = level name, description = `lesson_start_game`, and the level's stars as a badge (same star row as on `LevelsScreen`, extract that row into a shared composable if it is not one already, do not duplicate it). Its click calls the same function that the levels screen uses to start a level (`playLevel(level, ...)` in `ClocklingsApp.kt`); pass a new callback `onPlayLevel: (Int) -> Unit` into the lesson list screen.
6. Path numbering: lesson nodes currently show `index + 1`. Keep numbers for lessons, counted across the whole path (1 to 8), not restarting per chapter, so they match today's numbers. Play nodes have no number.
7. `completed` and `current` logic stays per lesson. The play node is `completed` when the level has 3 stars and never `current`.
8. Update `docs/implementation/STATUS.md` with the decision from section 2.1.

#### C2. Pelaa screen title [8]

File: `ui/levels/LevelsScreen.kt`, string `levels_title`.

Every other home tile opens a screen titled with the tile's own name. Change the Pelaa screen title to the tile label: use the same string resource as the home "Pelaa" tile (grep the home menu strings, for example `menu_pelaa`) instead of `levels_title`. If `levels_title` becomes unused, delete it from all three languages. If it is still used elsewhere (for example as an accessibility label), keep it there.

#### C3. Help (light-bulb) toggle state is unreadable [11]

Files: `ui/components/Panels.kt` (`HelpToggle`), `ui/components/IconButtons.kt` (`RoundIconButton`), `ui/theme/SkColors.kt`.

Today on and off differ only by a filled versus outlined bulb icon (`SkIcons.HelpOn` vs `SkIcons.Help`) on the same white circle.

Steps:
1. When `selected == true`, `RoundIconButton` gets a clearly different surface: a warm yellow fill (add `SkColors.HelpOnFill`, close to the bulb yellow, and a matching ledge colour via `ledgeColor`) and the filled bulb. When off: the normal white surface and the outlined bulb.
2. This affects every `RoundIconButton` that passes `selected`; grep the call sites and confirm the new selected style makes sense for each, or limit the new colour to `HelpToggle` by passing the fill explicitly.
3. The content descriptions `help_on`/`help_off` stay. Verify TalkBack announces the state (the button already sets `selected` semantics).

#### C4. Speech-bubble punctuation and number style [14, 15]

Files: string resources, all three languages.

Rules (write them as a short comment at the top of each `values*/strings.xml` lesson section, in English):
- A caption that is a full sentence (has a verb) ends with a period. A bare label (a number, "2 minuuttia", "20", a time in words) has no period.
- Quantities with units use digits ("5 minuuttia", "15 minuuttia", "30 minuuttia"). Counting words stay as words only in counting phrases where the number is not a measurement ("Neljä varttia on yksi tunti" stays, it names a count of quarters).

Steps:
1. Apply to at least: `l_vartti_say_four` (add period), `say_number_minutes` (add period), `l_puoli_say_half` (add period), `l_minuutit_p1b` (fi: "Vierekkäisten numeroiden välillä on 5 minuuttia." with a non-breaking space between 5 and minuuttia; adapt en/sv the same way). Then review every `l_*_say_*` and every `say_*` string in all three languages against the rules.
2. Do not change strings that are shown as quiz answer choices unless they break the rules.

#### C5. Minute-number bubbles in the five-minute lesson [16]

File: `ui/clock/ClockFace.kt` (`drawMark` for `Mark.Bubble`).

Keep the circled bubbles (they are an animated teaching highlight and should stand out), but make the text inside use the same font family, weight and colour as the help minute labels (`styles.minute`) at `BUBBLE_FONT` size, and make the bubble circle radius large enough that "55" does not touch the stroke. Check that a bubble at minute 5 does not overlap the clock rim more than the minute labels do (both sit at `MINUTE_LABEL_RADIUS`).

#### C6. Kysy button and Pelaa yhdessä icon [35]

Files: `ui/ask/AskScreen.kt`, `ui/components/Icons.kt`, `ui/home/HomeScreen.kt`, strings.

1. Kysy button: add an icon so it matches the random-time button. Use a hand-over or "pass the phone" meaning. Add `SkIcons.Pass` (two arrows forming a loop, or an arrow pointing to the right into a phone outline) using the existing `inkIcon` style.
2. Relabel `ask_go` to say what happens: fi "Anna parille" (en "Pass to partner", sv "Ge till din kompis"). Keep it short enough to fit a full-width button at 200% on 320 dp. Update the instruction text `ask_big_text` to use the new label in quotes-free form ("Napauta sitten Anna parille ja ojenna kello parillesi." or similar natural Finnish; the owner will review).
3. Home tile icon for Pelaa yhdessä: replace the question-mark speech bubbles with an icon showing two people or two hands. Add the icon in `Icons.kt` (same stroke style as the other home tile icons). Keep the tile colour.

#### C7. Hand colour names [21]

Covered by A3. No extra work.

---

### Phase D: Wardrobe

#### D1. Star progress on locked items [23]

Implement section 2.2.

Files: `ui/companion/WardrobeScreen.kt` (`LockedTile`, `WardrobeScreen`), strings in all three languages.

Steps:
1. `LockedTile` gets a new parameter `total: Int`. Its pill text is `stringResource(R.string.home_star_total, total, item.star)` (reuse the existing format; do not create a duplicate format string).
2. Add the explanatory line from 2.2 as a new string `doll_stars_explained`, shown once after `WardrobeStage` when `Wardrobe.lockedCount(kind, total) > 0`.
3. Update `doll_item_locked` to the two-argument text from 2.2 in all languages and pass `item.star, total`.
4. Update `PersonalizationGalleryTest` or `GalleryTest` assertions that look for the old pill text, if any.

#### D2. Discoverable route to the wardrobe from home [24]

Files: `ui/home/HomeScreen.kt` (`HomeStage`), `ui/components/Icons.kt`, strings.

Today the only route is tapping the whole home stage (`HomeDestination.NUKKE`), which opens `Screen.Personalize`, and from there the wardrobe. There is no visible hint.

Steps:
1. Add a visible round icon button anchored to the companion (bottom end corner of the companion area, overlapping the ground), using `RoundIconButton` with a new `SkIcons.Hanger` (clothes hanger, `inkIcon` style). Content description: existing `menu_nukke` string. Click: `onOpen(HomeDestination.NUKKE)`.
2. Keep the whole-stage tap as it is (it is a bonus, not the only path).
3. When `companion == CompanionKind.NONE`, do not show the hanger button; show nothing new.
4. Check that the button does not cover the speech bubble or the clock at 320 dp and 200% font scale.

---

### Phase E: Settings

#### E1. Switch off state is nearly invisible [5]

Files: `ui/settings/SettingsRows.kt` (`SettingsSwitch`), `ui/theme/SkColors.kt`.

`Switch(checked, onCheckedChange = null, enabled = enabled)` uses Material defaults mapped from the app colour scheme, which gives a pale pink thumb on a pale grey track (contrast well below 3:1).

Steps:
1. Pass `colors = SwitchDefaults.colors(...)` with explicit tokens:
   - checked: track `SkColors.Hour` (as today), thumb white.
   - unchecked: track `SkColors.Surface`, border `SkColors.InkSoft`, thumb `SkColors.InkSoft`.
   - disabled variants: same as above at reduced alpha, but still distinguishable from enabled-off.
   Add any missing token to `SkColors` (for example `SwitchOffBorder`) rather than inlining a colour.
2. Verify non-text contrast of the unchecked thumb and border against the card background is at least 3:1. Report the measured ratio.
3. If `MaterialTheme.colorScheme` in `ui/theme/Theme.kt` is the reason the defaults are pale (for example `outline` or `surfaceContainerHighest` mapped to a very light colour), fixing the scheme there is acceptable instead, as long as no other Material component regresses. Prefer the local `SwitchDefaults.colors` fix.

#### E2. Help-marks description is hard to read [33]

Strings: `settings_guides_*` in `values*/settings.xml`.

Rewrite the description as two short sentences:
- fi: "Näyttää minuuttiluvut ja kellon puoliskot värillä. 24 tunnin tilassa myös luvut 13–24."
- en: "Shows the minute numbers and colours the two halves of the clock. In 24-hour mode, also the numbers 13–24."
- sv: "Visar minuttalen och färgar klockans två halvor. I 24-timmarsläge även talen 13–24."
Check what the 24 h help actually draws (`drawHelp` with `is24`) and adjust "13–24" if it draws something else.

#### E3. "Ääneen lukeminen" row is a different kind of row [34]

Files: `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsRows.kt`, `values*/settings.xml`.

The row has a title and two supporting lines and no control. It is information, not a setting.

Steps:
1. Remove the row from the Ääniasetukset card's control rows. Put one line of `SkType.SmallSoft` text below the card instead, combining availability and behaviour:
   - available, fi: "Ääneen lukeminen on käytössä tällä kielellä. Puhe alkaa vain, kun napautat kaiutinta."
   - unavailable, fi: "Ääneen lukeminen ei ole käytössä tällä kielellä."
   Write matching en and sv. Note the text now says "kaiutinta" (speaker) because B3 turns Kuuntele into a speaker icon; keep the wording consistent with B3.
2. Delete the old strings `settings_speech_title`, `settings_speech_available`, `settings_speech_unavailable` and the old behaviour line if they become unused. Grep tests (`SettingsErrorTest`, `LanguageSettingsTest`) for these ids and update them.

---

## 4. Validation

Run in this order and report results exactly (pass/fail, counts, any failures with output):

1. `./gradlew --no-daemon :app:testDebugUnitTest`
2. `./gradlew --no-daemon :app:assembleDebug`
3. Instrumented tests on the dedicated test emulator only (see project memory `clocklings-validation-setup`; do not use a physical device or any other running emulator): run the affected test classes at minimum: `AppFlowTest`, `ChallengeScreenTest`, `GalleryTest`, `PersonalizationGalleryTest`, `LanguageSettingsTest`, `SettingsErrorTest`, `NativeFeelUiTest`, `ClockSurfaceStateTest`.
4. Stability baseline: if `:app:stabilityCheck` fails because of new composable parameters, run `:app:stabilityDump`, inspect that no new parameter is unstable, and report it.
5. Screenshots: capture and keep for the owner, in Finnish, English and Swedish where text changed:
   - Home, Opi path (top and bottom), Pelaa levels, Tutki with help on at 3:45 and 10:50, Pelaa yhdessä, a game read question at 12:10, lessons Vartit page 1, Viiden minuutin välein page 1 (mid-animation), Tarkat minuutit page 1, wardrobe for the cat, Settings.
   - Each of those at 320 dp width, at 200% font scale, and both combined (per `AGENTS.md`).
6. Report device evidence separately from source inspection. Stop every emulator or process you started; do not stop anything you did not start.

## 5. Report to the owner

Write the report in Finnish. Include: what was implemented per task id (A1 to E3), checks actually run with results, screenshots location, any task skipped or changed and why, and open questions. Do not commit.

## 6. Checklist

- [ ] A1 numerals and hands clearance
- [ ] A2 hand halo
- [ ] A3 hour hand is "pink" in all copy
- [ ] A4 five-minute lesson hand walks to each number
- [ ] B1 Ask buttons stacked, word-safe button labels
- [ ] B2 lesson bottom bar with equal-width previous/next
- [ ] B3 icon-only listen and replay everywhere
- [ ] B4 bubble tail clamped and pointing at the head
- [ ] B5 path dots clear of title and book buttons
- [ ] B6 digital chips aligned to the end
- [ ] B7 equal item name size per grid
- [ ] B8 non-breaking spaces between numbers and units
- [ ] B9 less empty space on home, game, challenge, Ask
- [ ] C1 Opi chapters, level colours, play nodes, `LessonNodes` deleted
- [ ] C2 Pelaa title equals tile label
- [ ] C3 help toggle on-state clearly visible
- [ ] C4 caption punctuation and number rules
- [ ] C5 minute bubble typography
- [ ] C6 Kysy icon and label, Pelaa yhdessä home icon
- [ ] D1 locked-item star progress and explanation
- [ ] D2 hanger button on home
- [ ] E1 switch off-state contrast
- [ ] E2 help-marks description
- [ ] E3 read-aloud info line
- [ ] Section 2.3 level names unified
- [ ] STATUS.md decisions updated (2.1, 2.2, 2.4, primary colour)
- [ ] Validation section 4 completed and reported
