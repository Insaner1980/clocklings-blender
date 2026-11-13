package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GermanTimeTest {
    @Test
    fun fiveMinutePhrasesNameTheCorrectHourAndOtherMinutesStayExact() {
        val examples =
            mapOf(
                0 to "drei Uhr",
                1 to "drei Uhr eins",
                5 to "fünf nach drei",
                10 to "zehn nach drei",
                14 to "drei Uhr vierzehn",
                15 to "Viertel nach drei",
                16 to "drei Uhr sechzehn",
                20 to "zwanzig nach drei",
                21 to "drei Uhr einundzwanzig",
                24 to "drei Uhr vierundzwanzig",
                25 to "fünf vor halb vier",
                26 to "drei Uhr sechsundzwanzig",
                29 to "drei Uhr neunundzwanzig",
                30 to "halb vier",
                31 to "drei Uhr einunddreißig",
                34 to "drei Uhr vierunddreißig",
                35 to "fünf nach halb vier",
                36 to "drei Uhr sechsunddreißig",
                39 to "drei Uhr neununddreißig",
                40 to "zwanzig vor vier",
                41 to "drei Uhr einundvierzig",
                44 to "drei Uhr vierundvierzig",
                45 to "Viertel vor vier",
                46 to "drei Uhr sechsundvierzig",
                50 to "zehn vor vier",
                55 to "fünf vor vier",
                59 to "drei Uhr neunundfünfzig",
            )
        examples.forEach { (minute, expected) ->
            assertEquals("03:$minute", expected, GermanTime.timeWords(180 + minute))
        }
    }

    @Test
    fun oneDependsOnItsConstructionAndNumericalReadingsKeepTheActualHour() {
        assertEquals("ein Uhr", GermanTime.timeWords(60))
        assertEquals("ein Uhr eins", GermanTime.timeWords(61))
        assertEquals("fünf nach eins", GermanTime.timeWords(65))
        assertEquals("halb eins", GermanTime.timeWords(750))
        assertEquals("Viertel vor eins", GermanTime.timeWords(765))
        assertEquals("fünfzehn Uhr dreißig", GermanTime.numericWords(930, true))
        assertEquals("einundzwanzig Uhr einundzwanzig", GermanTime.numericWords(1281, true))
        assertEquals("zweiundzwanzig Uhr zweiundzwanzig", GermanTime.numericWords(1342, true))
        assertEquals("null Uhr", GermanTime.numericWords(0, true))
        assertEquals("null Uhr eins", GermanTime.numericWords(1, true))
        assertEquals("zwölf Uhr", GermanTime.numericWords(720, true))
        assertEquals("15:30", GermanTime.digital(930, true))
        assertEquals("3:30", GermanTime.digital(930))
    }

    @Test
    fun contextualReadingsFollowTheNamedHourAcrossNoonAndMidnight() {
        assertEquals("zwölf Uhr", GermanTime.fullWords(0, false))
        assertEquals("zwölf Uhr", GermanTime.fullWords(720, false))
        assertEquals("Mitternacht", GermanTime.fullWords(0, true))
        assertEquals("Mittag", GermanTime.fullWords(720, true))
        assertEquals("fünf vor zwölf mittags", GermanTime.fullWords(715, true))
        assertEquals("elf Uhr neunundfünfzig vormittags", GermanTime.fullWords(719, true))
        assertEquals("zwölf Uhr eins mittags", GermanTime.fullWords(721, true))
        assertEquals("halb eins nachmittags", GermanTime.fullWords(750, true))
        assertEquals("fünf vor zwölf nachts", GermanTime.fullWords(1435, true))
        assertEquals("elf Uhr neunundfünfzig nachts", GermanTime.fullWords(1439, true))
        assertEquals("zwölf Uhr eins nachts", GermanTime.fullWords(1, true))
        assertEquals("halb eins nachts", GermanTime.fullWords(30, true))
        assertEquals("halb vier nachmittags", GermanTime.fullWords(930, true))
    }

    @Test
    fun everyMinuteIsDistinctAndWrapsWithoutAddingContextToAnalogReadings() {
        assertEquals(720, (0 until 720).map(GermanTime::timeWords).toSet().size)
        assertEquals(1440, (0 until 1440).map { GermanTime.fullWords(it, true) }.toSet().size)
        assertEquals(1440, (0 until 1440).map { GermanTime.numericWords(it, true) }.toSet().size)
        for (minute in 0 until 1440) {
            assertEquals(GermanTime.timeWords(minute), GermanTime.timeWords(minute + 720))
            assertEquals(GermanTime.fullWords(minute, true), GermanTime.fullWords(minute - 1440, true))
            assertEquals(GermanTime.digital(minute, true), GermanTime.digital(minute + 1440, true))
        }
    }

    @Test
    fun teachingSunAndGreetingsKeepTheirEstablishedBoundaries() {
        assertFalse(GermanTime.dayPart(359).isSun)
        assertTrue(GermanTime.dayPart(360).isSun)
        assertTrue(GermanTime.dayPart(1079).isSun)
        assertFalse(GermanTime.dayPart(1080).isSun)
        val greetings =
            mapOf(
                299 to Greeting.NIGHT,
                300 to Greeting.MORNING,
                599 to Greeting.MORNING,
                600 to Greeting.DAY,
                1079 to Greeting.DAY,
                1080 to Greeting.EVENING,
                1319 to Greeting.EVENING,
                1320 to Greeting.NIGHT,
            )
        greetings.forEach { (minute, expected) -> assertEquals(expected, GermanTime.greeting(minute)) }
    }
}
