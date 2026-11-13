package fi.clocklings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.Settings
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.settings.SettingsScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SettingsErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun reopeningSettingsAndResetDialogClearOnlyPreviousErrors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ProgressRepository(FailedSettingsStore())
        val services =
            object : AppServices by (context.applicationContext as ClocklingsApplication).container {
                override val repository = repository
            }
        val factory = viewModelFactory { initializer { AppViewModel(services) } }
        val visible = mutableStateOf(true)
        compose.setContent {
            val app = viewModel<AppViewModel>(factory = factory)
            CompositionLocalProvider(LocalAppViewModel provides app) {
                AppLocaleProvider(repository) {
                    ClocklingsTheme {
                        if (visible.value) {
                            SettingsScreen(Settings(), false, {}, { visible.value = false })
                        }
                    }
                }
            }
        }
        val error = context.getString(R.string.settings_save_error)
        val guides = context.getString(R.string.settings_guides_title)
        val reset = context.getString(R.string.settings_reset_action)
        click(guides)
        waitForError(error)
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle { visible.value = true }
        compose.onAllNodesWithText(error).assertCountEquals(0)
        click(guides)
        waitForError(error)
        click(reset)
        compose.onAllNodesWithText(error).assertCountEquals(0)
        click(reset)
        waitForError(error)
        click(context.getString(R.string.settings_cancel))
        click(reset)
        compose.onAllNodesWithText(error).assertCountEquals(0)
    }

    private fun waitForError(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) {
        val node = compose.onAllNodes(hasText(text) and hasClickAction()).onLast()
        if (generateSequence(node.fetchSemanticsNode().parent) { it.parent }.any {
                SemanticsActions.ScrollBy in
                    it.config
            }
        ) {
            node.scrollToIfScrollable()
        }
        node.performClick()
    }
}

private class FailedSettingsStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        throw IOException("Synthetic settings write failure")
}
