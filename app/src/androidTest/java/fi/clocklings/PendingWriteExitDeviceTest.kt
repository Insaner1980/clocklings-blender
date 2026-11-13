package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.AppViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Navigation while a name update waits before the real DataStore write, not during atomic commit. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PendingWriteExitDeviceTest : StorageDeviceTest() {
    @Test
    fun pendingNameSucceedsAfterLeavingAndReopensFromDisk() = verifyExit(fail = false)

    @Test
    fun pendingNameFailureAfterLeavingKeepsCommittedNameAndFile() = verifyExit(fail = true)

    private fun verifyExit(fail: Boolean) {
        val entered = CompletableDeferred<Unit>()
        val writable = CompletableDeferred<Unit>()
        val hold = AtomicBoolean(false)
        val job = SupervisorJob()
        val file = files.newFolder().resolve("exit.preferences_pb")
        val store =
            writeGatedPreferences(file, job) {
                if (hold.get()) {
                    entered.complete(Unit)
                    writable.await()
                    if (fail) throw IOException("Synthetic pending name exit failure")
                }
            }
        val services = servicesFor(store)
        val model = AppViewModel(services)
        try {
            runBlocking {
                services.repository.updateSettings { it.copy(sound = false) }
                services.repository.updateProgress { it.withName(CompanionKind.HUMAN, "Saved") }
            }
            val committed = file.readBytes()
            compose.setContent { ClocklingsApp(container = services, app = model) }
            openWardrobe()
            hold.set(true)
            compose.onNode(hasSetTextAction()).performTextReplacement("Exit")
            runBlocking { withTimeout(5_000) { entered.await() } }
            assertArrayEquals(committed, file.readBytes())
            leaveWardrobe()
            assertName(services.repository, "Saved")
            writable.complete(Unit)
            compose.waitUntil(5_000) {
                if (fail) {
                    model.settingsAction.value.failed
                } else {
                    model.progress.value?.nameFor(CompanionKind.HUMAN) ==
                        "Exit"
                }
            }
            val expected = if (fail) "Saved" else "Exit"
            if (fail) assertArrayEquals(committed, file.readBytes())
            openWardrobe()
            compose.waitUntil(5_000) {
                compose
                    .onNode(hasSetTextAction())
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.EditableText]
                    .text ==
                    expected
            }
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
            assertReopenedName(file, expected)
        } finally {
            writable.cancel()
            model.viewModelScope.cancel()
            runBlocking { job.cancelAndJoin() }
        }
    }

    private fun assertReopenedName(
        file: File,
        expected: String,
    ) {
        val job = SupervisorJob()
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            assertName(ProgressRepository(reopened), expected)
        } finally {
            runBlocking { job.cancelAndJoin() }
        }
    }

    private fun assertName(
        repository: ProgressRepository,
        expected: String,
    ) {
        assertEquals(expected, runBlocking { repository.progress.first().nameFor(CompanionKind.HUMAN) })
    }

    private fun openWardrobe() {
        click("My Companion")
        click("Choose an outfit")
    }

    private fun leaveWardrobe() {
        click("Back")
        click("Back")
        compose.onNode(hasText("Clocklings")).assertExists()
    }

    private fun click(text: String) = compose.clickStorageAction(text)
}
