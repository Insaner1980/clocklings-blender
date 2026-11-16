# Screenshot UX review fixes (2026-11-16)

Source: critical review of the 363 emulator captures in `reports/clocklings-emulator-screenshots-2026-11-16/`. Screenshot paths below are relative to that folder.

This plan is for the implementing agent (Codex). Read `AGENTS.md` and `docs/implementation/STATUS.md` first. All repository rules apply. The key rules are repeated here:

- Work on a new branch `codex/ui-review-fixes-2026-11-16`, created from the current branch. Do not commit, push or open a PR unless the owner asks for it separately.
- Keep identifiers, filenames and code comments in English. Put UI copy in Android resources. Every new or changed string needs all seven locales: `values`, `values-fi`, `values-sv`, `values-de`, `values-fr`, `values-es`, `values-pt-rBR`.
- Use existing theme tokens (`SkColors`, `SkDimens`, `SkType`, `SkShapes`). Add a new token rather than an inline value. Do not use middle-dot or pipe separators in UI. Do not add colored edge stripes.
- Tasks marked **OWNER DECISION** must not be implemented. List them in the final report as open questions.
- Report in Finnish at the end: what was implemented, which checks actually ran, and the open decisions or verification gaps.

## Verified non-issues (do not "fix")

- **Lesson clocks that do not match the text in captures.** Examples: quarter-hours pages 1 to 3 all at 3:00, half past page 4, 24 h page 2. These pages animate (`LessonCatalog.kt` `demo { move(...) }`), and the captures were taken before the motion ran. The end states are correct.
- **Faded status bar icons in `practice/*.png`.** `MainActivity.kt` already uses `SystemBarStyle.light`. This is capture timing. Only verify it on device.
- **Three stars on locked levels in `free/*.png`.** These come from synthetic test progress, not from a logic bug. Task T11 covers only the visual dimming.

## Phase 1: Clock legibility and task flow (highest priority)

### T1. Set-the-clock tasks must not start with the hands overlapping

Problem: `Levels.setTaskStart` (`domain/Levels.kt:144`) returns 0:00 (or 6:00 for whole-hour targets). At 0:00 both hands point at 12, so the pink hour hand is fully hidden under the blue minute hand. Evidence: `practice/level-1-set.png`, `level-2-set.png`, `level-3-set.png`, `level-4-set.png`, `level-5-set.png`. Children cannot see the hand they must drag.

Change:
- Keep the same 12 h or 24 h half (`half`).
- Start at a whole hour `H:00` where the hour hand sits at 3 or 9. The two hands are then 90 degrees apart and both grips are visible.
- Pick `H = 9` unless the target's hour position (`target mod 720 / 60`, also counting the next hour when target minutes > 30) is within 2 hours of 9. In that case pick `H = 3`.
- The start must never equal the target.
- Update the KDoc and the existing unit tests for `setTaskStart` (find them with grep under `app/src/test`). Add these cases:
  - target 9:00 → start 3:00
  - target 3:00 → start 9:00
  - target 8:45 → start 3:00
  - target 15:20 (24 h) → start 21:00
  - for every target in 0..1439 step 5: `start != target`, and the hour and minute angles differ by at least 60 degrees.

Acceptance: in every SET task the pink hand and its knob are fully visible at the start.

### T2. Draw the hour hand above the minute hand

Problem: when the hands nearly coincide, the shorter pink hour hand disappears under the blue minute hand. Example: 4:20 in `practice/level-4-read.png`.

Change: in `ui/clock/ClockFace.kt` `drawHands` (around lines 540 to 545), reorder the drawing to:
1. both shadows
2. the minute halo and body
3. the hour halo and body
4. the knobs, which stay last as today

The longer minute hand stays visible beyond the hour hand's tip.

Acceptance: at 4:20, 12:00, 6:30 and 3:15 both hands are distinguishable. Run any existing clock screenshot or Paparazzi-style tests, if present.

### T3. The 24 h day-part label must stay readable under the hands

