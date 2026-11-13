package fi.clocklings.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.sectionBackground

/** Short windows reserve a right-hand action row while the clock uses the full left-hand height. */
@Composable
internal fun ClockTaskScaffold(
    section: Section,
    topBar: @Composable () -> Unit,
    visual: @Composable (Dp) -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable (TaskActionFocus, Boolean) -> Unit)? = null,
    status: @Composable () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    compactScrollState: ScrollState = rememberScrollState(),
    clockHeightShare: Float = SkDimens.CLOCK_HEIGHT_SHARE,
    clockFit: ClockFit = ClockFit.NONE,
    animateClock: Boolean = false,
    content: @Composable (compact: Boolean) -> Unit,
) {
    val header = rememberMovingTaskContent(topBar)
    val progress = rememberMovingTaskContent(status)
    val actionFocus = remember { TaskActionFocus() }
    val latestActions by rememberUpdatedState(actions)
    val buttons = remember { movableContentOf<Boolean> { latestActions?.invoke(actionFocus, it) } }
    val latestVisual by rememberUpdatedState(visual)
    val clock = remember { movableContentOf<Dp> { latestVisual(it) } }
    val latestContent by rememberUpdatedState(content)
    val controls = remember { movableContentOf<Boolean> { latestContent(it) } }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = isCompactTask(maxWidth - SkDimens.Gutter * 2, maxHeight, LocalDensity.current.fontScale)
        val restoreFocus = remember(compact) { actionFocus.current }
        LaunchedEffect(compact) {
            if (restoreFocus != null) {
                withFrameNanos { }
                restoreFocus.requestFocus()
            }
        }
        if (compact) {
            CompactClockTask(
                section,
                header,
                progress,
                clock,
                if (actions !=
                    null
                ) {
                    { buttons(true) }
                } else {
                    null
                },
                compactScrollState,
            ) {
                controls(true)
            }
        } else {
            ScreenScaffold(
                section = section,
                wideContent = true,
                centerContent = true,
                topBar = header,
                bottomBar =
                    if (actions != null) {
                        { buttons(false) }
                    } else {
                        null
                    },
                scrollState = scrollState,
                clockHeightShare = clockHeightShare,
                clockFit = clockFit,
                animateClock = animateClock,
            ) { size ->
                progress()
                AdaptiveContent(size, clock, controlsAlignment = Alignment.CenterVertically) { controls(false) }
            }
        }
    }
}

@Composable
private fun CompactClockTask(
    section: Section,
    header: @Composable () -> Unit,
    status: @Composable () -> Unit,
    visual: @Composable (Dp) -> Unit,
    actions: (@Composable () -> Unit)?,
    scrollState: ScrollState,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().sectionBackground(section).safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = SkDimens.WideContentWidth)
                .fillMaxSize()
                .padding(horizontal = SkDimens.Gutter, vertical = SkDimens.GapS),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            header()
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val visualWidth =
                    min(
                        maxWidth * COMPACT_VISUAL_SHARE,
                        maxWidth - SkDimens.ControlsMinWidth * LocalDensity.current.fontScale - SkDimens.GapL,
                    )
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(SkDimens.GapL)) {
                    Column(
                        Modifier.width(visualWidth).fillMaxHeight().testTag("adaptive_visual"),
                        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                    ) {
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val size = min(min(maxWidth, maxHeight), SkDimens.ClockMax)
                            Box(Modifier.size(size).testTag("task_clock")) { visual(size) }
                        }
                        status()
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                    ) {
                        TaskInstructions(scrollState, Modifier.weight(1f), content)
                        TaskActions(actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInstructions(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().testTag("compact_task_controls").verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth().testTag("adaptive_controls")) { content() }
    }
}

@Composable
private fun TaskActions(actions: (@Composable () -> Unit)?) {
    if (actions != null) {
        Box(Modifier.fillMaxWidth().testTag("task_actions").padding(bottom = SkDimens.GapS)) { actions() }
    }
}

internal fun isCompactTask(
    width: Dp,
    height: Dp,
    fontScale: Float,
): Boolean =
    height < SkDimens.CompactTaskHeight && width >= SkDimens.SideBySideWidth &&
        width >= SkDimens.FitClockMin + SkDimens.ControlsMinWidth * fontScale + SkDimens.GapL

@Composable
private fun rememberMovingTaskContent(content: @Composable () -> Unit): @Composable () -> Unit {
    val latest by rememberUpdatedState(content)
    return remember { movableContentOf { latest() } }
}

private const val COMPACT_VISUAL_SHARE = 1f / 3f

@Stable
internal class TaskActionFocus {
    var current: FocusRequester? by mutableStateOf(null)
        private set

    fun changed(
        requester: FocusRequester,
        focused: Boolean,
    ) {
        if (focused) {
            current = requester
        } else if (current === requester) {
            current = null
        }
    }
}

/** A direct requester survives a pane move even when Compose clears the moved focus target. */
@Composable
internal fun Modifier.taskActionFocus(focus: TaskActionFocus): Modifier {
    val requester = remember { FocusRequester() }
    return focusRequester(requester).onFocusChanged { focus.changed(requester, it.isFocused) }
}
