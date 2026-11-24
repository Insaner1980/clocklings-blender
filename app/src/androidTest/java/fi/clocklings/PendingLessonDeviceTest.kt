package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.Lessons
import fi.clocklings.ui.AppViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Completion copy must represent a committed lesson, including a delayed failure and retry. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PendingLessonDeviceTest : StorageDeviceTest() {
    @Test
    fun lastLessonQuizStartsTheLevel() {
        val job = SupervisorJob()
        val store = writeGatedPreferences(files.newFolder().resolve("lesson.preferences_pb"), job) {}
        val services = servicesFor(store)
        val model = AppViewModel(services)
        try {
            runBlocking {
                services.repository.updateSettings { it.copy(sound = false) }
                services.repository.updateProgress {
                    it.copy(
                        lessonsDone =
                            Lessons.levelLessons
                                .first()
                                .dropLast(1)
                                .toSet(),
                    )
                }
            }
            compose.setContent { ClocklingsApp(container = services, app = model) }
            click("Play")
            click("Whole hours")
            repeat(2) { click("Next") }
            click("Try it")
            compose.onNodeWithText("Done").assertIsNotEnabled()
            click("Nine o'clock")
            click(context.getString(R.string.lesson_start_game))
            compose.onNodeWithTag("game_task_0").assertIsDisplayed()
        } finally {
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    @Test
    fun lessonCompletionWaitsForCommitAndDelayedFailureCanRetry() {
        val writable = CompletableDeferred<Unit>()
        val retryWritable = CompletableDeferred<Unit>()
        val hold = AtomicBoolean(false)
        val fail = AtomicBoolean(true)
        val job = SupervisorJob()
        val file = files.newFolder().resolve("lesson.preferences_pb")
        val store =
            writeGatedPreferences(file, job) {
                if (hold.get()) {
                    writable.await()
                    if (fail.getAndSet(false)) throw IOException("Synthetic delayed lesson write failure")
                    retryWritable.await()
                }
            }
        val services = servicesFor(store)
        val model = AppViewModel(services)
        try {
            runBlocking { services.repository.updateSettings { it.copy(sound = false) } }
            compose.setContent { ClocklingsApp(container = services, app = model) }
            click("Learn")
            click("Two hands")
            click("Next")
            click("Next")
            click("Try it")
            hold.set(true)
            click("The short pink hand")
            val completion = hasText("You completed Two hands.", substring = true)
            compose.onNode(completion).assertDoesNotExist()
            val done = compose.onNodeWithText("Done")
            done.assertIsDisplayed().assertIsNotEnabled()
            val before = runBlocking { services.repository.progress.first() }
            assertEquals(emptySet<String>(), before.lessonsDone)
            writable.complete(Unit)
            val error = context.getString(R.string.progress_save_error)
            compose.waitUntil(5_000) { compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty() }
            val retry = context.getString(R.string.settings_retry)
            click(retry)
            compose.onNodeWithText(retry).assertIsDisplayed().assertIsNotEnabled()
            retryWritable.complete(Unit)
            compose.waitUntil(5_000) {
                compose.onAllNodes(completion).fetchSemanticsNodes().isNotEmpty()
            }
            val saved = runBlocking { services.repository.progress.first() }
            assertEquals(setOf("viisarit"), saved.lessonsDone)
            done.assertIsEnabled()
        } finally {
            writable.cancel()
            retryWritable.cancel()
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    private fun click(text: String) = compose.clickStorageAction(text)
}
