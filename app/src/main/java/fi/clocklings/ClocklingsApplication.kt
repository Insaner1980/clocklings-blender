package fi.clocklings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import fi.clocklings.audio.Sfx
import fi.clocklings.audio.Sound
import fi.clocklings.audio.Speech
import fi.clocklings.billing.Purchases
import fi.clocklings.billing.UnconfiguredPurchases
import fi.clocklings.billing.createAppPurchases
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "clocklings")

/**
 * Palvelut, joita näkymämallit käyttävät. Testeissä voi antaa oman toteutuksen.
 * Vakaa: toteutukset pitävät vain muuttumattomia viittauksia, ja muuttuva tila kulkee StateFlow-virroilla.
 */
@Stable
interface AppServices {
    val repository: ProgressRepository
    val speechAvailable: StateFlow<Boolean>
    val purchases: Purchases

    /**
     * Plays a sound while the app is resumed and sound effects are enabled.
     * [force] bypasses the sound setting when previewing enabled sound effects.
     * [rate] changes the speed and pitch of a recorded clip (1 = original).
     */
    fun play(
        sound: Sound,
        force: Boolean = false,
        rate: Float = 1f,
    )

    fun speak(text: String)

    fun stopSpeech()

    /** The content language of text-to-speech and of the recorded narration. */
    fun setSpeechLanguage(language: AppLanguage)

    fun refreshSpeech()
}

/** Sovelluksen ainoat pitkäikäiset oliot: tallennus, äänet ja puhe. */
class AppContainer(
    context: Context,
    store: DataStore<Preferences> = context.dataStore,
    override val purchases: Purchases = UnconfiguredPurchases(),
) : AppServices {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val repository = ProgressRepository(store)
    private val sfx = Sfx(context)
    private val soundOn = repository.settings.map { it.sound }.stateIn(scope, SharingStarted.Eagerly, true)
    private val speech = Speech(context)
    override val speechAvailable: StateFlow<Boolean> = speech.available
    private var resumedActivities = 0

    override fun play(
        sound: Sound,
        force: Boolean,
        rate: Float,
    ) {
        if (resumedActivities > 0 && (force || soundOn.value)) sfx.play(sound, rate)
    }

    override fun speak(text: String) {
        if (resumedActivities > 0) speech.speak(text)
    }

    override fun stopSpeech() {
        speech.stop()
        sfx.stopNarration()
    }

    override fun setSpeechLanguage(language: AppLanguage) {
        speech.setLanguage(language)
        sfx.setLanguage(language)
    }

    override fun refreshSpeech() = speech.refresh()

    fun resume() {
        resumedActivities++
        if (resumedActivities == 1) purchases.onForeground()
    }

    /** Äänet ja puhe pysähtyvät, kun sovellus menee taustalle. */
    fun pause() {
        resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
        if (resumedActivities > 0) return
        purchases.onBackground()
        sfx.stopAll()
        speech.stop()
    }
}

open class ClocklingsApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this, preferencesStore(), purchases()) }

    protected open fun purchases(): Purchases = createAppPurchases(this)

    /** Where progress and settings are kept: the app's preferences file. Local JVM tests keep them in memory. */
    protected open fun preferencesStore(): DataStore<Preferences> = dataStore
}
