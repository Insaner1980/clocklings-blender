package fi.clocklings.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfettiStateTest {
    @Test
    fun aLongFrameGapStopsWorkOnceAllParticlesAndFlightsExpire() {
        val state = ConfettiState(1f, enabled = true)
        state.burst(Offset(50f, 100f), count = 20)
        state.flyStar(Offset.Zero, Offset(100f, 100f))
        val remainder = state.advance(600_000_000_000L, 2_000f)
        assertFalse(state.isActive)
        assertTrue("A frame gap must not simulate ${state.frame} empty frames", state.frame <= 200)
        assertTrue(remainder in 0L until 16_666_667L)
        val finished = state.frame
        state.advance(600_000_000_000L, 2_000f)
        assertEquals("An inactive animation needs no catch-up work", finished, state.frame)
    }

    @Test
    fun reducedMotionCreatesNoParticlesOrFlights() {
        val state = ConfettiState(2f, enabled = false)
        state.burst(Offset(10f, 20f))
        state.sparkle(Offset(10f, 20f))
        state.flyStar(Offset.Zero, Offset(100f, 100f))
        assertFalse(state.isActive)
        assertEquals(0L, state.running)
    }

    @Test
    fun burstStartsAtItsAnchorThenMovesAndExpires() {
        val state = ConfettiState(2f, enabled = true)
        state.burst(Offset(50f, 100f), count = 20)
        assertEquals(20, state.particles.size)
        assertTrue(state.particles.all { it.x == 50f && it.y == 100f && it.vy < 0f && it.size > 0f })
        state.step(2_000f)
        assertTrue(state.particles.all { it.y < 100f && it.life == 1 })
        repeat(150) { state.step(2_000f) }
        assertFalse(state.isActive)
        assertEquals(151L, state.frame)
    }

    @Test
    fun flyingStarLandsOnceAndReleasesSparklesAtDestination() {
        val state = ConfettiState(1f, enabled = true)
        val destination = Offset(200f, 300f)
        state.flyStar(Offset(10f, 20f), destination)
        assertEquals(destination, state.flights.single().to)
        repeat(39) { state.step(2_000f) }
        assertEquals(1f, state.flights.single().k)
        state.step(2_000f)
        assertTrue(state.flights.isEmpty())
        assertEquals(10, state.particles.size)
        assertTrue(state.particles.all { it.shape == ConfettiShape.STAR && it.life == 1 })
        repeat(150) { state.step(2_000f) }
        assertFalse(state.isActive)
        assertEquals(2L, state.running)
    }

    @Test
    fun particlesBelowTheViewportAreRemoved() {
        val state = ConfettiState(1f, enabled = true)
        state.sparkle(Offset(0f, 2_000f), count = 3)
        assertEquals(3, state.particles.size)
        state.step(100f)
        assertFalse(state.isActive)
    }

    @Test
    fun `clear removes particles and flights`() {
        val state = ConfettiState(1f, enabled = true)
        state.burst(Offset(50f, 100f), count = 20)
        state.flyStar(Offset(10f, 20f), Offset(200f, 300f))
        assertTrue(state.isActive)
        val framesBefore = state.frame
        state.clear()
        assertFalse(state.isActive)
        assertTrue(state.particles.isEmpty())
        assertTrue(state.flights.isEmpty())
        assertTrue(state.frame > framesBefore)
    }

    @Test
    fun `cleared flight does not sparkle at its destination later`() {
        val state = ConfettiState(1f, enabled = true)
        state.flyStar(Offset(10f, 20f), Offset(200f, 300f))
        repeat(20) { state.step(2_000f) }
        state.clear()
        repeat(50) { state.step(2_000f) }
        assertFalse(state.isActive)
    }
}
