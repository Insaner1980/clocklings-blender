package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** One learning step; an interactive trailing shortcut remains outside its main click target. */
@Composable
fun PathRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PathRowStyle = PathRowStyle.STEP,
    badgeColor: Color = if (style == PathRowStyle.CURRENT) SkColors.Primary else SkColors.KouluFill,
    completed: Boolean = false,
    locked: Boolean = false,
    isHeading: Boolean = false,
    enabled: Boolean = true,
    startLabel: String? = null,
    supporting: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    val current = style == PathRowStyle.CURRENT
    val practice = style == PathRowStyle.PRACTICE
    val shape = if (current) SkShapes.Card else SkShapes.Row
    val currentState = stringResource(R.string.path_current)
    val rowTrailing: (@Composable () -> Unit)? =
        if (locked) {
            { LockedPathBadge() }
        } else {
            trailing
        }
    val inlineShortcut = rowTrailing.takeIf { current && startLabel != null }
    val selectionBorder =
        if (current) Modifier.border(SkDimens.SelectionBorder, SkColors.Primary, shape) else Modifier
    val card =
        modifier
            .fillMaxWidth()
            .padding(start = if (practice) SkDimens.PracticeIndent else 0.dp)
            .background(if (practice) SkColors.KouluFill else SkColors.Surface, shape)
            .then(selectionBorder)
    val button: @Composable (Modifier) -> Unit = { hitArea ->
        PressSurface(
            onClick,
            Color.Transparent,
            shape,
            hitArea.semantics {
                contentDescription = description
                if (isHeading) heading()
                if (current) stateDescription = currentState
            },
            enabled = enabled,
            dimWhenDisabled = !locked,
            decorativeContent = true,
        ) {
            PathRowContent(
                title,
                style,
                badgeColor,
                completed,
                locked,
                startLabel,
                supporting,
                rowTrailing == null,
                leading,
                inlineShortcut != null,
            )
        }
    }
    if (inlineShortcut != null) {
        PathRowWithInlineShortcut(button, inlineShortcut, card)
    } else {
        PathRowWithTrailing(button, rowTrailing, card)
    }
}

