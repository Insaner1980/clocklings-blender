package fi.clocklings.ui

import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Testipalvelut: muistissa oleva tallennus ja äänien kirjaus. */
class FakeServices(
    fullAccess: Boolean,
) : AppServices {
    override val purchases = FakePurchases(fullAccess)
    override val repository = ProgressRepository(InMemoryPreferencesStore())
    override val speechAvailable: StateFlow<Boolean> = MutableStateFlow(false)
    val sounds = mutableListOf<Sound>()
    val spoken = mutableListOf<String>()
    var speechStops = 0
        private set
    var speechLanguage = AppLanguage.ENGLISH
        private set
    var speechRefreshes = 0
        private set

    override fun play(
        sound: Sound,
        force: Boolean,
        rate: Float,
    ) {
        sounds += sound
    }

    override fun speak(text: String) {
        spoken += text
    }

    override fun stopSpeech() {
        speechStops++
    }

    override fun setSpeechLanguage(language: AppLanguage) {
        speechLanguage = language
    }

    override fun refreshSpeech() {
        speechRefreshes++
    }
}
