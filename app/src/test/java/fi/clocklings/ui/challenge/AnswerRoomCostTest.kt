package fi.clocklings.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Levels
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.localizedContext
import fi.clocklings.ui.longAnswers
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A run measures its answer room without composing every answer: [ROOM_CANDIDATES] measured rows on START, only
 * the shown answers on the Start tap, at most the candidates plus the shown answers without START, and nothing
 * dropped on the first pick.
 */
@RunWith(RobolectricTestRunner::class)
class AnswerRoomCostTest {
    @get:Rule val compose = createComposeRule()

    private val rows = AnswerRowCounter()
    private val state = mutableStateOf(ChallengeUiState())
    private val run = ChallengeUiState(phase = ChallengePhase.RUN, options = longAnswers(LANGUAGE, H24, 3))
    private val firstPick =
        run.copy(optionStates = mapOf(run.options.first() to OptionState.CORRECT), busy = true)
    private val allAnswers = Levels.challengeTimes(H24).count()

    @Test
    fun continuousResizeRetainsOnlyRecentRoomsAndCanRebuildEvictedEstimates() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        val density = Density(2f, 1f)
        val keys = (640..839).map { AnswerRoomKey(AppLanguage.ENGLISH, false, 2f, 1f, 0, it) }
        compose.runOnIdle {
            AnswerRooms.clear()
            try {
                for (key in keys) {
                    AnswerRooms.rankNow(key, measurer, density)
                    AnswerRooms.storeHeights(key, listOf(48, 24))
                }
                val retained = keys.count { AnswerRooms.heights(it) != null }
                val candidates = keys.count { AnswerRooms.candidates(it) != null }
                assertTrue(
                    "200 resize keys retain $retained heights and $candidates candidate sets",
                    retained <= 16 && candidates <= 16,
                )
                val before = AnswerRooms.estimatesMade
                AnswerRooms.rankNow(keys.last(), measurer, density)
                assertEquals("Recent estimates are reused", before, AnswerRooms.estimatesMade)
                AnswerRooms.rankNow(keys.first(), measurer, density)
                assertEquals(
                    "Evicted estimates are recomputed",
                    before + Levels.challengeTimes(false).count(),
                    AnswerRooms.estimatesMade,
                )
            } finally {
                AnswerRooms.clear()
            }
        }
    }

    private fun show() {
        AnswerRooms.clear()
        compose.setContent {
            val context = localizedContext(LANGUAGE)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalAppLanguage provides LANGUAGE,
                LocalAnswerRowCounter provides rows,
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(360.dp, 640.dp)) {
                        ChallengeScreen(state.value, H24, false, {}, {}, {})
                    }
                }
            }
        }
    }

    /** Applies [change] and lets the screen settle, its first RUN frame included. */
    private fun settle(change: () -> Unit) {
        compose.runOnIdle(change)
        compose.waitForIdle()
    }

    private fun clockWidth() =
        compose
            .onNodeWithTag("challenge_clock", useUnmergedTree = true)
            .fetchSemanticsNode()
            .size.width

    @Test
    fun startMeasuresTheCandidatesThenTheRunComposesOnlyTheShownAnswers() {
        state.value = ChallengeUiState()
        show()
        compose.waitForIdle()
        assertEquals("START measures the candidates", ROOM_CANDIDATES, rows.composed)
        assertEquals("and drops them before the run", ROOM_CANDIDATES, rows.disposed)
        assertEquals(allAnswers, AnswerRooms.estimatesMade)
        settle { state.value = run }
        assertEquals("Start composes the shown answers only", ROOM_CANDIDATES + run.options.size, rows.composed)
        settle { state.value = firstPick }
        assertEquals("the first pick drops nothing", ROOM_CANDIDATES, rows.disposed)
    }

    @Test
    fun aRunOpenedWithoutStartComposesAtMostTheCandidatesAndTheShownAnswers() {
        state.value = run
        show()
        compose.waitForIdle()
        assertTrue("${rows.composed} rows", rows.composed <= ROOM_CANDIDATES + run.options.size)
        settle { state.value = firstPick }
        assertEquals("the first pick drops nothing", 0, rows.disposed)
        // A later question reuses the shown rows, and the measured rows stay until the run ends: nothing new.
        val composed = rows.composed
        settle { state.value = run.copy(options = longAnswers(LANGUAGE, H24, 6).takeLast(3)) }
        assertEquals(composed, rows.composed)
    }

    @Test
    fun startDuringTheRankingFinishesItWithoutRankingAnyAnswerTwice() {
        state.value = ChallengeUiState()
        // A paused clock, so setting the content does not run the whole ranking at once.
        compose.mainClock.autoAdvance = false
        show()
        var frames = 0
        while (AnswerRooms.estimatesMade == 0 && frames++ < MAX_FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            compose.onRoot().fetchSemanticsNode()
        }
        val ranked = AnswerRooms.estimatesMade
        assertTrue("START ranked part of the answers, $ranked of $allAnswers", ranked in 1 until allAnswers)
        compose.mainClock.autoAdvance = true
        settle { state.value = run }
        assertEquals("every answer ranked exactly once", allAnswers, AnswerRooms.estimatesMade)
        assertTrue("${rows.composed} rows", rows.composed <= ROOM_CANDIDATES + run.options.size)
        val early = clockWidth()
        // The same run after a START that finished its preparation keeps the same room.
        settle {
            AnswerRooms.clear()
            state.value = ChallengeUiState()
        }
        settle { state.value = run.copy(runId = run.runId + 1) }
        assertEquals(early, clockWidth())
    }

    @Test
    fun boldTextIsPartOfTheKey() {
        val keys = mutableListOf<AnswerRoomKey>()
        val bold = mutableStateOf(false)
        compose.setContent {
            val base = LocalConfiguration.current
            val configuration =
                if (bold.value) Configuration(base).apply { fontWeightAdjustment = BOLD_ADJUSTMENT } else base
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                keys += rememberAnswerRoomKey(H24)(360)
            }
        }
        compose.runOnIdle { bold.value = true }
        compose.waitForIdle()
        assertEquals(BOLD_ADJUSTMENT, keys.last().fontWeightAdjustment)
        assertNotEquals(keys.first(), keys.last())
    }

    private companion object {
        val LANGUAGE = AppLanguage.FINNISH
        const val H24 = true
        const val MAX_FRAMES = 50
        const val BOLD_ADJUSTMENT = 300
    }
}