Problem: `drawDayPart` runs before `drawHands`, so the hands cut the word. Evidence: "afte oon" and "eve ing" in `sheets/clock-styles.png`, "vening" in `lessons/09-vrk-quiz.png`. Lesson text tells the child to read this label ("Read the label in the centre"), so it must never be clipped.

Change:
- In `ClockFace.kt`, draw the day part (sun or moon icon and the word) after `drawHands`.
- Draw the word the same way `drawCoveredNumerals` redraws numerals: first a halo pass with `SkColors.Face` (stroke width `S.NUMERAL_HALO`, round join), then the fill.
- Give the sun or moon a `SkColors.Face` circle backdrop of radius `S.SUN_RADIUS + S.NUMERAL_HALO` so it reads over a hand.
- Do not move the label. The hands stay visible beyond the label because the hour hand (86) is longer than the label offset (about 60).

Acceptance: at 19:00, 15:30, 18:00 and 0:00 in 24 h mode the full word is readable, and both hands remain identifiable.

### T4. 24 h inner numbers (13 to 23, 0) are too small and too pale

Problem: `S.INNER_24_FONT` with color `SkColors.Hour24` renders at about 9 sp on a phone. Evidence: `main/03-explore-24h.png`, `lessons/09-vrk-page-1.png`.

Change:
- Raise `INNER_24_FONT` by about 30%.
- Darken `SkColors.Hour24` to reach at least 4.5:1 contrast against both the `SkColors.Yli` and `SkColors.Vaille` half fills.
- Check for collisions with the 1 to 12 numerals and the zone labels at 320 dp width. If they collide, reduce `INNER_24_RADIUS` slightly.

Acceptance: a 320 dp capture shows no overlaps. Contrast ratio is computed and reported.

### T5. Task feedback must never end up below the screen edge

Problem: the companion's hint, reveal or success bubble is clipped at the bottom of the viewport. Evidence: `practice/level-1-read-hint.png`, `level-1-read-reveal.png`, `level-1-read-correct.png`.

Change, in `ui/components/ClockTaskScaffold.kt` together with `FocusScroll.kt`, `ScrollViewport.kt` and `ScaffoldClockSizing.kt`:
- When the feedback content appears or changes, bring it fully into view with a `BringIntoViewRequester` and an animated scroll. Respect reduced motion: scroll without animation.
- If the layout is not scrollable at that moment, reduce the clock size so the card plus the feedback fits. Reuse the existing sizing logic in `ScaffoldClockSizing.kt` and do not add a second sizing system.

Acceptance: on a 1080×2400 emulator, at 320 dp width, at 200% font scale, and with both combined, the whole feedback bubble is visible without manual scrolling for READ hint, READ reveal, SET hint and SET reveal.

### T6. Lesson quiz options must not hide under the bottom bar

Problem: the third option is covered by the bottom button bar and the fade. Evidence: `lessons/02-kierros-quiz.png`.

Change: in `ui/lesson/LessonScreens.kt` and the shared scroll viewport, make the scroll content bottom padding equal to the measured bottom bar height plus `SkDimens.GapM`. Make sure the bottom fade does not cover the last interactive element when scrolled to the end.

Acceptance: every lesson quiz shows all three options fully, after scrolling at most once, in all four validation configurations.

## Phase 2: Rewards, progress and state clarity

### T7. Star requirement "15 / 4" reads as a fraction

Problem: locked outfit tiles show `home_star_total` ("%1$d / %2$d") with the child's total and the requirement, giving "15 / 4". Evidence: `free/04-wardrobe-p2.png`. The code is at `ui/companion/WardrobeScreen.kt:383`.

Change:
- Show only the requirement: the star icon plus `item.star`, as a new string `item_star_need` = "%1$d". The icon carries the meaning.
- Add a plural `item_star_need_description`, for example "Needs %1$d stars", for the content description.
- When the requirement is already met and only the full version blocks the item, hide the star pill and show only the existing "Full version" line.
- Translate both strings into all seven locales.

Acceptance: a locked tile never shows two numbers. TalkBack reads "Needs 4 stars".

