package fi.clocklings.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CompanionKind.DINOSAUR
import fi.clocklings.domain.CompanionKind.HUMAN
import fi.clocklings.domain.CompanionKind.ROBOT
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Slot.DRESS
import fi.clocklings.domain.Slot.EXTRA
import fi.clocklings.domain.Slot.HEAD
import fi.clocklings.domain.Slot.NECK
import fi.clocklings.domain.Slot.SHOES
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OutfitMigrationTest {
    @Test
    fun allReplacedItemsSurviveReadingSavingAndReopening() =
        runTest {
            val replacements =
                listOf(
                    Replacement(HUMAN, EXTRA, EXTRA, "extra-laukku", "extra-strawberry-bag"),
                    Replacement(HUMAN, DRESS, DRESS, "dress-prinsessa", "dress-overalls"),
                    Replacement(HUMAN, SHOES, SHOES, "shoes-kimalle", "shoes-roller-skates"),
                    Replacement(ROBOT, NECK, NECK, "robot-neck-tie", "robot-neck-cape"),
                    Replacement(ROBOT, NECK, EXTRA, "robot-neck-spring", "robot-extra-bubble-pack"),
                    Replacement(ROBOT, NECK, EXTRA, "robot-neck-lights", "robot-extra-wind-up-key"),
                    Replacement(DINOSAUR, NECK, EXTRA, "dinosaur-neck-lei", "dinosaur-extra-swim-ring"),
                    Replacement(DINOSAUR, HEAD, EXTRA, "dinosaur-head-explorer-hat", "dinosaur-extra-ice-cream"),
                )
            for (replacement in replacements) {
                val (kind, from, to) = replacement
                val oldId = replacement.oldId
                val newId = replacement.newId
                val store = InMemoryPreferencesStore()
                store.edit { it[key(kind, from)] = oldId }
                val repository = ProgressRepository(store)
                val migrated = repository.progress.first()
                assertEquals(oldId, newId, migrated.outfitFor(kind)[to])
                if (from != to) assertEquals(oldId, null, migrated.outfitFor(kind)[from])
                repository.updateProgress { it.withName(kind, "Mia") }
                assertEquals(newId, store.data.first()[key(kind, to)])
                assertEquals(migrated.withName(kind, "Mia"), ProgressRepository(store).progress.first())
                repository.updateProgress { it.withOutfit(kind, it.outfitFor(kind).with(EXTRA, null)) }
                assertEquals(null, repository.progress.first().outfitFor(kind)[EXTRA])
                repository.resetProgress()
                assertEquals(Progress(), repository.progress.first())
            }
        }

    @Test
    fun anExistingExtraWinsAndCompetingLegacyItemsUseHeadBeforeNeck() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(ROBOT, NECK)] = "robot-neck-spring"
                it[key(ROBOT, EXTRA)] = "robot-extra-jetpack"
                it[key(DINOSAUR, HEAD)] = "dinosaur-head-explorer-hat"
                it[key(DINOSAUR, NECK)] = "dinosaur-neck-lei"
            }
            val repository = ProgressRepository(store)
            val migrated = repository.progress.first()
            assertEquals(Outfit().with(EXTRA, "robot-extra-jetpack"), migrated.outfitFor(ROBOT))
            assertEquals(Outfit().with(EXTRA, "dinosaur-extra-ice-cream"), migrated.outfitFor(DINOSAUR))
            repository.updateProgress { it }
            assertEquals(migrated, ProgressRepository(store).progress.first())
        }

    @Test
    fun legacyIdsInTheWrongCompanionOrSourceSlotRemainInvalid() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(ROBOT, HEAD)] = "robot-neck-spring"
                it[key(DINOSAUR, NECK)] = "robot-neck-tie"
                it[key(HUMAN, HEAD)] = "dress-prinsessa"
            }
            assertEquals(Progress(), ProgressRepository(store).progress.first())
        }

    private data class Replacement(
        val kind: CompanionKind,
        val from: Slot,
        val to: Slot,
        val oldId: String,
        val newId: String,
    )

    private fun key(
        kind: CompanionKind,
        slot: Slot,
    ) = stringPreferencesKey(
        if (kind == HUMAN) {
            "outfit_${slot.name.lowercase()}"
        } else {
            "outfit_${kind.name.lowercase()}_${slot.name.lowercase()}"
        },
    )
}
