package fi.clocklings.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.Screen
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.TaskType
import fi.clocklings.ui.game.GameViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StorageReadRecoveryTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = RecoveringReadStore()
    private val repository = ProgressRepository(store)
    private val services =
        object : AppServices by FakeServices(fullAccess = true) {
            override val repository = this@StorageReadRecoveryTest.repository
        }

    @Test
    fun gameResultUsesTheCommittedSnapshotWhenItsOneShotReadFallsBack() =
        runTest(main.dispatcher) {
            repository.updateProgress { it.withLevelResult(0, 2).withLevelResult(1, 3) }
            store.readable.value = false
            val game = GameViewModel(0, false, services)
            repeat(10) {
                val task = game.state.value.task
                if (task.type == TaskType.READ) {
                    game.pick(task.time)
                } else {
                    game.onClockChange(task.time)
                    game.done()
                }
                advanceTimeBy(1_600)
                runCurrent()
            }
            assertEquals(Screen.Result(0, 3, previousBest = 2, starsBefore = 5), game.state.value.finished)
            store.readable.value = true
            assertEquals(listOf(3, 3, 0, 0, 0), repository.progress.first().stars)
        }

    @Test
    fun persistentReadFailureBacksOffItsAttemptsAndLogsThenRecovers() =
        runTest(main.dispatcher) {
            store.readable.value = false
            ShadowLog.clear()
            val app = AppViewModel(services)
            try {
                runCurrent()
                advanceTimeBy(60_000)
                runCurrent()
                val attempts = store.readAttempts
                val logs = ShadowLog.getLogsForTag("ProgressRepository").size
                assertTrue("60 seconds caused $attempts reads and $logs warnings", attempts <= 16 && logs <= 16)
                assertEquals(attempts, logs)
                store.readable.value = true
                repository.updateProgress { it.withLevelResult(0, 3) }
                advanceTimeBy(30_000)
                runCurrent()
                assertEquals(3, app.progress.value!!.totalStars)
                store.readable.value = false
                runCurrent()
                val before = store.readAttempts
                advanceTimeBy(1_000)
                runCurrent()
                assertTrue("Successful reads reset the short retry delay", store.readAttempts > before)
            } finally {
                app.viewModelScope.cancel()
            }
        }

    @Test
    fun startupReadFailureDoesNotDisconnectProgressAndSettings() =
        runTest(main.dispatcher) {
            store.readable.value = false
            val app = AppViewModel(services)
            try {
                runCurrent()
                assertEquals(0, app.progress.value!!.totalStars)
                store.readable.value = true
                repository.updateProgress { it.withLevelResult(0, 3) }
                repository.updateSettings { it.copy(sound = false) }
                advanceTimeBy(2_000)
                runCurrent()
                assertEquals(3, app.progress.value!!.totalStars)
                assertEquals(false, app.settings.value!!.sound)
                repository.updateProgress { it.withLevelResult(1, 2) }
                runCurrent()
                assertEquals(5, app.progress.value!!.totalStars)
            } finally {
                app.viewModelScope.cancel()
            }
        }

    @Test
    fun laterReadFailureKeepsLastKnownDataAndResumesUpdates() =
        runTest(main.dispatcher) {
            repository.updateProgress { it.withLevelResult(0, 3) }
            repository.updateSettings { it.copy(sound = false) }
            val app = AppViewModel(services)
            try {
                runCurrent()
                store.readable.value = false
                runCurrent()
                assertEquals(3, app.progress.value!!.totalStars)
                assertEquals(false, app.settings.value!!.sound)
                store.readable.value = true
                repository.updateProgress { it.withLevelResult(1, 2) }
                advanceTimeBy(2_000)
                runCurrent()
                assertEquals(5, app.progress.value!!.totalStars)
            } finally {
                app.viewModelScope.cancel()
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class RecoveringReadStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    val readable = MutableStateFlow(true)
    var readAttempts = 0
        private set
    override val data =
        flow {
            readAttempts++
            emitAll(
                readable.flatMapLatest { ready ->
                    if (ready) delegate.data else flow { throw IOException("Synthetic read failure") }
                },
            )
        }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        delegate.updateData(transform)
}
