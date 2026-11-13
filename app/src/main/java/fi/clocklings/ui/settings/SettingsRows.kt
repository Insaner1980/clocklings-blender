package fi.clocklings.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        SettingsCard(content)
    }
}

/** White rounded container; rows bring their own vertical padding, so the card adds only a little. */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SkColors.Surface, SkShapes.Card)
            .padding(horizontal = SkDimens.SettingsGroupPadding, vertical = SkDimens.SettingsCardInset),
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
            .heightIn(min = 48.dp)
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
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onChange(it)
            })
            .padding(vertical = SkDimens.GapS),
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
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = SkColors.Hour,
                        checkedThumbColor = SkColors.Surface,
                        uncheckedTrackColor = SkColors.Surface,
                        uncheckedBorderColor = SkColors.InkSoft,
                        uncheckedThumbColor = SkColors.InkSoft,
                        disabledCheckedTrackColor = SkColors.Hour.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
                        disabledCheckedThumbColor = SkColors.Surface.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
                        disabledUncheckedTrackColor = SkColors.Surface.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
                        disabledUncheckedBorderColor = SkColors.InkSoft.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
                        disabledUncheckedThumbColor = SkColors.InkSoft.copy(alpha = SkColors.DISABLED_CONTROL_ALPHA),
                    ),
            )
        }
        if (supportingBelow) Text(supporting, style = SkType.SmallSoft)
    }
}
