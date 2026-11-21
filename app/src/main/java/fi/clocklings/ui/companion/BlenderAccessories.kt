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

// The car's model is part of its bodywork, underneath its roof and bumper fittings.
private val AccessoryDrawOrder = listOf(Slot.MODEL) + (Slot.entries - Slot.MODEL)

// The puppy wears the raincoat beneath its cape or collar.
private val DogRaincoatDrawOrder = listOf(Slot.EXTRA, Slot.NECK, Slot.HEAD)

/** The accessories follow the same part transforms as the rendered character. */
internal class BlenderAccessories(
    internal val atlas: ImageBitmap?,
    private val regions: Map<String, List<BlenderAccessoryRegion>>,
) {
    fun draw(
        scope: DrawScope,
        outfit: Outfit,
        part: ArtPart,
        layer: ItemLayer,
        excludingItem: String? = null,
    ) {
        val image = atlas ?: return
        val slots = if (outfit[Slot.EXTRA] == "dog-extra-raincoat") DogRaincoatDrawOrder else AccessoryDrawOrder
        for (slot in slots) {
            val item = outfit[slot]
            if (item == null || item == excludingItem) continue
            for (piece in regions[item].orEmpty()) {
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
