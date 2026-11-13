// Decorations stay inside the outer rim, outside all numerals, ticks and teaching overlays.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.clock

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockTheme
import fi.clocklings.ui.theme.SkColors
import kotlin.math.cos
import kotlin.math.sin
import fi.clocklings.ui.theme.PersonalizationColors as C

internal fun DrawScope.drawThemedRim(
    appearance: ClockAppearance,
    reveal: Float,
) {
    val (rim, accent) = rimColors(appearance)
    drawCircle(SkColors.Line, 178f, Offset(200f, 200f), style = Stroke(17f))
    drawArc(rim, -90f, reveal, false, Offset(22f, 22f), Size(356f, 356f), style = Stroke(17f))
    if (appearance.theme == ClockTheme.CLASSIC) {
        drawCircle(rim, 168f, Offset(200f, 200f), style = Stroke(2f))
        return
    }
    for (i in 0 until 12) {
        val angle = i * 30f + 15f
        if (angle > reveal) continue
        val point = ClockSpec.point(178f, angle)
        rotate(angle, point) {
            when (appearance.theme) {
                ClockTheme.SPACE -> {
                    spaceMotif(point, accent, i)
                }

                ClockTheme.FOREST -> {
                    leaf(point, accent)
                }

                ClockTheme.OCEAN -> {
                    wave(point, accent)
                }

                ClockTheme.GARDEN -> {
                    flower(point, accent)
                }

                ClockTheme.WORKSHOP -> {
                    bolt(point, accent)
                }

                ClockTheme.CUSTOM -> {
                    customMotif(point, accent, appearance.custom.frame)
                }

                ClockTheme.CLASSIC, ClockTheme.RAINBOW -> {
                    // These themes use their existing rings without decorative motifs.
                }
            }
        }
    }
}

private fun rimColors(appearance: ClockAppearance): Pair<Color, Color> =
    when (appearance.theme) {
        ClockTheme.CLASSIC -> C.Classic to SkColors.Surface
        ClockTheme.SPACE -> C.Space to C.SpaceLight
        ClockTheme.FOREST -> C.Forest to C.ForestLight
        ClockTheme.OCEAN -> C.Ocean to C.OceanLight
        ClockTheme.GARDEN -> C.Garden to C.GardenLight
        ClockTheme.WORKSHOP -> C.Workshop to C.WorkshopLight
        ClockTheme.CUSTOM -> C.clock(appearance.custom.palette) to SkColors.Surface
        ClockTheme.RAINBOW -> SkColors.Ink to SkColors.Surface
    }

private fun DrawScope.spaceMotif(
    at: Offset,
    color: Color,
    index: Int,
) {
    if (index % 3 == 0) {
        drawOval(color, at - Offset(12f, 3f), Size(24f, 6f), style = Stroke(1.5f))
        drawCircle(SkColors.Gold, 5f, at)
    } else {
        val star = Path()
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val radius = if (i % 2 == 0) 6f else 2f
            val x = at.x + cos(angle).toFloat() * radius
            val y = at.y + sin(angle).toFloat() * radius
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, color)
    }
}

private fun DrawScope.leaf(
    at: Offset,
    color: Color,
) {
    val path =
        Path().apply {
            moveTo(at.x - 12f, at.y + 3f)
            quadraticTo(at.x, at.y - 12f, at.x + 12f, at.y - 3f)
            quadraticTo(at.x, at.y + 12f, at.x - 12f, at.y + 3f)
        }
    drawPath(path, color)
    drawLine(C.Forest, at - Offset(8f, -2f), at + Offset(8f, -2f), 1.5f)
}

private fun DrawScope.wave(
    at: Offset,
    color: Color,
) {
    drawArc(color, 5f, 170f, false, at - Offset(13f, 7f), Size(16f, 10f), style = Stroke(3f))
    drawArc(color, 185f, 170f, false, at - Offset(-2f, -2f), Size(16f, 10f), style = Stroke(3f))
    drawCircle(SkColors.Surface, 2f, at + Offset(19f, -4f))
}

private fun DrawScope.flower(
    at: Offset,
    color: Color,
) {
    for (petal in 0 until 5) {
        val angle = petal * Math.PI * 2 / 5
        drawCircle(color, 3.5f, at + Offset(cos(angle).toFloat() * 4f, sin(angle).toFloat() * 4f))
    }
    drawCircle(SkColors.Gold, 2.5f, at)
}

private fun DrawScope.bolt(
    at: Offset,
    color: Color,
) {
    drawRoundRect(color, at - Offset(9f, 6f), Size(18f, 12f), CornerRadius(3f))
    drawCircle(C.Workshop, 3f, at)
    drawLine(color, at - Offset(1.5f, 0f), at + Offset(1.5f, 0f), 1f)
}

private fun DrawScope.customMotif(
    at: Offset,
    color: Color,
    frame: ClockFrame,
) {
    when (frame) {
        ClockFrame.ROUND -> {
            // The plain round frame needs no additional motifs.
        }

        ClockFrame.SCALLOPED -> {
            drawCircle(color.copy(alpha = 0.65f), 7f, at)
            drawCircle(color, 2f, at)
        }

        ClockFrame.BLOCKS -> {
            drawRoundRect(color, at - Offset(10f, 4f), Size(20f, 8f), CornerRadius(2f))
        }
    }
}
