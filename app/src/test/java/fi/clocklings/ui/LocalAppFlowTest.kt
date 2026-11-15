package fi.clocklings.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ClocklingsApplication
import fi.clocklings.MainActivity
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.colorOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Real navigation and persistence, run locally without an emulator. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalTestApi::class)
class LocalAppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val repository
        get() =
            (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ClocklingsApplication)
                .container.repository
    private val fixture =
        object : ExternalResource() {
            override fun before() {
                runBlocking {
                    repository.updateProgress { Progress(introShown = setOf("0", "1", "2", "3", "4", "h24")) }
                    repository.updateSettings { Settings(sound = false) }
                }
            }
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(compose)

    @Test
    fun companionColorsAndOutfitsPersistAcrossRecreation() {
        runBlocking { repository.updateProgress { it.withLevelResult(0, 3) } }
        click("My Companion")
        for (kind in CompanionKind.entries) {
            tagClick("choose_companion_${kind.name}")
            waitSaved(
                "companion $kind",
            ) { runBlocking { repository.settings.first() }.personalization.companion == kind }
            waitSelected("choose_companion_${kind.name}")
        }
        tagClick("choose_companion_CAT")
        waitSaved("cat companion") {
            runBlocking { repository.settings.first() }.personalization.companion == CompanionKind.CAT
        }
        waitSelected("choose_companion_CAT")
        click("Choose an outfit")
        for (color in CompanionKind.CAT.colorOptions()) {
            tagClick("color_${color.name}")
            waitSaved {
                runBlocking { repository.settings.first() }.personalization.styleFor(CompanionKind.CAT).color == color
            }
            waitSelected("color_${color.name}")
        }
        click("Striped scarf")
        waitSaved(
            "cat scarf",
        ) { runBlocking { repository.progress.first() }.outfitFor(CompanionKind.CAT).items.isNotEmpty() }
        back()
        val expectedSettings = runBlocking { repository.settings.first() }
        val expectedProgress = runBlocking { repository.progress.first() }
        compose.activityRule.scenario.recreate()
        waitText("Make it yours")
        waitSelected("choose_companion_CAT")
        click("Choose an outfit")
        waitSelected("color_${expectedSettings.personalization.styleFor(CompanionKind.CAT).color.name}")
        val scarf =
            hasText("Striped scarf", substring = true) or
                hasContentDescription("Striped scarf", substring = true)
        compose.onNode(scarf and isSelected()).assertIsSelected()
        assertEquals(expectedSettings, runBlocking { repository.settings.first() })
        assertEquals(expectedProgress, runBlocking { repository.progress.first() })
    }

    @Test
    fun everyClockThemeRemainsAdjustableAndCustomChoicesPersist() {
        click("My Companion")
        click("Clock styles")
        for (theme in ClockTheme.entries) {
            tagClick("choose_clock_${theme.name}")
            waitSaved("clock theme $theme") {
                runBlocking { repository.settings.first() }.personalization.clock.theme ==
                    theme
            }
            waitSelected("choose_clock_${theme.name}")
        }
        val coral = CustomClock(palette = ClockPalette.CORAL)
        val blocks = coral.copy(frame = ClockFrame.BLOCKS)
        val plain = blocks.copy(numerals = NumeralStyle.PLAIN)
        val customSteps =
            listOf(
                "Coral" to coral,
                "Blocks" to blocks,
                "Plain" to plain,
                "Pointed ends" to plain.copy(hands = HandStyle.POINTER),
            )
        for ((label, custom) in customSteps) {
            click(label)
            waitSaved("custom clock $label") {
                runBlocking { repository.settings.first() }.personalization.clock.custom ==
                    custom
            }
        }
        val expected = runBlocking { repository.settings.first() }
        compose.activityRule.scenario.recreate()
        waitText("Make it yours")
        waitSelected("choose_clock_CUSTOM")
        assertEquals(expected, runBlocking { repository.settings.first() })
        back()
        click("Explore")
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5\u00A0minutes")
        compose.onNodeWithText("3:05").assertExists()
    }

    @Test
    fun togetherHidesTheAnswerAndKeepsItAcrossRecreation() {
        click("Play Together")
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5\u00A0minutes")
        click("Pass to partner")
        compose.onAllNodes(hasContentDescription("Clock face:", substring = true)).assertCountEquals(0)
        compose.activityRule.scenario.recreate()
        waitText("Show the answer")
        compose.onAllNodes(hasContentDescription("Clock face:", substring = true)).assertCountEquals(0)
        click("Show the answer")
        compose.onNodeWithText("3:05").assertExists()
        click("Swap turns")
        compose.onNode(hasContentDescription("Player 2 sets the time", substring = true)).assertExists()
        click("Random time")
        click("Pass to partner")
        compose.onNode(hasContentDescription("Player 1 answers", substring = true)).assertExists()
    }

    @Test
    fun lessonFeedbackAndCompletionReachTheLearningPath() {
        click("Learn")
        click("Two hands")
        click("Next")
        click("Next")
        click("Try it")
        click("The long blue hand")
        waitText("The hour hand is the shorter, pink hand.")
        click("The short pink hand")
        waitSaved { "viisarit" in runBlocking { repository.progress.first() }.lessonsDone }
        click("Done")
        compose.onNode(hasContentDescription("Completed", substring = true)).assertExists()
    }

    @Test
    fun settingsPersistAndResetRequiresConfirmation() {
        runBlocking { repository.updateProgress { it.withLevelResult(0, 3) } }
        click("Settings")
        val radio =
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.Role,
                androidx.compose.ui.semantics.Role.RadioButton,
            )
        assertEquals(2, compose.onAllNodes(radio).fetchSemanticsNodes().size)
        compose.onNode(radio and hasText("12 h")).assertExists()
        compose.onNode(radio and hasText("24 h")).assertExists()
        click("24 h")
        click("Clock guides")
        waitSaved { runBlocking { repository.settings.first() }.let { it.h24 && !it.help } }
        click("Reset progress")
        click("Cancel")
        assertEquals(3, runBlocking { repository.progress.first() }.totalStars)
        click("Reset progress")
        compose
            .onAllNodes(
                hasText("Reset progress") and hasClickAction(),
            ).let { it[it.fetchSemanticsNodes().lastIndex] }
            .performClick()
        waitSaved { runBlocking { repository.progress.first() }.totalStars == 0 }
        compose.activityRule.scenario.recreate()
        waitText("Clock display")
        assertEquals(true, runBlocking { repository.settings.first() }.h24)
        assertEquals(false, runBlocking { repository.settings.first() }.help)
    }

    private fun waitSaved(
        description: String = "settings or progress",
        condition: () -> Boolean,
    ) {
        compose.waitUntil(conditionDescription = description, timeoutMillis = 10_000) {
            compose.waitForIdle()
            condition()
        }
    }

    private fun waitSelected(tag: String) {
        compose.waitUntil(conditionDescription = "selected $tag", timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(tag) and isSelected()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag(tag).assertIsSelected()
    }

    private fun waitText(text: String) {
        compose.waitUntilAtLeastOneExists(
            matcher = hasText(text) or hasContentDescription(text, substring = true),
            timeoutMillis = 10_000,
        )
    }

    private fun click(text: String) {
        waitText(text)
        val node =
            compose.onAllNodes((hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction())[0]
        if (generateSequence(node.fetchSemanticsNode().parent) { it.parent }.any {
                SemanticsActions.ScrollBy in
                    it.config
            }
        ) {
            node.performScrollTo()
        }
        node.performClick()
    }

    private fun tagClick(tag: String) {
        val node = compose.onNodeWithTag(tag)
        node.performScrollTo().performClick()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
