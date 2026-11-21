// Atlas coordinates match the wardrobe export in art/blender/dinosaur.blend.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
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

/** Poses the dinosaur and its fitted wardrobe in the Blender camera's design grid. */
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
        neckCrop = Rect(58f, 55f, 202f, 199f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "dinosaur-extra-backpack" to Rect(12f, 129f, 135f, 252f),
                "dinosaur-extra-bucket" to Rect(120f, 177f, 240f, 297f),
                "dinosaur-extra-balloon" to Rect(24f, 0f, 240f, 216f),
                UMBRELLA_ITEM to Rect(62f, -76f, 374f, 236f),
                "dinosaur-extra-kite" to Rect(0f, 0f, 218f, 218f),
                "dinosaur-extra-skateboard" to Rect(10f, 80f, 230f, 300f),
                "dinosaur-extra-swim-ring" to Rect(50f, 136f, 212f, 298f),
                "dinosaur-extra-ice-cream" to Rect(110f, 100f, 230f, 220f),
            ),
    ) {
    private val tailBitmap by lazy(LazyThreadSafetyMode.NONE) {
        Bitmap.createBitmap(
            atlas.asAndroidBitmap(),
            Tail.source.x,
            Tail.source.y,
            Tail.sourceSize.width,
            Tail.sourceSize.height,
        )
    }
    private val tailVertices = FloatArray((TAIL_COLUMNS + 1) * 4)

    override fun full(outfit: Outfit): Rect = if (outfit[Slot.EXTRA] == UMBRELLA_ITEM) UmbrellaFrame else FullFrame

    override fun groundFor(outfit: Outfit): Rect =
        if (outfit[Slot.EXTRA] == "dinosaur-extra-skateboard") WheelGround else Ground

    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val extra = outfit[Slot.EXTRA]
        val holdingUmbrella = extra == UMBRELLA_ITEM
        val underJaw = outfit[Slot.NECK] != null || extra == "dinosaur-extra-backpack"
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
        drawTail(pose.partDeg)
        if (holdingUmbrella) {
            // The resting arm leaves a shadow in the body painting. The posed
            // tile restores that flank in front of the far shoulder.
            clipRect(158f, 155f, 194f, 203f, ClipOp.Difference) { part(Body) }
            accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.BEHIND)
            part(UmbrellaArm)
        } else {
            part(Body)
        }
        if (underJaw) {
            rotate(pose.headTiltDeg, Neck) {
                clipPath(PaintedNeck) { part(Head) }
            }
        }
        accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
        part(NearArm)
        if (!holdingUmbrella) part(FarArm)
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
        rotate(pose.headTiltDeg, Neck) {
            part(HeadPlates)
            if (underJaw) {
                clipPath(PaintedNeck, ClipOp.Difference) { part(Head) }
            } else {
                part(Head)
            }
            eye(NearEye, pose.blink, Offset(109.5f, 91.3f), Offset(118.9f, 99.1f), Offset(131.25f, 94.1f))
            eye(FarEye, pose.blink, Offset(157.8f, 77.8f), Offset(163.3f, 84.3f), Offset(171.25f, 79.15f))
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
        if (extra == "dinosaur-extra-kite") {
            accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.BEHIND)
        }
    }

    private fun DrawScope.drawTail(swing: Float) {
        if (swing == 0f) {
            part(Tail)
            return
        }
        val bottomRow = (TAIL_COLUMNS + 1) * 2
        for (column in 0..TAIL_COLUMNS) {
            val x = Tail.destination.x + Tail.size.width * column.toFloat() / TAIL_COLUMNS
            // Keep the hip and its plate in place. Move each cross-section intact, with more flex near the tip.
            val freeLength = ((64f - x) / 60f).coerceAtLeast(0f)
            val y = Tail.destination.y - swing * .5f * freeLength * freeLength
            val index = column * 2
            tailVertices[index] = x
            tailVertices[index + 1] = y
            tailVertices[bottomRow + index] = x
            tailVertices[bottomRow + index + 1] = y + Tail.size.height
        }
        drawIntoCanvas {
            it.nativeCanvas.drawBitmapMesh(tailBitmap, TAIL_COLUMNS, 1, tailVertices, 0, null, 0, tailPaint)
        }
    }

    private fun DrawScope.eye(
        region: BlenderBodyRegion,
        blink: Float,
        left: Offset,
        curve: Offset,
        right: Offset,
    ) {
        if (blink >= LIDS_CLOSED) return
        if (blink <= 0f) {
            part(region)
            return
        }
        // Reveal the painted eyelid as it closes over the stationary pupil and reflection.
        val open = 1f - blink / LIDS_CLOSED
        val height = region.size.height * open
        val margin = region.size.width * .19f * open
        val aperture =
            Path().apply {
                moveTo(left.x - margin, left.y)
                quadraticTo(curve.x, curve.y - 1.4f * height, right.x + margin, right.y)
                quadraticTo(curve.x, curve.y + height, left.x - margin, left.y)
                close()
            }
        clipPath(aperture) { part(region) }
    }
}

private const val LIDS_CLOSED = .8f
private const val TAIL_COLUMNS = 10
private const val UMBRELLA_ITEM = "dinosaur-extra-umbrella"
private val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(60, 30), IntSize(168, 126))
private val Body = BlenderBodyRegion(IntOffset(508, 0), IntOffset(38, 10), IntSize(156, 276))
private val Tail = BlenderBodyRegion(IntOffset(980, 0), IntOffset(4, 200), IntSize(100, 80))
private val HeadPlates = BlenderBodyRegion(IntOffset(980, 246), IntOffset(52, 14), IntSize(70, 111))
private val NearArm = BlenderBodyRegion(IntOffset(0, 384), IntOffset(94, 153), IntSize(55, 58))
private val FarArm = BlenderBodyRegion(IntOffset(169, 384), IntOffset(158, 161), IntSize(34, 46))
private val UmbrellaArm = BlenderBodyRegion(IntOffset(275, 384), IntOffset(149, 146), IntSize(72, 65))
private val NearEye = BlenderBodyRegion(IntOffset(0, 562), IntOffset(103, 75), IntSize(35, 38))
private val FarEye = BlenderBodyRegion(IntOffset(109, 562), IntOffset(151, 62), IntSize(27, 35))
private val Neck = Offset(130f, 148f)

// The head painting continues below the chin. Put that patch of neck behind the straps.
private val PaintedNeck =
    Path().apply {
        moveTo(100f, 129f)
        cubicTo(109f, 137f, 118f, 140.6f, 130f, 141.8f)
        cubicTo(141f, 142.9f, 151f, 142.6f, 158f, 141.1f)
        lineTo(165f, 140f)
        lineTo(165f, 155f)
        lineTo(100f, 155f)
        close()
    }

private val Ground = ovalRect(116f, 284f, 100f, 11f)
private val WheelGround = ovalRect(121f, 304f, 88f, 6f)

// Leave room for tall accessories beneath the wardrobe's star badge.
private val FullFrame = Rect(-12f, -80f, 252f, 310f)
private val UmbrellaFrame = Rect(-12f, -80f, 376f, 310f)
