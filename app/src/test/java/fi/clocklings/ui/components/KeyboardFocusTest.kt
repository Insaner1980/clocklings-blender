package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyboardFocusTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun focusedButtonHasAVisibleIndicatorAtNarrowWidthsAndLargeFonts() {
        val viewport = mutableStateOf(411 to 1f)
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            CompositionLocalProvider(LocalDensity provides Density(2f, viewport.value.second)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        key(viewport.value) {
                            Column {
                                SkButton("Other", {})
                                SkButton("Continue", {})
                            }
                        }
                    }
                }
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            val button = compose.onNodeWithText("Continue")
            compose.onNodeWithText("Other").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            // Focus semantics change before the collected interaction has recomposed and drawn the border.
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            button.assertIsNotFocused()
            val before =
                button.captureToImage().let { image ->
                    IntArray(image.width * image.height).also { image.readPixels(it) }
                }
            button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            button.assertIsFocused()
            val after =
                button.captureToImage().let { image ->
                    IntArray(image.width * image.height).also { image.readPixels(it) }
                }
            assertFalse("$cell: focus must change the rendered button", before.contentEquals(after))
        }
    }
}
