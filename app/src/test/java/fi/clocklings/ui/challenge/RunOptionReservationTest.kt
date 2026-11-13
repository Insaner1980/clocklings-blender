package fi.clocklings.ui.challenge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Levels
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.localizedContext
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.timeLabel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A run keeps room for the tallest answers a challenge can show. This measures every challenge answer, then
 * shows the three tallest after a question with short answers: nothing may move, and at 100 % text the whole
 * panel must still fit unless the clock is at its minimum. Font scale 1.5 puts the time below the words.
 */
@RunWith(RobolectricTestRunner::class)
class RunOptionReservationTest {
    @get:Rule val compose = createComposeRule()

    private class Cell(
        val width: Int,
        val fontScale: Float,
        val language: AppLanguage,
        val h24: Boolean,
    )

    @Test
    fun theTallestChallengeAnswersFitTheRoomEveryRunKeeps() {
        val cell = mutableStateOf(Cell(360, 1f, AppLanguage.ENGLISH, false))
        val run = mutableStateOf(ChallengeUiState(phase = ChallengePhase.RUN, options = SHORT))
        // Null shows the screen; a width shows every challenge answer at that width, to measure them.
        val rowWidth = mutableStateOf<Int?>(null)
        // Every cell opens straight into a run, so each ranks its answers and measures its candidates itself.
        AnswerRooms.clear()
        compose.setContent {
            ReservationContent(cell.value, run.value, rowWidth.value)
        }
        val ledge = with(compose.density) { SkDimens.OptionDepth.toPx() }
        val failures = mutableListOf<String>()
        for (current in CELLS) {
            val width = current.width
            val fontScale = current.fontScale
            val language = current.language
            val h24 = current.h24
            val label = "$language h24=$h24 $width dp x $fontScale"
            compose.runOnIdle {
                cell.value = Cell(width, fontScale, language, h24)
                run.value = ChallengeUiState(phase = ChallengePhase.RUN, options = SHORT)
            }
            val answerWidth = answer(language, h24, SHORT.first()).size.width
            compose.runOnIdle { rowWidth.value = answerWidth }
            val tallest =
                compose
                    .onAllNodes(isRow)
                    .fetchSemanticsNodes()
                    .map { it.config[SemanticsProperties.TestTag].removePrefix(ROW).toInt() to it.size.height }
                    .sortedByDescending { it.second }
                    .take(SHORT.size)
                    .map { it.first }
            compose.runOnIdle { rowWidth.value = null }
            val clock = clockWidth()
            compose.runOnIdle { run.value = run.value.copy(options = tallest, question = tallest.first()) }
            if (clockWidth() != clock) failures += "$label: $tallest resize the clock"
            val viewport =
                compose
                    .onNode(hasScrollAction())
                    .fetchSemanticsNode()
                    .boundsInRoot.bottom
            val nodes = tallest.map { answer(language, h24, it) }
            val top = nodes.minOf { it.positionInRoot.y }
            val bottom = nodes.maxOf { it.positionInRoot.y + it.size.height } + ledge
            val atMinimum = with(compose.density) { clock <= SkDimens.FitClockMin.roundToPx() }
            when {
                top >= viewport -> {
                    failures += "$label: first answer at $top, viewport $viewport"
                }

                fontScale == 1f && !atMinimum && bottom > viewport -> {
                    failures += "$label: $tallest end at $bottom, viewport $viewport"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Composable
    private fun ReservationContent(
        current: Cell,
        run: ChallengeUiState,
        rowWidth: Int?,
    ) {
        val base = LocalDensity.current
        val context = localizedContext(current.language)
        CompositionLocalProvider(
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides current.language,
            LocalDensity provides Density(base.density, current.fontScale),
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(current.width.dp, 640.dp)) {
                    val width = rowWidth
                    if (width == null) {
                        ChallengeScreen(run, current.h24, false, {}, {}, {})
                    } else {
                        AllAnswers(current.h24, with(LocalDensity.current) { width.toDp() })
                    }
                }
            }
        }
    }

    private fun answer(
        language: AppLanguage,
        h24: Boolean,
        t: Int,
    ) = compose.onNode(hasClickAction() and hasText(timeLabel(language, t, h24))).fetchSemanticsNode()

    private fun clockWidth() =
        compose
            .onNodeWithTag("challenge_clock", useUnmergedTree = true)
            .fetchSemanticsNode()
            .size.width

    private companion object {
        val CELLS =
            listOf(false, true).flatMap { h24 ->
                AppLanguage.entries.flatMap { language ->
                    listOf(360 to 1f, 320 to 2f, 360 to 1.5f, 411 to 1f).map { (width, fontScale) ->
                        Cell(width, fontScale, language, h24)
                    }
                }
            }

        /** One, two and three o'clock: one short line in every language. */
        val SHORT = listOf(60, 120, 180)
        val isRow =
            SemanticsMatcher("challenge answer row") {
                it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(ROW) == true
            }
    }
}

@Composable
private fun AllAnswers(
    h24: Boolean,
    width: Dp,
) {
    Column(Modifier.width(width).verticalScroll(rememberScrollState())) {
        for (t in Levels.challengeTimes(h24)) {
            RunOption(t, h24, OptionState.NORMAL, {}, Modifier.testTag("$ROW$t"))
        }
    }
}

private const val ROW = "row_"
