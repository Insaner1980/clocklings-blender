package fi.clocklings.ui.clock

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Hand
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClockStateOwnershipTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun commandsDragAndPeriodChangesCancelThePreviousAnimationOwner() {
        lateinit var state: ClockState
        var settled = 0
        compose.setContent {
            val scope = rememberCoroutineScope()
            state = remember { ClockState(180, ClockTime.DAY, scope) }
            state.onSettle = { settled++ }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state.apply(ClockCommand(360, 1_000, exact = true)) }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { state.apply(ClockCommand(540, 200, exact = true)) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.runOnIdle {
            assertEquals(540f, state.raw, 0.01f)
            assertEquals(1, settled)
            // An equal target settles immediately without a second animation completion.
            state.apply(ClockCommand(540, 200, exact = true))
            assertEquals(1, settled)
            state.apply(ClockCommand(900, 1_000, exact = true))
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { state.startDrag(Hand.MINUTE, 90f) }
        val dragged = state.raw
        compose.mainClock.advanceTimeBy(1_500)
        compose.runOnIdle {
            assertEquals(dragged, state.raw, 0.01f)
            assertEquals(1, settled)
            state.endDrag()
            state.apply(ClockCommand(1_200, 1_000, exact = true))
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { state.updatePeriod(ClockTime.HALF_DAY) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.runOnIdle {
            assertEquals(ClockTime.HALF_DAY, state.period)
            assertEquals(480, state.value)
            assertEquals(480f, state.raw, 0.01f)
            assertEquals(1, settled)
            state.apply(ClockCommand(300))
            assertEquals(300, state.value)
            assertEquals(300f, state.raw, 0.01f)
        }
    }
}
