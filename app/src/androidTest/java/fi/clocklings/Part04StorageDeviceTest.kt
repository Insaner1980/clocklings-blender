package fi.clocklings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Part04StorageDeviceTest {
    @get:Rule val files = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    @Test
    fun resetAllCurrentCompanionsPreservesSettingsAndUnknownKeysAfterReopen() =
        runBlocking {
            val file = files.newFolder().resolve("all-companions.preferences_pb")
            val firstJob = SupervisorJob()
            val settings = Settings(sound = false, help = false, h24 = true)
            try {
                val store =
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file }
                val repository = ProgressRepository(store)
                store.edit {
                    it[booleanPreferencesKey("companion_DOG_accessory")] = true
                    it[booleanPreferencesKey("companion_CAR_accessory")] = true
                    it[stringPreferencesKey("future_part04")] = "keep"
                }
                repository.updateSettings { settings }
                repository.setLegacyLanguage("sv")
                repository.updateProgress {
                    CompanionKind.entries.filter { it.slots.isNotEmpty() }.fold(
                        Progress(
                            stars = listOf(3, 2, 1, 3, 2),
                            challengeBest = 12,
                            lessonsDone = setOf("viisarit", "puoli"),
                            introShown = setOf("0", "h24"),
                        ),
                    ) { progress, kind ->
                        val outfit =
                            Wardrobe.items.filter { it.companion == kind }.fold(Outfit.default(kind)) { outfit, item ->
                                outfit.with(item.slot, item.id)
                            }
                        progress.withName(kind, "Mäx ${kind.name}").withOutfit(kind, outfit)
                    }
                }
                repository.resetProgress()
                assertEquals(Progress(), repository.progress.first())
            } finally {
                firstJob.cancelAndJoin()
            }
            val secondJob = SupervisorJob()
            try {
                val store =
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO)) { file }
                val repository = ProgressRepository(store)
                assertEquals(Progress(), repository.progress.first())
                assertEquals(settings, repository.settings.first())
                assertEquals("sv", repository.legacyLanguage.first())
                assertEquals("keep", store.data.first()[stringPreferencesKey("future_part04")])
            } finally {
                secondJob.cancelAndJoin()
            }
        }

    @Test
    fun realAtomicEditsMergeIndependentFieldsAndResetUsesTheCommittedModel() =
        runBlocking {
            val file = files.newFolder().resolve("atomic.preferences_pb")
            val storeJob = SupervisorJob()
            try {
                val store =
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(storeJob + Dispatchers.IO)) { file }
                val repository = ProgressRepository(store)
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val first =
                    launch {
                        store.edit {
                            entered.complete(Unit)
                            release.await()
                            it[intPreferencesKey("stars_0")] = 3
                        }
                    }
                entered.await()
                val name = launch { repository.updateProgress { it.withName(CompanionKind.CAT, "Mäx") } }
                val settings = launch { repository.updateSettings { it.copy(sound = false) } }
                release.complete(Unit)
                first.join()
                name.join()
                settings.join()
                assertEquals(
                    Progress().withLevelResult(0, 3).withName(CompanionKind.CAT, "Mäx"),
                    repository.progress.first(),
                )
                assertEquals(Settings(sound = false), repository.settings.first())
                repository.resetProgress()
                assertEquals(Progress(), repository.progress.first())
                repository.updateProgress { it.withName(CompanionKind.CAT, "After reset") }
                assertEquals(Progress().withName(CompanionKind.CAT, "After reset"), repository.progress.first())
                assertEquals(Settings(sound = false), repository.settings.first())
            } finally {
                storeJob.cancelAndJoin()
            }
        }
}
