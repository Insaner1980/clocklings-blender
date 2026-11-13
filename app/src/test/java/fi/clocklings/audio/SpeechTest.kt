package fi.clocklings.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SpeechTest {
    @Test
    fun brazilianSpeechRejectsEuropeanGenericNetworkAndMissingVoices() {
        withSpeech { speech, engine ->
            engine.voices = setOf(voice("european", "pt-PT"), voice("generic", "pt"))
            speech.setLanguage(AppLanguage.PORTUGUESE_BRAZIL)
            assertFalse(speech.available.value)
            speech.speak("ignored")
            assertTrue(engine.spoken.isEmpty())
            engine.voices = setOf(voice("network", "pt-BR", network = true), voice("missing", "pt-BR", missing = true))
            speech.refresh()
            assertFalse(speech.available.value)
            engine.voices = setOf(voice("brazilian", "pt-BR"), voice("european", "pt-PT"))
            speech.refresh()
            assertTrue(speech.available.value)
            assertEquals("brazilian", selectOfflineVoice(engine.voices, Locale.forLanguageTag("pt-BR"))?.name)
            speech.speak("É uma hora")
            assertEquals(listOf("É uma hora"), engine.spoken)
        }
    }

    @Test
    fun queuedSpeechIsDiscardedByStopLanguageRefreshAndShutdown() =
        runTest {
            val cancellations: List<(Speech) -> Unit> =
                listOf(
                    { it.stop() },
                    { it.setLanguage(AppLanguage.FINNISH) },
                    { it.refresh() },
                    { it.shutdown() },
                )
            for (cancel in cancellations) {
                val engine = FakeEngine()
                val speech =
                    Speech(StandardTestDispatcher(testScheduler)) { callback ->
                        callback(TextToSpeech.SUCCESS)
                        engine
                    }
                try {
                    runCurrent()
                    assertTrue(speech.available.value)
                    speech.speak("stale")
                    cancel(speech)
                    runCurrent()
                    assertTrue(engine.spoken.isEmpty())
                    if (engine.shutdowns == 0) {
                        speech.speak("current")
                        runCurrent()
                        assertEquals(listOf("current"), engine.spoken)
                    }
                } finally {
                    speech.shutdown()
                    runCurrent()
                }
            }
        }

    @Test
    fun queuedInitializationUsesLatestLanguageAndDoesNotReplayEarlySpeech() =
        runTest {
            val engine = FakeEngine()
            lateinit var initialize: (Int) -> Unit
            val speech =
                Speech(StandardTestDispatcher(testScheduler)) { callback ->
                    initialize = callback
                    engine
                }
            try {
                runCurrent()
                speech.setLanguage(AppLanguage.FINNISH)
                speech.setLanguage(AppLanguage.SWEDISH)
                speech.speak("before initialization")
                initialize(TextToSpeech.SUCCESS)
                runCurrent()
                assertTrue(speech.available.value)
                assertEquals(listOf("sv"), engine.selectedLanguages)
                assertTrue(engine.spoken.isEmpty())
            } finally {
                speech.shutdown()
                runCurrent()
            }
        }

    @Test
    fun blockedEngineNeverHoldsTheCallingThreadOrItsRequestLock() {
        val caller = Thread.currentThread()
        val calls = ConcurrentLinkedQueue<Thread>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val speaking = CountDownLatch(1)
        val releaseSpeech = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val timedOut = AtomicBoolean(false)
        val engine =
            FakeEngine().apply {
                onCall = { calls += Thread.currentThread() }
                beforeVoices = {
                    entered.countDown()
                    if (!release.await(5, TimeUnit.SECONDS)) timedOut.set(true)
                }
                beforeSpeak = {
                    speaking.countDown()
                    if (!releaseSpeech.await(5, TimeUnit.SECONDS)) timedOut.set(true)
                }
                afterShutdown = { finished.countDown() }
            }
        val speech =
            Speech { callback ->
                calls += Thread.currentThread()
                callback(TextToSpeech.SUCCESS)
                engine
            }
        val scope = ReflectionHelpers.getField<CoroutineScope>(speech, "scope")
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            speech.setLanguage(AppLanguage.FINNISH)
            speech.refresh()
            speech.speak("unavailable")
            speech.stop()
            release.countDown()
            runBlocking { withTimeout(5_000) { speech.available.first { it } } }
            speech.speak("active")
            assertTrue(speaking.await(5, TimeUnit.SECONDS))
            repeat(40) { speech.speak("queued-$it") }
            speech.stop()
            speech.shutdown()
            assertFalse(speech.available.value)
            assertEquals(1L, finished.count)
        } finally {
            release.countDown()
            releaseSpeech.countDown()
            speech.shutdown()
            assertTrue(finished.await(5, TimeUnit.SECONDS))
        }
        assertFalse(timedOut.get())
        assertTrue(calls.isNotEmpty())
        assertTrue(calls.all { it !== caller })
        assertEquals(listOf("active"), engine.spoken)
        awaitShutdown(scope)
    }

    private fun awaitShutdown(scope: CoroutineScope) {
        runBlocking { withTimeout(5_000) { scope.coroutineContext.job.join() } }
        assertFalse(
            "Shutdown must finish the queued jobs",
            scope.coroutineContext.job.children
                .any(),
        )
    }

    @Test
    fun latestLanguageWinsBeforeAsynchronousInitializationWithoutReplayingText() {
        val engine = FakeEngine()
        lateinit var initialize: (Int) -> Unit
        val speech =
            Speech(Dispatchers.Unconfined) { callback ->
                initialize = callback
                engine
            }
        try {
            speech.setLanguage(AppLanguage.FINNISH)
            speech.speak("old Finnish")
            speech.setLanguage(AppLanguage.SWEDISH)
            speech.speak("old Swedish")
            assertFalse(speech.available.value)
            initialize(TextToSpeech.SUCCESS)
            assertTrue(speech.available.value)
            assertEquals(listOf("sv"), engine.selectedLanguages)
            assertTrue(engine.spoken.isEmpty())
            speech.speak("new Swedish")
            assertEquals(listOf("new Swedish"), engine.spoken)
        } finally {
            speech.shutdown()
        }
        assertEquals(1, engine.shutdowns)
    }

    @Test
    fun synchronousInitializationConfiguresAfterEngineAssignment() {
        withSpeech { speech, engine ->
            assertTrue(speech.available.value)
            assertEquals(listOf("en"), engine.selectedLanguages)
            assertEquals(listOf(0.9f), engine.rates)
        }
    }

    @Test
    fun languageSwitchStopsSpeakingAndKeepsUnavailableUntilVoiceAndRateSucceed() {
        withSpeech { speech, engine ->
            speech.speak("English")
            val oldUtterance = engine.utteranceIds.single()
            val stopsBefore = engine.stops
            engine.beforeSetVoice = { assertFalse(speech.available.value) }
            engine.beforeSetRate = { assertFalse(speech.available.value) }
            speech.setLanguage(AppLanguage.FINNISH)
            speech.setLanguage(AppLanguage.SWEDISH)
            assertTrue(speech.available.value)
            assertEquals(stopsBefore + 2, engine.stops)
            assertEquals(listOf("en", "fi", "sv"), engine.selectedLanguages)
            assertEquals(listOf("English"), engine.spoken)
            engine.reportError(oldUtterance)
            assertTrue(speech.available.value)
            speech.speak("Swedish")
            engine.reportError(engine.utteranceIds.last())
            assertFalse(speech.available.value)
        }
    }

    @Test
    fun missingEngineAndFailedInitializationRemainUnavailable() {
        val missing = Speech(Dispatchers.Unconfined) { throw IllegalStateException("No engine") }
        try {
            missing.refresh()
            missing.speak("ignored")
            assertFalse(missing.available.value)
        } finally {
            missing.shutdown()
        }
        val engine = FakeEngine()
        val failed =
            Speech(Dispatchers.Unconfined) { callback ->
                callback(TextToSpeech.ERROR)
                engine
            }
        try {
            failed.setLanguage(AppLanguage.FINNISH)
            failed.refresh()
            failed.speak("ignored")
            assertFalse(failed.available.value)
            assertTrue(engine.selectedLanguages.isEmpty())
            assertTrue(engine.spoken.isEmpty())
        } finally {
            failed.shutdown()
        }
    }

    @Test
    fun absentNetworkMissingDataAndWrongLanguageVoicesCannotEnableSpeech() {
        val rejected =
            listOf(
                emptySet(),
                setOf(voice("network", "sv", network = true)),
                setOf(voice("missing", "sv", missing = true)),
                setOf(voice("wrong", "en")),
            )
        rejected.forEach { voices ->
            withSpeech { speech, engine ->
                engine.voices = voices
                speech.setLanguage(AppLanguage.SWEDISH)
                speech.speak("ignored")
                assertFalse(speech.available.value)
                assertTrue(engine.spoken.isEmpty())
            }
        }
    }

    @Test
    fun voiceRateSpeakAndStopFailuresMakeSpeechUnavailableAndRefreshCanRecover() {
        withSpeech { speech, engine ->
            engine.voiceResult = TextToSpeech.ERROR
            speech.setLanguage(AppLanguage.FINNISH)
            assertFalse(speech.available.value)
            engine.voiceResult = TextToSpeech.SUCCESS
            engine.rateResult = TextToSpeech.ERROR
            speech.refresh()
            assertFalse(speech.available.value)
            engine.rateResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            engine.speakResult = TextToSpeech.ERROR
            speech.speak("failed")
            assertFalse(speech.available.value)
            engine.speakResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            engine.stopResult = TextToSpeech.ERROR
            speech.setLanguage(AppLanguage.SWEDISH)
            assertFalse(speech.available.value)
            engine.stopResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            assertEquals("sv", engine.selectedLanguages.last())
        }
    }

    @Test
    fun refreshReflectsInstalledAndRemovedVoiceDataWithoutAutomaticSpeech() {
        withSpeech { speech, engine ->
            engine.voices = emptySet()
            speech.setLanguage(AppLanguage.SWEDISH)
            assertFalse(speech.available.value)
            engine.voices = setOf(voice("swedish", "sv-FI"))
            speech.refresh()
            assertTrue(speech.available.value)
            engine.voices = emptySet()
            speech.refresh()
            assertFalse(speech.available.value)
            assertTrue(engine.spoken.isEmpty())
        }
    }

    @Test
    fun shutdownIsFinalEvenWhenInitializationFinishesLate() {
        val engine = FakeEngine()
        lateinit var initialize: (Int) -> Unit
        val speech =
            Speech(Dispatchers.Unconfined) { callback ->
                initialize = callback
                engine
            }
        speech.shutdown()
        initialize(TextToSpeech.SUCCESS)
        speech.setLanguage(AppLanguage.FINNISH)
        speech.refresh()
        speech.speak("ignored")
        speech.shutdown()
        assertFalse(speech.available.value)
        assertTrue(engine.selectedLanguages.isEmpty())
        assertTrue(engine.spoken.isEmpty())
        assertEquals(1, engine.shutdowns)
    }

    private fun withSpeech(check: (Speech, FakeEngine) -> Unit) {
        val engine = FakeEngine()
        val speech =
            Speech(Dispatchers.Unconfined) { callback ->
                callback(TextToSpeech.SUCCESS)
                engine
            }
        try {
            check(speech, engine)
        } finally {
            speech.shutdown()
        }
    }

    private class FakeEngine : SpeechEngine {
        var onCall: () -> Unit = {}
        var beforeVoices: () -> Unit = {}
        var beforeSpeak: () -> Unit = {}
        var afterShutdown: () -> Unit = {}
        override var voices = setOf(voice("english", "en-GB"), voice("finnish", "fi-FI"), voice("swedish", "sv-SE"))
            get() {
                onCall()
                beforeVoices()
                return field
            }
        var voiceResult = TextToSpeech.SUCCESS
        var rateResult = TextToSpeech.SUCCESS
        var speakResult = TextToSpeech.SUCCESS
        var stopResult = TextToSpeech.SUCCESS
        var beforeSetVoice: () -> Unit = {}
        var beforeSetRate: () -> Unit = {}
        val selectedLanguages = mutableListOf<String>()
        val rates = mutableListOf<Float>()
        val spoken = mutableListOf<String>()
        val utteranceIds = mutableListOf<String>()
        var stops = 0
        var shutdowns = 0
        private var onError: (String) -> Unit = {}

        override fun setErrorListener(listener: (String) -> Unit) {
            onCall()
            onError = listener
        }

        override fun setVoice(voice: Voice): Int {
            onCall()
            beforeSetVoice()
            selectedLanguages += voice.locale.language
            return voiceResult
        }

        override fun setSpeechRate(rate: Float): Int {
            onCall()
            beforeSetRate()
            rates += rate
            return rateResult
        }

        override fun speak(
            text: String,
            utteranceId: String,
        ): Int {
            onCall()
            beforeSpeak()
            spoken += text
            utteranceIds += utteranceId
            return speakResult
        }

        override fun stop(): Int {
            onCall()
            stops++
            return stopResult
        }

        override fun shutdown() {
            onCall()
            shutdowns++
            afterShutdown()
        }

        fun reportError(utteranceId: String) = onError(utteranceId)
    }

    private companion object {
        fun voice(
            name: String,
            tag: String,
            network: Boolean = false,
            missing: Boolean = false,
        ) = Voice(
            name,
            Locale.forLanguageTag(tag),
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            network,
            if (missing) setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) else emptySet(),
        )
    }
}
