package fi.clocklings.domain

/** Spain Spanish: five-minute readings use y/menos; other minutes are read exactly. */
object SpanishTime : TimeLanguage {
    private val small =
        listOf(
            "cero",
            "uno",
            "dos",
            "tres",
            "cuatro",
            "cinco",
            "seis",
            "siete",
            "ocho",
            "nueve",
            "diez",
            "once",
            "doce",
            "trece",
            "catorce",
            "quince",
            "dieciséis",
            "diecisiete",
            "dieciocho",
            "diecinueve",
            "veinte",
            "veintiuno",
            "veintidós",
            "veintitrés",
            "veinticuatro",
            "veinticinco",
            "veintiséis",
            "veintisiete",
            "veintiocho",
            "veintinueve",
        )
    private val tens = listOf("", "", "veinte", "treinta", "cuarenta", "cincuenta")

    private fun number(value: Int): String =
        if (value < small.size) {
            small[value]
        } else {
            tens[value / DECIMAL_BASE] + if (value % DECIMAL_BASE == 0) "" else " y ${small[value % DECIMAL_BASE]}"
        }

    private fun hourLabel(hour: Int): String = if (hour == 1) "la una" else "las ${number(hour)}"

    private fun usesNextHour(minute: Int): Boolean =
        minute > ClockTime.MINUTES_PER_HALF_HOUR && minute % ClockTime.MINUTES_PER_NUMBER == 0

    override fun timeWords(total: Int): String {
        val time = ClockTime.reading(total, false)
        val hour = hourLabel(time.hour)
        val next = hourLabel(time.nextHour)
        return when {
            time.minute == 0 -> hour
            time.minute == 1 -> "$hour y un minuto"
            time.minute == QUARTER_PAST -> "$hour y cuarto"
            time.minute == ClockTime.MINUTES_PER_HALF_HOUR -> "$hour y media"
            time.minute == QUARTER_TO -> "$next menos cuarto"
            usesNextHour(time.minute) -> "$next menos ${number(ClockTime.MINUTES_PER_HOUR - time.minute)}"
            else -> "$hour y ${number(time.minute)}"
        }
    }

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        val hour =
            when (time.hour) {
                1 -> "la una"
                TWENTY_ONE -> "las veintiuna horas"
                else -> "las ${number(time.hour)} horas"
            }
        if (time.minute == 0) return hour
        val minute =
            when {
                time.minute == 1 -> {
                    "un minuto"
                }

                time.minute == TWENTY_ONE -> {
                    "veintiún minutos"
                }

                time.minute > ClockTime.MINUTES_PER_HALF_HOUR && time.minute % DECIMAL_BASE == 1 -> {
                    "${number(time.minute).removeSuffix("uno")}un minutos"
                }

                else -> {
                    "${number(time.minute)} minutos"
                }
            }
        return "$hour y $minute"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.reading(total, true)
        return when {
            time.hour == 0 && time.minute == 0 -> {
                DayPart("medianoche", "de la noche", time.isSun)
            }

            time.hour == ClockTime.HOURS_PER_HALF_DAY && time.minute == 0 -> {
                DayPart("mediodía", "del mediodía", time.isSun)
            }

            time.hour < MORNING_FROM -> {
                DayPart("madrugada", "de la madrugada", time.isSun)
            }

            time.hour < ClockTime.HOURS_PER_HALF_DAY -> {
                DayPart("mañana", "de la mañana", time.isSun)
            }

            time.hour < NIGHT_FROM -> {
                DayPart("tarde", "de la tarde", time.isSun)
            }

            else -> {
                DayPart("noche", "de la noche", time.isSun)
            }
        }
    }

    override fun referencedDayPart(total: Int): DayPart {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val minute = time % ClockTime.MINUTES_PER_HOUR
        val namedHour = time - minute + if (usesNextHour(minute)) ClockTime.MINUTES_PER_HOUR else 0
        return dayPart(namedHour)
    }

    fun timeSentence(
        total: Int,
        h24: Boolean,
    ): String {
        val words = fullWords(total, h24)
        return "${if (words.startsWith("las ")) "Son" else "Es"} $words"
    }
}

private const val DECIMAL_BASE = 10
private const val QUARTER_PAST = 15
private const val QUARTER_TO = 45
private const val TWENTY_ONE = 21
private const val MORNING_FROM = 6
private const val NIGHT_FROM = 20
