package fi.clocklings.domain

private fun head(id: String) = id to Slot.HEAD

private fun neck(id: String) = id to Slot.NECK

private fun extra(id: String) = id to Slot.EXTRA

private fun roof(id: String) = id to Slot.ROOF

private fun front(id: String) = id to Slot.FRONT

private fun wheels(id: String) = id to Slot.WHEELS

private fun model(id: String) = id to Slot.MODEL

/** One free item (star 0), then one unlock per star in the given order (star = 1-based position). */
private fun ladder(
    kind: CompanionKind,
    free: Pair<String, Slot>,
    vararg unlocks: Pair<String, Slot>,
): List<Item> =
    listOf(Item(free.first, kind, free.second, 0)) +
        unlocks.mapIndexed { i, (id, slot) -> Item(id, kind, slot, i + 1) }

/** The star-unlocked wardrobes of every companion but the doll. Item ids are `<kind>-<slot>-<name>`. */
internal val AnimalItems: List<Item> =
    ladder(
        CompanionKind.CAT,
        neck("cat-neck-scarf"),
        head("cat-head-bow"),
        extra("cat-extra-yarn"),
        neck("cat-neck-bell-collar"),
        head("cat-head-bobble-hat"),
        extra("cat-extra-glasses"),
        neck("cat-neck-bow-tie"),
        head("cat-head-sailor-hat"),
        extra("cat-extra-fish-backpack"),
        neck("cat-neck-ruffle"),
        head("cat-head-flower-crown"),
        neck("cat-neck-pearls"),
        extra("cat-extra-wand"),
        neck("cat-neck-cape"),
        head("cat-head-crown"),
        extra("cat-extra-wings"),
    ) +
        ladder(
            CompanionKind.DOG,
            neck("dog-neck-bandana"),
            head("dog-head-cap"),
            extra("dog-extra-ball"),
            neck("dog-neck-bone-collar"),
            head("dog-head-sun-hat"),
            extra("dog-extra-frisbee"),
            neck("dog-neck-bow-tie"),
            head("dog-head-party-hat"),
            extra("dog-extra-boots"),
            neck("dog-neck-lei"),
            head("dog-head-aviator"),
            extra("dog-extra-backpack"),
            neck("dog-neck-medal"),
            head("dog-head-helmet"),
            extra("dog-extra-raincoat"),
            neck("dog-neck-cape"),
        ) +
        ladder(
            CompanionKind.ROBOT,
            head("robot-head-antenna"),
            neck("robot-neck-bow-tie"),
            extra("robot-extra-wrench"),
            head("robot-head-propeller"),
            neck("robot-neck-cape"),
            extra("robot-extra-heart-eyes"),
            head("robot-head-headphones"),
            extra("robot-extra-bubble-pack"),
            extra("robot-extra-star-eyes"),
            head("robot-head-chef-hat"),
            extra("robot-extra-wind-up-key"),
            extra("robot-extra-pet"),
            head("robot-head-star-antennas"),
            neck("robot-neck-medal"),
            head("robot-head-astronaut"),
            extra("robot-extra-jetpack"),
        ) +
        ladder(
            CompanionKind.DRAGON,
            neck("dragon-neck-pendant"),
            head("dragon-head-flower-crown"),
            extra("dragon-extra-coins"),
            neck("dragon-neck-bow"),
            head("dragon-head-wizard-hat"),
            extra("dragon-extra-book"),
            neck("dragon-neck-scarf"),
            head("dragon-head-tiara"),
            extra("dragon-extra-shield"),
            neck("dragon-neck-chain"),
            head("dragon-head-knight-helmet"),
            extra("dragon-extra-star-cloud"),
            neck("dragon-neck-sparkles"),
            neck("dragon-neck-cape"),
            head("dragon-head-crown"),
            extra("dragon-extra-wings"),
        ) +
        ladder(
            CompanionKind.DINOSAUR,
            extra("dinosaur-extra-backpack"),
            head("dinosaur-head-flower"),
            extra("dinosaur-extra-bucket"),
            neck("dinosaur-neck-scarf"),
            head("dinosaur-head-cap"),
            extra("dinosaur-extra-balloon"),
            neck("dinosaur-neck-bow-tie"),
            head("dinosaur-head-party-hat"),
            extra("dinosaur-extra-umbrella"),
            extra("dinosaur-extra-swim-ring"),
            head("dinosaur-head-hard-hat"),
            extra("dinosaur-extra-kite"),
            neck("dinosaur-neck-binoculars"),
            neck("dinosaur-neck-medal"),
            extra("dinosaur-extra-ice-cream"),
            extra("dinosaur-extra-skateboard"),
        ) +
        ladder(
            CompanionKind.BUNNY,
            neck("bunny-neck-ribbon"),
            extra("bunny-extra-carrot"),
            head("bunny-head-ear-bow"),
            neck("bunny-neck-daisies"),
            extra("bunny-extra-basket"),
            head("bunny-head-sun-hat"),
            neck("bunny-neck-locket"),
            extra("bunny-extra-watering-can"),
            head("bunny-head-top-hat"),
            neck("bunny-neck-bow-tie"),
            extra("bunny-extra-bubbles"),
            head("bunny-head-flower-crown"),
            extra("bunny-extra-bouquet"),
            neck("bunny-neck-cape"),
            head("bunny-head-tiara"),
            extra("bunny-extra-wings"),
        ) +
        ladder(
            CompanionKind.UNICORN,
            neck("unicorn-neck-lei"),
            head("unicorn-head-horn-rings"),
            extra("unicorn-extra-cloud"),
            neck("unicorn-neck-stars"),
            head("unicorn-head-flower-crown"),
            extra("unicorn-extra-balloon"),
            neck("unicorn-neck-scarf"),
            head("unicorn-head-ribbons"),
            extra("unicorn-extra-boots"),
            neck("unicorn-neck-rainbow-scarf"),
            head("unicorn-head-butterfly"),
            extra("unicorn-extra-rainbow"),
            neck("unicorn-neck-moon"),
            head("unicorn-head-crown"),
            neck("unicorn-neck-cape"),
            extra("unicorn-extra-wings"),
        ) +
        ladder(
            CompanionKind.CAR,
            front("car-front-plate"),
            wheels("car-wheels-sport"),
            roof("car-roof-suitcase"),
            front("car-front-sunglasses"),
            wheels("car-wheels-offroad"),
            model("car-model-race"),
            roof("car-roof-surfboard"),
            front("car-front-bull-bar"),
            wheels("car-wheels-flames"),
            roof("car-roof-flag"),
            model("car-model-fire-engine"),
            front("car-front-plow"),
            roof("car-roof-taxi"),
            wheels("car-wheels-gold"),
            roof("car-roof-rally-lights"),
            model("car-model-rocket"),
        )
