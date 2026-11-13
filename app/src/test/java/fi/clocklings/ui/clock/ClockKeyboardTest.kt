package fi.clocklings.ui.clock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.ClockTime
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClockKeyboardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun arrowKeysSetTimeAcrossMidnightAndDisabledClockIgnoresThem() {
        val interactive = mutableStateOf(true)
        val viewport = mutableStateOf(411 to 1f)
        var value = 1_435
        lateinit var input: InputModeManager
        compose.setContent {
            input = LocalInputModeManager.current
            CompositionLocalProvider(LocalDensity provides Density(2f, viewport.value.second)) {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                    key(viewport.value) {
                        val command = remember { ClockCommand(1_435) }
                        ClocklingsTheme {
                            ClockFace(
                                command,
                                Modifier.size(280.dp).testTag("clock"),
                                period = ClockTime.DAY,
                                step = 5,
                                interactive = interactive.value,
                                onChange = { value = it },
                            )
                        }
                    }
                }
            }
        }
        compose.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle {
                viewport.value = cell
                interactive.value = true
                value = 1_435
            }
            val clock = compose.onNodeWithTag("clock")
            clock.assert(isFocusable())
            clock.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            clock.performKeyInput { pressKey(Key.DirectionRight) }
            compose.runOnIdle { assertEquals(0, value) }
            clock.performKeyInput { pressKey(Key.DirectionLeft) }
            compose.runOnIdle { assertEquals(1_435, value) }
            clock.performKeyInput { pressKey(Key.DirectionUp) }
            compose.runOnIdle { assertEquals(0, value) }
            clock.performKeyInput { pressKey(Key.DirectionDown) }
            compose.runOnIdle { assertEquals(1_435, value) }
            compose.runOnIdle { interactive.value = false }
            clock.performKeyInput { pressKey(Key.DirectionRight) }
            compose.runOnIdle { assertEquals(1_435, value) }
        }
    }
}
