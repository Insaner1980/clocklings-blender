package fi.clocklings.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CompanionStyle
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalizationTest {
    @Test
    fun changingEveryCompanionAndThemePreservesLearningAndHumanOutfit() =
        runTest {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            val progress = Progress().withLevelResult(2, 3).copy(lessonsDone = setOf("puoli"))
            repository.updateProgress { progress }
            for (kind in CompanionKind.entries) {
                for (theme in ClockTheme.entries) {
                    repository.updateSettings {
                        it.copy(
                            personalization =
                                it.personalization.copy(
                                    companion = kind,
                                    clock = it.personalization.clock.copy(theme = theme),
                                ),
                        )
                    }
                    assertEquals(
                        kind,
                        repository.settings
                            .first()
                            .personalization.companion,
                    )
                    assertEquals(
                        theme,
                        repository.settings
                            .first()
                            .personalization.clock.theme,
                    )
                    assertEquals(progress, repository.progress.first())
                }
            }
            val settings = repository.settings.first()
            repository.resetProgress()
            assertEquals(settings, repository.settings.first())
        }

    @Test
    fun oldAndUnknownPreferencesKeepSafeDefaults() =
        runTest {
            val store = InMemoryPreferencesStore()
            val repository = ProgressRepository(store)
            assertEquals(Settings(), repository.settings.first())
            store.edit { preferences ->
                for (key in listOf(
                    "companion",
                    "clock_theme",
                    "clock_palette",
                    "clock_frame",
                    "clock_numerals",
                    "clock_hands",
                    "companion_CAT_color",
                )) {
                    preferences[stringPreferencesKey(key)] = "future-or-invalid-value"
                }
            }
            assertEquals(Settings(), repository.settings.first())
        }

    @Test
    fun storedDragonPurpleReadsAsOriginal() =
        runTest {
            val store = InMemoryPreferencesStore()
            val repository = ProgressRepository(store)
            store.edit { it[stringPreferencesKey("companion_DRAGON_color")] = CompanionColor.PURPLE.name }
            assertEquals(
                CompanionStyle(),
                repository.settings
                    .first()
                    .personalization
                    .styleFor(CompanionKind.DRAGON),
            )
        }

    @Test
    fun returningToCompanionRestoresItsOwnStyle() =
        runTest {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            val blueCat = CompanionStyle(CompanionColor.BLUE)
            repository.updateSettings {
                it.copy(
                    personalization = it.personalization.withStyle(CompanionKind.CAT, blueCat),
                )
            }
            repository.updateSettings {
                it.copy(
                    personalization =
                        it.personalization.withStyle(
                            CompanionKind.DOG,
                            CompanionStyle(CompanionColor.PURPLE),
                        ),
                )
            }
            assertEquals(
                blueCat,
                repository.settings
                    .first()
                    .personalization
                    .styleFor(CompanionKind.CAT),
            )
            assertEquals(
                CompanionStyle(CompanionColor.PURPLE),
                repository.settings
                    .first()
                    .personalization
                    .styleFor(CompanionKind.DOG),
            )
        }
}
