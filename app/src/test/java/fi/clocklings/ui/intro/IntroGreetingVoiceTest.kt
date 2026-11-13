package fi.clocklings.ui.intro

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntroGreetingVoiceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun leavingHomeCancelsTheGreetingPermanentlyAcrossReturnAndRestoration() {
        val active = mutableStateOf(true)
        var greetings = 0
        val restoration = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        restoration.setContent { IntroGreetingVoiceEffect(active.value) { greetings++ } }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { assertEquals(0, greetings) }
        compose.runOnIdle { active.value = false }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(0, greetings) }
        compose.runOnIdle { active.value = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(0, greetings) }
    }

    @Test
    fun remainingOnHomeGreetsOnceWithoutReplayAfterRestoration() {
        var greetings = 0
        val restoration = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        restoration.setContent { IntroGreetingVoiceEffect(active = true) { greetings++ } }
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(1, greetings) }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(1, greetings) }
    }
}
