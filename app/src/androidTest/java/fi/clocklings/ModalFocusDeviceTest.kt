package fi.clocklings

import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/** Input-focus containment/return and rendered indication in the existing reset modal. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ModalFocusDeviceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Test
    fun resetModalContainsKeyboardFocusAndReturnsItWithoutResetting() {
        waitFor(hasText("Clocklings"))
        activate(hasContentDescription("Settings") and hasClickAction())
        val trigger = hasText("Reset progress") and hasClickAction()
        activate(trigger)
        waitFor(hasText("Reset learning progress?"))
        val cancel = hasText("Cancel") and hasClickAction()
        val reset = hasText("Reset progress") and hasClickAction() and hasAnyAncestor(isDialog())
        focus(cancel)
        key(KeyEvent.KEYCODE_TAB)
        compose.onNode(reset).assertIsFocused().assertIsDisplayed()
        val before = compose.onNode(cancel).captureToImage()
        val beforePixels = IntArray(before.width * before.height).also { before.readPixels(it) }
        key(KeyEvent.KEYCODE_TAB)
        val node = compose.onNode(cancel).assertIsFocused().assertIsDisplayed()
        val after = node.captureToImage()
        val afterPixels = IntArray(after.width * after.height).also { after.readPixels(it) }
        assertEquals(beforePixels.size, afterPixels.size)
        val changed = beforePixels.indices.count { beforePixels[it] != afterPixels[it] }
        assertTrue("Cancel rendered focus changes pixels: $changed", changed > 20)
        val directory =
            File(checkNotNull(compose.activity.getExternalFilesDir(null)), "audit-followup-focus").apply {
                mkdirs()
            }
        val cell = InstrumentationRegistry.getArguments().getString("cell", "unknown")
        File(directory, "$cell-cancel.png").outputStream().use {
            after.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        repeat(2) {
            key(KeyEvent.KEYCODE_TAB)
            compose.onNode(reset).assertIsFocused().assertIsDisplayed()
            key(KeyEvent.KEYCODE_TAB)
            compose.onNode(cancel).assertIsFocused().assertIsDisplayed()
        }
        key(KeyEvent.KEYCODE_TAB, shift = true)
        compose.onNode(reset).assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_TAB, shift = true)
        compose.onNode(cancel).assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_ENTER)
        compose.onNode(hasText("Reset learning progress?")).assertDoesNotExist()
        compose.onNode(trigger).assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_ENTER)
        waitFor(hasText("Reset learning progress?"))
        key(KeyEvent.KEYCODE_BACK)
        compose.onNode(hasText("Reset learning progress?")).assertDoesNotExist()
        compose.onNode(trigger).assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_ENTER)
        waitFor(hasText("Reset learning progress?"))
        key(KeyEvent.KEYCODE_ESCAPE)
        compose.onNode(hasText("Reset learning progress?")).assertDoesNotExist()
        compose.onNode(trigger).assertIsFocused().assertIsDisplayed()
        reportFocus(changed)
    }

    private fun reportFocus(changed: Int) {
        instrumentation.sendStatus(
            2,
            Bundle().apply {
                putString(
                    "stream",
                    "Modal Tab/Shift+Tab containment, Cancel/Back focus return; pixels changed=$changed\n",
                )
            },
        )
    }

    private fun focus(matcher: SemanticsMatcher) = compose.focusWithTab(matcher, { key(it) }, attempts = 40)

    private fun activate(matcher: SemanticsMatcher) {
        waitFor(matcher)
        focus(matcher)
        compose.onNode(matcher).assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_ENTER)
    }

    private fun key(
        code: Int,
        shift: Boolean = false,
    ) {
        val modifiers = if (shift) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        instrumentation.sendKeySync(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, modifiers))
        instrumentation.sendKeySync(KeyEvent(0, 0, KeyEvent.ACTION_UP, code, 0, modifiers))
        compose.waitForIdle()
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(10_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }
}
