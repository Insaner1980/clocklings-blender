package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import fi.clocklings.ui.locale.AppLocaleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Actual Android rendering with an owned real store and a controlled failing read boundary. */
@RunWith(AndroidJUnit4::class)
class StartupFailureDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val files = TemporaryFolder(context.cacheDir)
    private val job = SupervisorJob()
    private val readable = AtomicBoolean(false)
    private val file by lazy { files.newFolder().resolve("read-failure.preferences_pb") }
    private val delegate by lazy {
        PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
    }
    private val repository by lazy {
        ProgressRepository(
            object : DataStore<Preferences> {
                override val data =
                    flow {
                        if (!readable.get()) throw IOException("Synthetic startup read failure")
                        emitAll(delegate.data)
                    }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    delegate.updateData(transform)
            },
        )
    }
    private val services by lazy {
        object : AppServices by (context.applicationContext as ClocklingsApplication).container {
            override val repository = this@StartupFailureDeviceTest.repository
        }
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(files).around(compose)

    @Test
    fun startupReadFallbackDoesNotRewriteCommittedDataAndRecovers() {
        val committed = ProgressRepository(delegate)
        runBlocking {
            committed.updateProgress { it.withLevelResult(0, 3) }
            committed.updateSettings { it.copy(sound = false) }
        }
        val original = file.readBytes()
        val model = AppViewModel(services)
        try {
            compose.setContent { ClocklingsApp(container = services, app = model) }
            compose.waitUntil(5_000) { model.progress.value != null && model.settings.value != null }
            compose.onNodeWithText("Clocklings").assertIsDisplayed()
            assertEquals(0, model.progress.value?.totalStars)
            assertEquals(true, model.settings.value?.sound)
            assertArrayEquals(original, file.readBytes())
            readable.set(true)
            compose.waitUntil(10_000) { model.progress.value?.totalStars == 3 && model.settings.value?.sound == false }
            compose.onNodeWithText("Clocklings").assertIsDisplayed()
            assertArrayEquals(original, file.readBytes())
        } finally {
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    @Test
    fun openChallengeShowsTheStoredBestWhenReadsRecover() {
        runBlocking { ProgressRepository(delegate).updateProgress { it.withChallengeScore(7) } }
        val model = AppViewModel(services)
        try {
            compose.setContent { ClocklingsApp(container = services, app = model) }
            compose.clickStorageAction("Play")
            compose.clickStorageAction("Minute Challenge")
            compose.onNodeWithText("No score yet").scrollToIfScrollable().assertIsDisplayed()

            readable.set(true)
            val best = context.getString(R.string.challenge_best, 7)
            // A failed read can back off for up to 30 seconds before the store is read again.
            compose.waitUntil(40_000) {
                model.progress.value?.challengeBest == 7 &&
                    compose.onAllNodesWithText(best).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(best).scrollToIfScrollable().assertIsDisplayed()
        } finally {
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    @Test
    fun localeHandoffReadFailureShowsReachableRetryAndRecovers() {
        val platform =
            object : AppLocalePlatform {
                override val supported = true

                override fun read(): AppLanguage? = null

                override fun write(language: AppLanguage?) = Unit
            }
        val controller = AppLocaleController(repository, platform)
        try {
            compose.setContent {
                AppLocaleProvider(repository, controller = controller) { Text("Recovered locale content") }
            }
            val error = context.getString(R.string.language_save_error)
            val retry = context.getString(R.string.settings_retry)
            compose.waitUntil(5_000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(error).assertIsDisplayed()
            compose.waitUntil(5_000) { compose.onNodeWithText(retry).scrollToIfScrollable().isDisplayed() }
            compose.onNodeWithText(retry).assertIsDisplayed()
            compose.onNodeWithText("Recovered locale content").assertDoesNotExist()
            readable.set(true)
            compose.onNodeWithText(retry).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Recovered locale content").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(error).assertDoesNotExist()
            compose.onNodeWithText(retry).assertDoesNotExist()
        } finally {
            controller.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }
}
