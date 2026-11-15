package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import org.junit.rules.RuleChain
import java.io.File

/** Run the wardrobe tests on the dedicated emulator at 411/320 dp and 100%/200% font scale. */
abstract class BlenderWardrobeDeviceTest(
    private val companion: CompanionKind,
    private val captureDirectory: String = "blender-${companion.name.lowercase()}",
) {
    protected val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)
    private lateinit var previousProgress: Progress
    private lateinit var previousSettings: Settings
    private val companionTag = "companion_${companion.name}"

    @Before
    fun prepareCompanion() =
        runBlocking {
            val repository = (compose.activity.application as ClocklingsApplication).container.repository
            previousProgress = repository.progress.first()
            previousSettings = repository.settings.first()
            repository.updateProgress { Progress(stars = List(5) { 3 }) }
            repository.updateSettings {
                Settings(sound = false, personalization = Personalization(companion = companion))
            }
        }

    @After
    fun restoreState() =
        runBlocking {
            val repository = (compose.activity.application as ClocklingsApplication).container.repository
            repository.updateProgress { previousProgress }
            repository.updateSettings { previousSettings }
        }

    protected fun openWardrobe(captureScreens: Boolean = true) {
        waitForCompanion()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("logo_mascot", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        if (captureScreens) save("home")
        compose.clickByLabel("My Companion")
        waitForCompanion()
        if (captureScreens) save("appearance")
        compose.clickByLabel("Choose an outfit")
        waitForCompanion()
        compose.onNodeWithText("Outfits").assertIsDisplayed()
        if (captureScreens) save("wardrobe")
    }

    private fun waitForCompanion() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(companionTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun companionPreview() = compose.onNodeWithTag(companionTag, useUnmergedTree = true)

    protected fun captureColors(colors: List<String> = listOf("BLUE", "GREEN", "ORIGINAL")) {
        for (color in colors) {
            selectColor(color)
            companionPreview().scrollToIfScrollable()
            save(color.lowercase())
        }
    }

    protected fun selectColor(color: String) {
        val tag = "color_$color"
        compose.onNodeWithTag(tag).scrollToIfScrollable().performClick()
        waitUntilSelected(compose.onAllNodesWithTag(tag))
    }

    protected fun selectItem(label: String) {
        compose.clickByLabel(label)
        waitForItem(label)
    }

    protected fun waitForItem(label: String) {
        waitUntilSelected(compose.onAllNodes(clickableLabel(label)))
    }

    private fun waitUntilSelected(nodes: SemanticsNodeInteractionCollection) {
        compose.waitUntil(10_000) {
            nodes.fetchSemanticsNodes().any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
    }

    protected fun verifyOutfitAfterRecreation(
        color: String,
        item: String,
        capture: String,
    ) {
        compose.activityRule.scenario.recreate()
        val colorTag = "color_$color"
        waitUntilSelected(compose.onAllNodesWithTag(colorTag))
        waitForItem(item)
        compose.onNodeWithTag(colorTag).assertIsSelected()
        companionPreview().scrollToIfScrollable().assertIsDisplayed()
        save(capture)
    }

    protected fun save(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val case = InstrumentationRegistry.getArguments().getString("case") ?: "default"
        val directory =
            File(instrumentation.targetContext.getExternalFilesDir(null), captureDirectory).apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "${case}_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

private fun clickableLabel(
    label: String,
    textSubstring: Boolean = false,
) = (hasText(label, substring = textSubstring) or hasContentDescription(label, substring = true)) and hasClickAction()

internal fun ComposeTestRule.clickByLabel(
    label: String,
    textSubstring: Boolean = false,
) {
    val match = clickableLabel(label, textSubstring)
    waitUntil(10_000) { onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(match)[0].scrollToIfScrollable().performClick()
}
