package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrenchTimeTest {
    @Test
    fun clockReadingsUseFrenchHourReferencesAndExactMinutes() {
        val examples =
            mapOf(
                0 to "trois heures",
                1 to "trois heures une minute",
                5 to "trois heures cinq",
                7 to "trois heures sept",
                14 to "trois heures quatorze",
                15 to "trois heures et quart",
                16 to "trois heures seize",
                20 to "trois heures vingt",
                21 to "trois heures vingt et une",
                24 to "trois heures vingt-quatre",
                25 to "trois heures vingt-cinq",
                26 to "trois heures vingt-six",
                29 to "trois heures vingt-neuf",
                30 to "trois heures et demie",
                31 to "trois heures trente et une",
                34 to "trois heures trente-quatre",
                35 to "quatre heures moins vingt-cinq",
                36 to "trois heures trente-six",
                39 to "trois heures trente-neuf",
                40 to "quatre heures moins vingt",
                41 to "trois heures quarante et une",
                44 to "trois heures quarante-quatre",
                45 to "quatre heures moins le quart",
                46 to "trois heures quarante-six",
                50 to "quatre heures moins dix",
                51 to "trois heures cinquante et une",
                55 to "quatre heures moins cinq",
                59 to "trois heures cinquante-neuf",
            )
        examples.forEach { (minute, expected) ->
            assertEquals("03:$minute", expected, FrenchTime.timeWords(180 + minute))
        }
        assertEquals("une heure", FrenchTime.timeWords(60))
        assertEquals("une heure et demie", FrenchTime.timeWords(90))
        assertEquals("une heure moins cinq", FrenchTime.timeWords(775))
        assertEquals("douze heures et demie", FrenchTime.timeWords(750))
    }

    @Test
    fun contextualNoonAndMidnightUseTheNamedHourAndMasculineHalf() {
        val examples =
            mapOf(
                0 to "minuit",
                1 to "minuit une minute",
                30 to "minuit et demi",
                35 to "une heure moins vingt-cinq du matin",
                210 to "trois heures et demie du matin",
                695 to "midi moins vingt-cinq",
                705 to "midi moins le quart",
                715 to "midi moins cinq",
                719 to "onze heures cinquante-neuf du matin",
                720 to "midi",
                721 to "midi une minute",
                750 to "midi et demi",
                755 to "une heure moins vingt-cinq de l'après-midi",
                930 to "trois heures et demie de l'après-midi",
                1050 to "cinq heures et demie de l'après-midi",
                1055 to "six heures moins vingt-cinq du soir",
                1065 to "six heures moins le quart du soir",
                1075 to "six heures moins cinq du soir",
                1079 to "cinq heures cinquante-neuf de l'après-midi",
                1080 to "six heures du soir",
                1140 to "sept heures du soir",
                1410 to "onze heures et demie du soir",
                1415 to "minuit moins vingt-cinq",
                1425 to "minuit moins le quart",
                1435 to "minuit moins cinq",
                1439 to "onze heures cinquante-neuf du soir",
            )
        examples.forEach { (time, expected) -> assertEquals(expected, FrenchTime.fullWords(time, true)) }
        assertEquals("douze heures", FrenchTime.fullWords(0, false))
        assertEquals("douze heures", FrenchTime.fullWords(720, false))
    }

    @Test
    fun numericReadingsUseLiteralHoursAndFeminineAgreement() {
        assertEquals("trois heures trente", FrenchTime.numericWords(210))
        assertEquals("quinze heures trente", FrenchTime.numericWords(930, true))
        assertEquals("zéro heure", FrenchTime.numericWords(0, true))
        assertEquals("zéro heure une minute", FrenchTime.numericWords(1, true))
        assertEquals("une heure une minute", FrenchTime.numericWords(61, true))
        assertEquals("douze heures trente", FrenchTime.numericWords(750, true))
        assertEquals("vingt et une heures vingt et une", FrenchTime.numericWords(1281, true))
        assertEquals("vingt-deux heures vingt-deux", FrenchTime.numericWords(1342, true))
        assertEquals("3:30", FrenchTime.digital(930))
        assertEquals("15:30", FrenchTime.digital(930, true))
        assertEquals("00:00", FrenchTime.digital(0, true))
    }

    @Test
    fun allMinutesAreDistinctAndNormalizeWithoutAddingAnalogDayContext() {
        assertEquals(720, (0 until 720).map(FrenchTime::timeWords).toSet().size)
        assertEquals(1440, (0 until 1440).map { FrenchTime.fullWords(it, true) }.toSet().size)
        assertEquals(1440, (0 until 1440).map { FrenchTime.numericWords(it, true) }.toSet().size)
        for (minute in 0 until 1440) {
            assertEquals(FrenchTime.timeWords(minute), FrenchTime.timeWords(minute + 720))
            assertEquals(FrenchTime.fullWords(minute, true), FrenchTime.fullWords(minute - 1440, true))
            assertEquals(FrenchTime.numericWords(minute, true), FrenchTime.numericWords(minute + 1440, true))
            assertFalse(FrenchTime.timeWords(minute).contains("midi"))
            assertFalse(FrenchTime.timeWords(minute).contains("minuit"))
        }
    }

    @Test
    fun dayLabelsAndSunFollowTheExistingBoundaries() {
        assertEquals("nuit", FrenchTime.dayPart(299).name)
        assertEquals("matin", FrenchTime.dayPart(300).name)
        assertFalse(FrenchTime.dayPart(359).isSun)
        assertTrue(FrenchTime.dayPart(360).isSun)
        assertEquals("midi", FrenchTime.dayPart(720).name)
        assertEquals("après-midi", FrenchTime.dayPart(721).name)
        assertTrue(FrenchTime.dayPart(1079).isSun)
        assertEquals("soir", FrenchTime.dayPart(1080).name)
        assertFalse(FrenchTime.dayPart(1080).isSun)
        assertEquals("nuit", FrenchTime.dayPart(1320).name)
        assertEquals(Greeting.NIGHT, FrenchTime.greeting(299))
        assertEquals(Greeting.MORNING, FrenchTime.greeting(300))
        assertEquals(Greeting.MORNING, FrenchTime.greeting(599))
        assertEquals(Greeting.DAY, FrenchTime.greeting(600))
        assertEquals(Greeting.EVENING, FrenchTime.greeting(1080))
        assertEquals(Greeting.NIGHT, FrenchTime.greeting(1320))
    }
}
