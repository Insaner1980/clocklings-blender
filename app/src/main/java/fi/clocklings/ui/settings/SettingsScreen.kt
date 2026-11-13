package fi.clocklings.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Settings
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.locale.LocalAppLocale
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun SettingsScreen(
    settings: Settings,
    speechAvailable: Boolean,
    onLanguage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onParent: (() -> Unit)? = null,
) {
    val app = LocalAppViewModel.current
    val action by app.settingsAction.collectAsStateWithLifecycle()
    val locale by LocalAppLocale.current.selection.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    // A language change or activity recreation dismisses a destructive confirmation safely.
    var confirmReset by remember(language) { mutableStateOf(false) }
    LaunchedEffect(app) { app.clearSettingsError() }
    ScreenScaffold(
        modifier,
        section = Section.SETTINGS,
        topBar = { TopBar(stringResource(R.string.settings_title), onBack) },
    ) {
        // Every group: a small heading outside, then one calm container of rows with dividers.
        SettingsGroup(stringResource(R.string.language_title)) {
            LanguageRow(languageName(locale.choice), onLanguage)
        }
        ClockDisplayGroup(settings, enabled = !action.pending)
        SettingsGroup(stringResource(R.string.settings_sound_group)) {
            SettingsSwitch(
                stringResource(R.string.settings_sound_title),
                stringResource(R.string.settings_sound_description),
                settings.sound,
                !action.pending,
                app::setSound,
            )
        }
        SpeechAvailabilityInfo(speechAvailable)
        SettingsGroup(stringResource(R.string.settings_progress_title)) {
            ResetProgressRow(!action.pending) {
                app.clearSettingsError()
                confirmReset = true
            }
        }
        if (action.failed) Text(stringResource(R.string.settings_save_error), style = SkType.Body)
        if (onParent != null) {
            SkButton(stringResource(R.string.parent_title), onParent, Modifier.fillMaxWidth())
        }
    }
    if (confirmReset) {
        ResetProgressDialog(
            pending = action.pending,
            failed = action.failed,
            onDismiss = { confirmReset = false },
            onReset = { app.resetProgress { confirmReset = false } },
        )
    }
}

@Composable
internal fun SpeechAvailabilityInfo(available: Boolean) {
    // Refreshing the voice must not shrink the content and clamp a restored scroll offset.
    Box {
        for ((state, message) in listOf(
            true to R.string.settings_speech_info_available,
            false to R.string.settings_speech_info_unavailable,
        )) {
            Text(
                stringResource(message),
                modifier = if (state == available) Modifier else Modifier.alpha(0f).clearAndSetSemantics {},
                style = SkType.SmallSoft,
            )
        }
    }
}

@Composable
private fun ClockDisplayGroup(
    settings: Settings,
    enabled: Boolean,
) {
    val app = LocalAppViewModel.current
    val words = timeLanguage(LocalAppLanguage.current)
    SettingsGroup(stringResource(R.string.settings_clock_display)) {
        Column(Modifier.selectableGroup()) {
            RadioRow(
                stringResource(R.string.mode_12),
                words.digital(EXAMPLE_TIME),
                !settings.h24,
                enabled,
            ) {
                app.setH24(false)
            }
            SettingsDivider()
            RadioRow(
                stringResource(R.string.mode_24),
                words.digital(EXAMPLE_TIME, true),
                settings.h24,
                enabled,
            ) { app.setH24(true) }
        }
        SettingsDivider()
        SettingsSwitch(
            stringResource(R.string.settings_guides_title),
            stringResource(R.string.settings_guides_description),
            settings.help,
            enabled,
            app::setHelp,
        )
    }
}

/** Destructive, so it gets its own colour and a confirmation dialog. */
@Composable
private fun ResetProgressRow(
    enabled: Boolean,
    onReset: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SkDimens.IconButton)
            .clickable(enabled = enabled, role = Role.Button, onClick = onReset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.settings_reset_action), style = SkType.Body.copy(color = SkColors.Danger))
    }
}

@Composable
private fun ResetProgressDialog(
    pending: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!pending) onDismiss() },
        title = { Text(stringResource(R.string.settings_reset_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_reset_body))
                if (failed) Text(stringResource(R.string.settings_save_error))
            }
        },
        confirmButton = {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                SkButton(stringResource(R.string.settings_cancel), onDismiss, enabled = !pending)
                SkButton(stringResource(R.string.settings_reset_action), onReset, enabled = !pending)
            }
        },
    )
}

@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalAppLocale.current
    val selection by controller.selection.collectAsStateWithLifecycle()
    val app = LocalAppViewModel.current
    ScreenScaffold(
        modifier,
        section = Section.SETTINGS,
        topBar = { TopBar(stringResource(R.string.language_title), onBack) },
    ) {
        SettingsCard {
            Column(Modifier.selectableGroup()) {
                (listOf<AppLanguage?>(null) + AppLanguage.entries).forEachIndexed { index, language ->
                    if (index > 0) SettingsDivider()
                    RadioRow(languageName(language), null, selection.choice == language, !selection.pending) {
                        if (selection.choice != language) app.stopSpeech()
                        controller.select(language)
                    }
                }
            }
        }
        if (selection.failed) Text(stringResource(R.string.language_save_error), style = SkType.Body)
    }
}

@Composable
private fun languageName(language: AppLanguage?): String =
    stringResource(
        when (language) {
            null -> R.string.language_device
            AppLanguage.ENGLISH -> R.string.language_english
            AppLanguage.FINNISH -> R.string.language_finnish
            AppLanguage.SWEDISH -> R.string.language_swedish
            AppLanguage.GERMAN -> R.string.language_german
            AppLanguage.FRENCH -> R.string.language_french
            AppLanguage.SPANISH -> R.string.language_spanish
            AppLanguage.PORTUGUESE_BRAZIL -> R.string.language_portuguese_brazil
        },
    )

private const val EXAMPLE_TIME = 15 * 60 + 30

@Composable
private fun LanguageRow(
    value: String,
    onLanguage: () -> Unit,
) {
    val languageTitle = stringResource(R.string.language_title)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SkDimens.SettingsRowHeight)
            .semantics { contentDescription = "$languageTitle: $value" }
            .clickable(role = Role.Button, onClick = onLanguage),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, style = SkType.Body, modifier = Modifier.weight(1f))
        SkIcon(SkIcons.Chevron, SkDimens.ActionIcon)
    }
}
