package fi.clocklings.ui.companion

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameFieldEdgeCaseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun ordinaryNameBoundariesKeepTheEstablishedSixteenUnitLimit() {
        val name = TextFieldState()
        compose.setContent { ClocklingsTheme { NameField(name) } }
        for (input in listOf("", "A", "a".repeat(15), "a".repeat(16), "a".repeat(17), "ÅÄÖ", "e\u0301", "   ")) {
            compose.onNodeWithContentDescription("Companion name").performTextReplacement(input)
            compose.runOnIdle { assertEquals(input.take(16), name.text.toString()) }
        }
    }

    @Test
    fun emojiAtLengthBoundaryDoesNotLeaveAnUnpairedSurrogate() {
        val name = TextFieldState()
        compose.setContent { ClocklingsTheme { NameField(name) } }
        compose.onNodeWithContentDescription("Companion name").performTextReplacement("a".repeat(15) + "\uD83D\uDE00")
        compose.runOnIdle {
            assertFalse("PS-05: name ends with an unpaired high surrogate", name.text.last().isHighSurrogate())
            assertEquals("a".repeat(15), name.text.toString())
        }
        val fittingName = "a".repeat(14) + "\uD83D\uDE00"
        compose.onNodeWithContentDescription("Companion name").performTextReplacement(fittingName)
        compose.runOnIdle { assertEquals(fittingName, name.text.toString()) }
    }
}
