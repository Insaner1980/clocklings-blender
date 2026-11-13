package fi.clocklings.ui.components

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.localizedContext
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OptionSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun resultIsExposedOnTheAccessibleAnswerWithoutRevealingItBeforeSelection() {
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val viewport = mutableStateOf(411 to 1f)
        val state = mutableStateOf(OptionState.NORMAL)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            val context = localizedContext(language.value)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(2f, viewport.value.second),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        OptionButton(
                            "Three o'clock",
                            {},
                            Modifier.testTag("answer"),
                            state = state.value,
                            enabled = state.value == OptionState.NORMAL,
                        )
                    }
                }
            }
        }
        val results =
            listOf(
                AppLanguage.ENGLISH to ("Correct" to "Incorrect"),
                AppLanguage.FINNISH to ("Oikein" to "Väärin"),
                AppLanguage.SWEDISH to ("Rätt" to "Fel"),
                AppLanguage.GERMAN to ("Richtig" to "Falsch"),
                AppLanguage.FRENCH to ("Bonne réponse" to "Mauvaise réponse"),
                AppLanguage.SPANISH to ("Correcto" to "Incorrecto"),
                AppLanguage.PORTUGUESE_BRAZIL to ("Correto" to "Incorreto"),
            )
        for ((entry, labels) in results) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    language.value = entry
                    viewport.value = cell
                }
                assertFeedbackStates(state, labels, view)
            }
        }
    }

    private fun assertFeedbackStates(
        state: MutableState<OptionState>,
        labels: Pair<String, String>,
        view: View,
    ) {
        for (result in OptionState.entries) {
            compose.runOnIdle { state.value = result }
            val id = compose.onNodeWithTag("answer").fetchSemanticsNode().id
            val node = compose.runOnIdle { view.accessibilityNodeProvider.createAccessibilityNodeInfo(id)!! }
            when (result) {
                OptionState.NORMAL -> {
                    assertNull(node.stateDescription)
                }

                else -> {
                    val expected = if (result == OptionState.CORRECT) labels.first else labels.second
                    assertEquals(expected, node.stateDescription?.toString())
                    assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, node.liveRegion)
                }
            }
        }
    }
}
