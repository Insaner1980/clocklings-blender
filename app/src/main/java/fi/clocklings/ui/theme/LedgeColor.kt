package fi.clocklings.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * The ledge under a pressable surface: the same hue as [fill], deeper and slightly darker, so a
 * yellow tile sits on a golden ledge instead of a grey or lilac one. Near-white fills, which have
 * no hue of their own, get the shared soft lilac ledge.
 */
fun ledgeColor(fill: Color): Color {
    val r = fill.red
    val g = fill.green
    val b = fill.blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val saturation = if (maxC == 0f) 0f else delta / maxC
    if (saturation < NEUTRAL_SATURATION) return SkColors.BtnShadow
    val hue =
        when (maxC) {
            r -> (g - b) / delta
            g -> (b - r) / delta + GREEN_SECTOR
            else -> (r - g) / delta + BLUE_SECTOR
        }.let { ((it * HUE_SECTOR) + FULL_CIRCLE) % FULL_CIRCLE }
    return Color.hsv(
        hue,
        min(1f, saturation * LEDGE_SATURATION_GAIN + LEDGE_SATURATION_LIFT),
        maxC * LEDGE_VALUE,
        fill.alpha,
    )
}

private const val NEUTRAL_SATURATION = 0.06f
private const val HUE_SECTOR = 60f
private const val GREEN_SECTOR = 2f
private const val BLUE_SECTOR = 4f
private const val FULL_CIRCLE = 360f
private const val LEDGE_SATURATION_GAIN = 1.3f
private const val LEDGE_SATURATION_LIFT = 0.12f
private const val LEDGE_VALUE = 0.9f
