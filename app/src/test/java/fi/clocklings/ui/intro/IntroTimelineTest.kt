package fi.clocklings.ui.intro

import fi.clocklings.ui.companion.CompanionPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import fi.clocklings.ui.intro.IntroTimeline as T

class IntroTimelineTest {
    private val eps = 0.001f

    @Test
    fun `startup is usable within the short intro budget without waiting for the narrator`() {
        assertTrue(T.END_MS <= 1250L)
        assertTrue(T.STILL_MS <= 400L)
        assertTrue(T.GREETING_VOICE_MS > T.END_MS)
    }

    @Test
    fun `mascot hops waves and blinks but stays still with reduced motion`() {
        val frames = (0L..T.FADE_START_MS step 10L).map { logoPose(it, reducedMotion = false) }
        assertTrue(frames.any { it.lift > 5f })
        assertTrue(frames.any { it.armDeg > 60f })
        assertTrue(frames.any { it.blink > 0.9f })
        assertEquals(0f, logoPose(T.TICK_AT_MS, reducedMotion = false).lift, eps)
        assertTrue(T.TICK_AT_MS < T.FADE_START_MS)
        for (time in 0L..T.END_MS step 10L) {
            assertEquals(CompanionPose(), logoPose(time, reducedMotion = true))
        }
    }

    @Test
    fun `mascot pops in with a small overshoot and settles`() {
        assertEquals(T.MASCOT_START_SCALE, T.mascotScale(0L), eps)
        assertTrue((0L..T.MASCOT_IN_MS step 10L).any { T.mascotScale(it) > 1f })
        assertEquals(1f, T.mascotScale(T.MASCOT_IN_MS), eps)
        assertEquals(1f, T.mascotScale(T.END_MS), eps)
        assertEquals(0f, T.mascotAlpha(0L), eps)
        assertEquals(1f, T.mascotAlpha(T.MASCOT_FADE_MS), eps)
    }

    @Test
    fun `title letters appear one after another and are all shown before the fade`() {
        val title = "Clocklings"
        val firstShown = { i: Int -> (0L..T.END_MS step 5L).first { T.letterAlpha(it, i) > 0f } }
        val starts = title.indices.map(firstShown)
        assertEquals(starts.sorted(), starts)
        assertTrue(starts.zipWithNext().all { (a, b) -> b > a })
        title.indices.forEach {
            assertEquals(1f, T.letterScale(T.FADE_START_MS, it), eps)
            assertEquals(1f, T.letterAlpha(T.FADE_START_MS, it), eps)
        }
        assertEquals(1f, T.greetingAlpha(T.FADE_START_MS), eps)
    }

    @Test
    fun `content fades before the background so the screens never overlap`() {
        assertEquals(1f, T.contentAlpha(T.FADE_START_MS), eps)
        assertEquals(1f, T.overlayAlpha(0L), eps)
        val backgroundStart = T.FADE_START_MS + T.CONTENT_FADE_MS
        assertEquals(0f, T.contentAlpha(backgroundStart), eps)
        assertEquals(1f, T.overlayAlpha(backgroundStart), eps)
        assertEquals(0f, T.overlayAlpha(T.END_MS), eps)
        val fade = (T.FADE_START_MS..T.END_MS step 10L).map(T::overlayAlpha)
        assertEquals(fade.sortedDescending(), fade)
        assertTrue((0L..T.END_MS step 10L).all { T.contentAlpha(it) < 1f || T.overlayAlpha(it) == 1f })
    }

    @Test
    fun `ease out back starts at zero and ends at one`() {
        assertEquals(0f, easeOutBack(0f), eps)
        assertEquals(1f, easeOutBack(1f), eps)
        assertEquals(0f, progress(-5L, 0L, 100L), eps)
        assertEquals(0.5f, progress(150L, 100L, 100L), eps)
        assertEquals(1f, progress(500L, 100L, 100L), eps)
    }
}
