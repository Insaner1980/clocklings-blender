package fi.clocklings.audio

import android.content.Context
import android.media.AudioTrack
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [SfxTest.BlockingSoundPool::class])
class SfxTest {
    @Test
    fun slowResourceOpeningDoesNotBlockConstructionOrPlayAndFailedClipsStaySilent() {
        BlockingSoundPool.caller = Thread.currentThread()
        BlockingSoundPool.entered = CountDownLatch(1)
        BlockingSoundPool.release = CountDownLatch(1)
        BlockingSoundPool.laterEntered = CountDownLatch(1)
        BlockingSoundPool.laterRelease = CountDownLatch(1)
        BlockingSoundPool.loadedOnCaller.set(false)
        BlockingSoundPool.timedOut.set(false)
        val dispatched = AtomicBoolean(false)
        val ioDispatcher =
            object : CoroutineDispatcher() {
                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    dispatched.set(true)
                    Dispatchers.IO.dispatch(context, block)
                }
            }
        val sfx = Sfx(ApplicationProvider.getApplicationContext(), ioDispatcher)
        val pool = ReflectionHelpers.getField<SoundPool>(sfx, "pool")
        val shadow = Shadow.extract<BlockingSoundPool>(pool)
        val scope = ReflectionHelpers.getField<CoroutineScope>(sfx, "scope")
        try {
            assertTrue(BlockingSoundPool.entered.await(5, TimeUnit.SECONDS))
            assertTrue(dispatched.get())
            sfx.play(Sound.OK)
            assertFalse(shadow.wasResourcePlayed(R.raw.correct))
            sfx.play(Sound.INTRO)
            assertFalse(shadow.wasResourcePlayed(R.raw.clocklings))
            BlockingSoundPool.release.countDown()
            assertTrue(BlockingSoundPool.laterEntered.await(5, TimeUnit.SECONDS))
            sfx.play(Sound.OK)
            assertTrue(shadow.wasResourcePlayed(R.raw.correct))
            sfx.play(Sound.BAD)
            assertFalse(shadow.wasResourcePlayed(R.raw.wrong))
            BlockingSoundPool.laterRelease.countDown()
            awaitLoading(scope)
            sfx.play(Sound.BAD)
            assertFalse(shadow.wasResourcePlayed(R.raw.wrong))
            assertFalse(BlockingSoundPool.loadedOnCaller.get())
            assertFalse(BlockingSoundPool.timedOut.get())
            // A missed intro cue must not be replayed late after the greeting may have been skipped.
            assertFalse(shadow.wasResourcePlayed(R.raw.clocklings))
            sfx.play(Sound.INTRO)
            assertTrue(shadow.wasResourcePlayed(R.raw.clocklings))
        } finally {
            BlockingSoundPool.release.countDown()
            BlockingSoundPool.laterRelease.countDown()
            try {
                awaitLoading(scope)
            } finally {
                scope.cancel()
                sfx.stopAll()
                ReflectionHelpers.getField<Map<Sound, AudioTrack>>(sfx, "tracks").values.forEach { it.release() }
                pool.release()
            }
        }
    }

    private fun awaitLoading(scope: CoroutineScope) {
        runBlocking {
            withTimeout(5_000) {
                scope.coroutineContext.job.children
                    .toList()
                    .joinAll()
            }
        }
    }

    @Implements(SoundPool::class)
    class BlockingSoundPool : ShadowSoundPool() {
        @Implementation
        override fun load(
            context: Context,
            resId: Int,
            priority: Int,
        ): Int {
            if (Thread.currentThread() === caller) loadedOnCaller.set(true)
            entered.countDown()
            if (!release.await(5, TimeUnit.SECONDS)) timedOut.set(true)
            if (resId == R.raw.wrong) {
                laterEntered.countDown()
                if (!laterRelease.await(5, TimeUnit.SECONDS)) timedOut.set(true)
            }
            val id = super.load(context, resId, priority)
            notifyResourceLoaded(resId, resId != R.raw.wrong)
            return id
        }

        companion object {
            lateinit var caller: Thread
            lateinit var entered: CountDownLatch
            lateinit var release: CountDownLatch
            lateinit var laterEntered: CountDownLatch
            lateinit var laterRelease: CountDownLatch
            val loadedOnCaller = AtomicBoolean(false)
            val timedOut = AtomicBoolean(false)
        }
    }
}
