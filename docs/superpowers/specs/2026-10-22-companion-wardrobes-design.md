# Companion wardrobes and vinyl-toy artwork: design

Date: 22 October 2026
Branch: `codex/companions-clock-themes`
Status: written for owner review. Not yet approved for implementation.

## 1. Goal

All six companions should look like one premium toy series. The look is based on the owner's ChatGPT reference image (`C:\Users\EmmaH\Downloads\clocklings-screenshots\db38d5f4-7056-48f8-9325-776f1f5cdfd0.png`): smooth vinyl toys with soft shading. Every companion is animated, and every companion has a wardrobe of earned items that works like the human doll's.

### Owner decisions (22 October 2026)

- Technique: artwork stays code-drawn on Compose `Canvas`. A shared shading toolkit adds radial-gradient volume, specular highlights, contact shadows and a ground shadow. No raster images and no 3D engine.
- The approved style sample is the shaded cat with a bobble hat and scarf, and a blue cat with a crown and bell collar (scratchpad HTML probe, not kept).
- The human doll gets the same shading. Its 18 items and unlock rules stay the same.
- Cat, dog, robot, dragon and dinosaur each get 15 unlockable items, one per star 1 to 15, using the same total star count as the doll.
- Colors stay free. They become the first wardrobe section, and all four colors are available immediately.
- The owner approved the item list in chat. Section 4 completes that list so that every companion has exactly 15 unlockable items.

### Problem being fixed

On the companion picker, non-human companions have one loose on/off chip (Bandana, Antenna, Backpack and so on). It sits under the color chips without a heading, so it looks like a misplaced button. This design removes the chip and gives every companion a proper wardrobe screen.

### Success criteria

- Every companion renders in the shared vinyl style at all sizes used in the app: picker tile, preview, home card, result, intro and wardrobe stage.
- Every companion idles (breathing, blinking, its own part motion), greets, celebrates success, and reveals newly unlocked items with the spin used by the doll.
- With system animations disabled, every companion is static, and new items appear immediately.
- Each wardrobe screen shows colors and all items by slot, with wearing state, lock state and star cost, matching the doll screen.
- Readable and usable at 320 dp width, 200% font scale and both together.
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass. Device evidence comes from the dedicated test emulator.

### Out of scope

- Raster artwork, a 3D engine or new dependencies.
- New companion kinds, growth stages, companion space, presence options.
- Changing the doll's proportions. The doll keeps its current geometry so that all 18 existing items still fit. Only shading and face detail change.
- Finnish and Swedish item names. English is done first; the other languages follow in the localization pass required before publication.

## 2. Architecture overview

```
domain/
  Wardrobe.kt            catalog for all companions, toggle, unlock queries
  CompanionItems.kt      item lists for the five animal companions (data only)
data/
  ProgressRepository.kt  per-companion outfits
  PersonalizationPreferences.kt  color only; legacy accessory read once
ui/art/
  VinylShading.kt        shared shading toolkit (DrawScope extensions)
ui/companion/
  CompanionPose.kt       pose and motion (moved and widened from ui/doll)
  CompanionView.kt       one entry point for every companion (moved from ui/appearance)
  CatRig.kt, DogRig.kt, RobotRig.kt, DragonRig.kt, DinosaurRig.kt
  CatItems.kt, DogItems.kt, RobotItems.kt, DragonItems.kt, DinosaurItems.kt
  WardrobeScreen.kt      generalized from DollScreen
ui/doll/
  Doll.kt, DollParts.kt  shading applied, geometry kept
```

The current `ui/appearance/CompanionArt.kt` is replaced by the per-companion art files and then deleted. `AppearanceScreen.kt` keeps picking the companion and clock.

## 3. Domain model

### Slots

`Slot` gets a `NECK` entry. Each companion kind declares the slots it uses:

- Human: HAIR, DRESS, SHOES (required), HEAD, EXTRA (optional). Unchanged.
- Cat, dog, robot, dragon, dinosaur: HEAD, NECK, EXTRA, all optional.

`Slot.optional` stays a property of the slot. Required slots exist only for the human.

