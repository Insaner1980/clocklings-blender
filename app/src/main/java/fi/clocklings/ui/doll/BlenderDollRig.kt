// Atlas coordinates come from tools/blender/render_doll_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.doll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.companion.ArtPart
import fi.clocklings.ui.companion.BlenderAccessories
import fi.clocklings.ui.companion.CompanionMotion
import fi.clocklings.ui.companion.CompanionPose
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.companion.DollAccessoryRegions
import fi.clocklings.ui.companion.ItemLayer
import fi.clocklings.ui.companion.rememberBlenderAtlas

@Composable
internal fun rememberBlenderDollRig(loadAccessories: Boolean = true): CompanionRig {
    val atlas = rememberBlenderAtlas(R.drawable.doll_blender)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.doll_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderDollRig(atlas, BlenderAccessories(accessories, DollAccessoryRegions))
    }
}

/** Preserves the doll's 200 x 340 wardrobe anchors and independent left-arm wave. */
internal class BlenderDollRig(
    private val atlas: ImageBitmap,
    private val accessories: BlenderAccessories,
) : CompanionRig {
    override val width = DOLL_GRID_WIDTH
    override val height = DOLL_GRID_HEIGHT
    override val groundCenter = Ground.center

    override fun full(outfit: Outfit): Rect = Rect(-4f, -8f, 204f, 344f)

    override fun crop(slot: Slot): Rect =
        when (slot) {
            Slot.HAIR -> Rect(8f, 8f, 192f, 192f)

            Slot.HEAD -> Rect(22f, 0f, 178f, 156f)

            Slot.DRESS -> Rect(24f, 124f, 176f, 276f)

            // Square, so the thumbnail window does not grow it downwards past the doll's feet into empty space.
            Slot.SHOES -> Rect(56f, 250f, 56f + 88f, 250f + 88f)

            Slot.EXTRA -> Rect(0f, 100f, 200f, 268f)

            // The doll has no neck or car slots; these crops only keep the mapping total.
            Slot.NECK -> Rect(60f, 110f, 60f + 80f, 110f + 60f)

            Slot.ROOF, Slot.FRONT, Slot.WHEELS, Slot.MODEL -> Grid
        }

    override fun crop(item: Item): Rect {
        val fallback =
            when (item.id) {
                "extra-strawberry-bag" -> Rect(20f, 126f, 140f, 246f)
                "extra-siivet" -> Rect(0f, 84f, 200f, 284f)
                else -> crop(item.slot)
            }
        return accessories.crop(item.id, fallback)
    }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val ground = if (outfit[Slot.SHOES] == "shoes-roller-skates") SkateGround else Ground
        groundShadow(ground)
        posedDoll(pose, ground.center) { drawOutfit(outfit, pose) }
    }

    private fun DrawScope.drawOutfit(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val hair =
            when (outfit[Slot.HAIR]) {
                "hair-pitkat" -> GoldHair
                "hair-kiharat" -> CurlyHair
                "hair-sateenkaari" -> RainbowHair
                else -> BraidsHair
            }
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
        part(hair.back)
        part(Legs)
        drawShoes(outfit)
        rotate(pose.armDeg, ShoulderLeft) { part(ArmLeft) }
        part(ArmRight)
        part(Neck)
        part(
            when (outfit[Slot.DRESS]) {
                "dress-pilkku" -> DressDots
                "dress-tahti" -> DressStars
                "dress-sateenkaari" -> DressRainbow
                "dress-overalls" -> DressOveralls
                else -> DressYellow
            },
        )
        part(Head)
        eyes(pose.blink)
        part(hair.front)
        accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
    }

    private fun DrawScope.drawShoes(outfit: Outfit) {
        if (outfit[Slot.SHOES] == "shoes-roller-skates") {
            accessories.draw(this, outfit, ArtPart.WHEELS, ItemLayer.FRONT)
        } else {
            part(
                when (outfit[Slot.SHOES]) {
                    "shoes-punaiset" -> ShoesRed
                    "shoes-saappaat" -> ShoesBoots
                    else -> ShoesWhite
                },
            )
        }
    }

    private val eyeOpenings = Path()
    private val upperLashes = Path()

    private fun DrawScope.eyes(blink: Float) {
        if (blink <= 0f) {
            part(EyeLeft)
            part(EyeRight)
            return
        }
        val closure = (blink / 0.90f).coerceAtMost(1f)
        if (closure >= 1f) {
            part(ClosedEyes)
            return
        }
        val corner = 89f + 5f * closure
        val middle = 76f + 20.8f * closure
        val control = 2f * middle - corner
        val bottom = 110f - 13f * closure
        eyeOpenings.reset()
        upperLashes.reset()
        for (center in EyeCenters) {
            eyeOpenings.moveTo(center - 12f, corner)
            eyeOpenings.quadraticTo(center, control, center + 12f, corner)
            eyeOpenings.lineTo(center + 12f, bottom)
            eyeOpenings.quadraticTo(center, bottom + 3f * closure, center - 12f, bottom)
            eyeOpenings.close()
            upperLashes.moveTo(center - 12f, corner)
            upperLashes.quadraticTo(center, control, center + 12f, corner)
            upperLashes.quadraticTo(center, control + 2.6f, center - 12f, corner)
            upperLashes.close()
        }
        clipPath(eyeOpenings) {
            part(EyeLeft)
            part(EyeRight)
        }
        drawPath(upperLashes, LashColor, alpha = (closure * 3f).coerceAtMost(1f))
    }

    private fun DrawScope.part(region: DollRegion) {
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

private inline fun DrawScope.posedDoll(
    pose: CompanionPose,
    feet: Offset,
    crossinline block: DrawScope.() -> Unit,
) {
    translate(0f, -pose.lift) {
        rotate(pose.swayDeg, feet) {
            scale(pose.turn, 1f, Middle) {
                scale(1f, 1f + pose.breathe * CompanionMotion.BREATHE_SQUASH, feet) { block() }
            }
        }
    }
}

private class DollRegion(
    val source: IntOffset,
    val destination: IntOffset,
    val size: IntSize,
) {
    val sourceSize = IntSize(size.width * 3, size.height * 3)
}

private class DollHair(
    val back: DollRegion,
    val front: DollRegion,
)

private val HairGoldBack = DollRegion(IntOffset(0, 0), IntOffset(27, 18), IntSize(146, 198))
private val HairRainbowBack = DollRegion(IntOffset(442, 0), IntOffset(27, 18), IntSize(146, 198))
private val HairCurlyBack = DollRegion(IntOffset(884, 0), IntOffset(10, 0), IntSize(180, 196))
private val DressOveralls = DollRegion(IntOffset(1428, 0), IntOffset(40, 135), IntSize(120, 156))
private val HairBraidsBack = DollRegion(IntOffset(0, 598), IntOffset(10, 18), IntSize(180, 138))
private val DressYellow = DollRegion(IntOffset(544, 598), IntOffset(38, 132), IntSize(125, 127))
private val DressDots = DollRegion(IntOffset(923, 598), IntOffset(38, 132), IntSize(125, 127))
private val DressStars = DollRegion(IntOffset(1302, 598), IntOffset(38, 132), IntSize(125, 127))
private val DressRainbow = DollRegion(IntOffset(0, 1016), IntOffset(38, 132), IntSize(125, 127))
private val Head = DollRegion(IntOffset(379, 1016), IntOffset(47, 38), IntSize(106, 101))
private val HairBraidsFront = DollRegion(IntOffset(701, 1016), IntOffset(40, 18), IntSize(120, 90))
private val HairGoldFront = DollRegion(IntOffset(1065, 1016), IntOffset(40, 18), IntSize(120, 90))
private val HairCurlyFront = DollRegion(IntOffset(1429, 1016), IntOffset(37, 18), IntSize(126, 90))
private val HairRainbowFront = DollRegion(IntOffset(0, 1392), IntOffset(40, 18), IntSize(120, 90))
private val ArmLeft = DollRegion(IntOffset(364, 1392), IntOffset(20, 154), IntSize(51, 70))
private val ArmRight = DollRegion(IntOffset(521, 1392), IntOffset(129, 154), IntSize(51, 70))
private val ShoesBoots = DollRegion(IntOffset(678, 1392), IntOffset(54, 249), IntSize(90, 66))
private val Legs = DollRegion(IntOffset(952, 1392), IntOffset(64, 243), IntSize(69, 57))
private val ShoesWhite = DollRegion(IntOffset(1163, 1392), IntOffset(54, 280), IntSize(90, 35))
private val ShoesRed = DollRegion(IntOffset(1437, 1392), IntOffset(54, 280), IntSize(90, 35))
private val EyeLeft = DollRegion(IntOffset(1711, 1392), IntOffset(63, 77), IntSize(28, 32))
private val EyeRight = DollRegion(IntOffset(1799, 1392), IntOffset(109, 77), IntSize(28, 32))
private val ClosedEyes = DollRegion(IntOffset(1710, 1500), IntOffset(62, 89), IntSize(76, 13))
private val Neck = DollRegion(IntOffset(1887, 1392), IntOffset(87, 124), IntSize(26, 25))

private val BraidsHair = DollHair(HairBraidsBack, HairBraidsFront)
private val GoldHair = DollHair(HairGoldBack, HairGoldFront)
private val CurlyHair = DollHair(HairCurlyBack, HairCurlyFront)
private val RainbowHair = DollHair(HairRainbowBack, HairRainbowFront)
private val ShoulderLeft = Offset(62f, 166f)
private val Middle = Offset(100f, 170f)
private val EyeCenters = floatArrayOf(77f, 123f)
private val LashColor = Color(0xFF513444)
private val Ground = ovalRect(100f, 311f, 52f, 8f)
private val SkateGround = ovalRect(100f, 331f, 52f, 8f)

internal const val DOLL_GRID_WIDTH = 200f
internal const val DOLL_GRID_HEIGHT = 340f
private val Grid = Rect(0f, 0f, DOLL_GRID_WIDTH, DOLL_GRID_HEIGHT)
