// The editable unicorn and accessory scenes store these atlas regions in wardrobe_export.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
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
                "unicorn-extra-balloon" to Rect(0f, 112f, 120f, 232f),
                "unicorn-extra-boots" to Rect(56f, 163f, 193f, 300f),
                "unicorn-extra-rainbow" to Rect(3f, 66f, 237f, 300f),
                "unicorn-extra-wings" to Rect(102f, 87f, 222f, 207f),
            ),
    ) {
    private val legs by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            WalkingLeg(
                atlas,
                UnicornLeg.FAR_HIND,
                BlenderBodyRegion(IntOffset(453, 576), IntOffset(115, 209), IntSize(39, 67)),
                BlenderBodyRegion(IntOffset(101, 107), IntOffset(118, 238), IntSize(32, 36)),
            ),
            WalkingLeg(
                atlas,
                UnicornLeg.FAR_FORE,
                BlenderBodyRegion(IntOffset(215, 691), IntOffset(61, 209), IntSize(40, 67)),
                BlenderBodyRegion(IntOffset(2, 98), IntOffset(64, 240), IntSize(32, 38)),
            ),
            WalkingLeg(
                atlas,
                UnicornLeg.NEAR_HIND,
                BlenderBodyRegion(IntOffset(703, 492), IntOffset(140, 209), IntSize(47, 73)),
                BlenderBodyRegion(IntOffset(308, 110), IntOffset(150, 242), IntSize(36, 39)),
            ),
            WalkingLeg(
                atlas,
                UnicornLeg.NEAR_FORE,
                BlenderBodyRegion(IntOffset(572, 492), IntOffset(85, 209), IntSize(43, 76)),
                BlenderBodyRegion(IntOffset(200, 110), IntOffset(87, 246), IntSize(35, 39)),
            ),
        )
    }

    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val mobile = pose.walkTimeMs > 0 || pose.lift > 0f
        val stride = if (mobile) UnicornStride(pose.walkTimeMs, pose.lift / CompanionMotion.HOP_HEIGHT) else null
        val boots = if (outfit[Slot.EXTRA] == BOOT_ITEM) accessories.atlas else null
        translate(top = stride?.bodyY ?: 0f) {
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
            rotate(pose.partDeg, TailPivot) { part(Tail) }
            for (leg in legs) leg.draw(this, stride?.leg(leg.joints), boots)
            part(Body)
            accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT, excludingItem = BOOT_ITEM)
            drawHead(outfit, pose, stride?.nod ?: 0f)
        }
    }

    private fun DrawScope.drawHead(
        outfit: Outfit,
        pose: CompanionPose,
        nod: Float,
    ) {
        rotate(pose.headTiltDeg + nod, Neck) {
            part(Head)
            drawBlinkingEyes(Eyes, EyeCenter, pose.blink)
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
    }
}

private const val BOOT_ITEM = "unicorn-extra-boots"
private const val MESH_ROWS = 16
private const val MESH_COLUMNS = 2
private val legPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

private class WalkingLeg(
    private val atlas: ImageBitmap,
    val joints: UnicornLeg,
    private val region: BlenderBodyRegion,
    private val boot: BlenderBodyRegion,
) {
    private val shaft = IntSize(region.size.width, joints.sole.y.toInt() - 18 - region.destination.y)
    private val shaftSource = IntSize(shaft.width * 3, shaft.height * 3)
    private val legBitmap by lazy(LazyThreadSafetyMode.NONE) { crop(region.sourceSize) }
    private val shaftBitmap by lazy(LazyThreadSafetyMode.NONE) { crop(shaftSource) }

    private fun crop(size: IntSize): Bitmap =
        Bitmap.createBitmap(
            atlas.asAndroidBitmap(),
            region.source.x,
            region.source.y,
            size.width,
            size.height,
        )

    private val vertices = FloatArray((MESH_ROWS + 1) * (MESH_COLUMNS + 1) * 2)

    fun draw(
        scope: DrawScope,
        pose: UnicornLegPose?,
        footwear: ImageBitmap?,
    ) {
        val size = if (footwear == null) region.size else shaft
        if (pose == null) {
            scope.drawImage(
                atlas,
                region.source,
                if (footwear == null) region.sourceSize else shaftSource,
                region.destination,
                size,
                filterQuality = FilterQuality.Medium,
            )
        } else {
            drawMesh(scope, pose, size, if (footwear == null) legBitmap else shaftBitmap)
        }
        if (footwear != null) {
            val delta = if (pose == null) Offset.Zero else pose.sole - joints.sole
            scope.translate(delta.x, delta.y) {
                rotate(pose?.roll ?: 0f, joints.sole) {
                    drawImage(
                        footwear,
                        boot.source,
                        boot.sourceSize,
                        boot.destination,
                        boot.size,
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }
        }
    }

    private fun drawMesh(
        scope: DrawScope,
        pose: UnicornLegPose,
        size: IntSize,
        bitmap: Bitmap,
    ) {
        var index = 0
        for (row in 0..MESH_ROWS) {
            val y = region.destination.y + row * size.height.toFloat() / MESH_ROWS
            for (column in 0..MESH_COLUMNS) {
                val x = region.destination.x + column * region.size.width.toFloat() / MESH_COLUMNS
                val point = pose.skin(Offset(x, y))
                vertices[index++] = point.x
                vertices[index++] = point.y
            }
        }
        scope.drawIntoCanvas {
            it.nativeCanvas.drawBitmapMesh(bitmap, MESH_COLUMNS, MESH_ROWS, vertices, 0, null, 0, legPaint)
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