### Items

`Item` gains a `companion: CompanionKind` field. Ids are unique across the whole catalog, for example `cat-head-bow`. Existing doll ids (`hair-letit` and so on) do not change, so saved doll outfits keep working.

Each animal companion has 16 items: the existing accessory, free at star 0, plus 15 items at stars 1 to 15, one per star.

### Outfits

`Outfit` becomes a small map from slot to item id with the same operations as now (`get`, `with`, `isOn`). `Outfit.default(kind)` returns the doll's current default for the human and an empty outfit for animals. `Progress.outfit` becomes `Progress.outfits: Map<CompanionKind, Outfit>`, with `outfitFor(kind)`.

### Wardrobe queries

`Wardrobe` gets `itemsFor(kind)`, `itemsFor(kind, slot)`, `newlyUnlocked(kind, before, after)`, `lockedCount(kind, totalStars)` and `toggle(outfit, item)`. The toggle rule is unchanged: tapping wears an item, and tapping a worn optional item removes it. All of this stays pure Kotlin and is tested on the JVM.

`Wardrobe.MAX_STARS` stays 15 and remains the shared star ceiling.

## 4. Item catalog

Each line gives the star at which the item unlocks, its English name and its slot. The free item is the companion's existing accessory, redrawn in the new style. The list approved in chat had fewer than 15 items for some companions. Items marked "added" complete those lists.

### Cat (free: striped scarf, neck)

1. Bow (head)
2. Yarn ball (extra)
3. Bell collar (neck)
4. Bobble hat (head)
5. Round glasses (extra)
6. Bow tie (neck)
7. Sailor hat (head)
8. Fish backpack (extra)
9. Ruffle collar (neck), added
10. Flower crown (head)
11. Pearl necklace (neck)
12. Magic wand (extra)
13. Sparkle cape (neck)
14. Crown (head)
15. Butterfly wings (extra)

### Dog (free: bandana, neck)

1. Cap (head)
2. Tennis ball (extra)
3. Bone tag collar (neck)
4. Sun hat (head)
5. Frisbee (extra)
6. Bow tie (neck)
7. Party hat (head)
8. Rain boots (extra)
9. Flower lei (neck), added
10. Aviator cap (head)
11. Backpack (extra)
12. Medal (neck)
13. Firefighter helmet (head)
14. Raincoat (extra)
15. Hero cape (neck)

### Robot (free: antenna, head)

1. Bow tie (neck)
2. Wrench (extra)
3. Propeller cap (head)
4. Tie (neck), added
5. Heart-eyes screen (extra)
6. Headphones (head)
7. Spring collar (neck)
8. Star-eyes screen (extra)
9. Chef hat (head), added
10. Light-up necklace (neck)
11. Robot pet (extra)
12. Star antennas (head)
13. Gold medal (neck), added
14. Astronaut helmet (head)
15. Jetpack (extra)

The two screen items replace the robot's face display while worn. They are EXTRA items, so the robot can wear a screen face and a hat together.

### Dragon (free: pendant, neck)

1. Flower crown (head)
2. Gold coin pouch (extra)
3. Bow (neck), added
4. Wizard hat (head)
5. Magic book (extra)
6. Scarf (neck), added
7. Tiara (head)
8. Shield (extra)
9. Gold chain (neck)
10. Knight helmet (head)
11. Star cloud (extra)
12. Sparkle necklace (neck)
13. Royal cape (neck)
14. Crown (head)
15. Sparkle wings (extra)

Sparkle wings replace the dragon's own wings while worn.

### Dinosaur (free: backpack, extra)

1. Flower behind the ear (head)
2. Sand bucket (extra)
3. Scarf (neck)
4. Cap (head)
5. Balloon (extra)
6. Bow tie (neck)
7. Party hat (head)
8. Umbrella (extra)
9. Flower lei (neck), added
10. Hard hat (head)
11. Kite (extra), added
12. Binoculars (neck)
13. Medal (neck)
14. Explorer hat (head)
15. Skateboard (extra)

All names go into `strings.xml` as `item_<companion>_<name>`. The existing `itemName(id)` lookup is extended, and its uniqueness test covers every item.

