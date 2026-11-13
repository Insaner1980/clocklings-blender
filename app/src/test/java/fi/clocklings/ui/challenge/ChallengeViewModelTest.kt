package fi.clocklings.ui.challenge

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.ui.FakeServices
import fi.clocklings.ui.components.OptionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val services = FakeServices(fullAccess = true)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun expiredMonotonicDeadlineRejectsAnswerBeforeTimerDispatch() =
        scope.runTest {
            var now = 0L
            val vm = ChallengeViewModel(false, services, Random(1)) { now }
            vm.start()
            runCurrent()
            now = 60_001_000_000L
            assertEquals(0f, vm.state.value.remainingFraction(now))
            vm.pick(vm.state.value.question)
            val acceptedScore = vm.state.value.score
            advanceUntilIdle()
            assertEquals("CH-03: answer after the visible deadline must not score", 0, acceptedScore)
            assertEquals(
                0,
                services.repository.progress
                    .first()
                    .challengeBest,
            )
        }

    @Test
    fun timerFirstAtDeadlineRejectsAnswerAndPreservesProgress() =
        scope.runTest {
            val before = services.repository.progress.first()
            val vm = ChallengeViewModel(false, services, Random(1)) { dispatcher.scheduler.currentTime * 1_000_000 }
            vm.start()
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            vm.pick(vm.state.value.question)
            assertEquals(ChallengePhase.END, vm.state.value.phase)
            assertEquals(0, vm.state.value.score)
            assertEquals(before, services.repository.progress.first())
        }

    @Test
    fun delayedInitialBestCannotAnnounceALowerScoreAsNewRecord() =
        scope.runTest {
            val backing = InMemoryPreferencesStore()
            val actual = ProgressRepository(backing)
            actual.updateProgress { it.withChallengeScore(10) }
            val releaseRead = CompletableDeferred<Unit>()
            val delayed =
                object : DataStore<Preferences> by backing {
                    override val data =
                        flow {
                            releaseRead.await()
                            emitAll(backing.data)
                        }
                }
            val delayedServices =
                object : AppServices by services {
                    override val repository = ProgressRepository(delayed)
                }
            val vm = ChallengeViewModel(false, delayedServices, Random(1))
            vm.start()
            runCurrent()
            vm.pick(vm.state.value.question)
            advanceTimeBy(60_001)
            runCurrent()
            val ended = vm.state.value
            releaseRead.complete(Unit)
            advanceUntilIdle()
            assertEquals("CH-05: durable best is not reduced", 10, actual.progress.first().challengeBest)
            assertFalse("CH-05: score 1 is not a record over stored best 10", ended.newRecord)
            assertEquals(10, ended.best)
        }

    @Test
    fun repeatedCorrectAnswerAdvancesExactlyAt380Milliseconds() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(1))
            vm.start()
            runCurrent()
            val first = vm.state.value.question
            vm.pick(first)
            vm.pick(first)
            runCurrent()
            advanceTimeBy(379)
            assertEquals(1, vm.state.value.score)
            assertEquals(first, vm.state.value.question)
            advanceTimeBy(1)
            runCurrent()
            assertFalse(vm.state.value.busy)
            assertTrue(first != vm.state.value.question)
            advanceUntilIdle()
        }

    @Test
    fun `starts on the intro with the clock at a quarter to one`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(1))
            assertEquals(ChallengePhase.START, vm.state.value.phase)
            assertEquals(45, vm.state.value.clock.time)
        }

    @Test
    fun `correct answers score and the next question follows`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(1))
            vm.start()
            val first = vm.state.value.question
            vm.pick(first)
            assertEquals(1, vm.state.value.score)
            assertEquals(OptionState.CORRECT, vm.state.value.optionStates[first])
            advanceTimeBy(400)
            assertTrue(vm.state.value.question != first)
            assertTrue(
                vm.state.value.optionStates
                    .isEmpty(),
            )
        }

    @Test
    fun `wrong answer shows the right one and continues`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(2))
            vm.start()
            val s = vm.state.value
            val wrong = s.options.first { it != s.question }
            vm.pick(wrong)
            assertEquals(0, vm.state.value.score)
            assertEquals(OptionState.WRONG, vm.state.value.optionStates[wrong])
            assertEquals(OptionState.CORRECT, vm.state.value.optionStates[s.question])
            assertEquals(Sound.BAD, services.sounds.last())
            advanceTimeBy(1400)
            assertTrue(
                vm.state.value.optionStates
                    .isEmpty(),
            )
        }

    @Test
    fun `time runs out after a minute and the record is saved`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(3))
            vm.start()
            vm.pick(vm.state.value.question)
            runCurrent()
            advanceTimeBy(60_001)
            advanceUntilIdle()
            val s = vm.state.value
            assertEquals(ChallengePhase.END, s.phase)
            assertEquals(1, s.score)
            assertTrue(s.newRecord)
            assertEquals(Sound.CHALLENGE_GO, services.sounds.first())
            assertEquals(listOf(Sound.CHALLENGE_OVER, Sound.RECORD), services.sounds.takeLast(2))
            assertEquals(
                1,
                services.repository.progress
                    .first()
                    .challengeBest,
            )
        }

    @Test
    fun `displayed countdown uses the retained deadline and only a new run resets it`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(3)) { dispatcher.scheduler.currentTime * 1_000_000 }
            vm.start()
            runCurrent()
            val deadline = vm.state.value.deadlineNanos
            advanceTimeBy(20_000)
            assertEquals(deadline, vm.state.value.deadlineNanos)
            assertEquals(
                2f / 3f,
                vm.state.value.remainingFraction(dispatcher.scheduler.currentTime * 1_000_000),
                0.001f,
            )
            val recreatedDisplay = vm.state.value
            assertEquals(2f / 3f, recreatedDisplay.remainingFraction(20_000_000_000), 0.001f)
            advanceTimeBy(40_001)
            runCurrent()
            assertEquals(ChallengePhase.END, vm.state.value.phase)
            assertEquals(0f, vm.state.value.remainingFraction(60_001_000_000))
            vm.start()
            assertEquals(2, vm.state.value.runId)
            assertEquals(1f, vm.state.value.remainingFraction(dispatcher.scheduler.currentTime * 1_000_000))
        }

    @Test
    fun `the fanfare follows the narrator and a quick restart silences it`() =
        scope.runTest {
            val vm = ChallengeViewModel(false, services, Random(4))
            vm.start()
            advanceTimeBy(60_001)
            runCurrent()
            assertEquals(ChallengePhase.END, vm.state.value.phase)
            assertEquals(Sound.CHALLENGE_OVER, services.sounds.last())
            advanceTimeBy(1_501)
            assertEquals(Sound.WIN, services.sounds.last())
            vm.start()
            advanceTimeBy(60_001)
            runCurrent()
            vm.start()
            advanceTimeBy(2_000)
            assertEquals(Sound.CHALLENGE_GO, services.sounds.last())
            assertEquals(1, services.sounds.count { it == Sound.WIN })
        }
}
