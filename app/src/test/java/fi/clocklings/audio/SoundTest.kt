package fi.clocklings.audio

import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Greeting
import fi.clocklings.domain.Hint
import fi.clocklings.domain.HourPosition
import fi.clocklings.domain.MinutePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundTest {
    @Test
    fun `intro feedback sounds and companion voices are recorded clips`() {
        val recorded = Sound.entries - Sound.POP
        for (sound in recorded) assertNotNull("$sound", sound.clip)
        assertNull(Sound.POP.clip)
    }

    @Test
    fun `every companion has its own voice`() {
        val companions = CompanionKind.entries - CompanionKind.NONE
        val voices = companions.map { companionVoice(it) }
        for (voice in voices) assertTrue("$voice", voice?.voice == true)
        assertEquals(companions.size, voices.toSet().size)
        assertNull(companionVoice(CompanionKind.NONE))
    }

    @Test
    fun `narration lines are voices so that only one speaker talks at a time`() {
        val narration = Sound.entries.filter { it.narration }
        assertEquals(14, narration.size)
        for (sound in narration) assertTrue("$sound", sound.voice)
    }

    @Test
    fun `greetings results and hints map to their narrator lines`() {
        val greetings = Greeting.entries.map(::greetingVoice)
        assertEquals(Greeting.entries.size, greetings.toSet().size)
        assertTrue(Greeting.entries.all { greetingVoice(it).narration })
        assertEquals(Sound.RESULT_3, resultVoice(3))
        assertEquals(Sound.RESULT_2, resultVoice(2))
        assertEquals(Sound.RESULT_1, resultVoice(1))
        assertEquals(Sound.RESULT_1, resultVoice(0))
        val hour = HourPosition.Exact(3)
        val minute = MinutePosition.Top
        assertEquals(Sound.HINT_HOUR, hintVoice(Hint.ReadHour(hour, nearHalf = false)))
        assertEquals(Sound.HINT_HOUR, hintVoice(Hint.SetHour(hour, turnTip = false)))
        assertEquals(Sound.HINT_MINUTE, hintVoice(Hint.ReadMinute(minute, alsoHour = false)))
        assertEquals(Sound.HINT_MINUTE, hintVoice(Hint.SetMinute(minute)))
        assertNull(hintVoice(Hint.HandsRight(0)))
    }
}
