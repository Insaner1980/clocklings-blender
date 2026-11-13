package fi.clocklings.domain

/**
 * Clothing slots. Only the doll's hair, dress and shoes are required. Neck is an animal-only slot; roof,
 * front, wheels and model (transformations) are the car's.
 */
enum class Slot(
    val optional: Boolean,
) {
    HAIR(false),
    DRESS(false),
    SHOES(false),
    HEAD(true),
    NECK(true),
    EXTRA(true),
    ROOF(true),
    FRONT(true),
    WHEELS(true),
    MODEL(true),
}

/**
 * The slots a companion can dress, chosen by its anatomy. The doll has no neck slot; animals have only
 * head, neck and extra; the car has no head at all.
 */
val CompanionKind.slots: List<Slot>
    get() =
        when (this) {
            CompanionKind.HUMAN -> listOf(Slot.HAIR, Slot.DRESS, Slot.SHOES, Slot.HEAD, Slot.EXTRA)
            CompanionKind.CAR -> listOf(Slot.ROOF, Slot.FRONT, Slot.WHEELS, Slot.MODEL)
            CompanionKind.NONE -> emptyList()
            else -> listOf(Slot.HEAD, Slot.NECK, Slot.EXTRA)
        }

/** Vaate. [star] on kokonaistähtimäärä, jolla se avautuu (0 = heti). Nimi on strings.xml:ssä. */
data class Item(
    val id: String,
    val companion: CompanionKind,
    val slot: Slot,
    val star: Int,
)

/** What a companion wears, one item id per slot. Missing slots are empty. */
data class Outfit(
    val items: Map<Slot, String> = emptyMap(),
) {
    operator fun get(slot: Slot): String? = items[slot]

    /** Vaihtaa paikan vaatteen. Pakollista paikkaa ei voi tyhjentää. */
    fun with(
        slot: Slot,
        id: String?,
    ): Outfit =
        when {
            id != null -> Outfit(items + (slot to id))
            slot.optional -> Outfit(items - slot)
            else -> this
        }

    fun isOn(item: Item): Boolean = this[item.slot] == item.id

    /**
     * The outfit a wardrobe thumbnail of [item] shows: the required clothes (the doll cannot be bare)
     * plus the item itself, and no other optional item.
     */
    fun previewFor(item: Item): Outfit = Outfit(items.filterKeys { !it.optional } + (item.slot to item.id))

    companion object {
        val Default =
            Outfit(mapOf(Slot.HAIR to "hair-letit", Slot.DRESS to "dress-perus", Slot.SHOES to "shoes-tossut"))

        /** The doll starts in its free clothes; animals start bare. */
        fun default(kind: CompanionKind): Outfit = if (kind == CompanionKind.HUMAN) Default else Outfit()
    }
}

object Wardrobe {
    const val MAX_STARS = 15

    private fun doll(
        id: String,
        slot: Slot,
        star: Int,
    ) = Item(id, CompanionKind.HUMAN, slot, star)

    private val dollItems: List<Item> =
        listOf(
            doll("hair-letit", Slot.HAIR, 0),
            doll("dress-perus", Slot.DRESS, 0),
            doll("shoes-tossut", Slot.SHOES, 0),
            doll("dress-pilkku", Slot.DRESS, 1),
            doll("head-rusetti", Slot.HEAD, 2),
            doll("shoes-punaiset", Slot.SHOES, 3),
            doll("hair-pitkat", Slot.HAIR, 4),
            doll("dress-tahti", Slot.DRESS, 5),
            doll("head-kukat", Slot.HEAD, 6),
            doll("extra-strawberry-bag", Slot.EXTRA, 7),
            doll("shoes-saappaat", Slot.SHOES, 8),
            doll("hair-kiharat", Slot.HAIR, 9),
            doll("dress-overalls", Slot.DRESS, 10),
            doll("head-kruunu", Slot.HEAD, 11),
            doll("extra-siivet", Slot.EXTRA, 12),
            doll("shoes-roller-skates", Slot.SHOES, 13),
            doll("hair-sateenkaari", Slot.HAIR, 14),
            doll("dress-sateenkaari", Slot.DRESS, 15),
        )

    val items: List<Item> = dollItems + AnimalItems

    private val byId = items.associateBy { it.id }

    fun item(id: String): Item = byId.getValue(id)

    fun itemOrNull(id: String): Item? = byId[id]

    fun itemsFor(kind: CompanionKind): List<Item> = items.filter { it.companion == kind }

    fun itemsFor(
        kind: CompanionKind,
        slot: Slot,
    ): List<Item> = items.filter { it.companion == kind && it.slot == slot }

    /** Items a companion has from the start. */
    fun freeItems(kind: CompanionKind): List<Item> = itemsFor(kind).filter { it.star == 0 }

    fun newlyUnlocked(
        kind: CompanionKind,
        before: Int,
        after: Int,
    ): List<Item> = itemsFor(kind).filter { it.star in (before + 1)..after }

    fun lockedCount(
        kind: CompanionKind,
        totalStars: Int,
    ): Int = itemsFor(kind).count { it.star > totalStars }

    /** Painallus pukee vaatteen; vapaaehtoisen paikan vaate lähtee pois toisella painalluksella. */
    fun toggle(
        outfit: Outfit,
        item: Item,
    ): Outfit =
        if (outfit.isOn(item) &&
            item.slot.optional
        ) {
            outfit.with(item.slot, null)
        } else {
            outfit.with(item.slot, item.id)
        }
}
