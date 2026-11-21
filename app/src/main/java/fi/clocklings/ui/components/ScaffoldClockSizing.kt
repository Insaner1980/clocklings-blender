package fi.clocklings.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkDimens

/**
 * Reports the natural content height, not stretched to the viewport, without the [clockSize] it shows;
 * the layout itself is unchanged.
 */
internal fun Modifier.measureRestHeight(
    clockSize: Dp,
    onRestHeight: (Int) -> Unit,
) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    onRestHeight(placeable.height - clockSize.roundToPx())
    layout(placeable.width, maxOf(placeable.height, constraints.minHeight)) { placeable.place(0, 0) }
}

/** How [ScreenScaffold] fits its clock to the content below it. */
enum class ClockFit {
    /** The clock takes its full size. */
    NONE,

    /**
     * The content is measured and the clock capped so that all of it fits without scrolling, but never below
     * [SkDimens.FitClockMin]. The content must show the clock once, at exactly the given size.
     */
    FIT,
}

/** The clock size set while it is still unknown how much room the content leaves for it. */
private class HeldClockSize {
    var size: Dp? = null
    var task: Int? = null
}

/**
 * [base], or under [ClockFit.FIT] at most the [viewport] height left after [rest]. Until [rest] is first measured
 * the clock stays at its last size. With [animate] a change takes [SkMotion.RESIZE_DURATION],
 * or snaps with reduced motion.
 */
@Composable
internal fun scaffoldClockSize(
    base: Dp,
    viewport: Dp,
    fit: ClockFit,
    rest: Dp?,
    animate: Boolean,
    fitMin: Dp = SkDimens.FitClockMin,
    clockTaskKey: Int? = null,
): Dp {
    val held = remember { HeldClockSize() }
    val sameTaskSize = held.size.takeIf { held.task == clockTaskKey }
    val computed =
        when {
            fit == ClockFit.NONE -> base
            rest != null -> min(base, max(fitMin, viewport - rest))
            else -> (if (animate) held.size else sameTaskSize) ?: base
        }
    val target = if (clockTaskKey != null) min(computed, sameTaskSize ?: computed) else computed
    if (fit == ClockFit.NONE || rest != null) {
        SideEffect {
            held.size = target
            held.task = clockTaskKey
        }
    }
    if (!animate && clockTaskKey == null) return target
    val reduced = rememberReduceMotion()
    // An already animated scaffold can grow into a new run. Practice and quiz reset immediately.
    return key(clockTaskKey.takeUnless { animate }) {
        val shown by animateDpAsState(
            target,
            if (reduced) snap() else tween(SkMotion.RESIZE_DURATION),
            label = "clockSize",
        )
        shown
    }
}
