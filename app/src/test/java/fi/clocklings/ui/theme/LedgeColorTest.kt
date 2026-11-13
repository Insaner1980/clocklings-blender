package fi.clocklings.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgeColorTest {
    @Test
    fun neutralFillUsesSharedLedge() {
        assertEquals(SkColors.BtnShadow, ledgeColor(SkColors.Surface))
    }

    @Test
    fun colouredFillKeepsItsHueAndGetsDeeper() {
        val yellow = SkColors.KouluFill
        val ledge = ledgeColor(yellow)
        // Still warm: red and green clearly above blue, like the fill.
        assertTrue(ledge.red > ledge.blue && ledge.green > ledge.blue)
        // Deeper: more saturated, a little darker.
        assertTrue(saturation(ledge) > saturation(yellow))
        assertTrue(ledge.red <= yellow.red && ledge.green < yellow.green)
    }

    @Test
    fun blueStaysBlue() {
        val ledge = ledgeColor(SkColors.VapaaFill)
        assertTrue(ledge.blue > ledge.red && ledge.blue > ledge.green)
    }

    private fun saturation(c: Color): Float {
        val max = maxOf(c.red, c.green, c.blue)
        val min = minOf(c.red, c.green, c.blue)
        return if (max == 0f) 0f else (max - min) / max
    }
}
