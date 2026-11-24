package fi.clocklings.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.AppServices
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.DemoStep
import fi.clocklings.domain.Hint
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.companion.NameField
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.NextAction
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ReviewRegressionTest : ReviewTaskRegressionCases() {
    private val store = ReviewWriteStore()
    private val repository = ProgressRepository(store)
    private val services =
        object : AppServices by FakeServices(fullAccess = true) {
            override val repository = this@ReviewRegressionTest.repository
        }
    override val model = AppViewModel(services)

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun lockedCompanionTapShowsRequirement() = checkLockedCompanion(fullAccess = false)

    @Test
    @Config(qualifiers = "w411dp-h440dp")
    fun lockedCompanionTapInFullVersionWithoutStars() = checkLockedCompanion(fullAccess = true, height = 440)

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun lockedCompanionActionVisibleAfterTap() {
        val settings = Settings(personalization = Settings().personalization.copy(companion = CompanionKind.CAT))
        val app = AppViewModel(FakeServices(fullAccess = false))
        compose.setContent {
            Viewport(pixelHeight()) {
                CompositionLocalProvider(LocalAppViewModel provides app) {
                    key(viewport.value) { AppearanceScreen(settings.personalization, Progress(), {}, {}) }
                }
            }
        }
        val context = localizedContext(language.value)
        val requirement = compose.onNodeWithText(context.getString(R.string.companion_access_requirement))
        val parent = compose.onNode(hasText(context.getString(R.string.parent_title)) and hasClickAction())
        for (width in listOf(411, 320)) {
            compose.runOnIdle { viewport.value = width to 1f }
            compose.onNodeWithTag("choose_companion_DOG").scrollIfNeeded().performClick()
            for (node in listOf(requirement, parent)) {
                val bounds = node.fetchSemanticsNode()
                val clear = scrollBottom() - SkDimens.ScrollFade.value - bounds.positionInRoot.y - bounds.size.height
                assertTrue("$width dp, ${bounds.config}: clearance $clear", clear >= -1f)
            }
        }
    }

    private fun checkLockedCompanion(
        fullAccess: Boolean,
        height: Int = 914,
    ) {
        val fake = FakeServices(fullAccess)
        runBlocking {
            fake.repository.updateSettings {
                it.copy(
                    personalization = it.personalization.copy(companion = CompanionKind.CAT),
                )
            }
        }
        val app = AppViewModel(fake)
        compose.setContent {
            Viewport(height) {
                CompositionLocalProvider(LocalAppViewModel provides app) {
                    val settings by app.settings.collectAsStateWithLifecycle()
                    settings?.let { AppearanceScreen(it.personalization, Progress(), {}, {}) }
                }
            }
        }
        val context = localizedContext(AppLanguage.ENGLISH)
        val fullVersionRequirement = context.getString(R.string.companion_access_requirement)
        val requirement = if (fullAccess) "Earn 3 stars to unlock this companion." else fullVersionRequirement
        val dogTag = "choose_companion_DOG"
        if (height < SkDimens.CompactTaskHeight.value) {
            val thumbnailRow =
                compose.onNode(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange) and
                        hasAnyDescendant(hasTestTag(dogTag)),
                    useUnmergedTree = true,
                )
            thumbnailRow.performScrollTo().assertIsDisplayed()
        }
        val thumbnail = compose.onNodeWithTag(dogTag)
        thumbnail.scrollIfNeeded().assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(requirement)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(requirement).scrollIfNeeded().assertIsDisplayed()
        val parent = compose.onNode(hasText(context.getString(R.string.parent_title)) and hasClickAction())
        if (fullAccess) {
            parent.assertDoesNotExist()
            compose.onNodeWithText(fullVersionRequirement).assertDoesNotExist()
        } else {
            parent.assertIsDisplayed()
        }
        compose.runOnIdle {
            assertEquals(
                CompanionKind.CAT,
                app.settings.value!!
                    .personalization.companion,
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun everyCompanionResultFitsRewardAction() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        compose.setContent {
            Viewport(914) {
                CompositionLocalProvider(
                    LocalPersonalization provides LocalPersonalization.current.copy(companion = kind.value),
                ) {
                    ResultScreen(
                        AccessPolicy(true),
                        Screen.Result(0, 3, 0, 0),
                        Outfit.default(kind.value),
                        {},
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        val context = localizedContext(AppLanguage.ENGLISH)
        val outfitAction = compose.onNode(hasText(context.getString(R.string.dress_doll)) and hasClickAction())
        val replay = compose.onNode(hasText(context.getString(R.string.play_again)) and hasClickAction())
        for (companion in CompanionKind.entries) {
            compose.runOnIdle { kind.value = companion }
            replay.assertIsDisplayed()
            compose
                .onNodeWithContentDescription(
                    context.resources.getQuantityString(R.plurals.result_stars_description, 3, 3),
                ).assertIsDisplayed()
            if (companion == CompanionKind.NONE) {
                outfitAction.assertDoesNotExist()
                compose.onNodeWithText(context.getString(R.string.result_title_3)).assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.next_level)).assertIsDisplayed()
            } else {
                val action = outfitAction.fetchSemanticsNode()
                val clear = scrollBottom() - SkDimens.ScrollFade.value - action.positionInRoot.y - action.size.height
                assertTrue("$companion reward action clearance: $clear dp", clear >= -1f)
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h718dp")
    fun largeFontTaskShowsFirstOption() {
        viewport.value = TestViewports.last()
        val quiz = mutableStateOf<String?>("vartti")
        val answers = listOf(120, 180, 240)
        compose.setContent {
            Viewport(718) {
                val id = quiz.value
                if (id != null) {
                    val lesson = LessonCatalog.lesson(id, language.value)
                    val state = LessonUiState(lesson, page = lesson.pages.size, choices = lesson.quiz.choices)
                    LessonScreen(state, false, false, {}, {}, {}, {}, {}, {})
                } else {
                    val state = ChallengeUiState(ChallengePhase.RUN, score = 7, options = answers)
                    ChallengeScreen(state, false, true, {}, {}, {})
                }
            }
        }
        for (selected in listOf(AppLanguage.FINNISH, AppLanguage.GERMAN)) {
            compose.runOnIdle { language.value = selected }
            val res = localizedContext(selected).resources
            for (id in Lessons.levelLessons.flatten() + Lessons.DAY_LESSON) {
                compose.runOnIdle { quiz.value = id }
                val lesson = LessonCatalog.lesson(id, selected)
                val title = compose.onNode(hasText(lesson.title.resolve(res)) and isHeading())
                val layouts = mutableListOf<TextLayoutResult>()
                title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                assertTrue("$selected $id title", layout.lineCount <= 2 && !layout.didOverflowHeight)
                val first =
                    compose
                        .onNodeWithText(
                            lesson.quiz.choices
                                .first()
                                .text
                                .resolve(res),
                        ).fetchSemanticsNode()
                val clearance = scrollBottom() - SkDimens.ScrollFade.value - first.positionInRoot.y
                assertTrue("$selected $id first quiz choice clearance: $clearance dp", clearance > 0f)
            }
            compose.runOnIdle { quiz.value = null }
            compose.onNode(hasText(res.getString(R.string.challenge_title)) and isHeading()).assertIsDisplayed()
            compose.onNode(hasText(timeLabel(selected, answers.last(), false)) and hasClickAction()).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h718dp")
    fun challengeTimerKeepsWidthAtLargeFont() {
        viewport.value = TestViewports.last()
        compose.setContent {
            Viewport(718) {
                val state = ChallengeUiState(ChallengePhase.RUN, score = 7, options = listOf(120, 180, 240))
                ChallengeScreen(state, false, true, {}, {}, {})
            }
        }
        val context = localizedContext(language.value)
        compose.onNodeWithContentDescription(context.getString(R.string.challenge_score, 7)).assertIsDisplayed()
        val timer = compose.onNodeWithTag("challenge_timer", useUnmergedTree = true).fetchSemanticsNode()
        val row = compose.onNodeWithTag("task_progress_row", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(
            "Timer width ${timer.size.width}, progress row ${row.size.width}",
            timer.size.width * 2 >= row.size.width,
        )
    }

    @Test
    @Config(qualifiers = "w320dp-h718dp")
    fun challengeWrongAnswerShowsCorrectOption() {
        language.value = AppLanguage.FINNISH
        viewport.value = 320 to 2f
        val answers = listOf(120, 180, 240)
        val run = ChallengeUiState(ChallengePhase.RUN, options = answers)
        val state = mutableStateOf(run.copy(question = 180, clock = ClockCommand(180)))
        compose.setContent {
            Viewport(718) {
                ChallengeScreen(state.value, false, true, {}, {}, { pick ->
                    val correct = state.value.question
                    state.value =
                        state.value.copy(
                            busy = true,
                            optionStates =
                                mapOf(
                                    pick to OptionState.WRONG,
                                    correct to OptionState.CORRECT,
                                ),
                        )
                })
            }
        }
        for (correct in answers.drop(1)) {
            compose.runOnIdle { state.value = run.copy(question = correct, clock = ClockCommand(correct)) }
            compose.onNodeWithText(timeLabel(language.value, answers.first(), false)).assertIsDisplayed().performClick()
            val option =
                compose
                    .onNode(
                        hasText(timeLabel(language.value, correct, false)) and hasClickAction(),
                    ).fetchSemanticsNode()
            val clearance = scrollBottom() - SkDimens.ScrollFade.value - option.positionInRoot.y - option.size.height
            assertTrue("Correct option $correct clearance: $clearance dp", clearance >= -1f)
            assertEquals(option.size.height.toFloat(), option.boundsInRoot.height, 1f)
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h718dp")
    fun challengeScoreVisibleInShortViewport() {
        viewport.value = TestViewports.last()
        compose.setContent {
            Viewport(718) { ChallengeScreen(ChallengeUiState(ChallengePhase.END, score = 7), false, false, {}, {}, {}) }
        }
        val score = compose.onNodeWithText("7", useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(score.size.height.toFloat(), score.boundsInRoot.height, 1f)
        val playAgain = compose.onNodeWithText("Play again").fetchSemanticsNode()
        assertTrue(score.positionInRoot.y + score.size.height <= playAgain.positionInRoot.y)
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun challengeResultWithoutCompanionShowsClock() {
        compose.setContent {
            Viewport(914) {
                val none = LocalPersonalization.current.copy(companion = CompanionKind.NONE)
                CompositionLocalProvider(LocalPersonalization provides none) {
                    ChallengeScreen(ChallengeUiState(ChallengePhase.END, score = 7), false, false, {}, {}, {})
                }
            }
        }
        val clock = compose.onNodeWithTag("challenge_result_clock").fetchSemanticsNode()
        val score = compose.onNodeWithText("7", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(clock.boundsInRoot.bottom <= score.boundsInRoot.top)
        assertEquals(null, clock.config.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun namePlaceholderHidesWhileFocused() {
        val state = TextFieldState()
        compose.setContent { Viewport { NameField(state) } }
        val context = localizedContext(AppLanguage.ENGLISH)
        val placeholder =
            compose.onNodeWithText(
                context.getString(R.string.doll_name_placeholder),
                useUnmergedTree = true,
            )
        val field = compose.onNodeWithContentDescription(context.getString(R.string.doll_name_label))
        placeholder.assertIsDisplayed()
        field.performClick()
        placeholder.assertDoesNotExist()
        field.assertIsDisplayed()
    }

    @Test
    fun resultPrimaryActionIsLast() {
        compose.setContent {
            Viewport {
                ResultScreen(
                    AccessPolicy(true),
                    Screen.Result(0, 3, 0, 0),
                    Outfit.default(CompanionKind.HUMAN),
                    onPlayAgain = {},
                    onNextLevel = {},
                    onDress = {},
                    onLevels = {},
                )
            }
        }
        val replay = compose.onNodeWithText("Play again").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithText("Next level").fetchSemanticsNode().boundsInRoot
        assertTrue("Next level is below Play again", next.top >= replay.bottom)
    }

    @Test
    fun finishedQuizActionNamesItsDestination() {
        val lesson = LessonCatalog.lesson("viisarit", AppLanguage.ENGLISH)
        val quiz = LessonUiState(lesson, page = lesson.pages.size, choices = lesson.quiz.choices)
        val state = mutableStateOf(quiz)
        val destinations =
            listOf(
                NextAction.START_GAME to R.string.lesson_start_game,
                NextAction.NEXT_LESSON to R.string.lesson_next_lesson,
                NextAction.FINISH to R.string.done,
            )
        var action: NextAction? = null
        compose.setContent {
            Viewport {
                LessonScreen(state.value, false, false, {}, { action = state.value.nextAction }, {}, {}, {}, {})
            }
        }
        val context = localizedContext(AppLanguage.ENGLISH)
        compose.onNodeWithText(context.getString(R.string.done)).assertIsNotEnabled()
        for ((destination, label) in destinations) {
            compose.runOnIdle { state.value = quiz.copy(quizDone = true, nextAction = destination) }
            compose.onNodeWithText(context.getString(label)).performClick()
            assertEquals(destination, action)
        }
    }

    @Test
    fun failedNameDraftKeepsItsRetryAfterSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                WardrobeScreen(CompanionKind.CAT, progress ?: Progress(), CompanionColor.ORIGINAL, {})
            }
        }
        store.fail = true
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Draft")
        compose.onNodeWithText(retryText()).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
            assertEquals(errorText(), nameError())
        }
        compose
            .onNodeWithContentDescription(localizedContext(language.value).getString(R.string.doll_name_label))
            .scrollIfNeeded()
            .assertTextEquals("Draft")
        assertEquals("", model.progress.value!!.nameFor(CompanionKind.CAT))
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle { assertEquals("Draft", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        compose.onNodeWithText(retryText()).assertDoesNotExist()
        assertEquals(null, nameError())
    }

    @Test
    fun failedNameRemainsRetryableAfterAnotherChangeSucceeds() {
        compose.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                WardrobeScreen(CompanionKind.CAT, progress ?: Progress(), CompanionColor.ORIGINAL, {})
            }
        }
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Mia")
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.runOnIdle { assertEquals("", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        // A successful colour change must not hide the still-unsaved name or its retry action.
        store.fail = false
        compose.runOnIdle { model.setColor(CompanionKind.CAT, CompanionColor.BLUE) }
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle { assertEquals("Mia", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText(retryText()).assertDoesNotExist()
    }

    @Test
    fun appearanceSaveFailureIsVisibleAndRetrySavesTheChoice() {
        showAppearanceScreen()
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText("Clock styles").performClick()
        compose.onNodeWithTag("choose_clock_CUSTOM").scrollIfNeeded().performClick()
        store.fail = false
        compose.runOnIdle { model.setName(CompanionKind.CAT, "Mia") }
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.runOnIdle {
            val theme = requireNotNull(model.settings.value).personalization.clock.theme
            assertEquals(ClockTheme.CUSTOM, theme)
        }
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText(retryText()).assertDoesNotExist()
    }

    @Test
    fun wardrobeColorAndItemFailuresKeepTheirOwnRetries() {
        val item = Wardrobe.itemsFor(CompanionKind.CAT).first { it.star == 0 }
        val headItem = Wardrobe.itemsFor(CompanionKind.CAT).first { it.star == 1 }
        compose.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                val settings by model.settings.collectAsStateWithLifecycle()
                WardrobeScreen(
                    CompanionKind.CAT,
                    progress ?: Progress(),
                    (settings ?: Settings()).personalization.styleFor(CompanionKind.CAT).color,
                    {},
                )
            }
        }
        compose.runOnIdle { model.updateProgress { it.withLevelResult(0, 3) } }
        store.fail = true
        compose.onNodeWithTag("color_BLUE").scrollIfNeeded().performClick()
        val itemText = localizedContext(language.value).getString(itemName(item.id))
        compose.onNodeWithText(itemText).scrollIfNeeded().performClick()
        val headText = localizedContext(language.value).getString(itemName(headItem.id))
        compose.onNodeWithText(headText).scrollIfNeeded().performClick()
        assertEquals(null, nameError())
        store.fail = false
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Mia")
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            val color = requireNotNull(model.settings.value).personalization.styleFor(CompanionKind.CAT).color
            assertEquals(CompanionColor.BLUE, color)
            assertEquals(false, requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT).isOn(item))
        }
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            val outfit = requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT)
            assertEquals(true, outfit.isOn(headItem))
            assertEquals(false, outfit.isOn(item))
        }
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            assertEquals(true, requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT).isOn(item))
        }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
    }

    @Test
    fun companionChoiceCanRetryWithoutMovingToAnotherPage() {
        showAppearanceScreen()
        store.fail = true
        compose.onNodeWithTag("choose_companion_CAT").scrollIfNeeded().performClick()
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(CompanionKind.HUMAN, requireNotNull(model.settings.value).personalization.companion)
        }
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(CompanionKind.CAT, requireNotNull(model.settings.value).personalization.companion)
        }
    }

    @Test
    fun lessonHintTakesPriorityOverAnEarlierSaveFailure() {
        compose.setContent {
            Viewport {
                val lesson = LessonCatalog.lesson("viisarit", language.value)
                LessonScreen(
                    LessonUiState(
                        lesson = lesson,
                        page = lesson.pages.size,
                        choices = lesson.quiz.choices,
                        saveFailed = true,
                        showHint = true,
                    ),
                    false,
                    false,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        forEachCell {
            val lesson = LessonCatalog.lesson("viisarit", language.value)
            val hint = lesson.quiz.hint.resolve(localizedContext(language.value).resources)
            compose.onNodeWithText(hint).scrollIfNeeded().assertIsDisplayed()
            val saveError = localizedContext(language.value).getString(R.string.progress_save_error)
            compose.onNodeWithText(saveError).assertDoesNotExist()
        }
    }

    private fun showAppearanceScreen() {
        compose.setContent {
            Viewport {
                val settings by model.settings.collectAsStateWithLifecycle()
                AppearanceScreen((settings ?: Settings()).personalization, Progress(), {}, {})
            }
        }
    }

    private fun forEachCell(assertion: () -> Unit) {
        for (selectedLanguage in listOf(AppLanguage.ENGLISH, AppLanguage.FINNISH, AppLanguage.SWEDISH)) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    language.value = selectedLanguage
                    viewport.value = cell
                }
                assertion()
            }
        }
    }

    private fun errorText() = localizedContext(language.value).getString(R.string.settings_save_error)

    private fun nameError() =
        compose
            .onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Error)

    private fun retryText() = localizedContext(language.value).getString(R.string.settings_retry)
}

abstract class ReviewTaskRegressionCases {
    @get:Rule val compose = createComposeRule()
    protected val viewport = mutableStateOf(411 to 1f)
    protected val language = mutableStateOf(AppLanguage.ENGLISH)
    protected abstract val model: AppViewModel

    @Test
    @Config(qualifiers = "w1000dp-h914dp")
    fun lessonPageNavigationStartsAtTheTop() {
        val lesson = LessonCatalog.lesson("viisi", AppLanguage.ENGLISH)
        val pages = lesson.pages.take(2).map { it.copy(text = List(8) { _ -> it.text.first() }, demo = emptyList()) }
        val page = mutableIntStateOf(0)
        val height = mutableIntStateOf(718)
        compose.setContent {
            Viewport(height.intValue) {
                val state = LessonUiState(lesson.copy(pages = pages), page = page.intValue, demoFinished = true)
                LessonScreen(state, false, false, {}, {}, {}, {}, {}, {})
            }
        }
        val scroll =
            compose.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
                useUnmergedTree = true,
            )
        for ((width, fontScale) in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f, 1000 to 2f)) {
            compose.runOnIdle {
                viewport.value = width to fontScale
                height.intValue = if (width == 1000) 360 else 718
            }
            for (destination in listOf(1, 0)) {
                scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100f) }
                val before = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
                assertTrue("$width dp at ${fontScale}x: the source page is scrolled", before > 0f)
                compose.runOnIdle { page.intValue = destination }
                val after = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
                assertEquals("$width dp at ${fontScale}x: page $destination starts at the top", 0f, after, 0f)
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun lessonClockStaysStillWhenDemoFinishes() {
        language.value = AppLanguage.FINNISH
        val lesson = LessonCatalog.lesson("kierros", language.value)
        val start = LessonUiState(lesson, demoFinished = false)
        val end =
            start.copy(
                demoFinished = true,
                caption =
                    lesson.pages
                        .first()
                        .demo
                        .filterIsInstance<DemoStep.Say>()
                        .last()
                        .text,
            )
        val state = mutableStateOf(start)
        compose.setContent { Viewport(914) { LessonScreen(state.value, false, false, {}, {}, {}, {}, {}, {}) } }
        val clock = compose.onNodeWithTag("task_clock", useUnmergedTree = true)
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, TestViewports.last())) {
            compose.runOnIdle {
                viewport.value = cell
                state.value = start
            }
            val before = clock.fetchSemanticsNode()
            val top = before.positionInRoot.y
            val width = before.size.width
            compose.runOnIdle { state.value = end }
            val after = clock.fetchSemanticsNode()
            assertEquals("$cell dial top", top, after.positionInRoot.y, 0.5f)
            assertEquals("$cell dial width", width, after.size.width)
        }
    }

    @Test
    fun lessonReservedTextIsSilent() {
        val lesson = LessonCatalog.lesson("kierros", AppLanguage.ENGLISH)
        val page = lesson.pages.first()
        val caption =
            page.demo
                .filterIsInstance<DemoStep.Say>()
                .last()
                .text
        val state = mutableStateOf(LessonUiState(lesson, demoFinished = false))
        compose.setContent { Viewport { LessonScreen(state.value, false, false, {}, {}, {}, {}, {}, {}) } }
        val res = localizedContext(AppLanguage.ENGLISH).resources
        val paragraphNode = compose.onNodeWithText(page.text.last().resolve(res))
        val captionNode = compose.onNodeWithText(caption!!.resolve(res))
        paragraphNode.assertDoesNotExist()
        captionNode.assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(demoFinished = true, caption = caption) }
        paragraphNode.assertExists()
        captionNode.assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h824dp")
    fun fiveMinutePageOneDialNotSmallerThanPageTwo() {
        language.value = AppLanguage.FINNISH
        val lesson = LessonCatalog.lesson("viisi", language.value)
        val page = mutableIntStateOf(0)
        compose.setContent {
            Viewport(824) {
                val data = lesson.pages[page.intValue]
                LessonScreen(
                    LessonUiState(lesson, page = page.intValue, clock = ClockCommand(data.time), demoFinished = true),
                    false,
                    true,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        val dial = compose.onNodeWithTag("task_clock", useUnmergedTree = true)
        val first = dial.fetchSemanticsNode().size.width
        compose.runOnIdle { page.intValue = 1 }
        val second = dial.fetchSemanticsNode().size.width
        val lineHeight = SkType.LessonText.fontSize.value * SkType.LessonText.lineHeight.value
        assertTrue("Page 1: $first dp, page 2: $second dp", first >= second - lineHeight)
    }

    @Test
    @Config(qualifiers = "w411dp-h824dp")
    fun everyLessonPageShowsPlaybackAtDefaultSize() {
        language.value = AppLanguage.FINNISH
        val pages =
            (Lessons.levelLessons.flatten() + Lessons.DAY_LESSON).flatMap { id ->
                val lesson = LessonCatalog.lesson(id, language.value)
                lesson.pages.mapIndexed { index, page ->
                    LessonUiState(
                        lesson,
                        page = index,
                        clock = ClockCommand(page.time),
                        caption =
                            page.demo
                                .filterIsInstance<DemoStep.Say>()
                                .lastOrNull()
                                ?.text,
                        nextAction = if (index == lesson.pages.lastIndex) NextAction.TRY_IT else NextAction.NEXT_PAGE,
                    )
                }
            }
        val state = mutableStateOf(pages.first())
        compose.setContent {
            Viewport(if (viewport.value.first == 320) 718 else 824) {
                key(viewport.value, state.value.lesson.id, state.value.page) {
                    LessonScreen(state.value, false, true, {}, {}, {}, {}, {}, {})
                }
            }
        }
        val context = localizedContext(language.value)
        val failures = mutableListOf<String>()
        for (page in pages) {
            compose.runOnIdle { state.value = page }
            val viewportBottom = scrollBottom()
            val scrollEnd = viewportBottom - SkDimens.ScrollFade.value
            for (id in listOf(R.string.listen, R.string.lesson_replay)) {
                val button = compose.onNodeWithContentDescription(context.getString(id)).fetchSemanticsNode()
                val bottom = button.positionInRoot.y + button.size.height
                val inActions = button.positionInRoot.y >= viewportBottom && bottom <= 824f
                if (!inActions && bottom > scrollEnd + 1f) {
                    val description = context.getString(id)
                    failures += "${page.lesson.id} page ${page.page + 1}: $description by ${bottom - scrollEnd} dp"
                }
            }
        }
        assertTrue(failures.joinToString(), failures.isEmpty())
    }

    @Test
    @Config(qualifiers = "w411dp-h824dp")
    fun fiveMinutePagePlaybackFollowsLayout() {
        language.value = AppLanguage.FINNISH
        val lesson = LessonCatalog.lesson("viisi", language.value)
        val page = lesson.pages.first()
        val start = LessonUiState(lesson, clock = ClockCommand(page.time), demoFinished = false)
        val end =
            start.copy(
                demoFinished = true,
                caption =
                    page.demo
                        .filterIsInstance<DemoStep.Say>()
                        .last()
                        .text,
            )
        val state = mutableStateOf(start)
        compose.setContent {
            Viewport(if (viewport.value.first == 320) 718 else 824) {
                key(viewport.value) { LessonScreen(state.value, false, true, {}, {}, {}, {}, {}, {}) }
            }
        }
        val context = localizedContext(language.value)
        for (cell in listOf(411 to 1f) + FeedbackViewports) {
            compose.runOnIdle {
                viewport.value = cell
                state.value = start
            }
            for (stage in listOf(start, end)) {
                compose.runOnIdle { state.value = stage }
                checkPlaybackControls(context, cell, stage.demoFinished)
            }
        }
    }

    private fun checkPlaybackControls(
        context: Context,
        cell: Pair<Int, Float>,
        demoFinished: Boolean,
    ) {
        val tall = cell == (411 to 1f)
        val inPanel = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        for (id in listOf(R.string.listen, R.string.lesson_replay)) {
            val description = context.getString(id)
            val control = compose.onNodeWithContentDescription(description, useUnmergedTree = true)
            control.assert(if (tall) !inPanel else inPanel) { "$cell, demo finished=$demoFinished" }
            if (!tall) control.performScrollTo()
            val button = control.assertIsDisplayed().fetchSemanticsNode()
            assertEquals("$cell $description height", button.size.height.toFloat(), button.boundsInRoot.height, 1f)
        }
        compose.onNodeWithText(context.getString(R.string.next)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h718dp")
    fun compactLessonPreviousHasChevronWhileHeaderRetainsArrow() {
        language.value = AppLanguage.FINNISH
        viewport.value = 320 to 2f
        val lesson = LessonCatalog.lesson(Lessons.DAY_LESSON, language.value)
        compose.setContent {
            Viewport(718) {
                LessonScreen(LessonUiState(lesson, page = 2), false, true, {}, {}, {}, {}, {}, {})
            }
        }
        val context = localizedContext(language.value)
        val previous = compose.onNodeWithContentDescription(context.getString(R.string.lesson_prev)).assertIsDisplayed()
        val back =
            compose
                .onNodeWithContentDescription(
                    context.getString(R.string.back_description),
                ).assertIsDisplayed()
        val previousPixels = previous.captureToImage().toPixelMap()
        val backPixels = back.captureToImage().toPixelMap()
        val shaftX = backPixels.width * 5 / 8
        val centerY = backPixels.height / 2
        assertTrue("The header arrow has a shaft", backPixels[shaftX, centerY].luminance() < 0.25f)
        assertTrue("The previous chevron has no shaft", previousPixels[shaftX, centerY].luminance() > 0.5f)
        val tipX = previousPixels.width / 2 - SkDimens.ActionIcon.value.toInt() / 6
        assertTrue("The chevron points left", previousPixels[tipX, centerY].luminance() < 0.25f)
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun hintStaysInQuestionLineOnTallLayout() {
        language.value = AppLanguage.FINNISH
        val task = readTask(listOf(210, 270, 330)).copy(level = 1, h24 = false)
        val state = mutableStateOf(task)
        val hint = Hint.HandsRight(task.task.time)
        compose.setContent {
            Viewport(914) {
                GameScreen(
                    state.value,
                    true,
                    {},
                    {},
                    { state.value = task.copy(feedback = GameFeedback.HintText(hint)) },
                    {},
                    {},
                    {},
                )
            }
        }
        val context = localizedContext(language.value)
        val question = compose.onNodeWithText(context.getString(R.string.question_read))
        val option = compose.onNodeWithText(timeLabel(language.value, task.options.last(), false))
        question.assertIsDisplayed()
        option.performClick()
        question.assertDoesNotExist()
        val hintNode = compose.onNodeWithText(hint.text(context.resources)).assertIsDisplayed().fetchSemanticsNode()
        val first = compose.onNodeWithText(timeLabel(language.value, task.options.first(), false)).fetchSemanticsNode()
        assertTrue(hintNode.boundsInRoot.top < first.boundsInRoot.top)
        compose.runOnIdle {
            state.value =
                task.copy(
                    feedback = GameFeedback.RevealRead(task.task.time),
                    showNext = true,
                )
        }
        compose.onNodeWithText(hint.text(context.resources)).assertDoesNotExist()
        question.assertDoesNotExist()
        val time = timeLanguage(language.value).fullWords(task.task.time, false)
        val reveal = compose.onNodeWithText(context.getString(R.string.reveal_read, time)).fetchSemanticsNode()
        val firstAfterReveal =
            compose.onNodeWithText(timeLabel(language.value, task.options.first(), false)).fetchSemanticsNode()
        assertTrue(reveal.positionInRoot.y < firstAfterReveal.positionInRoot.y)
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun hintGoesBelowOptionsWhenClockIsAtMinimum() {
        val task = readTask(listOf(120, 180, 240))
        val hint = Hint.HandsRight(task.task.time)
        compose.setContent {
            Viewport(pixelHeight()) {
                GameScreen(task.copy(feedback = GameFeedback.HintText(hint)), true, {}, {}, {}, {}, {}, {})
            }
        }
        for (cell in listOf(320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            val context = localizedContext(language.value)
            compose.onNodeWithText(context.getString(R.string.question_read)).assertExists()
            val last =
                compose
                    .onNode(hasText(timeLabel(language.value, task.options.last(), task.h24)) and hasClickAction())
                    .fetchSemanticsNode()
            val hintNode = compose.onNodeWithText(hint.text(context.resources)).fetchSemanticsNode()
            assertTrue(
                "$cell hint is below options",
                hintNode.positionInRoot.y >= last.positionInRoot.y + last.size.height,
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun readHintBelowOptionsIsBroughtIntoView() {
        language.value = AppLanguage.FINNISH
        val task =
            readTask(
                listOf(240, 255, 180),
            ).copy(level = 1, task = Task(180, TaskType.READ), clock = ClockCommand(180))
        val state = mutableStateOf(task)
        compose.setContent {
            Viewport(pixelHeight()) {
                key(viewport.value) {
                    GameScreen(state.value, true, {}, {}, { pick ->
                        state.value =
                            state.value.copy(
                                feedback = GameFeedback.HintText(Hints.forRead(task.task.time, pick)),
                                optionStates = state.value.optionStates + (pick to OptionState.WRONG),
                            )
                    }, {}, {}, {})
                }
            }
        }
        val res = localizedContext(language.value).resources
        val failures = mutableListOf<String>()
        for (cell in FeedbackViewports) {
            compose.runOnIdle {
                viewport.value = cell
                state.value = task
            }
            for ((index, wrong) in task.options.take(2).withIndex()) {
                val option = compose.onNodeWithText(timeLabel(language.value, wrong, task.h24))
                if (index == 0) option.assertIsDisplayed() else option.performScrollTo()
                option.performClick()
                if (!feedbackTextStartsInViewport(Hints.forRead(task.task.time, wrong).text(res))) {
                    failures += "$cell wrong pick ${index + 1}"
                }
            }
        }
        assertTrue(failures.joinToString(), failures.isEmpty())
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun lessonQuizFeedbackIsBroughtIntoView() {
        language.value = AppLanguage.FINNISH
        val lesson = LessonCatalog.lesson("viisarit", language.value)
        val quiz = LessonUiState(lesson, page = lesson.pages.size, choices = lesson.quiz.choices)
        val state = mutableStateOf(quiz)
        val attempt = mutableIntStateOf(0)
        val failSave = mutableStateOf(false)
        compose.setContent {
            Viewport(pixelHeight()) {
                key(viewport.value, attempt.intValue) {
                    LessonScreen(state.value, false, false, {}, {}, {}, {}, { pick ->
                        val correct = quiz.choices[pick].correct
                        state.value =
                            state.value.copy(
                                choiceStates =
                                    state.value.choiceStates +
                                        (pick to if (correct) OptionState.CORRECT else OptionState.WRONG),
                                showHint = !correct,
                                quizDone = correct && !failSave.value,
                                saveFailed = correct && failSave.value,
                            )
                    }, {})
                }
            }
        }
        val res = localizedContext(language.value).resources
        val hint = lesson.quiz.hint.resolve(res)
        val completion = res.getString(R.string.lesson_learned, praise(res, 0), lesson.title.resolve(res))
        val failed = res.getString(R.string.progress_save_error)
        val correct =
            quiz.choices
                .single { it.correct }
                .text
                .resolve(res)
        val failures = mutableListOf<String>()
        for (cell in FeedbackViewports + (411 to 1f)) {
            compose.runOnIdle {
                viewport.value = cell
                state.value = quiz
                failSave.value = false
            }

            fun pick(
                answer: String,
                message: String,
            ) {
                compose.onNode(hasText(answer) and hasClickAction()).performScrollTo().performClick()
                if (!feedbackTextStartsInViewport(message)) {
                    failures += "$cell $message"
                }
            }
            for (wrong in quiz.choices.filterNot { it.correct }) pick(wrong.text.resolve(res), hint)
            pick(correct, completion)
            compose.runOnIdle {
                attempt.intValue++
                state.value = quiz
                failSave.value = true
            }
            pick(correct, failed)
        }
        assertTrue(failures.joinToString(), failures.isEmpty())
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun revealBringsCorrectOptionIntoView() {
        val task = readTask(listOf(120, 240, 180)).copy(task = Task(180, TaskType.READ), clock = ClockCommand(180))
        val state = mutableStateOf(task)
        compose.setContent {
            Viewport(pixelHeight()) {
                key(viewport.value, state.value.taskIndex) {
                    GameScreen(state.value, true, {}, {}, {}, {}, {}, {})
                }
            }
        }
        val correct = compose.onNode(hasText(timeLabel(language.value, task.task.time, task.h24)) and hasClickAction())
        for (cell in TestViewports) {
            for (feedback in listOf(GameFeedback.RevealRead(task.task.time), GameFeedback.Praise(0, task.task.time))) {
                compose.runOnIdle {
                    viewport.value = cell
                    state.value = task.copy(taskIndex = state.value.taskIndex + 1)
                }
                compose.waitForIdle()
                compose.runOnIdle {
                    state.value = state.value.copy(feedback = feedback, showNext = true, busy = true)
                }
                val node = correct.fetchSemanticsNode()
                assertEquals(
                    "$cell $feedback option is fully shown",
                    node.size.height.toFloat(),
                    node.boundsInRoot.height,
                    1f,
                )
                val clear = scrollBottom() - SkDimens.ScrollFade.value - node.positionInRoot.y - node.size.height
                assertTrue("$cell $feedback option clearance: $clear dp", clear >= -1f)
            }
        }
    }

    private fun feedbackTextStartsInViewport(message: String): Boolean {
        val tip = compose.onNodeWithText(message).fetchSemanticsNode()
        val viewport = scrollBounds()
        val top = tip.positionInRoot.y
        return !tip.boundsInRoot.isEmpty && top >= viewport.top - 1f &&
            top < viewport.bottom - SkDimens.ScrollFade.value
    }

    protected fun scrollBottom() = scrollBounds().bottom

    private fun scrollBounds() =
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            .boundsInRoot

    protected fun pixelHeight() = if (viewport.value.first == 320) 718 else 914

    private companion object {
        val FeedbackViewports = listOf(411 to 2f, 320 to 1f, 320 to 2f)
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun readTaskShowsEveryOptionBeforeAnswering() {
        val answers = listOf(1028, 1048, 1068)
        val defaultPhone = 411 to 1f
        val cell = mutableStateOf(defaultPhone)
        val lang = mutableStateOf(AppLanguage.FINNISH)
        compose.setContent {
            val context = localizedContext(lang.value)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalAppLanguage provides lang.value,
                LocalDensity provides Density(1f, cell.value.second),
            ) {
                ClocklingsTheme {
                    key(lang.value, cell.value) {
                        Box(Modifier.fillMaxHeight().requiredWidth(cell.value.first.dp)) {
                            GameScreen(readTask(answers), true, {}, {}, {}, {}, {}, {})
                        }
                    }
                }
            }
        }

        for (selected in listOf(AppLanguage.FINNISH, AppLanguage.GERMAN)) {
            compose.runOnIdle {
                lang.value = selected
                cell.value = defaultPhone
            }
            val controls = compose.onNodeWithTag("adaptive_controls").fetchSemanticsNode().boundsInRoot
            for (time in answers) {
                val label = timeLabel(selected, time, true)
                val option = compose.onNodeWithText(label).fetchSemanticsNode()
                assertEquals(
                    "$selected: $label is fully visible",
                    option.size.height.toFloat(),
                    option.boundsInRoot.height,
                    1f,
                )
                assertTrue(
                    "$selected: $label reaches the bottom fade",
                    option.boundsInRoot.bottom <= controls.bottom - SkDimens.ScrollFade.value,
                )
            }
            compose.runOnIdle { cell.value = TestViewports.last() }
            for (time in answers) {
                compose.onNodeWithText(timeLabel(selected, time, true)).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun readClockNeverGrowsWithinATask() {
        language.value = AppLanguage.FINNISH
        val before = readTask(listOf(1028, 1048, 1068))
        val state = mutableStateOf(before)
        compose.setContent { Viewport(914) { GameScreen(state.value, true, {}, {}, {}, {}, {}, {}) } }

        fun clockWidth() =
            compose
                .onNodeWithTag("task_clock", useUnmergedTree = true)
                .fetchSemanticsNode()
                .size.width

        val initial = clockWidth()
        compose.runOnIdle {
            state.value = before.copy(feedback = GameFeedback.HintText(Hint.HandsRight(before.task.time)))
        }
        val hint = clockWidth()
        assertTrue("The hint dial is $hint dp, expected at least 300 dp", hint >= 300)
        compose.runOnIdle {
            state.value = before.copy(feedback = GameFeedback.RevealRead(before.task.time), showNext = true)
        }
        val reveal = clockWidth()
        compose.runOnIdle { state.value = before.copy(feedback = GameFeedback.Praise(0, before.task.time)) }
        assertTrue(
            "The clock can only shrink during a task",
            initial >= hint && hint >= reveal && reveal >= clockWidth(),
        )
        compose.runOnIdle { state.value = before.copy(taskIndex = 1) }
        assertEquals("The next task starts at its own fitted size", initial, clockWidth())
    }

    private fun readTask(answers: List<Int>) =
        GameUiState(
            level = 4,
            h24 = true,
            taskIndex = 0,
            taskCount = 10,
            task = Task(answers.first(), TaskType.READ),
            options = answers,
            clock = ClockCommand(answers.first()),
            step = 1,
        )

    @Composable
    protected fun Viewport(
        height: Int = 640,
        content: @Composable () -> Unit,
    ) {
        val context = localizedContext(language.value)
        CompositionLocalProvider(
            LocalAppViewModel provides model,
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides language.value,
            LocalDensity provides Density(1f, viewport.value.second),
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(viewport.value.first.dp, height.dp)) { content() }
            }
        }
    }
}

private class ReviewWriteStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    override val data = delegate.data
    var fail = false

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (fail) throw IOException("Synthetic write failure")
        return delegate.updateData(transform)
    }
}
