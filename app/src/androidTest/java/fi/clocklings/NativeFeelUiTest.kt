package fi.clocklings

import androidx.activity.BackEventCompat
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The same real navigation gallery is captured before and after the presentation change. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NativeFeelUiTest {
    private val environment = LocaleTestEnvironment()
    private val compose = createAndroidComposeRule<MainActivity>()
    private var previousLocale = ""
    private val fixture =
        object : ExternalResource() {
            private lateinit var originalProgress: Progress
            private lateinit var originalSettings: Settings
            private var originalLegacy: String? = null

            override fun before() {
                runBlocking {
                    originalProgress = environment.repository.progress.first()
                    originalSettings = environment.repository.settings.first()
                    originalLegacy = environment.repository.legacyLanguage.first()
                }
                previousLocale = environment.prepare(Progress(introShown = setOf("0")), Settings())
            }

            override fun after() {
                runBlocking {
                    environment.repository.updateProgress { originalProgress }
                    environment.repository.updateSettings { originalSettings }
                    environment.repository.setLegacyLanguage(originalLegacy)
                }
                environment.setPlatformLanguage(previousLocale)
            }
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(compose)

    private fun click(text: String) {
        val match =
            (hasText(text, substring = true) or hasContentDescription(text, substring = true)) and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(match)[0].scrollToIfScrollable().performClick()
    }

    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        // The system screenshot observes the real compositor, not the Compose test clock.
        Thread.sleep(500)
        LocalizationScreenshots.capture("native-$name")
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun mainScreenGallery() {
        click("Settings")
        capture("settings")
        click("Language:")
        capture("language")
        back()
        back()
        capture("home")
        click("Explore")
        capture("explore")
        back()
        click("Play Together")
        capture("together-set")
        click("Pass to partner")
        capture("together-ask")
        click("Show the answer")
        capture("together-reveal")
        back()
        click("Learn")
        capture("learn")
        click("Two hands")
        capture("lesson")
        back()
        back()
        click("Play")
        capture("levels")
        click("Whole hours")
        capture("game")
        back()
        back()
        click("Play")
        click("Minute Challenge")
        capture("challenge-intro")
        click("Start!")
        compose.mainClock.autoAdvance = false
        try {
            compose.mainClock.advanceTimeBy(32)
            capture("challenge-playing")
            back()
        } finally {
            compose.mainClock.autoAdvance = true
        }
        back()
        click("My Companion")
        capture("appearance")
        click("Choose an outfit")
        capture("wardrobe")
        back()
        click("Clock styles")
        capture("clocks")
    }

    @Test
    fun localizedMainScreens() {
        for (language in AppLanguage.entries) {
            environment.changeLanguage(language, compose.activityRule.scenario)
            val res = environment.resources(language)
            click(res.getString(R.string.settings_title))
            capture("settings-" + language.tag)
            back()
            capture("home-" + language.tag)
            click(res.getString(R.string.menu_pelaa))
            capture("levels-" + language.tag)
            click(res.getString(R.string.level_tasatunnit))
            capture("game-" + language.tag)
            back()
            back()
        }
    }

    @Test
    fun pagerSwipeAndThumbnailSelectionSurviveRecreation() {
        runBlocking {
            environment.repository.updateProgress { it.copy(stars = listOf(AccessPolicy.COMPANION_STARS, 0, 0, 0, 0)) }
        }
        click("My Companion")
        compose.onNodeWithTag("companion_pager").performTouchInput { swipeLeft() }
        compose.waitUntil(10_000) {
            compose
                .onNodeWithTag("choose_companion_CAT")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Selected]
        }
        compose.onNodeWithTag("choose_companion_ROBOT").scrollToIfScrollable().performClick()
        compose.waitUntil(10_000) {
            compose
                .onNodeWithTag("choose_companion_ROBOT")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Selected]
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("choose_companion_ROBOT").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("choose_companion_ROBOT").scrollToIfScrollable().assertIsSelected()
    }

    @Test
    fun predictiveBackCancelPreservesExploreAndGameState() {
        click("Explore")
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5\u00A0minutes")
        cancelBack()
        compose.onNode(hasText("3:05")).assertExists()
        back()
        click("Play")
        click("Whole hours")
        val choices = answerLabels()
        cancelBack()
        assertEquals(choices, answerLabels())
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { answerLabels().isNotEmpty() }
        assertEquals(choices, answerLabels())
    }

    private fun answerLabels(): List<String> =
        compose
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .flatMap { node ->
                node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
            }.filter { it.matches(Regex("[0-9]{1,2}:[0-9]{2}")) }

    private fun cancelBack() {
        compose.runOnUiThread {
            val dispatcher = compose.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(160f, 400f, 0.5f, BackEventCompat.EDGE_LEFT))
        }
        compose.mainClock.advanceTimeBy(160)
        capture("predictive-in-progress")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    @Test
    fun homeActivityDescriptionsIncludeVisibleNames() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("companion_stage").fetchSemanticsNodes().isNotEmpty() }
        for (title in listOf("Learn", "Explore", "Play", "Play Together")) {
            val node = compose.onNode(hasText(title) and hasClickAction()).fetchSemanticsNode()
            org.junit.Assert.assertTrue(
                "Activity name must be announced: $title",
                node.config[SemanticsProperties.ContentDescription].any { it.contains(title) },
            )
        }
    }

    @Test
    fun appearanceTabsKeepWholeWordsInEveryLanguage() {
        click("My Companion")
        for (language in AppLanguage.entries) {
            environment.changeLanguage(language, compose.activityRule.scenario)
            for (key in listOf(R.string.companions_title, R.string.clock_styles_title)) {
                val label = environment.resources(language).getString(key)
                assertWholeWords(label)
            }
            capture("appearance-" + language.tag)
        }
    }

    private fun assertWholeWords(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(label, useUnmergedTree = true)
            .scrollToIfScrollable()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        org.junit.Assert.assertTrue(layouts.isNotEmpty())
        for (layout in layouts) {
            org.junit.Assert.assertFalse("Clipped: $label", layout.didOverflowHeight)
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                org.junit.Assert.assertTrue(
                    "Word split: $label",
                    end !in 1 until label.length || label[end - 1].isWhitespace() || label[end].isWhitespace(),
                )
            }
        }
    }

    @Test
    fun homeFitsAtNormalPhoneSize() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("companion_stage").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        val scroll =
            compose
                .onAllNodes(
                    androidx.compose.ui.test.SemanticsMatcher
                        .keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
                ).fetchSemanticsNodes()
                .single()
                .config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("Home must fit at 360 x 640 dp / 100%", 0f, scroll.maxValue(), 1f)
    }
}
