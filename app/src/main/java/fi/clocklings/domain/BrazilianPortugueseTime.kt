package fi.clocklings.domain

/** Brazilian clock readings: five-minute steps use e/para; other minutes are exact readings. */
object BrazilianPortugueseTime : TimeLanguage {
    override fun timeWords(total: Int): String {
        val time = ClockTime.reading(total, false)
        return if (usesRemaining(time.minute)) {
            "${number(ClockTime.MINUTES_PER_HOUR - time.minute)} para ${target(time.nextHour, false)}"
        } else {
            direct(time.hour, time.minute, false)
        }
    }

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        return hourWithUnit(time.hour) + if (time.minute == 0) "" else " e ${minuteWithUnit(time.minute)}"
    }

    override fun fullWords(
        total: Int,
        h24: Boolean,
    ): String {
        if (!h24) return timeWords(total)
        val time = ClockTime.reading(total, true)
        val remaining = usesRemaining(time.minute)
        val reference = if (remaining) (time.hour + 1) % ClockTime.HOURS_PER_DAY else time.hour
        val phrase =
            if (remaining) {
                "${number(ClockTime.MINUTES_PER_HOUR - time.minute)} para ${target(reference, true)}"
            } else {
                direct(time.hour, time.minute, true)
            }
        return if (namedHour(reference) != null) phrase else "$phrase ${periodAdverb(reference)}"
    }

    /** A complete current-time sentence, with agreement based on its actual construction. */
    fun timeSentence(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        if (usesRemaining(time.minute)) {
            val reference = if (h24) (time.hour + 1) % ClockTime.HOURS_PER_DAY else time.nextHour
            val suffix = if (h24 && namedHour(reference) == null) " ${periodAdverb(reference)}" else ""
            val minutes = minuteWithUnit(ClockTime.MINUTES_PER_HOUR - time.minute)
            return "Faltam $minutes para ${target(reference, h24)}$suffix"
        }
        val singular = time.hour % ClockTime.HOURS_PER_HALF_DAY == 1 || (h24 && namedHour(time.hour) != null)
        return "${if (singular) "É" else "São"} ${fullWords(total, h24)}"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.reading(total, true)
        return when {
            time.hour == 0 && time.minute == 0 -> {
                DayPart("meia-noite", "à meia-noite", time.isSun)
            }

            time.hour == ClockTime.HOURS_PER_HALF_DAY && time.minute == 0 -> {
                DayPart("meio-dia", "ao meio-dia", time.isSun)
            }

            time.hour < SUNRISE_HOUR -> {
                DayPart("madrugada", "da madrugada", time.isSun)
            }

            time.hour < ClockTime.HOURS_PER_HALF_DAY -> {
                DayPart("manhã", "da manhã", time.isSun)
            }

            time.hour < EVENING_FROM -> {
                DayPart("tarde", "da tarde", time.isSun)
            }

            else -> {
                DayPart("noite", "da noite", time.isSun)
            }
        }
    }

    override fun greeting(total: Int): Greeting = EnglishTime.greeting(total)
}

private val small =
    listOf(
        "zero",
        "um",
        "dois",
        "três",
        "quatro",
        "cinco",
        "seis",
        "sete",
        "oito",
        "nove",
        "dez",
        "onze",
        "doze",
        "treze",
        "catorze",
        "quinze",
        "dezesseis",
        "dezessete",
        "dezoito",
        "dezenove",
    )
private val tens = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta")

private fun number(value: Int): String =
    if (value < small.size) {
        small[value]
    } else {
        tens[value / DECIMAL_BASE] + if (value % DECIMAL_BASE == 0) "" else " e ${small[value % DECIMAL_BASE]}"
    }

private fun hourNumber(hour: Int): String =
    when (hour) {
        1 -> "uma"
        2 -> "duas"
        TWENTY_ONE -> "vinte e uma"
        TWENTY_TWO -> "vinte e duas"
        else -> number(hour)
    }

private fun hourWithUnit(hour: Int): String = "${hourNumber(hour)} ${if (hour <= 1) "hora" else "horas"}"

private fun minuteWithUnit(minute: Int): String = "${number(minute)} ${if (minute == 1) "minuto" else "minutos"}"

private fun usesRemaining(minute: Int): Boolean =
    minute > ClockTime.MINUTES_PER_HALF_HOUR && minute % ClockTime.MINUTES_PER_NUMBER == 0

private fun namedHour(hour: Int): String? =
    when (hour) {
        0 -> "meia-noite"
        ClockTime.HOURS_PER_HALF_DAY -> "meio-dia"
        else -> null
    }

private fun target(
    hour: Int,
    contextual: Boolean,
): String =
    when {
        contextual && hour == 0 -> "a meia-noite"
        contextual && hour == ClockTime.HOURS_PER_HALF_DAY -> "o meio-dia"
        hour % ClockTime.HOURS_PER_HALF_DAY == 1 -> "a uma"
        else -> "as ${hourNumber(ClockTime.reading(hour * ClockTime.MINUTES_PER_HOUR, false).hour)}"
    }

private fun direct(
    hour: Int,
    minute: Int,
    contextual: Boolean,
): String {
    val name = if (contextual) namedHour(hour) else null
    val analogHour = ClockTime.reading(hour * ClockTime.MINUTES_PER_HOUR, false).hour
    val label = name ?: hourNumber(analogHour)
    return when {
        minute == 0 -> name ?: hourWithUnit(analogHour)
        minute == ClockTime.MINUTES_PER_HALF_HOUR -> "$label e meia"
        minute % ClockTime.MINUTES_PER_NUMBER == 0 -> "$label e ${number(minute)}"
        else -> "${name ?: hourWithUnit(analogHour)} e ${minuteWithUnit(minute)}"
    }
}

private fun periodAdverb(hour: Int): String =
    when {
        hour < SUNRISE_HOUR -> "da madrugada"
        hour < ClockTime.HOURS_PER_HALF_DAY -> "da manhã"
        hour < EVENING_FROM -> "da tarde"
        else -> "da noite"
    }

private const val DECIMAL_BASE = 10
private const val TWENTY_ONE = 21
private const val TWENTY_TWO = 22
private const val SUNRISE_HOUR = 6
private const val EVENING_FROM = 18
