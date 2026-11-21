package fi.clocklings.ui.clock

import android.content.Context
import android.graphics.Paint
import androidx.annotation.FontRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import fi.clocklings.domain.ClockTime
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.hypot

/**
 * The inner 24 h numerals must clear each other, the main numerals and the hub.
 * Measure glyph ink with the real fonts and place it the way ClockFace draws it.
 */
@RunWith(RobolectricTestRunner::class)
class ClockDayPartLayoutTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = Density(1f)
    private val styles = ClockTextStyles(density)
    private val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)

    private data class TextBox(
        val width: Float,
        val height: Float,
        val baseline: Float,
    )

    /**
     * Glyph ink of [text] relative to the top-left of its line box, and the line box. The ink comes from the
     * font file [font], the face [style] resolves to; the text measurer only reports line boxes.
     */
    private fun ink(
        text: String,
        style: TextStyle,
        @FontRes font: Int,
    ): Pair<Rect, TextBox> {
        val layout = measurer.measure(text, style)
        val paint =
            Paint().apply {
                typeface = ResourcesCompat.getFont(context, font)
                textSize = style.fontSize.value
            }
        val bounds = android.graphics.Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        val box = TextBox(layout.size.width.toFloat(), layout.size.height.toFloat(), layout.firstBaseline)
        return bounds.toComposeRect().translate(0f, box.baseline) to box
    }

    private fun centered(
        text: String,
        style: TextStyle,
        @FontRes font: Int,
        at: Offset,
    ): Rect {
        val (bounds, box) = ink(text, style, font)
        return bounds.translate(at.x - box.width / 2f, at.y - box.height / 2f)
    }

    private fun innerNumerals(): List<Pair<String, Rect>> =
        (1..ClockTime.HOURS_PER_HALF_DAY).map { n ->
            val text = ((n + ClockTime.HOURS_PER_HALF_DAY) % ClockTime.HOURS_PER_DAY).toString()
            val at = ClockSpec.point(ClockSpec.INNER_24_RADIUS, n * ClockSpec.DEGREES_PER_HOUR)
            text to centered(text, styles.inner24, R.font.nunito_extrabold, at)
        }

    private val failures = mutableListOf<String>()

    private fun apart(
        what: String,
        a: Rect,
        b: Rect,
    ) {
        val clear =
            a.right + GAP <= b.left || b.right + GAP <= a.left || a.bottom + GAP <= b.top || b.bottom + GAP <= a.top
        if (!clear) failures += "$what: $a and $b"
    }

    private fun clearOfHub(
        what: String,
        a: Rect,
        radius: Float,
    ) {
        val dx = maxOf(a.left - ClockSpec.CENTER, 0f, ClockSpec.CENTER - a.right)
        val dy = maxOf(a.top - ClockSpec.CENTER, 0f, ClockSpec.CENTER - a.bottom)
        if (hypot(dx, dy) < radius + GAP) failures += "$what: $a reaches the hub"
    }

    @Test
    fun `inner numerals clear each other the outer numerals and the hub`() {
        val numerals = innerNumerals()
        val plain = ClockTextStyles(density, plainNumbers = true)
        for ((index, numeral) in numerals.withIndex()) {
            val (n, box) = numeral
            numerals.drop(index + 1).forEach { (other, otherBox) ->
                apart("inner numerals $n and $other", box, otherBox)
            }
            clearOfHub("inner numeral $n", box, HUB)
            for (outer in 1..ClockTime.HOURS_PER_HALF_DAY) {
                val at = ClockSpec.point(ClockSpec.NUMBER_RADIUS, outer * ClockSpec.DEGREES_PER_HOUR)
                val decorated = centered("$outer", styles.number, R.font.fredoka_semibold, at)
                val simple = centered("$outer", plain.number, R.font.nunito_semibold, at)
                apart("inner numeral $n and decorated outer numeral $outer", box, decorated)
                apart("inner numeral $n and plain outer numeral $outer", box, simple)
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private companion object {
        /** Clear space kept between two items, in clock units. */
        const val GAP = 2f

        /** The pin and the round tails of the hands, which sweep this circle whatever the time. */
        const val HUB = ClockSpec.MINUTE_TAIL + ClockSpec.MINUTE_WIDTH / 2f + ClockSpec.HAND_HALO
    }
}
