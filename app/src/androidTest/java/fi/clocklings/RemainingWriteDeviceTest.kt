package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import fi.clocklings.ui.AppViewModel
import kotlinx.coroutines.CancellationException
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException

/** Real file persistence with pre-updateData delays; these gates do not interrupt atomic commits. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RemainingWriteDeviceTest : StorageDeviceTest() {
    @Test fun companionExitSuccess() = appearanceExit("companion", false)

    @Test fun companionExitFailure() = appearanceExit("companion", true)

    @Test fun clockStyleExitSuccess() = appearanceExit("clock", false)

    @Test fun clockStyleExitFailure() = appearanceExit("clock", true)

    @Test fun colourExitSuccess() = appearanceExit("colour", false)

    @Test fun colourExitFailure() = appearanceExit("colour", true)

    @Test fun outfitExitSuccess() = appearanceExit("outfit", false)

    @Test fun outfitExitFailure() = appearanceExit("outfit", true)

    private fun appearanceExit(
        surface: String,
        fail: Boolean,
    ) = fixture(companion = if (surface == "colour") CompanionKind.CAT else CompanionKind.HUMAN) { fixture, model ->
        click("My Companion")
        if (surface == "clock") click("Clock styles")
        if (surface in listOf("colour", "outfit")) click("Choose an outfit")
        val committed = fixture.file.readBytes()
        val gate = fixture.arm(fail)
        when (surface) {
            "companion" -> clickTag("choose_companion_CAT")
            "clock" -> clickTag("choose_clock_CLASSIC")
            "colour" -> clickTag("color_BLUE")
            else -> click("Polka-dot dress")
        }
        await(gate.entered)
        assertArrayEquals(committed, fixture.file.readBytes())
        click("Back")
        if (surface in listOf("colour", "outfit")) click("Back")
        waitFor("Clocklings")
        gate.release.complete(Unit)
        await(gate.finished)
        compose.waitUntil(5_000) { model.settingsAction.value.failed == fail && fixture.matches(surface, !fail) }
        if (fail) assertArrayEquals(committed, fixture.file.readBytes())
        click("My Companion")
        if (surface == "clock") click("Clock styles")
        if (surface in listOf("colour", "outfit")) click("Choose an outfit")
        val selected = selectedAppearance(surface, fail)
        compose.waitUntil(5_000) {
            compose.onNode(selected).fetchSemanticsNode().config[SemanticsProperties.Selected]
        }
        fixture.reopenAndAssert()
    }

    private fun selectedAppearance(
        surface: String,
        fail: Boolean,
    ) = when (surface) {
        "companion" -> hasTestTag("choose_companion_${if (fail) "HUMAN" else "CAT"}")
        "clock" -> hasTestTag("choose_clock_${if (fail) "RAINBOW" else "CLASSIC"}")
        "colour" -> hasTestTag("color_${if (fail) "ORIGINAL" else "BLUE"}")
        else -> hasText(if (fail) "Yellow dress" else "Polka-dot dress") and hasClickAction()
    }

    @Test
    fun practicePendingExitCancelsAndDelayedFailureCanRetry() =
        fixture { fixture, _ ->
            openPractice()
            finishPracticeTasks(9)
            val committed = fixture.file.readBytes()
            val cancelled = fixture.arm(false)
            finishPracticeTasks(1, start = 9)
            await(cancelled.entered)
            compose.onNode(hasText("Whole hours complete!", substring = true)).assertDoesNotExist()
            click("Back")
            await(cancelled.cancelled)
            assertArrayEquals(committed, fixture.file.readBytes())
            assertEquals(
                0,
                runBlocking {
                    fixture.repository.progress
                        .first()
                        .stars[0]
                },
            )
            openPractice(fromLevels = true)
            finishPracticeTasks(9)
            val failure = fixture.arm(true)
            finishPracticeTasks(1, start = 9)
            retryFailedWrite(fixture, failure, committed, "Whole hours complete!", substring = true)
            assertTrue(
                runBlocking {
                    fixture.repository.progress
                        .first()
                        .stars[0] > 0
                },
            )
            fixture.reopenAndAssert()
        }

    @Test
    fun challengePendingExitCancelsAndDelayedFailureCanRetry() =
        fixture { fixture, _ ->
            click("Play")
            val committed = fixture.file.readBytes()
            val cancelled = fixture.arm(false)
            completeChallenge()
            await(cancelled.entered)
            click("Back")
            await(cancelled.cancelled)
            assertArrayEquals(committed, fixture.file.readBytes())
            assertEquals(
                0,
                runBlocking {
                    fixture.repository.progress
                        .first()
                        .challengeBest
                },
            )
            val failure = fixture.arm(true)
            completeChallenge()
            retryFailedWrite(fixture, failure, committed, "Play again")
            assertEquals(
                1,
                runBlocking {
                    fixture.repository.progress
                        .first()
                        .challengeBest
                },
            )
            fixture.reopenAndAssert()
        }

    private fun retryFailedWrite(
        fixture: WriteFixture,
        failure: WriteGate,
        committed: ByteArray,
        result: String,
        substring: Boolean = false,
    ) {
        await(failure.entered)
        failure.release.complete(Unit)
        waitFor(context.getString(R.string.settings_save_error))
        assertArrayEquals(committed, fixture.file.readBytes())
        click(context.getString(R.string.settings_retry))
        waitFor(result, substring = substring)
    }

    private fun completeChallenge() {
        compose.mainClock.autoAdvance = true
        click("Minute Challenge")
        compose.mainClock.autoAdvance = false
        click("Start!")
        // Read the actual offered labels until a correct answer reveals the clock's time.
        val answers = (hasText("past", true) or hasText("to", true) or hasText("o'clock", true)) and hasClickAction()
        compose.waitUntil(5_000) { compose.onAllNodes(answers).fetchSemanticsNodes().isNotEmpty() }
        while (true) {
            val labels =
                compose
                    .onAllNodes(
                        answers,
                    ).fetchSemanticsNodes()
                    .map { it.config[SemanticsProperties.Text].first().text }
            click(labels.first())
            // The score is available as displayed text; the clock description reveals the answer after any pick.
            val words =
                compose
                    .onNode(hasContentDescription("Clock face:", true))
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
                    .first {
                        it.startsWith(
                            "Clock face:",
                        )
                    }.removePrefix("Clock face: ")
            // A wrong answer advances to another random question. Keep choosing until one score is committed.
            if (labels.first().lowercase() == words) break
            Thread.sleep(1_500)
            compose.mainClock.advanceTimeBy(500)
        }
    }

    private fun openPractice(fromLevels: Boolean = false) {
        if (!fromLevels) click("Play")
        click("Whole hours")
        if (compose.onAllNodes(action("Skip and play")).fetchSemanticsNodes().isNotEmpty()) click("Skip and play")
    }

    private fun finishPracticeTasks(
        count: Int,
        start: Int = 0,
    ) {
        repeat(count) { index ->
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasTestTag("game_task_${start + index}")).fetchSemanticsNodes().isNotEmpty()
            }
            val clock = hasContentDescription("Clock face:", substring = true)
            if (compose.onAllNodes(hasText("What time is it?")).fetchSemanticsNodes().isNotEmpty()) {
                val choices =
                    compose
                        .onAllNodes(hasText("o'clock", true) and hasClickAction())
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].first().text }
                for (choice in choices) {
                    click(choice)
                    if (compose.onAllNodes(clock).fetchSemanticsNodes().isNotEmpty()) break
                }
                if (compose.onAllNodes(action("Next")).fetchSemanticsNodes().isNotEmpty()) click("Next")
            } else {
                val target =
                    compose
                        .onAllNodes(hasText("o'clock", true))
                        .fetchSemanticsNodes()
                        .first()
                        .config[SemanticsProperties.Text]
                        .first()
                        .text
                        .lowercase()
                repeat(12) {
                    val words =
                        compose
                            .onNode(clock)
                            .fetchSemanticsNode()
                            .config[SemanticsProperties.ContentDescription]
                            .first { it.startsWith("Clock face:") }
                            .removePrefix("Clock face: ")
                    if (words !=
                        target
                    ) {
                        compose.onNode(clock).performCustomAccessibilityActionWithLabel("Add 60\u00A0minutes")
                    }
                }
                click("Done")
            }
        }
    }

    private fun fixture(
        companion: CompanionKind = CompanionKind.HUMAN,
        block: (WriteFixture, AppViewModel) -> Unit,
    ) {
        val fixture = WriteFixture()
        val services =
            object : AppServices by (context.applicationContext as ClocklingsApplication).container {
                override val repository = fixture.repository
            }
        val model = AppViewModel(services)
        try {
            runBlocking {
                fixture.repository.updateSettings {
                    Settings(
                        sound = false,
                        personalization = Personalization(companion = companion),
                    )
                }
                fixture.repository.updateProgress { Progress().withLevelResult(1, 1) }
            }
            compose.setContent { ClocklingsApp(container = services, app = model) }
            block(fixture, model)
        } finally {
            model.viewModelScope.cancel()
            fixture.close()
        }
    }

    private inner class WriteFixture : AutoCloseable {
        val file = files.newFolder().resolve("remaining.preferences_pb")
        private val job = SupervisorJob()
        private val delegate = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }

        @Volatile private var next: WriteGate? = null
        val repository =
            ProgressRepository(
                object : DataStore<Preferences> {
                    override val data = delegate.data

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                        val gate = next
                        next = null
                        try {
                            gate?.entered?.complete(Unit)
                            gate?.release?.await()
                            if (gate?.fail == true) throw IOException("Synthetic delayed remaining-write failure")
                            return delegate.updateData(transform)
                        } catch (error: CancellationException) {
                            gate?.cancelled?.complete(Unit)
                            throw error
                        } finally {
                            gate?.finished?.complete(Unit)
                        }
                    }
                },
            )

        fun arm(fail: Boolean): WriteGate = WriteGate(fail).also { next = it }

        fun matches(
            surface: String,
            saved: Boolean,
        ): Boolean =
            runBlocking {
                val settings = repository.settings.first().personalization
                when (surface) {
                    "companion" -> {
                        settings.companion == if (saved) CompanionKind.CAT else CompanionKind.HUMAN
                    }

                    "clock" -> {
                        settings.clock.theme == if (saved) ClockTheme.CLASSIC else ClockTheme.RAINBOW
                    }

                    "colour" -> {
                        settings.styleFor(CompanionKind.CAT).color ==
                            if (saved) CompanionColor.BLUE else CompanionColor.ORIGINAL
                    }

                    else -> {
                        repository.progress.first().outfitFor(CompanionKind.HUMAN)[Slot.DRESS] ==
                            if (saved) "dress-pilkku" else "dress-perus"
                    }
                }
            }

        fun reopenAndAssert() {
            val progress = runBlocking { repository.progress.first() }
            val settings = runBlocking { repository.settings.first() }
            close()
            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    ProgressRepository(
                        PreferenceDataStoreFactory.create(
                            scope =
                                CoroutineScope(reopenedJob + Dispatchers.IO),
                        ) { file },
                    )
                assertEquals(progress, runBlocking { reopened.progress.first() })
                assertEquals(settings, runBlocking { reopened.settings.first() })
            } finally {
                runBlocking { reopenedJob.cancelAndJoin() }
            }
        }

        override fun close() = runBlocking { job.cancelAndJoin() }
    }

    private class WriteGate(
        val fail: Boolean,
    ) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
    }

    private fun await(signal: CompletableDeferred<Unit>) {
        compose.waitUntil(70_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            signal.isCompleted
        }
        runBlocking { withTimeout(1_000) { signal.await() } }
    }

    private fun action(label: String) =
        (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction()

    private fun click(label: String) {
        waitFor(label)
        compose.onAllNodes(action(label))[0].scrollToIfScrollable().performClick()
        if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeBy(1_000) else compose.waitForIdle()
    }

    private fun clickTag(tag: String) = compose.onNode(hasTestTag(tag)).scrollToIfScrollable().performClick()

    private fun waitFor(
        label: String,
        substring: Boolean = false,
    ) {
        compose.waitUntil(10_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            compose
                .onAllNodes(
                    hasText(label, substring) or hasContentDescription(label, true),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
