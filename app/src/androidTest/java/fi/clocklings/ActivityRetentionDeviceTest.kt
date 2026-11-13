package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.ref.WeakReference

/** Isolate Activity lifetime from ComposeTestRule's observation references. */
class ActivityRetentionDeviceTest {
    @Test
    fun applicationActivitiesAreReleasedAfterClosingTheTask() = verifyReleased(MainActivity::class.java)

    @Test
    fun emptyActivityControlIsReleasedAfterClosingTheTask() = verifyReleased(ComponentActivity::class.java)

    private fun <A : ComponentActivity> verifyReleased(type: Class<A>) {
        val previous = mutableListOf<WeakReference<A>>()
        ActivityScenario.launch(type).use { scenario ->
            repeat(6) {
                scenario.onActivity { activity -> previous += WeakReference(activity) }
                scenario.recreate()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
        }
        repeat(20) {
            Runtime.getRuntime().gc()
            if (previous.all { it.get() == null }) return
            Thread.sleep(250)
        }
        assertTrue(
            "${type.simpleName} retained ${previous.count { it.get() != null }} of 6",
            previous.all {
                it.get() ==
                    null
            },
        )
    }
}
