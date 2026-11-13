package fi.clocklings.ui.intro

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.ui.companion.CompanionMotion
import fi.clocklings.ui.companion.CompanionPose

/** Animates the actual launcher artwork, keeping the logo identical on both surfaces. */
@Composable
internal fun LogoMascot(
    time: () -> Long,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val artwork = ImageVector.vectorResource(R.drawable.ic_launcher_foreground)
    val body = remember(artwork) { (artwork.root.first() as VectorGroup).first() as VectorGroup }
    val painter =
        rememberVectorPainter(
            MascotSize,
            MascotSize,
            viewportWidth = VIEWPORT_SIZE,
            viewportHeight = VIEWPORT_SIZE,
            autoMirror = false,
        ) { _, _ ->
            val pose = logoPose(time(), reducedMotion)
            Group(translationX = -CROP_LEFT, translationY = -CROP_TOP) {
                Group(
                    rotation = pose.swayDeg,
                    pivotX = body.pivotX,
                    pivotY = body.pivotY,
                    translationY = -pose.lift,
                ) {
                    body.forEach { node ->
                        val part = node as VectorGroup
                        Group(
                            rotation = if (part.name == "right_hand") pose.armDeg * ARM_SCALE else 0f,
                            scaleY = if (part.name == "eyes") 1f - pose.blink * BLINK_SCALE else 1f,
                            pivotX = part.pivotX,
                            pivotY = part.pivotY,
                        ) {
                            RenderVectorGroup(part)
                        }
                    }
                }
            }
        }
    Image(painter, contentDescription = null, modifier = modifier.testTag("logo_mascot"))
}

internal fun logoPose(
    time: Long,
    reducedMotion: Boolean,
): CompanionPose {
    if (reducedMotion) return CompanionPose()
    val idle = CompanionMotion.idle(time)
    val jumpTime = (time - IntroTimeline.MASCOT_IN_MS).coerceAtLeast(0L)
    val greeting =
        CompanionMotion.celebration(
            jumpTime * CompanionMotion.JUMP_MS / IntroTimeline.MASCOT_JUMP_MS,
            spin = false,
            jump = true,
        )
    return greeting.pose.copy(
        blink = CompanionMotion.blink(time + BLINK_OFFSET_MS),
        swayDeg = idle.swayDeg,
    )
}

private const val ARM_SCALE = 0.35f
private const val BLINK_SCALE = 0.95f
private const val BLINK_OFFSET_MS = 2400L
private const val VIEWPORT_SIZE = 120f
private const val CROP_LEFT = 27f
private const val CROP_TOP = 40f
private val MascotSize = 240.dp
