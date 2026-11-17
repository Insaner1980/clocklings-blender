package fi.clocklings.ui.companion

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.rememberReduceMotion
import kotlin.math.min

/**
 * Draws any companion through its [CompanionRig]; nothing for [CompanionKind.NONE]. When [animated],
 * the companion idles and runs [celebration] once: a spin that reveals the new outfit with a sparkle,
 * then hops. With reduced motion it stays still and the new outfit shows at once. The pose is read
 * only while drawing, so motion redraws without recomposition. [cropItem] fits that item's thumbnail
 * area of the rig to the canvas instead of the whole companion.
 */
@Composable
fun CompanionView(
    kind: CompanionKind,
    outfit: Outfit,
    modifier: Modifier = Modifier,
    color: CompanionColor = LocalPersonalization.current.styleFor(kind).color,
    celebration: CompanionCelebration? = null,
    animated: Boolean = true,
    cropItem: Item? = null,
    onGroundPosition: ((Offset) -> Unit)? = null,
) {
    val loadAccessories = needsBlenderAccessories(kind, outfit, celebration?.newOutfit)
    val rig = rememberCompanionRig(kind, color, loadAccessories) ?: return
    val reduceMotion = rememberReduceMotion()
    val moving = animated && !reduceMotion
    val active = celebration?.takeIf { animated && (it.spin || it.jump) }
    // Keyed so that a new companion or celebration never sees the previous frame clock for one frame.
    val nowMs = remember(kind, active) { mutableLongStateOf(0L) }
    LaunchedEffect(kind, moving, active) {
        nowMs.longValue = 0L
        if (!moving) return@LaunchedEffect
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) withInfiniteAnimationFrameNanos { nowMs.longValue = (it - start) / NANOS_PER_MS }
    }
    val showNew by remember(kind, active, reduceMotion) {
        derivedStateOf { CompanionMotion.revealsNewOutfit(active, nowMs.longValue, reduceMotion) }
    }
    val confetti = LocalConfetti.current
    val center = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(showNew) {
        if (showNew && !reduceMotion) confetti?.sparkle(center.value)
    }
    val shown = active?.newOutfit?.takeIf { showNew } ?: outfit
    val crop = if (cropItem == null) rig.full() else rig.crop(cropItem)
    val art =
        modifier
            .then(if (cropItem == null) Modifier.testTag("companion_${kind.name}") else Modifier)
            .then(if (active != null) Modifier.confettiAnchor { center.value = it } else Modifier)
            .reportGroundPosition(crop, rig.groundCenter, onGroundPosition)
            .clearAndSetSemantics { }
    Canvas(art) {
        val pose = if (moving) poseAt(nowMs.longValue, active) else RestPose
        drawFitted(rig, crop, clip = cropItem != null, color, shown, pose)
    }
}

private fun Modifier.reportGroundPosition(
    crop: Rect,
    groundCenter: Offset,
    onGroundPosition: ((Offset) -> Unit)?,
): Modifier {
    if (onGroundPosition == null) return this
    return onGloballyPositioned { coordinates ->
        val width = coordinates.size.width.toFloat()
        val height = coordinates.size.height.toFloat()
        val scale = min(width / crop.width, height / crop.height)
        val inset = Offset((width - crop.width * scale) / 2, (height - crop.height * scale) / 2)
        val centre = inset + (groundCenter - crop.topLeft) * scale
        onGroundPosition(coordinates.positionInRoot() + centre)
    }
}

/**
 * Grows a thumbnail crop to the canvas proportions, so every thumbnail fills its window edge to edge
 * instead of showing a band of the companion with flat cut edges. A tall crop grows sideways around its
 * centre; a wide crop keeps its top and grows downwards, because the area above a neck crop is the
 * companion's mouth.
 */
internal fun Rect.grownTo(canvas: Size): Rect {
    val aspect = canvas.width / canvas.height
    return if (width / height < aspect) {
        val half = height * aspect / 2f
        Rect(center.x - half, top, center.x + half, bottom)
    } else {
        Rect(left, top, right, top + width / aspect)
    }
}

/**
 * Fits [crop] of [rig] to the canvas, centered (SVG xMidYMid meet), and draws the companion there,
 * clipped to the crop when [clip]. The whole companion is not clipped, so a waving arm or a hop is
 * never cut off.
 */
@Suppress("LongParameterList")
private fun DrawScope.drawFitted(
    rig: CompanionRig,
    crop: Rect,
    clip: Boolean,
    color: CompanionColor,
    outfit: Outfit,
    pose: CompanionPose,
) {
    val bounds = if (clip) crop.grownTo(size) else crop
    val s = min(size.width / bounds.width, size.height / bounds.height)
    translate((size.width - bounds.width * s) / 2f, (size.height - bounds.height * s) / 2f) {
        scale(s, s, pivot = Offset.Zero) {
            translate(-bounds.left, -bounds.top) {
                if (clip) {
                    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                        with(rig) { draw(color, outfit, pose) }
                    }
                } else {
                    with(rig) { draw(color, outfit, pose) }
                }
            }
        }
    }
}

/** Idle motion, or the celebration frame with the idle blink, breathing and head tilt kept. */
private fun poseAt(
    nowMs: Long,
    active: CompanionCelebration?,
): CompanionPose {
    val idle = CompanionMotion.idle(nowMs)
    val event = active?.takeIf { nowMs >= it.delayMs } ?: return idle
    val frame = CompanionMotion.celebration(nowMs - event.delayMs, event.spin, event.jump)
    return if (frame.done) {
        idle
    } else {
        frame.pose.copy(blink = idle.blink, breathe = idle.breathe, headTiltDeg = idle.headTiltDeg)
    }
}

private val RestPose = CompanionPose()

private const val NANOS_PER_MS = 1_000_000L
