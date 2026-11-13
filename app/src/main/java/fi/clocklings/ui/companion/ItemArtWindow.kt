package fi.clocklings.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes

/**
 * Every item preview sits in the same rounded window, so crops that reach the canvas edge read as a
 * framed close-up rather than a cut-off picture.
 */
@Composable
internal fun ItemArtWindow(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(SkDimens.ItemWindow)
            .clip(SkShapes.Caption)
            .background(SkColors.ItemWindow),
        contentAlignment = Alignment.Center,
    ) {
        CompanionView(
            item.companion,
            outfit.previewFor(item),
            Modifier.size(SkDimens.ItemWindow),
            color = color,
            animated = false,
            cropItem = item,
        )
    }
}

/** Draws the content without colour. */
internal fun Modifier.grayscale(): Modifier =
    drawWithContent {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
