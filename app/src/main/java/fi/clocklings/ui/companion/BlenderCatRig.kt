// Atlas coordinates come from tools/blender/render_cat_layers.py.
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ovalRect

@Composable
internal fun rememberBlenderCatRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.cat_blender_blue
            CompanionColor.GREEN -> R.drawable.cat_blender_green
            CompanionColor.PURPLE -> R.drawable.cat_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.cat_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.cat_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderCatRig(atlas, BlenderAccessories(accessories, CatAccessoryRegions))
    }
}

/** Reuses the cat's wardrobe anchors and crops, with the existing seated-cat motion. */
internal class BlenderCatRig(
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        // Keep tall headwear below the wardrobe's star badge, including at 200% font scale.
        fullFrame = FullFrame,
        ground = Ground,
        // Frame the accessory and the eyes together in the neckwear preview.
        headCrop = Rect(42f, 8f, 198f, 164f),
        neckCrop = Rect(55f, 83f, 185f, 213f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "cat-extra-yarn" to Rect(20f, 166f, 150f, 296f),
                "cat-extra-glasses" to Rect(36f, 28f, 204f, 196f),
                "cat-extra-fish-backpack" to Rect(0f, 126f, 138f, 264f),
                "cat-extra-wand" to Rect(0f, 143f, 139f, 282f),
                "cat-extra-wings" to Rect(0f, 40f, 240f, 280f),
            ),
    ) {
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        drawBodyWithAccessories(outfit) {
            rotate(-pose.partDeg, TailPivot) { part(Tail) }
            part(Body)
            val tuck = (pose.lift / CompanionMotion.HOP_HEIGHT).coerceIn(0f, 1f)
            scale(1f, 1f - .065f * tuck, Shoulder) {
                if (outfit[Slot.EXTRA] == "cat-extra-fish-backpack") {
                    part(Paws)
                    clipPath(ChestBetweenForelegs) {
                        accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
                    }
                } else {
                    accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
                    part(Paws)
                }
            }
        }
        rotate(pose.headTiltDeg, Neck) { head(outfit, pose.blink) }
    }

    private fun DrawScope.head(
        outfit: Outfit,
        blink: Float,
    ) {
        part(Ears)
        part(Head)
        part(Stripes)
        eyes(blink)
        accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.eyes(blink: Float) {
        if (blink <= 0f) {
            part(Eyes)
            return
        }
        part(Lids)
        if (blink >= LIDS_CLOSED) return
        val opening = 1f - blink / LIDS_CLOSED
        val top = 108f - 27f * opening
        val bottom = 108f + 21f * opening
        val apertures = Path()
        for (cx in EyeCenters) {
            apertures.moveTo(cx - 19f, top)
            apertures.quadraticTo(cx, top + 12f, cx + 19f, top)
            apertures.lineTo(cx + 19f, bottom)
            apertures.quadraticTo(cx, bottom + 12f, cx - 19f, bottom)
            apertures.close()
        }
        clipPath(apertures) { part(Eyes) }
    }
}

// Hold the painted lids together for 36 ms of the 180 ms blink.
private const val LIDS_CLOSED = .8f
private val EyeCenters = floatArrayOf(90f, 151.5f)
private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(30, 48), IntSize(180, 122))
private val Body = BlenderBodyRegion(IntOffset(544, 0), IntOffset(40, 146), IntSize(160, 140))
private val Tail = BlenderBodyRegion(IntOffset(1028, 0), IntOffset(170, 148), IntSize(65, 130))
private val Paws = BlenderBodyRegion(IntOffset(0, 424), IntOffset(72, 170), IntSize(98, 116))
private val Eyes = BlenderBodyRegion(IntOffset(300, 424), IntOffset(70, 88), IntSize(102, 44))
private val Lids = BlenderBodyRegion(IntOffset(615, 665), IntOffset(73, 105), IntSize(96, 12))
private val Ears = BlenderBodyRegion(IntOffset(610, 424), IntOffset(40, 18), IntSize(162, 73))
private val Stripes = BlenderBodyRegion(IntOffset(300, 560), IntOffset(94, 50), IntSize(52, 34))
private val TailPivot = Offset(190f, 247f)
private val Neck = Offset(120f, 160f)
private val Shoulder = Offset(120f, 178f)

// The paw sprite also contains chest fur. Trace the visible inside edges of both forelegs.
private val ChestBetweenForelegs =
    Path().apply {
        moveTo(63f, 147f)
        lineTo(179f, 147f)
        cubicTo(164f, 163f, 148f, 173f, 145.5f, 185f)
        cubicTo(144.4f, 191f, 145f, 201f, 142f, 207f)
        lineTo(100f, 207f)
        cubicTo(98.5f, 201f, 99f, 192f, 100.6f, 185f)
        cubicTo(103f, 176f, 95f, 169f, 82f, 165f)
        close()
    }
private val Ground = ovalRect(120f, 284f, 86f, 11f)
private val FullFrame = Rect(-12f, -48f, 252f, 310f)
