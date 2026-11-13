package fi.clocklings.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DOG
import fi.clocklings.domain.CompanionKind.HUMAN
import fi.clocklings.domain.CompanionKind.ROBOT
import fi.clocklings.domain.CompanionStyle
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepositoryTest {
    private val scope = TestScope(StandardTestDispatcher())
    private val store = InMemoryPreferencesStore()
    private val repository = ProgressRepository(store)

    @Test
    fun invalidStarAndBestValuesAreClampedWithoutDroppingValidFields() =
        scope.runTest {
            store.edit { preferences ->
                listOf(-1, 0, 3, 4, 2).forEachIndexed { index, value ->
                    preferences[intPreferencesKey("stars_$index")] = value
                }
                preferences[intPreferencesKey("challenge_best")] = -1
                preferences[stringPreferencesKey("doll_name")] = "Mia"
            }
            val progress = repository.progress.first()
            assertEquals(listOf(0, 0, 3, 3, 2), progress.stars)
            assertEquals(0, progress.challengeBest)
            assertEquals("Mia", progress.nameFor(HUMAN))
        }

    @Test
    fun queuedDisjointTransformsAndResetFollowSerializedCommitOrder() =
        scope.runTest {
            val release = CompletableDeferred<Unit>()
            val first =
                launch {
                    store.updateData { preferences ->
                        release.await()
                        preferences.toMutablePreferences().apply { this[intPreferencesKey("stars_0")] = 3 }
                    }
                }
            runCurrent()
            val name = launch { repository.updateProgress { it.withName(CAT, "Mia") } }
            val settings = launch { repository.updateSettings { it.copy(sound = false) } }
            runCurrent()
            assertEquals(Progress(), repository.progress.first())
            release.complete(Unit)
            first.join()
            name.join()
            settings.join()
            assertEquals(3, repository.progress.first().totalStars)
            assertEquals("Mia", repository.progress.first().nameFor(CAT))
            assertEquals(false, repository.settings.first().sound)
            repository.resetProgress()
            assertEquals(Progress(), repository.progress.first())
            assertEquals(false, repository.settings.first().sound)
        }

    @Test
    fun `empty store gives defaults`() =
        scope.runTest {
            assertEquals(Progress(), repository.progress.first())
            assertEquals(Settings(), repository.settings.first())
        }

    @Test
    fun `progress round trips`() =
        scope.runTest {
            val p =
                Progress(
                    stars = listOf(3, 2, 1, 0, 0),
                    challengeBest = 12,
                    lessonsDone = setOf("viisarit", "puoli"),
                    introShown = setOf("0", "h24"),
                ).withOutfit(HUMAN, Outfit.Default.with(Slot.DRESS, "dress-tahti").with(Slot.HEAD, "head-kruunu"))
                    .withName(HUMAN, "Ruusa")
            repository.updateProgress { p }
            assertEquals(p, repository.progress.first())
        }

    @Test
    fun `animal outfits and names round trip`() =
        scope.runTest {
            val p =
                Progress()
                    .withOutfit(CAT, Outfit().with(Slot.HEAD, "cat-head-crown").with(Slot.NECK, "cat-neck-scarf"))
                    .withOutfit(ROBOT, Outfit().with(Slot.EXTRA, "robot-extra-jetpack"))
                    .withName(CAT, "Miuku")
                    .withName(HUMAN, "Ruusa")
            repository.updateProgress { p }
            assertEquals(p, repository.progress.first())
        }

    @Test
    fun `an id from another companion is ignored`() =
        scope.runTest {
            store.edit { it[stringPreferencesKey("outfit_dog_head")] = "cat-head-bow" }
            assertEquals(Outfit(), repository.progress.first().outfitFor(DOG))
        }

    @Test
    fun `legacy accessory wears the free item only without a saved outfit`() =
        scope.runTest {
            store.edit {
                it[booleanPreferencesKey("companion_DOG_accessory")] = true
                it[booleanPreferencesKey("companion_CAT_accessory")] = true
                it[stringPreferencesKey("outfit_cat_head")] = "cat-head-bow"
            }
            val p = repository.progress.first()
            assertEquals("dog-neck-bandana", p.outfitFor(DOG)[Slot.NECK])
            assertEquals(null, p.outfitFor(CAT)[Slot.NECK])
            assertEquals("cat-head-bow", p.outfitFor(CAT)[Slot.HEAD])
        }

    @Test
    fun `an empty saved outfit counts as saved and blocks the legacy accessory`() =
        scope.runTest {
            store.edit {
                it[booleanPreferencesKey("companion_DOG_accessory")] = true
                it[stringPreferencesKey("outfit_dog_neck")] = ""
            }
            assertEquals(Outfit(), repository.progress.first().outfitFor(DOG))
        }

    @Test
    fun `reset clears every outfit and name but keeps colors`() =
        scope.runTest {
            repository.updateSettings {
                it.copy(personalization = it.personalization.withStyle(CAT, CompanionStyle(CompanionColor.BLUE)))
            }
            repository.updateProgress {
                it.withOutfit(CAT, Outfit().with(Slot.HEAD, "cat-head-bow")).withName(CAT, "Miuku")
            }
            repository.resetProgress()
            assertEquals(Progress(), repository.progress.first())
            assertEquals(
                CompanionColor.BLUE,
                repository.settings
                    .first()
                    .personalization
                    .styleFor(CAT)
                    .color,
            )
        }

    @Test
    fun `settings round trip`() =
        scope.runTest {
            repository.updateSettings { it.copy(sound = false, h24 = true) }
            assertEquals(Settings(sound = false, help = true, h24 = true), repository.settings.first())
        }

    @Test
    fun `reset clears progress but keeps settings`() =
        scope.runTest {
            repository.updateSettings { it.copy(help = false, h24 = true) }
            repository.updateProgress { it.withLevelResult(0, 3).withName(HUMAN, "Ruusa") }
            repository.resetProgress()
            assertEquals(Progress(), repository.progress.first())
            assertEquals(Settings(help = false, h24 = true), repository.settings.first())
        }

    @Test
    fun `unknown outfit ids fall back to defaults`() =
        scope.runTest {
            store.edit {
                it[stringPreferencesKey("outfit_dress")] = "dress-olematon"
                it[stringPreferencesKey("outfit_head")] = "hair-letit"
            }
            val outfit = repository.progress.first().outfitFor(HUMAN)
            assertEquals(Outfit.Default[Slot.DRESS], outfit[Slot.DRESS])
            assertEquals(null, outfit[Slot.HEAD])
        }
}
