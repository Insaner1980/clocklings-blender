package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AskScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lateSetterAndRepeatedSwapActionsPreserveTheTurn() {
        val viewport = mutableStateOf(360 to 1f)
        val model = AppViewModel(FakeServices(fullAccess = true))
        compose.setAskContent(model, viewport, resetOnResize = true)
        for (cell in TestViewports) {
            compose.runOnIdle { viewport.value = cell }
            val queued =
                compose
                    .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                    .fetchSemanticsNode()
                    .config[SemanticsActions.CustomActions]
                    .first()
            compose.onNodeWithText("Pass to partner").performClick()
            var accepted = true
            compose.runOnIdle { accepted = queued.action() }
            compose.onNodeWithText("Show the answer").performClick()
            compose.onNodeWithText("3:00").assertIsDisplayed()
            assertFalse("$cell: the setter action must be rejected after handover", accepted)
            val swap =
                compose
                    .onNodeWithText("Swap turns")
                    .fetchSemanticsNode()
                    .config[SemanticsActions.OnClick]
                    .action!!
            compose.runOnIdle {
                swap()
                swap()
            }
            compose.onNodeWithContentDescription("Player 2 sets the time").assertIsDisplayed()
        }
    }
}

internal fun ComposeContentTestRule.setAskContent(
    model: AppViewModel,
    viewport: State<Pair<Int, Float>>,
    resetOnResize: Boolean = false,
) {
    setContent {
        val (width, fontScale) = viewport.value
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalAppViewModel provides model,
            LocalDensity provides Density(base.density, fontScale),
        ) {
            key(if (resetOnResize) viewport.value else null) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(width.dp, 640.dp)) {
                        AskScreen(false, false, false, {})
                    }
                }
            }
        }
    }
}
