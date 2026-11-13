package fi.clocklings.domain

/** Germany-oriented five-minute readings and exact numerical readings. */
object GermanTime : TimeLanguage {
    private val small =
        listOf(
            "null",
            "eins",
            "zwei",
            "drei",
            "vier",
            "fünf",
            "sechs",
            "sieben",
            "acht",
            "neun",
            "zehn",
            "elf",
            "zwölf",
            "dreizehn",
            "vierzehn",
            "fünfzehn",
            "sechzehn",
            "siebzehn",
            "achtzehn",
            "neunzehn",
        )
    private val tens = listOf("", "", "zwanzig", "dreißig", "vierzig", "fünfzig")

    private fun number(value: Int): String =
        if (value < small.size) {
            small[value]
        } else {
            val unit = value % DECIMAL_BASE
            val prefix = if (unit == 0) "" else "${if (unit == 1) "ein" else small[unit]}und"
            prefix + tens[value / DECIMAL_BASE]
        }

    override fun timeWords(total: Int): String {
        val time = ClockTime.reading(total, false)
        val hour = number(time.hour)
        val next = number(time.nextHour)
        return when (time.minute) {
            0 -> numericWords(total)
            FIVE_MINUTES -> "fünf nach $hour"
            TEN_MINUTES -> "zehn nach $hour"
            QUARTER_PAST -> "Viertel nach $hour"
            TWENTY_MINUTES -> "zwanzig nach $hour"
            BEFORE_HALF -> "fünf vor halb $next"
            ClockTime.MINUTES_PER_HALF_HOUR -> "halb $next"
            AFTER_HALF -> "fünf nach halb $next"
            TWENTY_TO -> "zwanzig vor $next"
            QUARTER_TO -> "Viertel vor $next"
            TEN_TO -> "zehn vor $next"
            FIVE_TO -> "fünf vor $next"
            else -> numericWords(total)
        }
    }

    override fun numericWords(
        total: Int,
        h24: Boolean,
    ): String {
        val time = ClockTime.reading(total, h24)
        val hour = if (time.hour == 1) "ein" else number(time.hour)
        return "$hour Uhr" + if (time.minute == 0) "" else " ${number(time.minute)}"
    }

    override fun dayPart(total: Int): DayPart {
        val time = ClockTime.reading(total, true)
        return when {
            time.hour == 0 && time.minute == 0 -> DayPart("Mitternacht", "nachts", time.isSun)
            time.hour == ClockTime.HOURS_PER_HALF_DAY && time.minute == 0 -> DayPart("Mittag", "mittags", time.isSun)
            time.hour < MORNING_FROM || time.hour >= NIGHT_FROM -> DayPart("Nacht", "nachts", time.isSun)
            time.hour < LATE_MORNING_FROM -> DayPart("Morgen", "morgens", time.isSun)
            time.hour < ClockTime.HOURS_PER_HALF_DAY -> DayPart("Vormittag", "vormittags", time.isSun)
            time.hour < EVENING_FROM -> DayPart("Nachmittag", "nachmittags", time.isSun)
            else -> DayPart("Abend", "abends", time.isSun)
        }
    }

    override fun referencedDayPart(total: Int): DayPart {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val minute = time % ClockTime.MINUTES_PER_HOUR
        val namedHour =
            if (minute >= BEFORE_HALF && minute % ClockTime.MINUTES_PER_NUMBER == 0) {
                time + ClockTime.MINUTES_PER_HOUR - minute
            } else {
                time - minute
            }
        return dayPart(namedHour)
    }

    override fun greeting(total: Int): Greeting = EnglishTime.greeting(total)
}

private const val DECIMAL_BASE = 10
private const val FIVE_MINUTES = 5
private const val TEN_MINUTES = 10
private const val QUARTER_PAST = 15
private const val TWENTY_MINUTES = 20
private const val BEFORE_HALF = 25
private const val AFTER_HALF = 35
private const val TWENTY_TO = 40
private const val QUARTER_TO = 45
private const val TEN_TO = 50
private const val FIVE_TO = 55
private const val MORNING_FROM = 5
private const val LATE_MORNING_FROM = 10
private const val EVENING_FROM = 18
private const val NIGHT_FROM = 22
