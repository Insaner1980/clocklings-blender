package fi.clocklings.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.audio.Sound
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenStateRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val services = FakeServices(fullAccess = true)

    @Test
    fun exploreRestoresTheEditedTimeAndAppliesAModeChangeOnlyOnce() {
        val restoration = StateRestorationTester(compose)
        val h24 = mutableStateOf(false)
        val app = AppViewModel(services)
        restoration.setContent {
            CompositionLocalProvider(LocalAppViewModel provides app) {
                ClocklingsTheme { FreeClockScreen(h24.value, false, false, {}) }
            }
        }
        addFiveMinutes()
        compose.onNodeWithText("3:05").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("3:05").assertExists()
        compose.runOnIdle { h24.value = true }
        compose.onNodeWithText("15:05").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("15:05").assertExists()
        addFiveMinutes()
        compose.onNodeWithText("15:10").assertExists()
        assertEquals(emptyList<Any>(), services.sounds)
    }

    @Test
    fun togetherRestoresEachPhaseTimeAndSetterWithoutReplayingActionsAt320DpAnd200Percent() {
        val restoration = StateRestorationTester(compose)
        val restore = {
            val soundsBefore = services.sounds.toList()
            restoration.emulateSavedInstanceStateRestore()
            assertEquals(soundsBefore, services.sounds)
        }
        val app = AppViewModel(services)
        restoration.setContent {
            CompositionLocalProvider(
                LocalAppViewModel provides app,
                LocalDensity provides Density(LocalDensity.current.density, 2f),
            ) {
                Box(Modifier.requiredSize(320.dp, 914.dp)) {
                    ClocklingsTheme { AskScreen(false, false, false, {}) }
                }
            }
        }
        addFiveMinutes()
        restore()
        compose.onNodeWithContentDescription("Player 1 sets the time").assertExists()
        click("Pass to partner")
        restore()
        compose.onNodeWithContentDescription("Player 2 answers").assertExists()
        click("Show the answer")
        compose.onNodeWithText("3:05").assertExists()
        restore()
        compose.onNodeWithText("3:05").assertExists()
        click("Swap turns")
        restore()
        compose.onNodeWithContentDescription("Player 2 sets the time").assertExists()
        click("Pass to partner")
        click("Show the answer")
        compose.onNodeWithText("3:05").assertExists()
        assertEquals(2, services.sounds.count { it == Sound.QUESTION_READ })
    }

    private fun addFiveMinutes() {
        val action =
            compose
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first()
                .action
        compose.runOnIdle { action() }
        compose.waitForIdle()
    }

    private fun click(text: String) {
        val node = compose.onNodeWithText(text)
        if (generateSequence(node.fetchSemanticsNode().parent) { it.parent }.any {
                SemanticsActions.ScrollBy in it.config
            }
        ) {
            node.performScrollTo()
        }
        node.assertIsDisplayed().performClick()
    }
}
