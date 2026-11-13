package fi.clocklings.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthTest {
    private val synthesized = Sound.entries.filter { it.clip == null }

    @Test
    fun `tones match the web version`() {
        assertEquals(listOf(880.0), ToneSynth.tones(Sound.POP).map { it.freq })
    }

    @Test
    fun `recorded clips have no tones`() {
        for (sound in Sound.entries.filter { it.clip != null }) {
            assertTrue("$sound", ToneSynth.tones(sound).isEmpty())
        }
    }

    @Test
    fun `rendered sounds are short and audible`() {
        for (sound in synthesized) {
            val pcm = ToneSynth.render(sound)
            val seconds = pcm.size.toDouble() / ToneSynth.SAMPLE_RATE
            assertTrue("$sound $seconds s", seconds in 0.1..1.0)
            assertTrue("$sound hiljainen", pcm.maxOf { abs(it.toInt()) } > 2_000)
        }
    }

    @Test
    fun `sounds start and end near silence`() {
        for (sound in synthesized) {
            val pcm = ToneSynth.render(sound)
            assertTrue(abs(pcm.first().toInt()) < 200)
            assertTrue(abs(pcm.last().toInt()) < 200)
        }
    }
}
