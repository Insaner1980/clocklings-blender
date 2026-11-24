package fi.clocklings.domain

/** Shared Swedish clock wording for both Swedish regional locales. */
object SwedishTime : TimeLanguage {
    private val units = listOf("noll", "ett", "två", "tre", "fyra", "fem", "sex", "sju", "åtta", "nio")
    private val teens =
        listOf("tio", "elva", "tolv", "tretton", "fjorton", "femton", "sexton", "sjutton", "arton", "nitton")
    private val tens = listOf("", "", "tjugo", "trettio", "fyrtio", "femtio")

    private fun number(value: Int): String =
        when {
            value < DECIMAL_BASE -> units[value]
            value < HOUR_RELATIVE_LIMIT -> teens[value - DECIMAL_BASE]
            else -> tens[value / DECIMAL_BASE] + if (value % DECIMAL_BASE == 0) "" else units[value % DECIMAL_BASE]
        }

    private fun minutes(value: Int): String =
        when {
            value == 1 -> "en minut"
            value % ClockTime.MINUTES_PER_NUMBER == 0 -> number(value)
            else -> "${number(value)} minuter"
        }

    override fun timeWords(total: Int): String {
        val time = ClockTime.reading(total, false)
        val minute = time.minute
        val next = number(time.nextHour)
        return when {
            minute == 0 -> number(time.hour)

            minute == QUARTER_PAST -> "kvart över ${number(time.hour)}"

            minute == ClockTime.MINUTES_PER_HALF_HOUR -> "halv $next"

            minute == QUARTER_TO -> "kvart i $next"

            minute <= HOUR_RELATIVE_LIMIT -> "${minutes(minute)} över ${number(time.hour)}"

            minute < ClockTime.MINUTES_PER_HALF_HOUR -> "${minutes(
                ClockTime.MINUTES_PER_HALF_HOUR - minute,
            )} i halv $next"

            minute < NEXT_HOUR_FROM -> "${minutes(minute - ClockTime.MINUTES_PER_HALF_HOUR)} över halv $next"

            else -> "${minutes(ClockTime.MINUTES_PER_HOUR - minute)} i $next"
        }
    }

    override fun digital(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val shown =
            if (h24) {
                hour.toString().padStart(2, '0')
            } else {
                (hour % ClockTime.HOURS_PER_HALF_DAY)
                    .let {
                        if (it ==
                            0
                        ) {
                            ClockTime.HOURS_PER_HALF_DAY
                        } else {
                            it
                        }
                    }.toString()
            }
        return "$shown.${(time % ClockTime.MINUTES_PER_HOUR).toString().padStart(2, '0')}"
    }

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        if (!h24 && time.minute == 0) return timeWords(total)
        val reading =
            when {
                time.minute == 0 -> "noll noll"
                time.minute < DECIMAL_BASE -> "noll ${number(time.minute)}"
                else -> number(time.minute)
            }
        return "${number(time.hour)} $reading"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val sun = hour in SUNRISE_HOUR..LAST_SUN_HOUR
        return when {
            time == 0 -> DayPart("midnatt", "vid midnatt", sun)
            time == ClockTime.HALF_DAY -> DayPart("middag", "mitt på dagen", sun)
            hour < MORNING_FROM || hour >= NIGHT_FROM -> DayPart("natt", "på natten", sun)
            hour < DECIMAL_BASE -> DayPart("morgon", "på morgonen", sun)
            hour < ClockTime.HOURS_PER_HALF_DAY -> DayPart("förmiddag", "på förmiddagen", sun)
            hour < EVENING_FROM -> DayPart("eftermiddag", "på eftermiddagen", sun)
            else -> DayPart("kväll", "på kvällen", sun)
        }
    }

    override fun fullWords(
        total: Int,
        h24: Boolean,
    ): String {
        if (!h24) return timeWords(total)
        return when (ClockTime.mod(total, ClockTime.DAY)) {
            0 -> "midnatt"
            ClockTime.HALF_DAY -> "tolv på dagen"
            else -> "${timeWords(total)} ${dayPart(total).adverb}"
        }
    }
}

private const val QUARTER_TO = 45
private const val NEXT_HOUR_FROM = 40
private const val NIGHT_FROM = 22
private const val HOUR_RELATIVE_LIMIT = 20
private const val EVENING_FROM = 18
private const val LAST_SUN_HOUR = 17
private const val QUARTER_PAST = 15
private const val DECIMAL_BASE = 10
private const val SUNRISE_HOUR = 6
private const val MORNING_FROM = 5
