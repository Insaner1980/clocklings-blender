# New companions (bunny, car, unicorn): design

Date: 28 October 2026
Status: approved by the owner in chat on 28 October 2026. The pilot (bunny and car) is built first and checked by the owner before the unicorn.

## 1. Goal and owner decisions

The owner supplied eight ChatGPT character images (bunny, car, elephant, monster, octopus, penguin, train, unicorn). To keep the app small while giving every child something to like, the owner chose three that fill gaps in the current six:

- **Bunny**: the soft, small classic for the youngest children.
- **Unicorn**: the pastel fantasy fan base. The dragon is a different kind of fantasy.
- **Car**: vehicles. No current companion covers them.

The others (penguin, octopus, elephant, monster, train) are left for possible later content updates.

Other decisions:

- Style: the images are felt plush toys; the app draws shaded vinyl toys in code. The new companions are drawn in the app's vinyl style, after the images' shapes, proportions and colors. The images are references only and are not copied into the app.
- Each companion has its own wardrobe slots, chosen by its anatomy. A vehicle never has a head slot, so it can never wear a bow on its "head".
- Transformation is an optional item in its own slot, not an automatic growth stage. The child can wear it and take it off. No growth stages for animals.
- Same ladder as the other companions: one free item and one item per total star 1 to 15.
- The bunny and the car are the pilot. The owner checks them on the emulator before the unicorn is drawn.

## 2. Slots

`Slot` gains four optional slots for vehicles: `ROOF`, `FRONT`, `WHEELS` and `MODEL` (transformations). `CompanionKind.slots` lists each kind's slots:

- Human: unchanged.
- Animals, including the bunny and the unicorn: HEAD, NECK, EXTRA.
- Car: ROOF, FRONT, WHEELS, MODEL.

Section names: Roof, Front, Wheels, Transformations (fi: Katolle, Keulaan, Pyörät, Muodonmuutokset; sv: På taket, Fram, Hjul, Förvandlingar).

Items of different slots must never cover each other badly, because any combination can be worn. So car transformations keep the roof top, the wheels and the bumper free. The approved pilot keeps the low rear race-car spoiler. Draw the rear spoiler and its supports, fire-engine ladder and rocket fins behind the cabin through `ItemArt.back`; roof items remain on the cabin top. The lip spoiler is a separate lower-front part: `CarItems.raceCar` draws it at x=96–210, y=266–274 on `ArtPart.BODY` / `ItemLayer.FRONT`, after the body and wheels. Put door markings below the handle and above the rear tire, and draw hubcaps after the wheels. Foreground transformation details may cover body paint but must keep the other slots readable. These are placement and draw-order constraints, not permission to redesign the owner-approved artwork.

Persistence needs no new rules: keys are `outfit_<kind>_<slot>` and `companion_name_<kind>`, written for every kind that has slots. Kinds are stored by name, so new enum entries do not disturb saved data.

## 3. Item catalog

Star, English name, slot. Ids are `<kind>-<slot>-<name>`.

### Bunny (free: ribbon bow, neck)

1. Carrot (extra)
2. Ear bow (head)
3. Daisy chain (neck)
4. Egg basket (extra)
5. Sun hat (head)
6. Heart locket (neck)
7. Watering can (extra)
8. Magician's top hat (head)
9. Polka-dot bow tie (neck)
10. Bubble wand (extra)
11. Flower crown (head)
12. Bouquet (extra)
13. Star cape (neck)
14. Tiara (head)
15. Fairy wings (extra)

### Car (free: number plate, front)

The owner asked for vehicle-themed items instead of flowers, stars and bows.

1. Sport rims (wheels)
2. Roof rack with a suitcase (roof)
3. Sunglasses (front)
4. Off-road tires (wheels)
5. Race car (transformation): low rear spoiler behind the cabin, hood stripes, a number on the door and a separate lower-front lip spoiler
6. Surfboard (roof)
7. Bull bar with fog lights (front)
8. Flame hubcaps (wheels)
9. Checkered flag (roof)
10. Fire engine (transformation): flashing lights on the mirrors and a ladder behind the car
11. Snowplow (front)
12. Taxi sign (roof)
13. Gold rims (wheels)
14. Rally lights (roof)
15. Rocket car (transformation): rear fins, a nozzle and a flame

### Unicorn (free: flower garland, neck)

Head items sit around the horn so the horn always shows. Mane decorations are head items.

1. Horn rings (head)
2. Cloud cushion (extra)
3. Star necklace (neck)
4. Flower crown (head)
5. Star balloon (extra)
6. Neckerchief (neck)
7. Mane ribbons (head)
8. Sparkle boots (extra)
9. Rainbow scarf (neck)
10. Butterfly clip (head)
11. Rainbow (extra)
12. Moon pendant (neck)
13. Crown (head)
14. Royal cape (neck)
15. Feathered wings (extra)

## 4. Artwork

All three use the existing toolkit (`VinylShading.kt`, `ArtTones`, `ItemArtKit`), the 240 x 300 animal grid and `AnimalRig`, so they size like the other animals on every screen. Brushes are built once per color.

- **Bunny**: sitting, front view, after the reference. Large round head with small brows, long floppy ears with pink inner sides that hang beside the head, a pearl muzzle with a small pink nose and a "w" mouth, glossy eyes, cheeks, a round body with a pearl belly, small front paws and big feet with pink pads. Idle: breathing, blinking, head tilt, the ears swing with `partDeg`. Hats sit on top of the head and cover the ear roots, as on the dog.
- **Car**: a chunky toy car seen from the front, after the reference: cream cabin with a blue windshield, round mirrors like ears, a body in the companion color, a cream face panel with glossy eyes in cream rings, a smile and cheeks, a cream bumper and two round wheels with cream hubs showing at the lower corners. Idle: blinking, breathing squash, and the body rocks gently on its wheels with `partDeg`. Hubcap items are drawn on the hubs after the body and wheels (`ArtPart.WHEELS`).
- **Unicorn**: front view, standing, with the horn, a pastel mane and tail, pastel hooves and a pink belly patch.

Colors: new pigments in `PersonalizationColors` (bunny beige, car orange, car tire plum, windshield glass, unicorn white and mane pastels). The four color choices work as for the others; for the car they repaint the body.

Sounds: the new companions are silent until the owner supplies voice files, as before.

## 5. Picker

Ten choices (nine companions and "Just the clock") still fit in the existing pager and thumbnail row, which scrolls on narrow screens. No grouping is needed.

## 6. Localization

Companion names, slot names and item names in English, Finnish and Swedish.

## 7. Validation

- Unit: catalog invariants for the new kinds (16 items, one free, stars 1 to 15, slots belong to the kind), slots per kind, persistence round trip, every item visible and every color distinct (`CompanionRenderingTest`).
- Instrumented: `CompanionArtCoverageTest` and the `GalleryTest` helpers include the new kinds.
- `:app:testDebugUnitTest` and `:app:assembleDebug`. Screenshots of the new companions and their wardrobes at 320 dp, 200% font and both, on `emulator-5590` only.
