package fi.clocklings.ui.companion

import fi.clocklings.domain.Outfit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CompanionMotionTest {
    private val eps = 0.01f

    @Test
    fun `eyes are open most of the time and blink fully now and then`() {
        val samples = (0L until 60_000L step 10L).map { CompanionMotion.blink(it) }
        assertTrue(samples.all { it in 0f..1f })
        val closedShare = samples.count { it > 0f }.toFloat() / samples.size
        assertTrue("eyes closed $closedShare", closedShare < 0.1f)
        assertTrue(samples.any { it > 0.95f })
        assertEquals(0f, CompanionMotion.blink(0L), eps)
    }

    @Test
    fun `blinks happen at least every five seconds`() {
        var lastBlink = 0L
        for (t in 0L until 60_000L step 10L) {
            if (CompanionMotion.blink(t) > 0.5f) lastBlink = t
            assertTrue("no blink by $t", t - lastBlink < 5_000L)
        }
    }

    @Test
    fun `sway is gentle and starts upright`() {
        assertEquals(0f, CompanionMotion.sway(0L), eps)
        assertTrue((0L until 20_000L step 50L).all { abs(CompanionMotion.sway(it)) <= CompanionMotion.SWAY_DEG + eps })
        assertTrue((0L until 5_000L step 50L).any { abs(CompanionMotion.sway(it)) > CompanionMotion.SWAY_DEG / 2 })
    }

    @Test
    fun `idle breathing, tilt and swing stay in range`() {
        for (t in 0L..10_000L step 50) {
            val p = CompanionMotion.idle(t)
            assertTrue(p.breathe in -1f..1f)
            assertTrue(p.headTiltDeg in -3f..0f)
            assertTrue(p.partDeg in 0f..9f)
        }
    }

    @Test
    fun `idle starts at rest`() {
        val p = CompanionMotion.idle(0L)
        assertEquals(0f, p.breathe, 1e-4f)
        assertEquals(0f, p.headTiltDeg, 1e-4f)
        assertEquals(0f, p.partDeg, 1e-4f)
    }

    @Test
    fun `no celebration is done at once`() {
        assertEquals(0L, CompanionMotion.duration(spin = false, jump = false))
        assertTrue(CompanionMotion.celebration(0L, spin = false, jump = false).done)
    }

    @Test
    fun `spin turns all the way around and shows the new outfit after the first quarter`() {
        val spin = CompanionMotion.SPIN_MS
        assertEquals(1f, CompanionMotion.celebration(0L, spin = true, jump = false).pose.turn, eps)
        assertFalse(CompanionMotion.celebration(0L, spin = true, jump = false).showNewOutfit)
        assertEquals(-1f, CompanionMotion.celebration(spin / 2, spin = true, jump = false).pose.turn, eps)
        assertTrue(CompanionMotion.celebration(spin / 4 + 1, spin = true, jump = false).showNewOutfit)
        val end = CompanionMotion.celebration(spin, spin = true, jump = false)
        assertTrue(end.done)
        assertTrue(end.showNewOutfit)
        assertEquals(1f, end.pose.turn, eps)
    }

    @Test
    fun `jump hops twice while waving and lands at rest`() {
        val d = CompanionMotion.JUMP_MS
        val frames = (0L..d step 10L).map { CompanionMotion.celebration(it, spin = false, jump = true).pose }
        assertEquals(0f, frames.first().lift, eps)
        assertTrue(frames.any { it.lift > CompanionMotion.HOP_HEIGHT * 0.9f })
        assertTrue(frames.any { it.armDeg > 60f })
        // Two hops: the lift comes back close to zero halfway.
        assertEquals(0f, CompanionMotion.celebration(d / 2, spin = false, jump = true).pose.lift, 0.5f)
        val end = CompanionMotion.celebration(d, spin = false, jump = true)
        assertTrue(end.done)
        assertEquals(0f, end.pose.lift, eps)
        assertEquals(0f, end.pose.armDeg, eps)
    }

    @Test
    fun `jump flaps the parts twice as wide as the idle swing`() {
        val d = CompanionMotion.JUMP_MS
        val parts =
            (0L until d step 10L).map { CompanionMotion.celebration(it, spin = false, jump = true).pose.partDeg }
        assertTrue(parts.all { it in 0f..2 * CompanionMotion.PART_SWING_DEG + eps })
        assertTrue(parts.any { it > 2 * CompanionMotion.PART_SWING_DEG * 0.9f })
        assertEquals(0f, parts.first(), eps)
    }

    @Test
    fun `spin comes before the jump`() {
        val f = CompanionMotion.celebration(CompanionMotion.SPIN_MS / 2, spin = true, jump = true)
        assertEquals(0f, f.pose.lift, eps)
        assertFalse(f.done)
        assertEquals(
            CompanionMotion.SPIN_MS + CompanionMotion.JUMP_MS,
            CompanionMotion.duration(spin = true, jump = true),
        )
        val hop =
            CompanionMotion.celebration(
                CompanionMotion.SPIN_MS + CompanionMotion.JUMP_MS / 4,
                spin = true,
                jump = true,
            )
        assertTrue(hop.pose.lift > 0f)
        assertEquals(1f, hop.pose.turn, eps)
    }

    @Test
    fun `reduced motion reveals the new outfit immediately`() {
        val c = CompanionCelebration(newOutfit = Outfit.Default, jump = false, delayMs = 1_100L)
        assertTrue(CompanionMotion.revealsNewOutfit(c, elapsedMs = 0L, reduceMotion = true))
        assertFalse(CompanionMotion.revealsNewOutfit(c, elapsedMs = 0L, reduceMotion = false))
        assertTrue(CompanionMotion.revealsNewOutfit(c, elapsedMs = 1_100L + 300L, reduceMotion = false))
    }

    @Test
    fun `no reveal without a new outfit`() {
        assertFalse(CompanionMotion.revealsNewOutfit(CompanionCelebration(null, jump = true), 5_000L, true))
        assertFalse(CompanionMotion.revealsNewOutfit(null, 5_000L, true))
    }
}
