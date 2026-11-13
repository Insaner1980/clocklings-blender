package fi.clocklings.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.TaskType
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeViewModel
import fi.clocklings.ui.game.GameViewModel
import fi.clocklings.ui.lesson.LessonViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceFailureTest {
    @get:Rule val main = MainDispatcherRule()

    private val store = FailingWriteStore()
    private val repository = ProgressRepository(store)
    private val services =
        object : AppServices by FakeServices(fullAccess = true) {
            override val repository = this@PersistenceFailureTest.repository
        }

    @Test
    fun visualSettingsFailureKeepsSavedSelectionAndReportsError() =
        runTest(main.dispatcher) {
            val app = AppViewModel(services)
            advanceUntilIdle()
            store.failure = IOException("Synthetic write failure")
            app.personalize { it.copy(companion = CompanionKind.CAT) }
            advanceUntilIdle()
            assertEquals(
                CompanionKind.HUMAN,
                repository.settings
                    .first()
                    .personalization.companion,
            )
            assertTrue(app.settingsAction.value.failed)
            store.failure = null
            app.personalize { it.copy(companion = CompanionKind.CAT) }
            advanceUntilIdle()
            assertEquals(
                CompanionKind.CAT,
                repository.settings
                    .first()
                    .personalization.companion,
            )
        }

    @Test
    fun progressFailureKeepsSavedDataAndReportsError() =
        runTest(main.dispatcher) {
            val app = AppViewModel(services)
            advanceUntilIdle()
            store.failure = IOException("Synthetic write failure")
            app.setName(CompanionKind.CAT, "Mia")
            advanceUntilIdle()
            assertEquals(Progress(), repository.progress.first())
            assertTrue(app.settingsAction.value.failed)
            store.failure = null
            app.setName(CompanionKind.CAT, "Mia")
            advanceUntilIdle()
            assertEquals("Mia", repository.progress.first().nameFor(CompanionKind.CAT))
        }

    @Test
    fun failedGameSaveCanRetryWithoutLosingTheRound() =
        runTest(main.dispatcher) {
            val game = GameViewModel(0, false, services, Random(11))
            store.failure = IOException("Synthetic write failure")
            repeat(10) {
                val state = game.state.value
                if (state.task.type == TaskType.READ) {
                    game.pick(state.task.time)
                } else {
                    game.onClockChange(state.task.time)
                    game.done()
                }
                advanceTimeBy(1_600)
                runCurrent()
            }
            assertNull(game.state.value.finished)
            assertTrue(game.state.value.showNext)
            assertEquals(Progress(), repository.progress.first())
            store.failure = null
            game.next()
            advanceUntilIdle()
            assertEquals(3, repository.progress.first().stars[0])
            assertEquals(
                3,
                game.state.value.finished!!
                    .stars,
            )
        }

    @Test
    fun failedLessonSaveKeepsTheQuizAvailableForRetry() =
        runTest(main.dispatcher) {
            val lesson = LessonViewModel(listOf("viisarit"), null, services, Random(5))
            while (!lesson.state.value.isQuiz) lesson.next()
            advanceUntilIdle()
            val correct =
                lesson.state.value.choices
                    .indexOfFirst { it.correct }
            store.failure = IOException("Synthetic write failure")
            lesson.pick(correct)
            advanceUntilIdle()
            assertFalse(lesson.state.value.quizDone)
            assertTrue(
                repository.progress
                    .first()
                    .lessonsDone
                    .isEmpty(),
            )
            store.failure = null
            lesson.pick(correct)
            advanceUntilIdle()
            assertTrue(lesson.state.value.quizDone)
            assertEquals(setOf("viisarit"), repository.progress.first().lessonsDone)
        }

    @Test
    fun failedChallengeSaveEndsTheTimerWithoutClaimingASavedRecord() =
        runTest(main.dispatcher) {
            val sounds = mutableListOf<Sound>()
            val recording =
                object : AppServices by services {
                    override fun play(
                        sound: Sound,
                        force: Boolean,
                        rate: Float,
                    ) {
                        sounds += sound
                    }
                }
            val challenge = ChallengeViewModel(false, recording, Random(3))
            challenge.start()
            runCurrent()
            challenge.pick(challenge.state.value.question)
            store.failure = IOException("Synthetic write failure")
            advanceTimeBy(60_001)
            runCurrent()
            assertEquals(ChallengePhase.END, challenge.state.value.phase)
            assertEquals(1, challenge.state.value.score)
            assertEquals(0, challenge.state.value.best)
            assertFalse(challenge.state.value.newRecord)
            assertEquals(0, repository.progress.first().challengeBest)
            assertTrue(challenge.state.value.saveFailed)
            store.failure = null
            challenge.retrySave()
            challenge.retrySave()
            assertTrue(challenge.state.value.savePending)
            challenge.start()
            assertEquals(ChallengePhase.END, challenge.state.value.phase)
            advanceUntilIdle()
            assertEquals(1, repository.progress.first().challengeBest)
            assertEquals(1, challenge.state.value.score)
            assertTrue(challenge.state.value.newRecord)
            assertFalse(challenge.state.value.saveFailed)
            assertEquals(listOf(Sound.CHALLENGE_GO, Sound.OK, Sound.CHALLENGE_OVER, Sound.RECORD), sounds)
        }

    @Test
    fun cancelledWritesAreNotReportedAsStorageFailures() =
        runTest(main.dispatcher) {
            val app = AppViewModel(services)
            advanceUntilIdle()
            store.failure = CancellationException("Synthetic cancellation")
            app.personalize { it.copy(companion = CompanionKind.CAT) }
            app.setName(CompanionKind.CAT, "Mia")
            advanceUntilIdle()
            assertFalse(app.settingsAction.value.failed)
            assertEquals(Progress(), repository.progress.first())
            assertEquals(
                CompanionKind.HUMAN,
                repository.settings
                    .first()
                    .personalization.companion,
            )
        }
}

private class FailingWriteStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    override val data = delegate.data
    var failure: Exception? = null

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        failure?.let { throw it }
        return delegate.updateData(transform)
    }
}
