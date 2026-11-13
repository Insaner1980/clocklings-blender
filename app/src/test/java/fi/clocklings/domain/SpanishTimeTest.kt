package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpanishTimeTest {
    @Test
    fun conversationalAndExactMinutesUseTheIntendedHour() {
        val expected =
            mapOf(
                0 to "las tres",
                1 to "las tres y un minuto",
                7 to "las tres y siete",
                14 to "las tres y catorce",
                15 to "las tres y cuarto",
                16 to "las tres y dieciséis",
                20 to "las tres y veinte",
                21 to "las tres y veintiuno",
                24 to "las tres y veinticuatro",
                25 to "las tres y veinticinco",
                26 to "las tres y veintiséis",
                29 to "las tres y veintinueve",
                30 to "las tres y media",
                31 to "las tres y treinta y uno",
                34 to "las tres y treinta y cuatro",
                35 to "las cuatro menos veinticinco",
                36 to "las tres y treinta y seis",
                39 to "las tres y treinta y nueve",
                40 to "las cuatro menos veinte",
                41 to "las tres y cuarenta y uno",
                44 to "las tres y cuarenta y cuatro",
                45 to "las cuatro menos cuarto",
                46 to "las tres y cuarenta y seis",
                51 to "las tres y cincuenta y uno",
                55 to "las cuatro menos cinco",
                59 to "las tres y cincuenta y nueve",
            )
        expected.forEach { (minute, words) -> assertEquals(words, SpanishTime.timeWords(180 + minute)) }
    }

    @Test
    fun sentencesAgreeWithTheNamedHourAndNumericalUnits() {
        assertEquals("Es la una", SpanishTime.timeSentence(60, false))
        assertEquals("Son las dos", SpanishTime.timeSentence(120, false))
        assertEquals("Es la una menos cinco", SpanishTime.timeSentence(775, false))
        assertEquals("las veintiuna horas y veintiún minutos", SpanishTime.numericWords(1281, true))
        assertEquals("las veintidós horas y veintidós minutos", SpanishTime.numericWords(1342, true))
        assertEquals("las quince horas y treinta y un minutos", SpanishTime.numericWords(931, true))
        assertEquals("las quince horas y cuarenta y un minutos", SpanishTime.numericWords(941, true))
        assertEquals("las quince horas y cincuenta y un minutos", SpanishTime.numericWords(951, true))
        assertEquals("la una y un minuto", SpanishTime.numericWords(61, true))
        assertEquals("las tres horas y once minutos", SpanishTime.numericWords(191, true))
        assertEquals("las cero horas", SpanishTime.numericWords(0, true))
        assertEquals("las doce horas", SpanishTime.numericWords(720, true))
        assertEquals("las quince horas y treinta minutos", SpanishTime.numericWords(930, true))
    }

    @Test
    fun contextualNoonMidnightAndNamedHoursRemainUnambiguous() {
        assertEquals("las doce", SpanishTime.fullWords(0, false))
        assertEquals("las doce", SpanishTime.fullWords(720, false))
        assertEquals("medianoche", SpanishTime.fullWords(0, true))
        assertEquals("mediodía", SpanishTime.fullWords(720, true))
        assertEquals("Es medianoche", SpanishTime.timeSentence(0, true))
        assertEquals("Es mediodía", SpanishTime.timeSentence(720, true))
        assertEquals("las doce menos cinco del mediodía", SpanishTime.fullWords(715, true))
        assertEquals("las doce menos cinco de la noche", SpanishTime.fullWords(1435, true))
        assertEquals("las doce y un minuto de la noche", SpanishTime.fullWords(1, true))
        assertEquals("las doce y un minuto del mediodía", SpanishTime.fullWords(721, true))
        assertEquals("las tres y media de la tarde", SpanishTime.fullWords(930, true))
        assertEquals("las tres y media de la madrugada", SpanishTime.fullWords(210, true))
        assertEquals("las siete de la tarde", SpanishTime.fullWords(1140, true))
        assertEquals("las ocho de la noche", SpanishTime.fullWords(1200, true))
    }

    @Test
    fun allMinutesAreDistinctAndNormalizeAcrossDays() {
        assertEquals(720, (0 until 720).map(SpanishTime::timeWords).toSet().size)
        assertEquals(1440, (0 until 1440).map { SpanishTime.fullWords(it, true) }.toSet().size)
        for (minute in 0 until 1440) {
            assertEquals(SpanishTime.timeWords(minute), SpanishTime.timeWords(minute + 720))
            assertEquals(SpanishTime.fullWords(minute, true), SpanishTime.fullWords(minute - 1440, true))
            assertEquals(SpanishTime.numericWords(minute, true), SpanishTime.numericWords(minute + 1440, true))
        }
        assertEquals("00:00", SpanishTime.digital(1440, true))
        assertEquals("23:59", SpanishTime.digital(-1, true))
        assertEquals("12:00", SpanishTime.digital(0, false))
    }

    @Test
    fun culturalLabelsDoNotChangeTheTeachingSunOrGreetingBoundaries() {
        assertEquals("madrugada", SpanishTime.dayPart(359).name)
        assertEquals("mañana", SpanishTime.dayPart(360).name)
        assertEquals("tarde", SpanishTime.dayPart(721).name)
        assertEquals("noche", SpanishTime.dayPart(1200).name)
        assertFalse(SpanishTime.dayPart(359).isSun)
        assertTrue(SpanishTime.dayPart(360).isSun)
        assertTrue(SpanishTime.dayPart(1079).isSun)
        assertFalse(SpanishTime.dayPart(1080).isSun)
        assertEquals(Greeting.NIGHT, SpanishTime.greeting(299))
        assertEquals(Greeting.MORNING, SpanishTime.greeting(300))
        assertEquals(Greeting.DAY, SpanishTime.greeting(600))
        assertEquals(Greeting.EVENING, SpanishTime.greeting(1080))
        assertEquals(Greeting.NIGHT, SpanishTime.greeting(1320))
    }
}
