package fi.clocklings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Wardrobe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderPreviewTest {
    @Test
    fun firstLaunchMakesTheRobotAndItsItemsAvailable() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = context.getSharedPreferences("preview_test", Context.MODE_PRIVATE)
            val repository = ProgressRepository(InMemoryPreferencesStore())
            initializeBlenderPreview(repository, preferences)
            assertEquals(
                CompanionKind.ROBOT,
                repository.settings
                    .first()
                    .personalization.companion,
            )
            assertEquals(Wardrobe.MAX_STARS, repository.progress.first().totalStars)
        }

    @Test
    fun laterLaunchPreservesTheUsersChanges() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val preferences = context.getSharedPreferences("preview_test", Context.MODE_PRIVATE)
            val repository = ProgressRepository(InMemoryPreferencesStore())
            initializeBlenderPreview(repository, preferences)
            repository.updateProgress { it.copy(stars = listOf(1, 0, 0, 0, 0)) }
            repository.updateSettings {
                it.copy(
                    personalization = it.personalization.copy(companion = CompanionKind.CAT),
                )
            }
            initializeBlenderPreview(repository, preferences)
            assertEquals(
                CompanionKind.CAT,
                repository.settings
                    .first()
                    .personalization.companion,
            )
            assertEquals(1, repository.progress.first().totalStars)
        }
}
