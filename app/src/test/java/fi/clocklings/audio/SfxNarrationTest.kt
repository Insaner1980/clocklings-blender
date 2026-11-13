package fi.clocklings.audio

import android.content.Context
import android.media.AudioTrack
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSoundPool
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [SfxNarrationTest.ReadyPool::class])
class SfxNarrationTest {
    @Test
    fun languageChangeBlocksOldNarrationBeforeTheBackgroundLoaderRuns() {
        val scheduler = TestCoroutineScheduler()
        val sfx = Sfx(ApplicationProvider.getApplicationContext(), StandardTestDispatcher(scheduler))
        val pool = ReflectionHelpers.getField<SoundPool>(sfx, "pool")
        val shadow = Shadow.extract<ReadyPool>(pool)
        val scope = ReflectionHelpers.getField<CoroutineScope>(sfx, "scope")
        try {
            sfx.setLanguage(AppLanguage.ENGLISH)
            scheduler.runCurrent()
            sfx.play(Sound.QUESTION_READ)
            assertTrue(shadow.wasResourcePlayed(R.raw.voice_question_read))
            shadow.clearPlayed()

            sfx.setLanguage(AppLanguage.FINNISH)
            sfx.play(Sound.QUESTION_READ)
            assertFalse(
                "Old English sample must be unavailable immediately",
                shadow.wasResourcePlayed(R.raw.voice_question_read),
            )
            sfx.play(Sound.OK)
            assertTrue("Shared effects remain available", shadow.wasResourcePlayed(R.raw.correct))
            scheduler.runCurrent()
            sfx.play(Sound.QUESTION_READ)
            assertEquals("fi", shadow.playedLanguage())

            shadow.clearPlayed()
            sfx.setLanguage(AppLanguage.ENGLISH)
            sfx.setLanguage(AppLanguage.SWEDISH)
            sfx.play(Sound.QUESTION_READ)
            assertFalse(shadow.wasResourcePlayed(R.raw.voice_question_read))
            scheduler.runCurrent()
            sfx.play(Sound.QUESTION_READ)
            assertEquals("sv", shadow.playedLanguage())
        } finally {
            scope.cancel()
            scheduler.runCurrent()
            runBlocking { scope.coroutineContext.job.join() }
            sfx.stopAll()
            ReflectionHelpers.getField<Map<Sound, AudioTrack>>(sfx, "tracks").values.forEach { it.release() }
            pool.release()
        }
    }

    @Implements(SoundPool::class)
    class ReadyPool : ShadowSoundPool() {
        private val languages = mutableMapOf<Int, String>()

        @Implementation
        override fun load(
            context: Context,
            resId: Int,
            priority: Int,
        ): Int {
            val id = super.load(context, resId, priority)
            languages[id] =
                context.resources.configuration.locales[0]
                    .language
            notifyResourceLoaded(resId, true)
            return id
        }

        fun playedLanguage(): String? = languages[getResourcePlaybacks(R.raw.voice_question_read).last().soundId]
    }
}
