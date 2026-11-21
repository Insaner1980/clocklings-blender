// Atlas coordinates match the wardrobe export in art/blender/car.blend.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
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
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        fullFrame = FullFrame,
        ground = Ground,
        // The crops keep every item of the slot whole: the roof items rise above the cabin, the front items
        // sit on the face and bumper, and the square wheel crop shows both side wheels inside the grid.
        crops =
            mapOf(
                Slot.ROOF to Rect(10f, 96f, 200f, 226f),
                Slot.FRONT to Rect(84f, 186f, 236f, 296f),
                Slot.WHEELS to Rect(6f, 190f, 116f, 300f),
                Slot.MODEL to Rect(0f, 96f, 240f, 294f),
            ),
    ) {
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val rock = pose.partDeg * 0.25f
        rotate(rock, RockPivot) { part(WheelWells) }
        part(Wheels)
        accessories.draw(this, outfit, ArtPart.WHEELS, ItemLayer.FRONT)
        rotate(rock, RockPivot) {
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
            part(Body)
            drawEyes(pose.blink)
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
        }
    }

    private fun DrawScope.drawEyes(blink: Float) {
        if (blink >= LIDS_CLOSED) return
        if (blink <= 0f) {
            part(LeftEye)
            part(RightEye)
            return
        }
        // The pupils and their reflections stay still as the painted lids cover the lenses.
        val opening = 16f * (1f - blink / LIDS_CLOSED)
        val apertures = Path()
        apertures.lidOpening(105f, 230f, 146f, 230f, opening)
        apertures.lidOpening(195f, 229.5f, 228f, 222.5f, opening)
        clipPath(apertures) {
            part(LeftEye)
            part(RightEye)
        }
    }
}

private fun Path.lidOpening(
    left: Float,
    leftY: Float,
    right: Float,
    rightY: Float,
    opening: Float,
) {
    val middle = (left + right) / 2f
    val curveY = (leftY + rightY) / 2f + 5f
    moveTo(left, leftY - opening)
    quadraticTo(middle, curveY - opening, right, rightY - opening)
    lineTo(right, rightY + opening)
    quadraticTo(middle, curveY + opening, left, leftY + opening)
    close()
}

private val Body = BlenderBodyRegion(IntOffset(0, 0), IntOffset(3, 150), IntSize(237, 129))
private val WheelWells = BlenderBodyRegion(IntOffset(280, 671), IntOffset(5, 196), IntSize(98, 75))
private val Wheels = BlenderBodyRegion(IntOffset(0, 391), IntOffset(2, 191), IntSize(224, 92))
private val LeftEye = BlenderBodyRegion(IntOffset(0, 671), IntOffset(100, 215), IntSize(47, 30))
private val RightEye = BlenderBodyRegion(IntOffset(145, 671), IntOffset(191, 210), IntSize(38, 32))
private val RockPivot = Offset(120f, 276f)

// Keep the lids together for 36 ms of a blink so the closed pose is visible at 30 fps.
private const val LIDS_CLOSED = 0.8f

private val Ground = ovalRect(114f, 283f, 104f, 8f)
private val FullFrame = Rect(-24f, 76f, 248f, 306f)
