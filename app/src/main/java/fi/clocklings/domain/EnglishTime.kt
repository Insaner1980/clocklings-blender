package fi.clocklings.domain

/** The shared English past/to style selected for Clocklings. */
object EnglishTime : TimeLanguage {
    private val small =
        listOf(
            "zero",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
        )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty")

    private fun number(value: Int): String =
        if (value < small.size) {
            small[value]
        } else {
            tens[value / DECIMAL_BASE] + if (value % DECIMAL_BASE == 0) "" else "-${small[value % DECIMAL_BASE]}"
        }

    override fun timeWords(total: Int): String {
        val time = ClockTime.reading(total, false)
        return when (time.minute) {
            0 -> {
                "${number(time.hour)} o'clock"
            }

            MINUTES_PER_QUARTER_HOUR -> {
                "quarter past ${number(time.hour)}"
            }

            ClockTime.MINUTES_PER_HALF_HOUR -> {
                "half past ${number(time.hour)}"
            }

            ClockTime.MINUTES_PER_HOUR - MINUTES_PER_QUARTER_HOUR -> {
                "quarter to ${number(time.nextHour)}"
            }

            else -> {
                if (time.minute < ClockTime.MINUTES_PER_HALF_HOUR) {
                    "${minutePhrase(time.minute)} past ${number(time.hour)}"
                } else {
                    "${minutePhrase(ClockTime.MINUTES_PER_HOUR - time.minute)} to ${number(time.nextHour)}"
                }
            }
        }
    }

    private fun minutePhrase(minute: Int): String =
        when {
            minute == 1 -> "one minute"
            minute % ClockTime.MINUTES_PER_NUMBER == 0 -> number(minute)
            else -> "${number(minute)} minutes"
        }

    override fun digital(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val minute = (time % ClockTime.MINUTES_PER_HOUR).toString().padStart(2, '0')
        val displayHour =
            if (h24) {
                hour.toString().padStart(2, '0')
            } else {
                (hour % ClockTime.HOURS_PER_HALF_DAY)
                    .let {
                        if (it == 0) ClockTime.HOURS_PER_HALF_DAY else it
                    }.toString()
            }
        return "$displayHour:$minute"
    }

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        if (!h24 && time.minute == 0) return timeWords(total)
        val minutes =
            when {
                time.minute == 0 -> "zero zero"
                time.minute < DECIMAL_BASE -> "oh ${number(time.minute)}"
                else -> number(time.minute)
            }
        return "${number(time.hour)} $minutes"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val sun = hour in SUNRISE_HOUR until EVENING_FROM
        return when {
            time == 0 -> DayPart("midnight", "at midnight", sun)
            time == ClockTime.HALF_DAY -> DayPart("noon", "at noon", sun)
            hour < MORNING_FROM || hour >= NIGHT_FROM -> DayPart("night", "at night", sun)
            hour < ClockTime.HOURS_PER_HALF_DAY -> DayPart("morning", "in the morning", sun)
            hour < EVENING_FROM -> DayPart("afternoon", "in the afternoon", sun)
            else -> DayPart("evening", "in the evening", sun)
        }
    }

    override fun greeting(total: Int): Greeting =
        when (ClockTime.mod(total, ClockTime.DAY) / ClockTime.MINUTES_PER_HOUR) {
            in MORNING_FROM until DAY_GREETING_FROM -> Greeting.MORNING
            in DAY_GREETING_FROM until EVENING_FROM -> Greeting.DAY
            in EVENING_FROM until NIGHT_FROM -> Greeting.EVENING
            else -> Greeting.NIGHT
        }
}

private const val DECIMAL_BASE = 10
private const val MINUTES_PER_QUARTER_HOUR = 15
private const val SUNRISE_HOUR = 6
private const val MORNING_FROM = 5
private const val DAY_GREETING_FROM = 10
private const val EVENING_FROM = 18
private const val NIGHT_FROM = 22
