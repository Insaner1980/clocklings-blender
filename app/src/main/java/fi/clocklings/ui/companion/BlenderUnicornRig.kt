// Atlas coordinates come from tools/blender/render_unicorn_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ovalRect

@Composable
internal fun rememberBlenderUnicornRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.unicorn_blender_blue
            CompanionColor.GREEN -> R.drawable.unicorn_blender_green
            CompanionColor.PURPLE -> R.drawable.unicorn_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.unicorn_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.unicorn_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderUnicornRig(atlas, BlenderAccessories(accessories, UnicornAccessoryRegions))
    }
}

/** Keeps the unicorn's wardrobe anchors, head tilt, blinking and independent tail motion. */
internal class BlenderUnicornRig(
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        fullFrame = FullFrame,
        ground = Ground,
        // The crops keep every item of the slot whole: the head crop takes in the horn tip and the mane,
        // the neck crop the pendants and the cape clasp.
        headCrop = Rect(12f, 0f, 196f, 172f),
        neckCrop = Rect(34f, 90f, 191f, 247f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "unicorn-extra-cloud" to Rect(46f, 142f, 204f, 300f),
                "unicorn-extra-balloon" to Rect(0f, 157f, 120f, 277f),
                "unicorn-extra-boots" to Rect(56f, 163f, 193f, 300f),
                "unicorn-extra-rainbow" to Rect(3f, 66f, 237f, 300f),
                "unicorn-extra-wings" to Rect(102f, 87f, 222f, 207f),
            ),
    ) {
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        drawTailedBody(outfit, pose.partDeg, TailPivot, Tail, Body)
        rotate(pose.headTiltDeg, Neck) {
            part(Head)
            drawBlinkingEyes(Eyes, EyeCenter, pose.blink)
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
    }
}

private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(22, 0), IntSize(157, 191))
private val Body = BlenderBodyRegion(IntOffset(475, 0), IntOffset(63, 123), IntSize(122, 162))
private val Tail = BlenderBodyRegion(IntOffset(0, 577), IntOffset(151, 138), IntSize(70, 95))
private val Eyes = BlenderBodyRegion(IntOffset(214, 577), IntOffset(40, 99), IntSize(79, 37))
private val TailPivot = Offset(166f, 172f)
private val Neck = Offset(100f, 162f)
private val EyeCenter = Offset(79f, 117f)
private val Ground = ovalRect(125f, 280f, 64f, 8f)
private val FullFrame = Rect(0f, -20f, 240f, 300f)
