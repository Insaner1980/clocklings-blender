package fi.clocklings.ui.clock

import fi.clocklings.domain.Mark
import fi.clocklings.domain.MarkColor
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Test

class NumeralCoverTest {
    @Test
    fun `hand at twelve covers twelve`() = assertEquals(listOf(12), ClockSpec.numeralsUnder(0f))

    @Test
    fun `hand at six covers six`() = assertEquals(listOf(6), ClockSpec.numeralsUnder(180f))

    @Test
    fun `hand at nine covers nine`() = assertEquals(listOf(9), ClockSpec.numeralsUnder(270f))

    @Test
    fun `hand just before twelve covers twelve`() = assertEquals(listOf(12), ClockSpec.numeralsUnder(350f))

    @Test
    fun `hand halfway between one and two covers both`() = assertEquals(listOf(1, 2), ClockSpec.numeralsUnder(45f))

    @Test
    fun `hand at minute 3 covers only one`() = assertEquals(listOf(1), ClockSpec.numeralsUnder(18f))

    private fun halo(
        n: Int,
        help: Boolean = false,
        under: List<Mark> = emptyList(),
    ) = ClockSpec.numeralHalo(n, help, under)

    @Test
    fun `glow wins over a sector and the help halves on both sides`() {
        val under = listOf(Mark.Sector(0, 30, MarkColor.RED), Mark.NumberGlow(2))
        assertEquals(
            ClockSpec.NumeralHalo(SkColors.MarkNumber, SkColors.MarkNumber),
            halo(2, help = true, under = under),
        )
    }

    @Test
    fun `help colours a numeral inside one half on both sides`() {
        assertEquals(ClockSpec.NumeralHalo(SkColors.Yli, SkColors.Yli), halo(3, help = true))
        assertEquals(ClockSpec.NumeralHalo(SkColors.Vaille, SkColors.Vaille), halo(9, help = true))
    }

    @Test
    fun `help splits twelve and six along the dividing line`() {
        // Clockwise: the left half (Vaille) comes before twelve and the right half (Yli) after it.
        assertEquals(ClockSpec.NumeralHalo(SkColors.Vaille, SkColors.Yli), halo(12, help = true))
        assertEquals(ClockSpec.NumeralHalo(SkColors.Yli, SkColors.Vaille), halo(6, help = true))
    }

    @Test
    fun `plain face gives face colour on both sides`() {
        assertEquals(ClockSpec.NumeralHalo(SkColors.Face, SkColors.Face), halo(3))
    }

    @Test
    fun `sector starting at a numeral colours only the side after it`() {
        val under = listOf(Mark.Sector(45, 60, MarkColor.BLUE))
        assertEquals(ClockSpec.NumeralHalo(SkColors.MarkBlue, SkColors.Face), halo(12, under = under))
        assertEquals(ClockSpec.NumeralHalo(SkColors.MarkBlue, SkColors.MarkBlue), halo(11, under = under))
    }

    @Test
    fun `sector ending at a numeral colours only the side before it`() {
        val under = listOf(Mark.Sector(0, 15, MarkColor.GREEN))
        assertEquals(ClockSpec.NumeralHalo(SkColors.MarkGreen, SkColors.Face), halo(3, under = under))
        assertEquals(ClockSpec.NumeralHalo(SkColors.Face, SkColors.MarkGreen), halo(12, under = under))
    }

    @Test
    fun `sector wins over help and the last sector wins`() {
        val under = listOf(Mark.Sector(0, 30, MarkColor.RED), Mark.Sector(10, 20, MarkColor.GREEN))
        assertEquals(ClockSpec.NumeralHalo(SkColors.MarkGreen, SkColors.MarkGreen), halo(3, help = true, under = under))
    }
}
