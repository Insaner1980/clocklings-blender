package fi.clocklings.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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

    /**
     * The size set last stays, whatever the viewport, so the clock keeps its size while other content and a
     * bottom bar show. A new screen starts at the full size.
     */
    KEEP,
}

/** The clock size set while it is still unknown how much room the content leaves for it. */
private class HeldClockSize {
    var size: Dp? = null
}

/**
 * [base], or under [ClockFit.FIT] at most the [viewport] height left after [rest]. Until [rest] is first measured,
 * and under [ClockFit.KEEP], the clock stays at its last size, so it never shows the full size for a frame before
 * fitting. With [animate] a change takes [SkMotion.RESIZE_DURATION], or snaps with reduced motion.
 */
@Composable
internal fun scaffoldClockSize(
    base: Dp,
    viewport: Dp,
    fit: ClockFit,
    rest: Dp?,
    animate: Boolean,
): Dp {
    val held = remember { HeldClockSize() }
    val target =
        when {
            fit == ClockFit.NONE -> base
            fit == ClockFit.KEEP -> held.size ?: base
            rest != null -> min(base, max(SkDimens.FitClockMin, viewport - rest))
            else -> held.size ?: base
        }
    SideEffect { held.size = target }
    if (!animate) return target
    val reduced = rememberReduceMotion()
    val shown by animateDpAsState(
        target,
        if (reduced) snap() else tween(SkMotion.RESIZE_DURATION),
        label = "clockSize",
    )
    return shown
}
