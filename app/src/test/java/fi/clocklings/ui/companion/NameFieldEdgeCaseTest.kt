package fi.clocklings.ui.companion

import androidx.compose.runtime.mutableStateOf
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
        val name = mutableStateOf("")
        compose.setContent { ClocklingsTheme { NameField(name.value, { name.value = it }) } }
        for (input in listOf("", "A", "a".repeat(15), "a".repeat(16), "a".repeat(17), "ÅÄÖ", "e\u0301", "   ")) {
            compose.onNodeWithContentDescription("Companion name").performTextReplacement(input)
            compose.runOnIdle { assertEquals(input.take(16), name.value) }
        }
    }

    @Test
    fun emojiAtLengthBoundaryDoesNotLeaveAnUnpairedSurrogate() {
        val name = mutableStateOf("")
        compose.setContent { ClocklingsTheme { NameField(name.value, { name.value = it }) } }
        compose.onNodeWithContentDescription("Companion name").performTextReplacement("a".repeat(15) + "\uD83D\uDE00")
        compose.runOnIdle {
            assertFalse("PS-05: name ends with an unpaired high surrogate", name.value.last().isHighSurrogate())
            assertEquals("a".repeat(15), name.value)
        }
        val fittingName = "a".repeat(14) + "\uD83D\uDE00"
        compose.onNodeWithContentDescription("Companion name").performTextReplacement(fittingName)
        compose.runOnIdle { assertEquals(fittingName, name.value) }
    }
}
