// Atlas coordinates come from tools/blender/render_robot_layers.py.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.doll.rememberBlenderDollRig

@Composable
internal fun rememberCompanionRig(
    kind: CompanionKind,
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig? =
    when (kind) {
        CompanionKind.HUMAN -> rememberBlenderDollRig(loadAccessories)
        CompanionKind.ROBOT -> rememberBlenderRobotRig(color, loadAccessories)
        CompanionKind.CAT -> rememberBlenderCatRig(color, loadAccessories)
        CompanionKind.DOG -> rememberBlenderDogRig(color, loadAccessories)
        CompanionKind.DRAGON -> rememberBlenderDragonRig(color, loadAccessories)
        CompanionKind.DINOSAUR -> rememberBlenderDinosaurRig(color, loadAccessories)
        CompanionKind.BUNNY -> rememberBlenderBunnyRig(color, loadAccessories)
        CompanionKind.UNICORN -> rememberBlenderUnicornRig(color, loadAccessories)
        CompanionKind.CAR -> rememberBlenderCarRig(color, loadAccessories)
        CompanionKind.NONE -> null
    }

/** Loads one atlas per color outside the animation loop; all parts share the same bitmap. */
@Composable
internal fun rememberBlenderRobotRig(
    color: CompanionColor,
    loadAccessories: Boolean = true,
): CompanionRig {
    val resource =
        when (color) {
            CompanionColor.BLUE -> R.drawable.robot_blender_blue
            CompanionColor.GREEN -> R.drawable.robot_blender_green
            CompanionColor.PURPLE -> R.drawable.robot_blender_purple
            CompanionColor.ORIGINAL, CompanionColor.ORANGE -> R.drawable.robot_blender_original
        }
    val atlas = rememberBlenderAtlas(resource)
    val accessories = if (loadAccessories) rememberBlenderAtlas(R.drawable.robot_accessories_blender) else null
    return remember(atlas, accessories) {
        BlenderRobotRig(atlas, BlenderAccessories(accessories, RobotAccessoryRegions))
    }
}

/**
 * The Blender model rendered from the front, animated with the existing Compose pose and wardrobe.
 * The grid and crops preserve wardrobe framing. No 3D engine runs on the device.
 */
internal class BlenderRobotRig(
    atlas: ImageBitmap,
    accessories: BlenderAccessories,
) : AnimalRig(
        atlas = atlas,
        accessories = accessories,
        fullFrame = Rect(-20f, -40f, 260f, 306f),
        ground = Ground,
        // The crops keep every item of the slot whole; the neck crop starts below the face screen and leaves
        // out the lower arms and hands.
        headCrop = Rect(24f, 0f, 216f, 172f),
        neckCrop = Rect(60f, 70f, 180f, 190f),
        // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
        itemCrops =
            mapOf(
                "robot-extra-wrench" to Rect(120f, 143f, 240f, 263f),
                "robot-extra-heart-eyes" to Rect(60f, 40f, 180f, 160f),
                "robot-extra-star-eyes" to Rect(60f, 40f, 180f, 160f),
                "robot-extra-pet" to Rect(120f, 180f, 240f, 300f),
                "robot-extra-jetpack" to Rect(4f, 68f, 236f, 300f),
                "robot-extra-bubble-pack" to Rect(0f, 66f, 160f, 226f),
                "robot-extra-wind-up-key" to Rect(120f, 140f, 240f, 260f),
            ),
    ) {
    override fun DrawScope.drawBody(
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.BEHIND)
        part(Legs)
        rotate(pose.armDeg, LeftShoulder) { part(LeftArm) }
        accessories.draw(this, outfit, ArtPart.HAND, ItemLayer.FRONT)
        part(RightArm)
        // The torso occludes the inner shoulder joints in the original 3D model.
        part(Body)
        accessories.draw(this, outfit, ArtPart.BODY, ItemLayer.FRONT)
        rotate(pose.headTiltDeg, Neck) {
            rotate(pose.partDeg, AntennaBase) {
                accessories.draw(this, outfit, ArtPart.ANTENNA, ItemLayer.BEHIND)
            }
            part(Head)
            val screenItem =
                RobotAccessoryRegions[outfit[Slot.EXTRA]].orEmpty().any {
                    it.layer == ItemLayer.SCREEN
                }
            if (screenItem) {
                // Keep alternate eyes on the same screen and subject to the same blink.
                scale(1f, 1f - pose.blink * 0.92f, EyeCenter) {
                    accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.SCREEN)
                }
            } else {
                scale(1f, 1f - pose.blink * 0.92f, EyeCenter) { part(Eyes) }
            }
            accessories.draw(this, outfit, ArtPart.HEAD, ItemLayer.FRONT)
        }
    }
}

private val Head = BlenderBodyRegion(IntOffset(0, 0), IntOffset(30, 42), IntSize(180, 106))
private val Body = BlenderBodyRegion(IntOffset(544, 0), IntOffset(68, 136), IntSize(104, 96))
private val Legs = BlenderBodyRegion(IntOffset(244, 322), IntOffset(70, 202), IntSize(100, 82))
private val LeftArm = BlenderBodyRegion(IntOffset(548, 322), IntOffset(32, 144), IntSize(56, 89))
private val RightArm = BlenderBodyRegion(IntOffset(720, 322), IntOffset(152, 144), IntSize(56, 89))
private val Eyes = BlenderBodyRegion(IntOffset(0, 322), IntOffset(80, 75), IntSize(80, 35))
private val Neck = Offset(120f, 143f)
private val LeftShoulder = Offset(74f, 162f)
private val AntennaBase = Offset(120f, 46f)
private val EyeCenter = Offset(120f, 91f)
private val Ground = ovalRect(120f, 283f, 76f, 9f)
