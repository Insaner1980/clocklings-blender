package fi.clocklings

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeRoute
import fi.clocklings.ui.challenge.ChallengeViewModel
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Part03DeviceTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val environment = LocaleTestEnvironment()
    private lateinit var originalProgress: Progress
    private lateinit var originalSettings: Settings
    private val seed =
        Progress(
            stars = listOf(3, 2, 1, 0, 0),
            names = mapOf(CompanionKind.CAT to "Milo"),
            outfits = mapOf(CompanionKind.CAT to Outfit().with(Slot.NECK, "cat-neck-scarf")),
            lessonsDone = setOf("viisarit"),
            introShown = setOf("0"),
        )

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Before
    fun seedSyntheticData() =
        runBlocking {
            originalProgress = environment.repository.progress.first()
            originalSettings = environment.repository.settings.first()
            environment.repository.updateProgress { seed }
            environment.repository.updateSettings { Settings(sound = false, help = false) }
        }

    @After
    fun restoreSyntheticData() =
        runBlocking {
            environment.repository.updateProgress { originalProgress }
            environment.repository.updateSettings { originalSettings }
        }

    @Test
    fun togetherRejectsLateSetterAndDuplicateSwapAcrossActivityRecreation() {
        showTogether()
        val queued =
            compose
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first()
        compose.runOnIdle { assertTrue(queued.action()) }
        compose.onNodeWithText("Pass to partner").performClick()
        compose.runOnIdle { assertFalse(queued.action()) }
        compose.onNodeWithText("Show the answer").performClick()
        compose.onNodeWithText("3:05").scrollToIfScrollable().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        showTogether()
        compose.onNodeWithText("3:05").scrollToIfScrollable().assertIsDisplayed()
        val swap =
            compose
                .onNodeWithText("Swap turns")
                .fetchSemanticsNode()
                .config[SemanticsActions.OnClick]
                .action!!
        compose.runOnIdle {
            swap()
            swap()
        }
        compose.onNodeWithContentDescription("Player 2 sets the time").scrollToIfScrollable().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        showTogether()
        compose.onNodeWithContentDescription("Player 2 sets the time").scrollToIfScrollable().assertIsDisplayed()
        compose.onNodeWithText("Pass to partner").performClick()
        compose.onNodeWithText("Show the answer").performClick()
        compose.onNodeWithText("3:05").scrollToIfScrollable().assertIsDisplayed()
        compose.onNodeWithText("Swap turns").performClick()
        compose.onNodeWithContentDescription("Player 1 sets the time").scrollToIfScrollable().assertIsDisplayed()
        assertEquals(seed, runBlocking { environment.repository.progress.first() })
    }

    @Test
    fun challengeKeepsDeadlineAndScoreAcrossRecreationAndBackgroundExpiry() {
        lateinit var challenge: ChallengeViewModel
        val content: @Composable (AppViewModel) -> Unit = { app ->
            challenge =
                viewModel(
                    factory =
                        viewModelFactory {
                            initializer { ChallengeViewModel(false, app.services) }
                        },
                )
            ChallengeRoute(false, false, {}, app, challenge)
        }
        render(content)
        // The real deadline continues while the Compose animation clock is driven one frame at a time.
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithText("Start!").performClick()
            compose.runOnIdle { challenge.pick(challenge.state.value.question) }
            compose.waitUntil(5_000) {
                compose.mainClock.advanceTimeByFrame()
                !challenge.state.value.busy
            }
            val before = challenge.state.value
            val retained = challenge
            assertEquals(1, before.score)
            compose.activityRule.scenario.recreate()
            render(content)
            assertSame(retained, challenge)
            assertEquals(before, challenge.state.value)
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            val limit = SystemClock.elapsedRealtime() + 65_000
            while (challenge.state.value.phase != ChallengePhase.END && SystemClock.elapsedRealtime() < limit) {
                SystemClock.sleep(50)
            }
            assertEquals(ChallengePhase.END, challenge.state.value.phase)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText("Play again").scrollToIfScrollable().assertIsDisplayed()
            compose.runOnIdle { challenge.pick(challenge.state.value.question) }
            val ended = challenge.state.value
            assertEquals(before.runId, ended.runId)
            assertEquals(before.deadlineNanos, ended.deadlineNanos)
            assertEquals(1, ended.score)
            assertEquals(1, ended.best)
            assertTrue(ended.newRecord)
            assertEquals(seed.withChallengeScore(1), runBlocking { environment.repository.progress.first() })
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun companionThumbnailCommitsWhenScrolledBelowThePreview() {
        lateinit var model: AppViewModel
        render { app ->
            model = app
            val progress by app.progress.collectAsStateWithLifecycle()
            val settings by app.settings.collectAsStateWithLifecycle()
            AppearanceScreen((settings ?: Settings()).personalization, progress ?: seed, {}, {})
        }
        val target = compose.onNodeWithTag("choose_companion_CAT").scrollToIfScrollable()
        target.assertIsDisplayed()
        target.performClick()
        compose.waitUntil(10_000) {
            model.settings.value
                ?.personalization
                ?.companion == CompanionKind.CAT
        }
        val saved =
            runBlocking {
                environment.repository.settings
                    .first()
                    .personalization
                    .companion
            }
        assertEquals(
            CompanionKind.CAT,
            saved,
        )
    }

    @Test
    fun boundedNameKeepsSelectionAndDraftAcrossRealActivityRecreation() {
        showWardrobe()
        val field = compose.onNode(hasSetTextAction()).scrollToIfScrollable()
        field.performTextReplacement("a".repeat(15) + "\uD83D\uDE00")
        field.assertTextEquals("a".repeat(15))
        field.performTextInputSelection(TextRange(5))
        field.performTextInput("B")
        val full = "aaaaaB" + "a".repeat(10)
        field.assertTextEquals(full)
        field.performTextInputSelection(TextRange(full.length))
        field.performTextInput("C")
        field.assertTextEquals(full)
        val boundedSelection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        assertTrue(boundedSelection.min >= 0 && boundedSelection.max <= full.length)
        field.performTextInputSelection(TextRange(4, 7))
        field.performTextInput("Z")
        val shortened = "aaaaZ" + "a".repeat(9)
        field.assertTextEquals(shortened)
        val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        runBlocking {
            withTimeout(5_000) { environment.repository.progress.first { it.nameFor(CompanionKind.CAT) == shortened } }
        }
        compose.activityRule.scenario.recreate()
        showWardrobe()
        val restored = compose.onNode(hasSetTextAction()).scrollToIfScrollable()
        restored.assertTextEquals(shortened)
        assertEquals(selection, restored.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
    }

    private fun showTogether() = render { AskScreen(false, false, false, {}) }

    private fun showWardrobe() =
        render {
            val progress by environment.repository.progress.collectAsStateWithLifecycle(initialValue = seed)
            WardrobeScreen(CompanionKind.CAT, progress, CompanionColor.ORIGINAL, {})
        }

    private fun render(content: @Composable (AppViewModel) -> Unit) {
        val services = (environment.context.applicationContext as ClocklingsApplication).container
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val app = viewModel<AppViewModel>(factory = viewModelFactory { initializer { AppViewModel(services) } })
                CompositionLocalProvider(LocalAppViewModel provides app) { ClocklingsTheme { content(app) } }
            }
        }
        compose.waitForIdle()
    }
}
