package fi.clocklings.audio

import android.content.Context
import android.content.res.Resources
import android.media.AudioTrack
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.shadows.ShadowSoundPool
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.ConcurrentLinkedQueue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [SfxFailureTest.FailingTrack::class, SfxFailureTest.FailingPool::class])
class SfxFailureTest {
    @Test
    fun thrownPcmWriteReleasesTheTrack() = checkRejectedTrack(WriteFailure.THROW)

    @Test
    fun partialPcmWriteReleasesTheTrack() = checkRejectedTrack(WriteFailure.PARTIAL)

    @Test
    fun negativePcmWriteReleasesTheTrack() = checkRejectedTrack(WriteFailure.ERROR)

    private fun checkRejectedTrack(failure: WriteFailure) {
        FailingTrack.failure = failure
        FailingPool.fail = false
        FailingTrack.written.clear()
        val sfx = Sfx(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined)
        try {
            awaitLoading(sfx)
            assertTrue("The PCM failure must be exercised", FailingTrack.written.isNotEmpty())
            assertTrue("Failed tracks must be released", FailingTrack.written.all { it.released })
            assertTrue("Failed tracks must not be playable", tracks(sfx).isEmpty())
        } finally {
            cleanup(sfx)
        }
    }

    @Test
    fun thrownClipLoadStaysSilentAndDoesNotPreventLaterClipsLoading() = checkRejectedClip(returnZero = false)

    @Test
    fun zeroClipLoadIsRejectedAndDoesNotPreventLaterClipsLoading() = checkRejectedClip(returnZero = true)

    private fun checkRejectedClip(returnZero: Boolean) {
        FailingTrack.failure = WriteFailure.NONE
        FailingTrack.written.clear()
        FailingPool.fail = true
        FailingPool.returnZero = returnZero
        val errors = mutableListOf<Throwable>()
        val thread = Thread.currentThread()
        val previous = thread.uncaughtExceptionHandler
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, error -> errors += error }
        try {
            val sfx = Sfx(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined)
            try {
                awaitLoading(sfx)
                assertTrue("Load failure must not escape the sound scope: $errors", errors.isEmpty())
                val samples = ReflectionHelpers.getField<Map<Sound, *>>(sfx, "samples")
                assertTrue("Failed sample must not be retained", Sound.BAD !in samples)
                val pool = ReflectionHelpers.getField<SoundPool>(sfx, "pool")
                val shadow = Shadow.extract<FailingPool>(pool)
                sfx.play(Sound.BAD)
                assertTrue("The failed clip stays silent", !shadow.wasResourcePlayed(R.raw.wrong))
                sfx.play(Sound.INTRO)
                assertTrue("A later clip still loads and plays", shadow.wasResourcePlayed(R.raw.clocklings))
            } finally {
                cleanup(sfx)
            }
        } finally {
            thread.uncaughtExceptionHandler = previous
            FailingPool.fail = false
            FailingPool.returnZero = false
        }
    }

    private fun awaitLoading(sfx: Sfx) =
        runBlocking {
            val scope = ReflectionHelpers.getField<CoroutineScope>(sfx, "scope")
            withTimeout(5_000) {
                scope.coroutineContext.job.children
                    .toList()
                    .joinAll()
            }
        }

    private fun tracks(sfx: Sfx) = ReflectionHelpers.getField<Map<Sound, AudioTrack>>(sfx, "tracks")

    private fun cleanup(sfx: Sfx) {
        ReflectionHelpers.getField<CoroutineScope>(sfx, "scope").cancel()
        sfx.stopAll()
        FailingTrack.written.forEach { it.track.release() }
        tracks(sfx).values.forEach { it.release() }
        ReflectionHelpers.getField<SoundPool>(sfx, "pool").release()
        FailingTrack.written.clear()
    }

    enum class WriteFailure { NONE, THROW, PARTIAL, ERROR }

    @Implements(AudioTrack::class)
    class FailingTrack : ShadowAudioTrack() {
        @RealObject lateinit var track: AudioTrack
        var released = false

        @Implementation
        fun write(
            data: ShortArray,
            offset: Int,
            size: Int,
        ): Int {
            require(offset >= 0 && size <= data.size - offset)
            written += this
            return when (failure) {
                WriteFailure.NONE -> size
                WriteFailure.THROW -> error("Synthetic PCM write failure")
                WriteFailure.PARTIAL -> size - 1
                WriteFailure.ERROR -> AudioTrack.ERROR_BAD_VALUE
            }
        }

        @Implementation
        fun release() {
            released = true
            Shadow.directlyOn<Unit, AudioTrack>(track, AudioTrack::class.java, "release")
        }

        companion object {
            var failure = WriteFailure.NONE
            val written = ConcurrentLinkedQueue<FailingTrack>()
        }
    }

    @Implements(SoundPool::class)
    class FailingPool : ShadowSoundPool() {
        @Implementation
        override fun load(
            context: Context,
            resId: Int,
            priority: Int,
        ): Int {
            if (fail && resId == R.raw.wrong) {
                if (returnZero) return 0
                throw Resources.NotFoundException("Synthetic clip load failure")
            }
            return super.load(context, resId, priority).also { notifyResourceLoaded(resId, true) }
        }

        companion object {
            var fail = false
            var returnZero = false
        }
    }
}
