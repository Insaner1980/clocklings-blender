package fi.clocklings.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import fi.clocklings.R
import fi.clocklings.domain.DayPart
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import kotlin.math.cos
import kotlin.math.sin

private val MoonPath = PathParser().parsePathString("M3 -12A12 12 0 0 0 3 12A3 12 0 0 1 3 -12Z").toPath()

@Composable
internal fun DayPartLabels(
    part: DayPart,
    showZones: Boolean,
) {
    if (!showZones) {
        DayPartChip(part)
        return
    }
    Layout(
        content = {
            ZonePill(stringResource(R.string.clock_zone_vaille), SkColors.Vaille, SkColors.ZoneToBorder)
            DayPartChip(part)
            ZonePill(stringResource(R.string.clock_zone_yli), SkColors.Yli, SkColors.ZonePastBorder)
        },
    ) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val (to, dayPart, past) = children.map { it.measure(loose) }
        val gap = SkDimens.GapS.roundToPx()
        val zonesWidth = to.width + gap + past.width
        val oneLineWidth = zonesWidth + gap + dayPart.width
        val zoneHeight = maxOf(to.height, past.height)
        val oneLine = oneLineWidth <= constraints.maxWidth
        val width = constraints.constrainWidth(if (oneLine) oneLineWidth else maxOf(zonesWidth, dayPart.width))
        val height =
            constraints.constrainHeight(
                if (oneLine) maxOf(zoneHeight, dayPart.height) else dayPart.height + gap + zoneHeight,
            )
        layout(width, height) {
            if (oneLine) {
                val left = (width - oneLineWidth) / 2
                to.place(left, (height - to.height) / 2)
                dayPart.place(left + to.width + gap, (height - dayPart.height) / 2)
                past.place(left + to.width + dayPart.width + 2 * gap, (height - past.height) / 2)
            } else {
                val left = (width - zonesWidth) / 2
                val zoneTop = dayPart.height + gap
                dayPart.place((width - dayPart.width) / 2, 0)
                to.place(left, zoneTop + (zoneHeight - to.height) / 2)
                past.place(left + to.width + gap, zoneTop + (zoneHeight - past.height) / 2)
            }
        }
    }
}

@Composable
private fun ZonePill(
    text: String,
    fill: Color,
    border: Color,
) {
    Box(
        Modifier
            .heightIn(min = SkDimens.DayPartHeight)
            .background(fill, SkShapes.Pill)
            .border(SkDimens.BorderThin, border, SkShapes.Pill)
            .padding(horizontal = SkDimens.Gap),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = SkColors.Ink, style = SkType.DayPart, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun DayPartChip(part: DayPart) {
    Row(
        Modifier
            .heightIn(min = SkDimens.DayPartHeight)
            .background(SkColors.Surface, SkShapes.Pill)
            .padding(start = SkDimens.GapLabel, end = SkDimens.GapL),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(SkDimens.DayPartIcon)) {
            val unit = size.width / ICON_VIEW_SIZE
            if (part.isSun) {
                drawCircle(SkColors.Sun, SUN_RADIUS * unit)
                repeat(SUN_RAYS) {
                    val angle = 2 * Math.PI * it / SUN_RAYS
                    val ray = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * unit
                    drawLine(
                        SkColors.SunRay,
                        center + ray * SUN_RAY_START,
                        center + ray * SUN_RAY_END,
                        SUN_RAY_WIDTH * unit,
                        StrokeCap.Round,
                    )
                }
            } else {
                withTransform({
                    translate(center.x, center.y)
                    scale(unit, unit, Offset.Zero)
                }) { drawPath(MoonPath, SkColors.Moon) }
            }
        }
        Text(part.name, style = SkType.DayPart, maxLines = 1, softWrap = false)
    }
}

private const val ICON_VIEW_SIZE = 32f
private const val SUN_RADIUS = 8f
private const val SUN_RAYS = 8
private const val SUN_RAY_START = 11.5f
private const val SUN_RAY_END = 14.5f
private const val SUN_RAY_WIDTH = 3f
