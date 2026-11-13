// Sprite geometry is exported by tools/blender/render_reference_accessories.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot

/** The accessories follow the same part transforms as the rendered character. */
internal class BlenderAccessories(
    private val atlas: ImageBitmap?,
    private val regions: Map<String, List<BlenderAccessoryRegion>>,
) {
    fun draw(
        scope: DrawScope,
        outfit: Outfit,
        part: ArtPart,
        layer: ItemLayer,
    ) {
        val image = atlas ?: return
        for (slot in Slot.entries) {
            val pieces = regions[outfit[slot]] ?: continue
            for (piece in pieces) {
                if (piece.part != part || piece.layer != layer) continue
                scope.drawImage(
                    image = image,
                    srcOffset = piece.source,
                    srcSize = piece.sourceSize,
                    dstOffset = piece.destination,
                    dstSize = piece.size,
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }

    fun crop(
        id: String,
        fallback: Rect,
    ): Rect {
        var bounds = fallback
        for (piece in regions[id].orEmpty()) {
            val left = piece.destination.x.toFloat() - 6f
            val top = piece.destination.y.toFloat() - 6f
            bounds =
                Rect(
                    minOf(bounds.left, left),
                    minOf(bounds.top, top),
                    maxOf(bounds.right, left + piece.size.width + 12f),
                    maxOf(bounds.bottom, top + piece.size.height + 12f),
                )
        }
        return bounds
    }
}

/** The doll's required clothes live in its base atlas; optional modeled items use the accessory atlas. */
internal fun needsBlenderAccessories(
    kind: CompanionKind,
    outfit: Outfit,
    nextOutfit: Outfit? = null,
): Boolean {
    val currentItems = outfit.items.values
    val nextItems = nextOutfit?.items.orEmpty().values
    return when (kind) {
        CompanionKind.NONE -> {
            false
        }

        CompanionKind.HUMAN -> {
            currentItems.any { it in DollAccessoryRegions } || nextItems.any { it in DollAccessoryRegions }
        }

        else -> {
            currentItems.isNotEmpty() || nextItems.isNotEmpty()
        }
    }
}

internal class BlenderAccessoryRegion(
    val part: ArtPart,
    val layer: ItemLayer,
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}
