package fi.clocklings.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.ClocklingsApp
import fi.clocklings.audio.Sound
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val services = FakeServices(fullAccess = true)
    private val viewport = mutableStateOf(411 to 1f)

    @Test
    fun repeatedLessonBackReturnsOnlyToTheLessonList() {
        compose.setContent { App() }
        awaitHome()
        val openLearn = clickAction("Learn")
        compose.runOnIdle { openLearn() }
        val openLesson = clickAction("Two hands.")
        compose.runOnIdle { openLesson() }
        val leaveLesson = clickAction("Back")
        compose.runOnIdle {
            leaveLesson()
            leaveLesson()
        }
        clickAction("Two hands.")
        compose.onNodeWithText("My Companion").assertDoesNotExist()
    }

    @Test
    fun repeatedAndStaleCallbacksDoNotDuplicateOrSkipDestinations() {
        compose.setContent { App() }
        awaitHome()
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            val openSettings = clickAction("Settings")
            val openExplore = clickAction("Explore")
            compose.runOnIdle {
                openSettings()
                openSettings()
                openExplore()
            }
            compose.onNodeWithText("Clock display").assertIsDisplayed()

            val openLanguage = clickAction("Language:")
            val settingsBack = clickAction("Back")
            compose.runOnIdle {
                openLanguage()
                openLanguage()
                settingsBack()
            }
            compose.onNodeWithText("Device language").assertIsDisplayed()
            val languageBack = clickAction("Back")
            compose.runOnIdle {
                languageBack()
                languageBack()
            }
            compose.onNodeWithText("Clock display").assertIsDisplayed()
            val backHome = clickAction("Back")
            compose.runOnIdle {
                backHome()
                backHome()
            }
            awaitHome()
            compose.onNodeWithText("Clock display").assertDoesNotExist()
        }
    }

    @Test
    fun repeatedPracticeClicksReturnToTheirSourceWithOneBack() {
        compose.setContent { App() }
        awaitHome()
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            val routes = listOf("Play." to "Whole hours.", "Learn" to "Whole hours. Start playing!")
            for ((destination, practice) in routes) {
                val open = clickAction(destination)
                compose.runOnIdle { open() }
                // The first start includes introductory lessons; later starts open the game directly.
                repeat(2) {
                    val start = clickAction(practice)
                    compose.runOnIdle {
                        services.sounds.clear()
                        start()
                        start()
                        assertEquals(1, services.sounds.count { it == Sound.POP })
                    }
                    compose.waitForIdle()
                    compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
                    clickAction(practice)
                }
                val back = clickAction("Back")
                compose.runOnIdle { back() }
                awaitHome()
            }
        }
    }

    @Test
    fun savedNavigationRestoresLanguageAndBothBackSteps() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { App() }
        awaitHome()
        val openSettings = clickAction("Settings")
        compose.runOnIdle { openSettings() }
        val openLanguage = clickAction("Language:")
        compose.runOnIdle { openLanguage() }
        compose.onNodeWithText("Device language").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Device language").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Clock display").assertIsDisplayed()
        val back = clickAction("Back")
        compose.runOnIdle { back() }
        awaitHome()
    }

    @Test
    fun homeOutfitsOpensWardrobeAndStageStillOpensPersonalize() {
        compose.setContent { App() }
        awaitHome()
        val outfits = clickAction("Outfits")
        compose.runOnIdle { outfits() }
        compose.onNodeWithText("Choose a name").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        awaitHome()
        val stage = clickAction("My Companion.")
        compose.runOnIdle { stage() }
        compose.onNodeWithText("Make it yours").assertExists()
    }

    @Test
    fun settingsParentsRowOpensTheExistingGate() {
        compose.setContent { App() }
        awaitHome()
        val settings = clickAction("Settings")
        compose.runOnIdle { settings() }
        val parents = clickAction("For parents")
        compose.runOnIdle { parents() }
        compose.onNodeWithText("Continue").assertExists()
    }

    @Composable
    private fun App() {
        val (width, fontScale) = viewport.value
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            Box(Modifier.requiredSize(width.dp, 914.dp)) {
                ClocklingsApp(container = services)
            }
        }
    }

    private fun awaitHome() {
        compose.waitUntil(10_000) {
            if (compose.onAllNodesWithText("Skip intro").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Skip intro").performClick()
            }
            compose
                .onAllNodes(hasContentDescription("Settings") and hasClickAction())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun clickAction(label: String): () -> Boolean {
        val node =
            compose.onNode((hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction())
        if (generateSequence(node.fetchSemanticsNode().parent) { it.parent }.any {
                SemanticsActions.ScrollBy in it.config
            }
        ) {
            node.performScrollTo()
        }
        node.assertIsDisplayed()
        return requireNotNull(node.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
    }
}
