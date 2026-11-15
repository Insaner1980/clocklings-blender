package fi.clocklings

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Mark
import fi.clocklings.domain.MarkColor
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.Text
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.appearance.LocalCompanionOutfit
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.LessonViewModel
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.settings.LanguageScreen
import fi.clocklings.ui.settings.SettingsScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Actual device renders of deterministic states, at the device's configured width and font scale. */
class ScreenshotReviewGalleryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fiveMinuteLessonMovesAndReplayRestartsAtThree() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val services = (context.applicationContext as ClocklingsApplication).container
        lateinit var lesson: LessonViewModel
        val factory = viewModelFactory { initializer { LessonViewModel(listOf("viisi"), null, services) } }
        compose.setContent {
            lesson = viewModel(factory = factory)
            val state by lesson.state.collectAsStateWithLifecycle()
            ClocklingsTheme {
                LessonScreen(state, false, false, {}, lesson::next, lesson::previous, lesson::replay, lesson::pick, {})
            }
        }
        compose.waitUntil(5000) { lesson.state.value.caption == Text.Res(R.string.say_number_minutes, listOf(1, 5)) }
        assertEquals(185, lesson.state.value.clock.time)
        capture("live-five-minute-first-number")
        compose.runOnIdle {
            lesson.replay()
            assertEquals(180, lesson.state.value.clock.time)
            assertNull(lesson.state.value.caption)
        }
        capture("live-five-minute-replay")
        compose.waitUntil(5000) { lesson.state.value.caption == Text.Res(R.string.say_number_minutes, listOf(2, 10)) }
        assertEquals(190, lesson.state.value.clock.time)
    }

    @Test
    fun changedScreensInAllLanguages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val services = (context.applicationContext as ClocklingsApplication).container
        val factory = viewModelFactory { initializer { AppViewModel(services) } }
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val screen = mutableStateOf("home")
        val kind = mutableStateOf(CompanionKind.CAT)
        val progress = Progress(stars = listOf(3, 0, 0, 0, 0))
        val contexts =
            AppLanguage.entries.associateWith {
                context.createConfigurationContext(
                    Configuration(context.resources.configuration).apply {
                        setLocales(LocaleList.forLanguageTags(it.tag))
                    },
                )
            }
        compose.setContent {
            val app = viewModel<AppViewModel>(factory = factory)
            AppLocaleProvider(services.repository) {
                val localized = contexts.getValue(language.value)
                val personalization = Personalization(companion = kind.value)
                val settings = Settings(help = true, personalization = personalization)
                CompositionLocalProvider(
                    LocalAppViewModel provides app,
                    LocalContext provides localized,
                    LocalResources provides localized.resources,
                    LocalConfiguration provides localized.resources.configuration,
                    LocalAppLanguage provides language.value,
                    LocalPersonalization provides personalization,
                    LocalCompanionOutfit provides progress.outfitFor(kind.value),
                ) {
                    ClocklingsTheme {
                        key(language.value, screen.value, kind.value) {
                            ReviewScreen(screen.value, language.value, kind.value, progress, settings)
                        }
                    }
                }
            }
        }
        val requestedLanguage = InstrumentationRegistry.getArguments().getString("screenshotLanguage")
        val requestedScreen = InstrumentationRegistry.getArguments().getString("screenshotScreen")
        for (value in AppLanguage.entries.filter {
            requestedLanguage == null || it.tag in
                requestedLanguage.split(
                    ',',
                )
        }) {
            compose.runOnIdle { language.value = value }
            captureScreens(value, requestedScreen) { destination, companion ->
                compose.runOnIdle {
                    screen.value = destination
                    kind.value = companion
                }
            }
        }
    }

    @Composable
    private fun ReviewScreen(
        screen: String,
        language: AppLanguage,
        kind: CompanionKind,
        progress: Progress,
        settings: Settings,
    ) {
        when (screen) {
            "home" -> {
                HomeScreen(progress, settings, {})
            }

            "learn" -> {
                LessonListScreen(true, progress, fi.clocklings.domain.AccessPolicy(fullAccess = true), {}, {}, {})
            }

            "levels" -> {
                LevelsScreen(progress, fi.clocklings.domain.AccessPolicy(fullAccess = true), {}, {}, {}, {})
            }

            "explore" -> {
                FreeClockScreen(false, true, true, {})
            }

            "together" -> {
                AskScreen(false, true, true, {})
            }

            "wardrobe" -> {
                WardrobeScreen(kind, progress, CompanionColor.ORIGINAL, {})
            }

            "appearance" -> {
                AppearanceScreen(settings.personalization, progress, {}, {})
            }

            "settings" -> {
                SettingsScreen(settings, true, {}, {}, onParent = {})
            }

            "game", "game-wrong", "game-correct" -> {
                ReviewGame(screen)
            }

            else -> {
                ReviewExtraScreen(screen, language, kind, progress)
            }
        }
    }

    @Composable
    private fun ReviewGame(screen: String) {
        GameScreen(
            GameUiState(
                3,
                false,
                0,
                10,
                Task(10, TaskType.READ),
                listOf(10, 70, 130),
                clock = ClockCommand(10),
                step = 5,
                optionStates =
                    when (screen) {
                        "game-wrong" -> mapOf(70 to OptionState.WRONG)
                        "game-correct" -> mapOf(10 to OptionState.CORRECT)
                        else -> emptyMap()
                    },
                feedback =
                    when (screen) {
                        "game-wrong" -> GameFeedback.HintText(Hints.forRead(10, 70))
                        "game-correct" -> GameFeedback.Praise(0, 10)
                        else -> null
                    },
                showNext = screen == "game-correct",
            ),
            true,
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }

    @Composable
    private fun ReviewResult(
        screen: String,
        kind: CompanionKind,
        progress: Progress,
    ) {
        ResultScreen(
            AccessPolicy(fullAccess = true),
            Screen.Result(
                if (screen == "result-final") 4 else 0,
                if (screen == "result") 1 else 3,
                if (screen in listOf("result-final", "result-fallback")) 3 else 0,
                0,
            ),
            progress.outfitFor(kind),
            {},
            {},
            {},
            {},
            canReplay = screen != "result-fallback",
            canContinue = screen != "result-fallback",
        )
    }

    @Composable
    private fun ReviewExtraScreen(
        screen: String,
        language: AppLanguage,
        kind: CompanionKind,
        progress: Progress,
    ) {
        when (screen) {
            "language" -> {
                LanguageScreen({})
            }

            "parents" -> {
                ParentScreen(PurchaseUiState(fullAccess = true), {}, {}, {}, {})
            }

            "challenge" -> {
                ChallengeScreen(ChallengeUiState(), false, true, {}, {}, {})
            }

            "challenge-run" -> {
                ChallengeScreen(
                    ChallengeUiState(phase = ChallengePhase.RUN, options = listOf(180, 240, 420)),
                    false,
                    true,
                    {},
                    {},
                    {},
                )
            }

            "result", "result-many", "result-final", "result-fallback" -> {
                ReviewResult(screen, kind, progress)
            }

            else -> {
                LessonScreen(lessonState(screen, language), false, true, {}, {}, {}, {}, {}, {})
            }
        }
    }

    private fun captureScreens(
        language: AppLanguage,
        requestedScreen: String?,
        selectScreen: (String, CompanionKind) -> Unit,
    ) {
        for (destination in Screens.filter { requestedScreen == null || it in requestedScreen.split(',') }) {
            selectScreen(destination, CompanionKind.CAT)
            compose.waitForIdle()
            if (destination == "explore") {
                nudge(9)
                capture("${language.tag}-explore-0345")
                nudge(85)
                capture("${language.tag}-explore-1050")
            } else {
                capture("${language.tag}-$destination-top")
            }
            if (destination in
                ScrollableScreens
            ) {
                scrollToEnd()
                capture("${language.tag}-$destination-bottom")
            }
        }
        if (InstrumentationRegistry.getArguments().getString("screenshotCompanions") == "false") return
        if (requestedScreen != null && "wardrobe" !in requestedScreen.split(',')) return
        for (companion in CompanionKind.entries.filter { it != CompanionKind.NONE && it != CompanionKind.CAT }) {
            selectScreen("wardrobe", companion)
            capture("${language.tag}-wardrobe-${companion.name}-top")
            scrollToEnd()
            capture("${language.tag}-wardrobe-${companion.name}-bottom")
        }
        // The new companions on Home and in the picker, where the picker shows every companion.
        for (companion in CompanionKind.entries.filter { it != CompanionKind.CAT }) {
            selectScreen("home", companion)
            capture("${language.tag}-home-${companion.name}")
            selectScreen("appearance", companion)
            capture("${language.tag}-appearance-${companion.name}-top")
            scrollToEnd()
            capture("${language.tag}-appearance-${companion.name}-bottom")
        }
    }

    @Test
    fun clockClearanceAtEveryHourPositionAndBothHandStyles() {
        val time = mutableIntStateOf(0)
        val hands = mutableStateOf(HandStyle.ROUND)
        val h24 = mutableStateOf(false)
        compose.setContent {
            ClocklingsTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ClockFace(
                        ClockCommand(time.intValue),
                        Modifier.size(320.dp),
                        help = h24.value,
                        period = if (h24.value) 1440 else 720,
                        appearance = ClockAppearance(ClockTheme.CUSTOM, CustomClock(hands = hands.value)),
                    )
                }
            }
        }
        for (style in HandStyle.entries) {
            compose.runOnIdle { hands.value = style }
            val minutes = if (style == HandStyle.POINTER) (0..59).toList() else (0..55 step 5).toList()
            for (value in minutes + listOf(650, 715)) {
                compose.runOnIdle { time.intValue = value }
                capture("clock-${style.name}-$value")
            }
        }
        compose.runOnIdle {
            h24.value = true
            time.intValue = 885
        }
        capture("clock-inner-24")
    }

    private fun nudge(count: Int) {
        val actions =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                .onFirst()
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnIdle { repeat(count) { actions.first().action() } }
    }

    private fun scrollToEnd() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        if (nodes.fetchSemanticsNodes().isNotEmpty()) {
            nodes.onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 20000f) }
        }
    }

    private fun capture(name: String) {
        compose.onRoot().fetchSemanticsNode()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        Thread.sleep(500)
        val rendered =
            if (InstrumentationRegistry.getArguments().getString("screenshotScreen") != null) {
                compose.onRoot().captureToImage().asAndroidBitmap()
            } else {
                null
            }
        LocalizationScreenshots.capture("review-$name", rendered)
    }

    private fun lessonState(
        id: String,
        language: AppLanguage,
    ): LessonUiState {
        val lesson = LessonCatalog.lesson(id, language)
        val marks =
            when (id) {
                "vartti" -> {
                    listOf(
                        Mark.Sector(0, 15, MarkColor.RED),
                        Mark.Sector(15, 30, MarkColor.HALF),
                        Mark.Sector(30, 45, MarkColor.GREEN),
                        Mark.Sector(45, 60, MarkColor.BLUE),
                    )
                }

                "viisi" -> {
                    (1..5).map { Mark.NumberGlow(it) }
                }

                else -> {
                    (1..2).map { Mark.TickGlow(it) }
                }
            }
        val caption =
            when (id) {
                "vartti" -> Text.Res(R.string.l_vartti_say_four)
                "viisi" -> Text.Res(R.string.say_number_minutes, listOf(5, 25))
                else -> Text.Plural(R.plurals.minutes_count, 2)
            }
        return LessonUiState(
            lesson,
            clock = ClockCommand(if (id == "viisi") 205 else 180),
            under = marks,
            caption = caption,
            over = if (id == "viisi") (1..5).map { Mark.Bubble(it * 5, (it * 5).toString()) } else emptyList(),
        )
    }

    private companion object {
        val Screens =
            listOf(
                "home",
                "learn",
                "levels",
                "explore",
                "together",
                "game",
                "game-wrong",
                "game-correct",
                "result",
                "result-many",
                "result-final",
                "result-fallback",
                "challenge",
                "challenge-run",
                "parents",
                "language",
                "appearance",
                "vartti",
                "viisi",
                "minuutit",
                "wardrobe",
                "settings",
            )
        val ScrollableScreens =
            listOf(
                "home",
                "learn",
                "levels",
                "together",
                "wardrobe",
                "settings",
                "vartti",
                "viisi",
                "minuutit",
                "game-wrong",
                "game-correct",
                "result",
                "result-many",
                "result-final",
                "result-fallback",
                "challenge-run",
                "parents",
                "language",
                "appearance",
            )
    }
}
