package fi.clocklings.ui.clock

import fi.clocklings.domain.Hand
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragTickTest {
    @Test
    fun onlyChangedDragValuesProduceFeedback() =
        runTest {
            val ticks = mutableListOf<DragTick>()
            val state =
                ClockState(0, 720, backgroundScope).apply {
                    step = 1
                    reduceMotion = true
                    onDragStep = { ticks += it }
                }
            state.apply(ClockCommand(4))
            state.nudge(1)
            assertEquals(emptyList<DragTick>(), ticks)
            state.startDrag(Hand.MINUTE, 30f)
            state.dragTo(36f)
            state.dragTo(36f)
            state.dragTo(60f)
            state.endDrag()
            assertEquals(listOf(DragTick.MINUTE, DragTick.MARK), ticks)
        }

    @Test
    fun stationarySnappedValueDoesNotTick() {
        assertNull(dragTick(4, 4))
        assertNull(dragTick(60, 60))
    }

    @Test
    fun minuteChangesTickInBothDirections() {
        assertEquals(DragTick.MINUTE, dragTick(1, 2))
        assertEquals(DragTick.MINUTE, dragTick(4, 3))
        assertEquals(DragTick.MINUTE, dragTick(5, 4))
    }

    @Test
    fun fiveMinuteAndHourMarksUseTheStrongerTick() {
        assertEquals(DragTick.MARK, dragTick(4, 5))
        assertEquals(DragTick.MARK, dragTick(6, 5))
        assertEquals(DragTick.MARK, dragTick(59, 60))
        assertEquals(DragTick.MARK, dragTick(61, 60))
        assertEquals(DragTick.MARK, dragTick(0, 55))
    }

    @Test
    fun twelveAndTwentyFourHourWrapsKeepTheSameTickRules() {
        assertEquals(DragTick.MARK, dragTick(719, 0))
        assertEquals(DragTick.MARK, dragTick(1439, 0))
        assertEquals(DragTick.MINUTE, dragTick(0, 719))
        assertEquals(DragTick.MINUTE, dragTick(0, 1439))
    }
}
