package fi.clocklings

import android.content.res.Configuration
import android.os.LocaleList
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.NextAction
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resolve
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.timeLabel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockSurfaceStateTest {
    @get:Rule val compose = createComposeRule()
    private var evidencePrefix = "clock-surface"
    private val lessonLayoutCases =
        listOf(
            "puoli" to 2,
            "puoli" to 3,
            "viisi" to 2,
            "puoli" to 4,
            "viisarit" to 2,
            "ylivaille" to 0,
            "ylivaille" to 1,
            "ylivaille" to 2,
        )

    private fun localizedContexts(): Map<AppLanguage, android.content.Context> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return AppLanguage.entries.associateWith { language ->
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocales(LocaleList.forLanguageTags(language.tag))
            context.createConfigurationContext(configuration)
        }
    }

    @Test
    fun exactMinuteChoicesAndHalfHourLessonControlsRemainReadableInEveryLanguage() {
        val contexts = localizedContexts()
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val surface = mutableStateOf<Pair<String, Int>?>(null)
        val game = exactMinuteGame()
        compose.setContent {
            val localized = contexts.getValue(language.value)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalAppLanguage provides language.value,
            ) {
                ClocklingsTheme {
                    val lesson = surface.value?.let { lessonLayoutState(it, language.value) }
                    if (lesson == null) {
                        GameScreen(game, false, {}, {}, {}, {}, {}, {})
                    } else {
                        LessonScreen(lesson, false, true, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        AppLanguage.entries.forEach { value ->
            compose.runOnIdle {
                language.value = value
                surface.value = null
            }
            evidencePrefix = "${value.tag}-exact-minute"
            compose.waitForIdle()
            compose.waitForIdle()
            LocalizationScreenshots.capture("$evidencePrefix-clock-daypart")
            game.options.forEach { assertTextFits(timeLabel(value, it, true)) }
            for (case in lessonLayoutCases) {
                compose.runOnIdle { surface.value = case }
                evidencePrefix = "${value.tag}-${case.first}-page-${case.second}"
                val state = lessonLayoutState(case, value)
                val res = contexts.getValue(value).resources
                state.lesson.pages
                    .getOrNull(state.page)
                    ?.text
                    ?.forEach { assertTextFits(it.resolve(res)) }
                assertActionFits(res.getString(R.string.lesson_prev))
                val next =
                    when (state.nextAction) {
                        NextAction.TRY_IT -> R.string.lesson_try
                        NextAction.NEXT_LESSON -> R.string.lesson_next_lesson
                        else -> R.string.next
                    }
                assertActionFits(res.getString(next))
            }
        }
    }

    private fun exactMinuteGame() =
        GameUiState(
            level = 4,
            h24 = true,
            taskIndex = 0,
            taskCount = 10,
            task = Task(921, TaskType.READ),
            options = listOf(921, 939, 201),
            clock = ClockCommand(921),
            step = 1,
        )

    private fun lessonLayoutState(
        case: Pair<String, Int>,
        language: AppLanguage,
    ): LessonUiState {
        val lesson = LessonCatalog.lesson(case.first, language)
        val page = lesson.pages.getOrNull(case.second)
        return LessonUiState(
            lesson = lesson,
            page = case.second,
            clock = ClockCommand(page?.time ?: lesson.quiz.time),
            period = page?.period ?: lesson.quiz.period,
            under = page?.under.orEmpty(),
            choices = lesson.quiz.choices,
            quizDone = page == null,
            nextAction =
                when {
                    page == null -> NextAction.NEXT_LESSON
                    case.second == lesson.pages.lastIndex -> NextAction.TRY_IT
                    else -> NextAction.NEXT_PAGE
                },
        )
    }

    private fun assertActionFits(text: String) {
        assertTextFits(text)
        val node = compose.onNode(hasText(text) and hasClickAction()).scrollToIfScrollable()
        val bounds = node.getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        compose.waitForIdle()
        LocalizationScreenshots.capture("$evidencePrefix-footer-${text.hashCode()}")
        Log.i("ClocklingsLayout", "$evidencePrefix action=$text bounds=$bounds root=$root")
        node.assertIsDisplayed()
        assertTrue("Action extends outside screen: $text", bounds.left >= root.left && bounds.right <= root.right)
        assertTrue(
            "Action cannot scroll fully into view: $text",
            bounds.top >= root.top && bounds.bottom <= root.bottom,
        )
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onAllNodesWithText(text, useUnmergedTree = true)
            .onFirst()
            .scrollToIfScrollable()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        compose.waitForIdle()
        LocalizationScreenshots.capture("$evidencePrefix-text-${text.hashCode()}")
        assertTrue("Missing text layout: $text", layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertTrue("Text height overflows: $text", !layout.didOverflowHeight)
            for (line in 0 until layout.lineCount) {
                val left = layout.getLineLeft(line)
                val right = layout.getLineRight(line)
                val diagnostic =
                    "$evidencePrefix text=$text line=$line left=$left right=$right " +
                        "rendered=${right - left} node=${layout.size} paragraph=${layout.multiParagraph.width}"
                Log.i("ClocklingsLayout", diagnostic)
                assertTrue("Ellipsized teaching text: $text", !layout.isLineEllipsized(line))
                // Centered wrap-content Text retains coordinates from a wider paragraph.
                // Its visible line width must fit the node; paragraph offsets need not.
                assertTrue(
                    "Text extends outside its box: $diagnostic",
                    right - left <= layout.size.width + 1f,
                )
                val end = layout.getLineEnd(line)
                if (line < layout.lineCount - 1 && end in 1 until text.length) {
                    assertTrue(
                        "Word split: $text",
                        text[end - 1].isWhitespace() || text[end - 1] == '-' || text[end].isWhitespace(),
                    )
                }
            }
        }
    }

    @Test
    fun practiceKeepsItsAnswerHiddenAcrossLanguageChangesUntilRevealed() {
        val contexts = localizedContexts()
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val state =
            mutableStateOf(
                GameUiState(
                    level = 1,
                    h24 = false,
                    taskIndex = 0,
                    taskCount = 10,
                    task = Task(210, TaskType.READ),
                    options = listOf(210, 270, 330),
                    clock = ClockCommand(210),
                    step = 30,
                ),
            )
        compose.setContent {
            val localized = contexts.getValue(language.value)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalResources provides localized.resources,
                LocalConfiguration provides localized.resources.configuration,
                LocalAppLanguage provides language.value,
            ) {
                ClocklingsTheme {
                    GameScreen(state.value, false, {}, {}, {}, {}, {}, {})
                }
            }
        }
        for (value in AppLanguage.entries) {
            compose.runOnIdle { language.value = value }
            val res = contexts.getValue(value).resources
            val hidden = compose.onNodeWithContentDescription(res.getString(R.string.clock_question_description))
            hidden.assertExists()
            assertTrue(
                hidden
                    .fetchSemanticsNode()
                    .config
                    .getOrElse(SemanticsActions.CustomActions) { emptyList() }
                    .isEmpty(),
            )
            compose.onAllNodesWithText(res.getString(R.string.listen)).assertCountEquals(0)
            val answer = res.getString(R.string.clock_description, timeLanguage(value).fullWords(210, false))
            compose.onAllNodes(hasContentDescription(answer)).assertCountEquals(0)
        }
        compose.runOnIdle { state.value = state.value.copy(feedback = GameFeedback.RevealRead(210)) }
        for (value in AppLanguage.entries.reversed()) {
            compose.runOnIdle { language.value = value }
            val answer =
                contexts.getValue(value).resources.getString(
                    R.string.clock_description,
                    timeLanguage(value).fullWords(210, false),
                )
            compose.onNodeWithContentDescription(answer).assertExists()
        }
    }
}
