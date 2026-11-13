package fi.clocklings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Test
    fun challengeShowsOptionsDisablesAnswersAndOffersReplay() {
        val state = mutableStateOf(ChallengeUiState())
        var starts = 0
        var chosen = -1
        var backs = 0
        compose.setContent {
            ClocklingsTheme {
                ChallengeScreen(
                    state = state.value,
                    h24 = true,
                    help = true,
                    onBack = { backs++ },
                    onStart = { starts++ },
                    onPick = { chosen = it },
                )
            }
        }
        compose.onNodeWithText("NO SCORE YET").assertExists()
        compose.onNodeWithText("Start!").scrollToIfScrollable().performClick()
        compose.runOnIdle {
            assertEquals(1, starts)
            state.value = state.value.copy(phase = ChallengePhase.RUN, options = listOf(180, 240, 300))
        }
        compose.onNodeWithText("03:00").scrollToIfScrollable().performClick()
        compose.runOnIdle {
            assertEquals(180, chosen)
            state.value =
                state.value.copy(
                    busy = true,
                    optionStates = mapOf(180 to OptionState.WRONG, 240 to OptionState.CORRECT),
                )
        }
        compose.onNodeWithText("03:00").assertIsNotEnabled()
        compose.onNodeWithText("04:00").assertIsNotEnabled()
        compose.runOnIdle {
            state.value = state.value.copy(phase = ChallengePhase.END, score = 2, best = 5)
        }
        compose.onNodeWithText("You answered 2 correctly. Your best is 5.").scrollToIfScrollable().assertIsDisplayed()
        compose.onNodeWithText("Play again").scrollToIfScrollable().performClick()
        compose.onNodeWithText("Back").scrollToIfScrollable().performClick()
        compose.runOnIdle {
            assertEquals(2, starts)
            assertEquals(1, backs)
            state.value = ChallengeUiState(best = 5)
        }
        compose.onNodeWithText("BEST SCORE: 5 CORRECT").assertExists()
    }
}
