package fi.clocklings.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeTimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `polling stops in background and refreshes immediately on return`() {
        val owner = TestOwner()
        var clock = 720
        var reads = 0
        var displayed = -1
        val readTime = {
            reads++
            clock
        }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                displayed = rememberHomeTime(readTime)
            }
        }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitForIdle()
        compose.runOnIdle { clock = 721 }
        compose.mainClock.advanceTimeBy(10_001)
        compose.runOnIdle { assertEquals(721, displayed) }

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitForIdle()
        val readsWhenStopped = reads
        compose.runOnIdle { clock = 735 }
        compose.mainClock.advanceTimeBy(30_001)
        compose.runOnIdle {
            assertEquals(readsWhenStopped, reads)
            assertEquals(721, displayed)
            owner.registry.currentState = Lifecycle.State.STARTED
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(735, displayed)
            owner.registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
        override val lifecycle: Lifecycle
            get() = registry
    }
}
