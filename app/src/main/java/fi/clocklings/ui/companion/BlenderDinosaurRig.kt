// Atlas coordinates come from tools/blender/render_dinosaur_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ovalRect

@Composable
internal fun rememberBlenderDinosaurRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.dinosaur_blender_blue
            CompanionColor.PURPLE -> R.drawable.dinosaur_blender_purple
            CompanionColor.ORANGE -> R.drawable.dinosaur_blender_orange
            CompanionColor.ORIGINAL, CompanionColor.GREEN -> R.drawable.dinosaur_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.dinosaur_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderDinosaurRig(atlas, BlenderAccessories(accessories, DinosaurAccessoryRegions))
    }
}

/** Keeps the dinosaur's wardrobe anchors, alternate grip and existing tail motion. */
internal class BlenderDinosaurRig(
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        fullFrame = FullFrame,
        ground = Ground,
        // The crops keep every item of the slot whole: the head crop holds the hats and the head spikes and ends
        // under the jaw; the neck crop starts below the mouth.
        headCrop = Rect(34f, 0f, 226f, 146f),
        neckCrop = Rect(66f, 138f, 192f, 230f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "dinosaur-extra-backpack" to Rect(12f, 129f, 135f, 252f),
                "dinosaur-extra-bucket" to Rect(120f, 177f, 240f, 297f),
                "dinosaur-extra-balloon" to Rect(24f, 0f, 240f, 216f),
                "dinosaur-extra-umbrella" to Rect(2f, 0f, 240f, 238f),
                "dinosaur-extra-kite" to Rect(0f, 0f, 218f, 218f),
                "dinosaur-extra-skateboard" to Rect(10f, 80f, 230f, 300f),
                "dinosaur-extra-swim-ring" to Rect(50f, 136f, 212f, 298f),
                "dinosaur-extra-ice-cream" to Rect(110f, 100f, 230f, 220f),
            ),
    ) {
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val iceCream = outfit[Slot.EXTRA] == "dinosaur-extra-ice-cream"
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
        rotate(pose.partDeg, TailPivot) { part(Tail) }
        part(Body)
        part(NearArm)
        part(FarArm)
        if (iceCream) part(Fist)
        accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
        rotate(pose.headTiltDeg, Neck) {
            part(Head)
            scale(1f, 1f - pose.blink * 0.94f, NearEyeCenter) { part(NearEye) }
            scale(1f, 1f - pose.blink * 0.94f, FarEyeCenter) { part(FarEye) }
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
        if (iceCream) {
            // Keep the held scoops in front of the muzzle during head tilt.
            clipRect(150f, 100f, 240f, 181f) {
                accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
            }
        }
    }
}

private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(60, 30), IntSize(168, 126))
private val Body = BlenderBodyRegion(IntOffset(508, 0), IntOffset(38, 10), IntSize(156, 276))
private val Tail = BlenderBodyRegion(IntOffset(980, 0), IntOffset(4, 208), IntSize(100, 72))
private val NearArm = BlenderBodyRegion(IntOffset(0, 384), IntOffset(94, 153), IntSize(55, 58))
private val FarArm = BlenderBodyRegion(IntOffset(169, 384), IntOffset(158, 161), IntSize(34, 46))
private val Fist = BlenderBodyRegion(IntOffset(275, 384), IntOffset(180, 181), IntSize(33, 33))
private val NearEye = BlenderBodyRegion(IntOffset(0, 562), IntOffset(103, 75), IntSize(35, 38))
private val FarEye = BlenderBodyRegion(IntOffset(109, 562), IntOffset(151, 62), IntSize(27, 35))
private val Neck = Offset(130f, 148f)
private val TailPivot = Offset(82f, 250f)
private val NearEyeCenter = Offset(120f, 93f)
private val FarEyeCenter = Offset(164.6f, 78.5f)
private val Ground = ovalRect(116f, 284f, 100f, 11f)

// Leave room for tall accessories beneath the wardrobe's star badge.
private val FullFrame = Rect(-12f, -80f, 252f, 310f)
