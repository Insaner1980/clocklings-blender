package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.doll.DOLL_GRID_HEIGHT
import fi.clocklings.ui.doll.DOLL_GRID_WIDTH

/** An accessory is behind its part, in front of it, or replaces the robot's screen eyes. */
internal enum class ItemLayer { BEHIND, FRONT, SCREEN }

/** The companion part whose transform a Blender accessory follows. */
internal enum class ArtPart { BODY, HEAD, WINGS, HAND, ANTENNA, WHEELS }

/**
 * Draws one companion in its own [width] x [height] design grid. [CompanionView] fits the grid (or a
 * [crop]) into the canvas and supplies the pose; the rig applies its own part transforms and calls
 * its Blender accessories at each part and layer inside that part's transform.
 */
internal interface CompanionRig {
    val width: Float
    val height: Float

    /** The design-grid area that shows the [slot] in a wardrobe thumbnail. */
    fun crop(slot: Slot): Rect

    /** The design-grid area that shows [item] in a wardrobe thumbnail: its own crop, else its slot's. */
    fun crop(item: Item): Rect = crop(item.slot)

    /** The whole design grid. */
    fun full(): Rect

    fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    )
}

/**
 * The frame shared by the animal companions and the car: the 240 x 300 design grid, the wardrobe
 * thumbnail [crops] by slot, the [itemCrops] that zoom an item's thumbnail further by item id, and the
 * whole grid for every other slot.
 */
internal abstract class AnimalRig(
    private val crops: Map<Slot, Rect>,
    private val itemCrops: Map<String, Rect> = emptyMap(),
) : CompanionRig {
    constructor(headCrop: Rect, neckCrop: Rect, itemCrops: Map<String, Rect> = emptyMap()) :
        this(mapOf(Slot.HEAD to headCrop, Slot.NECK to neckCrop), itemCrops)

    final override val width = ANIMAL_GRID_WIDTH
    final override val height = ANIMAL_GRID_HEIGHT

    private val grid = Rect(0f, 0f, width, height)

    final override fun crop(slot: Slot): Rect = crops[slot] ?: grid

    override fun crop(item: Item): Rect = itemCrops[item.id] ?: crop(item.slot)
}

private const val ANIMAL_GRID_WIDTH = 240f
private const val ANIMAL_GRID_HEIGHT = 300f

/** Where an animal stands in its grid: the sway and the breathing turn around this point. */
internal val AnimalFeet = Offset(ANIMAL_GRID_WIDTH / 2, 280f)

/** The animal's vertical axis passes through this point; the spin turns around it. */
internal val AnimalMiddle = Offset(ANIMAL_GRID_WIDTH / 2, 150f)

/**
 * Draws [block] in the animal body pose: the hop lifts it, it sways around [feet], spins around
 * [AnimalMiddle] and breathes as a vertical squash around [feet].
 */
internal inline fun DrawScope.posedBody(
    pose: CompanionPose,
    feet: Offset = AnimalFeet,
    block: DrawScope.() -> Unit,
) = withTransform({
    translate(0f, -pose.lift)
    rotate(pose.swayDeg, feet)
    scale(pose.turn, 1f, AnimalMiddle)
    scale(1f, 1f + pose.breathe * CompanionMotion.BREATHE_SQUASH, feet)
}, block)

/** Width to height of the design grid, without loading artwork to size a companion view. */
internal fun companionAspectRatio(kind: CompanionKind): Float =
    when (kind) {
        CompanionKind.NONE -> 1f
        CompanionKind.HUMAN -> DOLL_GRID_WIDTH / DOLL_GRID_HEIGHT
        else -> ANIMAL_GRID_WIDTH / ANIMAL_GRID_HEIGHT
    }
