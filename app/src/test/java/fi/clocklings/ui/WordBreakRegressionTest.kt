package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Lesson
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Levels
import fi.clocklings.domain.Progress
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.TimeReadout
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.NextAction
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h718dp")
class WordBreakRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun noMidWordBreaksAtLargeFont() {
        val language = mutableStateOf(AppLanguage.FINNISH)
        val exploreTime = mutableStateOf<Int?>(null)
        val levels = mutableStateOf(false)
        compose.setContent { WordBreakScreenFixture(language.value, exploreTime.value, levels.value) }
        for (selected in AppLanguage.entries) {
            compose.runOnIdle {
                language.value = selected
                exploreTime.value = null
                levels.value = false
            }
            val res = localizedContext(selected).resources
            val factors =
                compose
                    .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                    .fetchSemanticsNodes()
                    .flatMap { it.config[SemanticsProperties.Text] }
                    .firstNotNullOf { Regex("([0-9]{2}) × ([0-9])").find(it.text) }
                    .groupValues
            val product = factors[1].toInt() * factors[2].toInt()
            compose.onNode(hasSetTextAction()).performTextReplacement(product.toString())
            compose.onNode(hasSetTextAction()).performImeAction()
            assertWordBreaks(res.getString(R.string.full_unlock_name))
            for (time in listOf(15 * 60, 18 * 60 + 15)) {
                compose.runOnIdle { exploreTime.value = time }
                assertWordBreaks(timeLabel(selected, time, h24 = true))
            }
            compose.runOnIdle { levels.value = true }
            (Levels.all.map { levelName(it.index) } + R.string.challenge_title).forEach {
                assertWordBreaks(res.getString(it))
            }
        }
    }

    @Composable
    private fun WordBreakScreenFixture(
        language: AppLanguage,
        exploreTime: Int?,
        showLevels: Boolean,
    ) {
        val context = localizedContext(language)
        CompositionLocalProvider(
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides language,
            LocalDensity provides Density(1f, 2f),
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredWidth(320.dp)) {
                    key(language) {
                        when {
                            showLevels -> {
                                LevelsScreen(Progress(), AccessPolicy(true), {}, {}, {}, {})
                            }

                            exploreTime != null -> {
                                ScreenScaffold {
                                    FlatPanel {
                                        TimeReadout(
                                            exploreTime,
                                            h24 = true,
                                            speechAvailable = true,
                                            onSpeak = {},
                                        )
                                    }
                                }
                            }

                            else -> {
                                ParentScreen(PurchaseUiState(), {}, {}, {}, {})
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h824dp")
    fun lessonActionsDoNotBreakWords() {
        val language = mutableStateOf(AppLanguage.FINNISH)
        val viewport = mutableStateOf(411 to 1f)
        val state = mutableStateOf(LessonUiState(LessonCatalog.lesson("viisi", language.value)))
        compose.setContent { LessonActionFixture(language.value, viewport.value, state.value) }
        val failures =
            AppLanguage.entries.flatMap { selected ->
                lessonActionFailures(selected) { cell, lessonState ->
                    compose.runOnIdle {
                        language.value = selected
                        viewport.value = cell
                        state.value = lessonState
                    }
                }
            }
        assertTrue("Mid-word breaks:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Composable
    private fun LessonActionFixture(
        language: AppLanguage,
        viewport: Pair<Int, Float>,
        state: LessonUiState,
    ) {
        val context = localizedContext(language)
        val (width, fontScale) = viewport
        CompositionLocalProvider(
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides language,
            LocalDensity provides Density(1f, fontScale),
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(width.dp, if (width == 320) 718.dp else 824.dp)) {
                    key(language, viewport, state.lesson.id, state.page) {
                        LessonScreen(state, false, true, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
    }

    private fun lessonActionFailures(
        language: AppLanguage,
        show: (Pair<Int, Float>, LessonUiState) -> Unit,
    ): List<String> {
        val failures = mutableListOf<String>()
        val resources = localizedContext(language).resources
        val actions =
            (listOf("viisi") + Lessons.levelLessons.flatten() + Lessons.DAY_LESSON)
                .distinct()
                .flatMap { lessonActionStates(LessonCatalog.lesson(it, language)) }
        for (cell in listOf(320 to 2f, 320 to 1f, 411 to 2f, 411 to 1f)) {
            for ((lessonState, label) in actions) {
                show(cell, lessonState)
                val text = resources.getString(label)
                if (breaksInsideWord(text)) {
                    failures += "$language $cell ${lessonState.lesson.id} page ${lessonState.page + 1}: '$text'"
                }
            }
        }
        return failures
    }

    private fun lessonActionStates(lesson: Lesson): List<Pair<LessonUiState, Int>> {
        val quiz =
            LessonUiState(
                lesson,
                page = lesson.pages.size,
                quizDone = true,
                nextAction = NextAction.FINISH,
            )
        return lesson.pages.mapIndexed { index, page ->
            val last = index == lesson.pages.lastIndex
            LessonUiState(
                lesson,
                page = index,
                clock = ClockCommand(page.time),
                nextAction = if (last) NextAction.TRY_IT else NextAction.NEXT_PAGE,
            ) to if (last) R.string.lesson_try else R.string.next
        } +
            listOf(
                quiz to R.string.done,
                quiz.copy(nextAction = NextAction.START_GAME) to R.string.lesson_start_game,
                quiz.copy(nextAction = NextAction.NEXT_LESSON) to R.string.lesson_next_lesson,
                quiz.copy(quizDone = false, saveFailed = true) to R.string.settings_retry,
            )
    }

    private fun assertWordBreaks(text: String) {
        assertTrue("Mid-word break in '$text'", !breaksInsideWord(text))
    }

    private fun breaksInsideWord(text: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        return (0 until layout.lineCount - 1).any { line ->
            val end = layout.getLineEnd(line)
            end in 1 until text.length &&
                !text[end - 1].isWhitespace() &&
                text[end - 1] != '-' &&
                !text[end].isWhitespace()
        }
    }
}
