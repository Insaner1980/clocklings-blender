package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import fi.clocklings.R
import fi.clocklings.domain.DayPart
import fi.clocklings.ui.clock.DayPartLabels
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes

/** An opaque card for informational content; its artwork children remain unclipped. */
@Composable
fun FlatPanel(
    modifier: Modifier = Modifier,
    padding: Dp = SkDimens.PanelPadding,
    color: Color = SkColors.Surface,
    shape: Shape = SkShapes.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(color, shape).padding(padding),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        content = content,
    )
}

/** Kello keskitettynä annettuun kokoon. */
@Composable
fun ClockBox(
    size: Dp,
    modifier: Modifier = Modifier,
    dayPart: DayPart? = null,
    showZonePills: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(Modifier.testTag("task_clock")) { content() }
            if (dayPart != null) DayPartLabels(dayPart, showZonePills)
        },
    ) { children, constraints ->
        val chip = children.getOrNull(1)?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val below = if (chip == null) 0 else chip.height + SkDimens.GapS.roundToPx()
        val maxSquare = minOf(constraints.maxWidth, (constraints.maxHeight - below).coerceAtLeast(0))
        val square = size.roundToPx().coerceIn(0, maxSquare)
        val dial = children.first().measure(Constraints.fixed(square, square))
        val width = constraints.constrainWidth(maxOf(dial.width, chip?.width ?: 0))
        layout(width, constraints.constrainHeight(dial.height + below)) {
            dial.placeRelative((width - dial.width) / 2, 0)
            chip?.placeRelative((width - chip.width) / 2, dial.height + SkDimens.GapS.roundToPx())
        }
    }
}

/** Toggle the clock guides, with the current state available to screen readers. */
@Composable
fun GuidesToggle(
    help: Boolean,
    onToggle: () -> Unit,
) {
    RoundIconButton(
        SkIcons.Guides,
        stringResource(if (help) R.string.guides_on else R.string.guides_off),
        onToggle,
        selected = help,
    )
}

@Composable
fun HelpTopBar(
    title: String,
    help: Boolean,
    onBack: () -> Unit,
    onToggleHelp: () -> Unit,
) {
    TopBar(
        title = title,
        onBack = onBack,
        trailing = { GuidesToggle(help, onToggleHelp) },
    )
}
