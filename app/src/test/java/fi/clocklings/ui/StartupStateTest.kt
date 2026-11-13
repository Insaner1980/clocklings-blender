package fi.clocklings.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.ClocklingsApp
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 33])
class StartupStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun delayedFirstReadShowsLoadingUntilSavedProgressArrives() {
        val readable = CompletableDeferred<Unit>()
        val delegate = InMemoryPreferencesStore()
        val store =
            object : DataStore<Preferences> {
                override val data =
                    flow {
                        readable.await()
                        emitAll(delegate.data)
                    }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    delegate.updateData(transform)
            }
        val audio = FakeServices(fullAccess = true)
        val services =
            object : AppServices by audio {
                override val repository = ProgressRepository(store)
            }
        runBlocking { services.repository.updateProgress { it.withLevelResult(0, 3) } }
        val model = AppViewModel(services)
        // Locale reads are ready independently of the app's blocked progress/settings reads, including on API 26.
        val localeServices = FakeServices(fullAccess = true)
        val viewport = mutableStateOf(411 to 1f)
        val loading =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, viewport.value.second)) {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                    ClocklingsApp(container = localeServices, app = model)
                }
            }
        }
        try {
            compose.waitUntil(5_000) { audio.speechRefreshes > 0 }
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle { viewport.value = cell }
                compose.onNode(loading).assertIsDisplayed()
                assertEquals(null, model.progress.value)
            }
            readable.complete(Unit)
            compose.waitUntil(10_000) { model.progress.value?.totalStars == 3 && model.settings.value != null }
            compose.onNode(loading).assertDoesNotExist()
            assertEquals(3, model.progress.value!!.totalStars)
        } finally {
            readable.cancel()
            model.viewModelScope.cancel()
        }
    }
}
