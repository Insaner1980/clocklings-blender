package fi.clocklings.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OfflineVoiceTest {
    @Test
    fun rejectsNetworkMissingDataAndWrongLanguage() {
        val voices =
            setOf(
                voice("network", Locale.ENGLISH, network = true),
                voice("missing", Locale.ENGLISH, features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)),
                voice("finnish", Locale.forLanguageTag("fi-FI")),
            )
        assertNull(selectOfflineVoice(voices, Locale.ENGLISH))
        assertNull(selectOfflineVoice(emptySet(), Locale.ENGLISH))
    }

    @Test
    fun choosesOnlyEligibleVoiceAndPrefersRequestedRegion() {
        val american = voice("american", Locale.US)
        val british = voice("british", Locale.UK)
        val voices = setOf(american, british, voice("network", Locale.UK, network = true))
        assertEquals(british, selectOfflineVoice(voices, Locale.UK))
        assertEquals(american, selectOfflineVoice(voices, Locale.ENGLISH))
    }

    @Test
    fun acceptsBothSwedishRegionsWithoutAnotherContentVariant() {
        for (tag in listOf("sv-SE", "sv-FI")) {
            val swedish = voice(tag, Locale.forLanguageTag(tag))
            assertEquals(swedish, selectOfflineVoice(setOf(swedish), Locale.forLanguageTag("sv")))
        }
    }

    private fun voice(
        name: String,
        locale: Locale,
        network: Boolean = false,
        features: Set<String> = emptySet(),
    ) = Voice(name, locale, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, network, features)
}
