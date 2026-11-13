package fi.clocklings.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/** On-demand speech using an installed offline voice in the content language. */
class Speech internal constructor(
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    createEngine: ((Int) -> Unit) -> SpeechEngine,
) {
    constructor(context: Context) : this(createEngine = { onInit -> AndroidSpeechEngine(context, onInit) })

    // Engine calls never suspend and run sequentially. The lock protects only short request-state updates.
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lock = Any()
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()
    private var engine: SpeechEngine? = null
    private var initialization: Int? = null
    private var language = AppLanguage.ENGLISH
    private var closed = false
    private var configuration = 0L
    private var generation = 0L
    private var utteranceNumber = 0L
    private var currentUtterance: String? = null

    init {
        scope.launch {
            engine =
                runCatching {
                    createEngine { status ->
                        scope.launch {
                            initialization = status
                            configure()
                        }
                    }
                }.getOrNull()
            engine?.setErrorListener { utteranceId ->
                scope.launch {
                    if (utteranceId == currentUtterance) _available.value = false
                }
            }
        }
    }

    private fun configure() {
        val activeEngine = engine
        if (activeEngine == null || initialization != TextToSpeech.SUCCESS) return
        val (selectedLanguage, revision) =
            synchronized(lock) {
                if (closed) return
                _available.value = false
                language to configuration
            }
        val available =
            runCatching {
                val locale = Locale.forLanguageTag(selectedLanguage.tag)
                val voice = selectOfflineVoice(activeEngine.voices, locale) ?: return@runCatching false
                activeEngine.setVoice(voice) == TextToSpeech.SUCCESS &&
                    activeEngine.setSpeechRate(RATE) == TextToSpeech.SUCCESS
            }.getOrDefault(false)
        synchronized(lock) {
            if (!closed && revision == configuration) _available.value = available
        }
    }

    fun setLanguage(language: AppLanguage) {
        val revision =
            synchronized(lock) {
                if (closed || this.language == language) return
                this.language = language
                _available.value = false
                generation++
                ++configuration
            }
        reconfigure(revision)
    }

    /** Recheck voice data after returning from the background; never replay old text. */
    fun refresh() {
        val revision =
            synchronized(lock) {
                if (closed) return
                _available.value = false
                generation++
                ++configuration
            }
        reconfigure(revision)
    }

    private fun reconfigure(revision: Long) {
        scope.launch {
            if (synchronized(lock) { closed || revision != configuration }) return@launch
            if (stopCurrent()) configure()
        }
    }

    fun speak(text: String) {
        val request =
            synchronized(lock) {
                if (!_available.value || closed) return
                generation
            }
        scope.launch {
            if (synchronized(lock) { closed || request != generation || !_available.value }) return@launch
            val utteranceId = "clocklings-${++utteranceNumber}"
            currentUtterance = utteranceId
            val result = runCatching { engine?.speak(text, utteranceId) }.getOrNull()
            if (result != TextToSpeech.SUCCESS) _available.value = false
        }
    }

    fun stop() {
        synchronized(lock) {
            if (closed) return
            generation++
        }
        scope.launch {
            if (!stopCurrent()) _available.value = false
        }
    }

    private fun stopCurrent(): Boolean {
        currentUtterance = null
        if (initialization != TextToSpeech.SUCCESS) return true
        return runCatching { engine?.stop() == TextToSpeech.SUCCESS }.getOrDefault(false)
    }

    /** Test-owned/replaced instances are closed explicitly; Activity recreation does not own this. */
    fun shutdown() {
        synchronized(lock) {
            if (closed) return
            closed = true
            generation++
            _available.value = false
        }
        scope.launch {
            stopCurrent()
            runCatching { engine?.shutdown() }
            engine = null
            scope.cancel()
        }
    }

    private companion object {
        const val RATE = 0.9f
    }
}

/** The small engine boundary lets tests control initialization and Android failure results. */
internal interface SpeechEngine {
    val voices: Set<Voice>

    fun setErrorListener(listener: (String) -> Unit)

    fun setVoice(voice: Voice): Int

    fun setSpeechRate(rate: Float): Int

    fun speak(
        text: String,
        utteranceId: String,
    ): Int

    fun stop(): Int

    fun shutdown()
}

private class AndroidSpeechEngine(
    context: Context,
    onInit: (Int) -> Unit,
) : SpeechEngine {
    private val tts = TextToSpeech(context.applicationContext, onInit)
    override val voices: Set<Voice> get() = tts.voices.orEmpty()

    override fun setErrorListener(listener: (String) -> Unit) {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit

                override fun onDone(utteranceId: String) = Unit

                @Deprecated("Required by the platform listener")
                override fun onError(utteranceId: String) = listener(utteranceId)

                override fun onError(
                    utteranceId: String,
                    errorCode: Int,
                ) = listener(utteranceId)
            },
        )
    }

    override fun setVoice(voice: Voice): Int = tts.setVoice(voice)

    override fun setSpeechRate(rate: Float): Int = tts.setSpeechRate(rate)

    override fun speak(
        text: String,
        utteranceId: String,
    ): Int = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    override fun stop(): Int = tts.stop()

    override fun shutdown() = tts.shutdown()
}

/** Prefer the requested locale; Brazilian Portuguese requires a Brazilian offline voice. */
internal fun selectOfflineVoice(
    voices: Set<Voice>,
    locale: Locale,
): Voice? =
    voices
        .filter {
            it.locale.language == locale.language &&
                (locale.toLanguageTag() != "pt-BR" || it.locale.country == "BR") &&
                !it.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }.sortedWith(compareBy<Voice> { it.locale != locale }.thenBy { it.name })
        .firstOrNull()
