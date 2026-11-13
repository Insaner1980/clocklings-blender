package fi.clocklings.fixture;

import android.content.Intent;
import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.Voice;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Separate-process native TTS fixture; no Kotlin runtime is bundled in the test APK. */
public final class AuditSpeechService extends TextToSpeechService {
    private volatile CountDownLatch pending;

    @Override protected int onIsLanguageAvailable(String language, String country, String variant) {
        return "eng".equals(language) ? TextToSpeech.LANG_COUNTRY_AVAILABLE : TextToSpeech.LANG_NOT_SUPPORTED;
    }

    @Override protected int onLoadLanguage(String language, String country, String variant) {
        return onIsLanguageAvailable(language, country, variant);
    }

    @Override protected String[] onGetLanguage() { return new String[]{"eng", "USA", ""}; }

    @Override public List<Voice> onGetVoices() {
        return Collections.singletonList(new Voice("audit-offline", Locale.US,
                Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false, Collections.emptySet()));
    }

    @Override public String onGetDefaultVoiceNameFor(String language, String country, String variant) {
        return "audit-offline";
    }

    @Override public int onIsValidVoiceName(String name) {
        return "audit-offline".equals(name) ? TextToSpeech.SUCCESS : TextToSpeech.ERROR;
    }

    @Override public int onLoadVoice(String name) { return onIsValidVoiceName(name); }

    @Override protected void onStop() {
        CountDownLatch latch = pending;
        if (latch != null) {
            event("stopped");
            latch.countDown();
        }
    }

    @Override protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        if ("Clocklings stalled request".contentEquals(request.getCharSequenceText())) {
            CountDownLatch latch = new CountDownLatch(1);
            pending = latch;
            event("started");
            try {
                event(latch.await(20, TimeUnit.SECONDS) ? "released" : "timeout");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                event("interrupted");
            } finally {
                pending = null;
                callback.error();
            }
        } else {
            callback.start(16000, AudioFormat.ENCODING_PCM_16BIT, 1);
            byte[] silence = new byte[320];
            callback.audioAvailable(silence, 0, silence.length);
            callback.done();
            event("completed");
        }
    }

    private void event(String value) {
        android.util.Log.i("ClocklingsAuditSpeech", value);
        sendBroadcast(new Intent("fi.clocklings.audit.SPEECH_EVENT")
                .setPackage("fi.clocklings").putExtra("event", value));
    }
}
