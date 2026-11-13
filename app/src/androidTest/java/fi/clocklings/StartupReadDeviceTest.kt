package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.ui.AppViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/** Delays the read boundary around an actual owned Preferences file on the dedicated emulator. */
@RunWith(AndroidJUnit4::class)
class StartupReadDeviceTest : StorageDeviceTest() {
    @Test
    fun delayedRealStoreReadAndSubmittedResetHaveObservableBoundaries() {
        val readable = CompletableDeferred<Unit>()
        val writable = CompletableDeferred<Unit>()
        val pauseWrites = AtomicBoolean(false)
        val job = SupervisorJob()
        val file = files.newFolder().resolve("startup.preferences_pb")
        val delegate = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        val gated =
            object : DataStore<Preferences> {
                override val data =
                    flow {
                        readable.await()
                        emitAll(delegate.data)
                    }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    if (pauseWrites.get()) writable.await()
                    return delegate.updateData(transform)
                }
            }
        val services = servicesFor(gated)
        val model = AppViewModel(services)
        val loading =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        try {
            runBlocking {
                services.repository.updateProgress { it.withLevelResult(0, 3) }
                services.repository.updateSettings { it.copy(sound = false) }
            }
            compose.setContent { ClocklingsApp(container = services, app = model) }
            compose.onNode(loading).assertIsDisplayed()
            assertEquals(null, model.progress.value)
            assertFalse("Loading is not a usable startup", compose.activity.fullyDrawnReporter.isFullyDrawnReported)
            readable.complete(Unit)
            runBlocking {
                withTimeout(10_000) {
                    model.progress.first { it?.totalStars == 3 }
                    model.settings.first { it != null }
                }
            }
            compose.onNode(loading).assertDoesNotExist()
            assertEquals(3, model.progress.value?.totalStars)
            compose.waitUntil(10_000) { compose.activity.fullyDrawnReporter.isFullyDrawnReported }
            observeSubmittedReset(model, pauseWrites, writable)
        } finally {
            readable.cancel()
            writable.cancel()
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    private fun observeSubmittedReset(
        model: AppViewModel,
        pauseWrites: AtomicBoolean,
        writable: CompletableDeferred<Unit>,
    ) {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Reset progress").scrollToIfScrollable().performClick()
        pauseWrites.set(true)
        compose.onAllNodesWithText("Reset progress").onLast().performClick()
        compose.waitUntil(5_000) { model.settingsAction.value.pending }
        assertEquals(3, model.progress.value?.totalStars)
        compose.onNodeWithText("Cancel").assertIsDisplayed().assertIsNotEnabled()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Reset learning progress?").assertIsDisplayed()
        assertTrue(model.settingsAction.value.pending)
        writable.complete(Unit)
        compose.waitUntil(5_000) { !model.settingsAction.value.pending && model.progress.value?.totalStars == 0 }
        compose.onNodeWithText("Reset learning progress?").assertDoesNotExist()
        assertEquals(false, model.settings.value?.sound)
    }
}