### T8. Header star total "15 / 15" (OWNER DECISION)

The home and wardrobe header pill reads like a score out of 15, while 15 is the maximum in the free version. Proposed: show "15" with the star icon, and keep "of 15" only in the content description. Ask the owner before changing.

### T9. Result screen: show all three star slots from the first frame

Problem: in `ui/game/ResultScreen.kt` `PoppingStar` (around line 266), earned stars start at scale 0, while unearned outline stars show immediately. The first impression is "nothing earned" and an off-centre row. Evidence: `practice/result-1-stars.png`, `result-2-stars.png`, `result-3-stars.png`.

Change: in each slot, always draw the `StarOff` outline at full scale, and draw the popping `StarOn` above it. Keep the sound and stagger timings unchanged. With reduced motion, show the final state immediately (as today).

Acceptance: a capture at any moment shows three evenly spaced slots.

### T10. Confetti must not cover result text

Problem: on a new record, confetti draws over "New best! You answered 31 correctly." Evidence: `play/07-challenge-record.png`. The relevant code is `ui/challenge/ChallengeScreen.kt` (`confettiAnchor`, around line 152) and `ui/components/Confetti.kt`.

Change: draw the confetti layer below the result card (lower z-order). If that is not possible, clip it so it never enters the card bounds. Apply the same rule in `ResultScreen.kt` if it uses confetti over text.

Acceptance: the record text is fully legible in a capture taken mid-animation.

### T11. Locked rows: dim the stars with the row

Problem: in `LevelsScreen.kt` and `LessonListScreen.kt`, locked rows are dimmed but `StarRow` keeps full-strength gold. Evidence: `free/01-levels.png`, `free/02-lessons.png`.

Change: when `locked = true`, render `StarRow` with the same disabled alpha the row text uses. Look up the existing value in `PathRow.kt`.

Acceptance: locked rows read as uniformly disabled.

### T12. Wrong answer marker uses the "replay" icon

Problem: `OptionButton.kt:144` uses `SkIcons.Replay` (↺) for wrong answers. The same icon means "replay animation" in lessons and "play again" on results. On reveal, two orange ↺ rows look like suggestions to try again. Evidence: `practice/level-1-read-reveal.png`, `lessons/01-viisarit-complete.png`.

Change: use `SkIcons.Close` for the WRONG state. Keep the colors and the content description.

### T13. Locked companion: separate "in use" from "previewing" (needs code inspection)

Problem: in `free/03-locked-companion.png` the selection ring is on the cat thumbnail while the large preview shows the dog. The two states are indistinguishable.

Change, in `ui/appearance/AppearanceScreen.kt` and `CompanionPagerState.kt`:
- The thumbnail ring marks the companion currently previewed (the pager page).
- The companion in use gets a small check badge on its thumbnail.
- When the previewed companion is locked, overlay `SkIcons.Lock` (the existing lock icon) on the large preview card. Keep the requirement text and the "For parents" button below it as they are.

Acceptance: from any capture you can tell which companion is in use and which is being previewed.

## Phase 3: Navigation and layout

### T14. Home tiles: show the subtitles that already exist

`MenuTile` (`ui/components/Buttons.kt:226`) receives `subtitle` but only uses it for semantics. The strings `menu_*_sub` already exist in all locales.

Change: render the subtitle under the title with `SkType.SmallSoft`, `maxLines = 2`, start-aligned. This separates "Play" from "Play Together". Keep `adaptiveGridColumns`.

Acceptance: at 320 dp and 200% font no tile text is clipped, and tiles in a row keep equal height.

### T15. "Outfits" icon reads as a lab flask

`SkIcons.Hanger` (`ui/components/Icons.kt:133`) renders as a narrow triangle with a neck. Evidence: the home "Outfits" button and `appearance/00-choose-companion.png`.

Change: replace the path with a T-shirt icon on the same 24-unit grid, for example `M8 3l-5 3 2 4 3-1v12h8V9l3 1 2-4-5-3a4 4 0 0 1-8 0z`. Rename the property to `SkIcons.Outfit` and update all callers (find them with grep).

