package fi.clocklings.audio

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.LocaleList
import android.util.Log
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/**
 * Sound effects. Synthesized PCM is rendered once on a background thread and played with
 * AudioTrack (MODE_STATIC); recorded clips ([Sound.clip]) are decoded and played by a SoundPool.
 * Narration clips ([Sound.narration]) are loaded in the language given to [setLanguage].
 * The sound on/off setting is checked in AppContainer.
 */
class Sfx(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = mutableMapOf<Sound, AudioTrack>()
    private val attributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attributes)
            .build()

    /** Sample ids whose decoding has finished; loading is asynchronous. */
    private val loaded = mutableSetOf<Int>()

    /** The latest stream of each clip, so a replay restarts it like an AudioTrack. */
    private val streams = mutableMapOf<Sound, Int>()

    private data class Sample(
        val id: Int,
        val language: AppLanguage?,
    )

    @Volatile private var samples: Map<Sound, Sample> = emptyMap()

    /** Serializes loading, so the shared and the narration loaders never overwrite each other. */
    private val loading = Mutex()

    @Volatile private var language: AppLanguage? = null

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loaded) { loaded += sampleId }
            } else {
                Log.w(TAG, "Äänitiedostoa $sampleId ei voitu ladata ($status)")
            }
        }
        scope.launch(ioDispatcher) {
            loading.withLock { Sound.entries.filterNot { it.narration }.forEach { load(context, it) } }
        }
        scope.launch {
            for (sound in Sound.entries.filter { it.clip == null }) {
                runCatching { createTrack(ToneSynth.render(sound)) }
                    .onSuccess { synchronized(tracks) { tracks[sound] = it } }
                    .onFailure { Log.w(TAG, "Ääntä $sound ei voitu luoda", it) }
            }
        }
    }

    /** Opens [sound]'s clip from [resources]; the APK descriptor opens synchronously, decoding is asynchronous. */
    private fun load(
        resources: Context,
        sound: Sound,
        language: AppLanguage? = null,
    ) {
        val clip = sound.clip ?: return
        runCatching { pool.load(resources, clip, 1).also { check(it != 0) { "SoundPool rejected clip" } } }
            .onSuccess { samples = samples + (sound to Sample(it, language)) }
            .onFailure { Log.w(TAG, "Could not load sound $sound", it) }
    }

    /**
     * Loads the narration recorded in [language], replacing the previous language's clips. Until the
     * new clips have loaded, narration stays silent rather than speaking another language.
     */
    fun setLanguage(language: AppLanguage) {
        synchronized(streams) {
            if (this.language == language) return
            this.language = language
            stopNarration()
        }
        scope.launch(ioDispatcher) {
            loading.withLock {
                // A later choice supersedes this one before any work is wasted on it.
                if (this@Sfx.language != language) return@withLock
                val previous = samples.filterKeys { it.narration }
                samples = samples - previous.keys
                previous.values.forEach { sample ->
                    pool.unload(sample.id)
                    synchronized(loaded) { loaded -= sample.id }
                }
                val localized = localizedContext(language)
                Sound.entries.filter { it.narration }.forEach { load(localized, it, language) }
            }
        }
    }

    /** Resolves resources in [language], so the locale-qualified raw folders supply its recordings. */
    private fun localizedContext(language: AppLanguage): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
        return context.createConfigurationContext(configuration)
    }

    private fun createTrack(pcm: ShortArray): AudioTrack {
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        var loaded = false
        try {
            loaded = track.write(pcm, 0, pcm.size) == pcm.size
            check(loaded) { "Incomplete sound buffer" }
            return track
        } finally {
            if (!loaded) track.release()
        }
    }

    /** Plays [sound]; a recorded clip plays at [rate] (1 = original pitch), a synthesized tone ignores it. */
    fun play(
        sound: Sound,
        rate: Float = NORMAL_RATE,
    ) {
        if (sound.clip != null) playClip(sound, rate) else playTone(sound)
    }

    private fun playTone(sound: Sound) {
        val track = synchronized(tracks) { tracks[sound] } ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }.onFailure { Log.w(TAG, "Ääntä $sound ei voitu soittaa", it) }
    }

    private fun playClip(
        sound: Sound,
        rate: Float,
    ) {
        synchronized(streams) {
            val sample = samples[sound] ?: return
            if (
                (sound.narration && sample.language != language) ||
                !synchronized(loaded) { sample.id in loaded }
            ) {
                return
            }
            // A new voice cuts off the previous one, e.g. while browsing companions.
            streams.filterKeys { it == sound || (sound.voice && it.voice) }.forEach { (playing, stream) ->
                pool.stop(stream)
                streams -= playing
            }
            val stream = pool.play(sample.id, FULL_VOLUME, FULL_VOLUME, 0, NO_LOOP, rate)
            if (stream == 0) Log.w(TAG, "Ääntä $sound ei voitu soittaa") else streams[sound] = stream
        }
    }

    /** Stops screen-specific narration while preserving companion voices and other effects. */
    fun stopNarration() {
        synchronized(streams) {
            streams.filterKeys { it.narration }.forEach { (sound, stream) ->
                pool.stop(stream)
                streams -= sound
            }
        }
    }

    /** Pysäyttää kaikki äänet (kutsutaan onPause-vaiheessa). */
    fun stopAll() {
        synchronized(tracks) { tracks.values.toList() }.forEach { runCatching { it.stop() } }
        synchronized(streams) {
            streams.values.forEach(pool::stop)
            streams.clear()
        }
    }

    private companion object {
        const val TAG = "Sfx"
        const val MAX_STREAMS = 4
        const val FULL_VOLUME = 1f
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f
    }
}
