package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BrazilianPortugueseTimeTest {
    @Test
    fun analogReadingsUseBrazilianFractionsAndExactMinutes() {
        val expected =
            mapOf(
                60 to "uma hora",
                61 to "uma hora e um minuto",
                122 to "duas horas e dois minutos",
                180 to "três horas",
                181 to "três horas e um minuto",
                194 to "três horas e catorze minutos",
                195 to "três e quinze",
                196 to "três horas e dezesseis minutos",
                200 to "três e vinte",
                201 to "três horas e vinte e um minutos",
                204 to "três horas e vinte e quatro minutos",
                205 to "três e vinte e cinco",
                206 to "três horas e vinte e seis minutos",
                209 to "três horas e vinte e nove minutos",
                210 to "três e meia",
                211 to "três horas e trinta e um minutos",
                214 to "três horas e trinta e quatro minutos",
                215 to "vinte e cinco para as quatro",
                216 to "três horas e trinta e seis minutos",
                219 to "três horas e trinta e nove minutos",
                220 to "vinte para as quatro",
                221 to "três horas e quarenta e um minutos",
                224 to "três horas e quarenta e quatro minutos",
                225 to "quinze para as quatro",
                226 to "três horas e quarenta e seis minutos",
                231 to "três horas e cinquenta e um minutos",
                239 to "três horas e cinquenta e nove minutos",
                0 to "doze horas",
                30 to "doze e meia",
                55 to "cinco para a uma",
            )
        for ((time, words) in expected) {
            assertEquals("$time", words, BrazilianPortugueseTime.timeWords(time))
            assertEquals("$time + 720", words, BrazilianPortugueseTime.timeWords(time + 720))
        }
    }

    @Test
    fun sentencesAgreeWithHoursAndRemainingMinutes() {
        assertEquals("É uma hora e um minuto", BrazilianPortugueseTime.timeSentence(61, false))
        assertEquals("São duas horas e dois minutos", BrazilianPortugueseTime.timeSentence(122, false))
        assertEquals("Faltam vinte e cinco minutos para as quatro", BrazilianPortugueseTime.timeSentence(215, false))
        assertEquals("Faltam cinco minutos para a uma", BrazilianPortugueseTime.timeSentence(55, false))
        assertEquals("É meio-dia e meia", BrazilianPortugueseTime.timeSentence(750, true))
        assertEquals("É meia-noite e meia", BrazilianPortugueseTime.timeSentence(30, true))
    }

    @Test
    fun contextualReadingsNameNoonMidnightAndTheReferencedHour() {
        val expected =
            mapOf(
                0 to "meia-noite",
                1 to "meia-noite e um minuto",
                30 to "meia-noite e meia",
                55 to "cinco para a uma da madrugada",
                355 to "cinco para as seis da manhã",
                715 to "cinco para o meio-dia",
                719 to "onze horas e cinquenta e nove minutos da manhã",
                720 to "meio-dia",
                721 to "meio-dia e um minuto",
                750 to "meio-dia e meia",
                775 to "cinco para a uma da tarde",
                930 to "três e meia da tarde",
                1075 to "cinco para as seis da noite",
                1435 to "cinco para a meia-noite",
                1439 to "onze horas e cinquenta e nove minutos da noite",
            )
        for ((time, words) in expected) assertEquals("$time", words, BrazilianPortugueseTime.fullWords(time, true))
    }

    @Test
    fun numericReadingsKeepHourAndMinuteGenderSeparate() {
        assertEquals("zero hora", BrazilianPortugueseTime.numericWords(0, true))
        assertEquals("zero hora e um minuto", BrazilianPortugueseTime.numericWords(1, true))
        assertEquals("quinze horas e trinta minutos", BrazilianPortugueseTime.numericWords(930, true))
        assertEquals("vinte e uma horas e vinte e um minutos", BrazilianPortugueseTime.numericWords(1281, true))
        assertEquals("vinte e duas horas e vinte e dois minutos", BrazilianPortugueseTime.numericWords(1342, true))
        assertEquals("23:59", BrazilianPortugueseTime.digital(-1, true))
        assertEquals("12:00", BrazilianPortugueseTime.digital(0, false))
        assertEquals("00:00", BrazilianPortugueseTime.digital(1440, true))
    }
}
