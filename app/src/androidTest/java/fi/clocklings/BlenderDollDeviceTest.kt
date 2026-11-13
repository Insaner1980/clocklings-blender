package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/** Run on the dedicated emulator at 411/320 dp and 100%/200% font scale. */
@RunWith(AndroidJUnit4::class)
class BlenderDollDeviceTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)
    private lateinit var previousProgress: Progress
    private lateinit var previousSettings: Settings

    @Before
    fun prepareDoll() =
        runBlocking {
            val repository = (compose.activity.application as ClocklingsApplication).container.repository
            previousProgress = repository.progress.first()
            previousSettings = repository.settings.first()
            repository.updateProgress { Progress(stars = List(5) { 3 }) }
            repository.updateSettings {
                Settings(sound = false, personalization = Personalization(companion = CompanionKind.HUMAN))
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
    fun dollOutfitSurvivesRecreation() {
        val configuration = compose.activity.resources.configuration
        val arguments = InstrumentationRegistry.getArguments()
        arguments.getString("widthDp")?.let { width ->
            assertEquals("Actual layout width", width.toInt(), configuration.screenWidthDp)
            assertEquals(
                "Actual font scale",
                requireNotNull(arguments.getString("fontScale")).toFloat(),
                configuration.fontScale,
                0.001f,
            )
        }
        waitForDoll()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("logo_mascot", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        click("My Companion")
        waitForDoll()
        click("Choose an outfit")
        waitForDoll()
        compose.onNodeWithText("Outfits").assertIsDisplayed()
        val stars = compose.onNodeWithTag("wardrobe_star_balance").fetchSemanticsNode().boundsInRoot
        val companion =
            compose.onNodeWithTag("companion_HUMAN", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Star balance covers the doll or its crown", stars.bottom <= companion.top)
        compose.onAllNodesWithTag("color_ORIGINAL").assertCountEquals(0)
        for (item in listOf("Golden hair", "Pink curls", "Rainbow hair", "Brown braids")) {
            click(item)
            waitSelected(item)
        }
        val selected = listOf("Overalls", "Crown", "Roller skates", "Strawberry bag")
        for (item in selected) {
            click(item)
            waitSelected(item)
        }
        compose.activityRule.scenario.recreate()
        for (item in selected + "Brown braids") waitSelected(item)
        compose.onNodeWithTag("companion_HUMAN", useUnmergedTree = true).scrollToIfScrollable().assertIsDisplayed()
        save()
    }

    private fun waitForDoll() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("companion_HUMAN", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitSelected(label: String) {
        val key = (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction()
        compose.waitUntil(10_000) {
            compose
                .onAllNodes(key)
                .fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    private fun click(label: String) {
        val match = (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(match)[0].scrollToIfScrollable().performClick()
    }

    private fun save() {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val case = InstrumentationRegistry.getArguments().getString("case") ?: "default"
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "blender-doll").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "${case}_overalls.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
