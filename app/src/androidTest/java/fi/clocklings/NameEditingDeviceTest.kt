package fi.clocklings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import android.view.WindowInsets
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Existing native field editing through real Android key chords and clipboard values. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NameEditingDeviceTest : InputDeviceTest() {
    @Test
    fun realClipboardCutCopyAndUndoRedoStayInsideTheNameField() {
        click(action("Outfits"))
        val field = compose.onNode(hasSetTextAction())
        field.scrollToIfScrollable().performTouchInput { click() }
        compose.waitUntil(5_000) { imeVisible() }
        waitForImeLayout(field)
        val clipboard = compose.activity.getSystemService(ClipboardManager::class.java)
        var previous: ClipData? = null
        instrumentation.runOnMainSync { previous = clipboard.primaryClip }
        try {
            instrumentation.sendStringSync("CutCopy")
            waitForNameField("CutCopy")
            controlKey(KeyEvent.KEYCODE_A)
            compose.waitUntil(5_000) {
                val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
                selection.min == 0 && selection.max == "CutCopy".length
            }
            controlKey(KeyEvent.KEYCODE_C)
            assertClipboard(clipboard)
            controlKey(KeyEvent.KEYCODE_X)
            waitForNameField("")
            assertClipboard(clipboard)
            controlKey(KeyEvent.KEYCODE_Z)
            waitForNameField("CutCopy")
            controlKey(KeyEvent.KEYCODE_Z, shift = true)
            waitForNameField("")
            controlKey(KeyEvent.KEYCODE_V)
            waitForNameField("CutCopy")
            key(KeyEvent.KEYCODE_BACK)
            runBlocking {
                withTimeout(5_000) {
                    environment.repository.progress.first { it.nameFor(CompanionKind.HUMAN) == "CutCopy" }
                }
            }
            key(KeyEvent.KEYCODE_BACK)
            waitFor(hasText("Clocklings"))
            click(hasContentDescription("My Companion", substring = true) and hasClickAction())
            click(action("Choose an outfit"))
            waitForNameField("CutCopy")
        } finally {
            instrumentation.runOnMainSync {
                previous?.let(clipboard::setPrimaryClip) ?: clipboard.clearPrimaryClip()
            }
        }
    }

    private fun waitForNameField(expected: String) {
        try {
            compose.waitUntil(5_000) { fieldText() == expected }
        } catch (error: Throwable) {
            throw AssertionError("Expected field '$expected', actual '${fieldText()}'", error)
        }
    }

    private fun fieldText(): String =
        compose
            .onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    private fun assertClipboard(clipboard: ClipboardManager) {
        compose.waitUntil(5_000) {
            var text: String? = null
            instrumentation.runOnMainSync {
                text =
                    clipboard.primaryClip
                        ?.getItemAt(0)
                        ?.text
                        ?.toString()
            }
            text == "CutCopy"
        }
        instrumentation.runOnMainSync {
            assertEquals(
                "CutCopy",
                clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString(),
            )
        }
    }

    override fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    private fun controlKey(
        code: Int,
        shift: Boolean = false,
    ) {
        val modifiers =
            if (shift) {
                "${KeyEvent.KEYCODE_CTRL_LEFT} ${KeyEvent.KEYCODE_SHIFT_LEFT}"
            } else {
                "${KeyEvent.KEYCODE_CTRL_LEFT}"
            }
        val command = "input keycombination $modifiers $code"
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.readBytes()
        }
        compose.waitForIdle()
    }

    private fun imeVisible(): Boolean {
        var visible = false
        instrumentation.runOnMainSync {
            visible = compose.activity.window.decorView.rootWindowInsets
                ?.isVisible(WindowInsets.Type.ime()) == true
        }
        return visible
    }

    private fun action(label: String) =
        (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction()
}
