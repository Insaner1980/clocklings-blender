package fi.clocklings.ui

import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RewardAccessActionsTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun directSelectionRequiresBothOwnershipAndThreeEarnedStars() =
        runTest {
            val services = FakeServices(fullAccess = false)
            services.repository.updateProgress { it.withLevelResult(0, 3) }
            val app = AppViewModel(services)
            runCurrent()
            app.personalize { it.copy(companion = CompanionKind.DRAGON) }
            runCurrent()
            assertEquals(
                CompanionKind.HUMAN,
                services.repository.settings
                    .first()
                    .personalization.companion,
            )
            services.purchases.state.value =
                services.purchases.state.value
                    .copy(fullAccess = true)
            services.repository.updateProgress { Progress() }
            runCurrent()
            app.personalize { it.copy(companion = CompanionKind.DRAGON) }
            runCurrent()
            assertEquals(
                CompanionKind.HUMAN,
                services.repository.settings
                    .first()
                    .personalization.companion,
            )
            services.repository.updateProgress { it.withLevelResult(0, 3) }
            runCurrent()
            app.personalize { it.copy(companion = CompanionKind.DRAGON) }
            runCurrent()
            assertEquals(
                CompanionKind.DRAGON,
                services.repository.settings
                    .first()
                    .personalization.companion,
            )
        }

    @Test fun directItemSelectionCannotBypassAccessStarsOrCatalogIdentity() =
        runTest {
            val services = FakeServices(fullAccess = false)
            services.repository.updateProgress { it.copy(stars = List(5) { 3 }) }
            val app = AppViewModel(services)
            runCurrent()
            val paid = Wardrobe.item("dress-sateenkaari")
            app.toggleItem(CompanionKind.HUMAN, paid)
            app.toggleItem(CompanionKind.HUMAN, paid.copy(star = 0))
            app.toggleItem(CompanionKind.CAT, Wardrobe.item("dress-pilkku"))
            runCurrent()
            assertEquals(
                emptyMap<CompanionKind, Outfit>(),
                services.repository.progress
                    .first()
                    .outfits,
            )
            services.purchases.state.value =
                services.purchases.state.value
                    .copy(fullAccess = true)
            services.repository.updateProgress { Progress() }
            runCurrent()
            app.toggleItem(CompanionKind.HUMAN, paid)
            runCurrent()
            assertEquals(
                emptyMap<CompanionKind, Outfit>(),
                services.repository.progress
                    .first()
                    .outfits,
            )
        }

    @Test fun lossAndUnrelatedPreferenceChangesPreservePaidCompanionNamesAndOutfits() =
        runTest {
            val services = FakeServices(fullAccess = true)
            val saved =
                Progress(stars = List(5) { 3 })
                    .withName(CompanionKind.DRAGON, "Saved name")
                    .withOutfit(CompanionKind.DRAGON, Outfit().with(Slot.HEAD, "dragon-head-crown"))
            services.repository.updateProgress { saved }
            services.repository.updateSettings {
                it.copy(personalization = it.personalization.copy(companion = CompanionKind.DRAGON))
            }
            val app = AppViewModel(services)
            runCurrent()
            services.purchases.state.value =
                services.purchases.state.value
                    .copy(fullAccess = false)
            app.personalize { it.copy(clock = it.clock.copy(theme = ClockTheme.OCEAN)) }
            runCurrent()
            val settings = services.repository.settings.first()
            assertEquals(CompanionKind.DRAGON, settings.personalization.companion)
            assertEquals(ClockTheme.OCEAN, settings.personalization.clock.theme)
            assertEquals(saved, services.repository.progress.first())
        }
}
