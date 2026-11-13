package fi.clocklings.domain

/**
 * Lapsen edistyminen. "Aloita alusta" korvaa tämän oletusarvoilla, mutta [Settings] säilyy.
 * [introShown] sisältää tasot ("0"–"4") ja "h24", joiden oppitunnit on jo näytetty automaattisesti.
 */
data class Progress(
    val stars: List<Int> = List(LEVEL_COUNT) { 0 },
    val challengeBest: Int = 0,
    val outfits: Map<CompanionKind, Outfit> = emptyMap(),
    val names: Map<CompanionKind, String> = emptyMap(),
    val lessonsDone: Set<String> = emptySet(),
    val introShown: Set<String> = emptySet(),
) {
    val totalStars: Int get() = stars.sum()

    fun outfitFor(kind: CompanionKind): Outfit = outfits[kind] ?: Outfit.default(kind)

    fun nameFor(kind: CompanionKind): String = names[kind].orEmpty()

    /** Stores a companion's outfit; the default outfit is not kept in the map. */
    fun withOutfit(
        kind: CompanionKind,
        outfit: Outfit,
    ): Progress = copy(outfits = if (outfit == Outfit.default(kind)) outfits - kind else outfits + (kind to outfit))

    /** Stores a companion's name; an empty name is not kept in the map. */
    fun withName(
        kind: CompanionKind,
        name: String,
    ): Progress = copy(names = if (name.isEmpty()) names - kind else names + (kind to name))

    /** Tallentaa tason tuloksen, jos se on parempi kuin aiempi. */
    fun withLevelResult(
        level: Int,
        stars: Int,
    ): Progress =
        if (stars >
            this.stars[level]
        ) {
            copy(stars = this.stars.toMutableList().also { it[level] = stars })
        } else {
            this
        }

    fun withChallengeScore(score: Int): Progress = if (score > challengeBest) copy(challengeBest = score) else this

    fun withLessonDone(id: String): Progress = copy(lessonsDone = lessonsDone + id)

    companion object {
        const val LEVEL_COUNT = 5
    }
}

/** Asetukset, jotka säilyvät "Aloita alusta" -toiminnossa. Oletus on 12 h. */
data class Settings(
    val sound: Boolean = true,
    val help: Boolean = true,
    val h24: Boolean = false,
    val personalization: Personalization = Personalization(),
)
