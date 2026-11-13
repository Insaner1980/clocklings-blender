package fi.clocklings

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Real system split-screen lifecycle, with only Clocklings-owned tasks. */
@RunWith(AndroidJUnit4::class)
class MultiWindowDeviceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Test
    fun simultaneousResumedActivitiesKeepApplicationAudioOwnership() {
        var original = compose.activity
        val originalTask = original.taskId
        var second: MainActivity? = null
        try {
            shell("am start -n fi.clocklings/.MainActivity -f 0x18000000")
            compose.waitUntil(10_000) {
                instrumentation.runOnMainSync {
                    second = resumed().filterIsInstance<MainActivity>().firstOrNull { it !== original }
                }
                second != null
            }
            val secondTask = checkNotNull(second).taskId
            shell("dumpsys activity service SystemUIService WMShell splitscreen moveToSideStage $secondTask 1")
            compose.waitUntil(10_000) {
                var both = false
                instrumentation.runOnMainSync {
                    val activities = resumed().filterIsInstance<MainActivity>()
                    activities.firstOrNull { it.taskId == originalTask }?.let { original = it }
                    activities.firstOrNull { it.taskId == secondTask }?.let { second = it }
                    both = original in activities && second in activities
                }
                both
            }
            instrumentation.runOnMainSync {
                assertTrue(original.isInMultiWindowMode)
                assertTrue(checkNotNull(second).isInMultiWindowMode)
                assertEquals(2, resumedAudioOwners(original))
            }
            shell("dumpsys activity service SystemUIService WMShell splitscreen exitSplitScreen ${original.taskId}")
            instrumentation.runOnMainSync { checkNotNull(second).finishAndRemoveTask() }
            compose.waitUntil(10_000) {
                var returned = false
                instrumentation.runOnMainSync {
                    val activities = resumed().filterIsInstance<MainActivity>()
                    activities.firstOrNull { it.taskId == originalTask }?.let { original = it }
                    returned = resumedAudioOwners(original) == 1 && original in activities
                }
                returned
            }
        } finally {
            instrumentation.runOnMainSync { second?.takeUnless { it.isDestroyed }?.finishAndRemoveTask() }
            runCatching {
                shell("dumpsys activity service SystemUIService WMShell splitscreen exitSplitScreen ${original.taskId}")
            }
        }
    }

    private fun resumed() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)

    private fun resumedAudioOwners(activity: MainActivity): Int {
        val services = (activity.application as ClocklingsApplication).container
        val field = services.javaClass.getDeclaredField("resumedActivities").apply { isAccessible = true }
        return field.getInt(services)
    }

    private fun shell(command: String) {
        ParcelFileDescriptor
            .AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("$command 2>&1"),
            ).use {
                val result = it.bufferedReader().readText()
                assertTrue(result, !result.contains("Error") && !result.contains("Exception"))
            }
    }
}
