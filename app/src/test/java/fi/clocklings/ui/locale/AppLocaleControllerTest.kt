package fi.clocklings.ui.locale

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppLocaleControllerTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun legacySelectionPersistsAndResetKeepsIt() =
        runTest(main.dispatcher) {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            val platform = FakeLocalePlatform(false)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            assertNull(controller.selection.value.choice)
            repository.updateProgress { it.withLevelResult(0, 3) }
            repository.updateSettings { it.copy(h24 = true, sound = false) }
            controller.select(AppLanguage.FINNISH)
            advanceUntilIdle()
            assertEquals("fi", repository.legacyLanguage.first())
            val restarted = AppLocaleController(repository, platform)
            advanceUntilIdle()
            assertEquals(AppLanguage.FINNISH, restarted.selection.value.choice)
            repository.resetProgress()
            assertEquals(Progress(), repository.progress.first())
            assertEquals("fi", repository.legacyLanguage.first())
            assertTrue(repository.settings.first().h24)
            assertFalse(repository.settings.first().sound)
            restarted.select(null)
            advanceUntilIdle()
            assertNull(repository.legacyLanguage.first())
            assertEquals(0, platform.writes)
        }

    @Test
    fun failedLegacyWriteKeepsCommittedChoiceAndProgress() =
        runTest(main.dispatcher) {
            val store = FailingPreferencesStore()
            val repository = ProgressRepository(store)
            repository.setLegacyLanguage("sv")
            repository.updateProgress { it.withLevelResult(0, 2) }
            val before = repository.progress.first()
            val controller = AppLocaleController(repository, FakeLocalePlatform(false))
            advanceUntilIdle()
            store.fail = true
            controller.select(AppLanguage.FINNISH)
            advanceUntilIdle()
            assertTrue(controller.selection.value.failed)
            assertEquals(AppLanguage.SWEDISH, controller.selection.value.choice)
            assertEquals("sv", repository.legacyLanguage.first())
            assertEquals(before, repository.progress.first())
            store.fail = false
            controller.select(AppLanguage.FINNISH)
            advanceUntilIdle()
            assertFalse(controller.selection.value.failed)
            assertEquals("fi", repository.legacyLanguage.first())
        }

    @Test
    fun handoffImportsOnceAndSystemChoiceWinsAfterwards() =
        runTest(main.dispatcher) {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            repository.setLegacyLanguage("fi")
            val platform = FakeLocalePlatform(true)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            assertEquals(AppLanguage.FINNISH, platform.choice)
            assertNull(repository.legacyLanguage.first())
            assertEquals(1, platform.writes)
            platform.choice = AppLanguage.SWEDISH
            controller.refresh()
            advanceUntilIdle()
            assertEquals(AppLanguage.SWEDISH, controller.selection.value.choice)
            controller.select(null)
            advanceUntilIdle()
            assertNull(platform.choice)
            controller.refresh()
            advanceUntilIdle()
            assertNull(controller.selection.value.choice)
            assertNull(repository.legacyLanguage.first())
            assertEquals(2, platform.writes)
        }

    @Test
    fun handoffReadFailureRetainsLegacyLanguageUntilSuccessfulImport() =
        runTest(main.dispatcher) {
            val store = FailingPreferencesStore()
            val repository = ProgressRepository(store)
            repository.setLegacyLanguage("fi")
            store.failRead = true
            val platform = FakeLocalePlatform(true)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            val readFailed = controller.selection.value.failed
            assertFalse(controller.selection.value.ready)
            assertNull(platform.choice)
            assertEquals(0, platform.writes)

            store.failRead = false
            assertEquals("fi", repository.legacyLanguage.first())
            controller.refresh()
            advanceUntilIdle()

            assertEquals(AppLanguage.FINNISH, platform.choice)
            assertEquals(AppLanguage.FINNISH, controller.selection.value.choice)
            assertNull(repository.legacyLanguage.first())
            assertEquals(1, platform.writes)
            assertTrue(readFailed)
            assertFalse(controller.selection.value.failed)
        }

    @Test
    fun existingPlatformChoiceAllowsStartupAndCleanupRetryAfterReadFailure() =
        runTest(main.dispatcher) {
            for (legacy in listOf(null, "fi")) {
                val store = FailingPreferencesStore()
                val repository = ProgressRepository(store)
                repository.setLegacyLanguage(legacy)
                store.failRead = true
                val platform = FakeLocalePlatform(true, AppLanguage.SWEDISH)
                val controller = AppLocaleController(repository, platform)
                advanceUntilIdle()

                assertTrue(controller.selection.value.ready)
                assertTrue(controller.selection.value.failed)
                assertFalse(controller.selection.value.pending)
                assertEquals(AppLanguage.SWEDISH, controller.selection.value.choice)
                store.failRead = false
                assertEquals(legacy, repository.legacyLanguage.first())

                store.failRead = true
                platform.choice = null
                controller.refresh()
                advanceUntilIdle()
                assertTrue(controller.selection.value.ready)
                assertTrue(controller.selection.value.failed)
                assertNull(controller.selection.value.choice)

                store.failRead = false
                controller.refresh()
                advanceUntilIdle()
                assertFalse(controller.selection.value.failed)
                assertNull(controller.selection.value.choice)
                assertNull(repository.legacyLanguage.first())
                assertEquals(0, platform.writes)
            }
        }

    @Test
    fun existingPlatformChoiceAndNoOpNeverWriteAnAlternative() =
        runTest(main.dispatcher) {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            repository.setLegacyLanguage("fi")
            val platform = FakeLocalePlatform(true, AppLanguage.ENGLISH)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            assertEquals(AppLanguage.ENGLISH, controller.selection.value.choice)
            controller.select(AppLanguage.ENGLISH)
            advanceUntilIdle()
            assertNull(repository.legacyLanguage.first())
            assertEquals(0, platform.writes)
        }

    @Test
    fun cleanupFailureRetriesWithoutOverwritingExternalDeviceChoice() =
        runTest(main.dispatcher) {
            val store = FailingPreferencesStore()
            val repository = ProgressRepository(store)
            repository.setLegacyLanguage("fi")
            store.fail = true
            val platform = FakeLocalePlatform(true)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            assertTrue(controller.selection.value.failed)
            assertTrue(controller.selection.value.ready)
            assertEquals(AppLanguage.FINNISH, controller.selection.value.choice)
            assertEquals(AppLanguage.FINNISH, platform.choice)
            platform.choice = null
            store.fail = false
            controller.refresh()
            advanceUntilIdle()
            assertTrue(controller.selection.value.ready)
            assertNull(controller.selection.value.choice)
            assertNull(repository.legacyLanguage.first())
            assertEquals(1, platform.writes)
        }

    @Test
    fun failedPlatformApplicationIsRetryableAndDoesNotCommitChoice() =
        runTest(main.dispatcher) {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            val platform = FakeLocalePlatform(true, AppLanguage.ENGLISH)
            val controller = AppLocaleController(repository, platform)
            advanceUntilIdle()
            platform.fail = true
            controller.select(AppLanguage.SWEDISH)
            advanceUntilIdle()
            assertTrue(controller.selection.value.failed)
            assertEquals(AppLanguage.ENGLISH, controller.selection.value.choice)
            platform.fail = false
            controller.select(AppLanguage.SWEDISH)
            advanceUntilIdle()
            assertEquals(AppLanguage.SWEDISH, controller.selection.value.choice)
        }
}

private class FakeLocalePlatform(
    override val supported: Boolean,
    var choice: AppLanguage? = null,
) : AppLocalePlatform {
    var writes = 0
    var fail = false

    override fun read() = choice

    override fun write(language: AppLanguage?) {
        check(!fail) { "Synthetic locale failure" }
        writes++
        choice = language
    }
}

private class FailingPreferencesStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    var fail = false
    var failRead = false
    override val data =
        flow {
            if (failRead) throw IOException("Synthetic read failure")
            emitAll(delegate.data)
        }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (fail) throw IOException("Synthetic storage failure")
        return delegate.updateData(transform)
    }
}
