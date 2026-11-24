package fi.clocklings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Full app paths on the dedicated emulator. Read exercises use visible choices and feedback;
 * adjustable clocks expose their current hand positions to accessibility.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val environment = LocaleTestEnvironment()
    private val freshData =
        object : ExternalResource() {
            private lateinit var originalProgress: Progress
            private lateinit var originalSettings: Settings
            private var originalLegacy: String? = null
            private var originalPlatform = ""

            override fun before() {
                runBlocking {
                    originalProgress = environment.repository.progress.first()
                    originalSettings = environment.repository.settings.first()
                    originalLegacy = environment.repository.legacyLanguage.first()
                }
                originalPlatform = environment.prepare(Progress(), Settings())
            }

            override fun after() {
                runBlocking {
                    environment.repository.updateProgress { originalProgress }
                    environment.repository.updateSettings { originalSettings }
                    environment.repository.setLegacyLanguage(originalLegacy)
                }
                environment.setPlatformLanguage(originalPlatform)
            }
        }
    private val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val LEVEL_CARDS = 6
    }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshData).around(compose)

    private fun waitForText(
        text: String,
        substring: Boolean = false,
        timeout: Long = 10_000,
    ) {
        compose.waitUntil(timeout) {
            compose
                .onAllNodes(
                    hasText(text, substring = substring) or hasContentDescription(text, substring = true),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun click(text: String) {
        waitForText(text)
        compose
            .onAllNodes(
                (hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction(),
            )[0]
            .scrollToIfScrollable()
            .performClick()
    }

    private fun clockWords(): String {
        val node = compose.onNode(hasContentDescription("Clock face:", substring = true)).fetchSemanticsNode()
        return node.config[SemanticsProperties.ContentDescription]
            .first {
                it.startsWith("Clock face:")
            }.removePrefix("Clock face: ")
    }

    /** Asetus tallentuu DataStoreen taustasäikeessä, joten valintaa odotetaan eikä sitä tarkisteta heti. */
    private fun waitUntilSelected(text: String) {
        compose.waitUntil(5_000) {
            compose
                .onAllNodes((hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction())
                .fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    private fun waitForSoundOff() {
        compose.waitUntil(5_000) {
            compose.onNodeWithText("Sound effects").fetchSemanticsNode().config[SemanticsProperties.ToggleableState] ==
                ToggleableState.Off
        }
    }

    private fun isShown(
        text: String,
        substring: Boolean = false,
    ) = compose
        .onAllNodes(
            hasText(text, substring = substring) or hasContentDescription(text, substring = true),
        ).fetchSemanticsNodes()
        .isNotEmpty()

    /** Gives the first level [stars] stars straight through the repository. */
    private fun seedStars(stars: Int) {
        waitForText("Clocklings")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = (context.applicationContext as ClocklingsApplication).container.repository
        runBlocking { repository.updateProgress { it.withLevelResult(0, stars) } }
    }

    /** The item's tile (merged with its "Wearing" pill) shows that the companion wears it. */
    private fun wearing(item: String): Boolean =
        compose
            .onAllNodes(
                hasText(item) and hasClickAction() and
                    androidx.compose.ui.test.SemanticsMatcher
                        .expectValue(SemanticsProperties.Selected, true),
            ).fetchSemanticsNodes()
            .isNotEmpty()

    private fun assertWearing(item: String) {
        compose.waitUntil(5_000) { wearing(item) }
    }

    private fun assertNotWearing(item: String) {
        compose.waitUntil(5_000) { !wearing(item) }
        waitForText(item)
    }

    private fun assertWholeWords(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .scrollToIfScrollable()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Missing text layout: $text", layouts.isNotEmpty())
        for (layout in layouts) {
            assertTrue("Text height overflows: $text", !layout.didOverflowHeight)
            // A paragraph can retain the available width when its Text node wraps content.
            // Check the rendered lines, allowing one pixel for integer size rounding.
            for (line in 0 until layout.lineCount) {
                assertTrue(
                    "Text width overflows: $text",
                    layout.getLineLeft(line) >= -1f && layout.getLineRight(line) <= layout.size.width + 1f,
                )
            }
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                assertTrue(
                    "Word split in: $text",
                    end !in 1 until text.length ||
                        text[end - 1].isWhitespace() ||
                        text[end - 1] == '-' ||
                        text[end].isWhitespace(),
                )
            }
        }
    }

    @Test
    fun homeAndTogetherKeepWordsReadable() {
        waitForText("Clocklings")
        assertWholeWords(clockWords().replaceFirstChar { it.uppercase() })
        val title = compose.activity.getString(R.string.menu_nukke)
        val action = compose.activity.getString(R.string.home_companion_choices)
        compose.onNodeWithContentDescription("$title. $action").assertIsEnabled()
        assertWholeWords("Explore")
        assertWholeWords("Play Together")

        click("Play Together")
        assertWholeWords("Play Together")
    }

    @Test
    fun playWholeHoursThenDressCompanionAndSurviveRecreate() {
        click("Play")
        waitForText("Whole hours")
        click("Whole hours")
        click("Skip and play")
        repeat(10) { task ->
            compose.waitUntil(5_000) { isShown("What time is it?") || isShown("Set the clock to") }
            compose.waitForIdle()
            if (isShown("What time is it?")) {
                compose.onAllNodes(hasContentDescription("Clock face:", substring = true)).assertCountEquals(0)
                val choices =
                    compose
                        .onAllNodes(hasText("o'clock", substring = true) and hasClickAction())
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].first().text }
                for (choice in choices) {
                    click(choice)
                    compose.waitForIdle()
                    if (compose
                            .onAllNodes(hasContentDescription("Clock face:", substring = true))
                            .fetchSemanticsNodes()
                            .isNotEmpty()
                    ) {
                        break
                    }
                }
                if (isShown("Next")) click("Next")
            } else {
                val target =
                    compose
                        .onAllNodesWithText("o'clock", substring = true)
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].first().text }
                        .first()
                        .lowercase()
                var guard = 0
                while (clockWords() != target && guard++ < 12) {
                    compose
                        .onNode(hasContentDescription("Clock face:", substring = true))
                        .performCustomAccessibilityActionWithLabel("Add 60\u00A0minutes")
                    compose.waitForIdle()
                }
                assertTrue("tehtävä $task: kello ${clockWords()} != $target", clockWords() == target)
                click("Check")
            }
            if (task < 9) awaitTask(task + 1)
        }
        waitForText("Whole hours complete!", substring = true)
        assertTrue(isShown("You earned", substring = true))
        assertTrue(isShown("Polka-dot dress"))

        click("Choose an outfit")
        waitForText("Outfits")
        click("Polka-dot dress")
        compose.waitUntil(5_000) { pilkkumekkoSelected() }

        compose.activityRule.scenario.recreate()
        waitForText("Outfits")
        compose.waitUntil(5_000) { pilkkumekkoSelected() }
        compose.onAllNodes(hasText("Polka-dot dress") and hasClickAction())[0].assertIsSelected()
    }

    private fun pilkkumekkoSelected(): Boolean =
        compose
            .onAllNodes(hasText("Polka-dot dress") and hasClickAction())
            .fetchSemanticsNodes()
            .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }

    @Test
    fun animalWardrobeWearsItemsAndColors() {
        seedStars(3)
        click("My Companion")
        compose.onNodeWithTag("choose_companion_CAT").scrollToIfScrollable().performClick()
        click("Choose an outfit")
        waitForText("Outfits")
        click("Striped scarf")
        assertWearing("Striped scarf")
        click("Striped scarf")
        assertNotWearing("Striped scarf")
        compose.onNodeWithTag("color_BLUE").scrollToIfScrollable().performClick()
        compose.waitUntil(5_000) {
            compose
                .onNodeWithTag("color_BLUE")
                .fetchSemanticsNode()
                .config
                .getOrElse(SemanticsProperties.Selected) { false }
        }
        compose.onNodeWithTag("color_BLUE").assertIsSelected()
        // The bobble hat opens at four stars.
        compose.onNodeWithContentDescription("Needs 4 stars").assertExists()
        // No loose free-item chip from the old picker.
        compose.onAllNodesWithText("Bandana").assertCountEquals(0)
    }

    @Test
    fun freeClockAndTogetherPlayKeepSettingsAcrossRecreation() {
        click("Settings")
        click("24 h")
        waitUntilSelected("24 h")
        click("Sound effects")
        waitForSoundOff()
        click("Back")
        click("Explore")
        waitForText("Explore")
        assertTrue(clockWords().contains("three"))
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5\u00A0minutes")
        compose.onNodeWithText("15:05").assertExists()
        val subtract =
            compose
                .onNode(hasContentDescription("Clock face:", substring = true))
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .single { it.label == "Subtract 5\u00A0minutes" }
        compose.runOnIdle { repeat(144) { subtract.action() } }
        compose.onNodeWithText("03:05").assertExists()
        click("Clock guides on")
        waitForText("Clock guides off")
        compose.activityRule.scenario.recreate()
        waitForText("Clock guides off")
        compose.onNodeWithText("03:05").assertExists()
        click("Back")
        click("Play Together")
        click("Random time")
        click("Pass to partner")
        waitForText("What time is it?")
        compose.onAllNodes(hasContentDescription("Clock face:", substring = true)).assertCountEquals(0)
        compose.onNodeWithContentDescription("Player 2 answers", substring = true).assertExists()
        compose.activityRule.scenario.recreate()
        waitForText("What time is it?")
        compose.onAllNodes(hasContentDescription("Clock face:", substring = true)).assertCountEquals(0)
        click("Show the answer")
        compose.onNodeWithText("Swap turns").assertIsDisplayed()
        assertTrue(clockWords().isNotBlank())
        click("Swap turns")
        waitForText("Choose a time")
        compose.onNodeWithContentDescription("Player 2 sets the time", substring = true).assertExists()
        compose.activityRule.scenario.recreate()
        waitForText("Player 2 sets the time")
        click("Pass to partner")
        compose.onNodeWithContentDescription("Player 1 answers", substring = true).assertExists()
        click("Back")
        compose.onNodeWithText("0 / 15").assertExists()
        click("Settings")
        click("12 h")
        waitUntilSelected("12 h")
        click("Reset progress")
        waitForText("Reset learning progress?")
        compose.onAllNodesWithText("Reset progress").onLast().performClick()
        compose.waitUntil(5_000) { !isShown("Reset learning progress?") }
        compose.onNodeWithText("Sound effects").scrollToIfScrollable().assertIsOff()
    }

    @Test
    fun everyLevelAndTheChallengeArePlayableFromTheStart() {
        click("Play")
        waitForText("Whole hours")
        // Viisi tasoa ja Minuuttihaaste: kaikki Pelaa-napit toimivat ilman tähtiä.
        val play = compose.onAllNodes(hasContentDescription("For example", substring = true) and hasClickAction())
        compose.waitUntil(5_000) { play.fetchSemanticsNodes().size == LEVEL_CARDS - 1 }
        repeat(LEVEL_CARDS - 1) { play[it].scrollToIfScrollable().assertIsEnabled() }
        click("Minute Challenge")
        waitForText("Start!")
    }

    private fun awaitTask(task: Int) {
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithTag("game_task_$task", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun passTheFirstLessonQuiz() {
        click("Learn")
        waitForText("Two hands")
        click("Two hands")
        click("Next")
        click("Next")
        click("Try it")
        waitForText("Which hand shows the hour?")
        click("The long blue hand")
        waitForText("The hour hand is the shorter, pink hand.")
        click("The short pink hand")
        waitForText("You completed Two hands.", substring = true)
        click("Done")
        waitForText("Completed")
        compose.onNodeWithContentDescription("Completed", substring = true).assertExists()
    }
}
