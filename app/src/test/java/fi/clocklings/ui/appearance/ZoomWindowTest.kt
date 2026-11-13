package fi.clocklings.ui.appearance

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import fi.clocklings.domain.ClockTime
import fi.clocklings.ui.clock.ClockSpec
import fi.clocklings.ui.theme.SkDimens
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomWindowTest {
    /** The part of the 400 x 400 face, in face units, that the zoom window shows. */
    private val window: Rect =
        run {
            val unit = SkDimens.ZoomClock.value / ClockSpec.VIEW_SIZE
            val width = SkDimens.ItemWindow.value * ZOOM_WINDOW_ASPECT / unit
            val top = -SkDimens.ZoomClockShift.value / unit + ClockSpec.VIEW_ORIGIN
            Rect(
                Offset(ClockSpec.CENTER - width / 2, top),
                Size(width, SkDimens.ZoomWindowHeight.value / unit),
            )
        }

    /** A conservative box around a numeral, sized for the widest and tallest digits of both numeral fonts. */
    private fun numeralBox(
        n: Int,
        pad: Float = 0f,
    ): Rect {
        val centre = ClockSpec.point(ClockSpec.NUMBER_RADIUS, n * ClockSpec.DEGREES_PER_HOUR)
        val halfWidth = (if (n < TWO_DIGITS) ONE_DIGIT_HALF_WIDTH else TWO_DIGIT_HALF_WIDTH) + pad
        val halfHeight = DIGIT_HALF_HEIGHT + pad
        return Rect(centre.x - halfWidth, centre.y - halfHeight, centre.x + halfWidth, centre.y + halfHeight)
    }

    private fun handTipBox(
        length: Float,
        angle: Float,
        width: Float,
    ): Rect {
        val tip = ClockSpec.point(length, angle)
        val radius = width / 2 + ClockSpec.HAND_HALO
        return Rect(tip.x - radius, tip.y - radius, tip.x + radius, tip.y + radius)
    }

    private fun contains(box: Rect) =
        box.left >= window.left && box.right <= window.right && box.top >= window.top && box.bottom <= window.bottom

    @Test
    fun windowIsAtLeastTheMinimumTouchThumbnailInBothDirections() {
        val unit = SkDimens.ZoomClock.value / ClockSpec.VIEW_SIZE
        assertTrue(window.height * unit >= SkDimens.ThumbnailMin.value)
        assertTrue(window.width * unit >= SkDimens.ThumbnailMin.value)
    }

    @Test
    fun windowShowsTheRimAndTheWholeNumeralsElevenTwelveAndOne() {
        assertTrue(window.top <= ClockSpec.CENTER - ClockSpec.RING_RADIUS - ClockSpec.RING_WIDTH / 2)
        // A numeral under the minute hand is redrawn with a halo, which has to fit as well.
        for (n in listOf(11, 12, 1)) assertTrue("numeral $n", contains(numeralBox(n, ClockSpec.NUMERAL_HALO / 2)))
    }

    @Test
    fun windowShowsNoPartOfTenOrTwo() {
        for (n in listOf(10, 2)) assertFalse("numeral $n", window.overlaps(numeralBox(n)))
    }

    @Test
    fun windowShowsBothHandTipsAtTheZoomTime() {
        val hour = handTipBox(ClockSpec.HOUR_LENGTH, ClockTime.hourAngle(ZOOM_TIME.toFloat()), ClockSpec.HOUR_WIDTH)
        val minute =
            handTipBox(ClockSpec.MINUTE_LENGTH, ClockTime.minuteAngle(ZOOM_TIME.toFloat()), ClockSpec.MINUTE_WIDTH)
        assertTrue("hour tip", contains(hour))
        assertTrue("minute tip", contains(minute))
    }

    private companion object {
        const val TWO_DIGITS = 10
        const val ONE_DIGIT_HALF_WIDTH = 12f
        const val TWO_DIGIT_HALF_WIDTH = 24f
        const val DIGIT_HALF_HEIGHT = 15.5f
    }
}
