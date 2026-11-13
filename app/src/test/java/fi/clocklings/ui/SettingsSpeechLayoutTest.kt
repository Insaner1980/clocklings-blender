package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.settings.SpeechAvailabilityInfo
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSpeechLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun voiceRefreshKeepsHeightAndExposesOnlyTheCurrentMessage() {
        val available = mutableStateOf(true)
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val viewport = mutableStateOf(360 to 1f)
        compose.setContent {
            val context = localizedContext(language.value)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(1f, viewport.value.second),
            ) {
                ClocklingsTheme {
                    Box(Modifier.width((viewport.value.first - 32).dp).testTag("speech_info")) {
                        SpeechAvailabilityInfo(available.value)
                    }
                }
            }
        }
        for (entry in AppLanguage.entries) {
            for (cell in TestViewports) {
                compose.runOnIdle {
                    language.value = entry
                    viewport.value = cell
                    available.value = true
                }
                val resources = localizedContext(entry).resources
                val ready = resources.getString(R.string.settings_speech_info_available)
                val missing = resources.getString(R.string.settings_speech_info_unavailable)
                compose.onNodeWithText(ready).assertExists()
                compose.onNodeWithText(missing).assertDoesNotExist()
                val before = compose.onNodeWithTag("speech_info").getUnclippedBoundsInRoot()
                compose.runOnIdle { available.value = false }
                compose.onNodeWithText(ready).assertDoesNotExist()
                compose.onNodeWithText(missing).assertExists()
                val after = compose.onNodeWithTag("speech_info").getUnclippedBoundsInRoot()
                assertEquals(
                    "$entry / $cell",
                    before.bottom - before.top,
                    after.bottom - after.top,
                )
            }
        }
    }
}
