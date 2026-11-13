package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.companion.NameField
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdaptiveContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun availableWidthAndLargeTextReflowWithoutReplacingTheEditor() {
        val width = mutableIntStateOf(320)
        val font = mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font.floatValue)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(width.intValue.dp, 800.dp)) {
                        ScreenScaffold(wideContent = true) { size ->
                            AdaptiveContent(size, visual = { ClockBox(it) {} }) {
                                NameField("Clockling", {})
                            }
                        }
                    }
                }
            }
        }
        val field = compose.onNodeWithContentDescription("Companion name")
        field.performTextInputSelection(TextRange(1, 4))
        val id = field.fetchSemanticsNode().id
        for ((window, scale, horizontal) in listOf(
            Triple(320, 1f, false),
            Triple(800, 1f, true),
            Triple(800, 2f, false),
            Triple(1120, 2f, true),
            Triple(320, 2f, false),
        )) {
            compose.runOnIdle {
                width.intValue = window
                font.floatValue = scale
            }
            val visual = compose.onNodeWithTag("adaptive_visual").fetchSemanticsNode().boundsInRoot
            val controls = compose.onNodeWithTag("adaptive_controls").fetchSemanticsNode().boundsInRoot
            if (horizontal) {
                assertTrue("$window/$scale: controls beside clock", controls.left >= visual.right)
            } else {
                assertTrue("$window/$scale: controls below clock", controls.top >= visual.bottom)
            }
            assertEquals(id, field.fetchSemanticsNode().id)
            assertEquals(TextRange(1, 4), field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        }
    }

    @Test
    fun shortWindowKeepsTaskActionsPinnedWhileContentScrollsAndResizes() {
        val window = mutableStateOf(Triple(1000, 360, 1f))
        var presses = 0
        lateinit var scroll: androidx.compose.foundation.ScrollState
        compose.setContent {
            val density = LocalDensity.current
            val (width, height, font) = window.value
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                ClocklingsTheme {
                    scroll = rememberScrollState()
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("viewport")) {
                        ScreenScaffold(
                            scrollState = scroll,
                            topBar = { TopBar("Lesson", {}) },
                            bottomBar = { SkButton("Next", { presses++ }, Modifier.testTag("next")) },
                        ) {
                            Box(Modifier.height(1200.dp))
                        }
                    }
                }
            }
        }
        val cells =
            listOf(
                Triple(1000, 360, 1f),
                Triple(1280, 720, 1f),
                Triple(320, 800, 1f),
                Triple(420, 900, 2f),
                Triple(320, 640, 2f),
                Triple(1000, 360, 2f),
            )
        for (cell in cells) {
            compose.runOnIdle { window.value = cell }
            compose.waitForIdle()
            val root = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("next")
            val before = action.fetchSemanticsNode()
            val top = before.positionInRoot.y
            assertTrue("$cell: entire action visible", top >= root.top && top + before.size.height <= root.bottom)
            compose.runOnIdle { runBlocking { scroll.scrollTo(scroll.maxValue) } }
            assertEquals(top, action.fetchSemanticsNode().positionInRoot.y)
            action.performClick()
        }
        assertEquals(cells.size, presses)
    }

    @Test
    fun wideTaskUsesTheViewportAndAlignsUnequalPanesVertically() {
        compose.setContent {
            ClocklingsTheme {
                Box(Modifier.requiredSize(1280.dp, 720.dp).testTag("viewport")) {
                    ScreenScaffold(
                        wideContent = true,
                        centerContent = true,
                        topBar = { TopBar("Practice", {}) },
                    ) { size ->
                        AdaptiveContent(
                            size,
                            visual = { ClockBox(it) {} },
                            controlsAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.height(180.dp))
                        }
                    }
                }
            }
        }
        val root = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val visual = compose.onNodeWithTag("adaptive_visual").fetchSemanticsNode()
        val controls = compose.onNodeWithTag("adaptive_controls").fetchSemanticsNode()
        assertEquals(
            visual.positionInRoot.y + visual.size.height / 2f,
            controls.positionInRoot.y + controls.size.height / 2f,
            1f,
        )
        assertTrue("Task has space above it", visual.positionInRoot.y > root.top + root.height * 0.15f)
        assertTrue("Task remains fully visible", visual.positionInRoot.y + visual.size.height < root.bottom)
    }

    @Test
    fun nameSelectionSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val text = rememberSaveable { mutableStateOf("Clockling") }
            ClocklingsTheme { NameField(text.value, { text.value = it }) }
        }
        val field = compose.onNodeWithContentDescription("Companion name")
        field.performTextInputSelection(TextRange(1, 4))
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(TextRange(1, 4), field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
    }
}
