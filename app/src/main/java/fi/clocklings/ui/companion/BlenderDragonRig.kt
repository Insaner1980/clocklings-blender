// Atlas coordinates come from tools/blender/render_dragon_layers.py.
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
internal fun rememberBlenderDragonRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.dragon_blender_blue
            CompanionColor.GREEN -> R.drawable.dragon_blender_green
            CompanionColor.ORANGE -> R.drawable.dragon_blender_orange
            CompanionColor.ORIGINAL, CompanionColor.PURPLE -> R.drawable.dragon_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.dragon_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderDragonRig(atlas, BlenderAccessories(accessories, DragonAccessoryRegions))
    }
}

/** Reuses the dragon's wardrobe anchors and crops, with the existing wing motion. */
internal class BlenderDragonRig(
    private val atlas: ImageBitmap,
    private val accessories: BlenderAccessories,
) : AnimalRig(
        // The crops keep every item of the slot whole. The head crop shows the whole face, above the wings;
        // the neck crop starts below the mouth and is wide enough to show the royal cape beside the body.
        headCrop = Rect(24f, 0f, 216f, 151f),
        neckCrop = Rect(60f, 156f, 180f, 240f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "dragon-extra-coins" to Rect(64f, 174f, 190f, 300f),
                "dragon-extra-book" to Rect(60f, 162f, 180f, 282f),
                "dragon-extra-shield" to Rect(0f, 164f, 120f, 284f),
                "dragon-extra-star-cloud" to Rect(4f, 67f, 237f, 300f),
                "dragon-extra-wings" to Rect(0f, 60f, 240f, 300f),
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
            wings(outfit, pose.partDeg)
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
            part(Body)
            accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
            part(Limbs)
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) { head(outfit, pose.blink) }
        }
    }

    private fun DrawScope.wings(
        outfit: Outfit,
        angle: Float,
    ) {
        val replacement = DragonAccessoryRegions[outfit[Slot.EXTRA]].orEmpty().any { it.part == ArtPart.WINGS }
        rotate(angle, LeftShoulder) {
            if (replacement) {
                clipRect(-width, -height, 120f, 2 * height) {
                    accessories.draw(this, outfit, ArtPart.WINGS, ItemLayer.BEHIND)
                }
            } else {
                part(LeftWing)
            }
        }
        rotate(-angle, RightShoulder) {
            if (replacement) {
                clipRect(120f, -height, 2 * width, 2 * height) {
                    accessories.draw(this, outfit, ArtPart.WINGS, ItemLayer.BEHIND)
                }
            } else {
                part(RightWing)
            }
        }
    }

    private fun DrawScope.head(
        outfit: Outfit,
        blink: Float,
    ) {
        part(Horns)
        part(Head)
        scale(1f, 1f - blink * 0.94f, EyeCenter) { part(Eyes) }
        accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.part(region: DragonRegion) {
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

private class DragonRegion(
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}

private val Head = DragonRegion(IntOffset(0, 0), IntOffset(20, 42), IntSize(200, 122))
private val Body = DragonRegion(IntOffset(604, 0), IntOffset(56, 132), IntSize(184, 150))
private val LeftWing = DragonRegion(IntOffset(1160, 0), IntOffset(0, 134), IntSize(100, 82))
private val RightWing = DragonRegion(IntOffset(1160, 250), IntOffset(140, 134), IntSize(100, 82))
private val Limbs = DragonRegion(IntOffset(0, 456), IntOffset(56, 158), IntSize(130, 128))
private val Horns = DragonRegion(IntOffset(394, 456), IntOffset(70, 12), IntSize(100, 53))
private val Eyes = DragonRegion(IntOffset(698, 456), IntOffset(68, 79), IntSize(105, 45))
private val LeftShoulder = Offset(78f, 166f)
private val RightShoulder = Offset(162f, 166f)
private val Neck = Offset(120f, 158f)
private val EyeCenter = Offset(120f, 100.4f)
private val Ground = ovalRect(120f, 284f, 92f, 11f)
private val FullFrame = Rect(-12f, -64f, 252f, 310f)
