// Atlas coordinates come from tools/blender/render_bunny_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalRect

@Composable
internal fun rememberBlenderBunnyRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.bunny_blender_blue
            CompanionColor.GREEN -> R.drawable.bunny_blender_green
            CompanionColor.PURPLE -> R.drawable.bunny_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.bunny_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.bunny_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderBunnyRig(atlas, BlenderAccessories(accessories, BunnyAccessoryRegions))
    }
}

/** Reuses the bunny's wardrobe anchors, seated pose and independent lop-ear motion. */
internal class BlenderBunnyRig(
    private val atlas: ImageBitmap,
    private val accessories: BlenderAccessories,
) : AnimalRig(
        headCrop = Rect(20f, 0f, 220f, 176f),
        neckCrop = Rect(60f, 160f, 180f, 240f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "bunny-extra-carrot" to Rect(59f, 170f, 179f, 290f),
                "bunny-extra-basket" to Rect(120f, 180f, 240f, 300f),
                "bunny-extra-watering-can" to Rect(81f, 167f, 201f, 287f),
                "bunny-extra-bubbles" to Rect(110f, 163f, 240f, 293f),
                "bunny-extra-bouquet" to Rect(60f, 162f, 180f, 282f),
                "bunny-extra-wings" to Rect(0f, 50f, 240f, 290f),
            ),
    ) {
    override fun full(): Rect = FullFrame

    override fun crop(item: Item): Rect = accessories.crop(item.id, super.crop(item))

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        groundShadow(Ground)
        posedBody(pose) {
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
            part(Body)
            part(Arms)
            accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
            // Only the paws close over held items; leaves and flowers stay in front of the arms.
            clipRect(top = 224f) { part(Arms) }
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) {
                part(Head)
                rotate(pose.partDeg, LeftEarPivot) { part(LeftEar) }
                rotate(-pose.partDeg, RightEarPivot) { part(RightEar) }
                scale(1f, 1f - pose.blink * 0.94f, EyeCenter) { part(Eyes) }
                accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
            }
        }
    }

    private fun DrawScope.part(region: BunnyRegion) {
        drawImage(
            image = atlas,
            srcOffset = region.source,
            srcSize = region.sourceSize,
            dstOffset = region.destination,
            dstSize = region.size,
            filterQuality = FilterQuality.Medium,
        )
    }
}

private class BunnyRegion(
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}

private val Head = BunnyRegion(IntOffset(0, 0), IntOffset(48, 28), IntSize(146, 150))
private val Body = BunnyRegion(IntOffset(442, 0), IntOffset(46, 154), IntSize(161, 133))
private val LeftEar = BunnyRegion(IntOffset(0, 454), IntOffset(0, 42), IntSize(112, 190))
private val RightEar = BunnyRegion(IntOffset(340, 454), IntOffset(133, 42), IntSize(107, 172))
private val Arms = BunnyRegion(IntOffset(665, 454), IntOffset(68, 164), IntSize(107, 89))
private val Eyes = BunnyRegion(IntOffset(665, 725), IntOffset(67, 91), IntSize(108, 44))
private val LeftEarPivot = Offset(84f, 58f)
private val RightEarPivot = Offset(156f, 58f)
private val Neck = Offset(120f, 172f)
private val EyeCenter = Offset(120.5f, 112.75f)
private val Ground = ovalRect(120f, 284f, 96f, 11f)
private val FullFrame = Rect(0f, -80f, 240f, 300f)
