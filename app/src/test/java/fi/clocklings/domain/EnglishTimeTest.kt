package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EnglishTimeTest {
    @Test
    fun phrasesUseCorrectReferenceHourAndSingularMinutes() {
        val examples =
            mapOf(
                0 to "three o'clock",
                1 to "one minute past three",
                2 to "two minutes past three",
                5 to "five past three",
                14 to "fourteen minutes past three",
                15 to "quarter past three",
                16 to "sixteen minutes past three",
                25 to "twenty-five past three",
                29 to "twenty-nine minutes past three",
                30 to "half past three",
                31 to "twenty-nine minutes to four",
                35 to "twenty-five to four",
                44 to "sixteen minutes to four",
                45 to "quarter to four",
                46 to "fourteen minutes to four",
                58 to "two minutes to four",
                59 to "one minute to four",
            )
        examples.forEach { (minute, expected) ->
            assertEquals("03:$minute", expected, EnglishTime.timeWords(180 + minute))
        }
        assertEquals("quarter to one", EnglishTime.timeWords(765))
        assertEquals("one minute to twelve", EnglishTime.timeWords(719))
    }

    @Test
    fun noonAndMidnightRequireDayContext() {
        assertEquals("twelve o'clock", EnglishTime.fullWords(0, false))
        assertEquals("twelve o'clock", EnglishTime.fullWords(720, false))
        assertEquals("midnight", EnglishTime.fullWords(0, true))
        assertEquals("noon", EnglishTime.fullWords(720, true))
        assertEquals("half past three at night", EnglishTime.fullWords(210, true))
        assertEquals("half past three in the afternoon", EnglishTime.fullWords(930, true))
    }

    @Test
    fun numericReadingsAndDisplaysUseTheActualHour() {
        assertEquals("three thirty", EnglishTime.numericWords(210))
        assertEquals("three oh seven", EnglishTime.numericWords(187))
        assertEquals("three o'clock", EnglishTime.numericWords(180))
        assertEquals("fifteen thirty", EnglishTime.numericWords(930, true))
        assertEquals("twenty-two oh eight", EnglishTime.numericWords(1328, true))
        assertEquals("fifteen zero zero", EnglishTime.numericWords(900, true))
        assertEquals("3:30", EnglishTime.digital(930))
        assertEquals("15:30", EnglishTime.digital(930, true))
        assertEquals("00:00", EnglishTime.digital(0, true))
        assertEquals("12:00", EnglishTime.digital(0))
    }

    @Test
    fun everyAnalogMinuteHasOneUnambiguousPhraseAndNormalizesAcrossDays() {
        val phrases = (0 until 720).map(EnglishTime::timeWords)
        assertEquals(720, phrases.toSet().size)
        for (minute in 0 until 1440) {
            assertTrue(EnglishTime.fullWords(minute, true).isNotBlank())
            assertEquals(EnglishTime.timeWords(minute), EnglishTime.timeWords(minute + 720))
            assertEquals(EnglishTime.timeWords(minute), EnglishTime.timeWords(minute - 1440))
            assertEquals(EnglishTime.digital(minute, true), EnglishTime.digital(minute + 1440, true))
            assertNotEquals(EnglishTime.fullWords(minute, true), EnglishTime.fullWords(minute + 720, true))
        }
    }

    @Test
    fun generatedChoicesHaveDistinctEnglishLabelsAndNumericTimes() {
        for (h24 in listOf(false, true)) {
            for (level in Levels.all) {
                for (time in 0 until ClockTime.period(h24) step level.step) {
                    val choices = Levels.optionsFor(time, level, h24, Random(time))
                    assertEquals(3, choices.map { EnglishTime.fullWords(it, h24) }.distinct().size)
                    assertEquals(3, choices.map { EnglishTime.digital(it, h24) }.distinct().size)
                }
            }
        }
    }
}