### T16. Wardrobe and clock styles: keep the preview visible while choosing

Problem: the preview card scrolls away, so tapping lower items changes the character off screen. Evidence: `wardrobe/bunny-p2.png`, `wardrobe/car-p3.png`, `clock-styles/workshop-p2.png`.

Change, in `ui/companion/WardrobeScreen.kt` (`WardrobeStage` and `WardrobeItems`) and `ui/appearance/AppearanceScreen.kt` (`ZoomedClock`):
- When the full preview has scrolled out of view, show a compact pinned preview strip under the section chips: an 88 dp companion or clock, the companion name, and nothing else. Drive it from the scroll state with `derivedStateOf`.
- When the full preview is visible, hide the compact strip.
- With reduced motion, show or hide it without animation.
- In landscape or at 200% font, the strip must not exceed 25% of the viewport height.

Acceptance: after tapping any item at any scroll position, the change is visible without scrolling.

### T17. Reset progress dialog buttons

Problem: the `FlowRow` in `ui/settings/SettingsScreen.kt` (around line 182) wraps into two right-aligned buttons of different widths. Evidence: `main/09-reset-dialog.png`.

Change: replace the `FlowRow` with a `Column(Modifier.fillMaxWidth())` of two full-width buttons: Danger "Reset progress" first, then Tonal "Cancel", with `SkDimens.GapM` between them. Keep the `enabled = !pending` behavior.

### T18. Store unavailable state

Problem: the purchase button is disabled with the error sentence as its label, and the same sentence repeats in the body. "Try again" and "Restore purchases" look identical. Evidence: `free/07-store-unavailable.png`. Find the code in `ui/parent/ParentScreen.kt` and `billing/PurchaseUiState.kt`.

Change:
- Show `purchase_unavailable` once, in an inline message panel. Reuse an existing panel from `Panels.kt` with an error or warning tone token.
- Make "Try again" the primary button.
- Keep "Restore purchases" as the secondary button.
- Do not render a disabled purchase button.

### T19. Parent gate error state

Problem: a wrong code only shows plain text below the field. Evidence: `free/06-parent-gate-error.png`.

Change:
- Set `isError = true` on the field and move the message to `supportingText` in the error color token.
- Clear the field after a wrong attempt.
- Investigate the grainy texture on the Continue button in that capture (pressed or ripple state). Fix it if it comes from a custom shader or brush.

Changing the gate method itself (the reversed digits are easy for a reading child) is an **OWNER DECISION**.

### T20. Learn list: practise rows are indistinguishable

Every practise row reads "Practise / Play again". Evidence: `lessons/00-learning-path.png`.

Change: in `LessonListScreen.kt` (around lines 136 to 158), use the level title as the row title (for example "Half past") with a leading star icon. Keep "Play again" or "Full version" as supporting text. Existing level title strings cover this, so no new strings should be needed. Update the accessibility description to match.

### T21. Header shows scrolled content slivers

Problem: when lists scroll, a sliver of the previous card shows under the title row. Evidence: `free/02-lessons-p2.png`, `free/04-wardrobe-p2.png`, `lessons/00-learning-path-p2.png`.

Change: in the shared screen header (find it in `ui/components/Layout.kt` or `ScrollViewport.kt`), give the header an opaque background equal to the screen tint. Add the existing top fade below the header, not above it.

### T22. Section chips have no scroll affordance

The chip row is clipped at the right edge ("Extras" cut off). Evidence: `wardrobe/bunny.png`.

Change: in `ui/components/SectionChips.kt`, add an end-edge fade (and a start-edge fade once scrolled), using the screen background color. Also scroll the selected chip fully into view on selection.

### T23. Settings: loose "Read aloud" sentence

The "Read aloud is available…" sentence sits between cards. Evidence: `main/08-settings.png`.

Change: in `SettingsScreen.kt` and `SettingsRows.kt`, move it inside the Sound card as supporting text under the sound-effects row.

