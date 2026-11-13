package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TimeLanguageTest {
    @Test
    fun dispatcherKeepsLanguageSpecificReferenceHours() {
        assertEquals("half past three", timeLanguage(AppLanguage.ENGLISH).timeWords(210))
        assertEquals("puoli neljä", timeLanguage(AppLanguage.FINNISH).timeWords(210))
        assertEquals("halv fyra", timeLanguage(AppLanguage.SWEDISH).timeWords(210))
    }

    @Test
    fun finnishNumericReadingUsesDisplayMode() {
        assertEquals("kolme nolla seitsemän", FinnishTime.numericWords(187))
        assertEquals("tasan kolme", FinnishTime.numericWords(180))
        assertEquals("kolme kolmekymmentä", FinnishTime.numericWords(930))
        assertEquals("viisitoista kolmekymmentä", FinnishTime.numericWords(930, true))
        assertEquals("nolla nolla nolla", FinnishTime.numericWords(0, true))
        assertEquals("nolla nolla yksi", FinnishTime.numericWords(1, true))
        assertEquals("kaksikymmentäkolme viisikymmentäyhdeksän", FinnishTime.numericWords(1439, true))
    }

    @Test
    fun everyAnalogMinuteIsDistinctAndIndependentOfDayContext() {
        AppLanguage.entries.forEach { language ->
            val formatter = timeLanguage(language)
            val phrases = (0 until 720).map(formatter::timeWords)
            assertEquals(language.tag, 720, phrases.toSet().size)
            for (time in 0 until 720) {
                assertTrue(phrases[time].isNotBlank())
                assertFalse(phrases[time].contains("  "))
                assertEquals(phrases[time], formatter.timeWords(time + 720))
                assertEquals(phrases[time], formatter.fullWords(time, false))
                assertEquals(formatter.digital(time), formatter.digital(time + 720))
                assertEquals(formatter.numericWords(time), formatter.numericWords(time + 720))
            }
        }
    }

    @Test
    fun everyFullDayMinuteHasDistinctContextAndNormalizesAcrossDays() {
        AppLanguage.entries.forEach { language ->
            val formatter = timeLanguage(language)
            assertEquals(language.tag, 1440, (0 until 1440).map { formatter.fullWords(it, true) }.toSet().size)
            assertEquals(language.tag, 1440, (0 until 1440).map { formatter.digital(it, true) }.toSet().size)
            assertEquals(language.tag, 1440, (0 until 1440).map { formatter.numericWords(it, true) }.toSet().size)
            for (time in 0 until 1440) {
                assertTrue(formatter.fullWords(time, true).isNotBlank())
                assertTrue(formatter.numericWords(time, true).isNotBlank())
                assertNotEquals(formatter.fullWords(time, true), formatter.fullWords(time + 720, true))
                assertEquals(time in 360 until 1080, formatter.dayPart(time).isSun)
                for (offset in listOf(-2880, -1440, 1440, 2880)) {
                    assertEquals(formatter.timeWords(time), formatter.timeWords(time + offset))
                    assertEquals(formatter.fullWords(time, true), formatter.fullWords(time + offset, true))
                    assertEquals(formatter.digital(time, true), formatter.digital(time + offset, true))
                    assertEquals(formatter.numericWords(time, true), formatter.numericWords(time + offset, true))
                    assertEquals(formatter.dayPart(time), formatter.dayPart(time + offset))
                    assertEquals(formatter.greeting(time), formatter.greeting(time + offset))
                }
            }
        }
    }

    @Test
    fun dayPartNamesCoverEveryCategoryBoundaryAndAdjacentMinutes() {
        val expectedNames =
            mapOf(
                AppLanguage.ENGLISH to
                    listOf("midnight", "night", "morning", "morning", "noon", "afternoon", "evening", "night"),
                AppLanguage.FINNISH to
                    listOf("keskiyö", "yö", "aamu", "aamupäivä", "keskipäivä", "iltapäivä", "ilta", "yö"),
                AppLanguage.SWEDISH to
                    listOf("midnatt", "natt", "morgon", "förmiddag", "middag", "eftermiddag", "kväll", "natt"),
            )
        expectedNames.forEach { (language, names) ->
            val formatter = timeLanguage(language)
            val intervals =
                listOf(
                    0..0 to names[0],
                    1..299 to names[1],
                    300..599 to names[2],
                    600..719 to names[3],
                    720..720 to names[4],
                    721..1079 to names[5],
                    1080..1319 to names[6],
                    1320..1439 to names[7],
                )
            intervals.forEach { (range, name) ->
                assertEquals("$language ${range.first}", name, formatter.dayPart(range.first).name)
                assertEquals("$language ${range.last}", name, formatter.dayPart(range.last).name)
                if (range.first < range.last) assertEquals(name, formatter.dayPart(range.first + 1).name)
            }
            assertEquals(names[2], formatter.dayPart(359).name)
            assertEquals(names[2], formatter.dayPart(360).name)
            assertEquals(names[2], formatter.dayPart(361).name)
            assertEquals(names[7], formatter.dayPart(-1).name)
            assertEquals(names[0], formatter.dayPart(1440).name)
            assertEquals(names[1], formatter.dayPart(1441).name)
        }
    }

    @Test
    fun greetingsKeepTeachingBoundariesAcrossAllLanguages() {
        val examples =
            mapOf(
                -1 to Greeting.NIGHT,
                0 to Greeting.NIGHT,
                1 to Greeting.NIGHT,
                299 to Greeting.NIGHT,
                300 to Greeting.MORNING,
                301 to Greeting.MORNING,
                359 to Greeting.MORNING,
                360 to Greeting.MORNING,
                361 to Greeting.MORNING,
                599 to Greeting.MORNING,
                600 to Greeting.DAY,
                601 to Greeting.DAY,
                719 to Greeting.DAY,
                720 to Greeting.DAY,
                721 to Greeting.DAY,
                1079 to Greeting.DAY,
                1080 to Greeting.EVENING,
                1081 to Greeting.EVENING,
                1319 to Greeting.EVENING,
                1320 to Greeting.NIGHT,
                1321 to Greeting.NIGHT,
                1439 to Greeting.NIGHT,
                1440 to Greeting.NIGHT,
                1441 to Greeting.NIGHT,
            )
        AppLanguage.entries.forEach { language ->
            examples.forEach { (time, greeting) -> assertEquals(greeting, timeLanguage(language).greeting(time)) }
        }
    }

    @Test
    fun generatedPracticeChoicesRemainDistinctInEveryLanguage() {
        AppLanguage.entries.forEach { language ->
            val formatter = timeLanguage(language)
            for (h24 in listOf(false, true)) {
                for (level in Levels.all) {
                    assertDistinctChoices(formatter, h24, level)
                }
            }
        }
    }

    private fun assertDistinctChoices(
        formatter: TimeLanguage,
        h24: Boolean,
        level: Level,
    ) {
        for (time in 0 until ClockTime.period(h24) step level.step) {
            val choices = Levels.optionsFor(time, level, h24, Random(time))
            assertEquals("$formatter $time", 3, choices.map { formatter.fullWords(it, h24) }.distinct().size)
        }
    }
}