@Composable
private fun LockedPathBadge() {
    Box(
        Modifier.size(SkDimens.IconButton).background(SkColors.TonalFill, SkShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        SkIcon(SkIcons.Lock, SkDimens.ActionIcon, tint = SkColors.Lock)
    }
}

@Composable
private fun PathRowWithInlineShortcut(
    button: @Composable (Modifier) -> Unit,
    inlineShortcut: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Place the book over the reserved header space as a separate accessibility target.
    Layout(
        modifier = modifier,
        content = {
            button(Modifier.fillMaxWidth())
            inlineShortcut()
        },
    ) { children, constraints ->
        val main = children[0].measure(constraints)
        val book = children[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(main.width, main.height) {
            main.placeRelative(0, 0)
            book.placeRelative(
                main.width - SkDimens.Gap.roundToPx() - book.width,
                main[HeaderCenter] - book.height / 2,
            )
        }
    }
}

@Composable
private fun PathRowWithTrailing(
    button: @Composable (Modifier) -> Unit,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val stackShortcut = LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
    FlowRow(modifier, maxItemsInEachRow = if (stackShortcut) 1 else 2) {
        button(Modifier.weight(1f).align(Alignment.CenterVertically))
        if (trailing != null) {
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .then(if (stackShortcut) Modifier.fillMaxWidth().padding(bottom = SkDimens.GapS) else Modifier)
                    .padding(end = SkDimens.Gap),
                contentAlignment = Alignment.CenterEnd,
            ) { trailing() }
        }
    }
}

private val HeaderCenter = HorizontalAlignmentLine(::minOf)

@Composable
private fun PathRowState(completed: Boolean) {
    when {
        completed -> {
            Box(
                Modifier.size(SkDimens.RowStateIcon).background(SkColors.Good, SkShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                SkIcon(SkIcons.Check, SkDimens.ActionIcon, tint = SkColors.OnPrimary)
            }
        }

        else -> {
            SkIcon(SkIcons.Chevron, SkDimens.ActionIcon, tint = SkColors.UiInkSoft)
        }
    }
}

@Composable
private fun PathRowContent(
    title: String,
    style: PathRowStyle,
    badgeColor: Color,
    completed: Boolean,
    locked: Boolean,
    startLabel: String?,
    supporting: (@Composable ColumnScope.() -> Unit)?,
    showState: Boolean,
    leading: @Composable () -> Unit,
    hasInlineShortcut: Boolean,
) {
    val current = style == PathRowStyle.CURRENT
    val practice = style == PathRowStyle.PRACTICE
    val stateOnOwnLine = LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
    val showIndicator = showState && style == PathRowStyle.STEP
    val padding =
        if (current) {
            SkDimens.Gap
        } else if (practice) {
            SkDimens.GapS
        } else {
            SkDimens.GapM
        }
    Column(
        Modifier
            .heightIn(min = if (practice) SkDimens.PracticeRowMinHeight else SkDimens.RowMinHeight)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(SkDimens.Gap),
    ) {
        Row(
            modifier = if (hasInlineShortcut) Modifier.reserveShortcutSpace() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (practice) SkDimens.GapM else SkDimens.GapLabel),
        ) {
            PathRowBadge(current, practice, badgeColor, locked, leading)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkDimens.GapXS)) {
                PathRowTitle(title, style)
                if (locked) LockPill() else supporting?.invoke(this)
            }
            if (showIndicator && !stateOnOwnLine) PathRowState(completed)
        }
        if (showIndicator && stateOnOwnLine) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                PathRowState(completed)
            }
        }
        if (current && startLabel != null) {
            PathRowStart(startLabel)
        }
    }
}

@Composable
private fun PathRowTitle(
    title: String,
    style: PathRowStyle,
) {
    WordSafeText(
        title,
        if (style == PathRowStyle.CURRENT) {
            SkType.LessonTitleCurrent
        } else if (style == PathRowStyle.PRACTICE) {
            SkType.PracticeTitle
        } else {
            SkType.LessonTitle
        },
        textAlign = TextAlign.Start,
    )
}

private fun Modifier.reserveShortcutSpace(): Modifier =
    padding(end = SkDimens.IconButton + SkDimens.GapLabel).layout { measurable, constraints ->
        val header = measurable.measure(constraints)
        layout(header.width, header.height, mapOf(HeaderCenter to header.height / 2)) {
            header.placeRelative(0, 0)
        }
    }

@Composable
private fun PathRowStart(startLabel: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = SkDimens.ButtonHeightSmall,
            ).background(SkColors.Primary, SkShapes.Pill)
            .padding(SkDimens.GapM),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapOptions, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkIcon(SkIcons.Play, SkDimens.ButtonIcon, tint = SkColors.OnPrimary)
        WordSafeText(
            startLabel,
            SkType.Button.copy(color = SkColors.OnPrimary),
            Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun PathRowBadge(
    current: Boolean,
    practice: Boolean,
    badgeColor: Color,
    locked: Boolean,
    leading: @Composable () -> Unit,
) {
    val badge = if (current) SkDimens.RowBadgeCurrent else SkDimens.RowBadge
    Box(
        Modifier
            .alpha(if (locked) SkColors.LOCKED_BADGE_ALPHA else 1f)
            .sizeIn(minWidth = badge, minHeight = badge)
            .background(
                if (practice) SkColors.Surface else badgeColor,
                if (practice) {
                    SkShapes.Pill
                } else if (current) {
                    SkShapes.BadgeLarge
                } else {
                    SkShapes.Badge
                },
            ),
        contentAlignment = Alignment.Center,
    ) { leading() }
}
