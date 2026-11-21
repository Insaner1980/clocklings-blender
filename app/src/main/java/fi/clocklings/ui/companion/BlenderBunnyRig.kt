// Atlas regions are stored with the editable bunny.blend model.
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
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
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        fullFrame = FullFrame,
        ground = Ground,
        headCrop = Rect(20f, 0f, 220f, 176f),
        neckCrop = Rect(56f, 90f, 184f, 218f),
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
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val tuck = (pose.lift / CompanionMotion.HOP_HEIGHT).coerceIn(0f, 1f)
        scale(1f - .025f * tuck, 1f + .035f * tuck, AnimalFeet) {
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
            part(Body)
            hindFeet(tuck)
            translate(top = -5.5f * tuck) {
                accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
                part(Arms)
            }
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg - 2f * tuck, Neck) { head(outfit, pose) }
        }
    }

    private fun DrawScope.hindFeet(tuck: Float) {
        if (tuck == 0f) {
            part(Feet)
            return
        }
        translate(4f * tuck, -5f * tuck) {
            rotate(13f * tuck, LeftAnkle) { clipRect(right = 120f) { part(Feet) } }
        }
        translate(-4f * tuck, -5f * tuck) {
            rotate(-13f * tuck, RightAnkle) { clipRect(left = 120f) { part(Feet) } }
        }
    }

    private fun DrawScope.head(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val swing = -pose.partDeg * .37f
        rotate(swing, LeftEarPivot) { part(LeftEar) }
        rotate(-swing, RightEarPivot) { part(RightEar) }
        part(Head)
        eyes(pose.blink)
        if (outfit[Slot.HEAD] == "bunny-head-ear-bow") {
            rotate(swing, LeftEarPivot) { accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT) }
        } else {
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
    }

    private fun DrawScope.eyes(blink: Float) {
        if (blink <= 0f) {
            part(Eyes)
            return
        }
        part(Lids)
        val open = (1f - blink / .91f).coerceAtLeast(0f)
        if (open > 0f) {
            val apertures = Path()
            for (center in EyeCenters) {
                val top = 114f - 23f * open
                val bottom = 114f + 17f * open
                apertures.moveTo(center - 18f, top + 5f)
                apertures.quadraticTo(center, top - 4f, center + 18f, top + 5f)
                apertures.lineTo(center + 18f, bottom + 5f)
                apertures.quadraticTo(center, bottom - 4f, center - 18f, bottom + 5f)
                apertures.close()
            }
            clipPath(apertures) { part(Eyes) }
        }
    }
}

private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(48, 28), IntSize(146, 150))
private val Body = BlenderBodyRegion(IntOffset(442, 0), IntOffset(46, 154), IntSize(161, 133))
private val LeftEar = BlenderBodyRegion(IntOffset(0, 454), IntOffset(0, 42), IntSize(112, 190))
private val RightEar = BlenderBodyRegion(IntOffset(340, 454), IntOffset(133, 42), IntSize(107, 172))
private val Arms = BlenderBodyRegion(IntOffset(665, 454), IntOffset(68, 164), IntSize(107, 89))
private val Eyes = BlenderBodyRegion(IntOffset(665, 725), IntOffset(67, 91), IntSize(108, 44))
private val Feet = BlenderBodyRegion(IntOffset(0, 1032), IntOffset(46, 225), IntSize(144, 59))
private val Lids = BlenderBodyRegion(IntOffset(665, 866), IntOffset(70, 108), IntSize(102, 15))
private val LeftAnkle = Offset(87f, 239f)
private val RightAnkle = Offset(154f, 240f)
private val LeftEarPivot = Offset(79f, 56f)
private val RightEarPivot = Offset(154f, 56f)
private val Neck = Offset(120f, 172f)
private val EyeCenters = floatArrayOf(88f, 153f)
private val Ground = ovalRect(120f, 284f, 96f, 11f)
private val FullFrame = Rect(-20f, -80f, 260f, 300f)
