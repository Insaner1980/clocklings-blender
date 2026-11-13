package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/** Run on the dedicated emulator at 411/320 dp and 100%/200% font scale. */
@RunWith(AndroidJUnit4::class)
class BlenderRobotDeviceTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)
    private lateinit var previousProgress: Progress
    private lateinit var previousSettings: Settings

    @Before
    fun prepareRobot() =
        runBlocking {
            val repository = (compose.activity.application as ClocklingsApplication).container.repository
            previousProgress = repository.progress.first()
            previousSettings = repository.settings.first()
            repository.updateProgress { Progress(stars = List(5) { 3 }) }
            repository.updateSettings {
                Settings(sound = false, personalization = Personalization(companion = CompanionKind.ROBOT))
            }
        }

    @After
    fun restoreState() =
        runBlocking {
            val repository = (compose.activity.application as ClocklingsApplication).container.repository
            repository.updateProgress { previousProgress }
            repository.updateSettings { previousSettings }
        }

    @Test
    fun robotColorAndOutfitSurviveRecreation() {
        waitForRobot()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("logo_mascot", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        save("home")
        click("My Companion")
        waitForRobot()
        save("appearance")
        click("Choose an outfit")
        waitForRobot()
        compose.onNodeWithText("Outfits").assertIsDisplayed()
        save("wardrobe")
        compose.onNodeWithTag("color_PURPLE").scrollToIfScrollable().performClick()
        waitSelected("color_PURPLE")
        click("Wind-up key")
        waitForKey()
        compose.activityRule.scenario.recreate()
        waitSelected("color_PURPLE")
        waitForKey()
        compose.onNodeWithTag("color_PURPLE").assertIsSelected()
        compose.onNodeWithTag("companion_ROBOT", useUnmergedTree = true).scrollToIfScrollable().assertIsDisplayed()
        save("purple_key")
    }

    private fun waitForRobot() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("companion_ROBOT", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForKey() {
        val key =
            (hasText("Wind-up key") or hasContentDescription("Wind-up key", substring = true)) and hasClickAction()
        compose.waitUntil(10_000) {
            compose
                .onAllNodes(
                    key,
                ).fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    private fun waitSelected(tag: String) {
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithTag(
                    tag,
                ).fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    private fun click(label: String) {
        val match = (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(match)[0].scrollToIfScrollable().performClick()
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val case = InstrumentationRegistry.getArguments().getString("case") ?: "default"
        val directory =
            File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "blender-robot",
            ).apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "${case}_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
