package fi.clocklings.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.components.ConfettiState
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.provider.Settings as SystemSettings

@RunWith(RobolectricTestRunner::class)
class ChallengeInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun remainingTimeIsExposedToAccessibilityAcrossLanguagesAndDisplaySizes() {
        val cell = mutableStateOf(Triple(360, 1f, AppLanguage.ENGLISH))
        val state = mutableStateOf(ChallengeUiState())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LocalizedTestViewport(cell.value) {
                ChallengeScreen(state.value, false, false, {}, {}, {})
            }
        }
        val timer = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        for (language in AppLanguage.entries) {
            for ((width, fontScale) in listOf(360 to 1f, 320 to 1f, 360 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    cell.value = Triple(width, fontScale, language)
                    state.value = ChallengeUiState()
                }
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeByFrame()
                    timer.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 1f
                }
                val initial = timer.assertIsDisplayed().fetchSemanticsNode().config
                assertTrue(initial[SemanticsProperties.ContentDescription].single().isNotBlank())
                assertEquals(1f, initial[SemanticsProperties.ProgressBarRangeInfo].current)
                compose.runOnIdle {
                    state.value =
                        ChallengeUiState(
                            phase = ChallengePhase.RUN,
                            deadlineNanos = System.nanoTime() + 30_000_000_000L,
                        )
                }
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeByFrame()
                    timer.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current < 1f
                }
                val running = timer.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
                val latestFraction = state.value.remainingFraction(System.nanoTime())
                assertTrue(
                    "$language / $width / $fontScale: ${running.current} must be within $latestFraction..0.5",
                    running.current in latestFraction..0.5f,
                )
                compose.runOnIdle { state.value = state.value.copy(phase = ChallengePhase.END) }
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeByFrame()
                    timer.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 0f
                }
                assertEquals(0f, timer.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
            }
        }
    }

    @Test
    fun challengeStartActionStaysVisibleWithoutScrollingAtNarrowWidthAndLargeFont() {
        val viewport = mutableStateOf(320 to 2f)
        var starts = 0
        compose.setContent {
            TestViewport(viewport.value.first, viewport.value.second) {
                ChallengeScreen(ChallengeUiState(), false, false, {}, { starts++ }, {})
            }
        }
        for (cell in listOf(320 to 2f, 360 to 2f, 320 to 1f, 360 to 1f)) {
            compose.runOnIdle { viewport.value = cell }
            compose.onNodeWithText("Start!").assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(4, starts) }
    }

    @Test
    fun challengeClockGrowsIntoTheStartBarRoomWithoutJumpingAtStart() {
        val viewport = mutableStateOf(360 to 1f)
        val state = mutableStateOf(ChallengeUiState())
        compose.setContent {
            TestViewport(viewport.value.first, viewport.value.second) {
                ChallengeScreen(state.value, false, false, {}, {}, {})
            }
        }

        fun frames(count: Int) =
            repeat(count) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
            }

        fun clockWidth() =
            compose
                .onNodeWithTag("challenge_clock", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.width

        for (cell in listOf(360 to 1f, 320 to 2f)) {
            compose.runOnIdle {
                viewport.value = cell
                state.value = ChallengeUiState(phase = ChallengePhase.RUN)
            }
            val run = clockWidth()
            compose.runOnIdle { state.value = ChallengeUiState() }
            val start = clockWidth()
            assertTrue("$cell: the start bar takes height from the clock", start < run)
            compose.mainClock.autoAdvance = false
            compose.runOnIdle { state.value = ChallengeUiState(phase = ChallengePhase.RUN) }
            // Frame by frame with a layout pass after each, as on a device; the clock size is known only in layout.
            frames(SkMotion.RESIZE_DURATION / 2 / FRAME_MS)
            val halfway = clockWidth()
            assertTrue("$cell: the clock grows gradually, $halfway", halfway > start + 1f && halfway < run - 1f)
            frames(SkMotion.RESIZE_DURATION * 2 / FRAME_MS)
            assertEquals("$cell: settled RUN size", run, clockWidth(), 1f)
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun withReducedMotionTheFittedClockGoesStraightToItsRunSizeAndKeepsItThroughTheResult() {
        val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        SystemSettings.Global.putFloat(resolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            val cell = mutableStateOf(Triple(360, 1f, AppLanguage.FINNISH))
            val state = mutableStateOf(ChallengeUiState())
            compose.setContent {
                // A fresh screen per cell, so the first Start has no measured run panel yet.
                key(cell.value) {
                    LocalizedTestViewport(cell.value) {
                        ChallengeScreen(state.value, false, false, {}, {}, {})
                    }
                }
            }

            fun clockWidth() =
                compose
                    .onNodeWithTag("challenge_clock", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .size.width

            // Each frame with a layout pass after it, as on a device; the fitted size is known only in layout.
            fun widthsWhile(change: () -> Unit): List<Int> {
                compose.mainClock.autoAdvance = false
                compose.runOnIdle(change)
                val widths =
                    List(FRAMES_AFTER_CHANGE) {
                        compose.mainClock.advanceTimeByFrame()
                        compose.waitForIdle()
                        clockWidth()
                    }
                compose.mainClock.autoAdvance = true
                return widths
            }
            for (next in listOf(Triple(360, 1f, AppLanguage.FINNISH), Triple(320, 2f, AppLanguage.ENGLISH))) {
                compose.runOnIdle {
                    cell.value = next
                    state.value = ChallengeUiState()
                }
                val start = clockWidth()
                val run = ChallengeUiState(phase = ChallengePhase.RUN, options = longAnswers(next.third, false, 3))
                val atStart = widthsWhile { state.value = run }
                val fitted = atStart.last()
                assertTrue("$next: the run fits a different size, $start and $fitted", fitted != start)
                val onlyTwo = atStart.all { it == start || it == fitted }
                assertTrue("$next: only the START or the RUN size, $atStart", onlyTwo)
                val atEnd = widthsWhile { state.value = run.copy(phase = ChallengePhase.END) }
                assertTrue("$next: the result keeps the run size, $atEnd", atEnd.all { it == fitted })
                val again = widthsWhile { state.value = run.copy(runId = run.runId + 1) }
                assertTrue("$next: Play again keeps the run size, $again", again.all { it == fitted })
            }
        } finally {
            SystemSettings.Global.putFloat(resolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    @Test
    fun challengeRunFitsTheWholeOptionsPanelAt360x640AndKeepsTheFirstAnswerVisibleAtLargeFont() {
        val cell = mutableStateOf(Triple(360, 1f, AppLanguage.ENGLISH))
        val state = mutableStateOf(ChallengeUiState())
        compose.setContent {
            LocalizedTestViewport(cell.value) {
                ChallengeScreen(state.value, false, false, {}, {}, {})
            }
        }
        val ledge = with(compose.density) { SkDimens.OptionDepth.toPx() }

        // Unclipped tops and bottoms: an answer below the fold must not count as one clipped to nothing.
        fun answers(options: List<Int>) =
            options.map { t ->
                val node =
                    compose
                        .onNode(hasClickAction() and hasText(timeLabel(cell.value.third, t, false)))
                        .fetchSemanticsNode()
                node.positionInRoot.y to node.positionInRoot.y + node.size.height
            }

        fun clockAndFirstAnswer(options: List<Int>) =
            compose
                .onNodeWithTag("challenge_clock", useUnmergedTree = true)
                .fetchSemanticsNode()
                .size.width to
                answers(options).minOf { it.first }
        for (language in AppLanguage.entries) {
            // The three longest answers a challenge can show in this language make about the tallest options panel.
            val options = longAnswers(language, false, 3)
            for ((width, fontScale) in listOf(360 to 1f, 320 to 2f)) {
                val label = "$language $width dp x $fontScale"
                compose.runOnIdle {
                    cell.value = Triple(width, fontScale, language)
                    state.value = ChallengeUiState()
                }
                // Start, then let the clock resize settle.
                compose.runOnIdle { state.value = ChallengeUiState(phase = ChallengePhase.RUN, options = options) }
                compose.waitForIdle()
                val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
                if (fontScale == 1f) {
                    val bottom = answers(options).maxOf { it.second } + ledge
                    assertTrue("$label: answers end at $bottom, viewport ${viewport.bottom}", bottom <= viewport.bottom)
                } else {
                    val top = answers(options).minOf { it.first }
                    assertTrue("$label: first answer at $top, viewport at ${viewport.bottom}", top < viewport.bottom)
                }
                // The next question has the shortest answers; neither the clock nor the answers move.
                val before = clockAndFirstAnswer(options)
                val short = SHORT_ANSWERS
                compose.runOnIdle { state.value = state.value.copy(options = short, question = short.first()) }
                compose.waitForIdle()
                assertEquals("$label: same layout for every question", before, clockAndFirstAnswer(short))
            }
        }
    }

    @Test
    fun challengeRunOpensAtTheTopAfterAScrolledStartOrResult() {
        val state = mutableStateOf(ChallengeUiState())
        compose.setContent {
            TestViewport(320, 2f) {
                ChallengeScreen(state.value, false, false, {}, {}, {})
            }
        }
        val scroller = compose.onNode(hasScrollAction())

        fun scroll() = scroller.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        val run = ChallengeUiState(phase = ChallengePhase.RUN, options = listOf(60, 90, 135))
        // START is the intro before the first run; END is the result before Play again.
        for (before in listOf(ChallengeUiState(), run.copy(phase = ChallengePhase.END))) {
            compose.runOnIdle { state.value = before }
            scroller.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, FAR) }
            assertTrue("${before.phase} is scrolled", scroll() > 0f)
            compose.runOnIdle { state.value = run.copy(runId = before.runId + 1) }
            assertEquals("RUN after ${before.phase}", 0f, scroll())
        }
    }

    @Test
    fun challengeResultKeepsItsActionsInTheBottomBarAndTheScoreInViewWithoutScrolling() {
        val cell = mutableStateOf(Triple(360, 1f, AppLanguage.ENGLISH))
        val state = mutableStateOf(ChallengeUiState())
        var played = 0
        var left = 0
        compose.setContent {
            key(cell.value) {
                // 576 dp is a 360 x 640 dp phone without its status and navigation bars.
                LocalizedTestViewport(cell.value, height = RESULT_HEIGHT) {
                    ChallengeScreen(state.value, false, false, { left++ }, { played++ }, {})
                }
            }
        }
        val height = with(compose.density) { RESULT_HEIGHT.dp.toPx() }

        for (language in AppLanguage.entries) {
            val cells =
                listOf(Triple(360, 1f, false), Triple(320, 2f, false), Triple(360, 1f, true), Triple(320, 2f, true))
            for ((width, fontScale, record) in cells) {
                val label = "$language $width dp x $fontScale record=$record"
                val context = localizedContext(language)
                val again = context.getString(R.string.play_again)
                val back = context.getString(R.string.back_description)
                val run = ChallengeUiState(phase = ChallengePhase.RUN, options = longAnswers(language, false, 3))
                compose.runOnIdle {
                    cell.value = Triple(width, fontScale, language)
                    state.value = run
                }
                compose.waitForIdle()
                compose.mainClock.autoAdvance = false
                compose.runOnIdle {
                    state.value = run.copy(phase = ChallengePhase.END, score = SCORE, best = BEST, newRecord = record)
                }
                assertScoreStaysPut(label)
                assertResultVisible(label, language, fontScale, record, height)
                compose.onNodeWithText(again).performClick()
                compose.onNodeWithText(back).performClick()
            }
        }
        compose.runOnIdle { assertEquals(AppLanguage.entries.size * 4, played) }
        compose.runOnIdle { assertEquals(AppLanguage.entries.size * 4, left) }
    }

    @Test
    fun recordConfettiUsesTheScoreOrTheVisibleBarWhenTheScoreIsFullyClipped() {
        val cell = mutableStateOf(Triple(360, RESULT_HEIGHT, 1f))
        val state = mutableStateOf(ChallengeUiState())
        val confetti = ConfettiState(1f, enabled = true)
        compose.setContent {
            val (width, height, fontScale) = cell.value
            val base = LocalDensity.current
            key(cell.value) {
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density, fontScale),
                    LocalConfetti provides confetti,
                ) {
                    ClocklingsTheme {
                        Box(Modifier.requiredSize(width.dp, height.dp)) {
                            ChallengeScreen(state.value, false, false, {}, {}, {})
                        }
                    }
                }
            }
        }
        for (c in listOf(Triple(360, RESULT_HEIGHT, 1f), Triple(320, 420, 2f))) {
            compose.runOnIdle {
                confetti.clear()
                cell.value = c
                state.value = ChallengeUiState(phase = ChallengePhase.RUN, options = SHORT_ANSWERS)
            }
            compose.waitForIdle()
            compose.runOnIdle {
                state.value = state.value.copy(phase = ChallengePhase.END, score = SCORE, newRecord = true)
            }
            val score = compose.onNodeWithText(SCORE.toString()).fetchSemanticsNode().boundsInRoot
            val back = compose.onNodeWithText("Back").fetchSemanticsNode().boundsInRoot
            val again = compose.onNodeWithText("Play again").fetchSemanticsNode().boundsInRoot
            assertEquals("$c: short cell must exercise a fully clipped score", c.third == 2f, score.isEmpty)
            val expected =
                if (score.isEmpty) {
                    Offset(
                        (back.left + back.right) / 2,
                        (minOf(back.top, again.top) + maxOf(back.bottom, again.bottom)) / 2,
                    )
                } else {
                    score.center
                }
            compose.runOnIdle {
                assertTrue("$c: record celebration fired", confetti.particles.isNotEmpty())
                assertTrue("$c: origin $expected", confetti.particles.all { it.x == expected.x && it.y == expected.y })
            }
        }
    }

    @Composable
    private fun TestViewport(
        width: Int,
        fontScale: Float,
        height: Int = 640,
        content: @Composable () -> Unit,
    ) {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(width.dp, height.dp)) { content() }
            }
        }
    }

    @Composable
    private fun LocalizedTestViewport(
        cell: Triple<Int, Float, AppLanguage>,
        height: Int = 640,
        content: @Composable () -> Unit,
    ) {
        val (width, fontScale, language) = cell
        val context = localizedContext(language)
        CompositionLocalProvider(
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides language,
        ) {
            TestViewport(width, fontScale, height, content)
        }
    }

    private fun assertScoreStaysPut(label: String) {
        // From its first frame the score does not move while the bar appears, so the record confetti starts
        // on it.
        val scoreTops =
            List(FRAMES_AFTER_CHANGE) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
                compose
                    .onAllNodesWithText(SCORE.toString())
                    .fetchSemanticsNodes()
                    .firstOrNull()
                    ?.boundsInRoot
            }.mapNotNull { it?.top }
        compose.mainClock.autoAdvance = true
        val still = scoreTops.size > FRAMES_AFTER_CHANGE / 2 && scoreTops.all { it == scoreTops.first() }
        assertTrue("$label: the score stays put, $scoreTops", still)
    }

    private fun assertResultVisible(
        label: String,
        language: AppLanguage,
        fontScale: Float,
        record: Boolean,
        height: Float,
    ) {
        val context = localizedContext(language)
        val again = context.getString(R.string.play_again)
        val back = context.getString(R.string.back_description)

        fun top(text: String) =
            compose
                .onNodeWithText(text)
                .fetchSemanticsNode()
                .boundsInRoot.top

        fun score() = compose.onNodeWithText(SCORE.toString()).fetchSemanticsNode().boundsInRoot
        // Both actions are fully on screen without scrolling, in the bar below the scrolling result.
        for (action in listOf(again, back)) {
            val bounds =
                compose
                    .onNodeWithText(action)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertTrue("$label: $action ends at ${bounds.bottom} of $height", bounds.bottom <= height)
        }
        val barTop = minOf(top(again), top(back))
        if (fontScale == 1f) {
            // The bar starts a small gap above its buttons; the score's line box ends above it.
            val barEdge = barTop - with(compose.density) { SkDimens.GapS.toPx() }
            val bottom = score().bottom
            assertTrue("$label: score ends at $bottom, bar at $barEdge", bottom <= barEdge)
            val headline =
                context.getString(if (record) R.string.challenge_new_best else R.string.challenge_over)
            val title =
                compose
                    .onNodeWithText(headline, ignoreCase = true)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            assertTrue("$label: headline above the bar", title.bottom <= barEdge)
        } else {
            compose.onNodeWithText(SCORE.toString()).scrollIfNeeded().assertIsDisplayed()
        }
    }
}

/** One frame of the test clock. */
private const val FRAME_MS = 16

/** A scroll distance past the end of any screen. */
private const val FAR = 100_000f

/** Frames watched after a phase change: more than a resize takes, with the layout pass that fits it. */
private const val FRAMES_AFTER_CHANGE = 30

/** One, two and three o'clock: one short line in every language. */
private val SHORT_ANSWERS = listOf(60, 120, 180)

/** A result that is not a new record, with its longer sentence. */
private const val SCORE = 12
private const val BEST = 15
