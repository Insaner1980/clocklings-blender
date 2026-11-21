package fi.clocklings.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import fi.clocklings.ui.components.PressSurface
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SvgIcon
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

@Composable
internal fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Text(
            title,
            style = SkType.SettingsHeading,
            modifier = Modifier.padding(start = SkDimens.GapS).semantics { heading() },
        )
        content()
    }
}

/** White rounded container; rows bring their own vertical padding, so the card adds only a little. */
@Composable
internal fun SettingsCard(
    horizontalPadding: Dp = SkDimens.SettingsGroupPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SkColors.Surface, SkShapes.Card)
            .padding(horizontal = horizontalPadding, vertical = SkDimens.SettingsCardInset),
        content = content,
    )
}

@Composable
internal fun SettingsDivider() = HorizontalDivider(thickness = SkDimens.Divider, color = SkColors.Line)

@Composable
internal fun RadioRow(
    text: String,
    supporting: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SkDimens.SettingsRowHeight)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = SkDimens.GapS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        RadioButton(selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkDimens.SettingsLineGap)) {
            Text(text, style = SkType.Body)
            supporting?.let { Text(it, style = SkType.SmallSoft) }
        }
    }
}

@Composable
internal fun SettingsSwitch(
    text: String,
    supporting: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val supportingBelow = LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SkDimens.IconButton)
            .clip(SkShapes.Row)
            .background(SkColors.Surface)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onChange(it)
            })
            .padding(SkDimens.Gap),
        verticalArrangement = Arrangement.spacedBy(SkDimens.SettingsLineGap),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkDimens.SettingsLineGap)) {
                Text(text, style = SkType.Body)
                if (!supportingBelow) Text(supporting, style = SkType.SmallSoft)
            }
            Switch(
                checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent =
                    if (checked) {
                        (
                            {
                                SkIcon(
                                    SkIcons.Check,
                                    SkDimens.SmallIcon,
                                    tint = SkColors.Primary,
                                )
                            }
                        )
                    } else {
                        null
                    },
                colors =
                    settingsSwitchColors(),
            )
        }
        if (supportingBelow) Text(supporting, style = SkType.SmallSoft)
    }
}

/** A navigation or destructive-action row with a distinct icon badge. */
@Composable
internal fun SettingsNavRow(
    icon: SvgIcon,
    badgeColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = SkColors.Ink,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    PressSurface(onClick, SkColors.Surface, SkShapes.Row, modifier.fillMaxWidth(), enabled = enabled) {
        Row(
            Modifier
                .heightIn(
                    min = SkDimens.ButtonHeight,
                ).padding(start = SkDimens.GapM, end = SkDimens.Gap, top = SkDimens.GapM, bottom = SkDimens.GapM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            Box(
                Modifier.size(SkDimens.IconBadge).background(badgeColor, SkShapes.IconBadge),
                contentAlignment = Alignment.Center,
            ) {
                SkIcon(icon, SkDimens.ButtonIcon, tint = titleColor)
            }
            WordSafeText(title, SkType.Body.copy(color = titleColor), Modifier.weight(1f), textAlign = TextAlign.Start)
            if (showChevron) SkIcon(SkIcons.Chevron, SkDimens.ActionIcon, tint = SkColors.UiInkSoft)
        }
    }
}

/** Two radio choices that stack when large text or a narrow card needs the room. */
@Composable
internal fun ChoiceSegments(
    firstTitle: String,
    secondTitle: String,
    firstExample: String,
    secondExample: String,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(SkColors.Surface, SkShapes.Card)
            .padding(SkDimens.GapS)
            .selectableGroup(),
    ) {
        val stack =
            maxWidth + SkDimens.GapS * 2 < SkDimens.GridBreakpoint ||
                LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        val segment: @Composable (Int, Modifier) -> Unit = { index, modifier ->
            ClockDisplaySegment(
                if (index ==
                    0
                ) {
                    firstTitle
                } else {
                    secondTitle
                },
                if (index == 0) firstExample else secondExample,
                index == selectedIndex,
                enabled,
                { onSelect(index) },
                modifier,
            )
        }

        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                segment(0, Modifier.fillMaxWidth())
                segment(1, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                segment(0, Modifier.weight(1f))
                segment(1, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun settingsSwitchColors() =
    SwitchDefaults.colors(
        checkedTrackColor = SkColors.Primary,
        checkedThumbColor = SkColors.Surface,
        uncheckedTrackColor = SkColors.Surface,
        uncheckedBorderColor = SkColors.UiInkSoft,
        uncheckedThumbColor = SkColors.UiInkSoft,
        disabledCheckedTrackColor = SkColors.Primary.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
        disabledCheckedThumbColor = SkColors.Surface.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
        disabledUncheckedTrackColor = SkColors.Surface.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
        disabledUncheckedBorderColor = SkColors.UiInkSoft.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
        disabledUncheckedThumbColor = SkColors.UiInkSoft.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
    )

@Composable
private fun ClockDisplaySegment(
    title: String,
    example: String,
    on: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressSurface(
        onClick,
        if (on) SkColors.Primary else SkColors.SettingsBackground,
        SkShapes.Option,
        modifier.semantics {
            selected =
                on
        },
        enabled = enabled,
        role = Role.RadioButton,
    ) {
        Row(
            Modifier.heightIn(min = SkDimens.ButtonHeight).padding(SkDimens.GapS),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (on) SkIcon(SkIcons.Check, SkDimens.ButtonIcon, tint = SkColors.OnPrimary)
            Column(Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.CenterHorizontally) {
                WordSafeText(
                    title,
                    SkType.ButtonSmall.copy(color = if (on) SkColors.OnPrimary else SkColors.Ink),
                )
                Text(
                    example,
                    style =
                        SkType.SmallSoft.copy(
                            color = if (on) SkColors.OnPrimary else SkColors.UiInkSoft,
                        ),
                )
            }
        }
    }
}
