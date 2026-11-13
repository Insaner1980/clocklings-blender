package fi.clocklings.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import fi.clocklings.ui.theme.SkColors

private const val GROUND_SHADOW_ALPHA = 0.22f
private const val UNIT_CENTER = 0.5f
private val UnitSize = Size(1f, 1f)
private val GroundShadowBrush =
    Brush.radialGradient(
        0f to SkColors.Ink.copy(alpha = GROUND_SHADOW_ALPHA),
        1f to SkColors.Ink.copy(alpha = 0f),
        center = Offset(UNIT_CENTER, UNIT_CENTER),
        radius = UNIT_CENTER,
    )

/** Wide ground shadow: the shared unit-box gradient fades to transparent at the whole edge. */
fun DrawScope.groundShadow(bounds: Rect) {
    if (bounds.width <= 0f || bounds.height <= 0f) return
    withTransform({
        translate(bounds.left, bounds.top)
        // Preserve the original pivot transform's pixel rounding in tightly cropped thumbnails.
        rotate(0f, Offset(bounds.width / 2, bounds.height / 2))
        scale(bounds.width, bounds.height, Offset.Zero)
    }) {
        drawOval(GroundShadowBrush, Offset.Zero, UnitSize)
    }
}

fun ovalRect(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float = rx,
): Rect = Rect(cx - rx, cy - ry, cx + rx, cy + ry)
