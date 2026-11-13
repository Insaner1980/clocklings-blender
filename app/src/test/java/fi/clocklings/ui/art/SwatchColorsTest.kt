package fi.clocklings.ui.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.colorOptions
import fi.clocklings.ui.components.swatchNeedsOutline
import fi.clocklings.ui.theme.PersonalizationColors
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SwatchColorsTest {
    @Test
    fun `near white unicorn swatch needs an outline`() = assertTrue(swatchNeedsOutline(PersonalizationColors.Unicorn))

    @Test
    fun `blue swatch does not`() = assertFalse(swatchNeedsOutline(PersonalizationColors.Blue))

    @Test
    fun `options of one companion are clearly different`() {
        val failures = mutableListOf<String>()
        for (kind in CompanionKind.entries.filter { it != CompanionKind.HUMAN && it != CompanionKind.NONE }) {
            failures += similarOptions(kind)
        }
        assertTrue("Options below delta E $MIN_DELTA_E: $failures", failures.isEmpty())
    }

    private fun similarOptions(kind: CompanionKind): List<String> {
        val failures = mutableListOf<String>()
        val options = kind.colorOptions()
        val colors = options.map { PersonalizationColors.companion(kind, it) }
        for (i in colors.indices) {
            for (j in i + 1 until colors.size) {
                val delta = deltaE76(colors[i], colors[j])
                if (delta < MIN_DELTA_E) failures += "$kind ${options[i]} vs ${options[j]}: ${"%.1f".format(delta)}"
            }
        }
        return failures
    }

    /** CIE76 colour difference: the Euclidean distance between two colours in CIE L*a*b*. */
    private fun deltaE76(
        a: Color,
        b: Color,
    ): Double {
        val labA = a.convert(ColorSpaces.CieLab)
        val labB = b.convert(ColorSpaces.CieLab)
        val dl = labA.red - labB.red
        val da = labA.green - labB.green
        val db = labA.blue - labB.blue
        return sqrt((dl * dl + da * da + db * db).toDouble())
    }

    private companion object {
        const val MIN_DELTA_E = 25.0
    }
}