### T24. Explore guides toggle and practice hint share the lamp icon

Explore uses the lamp to toggle clock guides, and practice uses it for a hint.

Change:
- Add `SkIcons.Guides`: a clock outline with the right half filled (`SkColors.Yli`).
- Use it for the Explore and Play Together guides toggle.
- Keep the lamp for hints only.
- Add a content description that states the on or off state.

### T25. Minute challenge timing and result redundancy

Evidence: `play/05-challenge-playing.png`, `play/06-challenge-result.png`.

Change:
- Show the remaining seconds as a number at the end of the time bar.
- Use `task_position`-like formatting with a new string `challenge_seconds_left`, translated into all locales.
- On the result screen, hide the "N correct" header pill (the big number already shows it). Keep the sentence that compares the result with the best score.

## Phase 4: Typography, color and copy

### T26. Eyebrow labels (TRY IT, NEW BEST!, TIME IS UP!, NO SCORE YET)

`SkType.Eyebrow` (`ui/theme/Type.kt:64`) is 12 sp, extra bold, letter-spaced, and rendered in caps. Early readers struggle with it.

Change:
- Make it 14 sp with no extra letter spacing.
- Find where these strings are uppercased (grep for `uppercase`, `toUpperCase`, `capitalize` and `TextTransform`) and remove the transform so the resource text shows in sentence case.

### T27. Hint feedback looks like an error

Hint bubbles and wrong-answer options share the same peach and orange. Evidence: `practice/level-1-read-hint.png`.

Change:
- Give hint bubbles their own token: a soft yellow, reusing the existing gold family, for example `SkColors.HintFill`.
- Keep orange for wrong answers only.
- Check contrast for the ink text (at least 4.5:1).

### T28. Low-contrast text

Text to fix:
- the custom clock hint (orange text on peach), `clock-styles/custom-p2.png`
- "Drag either hand to change the time" (`SkColors.InkSoft` family on lavender), `main/02-explore-12h.png`
- the minute labels 0 to 55 around the clock

Change: use `SkColors.Ink` on the peach panel. Darken the soft ink token used on tinted panels until it reaches 4.5:1. Report the computed ratios.

### T29. Wardrobe instruction hierarchy

In `free/04-wardrobe.png`, the "full version includes more outfits" line is larger than the instruction "Tap an item to put it on".

Change: use the same `SkType.SmallSoft` for both. Put the full-version line second.

### T30. English spelling is mixed (OWNER DECISION)

The English strings mix British spelling (Practise, colours, centre, neighbouring) with American spelling (Color, favorite colors, Frame color). Ask the owner which variant the default `values` locale uses, then make all English strings consistent. Do not change the other six locales.

## Phase 5: Companions and art

Code tasks (implement):

### A1. Companions float above the ground on the home stage

Problem: feet and shadows sit above the green horizon, and the car's shadow is detached by about 40 px. Evidence: `home-companions/car.png`, `cat.png`, `dog.png`.

Change: in `ui/home/HomeStage.kt`, `HomeStageMath.kt` and `ui/art/GroundShadow.kt`, align the companion's bottom (shadow center) to the ground's top edge plus a small overlap token. For the car, fix the per-rig shadow offset in `BlenderCarRig.kt` or `CompanionPose.kt` so the shadow sits directly under the wheels.

Acceptance: every companion stands on the ground with its shadow touching its base, at 320 dp and 200% font.

### A2. Speech bubble clipped at the stage's right edge

Evidence: `home-companions/*.png`.

Change: add end padding equal to `SkDimens.Gap` inside the stage group so the bubble keeps its rounded right corners.

### A3. Item thumbnails: consistent framing, no headless torsos

Problem: neckwear tiles show bodies without heads, and the transformation thumbnails are smaller than the others. Evidence: `wardrobe/bunny-p2.png`, `wardrobe/car-p3.png`, `sheets/*-crops.png`.

Change: in `ui/companion/ItemArtWindow.kt`, change the neck-slot windows to a head-and-shoulders frame (the face visible to the eyes at least). Normalize the car "Transformations" windows to the same fill ratio as the other car slots.

