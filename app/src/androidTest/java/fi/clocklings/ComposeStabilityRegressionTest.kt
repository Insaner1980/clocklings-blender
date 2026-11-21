package fi.clocklings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.components.ConfettiState
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeStabilityRegressionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Test
    fun consumedLessonOrGameCelebrationIsNotRepeatedAfterRecreation() {
        val restoration = StateRestorationTester(compose)
        val celebrate = mutableIntStateOf(0)
        val confetti = ConfettiState(density = 1f, enabled = true)
        restoration.setContent {
            CompositionLocalProvider(LocalConfetti provides confetti) {
                val anchors = rememberCelebrationAnchors(celebrate.intValue, 5) { 1 }
                anchors[1] = Offset.Zero
            }
        }
        compose.runOnIdle { celebrate.intValue = 1 }
        compose.runOnIdle { assertEquals(5, confetti.particles.size) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(5, confetti.particles.size) }
        compose.runOnIdle { celebrate.intValue = 2 }
        compose.runOnIdle { assertEquals(10, confetti.particles.size) }
    }

    @Test
    fun recordConfettiUsesActionBarAnchorAndDoesNotRepeatOnLayout() {
        val restoration = StateRestorationTester(compose)
        val state = mutableStateOf(ChallengeUiState(phase = ChallengePhase.END, score = 12, best = 10))
        val confetti = ConfettiState(density = 1f, enabled = true)
        restoration.setContent {
            ClocklingsTheme {
                CompositionLocalProvider(LocalConfetti provides confetti) {
                    ChallengeScreen(state.value, h24 = false, help = false, onBack = {}, onStart = {}, onPick = {})
                }
            }
        }
        val back = compose.onNodeWithText("Back").assertIsDisplayed().getUnclippedBoundsInRoot()
        val again = compose.onNodeWithText("Play again").assertIsDisplayed().getUnclippedBoundsInRoot()
        val centerX = (minOf(back.left.value, again.left.value) + maxOf(back.right.value, again.right.value)) / 2f
        val centerY = (minOf(back.top.value, again.top.value) + maxOf(back.bottom.value, again.bottom.value)) / 2f
        val density =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        compose.runOnIdle { state.value = state.value.copy(newRecord = true) }
        compose.runOnIdle {
            assertEquals(120, confetti.particles.size)
            confetti.particles.forEach { particle ->
                assertEquals(centerX * density, particle.x, 1f)
                assertEquals(centerY * density, particle.y, 1f)
            }
        }
        compose.onNodeWithText("Back").scrollToIfScrollable().assertIsDisplayed()
        compose.runOnIdle { assertEquals(120, confetti.particles.size) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(120, confetti.particles.size) }
    }
}
