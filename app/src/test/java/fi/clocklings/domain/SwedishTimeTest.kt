package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SwedishTimeTest {
    @Test
    fun exactMinuteFixturesRetainAmountsAndHalfHourReferences() {
        val examples =
            mapOf(
                0 to "tre",
                1 to "en minut över tre",
                2 to "två minuter över tre",
                4 to "fyra minuter över tre",
                5 to "fem över tre",
                6 to "sex minuter över tre",
                14 to "fjorton minuter över tre",
                15 to "kvart över tre",
                16 to "sexton minuter över tre",
                19 to "nitton minuter över tre",
                20 to "tjugo över tre",
                21 to "nio minuter i halv fyra",
                24 to "sex minuter i halv fyra",
                25 to "fem i halv fyra",
                26 to "fyra minuter i halv fyra",
                29 to "en minut i halv fyra",
                30 to "halv fyra",
                31 to "en minut över halv fyra",
                34 to "fyra minuter över halv fyra",
                35 to "fem över halv fyra",
                36 to "sex minuter över halv fyra",
                39 to "nio minuter över halv fyra",
                40 to "tjugo i fyra",
                41 to "nitton minuter i fyra",
                44 to "sexton minuter i fyra",
                45 to "kvart i fyra",
                46 to "fjorton minuter i fyra",
                54 to "sex minuter i fyra",
                55 to "fem i fyra",
                56 to "fyra minuter i fyra",
                58 to "två minuter i fyra",
                59 to "en minut i fyra",
            )
        examples.forEach { (minute, expected) ->
            assertEquals("03:$minute", expected, SwedishTime.timeWords(180 + minute))
        }
    }

    @Test
    fun hourOneAndHourWrappingUseEtt() {
        val examples =
            mapOf(
                60 to "ett",
                61 to "en minut över ett",
                85 to "fem i halv två",
                90 to "halv två",
                95 to "fem över halv två",
                690 to "halv tolv",
                705 to "kvart i tolv",
                720 to "tolv",
                745 to "fem i halv ett",
                750 to "halv ett",
                755 to "fem över halv ett",
                765 to "kvart i ett",
                1439 to "en minut i tolv",
            )
        examples.forEach { (time, expected) -> assertEquals(expected, SwedishTime.timeWords(time)) }
    }

    @Test
    fun noonAndMidnightNeedKnownDayContext() {
        assertEquals("tolv", SwedishTime.fullWords(0, false))
        assertEquals("tolv", SwedishTime.fullWords(720, false))
        assertEquals("midnatt", SwedishTime.fullWords(0, true))
        assertEquals("tolv på dagen", SwedishTime.fullWords(720, true))
        assertEquals("halv fyra på natten", SwedishTime.fullWords(210, true))
        assertEquals("halv fyra på eftermiddagen", SwedishTime.fullWords(930, true))
        assertEquals("en minut i tolv på förmiddagen", SwedishTime.fullWords(719, true))
        assertEquals("en minut över tolv på eftermiddagen", SwedishTime.fullWords(721, true))
    }

    @Test
    fun digitalAndNumericReadingsFollowSelectedStyle() {
        assertEquals("3.07", SwedishTime.digital(187))
        assertEquals("03.07", SwedishTime.digital(187, true))
        assertEquals("15.30", SwedishTime.digital(930, true))
        assertEquals("12.00", SwedishTime.digital(0))
        assertEquals("00.00", SwedishTime.digital(0, true))
        assertEquals("tre noll sju", SwedishTime.numericWords(187))
        assertEquals("tre", SwedishTime.numericWords(180))
        assertEquals("tre trettio", SwedishTime.numericWords(930))
        assertEquals("femton trettio", SwedishTime.numericWords(930, true))
        assertEquals("ett noll ett", SwedishTime.numericWords(61, true))
        assertEquals("tjugoett noll noll", SwedishTime.numericWords(1260, true))
        assertEquals("noll noll noll", SwedishTime.numericWords(0, true))
        assertEquals("tjugotre femtionio", SwedishTime.numericWords(1439, true))
    }
}
