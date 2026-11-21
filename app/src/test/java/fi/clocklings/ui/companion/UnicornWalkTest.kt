package fi.clocklings.ui.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class UnicornWalkTest {
    @Test
    fun fourSeparateFootfallsKeepAtLeastTwoHoovesPlanted() {
        val lifted = mutableListOf<UnicornLeg>()
        val previouslyInAir = mutableSetOf<UnicornLeg>()
        for (time in 1_560L..3_119L step 5L) {
            val stride = UnicornStride(time)
            val inAir =
                UnicornLeg.entries.filterTo(mutableSetOf()) { leg ->
                    stride.leg(leg).sole.y + stride.bodyY < leg.sole.y - .1f
                }
            assertTrue("Fewer than two supporting legs at $time", inAir.size <= 2)
            lifted += inAir - previouslyInAir
            previouslyInAir.clear()
            previouslyInAir.addAll(inAir)
            for (leg in UnicornLeg.entries) {
                val phase = ((time % 1_560L) / 1_560f - leg.beat + 1f) % 1f
                if (phase >= .36f) {
                    val sole = stride.leg(leg).sole
                    assertEquals(leg.sole.x, sole.x, .001f)
                    assertEquals(leg.sole.y, sole.y + stride.bodyY, .001f)
                }
            }
        }
        // The far foreleg finishes the previous cycle's swing at the beginning of this sample.
        assertEquals(
            listOf(
                UnicornLeg.FAR_FORE,
                UnicornLeg.NEAR_HIND,
                UnicornLeg.NEAR_FORE,
                UnicornLeg.FAR_HIND,
                UnicornLeg.FAR_FORE,
            ),
            lifted,
        )
    }

    @Test
    fun foreKneesFlexForwardAndHindHocksFoldBack() {
        for (leg in UnicornLeg.entries) {
            val middle = 1_560L + ((leg.beat + .18f) * 1_560).toLong()
            val stride = UnicornStride(middle)
            val pose = stride.leg(leg)
            assertTrue(pose.sole.y + stride.bodyY < leg.sole.y - 9f)
            val jointTravel = pose.skin(leg.joint).x - leg.joint.x
            assertTrue("Joint bent the wrong way for $leg", if (leg.fore) jointTravel < -3f else jointTravel > 3f)
            assertTrue(abs(pose.skin(leg.sole).x - pose.sole.x) < .001f)
            assertTrue(abs(pose.skin(leg.sole).y - pose.sole.y) < .001f)
        }
    }
}
