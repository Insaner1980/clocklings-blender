package fi.clocklings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameViewModel
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Render the real failure states and exercise their retry controls on the dedicated test AVD. */
@RunWith(AndroidJUnit4::class)
class SaveFailureUiTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<ClocklingsApplication>()
    private val language = mutableStateOf(AppLanguage.ENGLISH)
    private val viewport = mutableStateOf(411 to 1f)
    private val screen = mutableIntStateOf(0)
    private var retries = 0
    private val game by lazy {
        GameViewModel(0, false, app.container).state.value.copy(
            busy = true,
            showNext = true,
            feedback = GameFeedback.SaveFailed,
        )
    }

    @Test
    fun saveErrorsAndRetryControlsRemainReadableInEveryLanguageAndDisplaySize() {
        compose.setContent { FailureContent() }
        for (selectedLanguage in AppLanguage.entries) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                for (selectedScreen in 0..2) {
                    compose.runOnIdle {
                        language.value = selectedLanguage
                        viewport.value = cell
                        screen.intValue = selectedScreen
                    }
                    assertFailureAndRetry()
                }
            }
        }
    }

    @Composable
    private fun FailureContent() {
        val context =
            app.createConfigurationContext(
                Configuration(app.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(language.value.tag))
                    fontScale = viewport.value.second
                },
            )
        CompositionLocalProvider(
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalDensity provides Density(2f, viewport.value.second),
            LocalAppLanguage provides language.value,
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) { FailureScreen() }
            }
        }
    }

    @Composable
    private fun FailureScreen() {
        when (screen.intValue) {
            0 -> {
                GameScreen(game, true, {}, {}, {}, {}, {}, { retries++ })
            }

            1 -> {
                val lesson = LessonCatalog.lesson("viisarit", language.value)
                LessonScreen(
                    state =
                        LessonUiState(
                            lesson = lesson,
                            page = lesson.pages.size,
                            choices = lesson.quiz.choices,
                            clock = ClockCommand(lesson.quiz.time),
                            saveFailed = true,
                        ),
                    skipToGame = false,
                    speechAvailable = false,
                    onBack = {},
                    onNext = {},
                    onPrevious = {},
                    onReplay = {},
                    onPick = { retries++ },
                    onSpeak = {},
                    onRetrySave = { retries++ },
                )
            }

            else -> {
                ChallengeScreen(
                    state = ChallengeUiState(phase = ChallengePhase.END, score = 3, best = 2, saveFailed = true),
                    h24 = false,
                    help = true,
                    onBack = {},
                    onStart = { retries++ },
                    onPick = {},
                )
            }
        }
    }

    private fun assertFailureAndRetry() {
        val context =
            app.createConfigurationContext(
                Configuration(app.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(language.value.tag))
                },
            )
        val error = context.getString(R.string.progress_save_error)
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(error)
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Error text has a layout", layouts.isNotEmpty())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            LocalizationScreenshots.capture(
                "save-${language.value.tag}-${viewport.value.first}-${viewport.value.second}-${screen.intValue}",
                bitmap,
            )
            assertFalse("Capture must preserve its caller's bitmap", bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
        assertReadableText(layouts, error)
        if (screen.intValue == 1) {
            val previous = context.getString(R.string.lesson_prev)
            val navigationLayouts = mutableListOf<TextLayoutResult>()
            compose
                .onNodeWithText(previous)
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(navigationLayouts) }
            assertReadableText(navigationLayouts, previous)
        }
        val label = context.getString(R.string.settings_retry)
        val before = retries
        compose
            .onNodeWithText(label)
            .scrollToIfScrollable()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(before + 1, retries) }
    }

    private fun assertReadableText(
        layouts: List<TextLayoutResult>,
        text: String,
    ) {
        val case = "${language.value.tag}/${viewport.value}/${screen.intValue}"
        layouts.forEach { layout ->
            assertFalse("$case: text height overflows", layout.didOverflowHeight)
            for (line in 0 until layout.lineCount) {
                assertFalse("$case: text is ellipsized", layout.isLineEllipsized(line))
                // A wrap-content node can retain a wider paragraph; measure its actual rendered lines.
                val width = layout.getLineRight(line) - layout.getLineLeft(line)
                assertTrue("$case: rendered width=$width, node=${layout.size}", width <= layout.size.width + 1f)
                if (line < layout.lineCount - 1) {
                    val end = layout.getLineEnd(line)
                    assertTrue(
                        "$case: text splits a word: $text",
                        end !in 1 until text.length || text[end - 1].isWhitespace() ||
                            text[end - 1] == '-' || text[end].isWhitespace(),
                    )
                }
            }
        }
    }
}
