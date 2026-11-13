package fi.clocklings

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.theme.SkColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/** Real window/key/IME observations; display settings are controlled by the dedicated AVD runner. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DeviceInputVerificationTest : InputDeviceTest() {
    override val requireCutout = true

    @Test
    fun keyboardOnlyExploreAndResetCancellation() {
        activate(hasText("Explore") and hasClickAction())
        val clock = hasContentDescription("Clock face:", substring = true)
        focus(clock)
        compose.onNode(clock).assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("3:05").assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithText("3:00").assertIsDisplayed()
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
        activate(hasContentDescription("Settings") and hasClickAction())
        activate(hasText("Reset progress") and hasClickAction())
        waitFor(hasText("Reset learning progress?"))
        activate(hasText("Cancel") and hasClickAction())
        compose.onNodeWithText("Reset learning progress?").assertDoesNotExist()
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun keyboardOnlyWholeLessonCommitsAndReturnsHome() {
        activate((hasText("Learn") or hasContentDescription("Learn", substring = true)) and hasClickAction())
        activate((hasText("Two hands") or hasContentDescription("Two hands", substring = true)) and hasClickAction())
        repeat(2) { activate(hasText("Next") and hasClickAction()) }
        activate(hasText("Try it") and hasClickAction())
        activate(hasText("The short pink hand") and hasClickAction())
        waitFor(hasText("You completed Two hands.", substring = true))
        runBlocking {
            withTimeout(5_000) { environment.repository.progress.first { "viisarit" in it.lessonsDone } }
        }
        activate(hasText("Done") and hasClickAction())
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun keyboardOnlyWholePracticeCommitsAndReachesResultActions() {
        activate(hasText("Play") and hasClickAction())
        activate(action("Whole hours"))
        activate(action("Skip and play"))
        val clock = hasContentDescription("Clock face:", substring = true)
        repeat(10) { task ->
            waitFor(hasTestTag("game_task_$task"))
            if (compose.onAllNodes(hasText("What time is it?")).fetchSemanticsNodes().isNotEmpty()) {
                val choices =
                    compose
                        .onAllNodes(hasText("o'clock", substring = true) and hasClickAction())
                        .fetchSemanticsNodes()
                        .map { it.config[SemanticsProperties.Text].first().text }
                assertTrue("Reading task exposes answer choices", choices.isNotEmpty())
                for (choice in choices) {
                    activate(action(choice))
                    if (compose.onAllNodes(clock).fetchSemanticsNodes().isNotEmpty()) break
                }
                if (compose.onAllNodes(action("Next")).fetchSemanticsNodes().isNotEmpty()) activate(action("Next"))
            } else {
                val target =
                    compose
                        .onAllNodes(hasText("o'clock", substring = true))
                        .fetchSemanticsNodes()
                        .first()
                        .config[SemanticsProperties.Text]
                        .first()
                        .text
                        .lowercase()
                focus(clock)
                repeat(12) {
                    if (clockWords() != target) key(KeyEvent.KEYCODE_DPAD_RIGHT)
                }
                assertEquals("Keyboard sets task $task", target, clockWords())
                activate(action("Done"))
            }
        }
        waitFor(hasText("Whole hours complete!", substring = true))
        runBlocking {
            withTimeout(5_000) { environment.repository.progress.first { it.stars[0] > 0 } }
        }
        for (label in listOf("Play again", "Next level", "Choose an outfit", "Back")) {
            focus(action(label))
            compose.onNode(action(label)).assertIsDisplayed()
        }
        activate(action("Choose an outfit"))
        waitFor(hasText("Outfits"))
        key(KeyEvent.KEYCODE_BACK)
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    private fun action(label: String) =
        (hasText(label) or hasContentDescription(label) or hasContentDescription("$label.", substring = true)) and
            hasClickAction()

    @Test
    fun keyboardOnlyTogetherCompletesBothTurns() {
        activate(action("Play Together"))
        val clock = hasContentDescription("Clock face:", substring = true)
        focus(clock)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        val chosen = clockWords()
        activate(action("Pass to partner"))
        waitFor(hasText("What time is it?"))
        compose.onNode(action("Show the answer") and isFocused()).assertExists()
        compose.onNode(clock).assertDoesNotExist()
        activate(action("Show the answer"))
        compose.onNode(action("Swap turns") and isFocused()).assertExists()
        assertEquals(chosen, clockWords())
        activate(action("Swap turns"))
        compose.onNode(action("Pass to partner") and isFocused()).assertExists()
        waitFor(hasContentDescription("Player 2 sets the time", substring = true))
        activate(action("Random time"))
        val second = clockWords()
        activate(action("Pass to partner"))
        activate(action("Show the answer"))
        assertEquals(second, clockWords())
        activate(action("Back"))
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun keyboardOnlyAppearanceWardrobeAndNameCommit() {
        runBlocking { environment.repository.updateProgress { it.withLevelResult(0, 1) } }
        activate(hasContentDescription("My Companion") and hasClickAction())
        activate(hasTestTag("choose_companion_CAT") and hasClickAction())
        runBlocking {
            checkNotNull(
                withTimeoutOrNull(5_000) {
                    environment.repository.settings.first { it.personalization.companion == CompanionKind.CAT }
                },
            ) { "Keyboard did not commit the selected cat" }
        }
        activate(action("Choose an outfit"))
        activate(hasTestTag("color_BLUE") and hasClickAction())
        runBlocking {
            checkNotNull(
                withTimeoutOrNull(5_000) {
                    environment.repository.settings.first {
                        it.personalization.styleFor(CompanionKind.CAT).color == CompanionColor.BLUE
                    }
                },
            ) { "Keyboard did not commit the cat colour" }
        }
        activate(action("Back"))
        activate(hasTestTag("choose_companion_HUMAN") and hasClickAction())
        runBlocking {
            checkNotNull(
                withTimeoutOrNull(5_000) {
                    environment.repository.settings.first { it.personalization.companion == CompanionKind.HUMAN }
                },
            ) { "Keyboard did not commit the selected human" }
        }
        activate(action("Choose an outfit"))
        focus(hasSetTextAction())
        val nameField = compose.onNode(hasSetTextAction()).assertIsDisplayed()
        instrumentation.sendStringSync("Keys")
        compose.waitUntil(5_000) {
            nameField.fetchSemanticsNode().config[SemanticsProperties.EditableText].text == "Keys"
        }
        key(KeyEvent.KEYCODE_TAB)
        activate(action("Polka-dot dress"))
        compose.waitUntil(5_000) {
            compose.onNode(action("Polka-dot dress")).fetchSemanticsNode().config[SemanticsProperties.Selected]
        }
        runBlocking {
            checkNotNull(
                withTimeoutOrNull(5_000) {
                    environment.repository.progress.first {
                        it.nameFor(CompanionKind.HUMAN) == "Keys"
                    }
                },
            ) { "Keyboard name was '${environment.repository.progress.first().nameFor(CompanionKind.HUMAN)}'" }
        }
        activate(action("Back"))
        activate(action("Clock styles"))
        activate(hasTestTag("choose_clock_CLASSIC") and hasClickAction())
        activate(action("Back"))
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun keyboardOnlyChallengeAnswersFinishesAndRestarts() {
        activate(hasText("Play") and hasClickAction())
        activate(action("Minute Challenge"))
        compose.mainClock.autoAdvance = false
        activate(action("Start!"))
        val answers =
            (
                hasText("past", substring = true) or hasText("to", substring = true) or
                    hasText("o'clock", substring = true)
            ) and hasClickAction()
        waitFor(answers)
        val choice =
            compose
                .onAllNodes(answers)
                .fetchSemanticsNodes()
                .first()
                .config[SemanticsProperties.Text]
                .first()
                .text
        activate(hasText(choice) and hasClickAction())
        compose.waitUntil(5_000) {
            compose.mainClock.advanceTimeByFrame()
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
                .fetchSemanticsNodes()
                .any {
                    it.config[SemanticsProperties.StateDescription] in listOf("Correct", "Incorrect")
                }
        }
        compose.waitUntil(70_000) {
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodes(action("Play again")).fetchSemanticsNodes().isNotEmpty()
        }
        focus(action("Play again"))
        compose.onNode(action("Play again")).assertIsDisplayed()
        activate(hasText("Back") and hasClickAction())
        activate(action("Minute Challenge"))
        activate(action("Start!"))
        waitFor(answers)
        key(KeyEvent.KEYCODE_BACK)
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun keyboardOnlyLanguageChoicesSurviveActualLocaleChanges() {
        activate(action("Settings"))
        activate(action("Language: English"))
        for ((label, language) in languageTestChoices) {
            activate(action(label))
            waitFor(hasText(environment.resources(language).getString(R.string.language_title)))
            compose.waitUntil(5_000) {
                compose.onNode(action(label)).fetchSemanticsNode().config[SemanticsProperties.Selected]
            }
        }
        activate(action("Back"))
        activate(action("Back"))
        waitFor(hasText("Clocklings"))
    }

    private fun clockWords(): String =
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first { it.startsWith("Clock face:") }
            .removePrefix("Clock face: ")

    @Test
    fun directionalFocusOrderAndRenderedIndicators() {
        verifyHomeDirections()
        verifySettingsFocus()
        runBlocking {
            environment.repository.updateSettings {
                it.copy(personalization = it.personalization.copy(companion = CompanionKind.CAT))
            }
        }
        activate(hasContentDescription("My Companion") and hasClickAction())
        focus(action("Companions"))
        focusEvidence("segment", action("Clock styles")) { key(KeyEvent.KEYCODE_DPAD_RIGHT) }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNode(action("Clock styles")).assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNode(action("Companions")).assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        activate(action("Choose an outfit"))
        focus(hasTestTag("color_ORIGINAL"))
        focusEvidence("swatch", hasTestTag("color_BLUE"), inkBorder = true) { key(KeyEvent.KEYCODE_DPAD_RIGHT) }
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNode(hasTestTag("color_ORIGINAL")).assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        runBlocking {
            withTimeout(5_000) {
                environment.repository.settings.first {
                    it.personalization.styleFor(CompanionKind.CAT).color == CompanionColor.BLUE
                }
            }
        }
        key(KeyEvent.KEYCODE_BACK)
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun lessonAnswerShowsRenderedKeyboardFocus() {
        val language =
            AppLanguage.fromTag(InstrumentationRegistry.getArguments().getString("language")) ?: AppLanguage.ENGLISH
        environment.changeLanguage(language, compose.activityRule.scenario)
        waitFor(hasText("Clocklings"))
        val resources = environment.resources(language)
        activate(action(resources.getString(R.string.menu_koulu)))
        activate(action(resources.getString(R.string.l_viisarit_title)))
        repeat(2) { activate(action(resources.getString(R.string.next))) }
        activate(action(resources.getString(R.string.lesson_try)))
        val answer = action(resources.getString(R.string.l_viisarit_c1))
        focus(action(resources.getString(R.string.back_description)))
        focusEvidence("lesson-answer", answer, inkBorder = true, directoryName = "audit-accessibility-focus") {
            focus(answer)
        }
    }

    private fun verifyHomeDirections() {
        val singleColumn =
            compose
                .onNode(action("Learn"))
                .fetchSemanticsNode()
                .boundsInRoot.top !=
                compose
                    .onNode(action("Explore"))
                    .fetchSemanticsNode()
                    .boundsInRoot.top
        focus(action("Explore"))
        focusEvidence("menu", action("Learn"), inkBorder = true) {
            key(if (singleColumn) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_LEFT)
        }
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNode(action(if (singleColumn) "Explore" else "Play")).assertIsFocused()
        if (!singleColumn) {
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            compose.onNode(action("Play Together")).assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_UP)
            compose.onNode(action("Explore")).assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_LEFT)
        } else {
            key(KeyEvent.KEYCODE_DPAD_UP)
        }
        compose.onNode(action("Learn")).assertIsFocused()
    }

    private fun verifySettingsFocus() {
        val settings = hasContentDescription("Settings") and hasClickAction()
        focusEvidence("icon", settings, inkBorder = true) { focus(settings) }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitFor(hasText("Sound effects"))
        focus(action("Back"))
        focusEvidence("switch", hasText("Sound effects") and hasClickAction()) {
            focus(hasText("Sound effects") and hasClickAction())
        }
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun cancelledClockPointerAllowsTheNextDragAndKeyboardEdit() {
        activate(action("Explore"))
        val clock = hasContentDescription("Clock face:", substring = true)
        val node = compose.onNode(clock)
        node.assertIsDisplayed()
        node.performTouchInput {
            down(Offset(center.x, center.y - height * 0.35f))
            moveTo(Offset(center.x + width * 0.35f, center.y), delayMillis = 100)
            cancel()
        }
        compose.onNodeWithText("3:15").assertIsDisplayed()
        node.performTouchInput {
            down(Offset(center.x + width * 0.35f, center.y))
            moveTo(Offset(center.x, center.y + height * 0.35f), delayMillis = 100)
            up()
        }
        compose.onNodeWithText("3:30").assertIsDisplayed()
        focus(clock)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("3:35").assertIsDisplayed()
        key(KeyEvent.KEYCODE_BACK)
        waitFor(hasText("Clocklings"))
    }

    @Test
    fun realKeyboardSelectionAndPasteCommitTheSelectedReplacement() {
        click(hasContentDescription("My Companion", substring = true) and hasClickAction())
        click(hasText("Choose an outfit") and hasClickAction())
        val field = compose.onNode(hasSetTextAction())
        field.scrollToIfScrollable().performTouchInput { click() }
        compose.waitUntil(5_000) { imeVisible() }
        waitForImeLayout(field)
        instrumentation.sendStringSync("Clock")
        val clipboard = compose.activity.getSystemService(ClipboardManager::class.java)
        var previous: ClipData? = null
        instrumentation.runOnMainSync {
            previous = clipboard.primaryClip
            clipboard.setPrimaryClip(ClipData.newPlainText("Clocklings synthetic test", "Clök 12"))
        }
        try {
            controlKey(KeyEvent.KEYCODE_A)
            compose.waitUntil(5_000) {
                val node = field.fetchSemanticsNode()
                val selected = node.config[SemanticsProperties.TextSelectionRange]
                selected.min == 0 && selected.max == node.config[SemanticsProperties.EditableText].length
            }
            val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
            assertEquals(0, selection.min)
            assertEquals(field.fetchSemanticsNode().config[SemanticsProperties.EditableText].length, selection.max)
            controlKey(KeyEvent.KEYCODE_V)
            compose.waitUntil(5_000) {
                field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text == "Clök 12"
            }
            key(KeyEvent.KEYCODE_BACK)
            runBlocking {
                withTimeout(5_000) {
                    environment.repository.progress.first { it.nameFor(CompanionKind.HUMAN) == "Clök 12" }
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                previous?.let(clipboard::setPrimaryClip) ?: clipboard.clearPrimaryClip()
            }
        }
    }

    @Test
    fun realImeKeepsNameReachableAndCursorEditsCommit() {
        for (language in listOf(AppLanguage.ENGLISH, AppLanguage.FINNISH, AppLanguage.SWEDISH, AppLanguage.GERMAN)) {
            runBlocking { environment.repository.updateProgress { Progress() } }
            environment.changeLanguage(language, compose.activityRule.scenario)
            val resources = environment.resources(language)
            waitFor(hasText("Clocklings"))
            val companion = resources.getString(R.string.menu_nukke)
            click(hasContentDescription(companion, substring = true) and hasClickAction())
            click(hasText(resources.getString(R.string.dress_doll)) and hasClickAction())
            val field = compose.onNode(hasSetTextAction())
            field.scrollToIfScrollable().performTouchInput { click() }
            compose.waitUntil(10_000) { imeVisible() }
            waitForImeLayout(field)
            compose.waitForIdle()
            field.assertIsDisplayed()
            val bounds = field.fetchSemanticsNode().boundsInWindow
            val fullHeight = field.fetchSemanticsNode().size.height
            assertEquals(fullHeight.toFloat(), bounds.height, 1f)
            compose.runOnIdle {
                val decor = compose.activity.window.decorView
                val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
                val top =
                    insets
                        .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                        .top
                val bottom =
                    insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars()).bottom
                assertTrue("Name above top obstruction: $bounds / $top", bounds.top >= top)
                val visibleBottom = decor.height - bottom
                assertTrue("Name above real IME: $bounds / $visibleBottom", bounds.bottom <= visibleBottom)
            }
            instrumentation.sendStringSync("Clock")
            key(KeyEvent.KEYCODE_MOVE_END)
            key(KeyEvent.KEYCODE_DPAD_LEFT)
            key(KeyEvent.KEYCODE_DEL)
            key(KeyEvent.KEYCODE_B)
            compose.waitUntil(5_000) {
                field
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.EditableText]
                    .text
                    .endsWith("Clobk")
            }
            key(KeyEvent.KEYCODE_BACK)
            compose.waitUntil(5_000) { !imeVisible() }
            val edited = field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
            runBlocking {
                val saved =
                    withTimeout(5_000) {
                        environment.repository.progress.first { it.nameFor(CompanionKind.HUMAN) == edited.trim() }
                    }
                assertEquals(edited.trim(), saved.nameFor(CompanionKind.HUMAN))
            }
            key(KeyEvent.KEYCODE_BACK)
            key(KeyEvent.KEYCODE_BACK)
            waitFor(hasText("Clocklings"))
        }
    }

    private fun imeVisible(): Boolean {
        var visible = false
        instrumentation.runOnMainSync {
            visible =
                ViewCompat
                    .getRootWindowInsets(compose.activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }

    private fun controlKey(code: Int) {
        val command = "input keycombination ${KeyEvent.KEYCODE_CTRL_LEFT} $code"
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.readBytes()
        }
        compose.waitForIdle()
    }
}
