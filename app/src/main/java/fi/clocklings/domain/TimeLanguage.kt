package fi.clocklings.domain

enum class Greeting { MORNING, DAY, EVENING, NIGHT }

/** A contextual day label, its phrase form and the teaching sun indicator. */
data class DayPart(
    val name: String,
    val adverb: String,
    val isSun: Boolean,
)

interface TimeLanguage {
    /** An analog reading with no inferred morning, evening, noon or midnight. */
    fun timeWords(total: Int): String

    fun digital(
        total: Int,
        h24: Boolean = false,
    ): String = EnglishTime.digital(total, h24)

    fun numericWords(
        total: Int,
        h24: Boolean = false,
    ): String

    fun dayPart(total: Int): DayPart

    /** The period of the hour named in an analog phrase, which may be the following hour. */
    fun referencedDayPart(total: Int): DayPart = dayPart(total)

    fun fullWords(
        total: Int,
        h24: Boolean,
    ): String {
        if (!h24) return timeWords(total)
        val time = ClockTime.mod(total, ClockTime.DAY)
        return if (time == 0 || time == ClockTime.HALF_DAY) {
            dayPart(time).name
        } else {
            "${timeWords(time)} ${referencedDayPart(time).adverb}"
        }
    }

    fun greeting(total: Int): Greeting
}

fun timeLanguage(language: AppLanguage): TimeLanguage =
    when (language) {
        AppLanguage.ENGLISH -> EnglishTime
        AppLanguage.FINNISH -> FinnishTime
        AppLanguage.SWEDISH -> SwedishTime
        AppLanguage.GERMAN -> GermanTime
        AppLanguage.FRENCH -> FrenchTime
        AppLanguage.SPANISH -> SpanishTime
        AppLanguage.PORTUGUESE_BRAZIL -> BrazilianPortugueseTime
    }
