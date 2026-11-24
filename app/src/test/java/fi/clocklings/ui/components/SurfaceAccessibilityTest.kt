package fi.clocklings.ui.components

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Progress
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.localizedContext
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What a screen reader gets from the platform accessibility tree, not only from the Compose semantics: the
 * focusable node itself must carry the label, the role and the state.
 */
@RunWith(RobolectricTestRunner::class)
class SurfaceAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun disabledPrimaryLabelHasReadableContrast() {
        compose.setContent { ClocklingsTheme { SkButton("Done", {}, kind = ButtonKind.PRIMARY, enabled = false) } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText("Done", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val label =
            layouts
                .single()
                .layoutInput.style.color
                .luminance()
        val fill = SkColors.PrimaryContainer.luminance()
        val contrast = (maxOf(label, fill) + 0.05f) / (minOf(label, fill) + 0.05f)
        assertTrue("Disabled primary button contrast: $contrast", contrast >= 4.5f)
    }

    private lateinit var view: View

    private fun info(matcher: SemanticsMatcher): AccessibilityNodeInfo {
        val id = compose.onNode(matcher).fetchSemanticsNode().id
        return compose.runOnIdle { view.accessibilityNodeProvider.createAccessibilityNodeInfo(id)!! }
    }

    @Test
    fun selectedSwatchIsALabelledRadioButtonInItsSelectedState() {
        compose.setContent {
            view = LocalView.current
            ClocklingsTheme {
                Row {
                    SwatchTile(Color.Red, "Orange", true, {})
                    SwatchTile(Color.Blue, "Blue", false, {})
                }
            }
        }
        for ((label, selected) in listOf("Orange" to true, "Blue" to false)) {
            val node = info(hasContentDescription(label))
            assertEquals(label, label, node.contentDescription?.toString())
            assertEquals(label, RadioButton::class.java.name, node.className)
            assertTrue(label, node.isCheckable)
            assertEquals(label, selected, node.isChecked)
            assertTrue(label, node.isFocusable)
        }
    }

    @Test
    fun chapterHeadingNodeCarriesItsOwnLabelAndButtonRole() {
        val progress = Progress(stars = listOf(3, 0, 0, 0, 0), lessonsDone = Lessons.levelLessons[0].toSet())
        compose.setContent {
            view = LocalView.current
            ClocklingsTheme {
                LessonListScreen(
                    false,
                    progress,
                    fi.clocklings.domain.AccessPolicy(fullAccess = true),
                    {},
                    {},
                    {},
                )
            }
        }
        // The first chapter's stops are done, so the heading stop also shows the check mark.
        val first =
            compose.onAllNodes(isHeading()).fetchSemanticsNodes().first {
                it.config.getOrNull(SemanticsProperties.ContentDescription) != null
            }
        val node = compose.runOnIdle { view.accessibilityNodeProvider.createAccessibilityNodeInfo(first.id)!! }
        assertTrue(node.isHeading)
        assertTrue(node.isFocusable)
        assertEquals(Button::class.java.name, node.className)
        assertTrue("${node.contentDescription}", node.contentDescription?.startsWith("Whole hours. Two hands.") == true)
        assertEquals("no generated label children", 0, node.childCount)
    }

    @Test
    fun currentStopOnTheLearnAndPlayPathsAnnouncesItsState() {
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val levels = mutableStateOf(false)
        // The first chapter and the first level are done, so the second ones are current.
        val progress = Progress(stars = listOf(3, 0, 0, 0, 0), lessonsDone = Lessons.levelLessons[0].toSet())
        compose.setContent {
            view = LocalView.current
            val context = localizedContext(language.value)
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalAppLanguage provides language.value,
            ) {
                ClocklingsTheme {
                    if (levels.value) {
                        LevelsScreen(progress, fi.clocklings.domain.AccessPolicy(fullAccess = true), {}, {}, {}, {})
                    } else {
                        LessonListScreen(
                            false,
                            progress,
                            fi.clocklings.domain.AccessPolicy(fullAccess = true),
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        for ((lang, state) in CURRENT) {
            for (onLevels in listOf(false, true)) {
                compose.runOnIdle {
                    language.value = lang
                    levels.value = onLevels
                }
                val label = "$lang levels=$onLevels"
                val stated = compose.onAllNodes(hasStateDescription()).fetchSemanticsNodes()
                assertEquals("$label: one current stop", 1, stated.size)
                val id = stated.single().id
                val node = compose.runOnIdle { view.accessibilityNodeProvider.createAccessibilityNodeInfo(id)!! }
                assertEquals(label, state, node.stateDescription?.toString())
                assertTrue(label, node.isFocusable)
                assertEquals(label, Button::class.java.name, node.className)
                // The stop after the completed ones, labelled on the same node.
                val description = node.contentDescription?.toString().orEmpty()
                assertTrue("$label: $description", description.startsWith(SECOND_STOP.getValue(lang)))
            }
        }
    }

    private fun hasStateDescription() = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    private companion object {
        val CURRENT =
            mapOf(AppLanguage.ENGLISH to "Up next", AppLanguage.FINNISH to "Vuorossa", AppLanguage.SWEDISH to "På tur")

        /** The second lesson chapter and the second level share their title. */
        val SECOND_STOP =
            mapOf(
                AppLanguage.ENGLISH to "Half past.",
                AppLanguage.FINNISH to "Puoli tuntia.",
                AppLanguage.SWEDISH to "Halva timmar.",
            )
    }
}
