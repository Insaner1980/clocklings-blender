package fi.clocklings.ui.clock

import android.content.Context
import android.content.res.Configuration
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
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.timeLanguage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.math.hypot

/**
 * The 24 h help layout around the pin: the day-part word, its sun or moon, the "past" and "to" labels, the inner
 * 24 h numerals and the circle the hand tails sweep around the pin must stay apart for every day-part word and
 * every to/past word in every language. Text is measured as glyph ink with the real fonts, at one pixel per
 * clock unit, and placed the way ClockFace draws it.
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

    private fun dayWord(text: String): Rect {
        val (bounds, box) = ink(text, styles.day, R.font.fredoka_semibold)
        return bounds.translate(ClockSpec.CENTER - box.width / 2f, ClockSpec.DAY_TEXT_Y - box.baseline)
    }

    private fun zoneLabels(language: AppLanguage): List<Pair<String, Rect>> {
        val past = ClockSpec.zoneLabelAngle(is24 = true)
        val radius = ClockSpec.zoneLabelRadius(is24 = true)
        return listOf(R.string.clock_zone_yli to past, R.string.clock_zone_vaille to -past).map { (id, angle) ->
            val text = localized(language).getString(id)
            text to centered(text, styles.zone, R.font.fredoka_semibold, ClockSpec.point(radius, angle))
        }
    }

    private fun innerNumerals(): List<Pair<String, Rect>> =
        (1..ClockTime.HOURS_PER_HALF_DAY).map { n ->
            val text = ((n + ClockTime.HOURS_PER_HALF_DAY) % ClockTime.HOURS_PER_DAY).toString()
            val at = ClockSpec.point(ClockSpec.INNER_24_RADIUS, n * ClockSpec.DEGREES_PER_HOUR)
            text to centered(text, styles.inner24, R.font.nunito_extrabold, at)
        }

    private fun localized(language: AppLanguage): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language.tag))
        return context.createConfigurationContext(config)
    }

    private fun dayWords(language: AppLanguage): Set<String> =
        (0 until ClockTime.DAY).map { timeLanguage(language).dayPart(it).name }.toSet()

    /** The sun with its rays; the moon fits inside the same square. */
    private val dayIcon =
        Offset(ClockSpec.CENTER, ClockSpec.SUN_CY).let { c ->
            val r = ClockSpec.SUN_RAY_TO + ClockSpec.SUN_RAY_WIDTH / 2f
            Rect(c.x - r, c.y - r, c.x + r, c.y + r)
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
    ) {
        val dx = maxOf(a.left - ClockSpec.CENTER, 0f, ClockSpec.CENTER - a.right)
        val dy = maxOf(a.top - ClockSpec.CENTER, 0f, ClockSpec.CENTER - a.bottom)
        if (hypot(dx, dy) < HUB + GAP) failures += "$what: $a reaches the hub"
    }

    @Test
    fun `day part word and to past labels keep apart from each other and from the numerals and hub`() {
        val numerals = innerNumerals()
        // Every language's to/past words, so labelling the halves in Finnish or Swedish later still fits.
        val labels = AppLanguage.entries.flatMap { zoneLabels(it) }
        for (language in AppLanguage.entries) {
            val (past, to) = zoneLabels(language)
            apart("labels ${past.first} and ${to.first}", past.second, to.second)
            for (word in dayWords(language)) {
                val rect = dayWord(word)
                clearOfHub("day word $word", rect)
                apart("day word $word and its icon", rect, dayIcon)
                labels.forEach { (label, box) -> apart("day word $word and label $label", rect, box) }
                numerals.forEach { (n, box) -> apart("day word $word and numeral $n", rect, box) }
            }
        }
        for ((label, box) in labels) {
            clearOfHub("label $label", box)
            apart("label $label and the day icon", box, dayIcon)
            numerals.forEach { (n, numeral) -> apart("label $label and numeral $n", box, numeral) }
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
