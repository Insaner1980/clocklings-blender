package fi.clocklings

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.theme.SkColors
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

/** Remaining finite audit criteria on the dedicated test emulator. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AuditCompletionDeviceTest : InputDeviceTest() {
    override val requireCutout = true

    @Test
    fun homeArtworkCanFitInsideTheWindow() {
        artworkLanguage()
        assertArtworkFits(hasTestTag("companion_stage"))
    }

    @Test
    fun clockStylePreviewCanFitInsideTheWindow() {
        val resources = environment.resources(artworkLanguage())
        click(hasContentDescription(resources.getString(R.string.menu_nukke), substring = true) and hasClickAction())
        click(action(resources.getString(R.string.clock_styles_title)))
        assertArtworkFits(hasTestTag("selected_clock"))
    }

    private fun artworkLanguage(): AppLanguage {
        val language =
            AppLanguage.fromTag(InstrumentationRegistry.getArguments().getString("language")) ?: AppLanguage.ENGLISH
        environment.changeLanguage(language, compose.activityRule.scenario)
        val companionName = environment.resources(language).getString(R.string.menu_nukke)
        waitFor(
            hasContentDescription(companionName, substring = true) and hasClickAction(),
        )
        return language
    }

    private fun assertArtworkFits(matcher: SemanticsMatcher) {
        val artwork = compose.onNode(matcher).scrollToIfScrollable()
        val viewport = compose.onRoot().getUnclippedBoundsInRoot()
        val bounds = artwork.getUnclippedBoundsInRoot()
        val artworkHeight = bounds.bottom - bounds.top
        val viewportHeight = viewport.bottom - viewport.top
        assertTrue(
            "Artwork height $artworkHeight must fit window height $viewportHeight",
            artworkHeight <= viewportHeight,
        )
    }

    @Test
    fun remainingControlTypesShowRenderedFocus() {
        activate(action("Explore"))
        val clock = hasContentDescription("Clock face:", substring = true)
        focus(action("Back"))
        focusEvidence("clock", clock, inkBorder = true, directoryName = "audit-completion-focus") { focus(clock) }
        key(KeyEvent.KEYCODE_BACK)
        activate(hasContentDescription("Settings") and hasClickAction())
        val resources = environment.resources(AppLanguage.ENGLISH)
        for ((label, matcher) in listOf(
            "radio" to action(resources.getString(R.string.mode_24)),
            "language-row" to (hasContentDescription("Language:", substring = true) and hasClickAction()),
            "reset-row" to action("Reset progress"),
        )) {
            focus(action("Back"))
            focusEvidence(label, matcher, directoryName = "audit-completion-focus") { focus(matcher) }
        }
        key(KeyEvent.KEYCODE_BACK)
        activate(hasContentDescription("My Companion", substring = true) and hasClickAction())
        activate(action("Choose an outfit"))
        focus(action("Back"))
        focusEvidence(
            "name",
            hasSetTextAction(),
            directoryName = "audit-completion-focus",
        ) { focus(hasSetTextAction()) }
    }

    @Test
    fun secondClockPointerDoesNotMoveTheActiveHand() {
        activate(action("Explore"))
        val clock = compose.onNode(hasContentDescription("Clock face:", substring = true))
        clock.performTouchInput {
            down(0, Offset(center.x, center.y - height * 0.35f))
            down(1, Offset(center.x + width * 0.35f, center.y))
            moveTo(1, Offset(center.x, center.y + height * 0.35f))
            up(1)
        }
        compose.onNodeWithText("3:00").assertIsDisplayed()
        clock.performTouchInput {
            moveTo(0, Offset(center.x + width * 0.35f, center.y))
            up(0)
        }
        compose.onNodeWithText("3:15").assertIsDisplayed()
        focus(hasContentDescription("Clock face:", substring = true))
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("3:20").assertIsDisplayed()
    }

    @Test
    fun nameDraftSurvivesActivityRecreation() {
        activate(hasContentDescription("My Companion", substring = true) and hasClickAction())
        activate(action("Choose an outfit"))
        focus(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement("Draft ")
        assertEquals(
            "Draft ",
            compose
                .onNode(hasSetTextAction())
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        compose.activityRule.scenario.recreate()
        waitFor(hasSetTextAction())
        assertEquals(
            "Draft including its unsaved trailing space survives recreation",
            "Draft ",
            compose
                .onNode(hasSetTextAction())
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
    }

    @Test
    fun settingsScrollSurvivesActivityRecreation() {
        click(hasContentDescription("Settings") and hasClickAction())
        val speechReady =
            hasText(environment.resources(AppLanguage.ENGLISH).getString(R.string.settings_speech_info_available))
        waitFor(speechReady)
        compose
            .onNode(
                hasText(
                    environment.resources(AppLanguage.ENGLISH).getString(R.string.settings_speech_info_unavailable),
                ),
            ).assertDoesNotExist()
        val scrolling = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        repeat(3) { compose.onNode(scrolling).performTouchInput { swipeUp(durationMillis = 600) } }
        compose.onNode(action("Reset progress")).assertIsDisplayed()
        val before =
            compose
                .onNode(
                    scrolling,
                ).fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        compose.activityRule.scenario.recreate()
        waitFor(action("Reset progress"))
        waitFor(speechReady)
        compose.onNode(action("Reset progress")).assertIsDisplayed()
        val restored =
            compose
                .onNode(
                    scrolling,
                ).fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertEquals("Settings retains scroll offset", before, restored, 1f)
    }

    private fun action(label: String) =
        (hasText(label) or hasContentDescription(label) or hasContentDescription("$label.", substring = true)) and
            hasClickAction()
}
