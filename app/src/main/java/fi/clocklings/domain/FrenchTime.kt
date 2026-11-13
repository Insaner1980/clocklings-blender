package fi.clocklings.domain

/** France-oriented clock readings, with exact minutes between five-minute steps. */
object FrenchTime : TimeLanguage {
    private fun clockPhrase(
        total: Int,
        contextual: Boolean,
    ): String {
        val time = ClockTime.reading(total, true)
        val referenceHour = (time.hour + if (usesNextHour(time.minute)) 1 else 0) % ClockTime.HOURS_PER_DAY
        val analogHour = referenceHour % ClockTime.HOURS_PER_HALF_DAY
        val specialHour = contextual && referenceHour % ClockTime.HOURS_PER_HALF_DAY == 0
        val hour =
            if (specialHour) {
                if (referenceHour == 0) "minuit" else "midi"
            } else {
                hourWords(if (analogHour == 0) ClockTime.HOURS_PER_HALF_DAY else analogHour)
            }
        val phrase = minutePhrase(hour, time.minute, specialHour)
        return if (contextual && !specialHour) {
            "$phrase ${dayPart(referenceHour * ClockTime.MINUTES_PER_HOUR).adverb}"
        } else {
            phrase
        }
    }

    override fun timeWords(total: Int): String = clockPhrase(total, contextual = false)

    override fun fullWords(
        total: Int,
        h24: Boolean,
    ): String = clockPhrase(total, contextual = h24)

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        val hour = hourWords(time.hour)
        return if (time.minute == 0) hour else "$hour ${minuteWords(time.minute)}"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.reading(total, true)
        return when {
            time.hour == 0 && time.minute == 0 -> DayPart("minuit", "à minuit", time.isSun)
            time.hour == ClockTime.HOURS_PER_HALF_DAY && time.minute == 0 -> DayPart("midi", "à midi", time.isSun)
            time.hour < MORNING_FROM -> DayPart("nuit", "du matin", time.isSun)
            time.hour < ClockTime.HOURS_PER_HALF_DAY -> DayPart("matin", "du matin", time.isSun)
            time.hour < EVENING_FROM -> DayPart("après-midi", "de l'après-midi", time.isSun)
            time.hour < NIGHT_FROM -> DayPart("soir", "du soir", time.isSun)
            else -> DayPart("nuit", "du soir", time.isSun)
        }
    }

    override fun greeting(total: Int): Greeting = EnglishTime.greeting(total)
}

private val small =
    listOf(
        "zéro",
        "une",
        "deux",
        "trois",
        "quatre",
        "cinq",
        "six",
        "sept",
        "huit",
        "neuf",
        "dix",
        "onze",
        "douze",
        "treize",
        "quatorze",
        "quinze",
        "seize",
    )
private val tens = listOf("", "dix", "vingt", "trente", "quarante", "cinquante")

private fun number(value: Int): String =
    when {
        value < small.size -> small[value]
        value % DECIMAL_BASE == 0 -> tens[value / DECIMAL_BASE]
        value % DECIMAL_BASE == 1 -> "${tens[value / DECIMAL_BASE]} et une"
        else -> "${tens[value / DECIMAL_BASE]}-${small[value % DECIMAL_BASE]}"
    }

private fun hourWords(hour: Int): String = "${number(hour)} ${if (hour <= 1) "heure" else "heures"}"

private fun minuteWords(minute: Int): String = if (minute == 1) "une minute" else number(minute)

private fun usesNextHour(minute: Int): Boolean =
    minute > ClockTime.MINUTES_PER_HALF_HOUR && minute % ClockTime.MINUTES_PER_NUMBER == 0

private fun minutePhrase(
    hour: String,
    minute: Int,
    specialHour: Boolean,
): String =
    when (minute) {
        0 -> {
            hour
        }

        MINUTES_PER_QUARTER_HOUR -> {
            "$hour et quart"
        }

        ClockTime.MINUTES_PER_HALF_HOUR -> {
            "$hour et ${if (specialHour) "demi" else "demie"}"
        }

        ClockTime.MINUTES_PER_HOUR - MINUTES_PER_QUARTER_HOUR -> {
            "$hour moins le quart"
        }

        else -> {
            if (usesNextHour(minute)) {
                "$hour moins ${number(ClockTime.MINUTES_PER_HOUR - minute)}"
            } else {
                "$hour ${minuteWords(minute)}"
            }
        }
    }

private const val DECIMAL_BASE = 10
private const val MINUTES_PER_QUARTER_HOUR = 15
private const val MORNING_FROM = 5
private const val EVENING_FROM = 18
private const val NIGHT_FROM = 22