### A4. Nested containers in item tiles

Each tile is a white card that contains a separately tinted, rounded image box.

Change: in the `ItemTile` and `LockedTile` code in `WardrobeScreen.kt`, remove the inner tinted box. The art sits directly on the tile surface.

### A5. Launch sequence

Problem: the splash (dark circle icon) is followed by a blank screen with a spinner and then the intro. These are three different states. Evidence: `startup/*.png`.

Change: keep the system splash on screen with `setKeepOnScreenCondition` until the first frame of the intro is ready, so the loading spinner screen never shows. If the splash icon background is configured in `res/values/themes.xml`, match it to the intro background.

Art and product tasks (OWNER DECISION, do not implement; list them in the report):

- **A6. Accessory scale.** Neck items are too small to read in thumbnails: cat bow tie, unicorn moon pendant and mane ribbons, bunny carrot, dragon pendant. Proposal: scale them 1.5 to 2 times in Blender.
- **A7. Held items float.** Wands, umbrella, kite, watering can and bubble wand float because the companions have no grip pose. Options: add grip poses, attach the items to the paws, or keep only worn items.
- **A8. Back items are invisible from the front.** Wings and capes barely show: unicorn Royal cape, cat butterfly wings, robot jetpack (reads as arm cannons). Options: a 3/4 preview pose, or larger wings visible over the shoulders.
- **A9. Detached or odd items.** Snowplow and rocket parts float away from the car, the dog frisbee is worn as a collar, and the coin pouch, book and star cloud sit on the ground like scenery.
- **A10. Duplicate items.** Dragon Pendant, Gold chain and Sparkle necklace look alike, and capes, crowns, flower crowns and wings repeat on every companion. Proposal: signature items per companion.
- **A11. Color variants.** Cheek marks stay orange on the blue, green and purple cat, and the green variants look muddy.
- **A12. Doll representation.** There is one light-skinned doll with a "Dresses" category. Proposal: skin tone and hair type options, and renaming the category to "Clothes".
- **A13. Two mascots.** The flat clock mascot (splash and intro) never appears again, while the 3D doll appears in tips. Decide the brand mascot.
- **A14. Clock theme identity.** Themes differ only by rim color, so the thumbnails look identical (`clock-styles/00-picker.png`). Proposal: themed dial background, numerals and center cap per theme. The custom clock option thumbnails (frame pattern, numbers, hands) differ by a few pixels. Proposal: full mini clocks with labels.

## Order of work

1. Phase 1: T1 to T6.
2. Phase 2: T7, T9 to T13 (T8 is an owner decision).
3. Phase 3: T14 to T25.
4. Phase 4: T26 to T29 (T30 is an owner decision).
5. Phase 5 code tasks: A1 to A5.

Run `:app:testDebugUnitTest` after each phase. Do not start a phase while the previous phase's tests fail.

## Validation (required before the final report)

1. Run `./gradlew --no-daemon :app:testDebugUnitTest` from the repository root.
2. Run `./gradlew --no-daemon :app:assembleDebug`.
3. Use the dedicated test emulator only, and name it explicitly in the report. Re-capture each changed screen at:
   - the default size
   - 320 dp width
   - 200% font scale
   - 320 dp width combined with 200% font scale
4. Save the new captures under `reports/clocklings-emulator-screenshots-<date>-after/`, using the same file names as the source folder, so before and after can be compared.
5. Report device evidence separately from source inspection. Do not claim that `lc` or `sc` passed. The owner runs them.
6. Stop every emulator or Gradle process you started. Leave other processes alone.

## Final report template (in Finnish)

- Toteutetut tehtävät (T- ja A-tunnisteet) ja muutetut tiedostot
- Ajetut tarkistukset ja niiden tulokset
- Laitteella todennetut näkymät ja konfiguraatiot
- Avoimet omistajan päätökset: T8, T19 (tunnistustapa), T30, A6–A14
- Todentamatta jääneet kohdat