## 5. Artwork

### Shared shading toolkit (`ui/art/VinylShading.kt`)

One set of `DrawScope` helpers used by every companion, every item and the doll:

- `vinylFill(path or oval, base)`: radial gradient with its center at 36% / 30% of the shape bounds and a radius of 0.78 of the larger side. Stops: light tint (base mixed 45% toward white), base at 0.55, shade (base mixed 30% toward a warm dark) at 1. This is the recipe from the approved sample.
- `vinylStroke(path, base, width)`: the same gradient on strokes (tails, straps).
- `specular(rect, rotation)`: a soft white highlight fading to transparent, 55% opacity at the center.
- `contactShadow(rect)`: a warm dark radial fade under heads and overhanging parts.
- `groundShadow(rect)`: a soft ink-colored ellipse under the feet.
- `glossyEye(center, size, blink)`: dark gradient eye with two highlights, closing to a line when blinking.

Every companion is drawn in a fixed design grid (240 x 300 for animals, the doll's existing 200 x 340) and scaled to the canvas. Gradient coordinates are therefore constant, so brushes are built once per color and reused on every frame instead of being allocated per frame.

Color tones come only from theme files. `PersonalizationColors` gets the base pigments for each companion's fur, plates, metal and screen and for accessory materials (knit, teal, gold, red, pearl, denim and so on). `SkDollColors` keeps the doll's pigments. Light and shade tones are derived by the toolkit, never hardcoded per shape.

### Companion rig

Each companion is drawn in named parts so that motion and items can attach to them:

- Body: breathes (a 1.2% vertical squash around the feet).
- Head: tilts (about 3 degrees) around the neck point.
- Tail, ears, wings, arms: each companion moves its own parts. Examples: the cat's tail wags, the dog's ear flops, the robot's antenna bounces, the dragon's wings flap, the dinosaur's tail swings.
- Eyes: blink using the doll's existing irregular blink timing.

Proportions follow the reference image: large heads, cream muzzles and bellies, glossy eyes and pink cheeks. The cat follows the approved sample. The dog has floppy dark ears, an eye patch and a tongue. The robot has a dark face screen, ear discs, a chest panel and jointed limbs. The dragon has horns, wing membranes, a plated belly and a gold tail tip. The dinosaur stands upright with back spikes and spots.

### Item anchoring

Every item declares the part it follows and a draw layer:

- Part: HEAD items move with the head, NECK items with the body below the head, and EXTRA items with the part they belong to (back, paw or hand, face, ground).
- Layer: BEHIND (capes, wings, backpacks and other items drawn before the body), FRONT (most items), or OVER_EARS (hats drawn before the ears so that ears come through, as in the approved sample).

Items are drawn with the same toolkit and use their own fixed colors, so they look the same in every companion color.

### The human doll

`Doll.kt` keeps all of its geometry and drawing order. Flat fills are replaced with toolkit calls for skin, hair, clothing, shoes and accessories. The face gets glossy eyes and softer cheeks. Wardrobe thumbnails keep their existing crops.

## 6. Motion

`DollPose` and `DollMotion` move to `ui/companion/CompanionPose.kt` and become `CompanionPose` and `CompanionMotion`. All existing fields and timings stay the same. New fields:

- `breathe`: body squash factor.
- `headTiltDeg`: head tilt.
- `partDeg`: one oscillating angle that each companion maps to its own part (tail, ear, wings).

`DollCelebration` becomes `CompanionCelebration` with the same behavior: a spin that reveals the new outfit, then two hops. Animals hop and wave or flap instead of the doll's arm wave. The motion stays as pure functions of time, tested on the JVM, and is read only while drawing, so animation does not cause recomposition.

`CompanionView` becomes the single entry point for every companion and handles reduced motion in one place. The doll's separate `LivelyDoll` logic merges into it.

## 7. Screens

### Companion picker (`AppearanceScreen`)

- Remove `CompanionControls`, the color chips and the loose accessory chip.
- For every companion except "Just the clock", show the "Choose an outfit" button that the doll uses now. It opens the wardrobe of the selected companion.
- Picker tiles and the large preview show each companion in its current color and outfit.

### Wardrobe screen (`WardrobeScreen`, generalized from `DollScreen`)

- `Screen.Doll` becomes `Screen.Wardrobe`. It always shows the currently selected companion.
- The stage shows the large animated companion and its name field. Each companion has its own name; the existing doll name becomes the human's name.
- For animals, the first section is **Color**. Its four tiles show the companion in each color. All are free and one is selected.
- Then one section per slot (Head, Neck, Extra for animals; the doll's existing five), with the existing item tiles, "Wearing" pill, dashed locked tiles and star cost.
- An item tile shows the companion wearing that item, cropped to the slot. Each companion declares its slot crops, as `DollParts.crop` does for the doll.
- Unlock text and the "all items available" text count only the current companion's items.

### Home, intro and results

- Home: the companion card keeps the total star count. The unlock hint applies to every companion.
- Results: `visibleUnlocks` returns the current companion's newly unlocked items for every kind, not just the human. New items show as tiles, the companion spins them on, and the "Choose an outfit" button appears.
- Intro: uses the current companion's outfit.

## 8. Persistence

- The doll keeps its keys (`outfit_hair` and so on).
- Animal outfits use `outfit_<kind>_<slot>`, for example `outfit_cat_neck`. Empty means nothing worn.
- Names use `companion_name_<kind>`. The human keeps the existing name key.
- Colors stay in personalization (`companion_<kind>_color`). The `accessory` boolean is removed from `CompanionStyle`.
- Legacy read: if a kind has no saved outfit and the old `companion_<kind>_accessory` is true, its free item is worn. The old key is not written again.
- Unknown or mismatched ids fall back as they do now: optional slots empty, required slots default.
- "Reset stars and outfits" resets every companion's outfit and keeps colors, as it keeps other settings now.

## 9. Testing and validation

Unit tests (JVM):

- Catalog invariants per animal kind: exactly 16 items, one free, stars 1 to 15 each used once, every slot belongs to that kind, ids unique across the catalog.
- Toggle, `newlyUnlocked` and `lockedCount` per kind. Existing doll wardrobe tests keep passing unchanged.
- Persistence: round trip for every kind, legacy accessory read, unknown ids, reset.
- Motion: new pose channels stay within bounds and return to rest; celebration timings unchanged.
- Item name lookup covers every item.

Instrumented and visual:

- `GalleryTest` renders every companion in every color, and with every item, for screenshot inspection.
- `AppFlowTest`: open an animal's wardrobe, wear and remove an item, change color, check that locked items cannot be worn.
- Screenshots at 320 dp width, 200% font scale and both together for the picker, the wardrobe screen and results.
- Build: `:app:testDebugUnitTest` and `:app:assembleDebug`. Device checks only on the dedicated test emulator. The owner runs `lc` and `sc`.

## 10. Implementation order

Each step leaves the app building and its tests passing.

1. Shading toolkit and pose/motion move, with no visual change yet.
2. Domain and persistence for per-companion wardrobes, with the final English item names.
3. Wardrobe screen generalization and picker cleanup (fixes the loose accessory chip).
4. Cat: new body and all 16 items. The owner checks it on the emulator before the remaining companions are drawn.
5. Dog, robot, dragon, dinosaur, each with all 16 items.
6. Doll shading.
7. Results, home and intro integration, then gallery screenshots and a STATUS.md update.

## 11. Risks

- Drawing effort: 80 animal items plus restyling the doll is the largest part of the work. The cat checkpoint in step 4 tests the effort and the look before the rest is built.
- Rendering cost: animated gradient drawing on low-end devices. Mitigation: brushes are cached per color, and a frame-time check runs on the emulator after the cat.
- Small sizes: highlights and whiskers can turn into noise in 96 dp tiles. The toolkit can skip fine detail below a size threshold if screenshots show it is needed.
- Spin reveal on asymmetric animals: a horizontal flip can look odd for the dinosaur's side tail. If it does, that companion uses a hop with a sparkle instead of the spin.
