// Atlas coordinates come from tools/blender/render_dog_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
internal fun rememberBlenderDogRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.dog_blender_blue
            CompanionColor.GREEN -> R.drawable.dog_blender_green
            CompanionColor.PURPLE -> R.drawable.dog_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.dog_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.dog_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderDogRig(atlas, BlenderAccessories(accessories, DogAccessoryRegions))
    }
}

/** Reuses the dog's wardrobe anchors and crops, with the existing seated-dog motion. */
internal class BlenderDogRig(
    private val atlas: ImageBitmap,
    private val accessories: BlenderAccessories,
) : AnimalRig(
        // The crops keep every item of the slot whole while leaving out the tail and the mouth and tongue.
        headCrop = Rect(20f, 0f, 220f, 174f),
        neckCrop = Rect(66f, 158f, 174f, 238f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "dog-extra-ball" to Rect(60f, 180f, 180f, 300f),
                "dog-extra-frisbee" to Rect(60f, 84f, 180f, 204f),
                "dog-extra-boots" to Rect(57f, 174f, 183f, 300f),
                "dog-extra-backpack" to Rect(14f, 116f, 185f, 287f),
                "dog-extra-raincoat" to Rect(40f, 128f, 200f, 288f),
            ),
    ) {
    // Keep tall headwear below the wardrobe's star badge, including at 200% font scale.
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
            rotate(pose.partDeg, TailPivot) { part(Tail) }
            part(Body)
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) { head(outfit, pose) }
        }
    }

    private fun DrawScope.head(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        part(Head)
        scale(1f, 1f - pose.blink * 0.94f, EyeCenter) { part(Eyes) }
        part(LeftEar)
        rotate(-pose.partDeg, RightEarPivot) { part(RightEar) }
        accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.part(region: DogRegion) {
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

private class DogRegion(
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}

private val Head = DogRegion(IntOffset(0, 0), IntOffset(48, 28), IntSize(144, 147))
private val Body = DogRegion(IntOffset(436, 0), IntOffset(38, 148), IntSize(164, 138))
private val Tail = DogRegion(IntOffset(932, 0), IntOffset(169, 186), IntSize(59, 90))
private val LeftEar = DogRegion(IntOffset(0, 448), IntOffset(18, 26), IntSize(77, 143))
private val RightEar = DogRegion(IntOffset(236, 448), IntOffset(145, 26), IntSize(77, 143))
private val Eyes = DogRegion(IntOffset(472, 448), IntOffset(68, 83), IntSize(104, 49))
private val TailPivot = Offset(189f, 251f)
private val RightEarPivot = Offset(162f, 52f)
private val Neck = Offset(120f, 160f)
private val EyeCenter = Offset(120f, 108f)
private val Ground = ovalRect(120f, 285f, 92f, 11f)
private val FullFrame = Rect(-12f, -64f, 252f, 310f)
