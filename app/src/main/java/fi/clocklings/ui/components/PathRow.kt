package fi.clocklings.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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
    val stackShortcut = LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
    FlowRow(
        modifier
            .fillMaxWidth()
            .padding(start = if (practice) SkDimens.PracticeIndent else 0.dp)
            .background(if (practice) SkColors.KouluFill else SkColors.Surface, shape)
            .then(
                if (current) {
                    Modifier.border(
                        BorderStroke(SkDimens.SelectionBorder, SkColors.Primary),
                        shape,
                    )
                } else {
                    Modifier
                },
            ),
        maxItemsInEachRow = if (stackShortcut) 1 else 2,
    ) {
        PressSurface(
            onClick,
            Color.Transparent,
            shape,
            Modifier.weight(1f).align(Alignment.CenterVertically).semantics {
                contentDescription = description
                if (isHeading) heading()
                if (current) stateDescription = currentState
            },
            enabled = enabled,
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
                trailing == null,
                leading,
            )
        }
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

@Composable
private fun PathRowState(
    completed: Boolean,
    locked: Boolean,
) {
    when {
        completed -> {
            Box(
                Modifier.size(SkDimens.RowStateIcon).background(SkColors.Good, SkShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                SkIcon(SkIcons.Check, SkDimens.ActionIcon, tint = SkColors.OnPrimary)
            }
        }

        locked -> {
            SkIcon(SkIcons.Lock, SkDimens.ActionIcon)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (practice) SkDimens.GapM else SkDimens.GapLabel),
        ) {
            PathRowBadge(current, practice, badgeColor, leading)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkDimens.GapXS)) {
                WordSafeText(
                    title,
                    if (current) {
                        SkType.LessonTitleCurrent
                    } else if (practice) {
                        SkType.PracticeTitle
                    } else {
                        SkType.LessonTitle
                    },
                    textAlign = TextAlign.Start,
                )
                supporting?.invoke(this)
            }
            if (showIndicator && !stateOnOwnLine) PathRowState(completed, locked)
        }
        if (showIndicator && stateOnOwnLine) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                PathRowState(completed, locked)
            }
        }
        if (current && startLabel != null) {
            PathRowStart(startLabel)
        }
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
    leading: @Composable () -> Unit,
) {
    val badge = if (current) SkDimens.RowBadgeCurrent else SkDimens.RowBadge
    Box(
        Modifier
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
