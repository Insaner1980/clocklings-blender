package fi.clocklings.ui.clock

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The text measurer hands the halo outline and the numeral fill the same cached paragraph, so the
 * outline's stroke must never stay on that paragraph's paint and thicken the numeral drawn after it.
 */
@RunWith(RobolectricTestRunner::class)
class ClockFaceHaloRenderTest {
    @get:Rule val compose = createComposeRule()

    private val command = mutableStateOf(ClockCommand(NOON))

    /** Every capture follows a time change: in a long suite the very first frame may not be drawn yet. */
    private fun inkInThreeAt(time: Int): Int {
        compose.runOnIdle { command.value = ClockCommand(time) }
        val ink =
            compose
                .onNodeWithTag(TAG)
                .captureToImage()
                .toPixelMap()
                .inkAround(THREE_X, THREE_Y)
        assertTrue("No numeral 3 drawn at $time", ink > 0)
        return ink
    }

    @Test
    fun `numeral under the minute hand is no bolder than a free one`() {
        showClock()
        val free = inkInThreeAt(HALF_PAST)
        val covered = inkInThreeAt(QUARTER_PAST)
        assertTrue("Covered 3 has $covered ink pixels, a free 3 has $free", covered <= free * TOLERANCE)
    }

    @Test
    fun `halo stroke does not leak into later draws of the same numeral`() {
        showClock()
        val before = inkInThreeAt(HALF_PAST)
        inkInThreeAt(QUARTER_PAST)
        val after = inkInThreeAt(HALF_PAST)
        assertTrue("Free 3 had $before ink pixels, $after after being covered", after <= before * TOLERANCE)
    }

    private fun showClock() =
        compose.setContent {
            ClocklingsTheme { ClockFace(command.value, Modifier.size(CLOCK_DP.dp).testTag(TAG)) }
        }

    private fun PixelMap.inkAround(
        viewX: Float,
        viewY: Float,
    ): Int {
        val scale = width / ClockSpec.VIEW_SIZE
        val cx = ((viewX - ClockSpec.VIEW_ORIGIN) * scale).roundToInt()
        val cy = ((viewY - ClockSpec.VIEW_ORIGIN) * scale).roundToInt()
        val half = (REGION_HALF * scale).roundToInt()
        var count = 0
        for (x in cx - half..cx + half) {
            for (y in cy - half..cy + half) {
                if (this[x, y].isInk()) count++
            }
        }
        return count
    }

    private fun Color.isInk(): Boolean {
        val ink = SkColors.Ink
        return abs(red - ink.red) < INK_DISTANCE &&
            abs(green - ink.green) < INK_DISTANCE &&
            abs(blue - ink.blue) < INK_DISTANCE
    }

    private companion object {
        const val TAG = "clock"
        const val CLOCK_DP = 400
        const val NOON = 0
        const val QUARTER_PAST = 15
        const val HALF_PAST = 30

        /** The numeral 3 sits on the horizontal through the centre, right of the pin. */
        const val THREE_X = ClockSpec.CENTER + ClockSpec.NUMBER_RADIUS
        const val THREE_Y = ClockSpec.CENTER

        /** Covers the glyph and its halo but stays inside the big tick at minute 15. */
        const val REGION_HALF = 24f
        const val INK_DISTANCE = 0.16f

        /** Anti-aliasing over the blue hand differs a little from over the white face. */
        const val TOLERANCE = 1.15f
    }
}
