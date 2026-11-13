package fi.clocklings.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens

/** Kuvakkeen kerros: SVG-polku, täyttö ja valinnainen ääriviiva. Polkua ei muuteta luomisen jälkeen. */
@Immutable
class IconLayer(
    d: String,
    val fill: Color? = null,
    val stroke: Color? = null,
    val strokeWidth: Float = 0f,
    val cap: StrokeCap = StrokeCap.Butt,
) {
    val path: Path = PathParser().parsePathString(d).toPath()
}

/** Authored vector icons. */
@Immutable
class SvgIcon(
    val viewBox: Float,
    val layers: List<IconLayer>,
)

@Composable
fun SkIcon(
    icon: SvgIcon,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / icon.viewBox
        scale(s, s, pivot = Offset.Zero) {
            for (layer in icon.layers) {
                layer.fill?.let { drawPath(layer.path, it) }
                layer.stroke?.let {
                    drawPath(
                        layer.path,
                        it,
                        style = Stroke(layer.strokeWidth, cap = layer.cap, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

@Composable
fun StarIcon(
    on: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = SkDimens.Star,
) {
    SkIcon(if (on) SkIcons.StarOn else SkIcons.StarOff, size, modifier)
}

/** Three stars with [count] filled; [description] is read by the screen reader. */
@Composable
fun StarRow(
    count: Int,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = SkDimens.Star,
) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(STAR_COUNT) { StarIcon(it < count, size = size) }
    }
}

private const val STAR_COUNT = 3

object SkIcons {
    val Back = inkIcon("M15 5l-7 7 7 7M8 12h13")
    val Chevron = inkIcon("M9 5l7 7-7 7")
    val Pencil = inkIcon("M4 20l4.5-1L19 8.5 15.5 5 5 15.5zM13.5 7l3.5 3.5")

    /** An eight-tooth gear with a soft fill, generated as an even polygon so every tooth matches. */
    val Settings =
        SvgIcon(
            ICON_VIEW_BOX,
            listOf(
                IconLayer(
                    "M9.91 4.69L10.39 1.83A10.3 10.3 0 0 1 13.61 1.83L14.09 4.69A7.6 7.6 0 0 1 15.68 5.35L18.05" +
                        " 3.67A10.3 10.3 0 0 1 20.33 5.95L18.65 8.32A7.6 7.6 0 0 1 19.31 9.91L22.17 10.39A10.3 10.3" +
                        " 0 0 1 22.17 13.61L19.31 14.09A7.6 7.6 0 0 1 18.65 15.68L20.33 18.05A10.3 10.3 0 0 1 18.05" +
                        " 20.33L15.68 18.65A7.6 7.6 0 0 1 14.09 19.31L13.61 22.17A10.3 10.3 0 0 1 10.39 22.17L9.91 " +
                        "19.31A7.6 7.6 0 0 1 8.32 18.65L5.95 20.33A10.3 10.3 0 0 1 3.67 18.05L5.35 15.68A7.6 7.6 0 " +
                        "0 1 4.69 14.09L1.83 13.61A10.3 10.3 0 0 1 1.83 10.39L4.69 9.91A7.6 7.6 0 0 1 5.35 8.32L3.6" +
                        "7 5.95A10.3 10.3 0 0 1 5.95 3.67L8.32 5.35A7.6 7.6 0 0 1 9.91 4.69Z",
                    SkColors.IconFill,
                    SkColors.Ink,
                    2f,
                ),
                IconLayer("M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0Z", SkColors.Surface, SkColors.Ink, 2f),
            ),
        )
    val Help = inkIcon("M9 18h6M10 21h4M8 14a6 6 0 1 1 8 0l-1 2H9z")
    val HelpOn =
        SvgIcon(
            24f,
            listOf(
                IconLayer("M8 14a6 6 0 1 1 8 0l-1 2H9z", SkColors.Gold, SkColors.Ink, 2f),
                IconLayer("M9 18h6M10 21h4", stroke = SkColors.Ink, strokeWidth = 2f),
            ),
        )
    val Listen = inkIcon("M3 9h4l5-5v16l-5-5H3zM16 8a6 6 0 0 1 0 8M19 4a11 11 0 0 1 0 16")
    val Replay = inkIcon("M4 10a8 8 0 1 1 1 8M4 4v6h6")
    val Hanger = inkIcon("M9 6a3 3 0 1 1 5 2l-2 2v2M12 12L2 18v2h20v-2z")
    val Together =
        inkIcon(
            "M10 7a3 3 0 1 1-6 0a3 3 0 1 1 6 0ZM20 7a3 3 0 1 1-6 0a3 3 0 1 1 6 " +
                "0ZM2 21v-3a5 5 0 0 1 10 0v3M12 21v-3a5 5 0 0 1 10 0v3",
        )
    val Pass = inkIcon("M14 3h6v18h-6M3 12h12M10 7l5 5-5 5")
    val Shuffle = inkIcon("M3 6h3c6 0 6 12 12 12h3M18 15l3 3-3 3M3 18h3c6 0 6-12 12-12h3M18 3l3 3-3 3")
    val Check =
        SvgIcon(
            24f,
            listOf(IconLayer("M5 12l4 4L19 6", stroke = SkColors.Surface, strokeWidth = 3f, cap = StrokeCap.Round)),
        )

    private const val ICON_VIEW_BOX = 24f

    private fun inkIcon(path: String) =
        SvgIcon(
            ICON_VIEW_BOX,
            listOf(IconLayer(path, stroke = SkColors.Ink, strokeWidth = 2f, cap = StrokeCap.Round)),
        )

    private const val STAR_D = "M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5L2.5 9.4l6.6-.9z"
    private const val INK_STROKE = 2.6f
    private const val ICON_DETAIL_STROKE = 2.2f

    val StarOn = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOn, SkColors.StarOnStroke, 1.4f)))
    val StarOff = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOff, SkColors.StarOffStroke, 1.4f)))

    val Lock =
        SvgIcon(
            24f,
            listOf(
                IconLayer("M8 11h8a3 3 0 0 1 3 3v4a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-4a3 3 0 0 1 3-3z", SkColors.Lock),
                IconLayer("M8 11V8a4 4 0 0 1 8 0v3", stroke = SkColors.Lock, strokeWidth = 2.6f),
            ),
        )

    /** An open book with a coloured cover and a red ribbon. */
    val Book =
        SvgIcon(
            40f,
            listOf(
                IconLayer(
                    "M3 9c5.5-2.5 11.5-2 17 1.5 5.5-3.5 11.5-4 17-1.5v25c-5.5-2.5-11.5-2-17 1.5" +
                        "-5.5-3.5-11.5-4-17-1.5z",
                    SkColors.IconBookCover,
                    SkColors.Ink,
                    INK_STROKE,
                ),
                IconLayer(
                    "M6 8.5c4.8-1.8 9.8-1.3 14 1.5v22c-4.2-2.8-9.2-3.3-14-1.5zM34 8.5c-4.8-1.8-9.8-1.3-14 1.5v22" +
                        "c4.2-2.8 9.2-3.3 14-1.5z",
                    SkColors.Surface,
                    SkColors.Ink,
                    ICON_DETAIL_STROKE,
                ),
                IconLayer("M25.5 9.5v12l2.5-2 2.5 2V8.8z", SkColors.Hour),
            ),
        )

    /** A little clock with the app's own pink hour hand and blue minute hand. */
    val Clock =
        SvgIcon(
            40f,
            listOf(
                IconLayer("M20 2a18 18 0 1 1 0 36a18 18 0 1 1 0-36z", SkColors.IconClockRing, SkColors.Ink, INK_STROKE),
                IconLayer("M20 7.5a12.5 12.5 0 1 1 0 25a12.5 12.5 0 1 1 0-25z", SkColors.Surface),
                IconLayer("M20 20V10.5", stroke = SkColors.Minute, strokeWidth = 3f, cap = StrokeCap.Round),
                IconLayer("M20 20l-5.5-4.5", stroke = SkColors.Hour, strokeWidth = 3.5f, cap = StrokeCap.Round),
                IconLayer("M20 17.8a2.2 2.2 0 1 1 0 4.4a2.2 2.2 0 1 1 0-4.4z", SkColors.Ink),
            ),
        )

    val Star = SvgIcon(24f, listOf(IconLayer(STAR_D, SkColors.StarOn, SkColors.StarOnStroke, 1.4f)))

    val Dress =
        SvgIcon(
            40f,
            listOf(
                IconLayer("M14 5h12l-2 7 9 22H7l9-22z", SkColors.Surface, SkColors.Ink, INK_STROKE),
                IconLayer("M15 12h10", stroke = SkColors.MenuDressBelt, strokeWidth = INK_STROKE),
            ),
        )
}
