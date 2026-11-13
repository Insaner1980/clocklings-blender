// Atlas coordinates come from tools/blender/render_car_layers.py.
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
internal fun rememberBlenderCarRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.car_blender_blue
            CompanionColor.GREEN -> R.drawable.car_blender_green
            CompanionColor.PURPLE -> R.drawable.car_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.car_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.car_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderCarRig(atlas, BlenderAccessories(accessories, CarAccessoryRegions))
    }
}

/** Keeps the car's roof, bumper and hub anchors while the body rocks on its wheels. */
internal class BlenderCarRig(
    private val atlas: ImageBitmap,
    private val accessories: BlenderAccessories,
) : AnimalRig(
        // The crops keep every item of the slot whole: the roof items rise above the cabin, the front items
        // sit on the face and bumper, and the square wheel crop shows both side wheels inside the grid.
        crops =
            mapOf(
                Slot.ROOF to Rect(10f, 96f, 200f, 226f),
                Slot.FRONT to Rect(84f, 186f, 236f, 296f),
                Slot.WHEELS to Rect(6f, 190f, 116f, 300f),
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
            part(Wheels)
            accessories.draw(this, outfit, ArtPart.WHEELS, ItemLayer.FRONT)
            rotate(pose.partDeg * 0.25f, RockPivot) {
                accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
                part(Body)
                scale(1f, 1f - pose.blink * 0.94f, LeftEyeCenter) { part(LeftEye) }
                scale(1f, 1f - pose.blink * 0.94f, RightEyeCenter) { part(RightEye) }
                accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
            }
        }
    }

    private fun DrawScope.part(region: CarRegion) {
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

private class CarRegion(
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}

private val Body = CarRegion(IntOffset(0, 0), IntOffset(3, 150), IntSize(237, 129))
private val Wheels = CarRegion(IntOffset(0, 391), IntOffset(2, 191), IntSize(224, 92))
private val LeftEye = CarRegion(IntOffset(0, 671), IntOffset(100, 215), IntSize(47, 30))
private val RightEye = CarRegion(IntOffset(145, 671), IntOffset(191, 212), IntSize(33, 30))
private val RockPivot = Offset(120f, 276f)
private val LeftEyeCenter = Offset(127f, 231f)
private val RightEyeCenter = Offset(206.5f, 229f)
private val Ground = ovalRect(122f, 278f, 104f, 8f)
private val FullFrame = Rect(-24f, 76f, 248f, 306f)
