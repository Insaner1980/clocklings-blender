package fi.clocklings

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real clipboard without app fixtures, provider changes or persisted learning data. */
@RunWith(AndroidJUnit4::class)
class ClipboardRestorationDeviceTest {
    @Test
    fun absentClipboardIsAbsentAfterRestoration() =
        withClipboard { clipboard ->
            clipboard.clearPrimaryClip()
            val previous = clipboard.primaryClip
            assertNull(previous)
            clipboard.setPrimaryClip(ClipData.newPlainText("Clocklings clipboard test", "Temporary fixture"))
            assertTrue(clipboard.hasPrimaryClip())

            restoreClipboard(clipboard, previous)

            assertFalse("An absent clipboard must not become an empty text clip", clipboard.hasPrimaryClip())
            assertNull(clipboard.primaryClip)
        }

    @Test
    fun existingClipboardIsRestoredAfterTemporaryReplacement() =
        withClipboard { clipboard ->
            clipboard.setPrimaryClip(ClipData.newPlainText("Clocklings clipboard test", "Original fixture"))
            val previous = clipboard.primaryClip
            assertTrue(clipboard.hasPrimaryClip())
            clipboard.setPrimaryClip(ClipData.newPlainText("Clocklings clipboard test", "Temporary fixture"))

            restoreClipboard(clipboard, previous)

            assertTrue("The original clip must remain present", clipboard.hasPrimaryClip())
            assertTrue(
                "The original synthetic content must be restored",
                clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString() == "Original fixture",
            )
        }

    private fun withClipboard(check: (ClipboardManager) -> Unit) {
        assumeTrue("Clearing an absent clipboard requires API 28", Build.VERSION.SDK_INT >= 28)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            val deadline = SystemClock.uptimeMillis() + FOCUS_TIMEOUT_MS
            var focused = false
            while (!focused && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity { focused = it.hasWindowFocus() }
                if (!focused) SystemClock.sleep(FOCUS_POLL_MS)
            }
            assertTrue("The clipboard test needs an unlocked, focused Activity", focused)
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(ClipboardManager::class.java)
                val original = clipboard.primaryClip
                try {
                    check(clipboard)
                } finally {
                    restoreClipboard(clipboard, original)
                    assertEquals(
                        "Initial clipboard presence must be restored",
                        original != null,
                        clipboard.hasPrimaryClip(),
                    )
                }
            }
        }
    }

    private companion object {
        const val FOCUS_TIMEOUT_MS = 5_000L
        const val FOCUS_POLL_MS = 50L
    }
}
