package fi.clocklings.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.ledgeColor

/**
 * One stop on a winding path. Nodes alternate between the left and right side and their title,
 * and [badge] sit beside them, so the path uses the screen width instead of a tall column.
 * A pebble trail runs behind the nodes from centre to centre.
 *
 * Done stops are fully coloured with a check, the [current] stop is larger, ringed and gently
 * pulsing and announced as up next, and stops ahead are paler. An [isHeading] stop is a screen reader
 * heading without any visual change.
 */
@Composable
fun PathNode(
    index: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SkColors.KouluFill,
    completed: Boolean = false,
    current: Boolean = false,
    first: Boolean = index == 0,
    last: Boolean = false,
    isHeading: Boolean = false,
    enabled: Boolean = true,
    badge: @Composable () -> Unit = {},
    art: @Composable () -> Unit = { Text((index + 1).toString(), style = SkType.LessonNumber) },
) {
    val reduced = rememberReduceMotion()
    val pulse = if (current && !reduced) nodePulse() else 1f
    val nodeOnStart = index % 2 == 0
    val fill = if (completed || current) color else lerp(color, SkColors.Surface, AHEAD_FADE)
    val currentState = stringResource(R.string.path_current)
    val node: @Composable () -> Unit = {
        DepthSurface(
            onClick,
            fill,
            ledgeColor(color),
            SkShapes.Node,
            Modifier
                .size(SkDimens.PathNode)
                .semantics {
                    contentDescription = description
                    if (isHeading) heading()
                    if (current) stateDescription = currentState
                }.graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }.then(
                    if (current) Modifier.border(SkDimens.Border, SkColors.GoldDeep, SkShapes.Node) else Modifier,
                ),
            // The number, icon or clock and the check only repeat what the description says.
            decorativeContent = true,
            enabled = enabled,
        ) {
            Box(contentAlignment = Alignment.Center) {
                art()
                if (completed) SelectionBadge(Modifier.align(Alignment.TopEnd))
            }
        }
    }
    val texts: @Composable (Modifier) -> Unit = { textModifier ->
        PathNodeLabel(title, nodeOnStart, textModifier, badge)
    }
    Row(
        modifier
            .fillMaxWidth()
            .pathTrail(nodeOnStart, first, last)
            .padding(vertical = SkDimens.PathGap / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.Gap),
    ) {
        if (nodeOnStart) {
            Spacer(Modifier.width(SkDimens.PathLead))
            node()
            texts(Modifier.weight(1f))
        } else {
            texts(Modifier.weight(1f))
            node()
            Spacer(Modifier.width(SkDimens.PathLead))
        }
    }
}

private const val AHEAD_FADE = 0.45f

@Composable
private fun nodePulse(): Float {
    val transition = rememberInfiniteTransition(label = "current stop")
    val scale by transition.animateFloat(
        1f,
        SkMotion.PULSE_SCALE,
        infiniteRepeatable(tween(SkMotion.PULSE_DURATION), RepeatMode.Reverse),
        label = "node pulse",
    )
    return scale
}

/**
 * The trail from the previous stop (entering at the top centre) to this node's centre, and on to
 * the next stop (leaving at the bottom centre). Neighbouring rows meet at the centre line, so the
 * pieces join into one smooth S-curve.
 */
private fun Modifier.pathTrail(
    nodeOnStart: Boolean,
    first: Boolean,
    last: Boolean,
): Modifier =
    drawBehind {
        val lead = SkDimens.PathLead.toPx() + SkDimens.PathNode.toPx() / 2
        val node = Offset(if (nodeOnStart) lead else size.width - lead, size.height / 2)
        val top = Offset(size.width / 2, 0f)
        val bottom = Offset(size.width / 2, size.height)
        val outside = if (nodeOnStart) SkDimens.PathLead.toPx() else size.width - SkDimens.PathLead.toPx()
        val path =
            Path().apply {
                if (!first) {
                    moveTo(top.x, top.y)
                    cubicTo(top.x, top.y, outside, top.y, outside, node.y)
                    lineTo(node.x, node.y)
                } else {
                    moveTo(node.x, node.y)
                }
                if (!last) {
                    lineTo(outside, node.y)
                    cubicTo(outside, bottom.y, bottom.x, bottom.y, bottom.x, bottom.y)
                }
            }
        drawPath(
            path,
            SkColors.Trail,
            style =
                Stroke(
                    SkDimens.PathStroke.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(SkDimens.PathDash.toPx(), SkDimens.PathDashGap.toPx()),
                        ),
                ),
        )
    }

@Composable
private fun PathNodeLabel(
    title: String,
    nodeOnStart: Boolean,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit,
) {
    Column(
        modifier,
        horizontalAlignment = if (nodeOnStart) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        WordSafeText(title, SkType.LessonTitle, textAlign = if (nodeOnStart) TextAlign.Start else TextAlign.End)
        badge()
    }
}
