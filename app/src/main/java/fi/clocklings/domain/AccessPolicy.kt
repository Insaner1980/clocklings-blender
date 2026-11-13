package fi.clocklings.domain

/** Commercial access never changes educational progress. Indices are zero-based. */
data class AccessPolicy(
    val fullAccess: Boolean,
) {
    fun level(index: Int): Boolean = index in Lessons.levelLessons.indices && (fullAccess || index == 0)

    fun lesson(id: String): Boolean =
        when (id) {
            // The optional 24-hour lesson remains free by the approved content mapping.
            Lessons.DAY_LESSON -> true

            else -> Lessons.levelOf(id)?.let(::level) ?: false
        }

    fun lessonQueue(queue: List<String>): List<String> = queue.filter(::lesson)

    fun challenge(): Boolean = fullAccess

    fun companion(
        kind: CompanionKind,
        stars: Int,
    ): Boolean = kind in FREE_COMPANIONS || (fullAccess && stars >= COMPANION_STARS)

    fun itemContent(item: Item): Boolean =
        fullAccess || (item.companion in FREE_COMPANIONS && item.star <= FREE_ITEM_STARS)

    fun item(
        item: Item,
        stars: Int,
    ): Boolean = companion(item.companion, stars) && itemContent(item) && item.star <= stars

    /** Display-only fallback. Never persist it over a saved choice. NONE is always available. */
    fun displayedCompanion(
        saved: CompanionKind,
        stars: Int,
    ): CompanionKind = if (companion(saved, stars)) saved else CompanionKind.HUMAN

    fun displayedOutfit(
        kind: CompanionKind,
        saved: Outfit,
        stars: Int,
    ): Outfit =
        Outfit(
            Outfit.default(kind).items +
                saved.items.filter { (slot, id) ->
                    Wardrobe.itemOrNull(id)?.let { it.companion == kind && it.slot == slot && item(it, stars) } == true
                },
        )

    companion object {
        const val COMPANION_STARS = 3
        private const val FREE_ITEM_STARS = 3
        private val FREE_COMPANIONS = setOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.NONE)
    }
}
