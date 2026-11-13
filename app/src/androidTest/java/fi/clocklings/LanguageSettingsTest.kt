package fi.clocklings

import android.app.LocaleManager
import android.os.Build
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.util.Locale

/** Real navigation/resources/storage; the runner supplies each width/font configuration. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class LanguageSettingsTest {
    private val locale = LocaleTestEnvironment()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seededProgress =
        Progress(
            stars = listOf(3, 2, 1, 0, 0),
            challengeBest = 7,
            lessonsDone = setOf("puoli"),
            introShown = setOf("0", "1", "h24"),
        ).withName(CompanionKind.CAT, "Test Companion")
    private val seededSettings =
        Settings(
            sound = false,
            help = false,
            h24 = true,
            personalization =
                Personalization(
                    companion = CompanionKind.CAT,
                    clock = ClockAppearance(theme = ClockTheme.FOREST),
                ),
        )
    private val state =
        object : ExternalResource() {
            private lateinit var progress: Progress
            private lateinit var settings: Settings
            private var legacyLanguage: String? = null
            private var platformLanguage = ""

            override fun before() {
                runBlocking {
                    progress = locale.repository.progress.first()
                    settings = locale.repository.settings.first()
                    legacyLanguage = locale.repository.legacyLanguage.first()
                }
                platformLanguage = locale.prepare(seededProgress, seededSettings)
            }

            override fun after() {
                runBlocking {
                    locale.repository.updateProgress { progress }
                    locale.repository.updateSettings { settings }
                    locale.repository.setLegacyLanguage(legacyLanguage)
                }
                locale.setPlatformLanguage(platformLanguage)
            }
        }

    // Bound the whole test, including setup/cleanup and implicit Compose idle waits.
    @get:Rule val rules: RuleChain = RuleChain.outerRule(Timeout.seconds(120)).around(state).around(compose)

    @Test
    fun homeLanguagePickerPersistsAllChoicesWithoutChangingLearningData() {
        openSettings()
        click("Language: English")
        for ((autonym, language) in languageTestChoices) {
            click(autonym)
            waitText(string(language, R.string.language_title))
            radio(autonym).assertIsSelected()
            assertPersistedLanguage(language)
            compose.activityRule.scenario.recreate()
            waitText(string(language, R.string.language_title))
            radio(autonym).assertIsSelected()
            assertDataUnchanged()
        }
        click("Device language")
        waitSelectedDevice()
        assertPersistedLanguage(null)
        compose.activityRule.scenario.recreate()
        waitSelectedDevice()
        assertDataUnchanged()
    }

    @Test
    fun settingsSwitchesWriteTheExistingPreferencesAndExposeRoles() {
        openSettings()
        val guides = compose.onNode(hasText("Clock guides") and hasClickAction())
        guides
            .scrollToIfScrollable()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch),
            ).assertIsOff()
        guides.performClick()
        compose.waitUntil(10_000) { runBlocking { locale.repository.settings.first() }.help }
        guides.assertIsOn()
        val sound = compose.onNode(hasText("Sound effects") and hasClickAction())
        sound
            .scrollToIfScrollable()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch),
            ).assertIsOff()
        sound.performClick()
        compose.waitUntil(10_000) { runBlocking { locale.repository.settings.first() }.sound }
        sound.assertIsOn()
        click("12 h")
        compose.waitUntil(10_000) { !runBlocking { locale.repository.settings.first() }.h24 }
        radio("12 h").assertIsSelected()
        compose.activityRule.scenario.recreate()
        waitText("Clock display")
        radio("12 h").assertIsSelected()
        compose.onNode(hasText("Clock guides") and hasClickAction()).assertIsOn()
        compose.onNode(hasText("Sound effects") and hasClickAction()).assertIsOn()
        assertEquals(seededProgress, runBlocking { locale.repository.progress.first() })
        assertEquals(seededSettings.personalization, runBlocking { locale.repository.settings.first() }.personalization)
    }

    @Test
    fun resetCancellationAndSuccessfulResetHaveTheDocumentedScope() {
        openSettings()
        click("Reset progress")
        waitText("Reset learning progress?")
        click("Cancel")
        assertDataUnchanged()
        click("Reset progress")
        compose.onAllNodes(hasText("Reset progress") and hasClickAction()).onLast().performClick()
        compose.waitUntil(10_000) { runBlocking { locale.repository.progress.first() == Progress() } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Reset learning progress?").fetchSemanticsNodes().isEmpty()
        }
        assertEquals(seededSettings, runBlocking { locale.repository.settings.first() })
        assertPersistedLanguage(AppLanguage.ENGLISH)
        compose.activityRule.scenario.recreate()
        waitText("Clock display")
        assertEquals(Progress(), runBlocking { locale.repository.progress.first() })
        assertEquals(seededSettings, runBlocking { locale.repository.settings.first() })
    }

    @Test
    fun externalLocaleChangePreservesExploreAndTogetherHiddenAnswer() {
        home()
        click("Explore")
        waitText("15:00")
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5\u00A0minutes")
        waitText("15:05")
        changeExternalLanguage(AppLanguage.FINNISH)
        waitText(string(AppLanguage.FINNISH, R.string.free_title))
        waitText("15.05")
        compose.onNodeWithContentDescription(string(AppLanguage.FINNISH, R.string.help_off)).assertIsDisplayed()
        assertDataUnchanged()
        back()
        click(string(AppLanguage.FINNISH, R.string.menu_kysy))
        val add = locale.resources(AppLanguage.FINNISH).getQuantityString(R.plurals.clock_add, 5, 5)
        compose
            .onNode(
                hasContentDescription("Kellotaulu:", substring = true),
            ).performCustomAccessibilityActionWithLabel(add)
        click(string(AppLanguage.FINNISH, R.string.ask_go))
        waitText(string(AppLanguage.FINNISH, R.string.ask_reveal))
        compose.onAllNodes(hasContentDescription("Kellotaulu:", substring = true)).assertCountEquals(0)
        changeExternalLanguage(AppLanguage.SWEDISH)
        waitText(string(AppLanguage.SWEDISH, R.string.ask_reveal))
        val player = locale.resources(AppLanguage.SWEDISH).getString(R.string.ask_small_eyebrow, 2)
        compose.onNodeWithContentDescription(player, substring = true).assertExists()
        compose.onAllNodes(hasContentDescription("Urtavla:", substring = true)).assertCountEquals(0)
        compose.onAllNodesWithText("15.05").assertCountEquals(0)
        click(string(AppLanguage.SWEDISH, R.string.ask_reveal))
        waitText("15.05")
        assertDataUnchanged()
    }

    @Test
    fun externalLocaleChangeDismissesResetWithoutWritingProgress() {
        openSettings()
        click("Reset progress")
        waitText("Reset learning progress?")
        changeExternalLanguage(AppLanguage.FINNISH)
        waitText(string(AppLanguage.FINNISH, R.string.settings_clock_display))
        compose.onAllNodesWithText(string(AppLanguage.FINNISH, R.string.settings_reset_title)).assertCountEquals(0)
        assertDataUnchanged()
    }

    @Test
    fun settingsLanguageAndResetLayoutsAreReadableInAllLanguages() {
        openSettings()
        click("Language: English")
        for (language in AppLanguage.entries) {
            val autonym =
                when (language) {
                    AppLanguage.ENGLISH -> "English"
                    AppLanguage.FINNISH -> "Suomi"
                    AppLanguage.SWEDISH -> "Svenska"
                    AppLanguage.GERMAN -> "Deutsch"
                    AppLanguage.FRENCH -> "Français"
                    AppLanguage.SPANISH -> "Español (España)"
                    AppLanguage.PORTUGUESE_BRAZIL -> "Português (Brasil)"
                }
            click(autonym)
            waitText(string(language, R.string.language_title))
            assertLanguageOptionsReadable(language)
            compose.waitForIdle()
            LocalizationScreenshots.capture("${language.tag}-language")
            back()
            assertSettingsAndResetReadable(language)
            back()
            waitText(string(language, R.string.settings_title))
            compose.onNodeWithContentDescription(string(language, R.string.settings_title)).assertIsDisplayed()
            compose.waitForIdle()
            LocalizationScreenshots.capture("${language.tag}-home")
            click(string(language, R.string.settings_title))
            click("${string(language, R.string.language_title)}: $autonym")
        }
        assertDataUnchanged()
    }

    private fun speechInfoKey(language: AppLanguage): Int {
        val available = string(language, R.string.settings_speech_info_available)
        return if (compose.onAllNodesWithText(available).fetchSemanticsNodes().isNotEmpty()) {
            R.string.settings_speech_info_available
        } else {
            R.string.settings_speech_info_unavailable
        }
    }

    private fun assertSettingsAndResetReadable(language: AppLanguage) {
        waitText(string(language, R.string.settings_clock_display))
        compose
            .onNodeWithText(string(language, R.string.settings_clock_display))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        for (key in listOf(
            R.string.settings_title,
            R.string.language_title,
            R.string.settings_clock_display,
            R.string.settings_guides_title,
            R.string.settings_guides_description,
            R.string.settings_sound_title,
            R.string.settings_sound_description,
            speechInfoKey(language),
            R.string.settings_progress_title,
            R.string.settings_reset_action,
        )) {
            val screenshot =
                when (key) {
                    R.string.settings_title -> "${language.tag}-settings-top"
                    R.string.settings_guides_description -> "${language.tag}-settings-guides"
                    speechInfoKey(language) -> "${language.tag}-settings-speech-description"
                    R.string.settings_reset_action -> "${language.tag}-settings-progress"
                    else -> null
                }
            assertReadable(string(language, key), screenshot = screenshot)
        }
        click(string(language, R.string.settings_reset_action))
        compose.waitForIdle()
        LocalizationScreenshots.capture("${language.tag}-reset")
        for (key in listOf(
            R.string.settings_reset_title,
            R.string.settings_reset_body,
            R.string.settings_cancel,
            R.string.settings_reset_action,
        )) {
            assertReadable(string(language, key), scroll = key == R.string.settings_reset_body)
        }
        assertResetActionsDoNotOverlap(language)
        scrollResetBodyToEnd(language)
        click(string(language, R.string.settings_cancel))
    }

    private fun assertResetActionsDoNotOverlap(language: AppLanguage) {
        val confirm =
            compose
                .onAllNodes(hasText(string(language, R.string.settings_reset_action)) and hasClickAction())
                .onLast()
                .getUnclippedBoundsInRoot()
        val cancel =
            compose
                .onNode(hasText(string(language, R.string.settings_cancel)) and hasClickAction())
                .getUnclippedBoundsInRoot()
        assertTrue(
            "Reset dialog actions overlap: confirm=$confirm, cancel=$cancel",
            confirm.right <= cancel.left || cancel.right <= confirm.left ||
                confirm.bottom <= cancel.top || cancel.bottom <= confirm.top,
        )
    }

    private fun scrollResetBodyToEnd(language: AppLanguage) {
        val body = string(language, R.string.settings_reset_body)
        val scroll = compose.onNode(hasScrollAction() and hasAnyDescendant(hasText(body)))
        val axis = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        val remaining = axis.maxValue() - axis.value()
        if (remaining > 0f) {
            scroll.performSemanticsAction(SemanticsActions.ScrollBy) { action -> assertTrue(action(0f, remaining)) }
        }
        compose.waitUntil(10_000) {
            val current = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            current.value() >= current.maxValue() - 0.5f
        }
        compose.waitForIdle()
        LocalizationScreenshots.capture("${language.tag}-reset-bottom")
        val viewport = scroll.fetchSemanticsNode().boundsInRoot
        val bodyBounds = compose.onNodeWithText(body, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val bottom = bodyBounds.bottom.value * locale.context.resources.displayMetrics.density
        assertTrue(
            "Reset body end is hidden: body=$bodyBounds, viewport=$viewport",
            bottom <= viewport.bottom + 1f && bottom > viewport.top,
        )
    }

    private fun assertLanguageOptionsReadable(language: AppLanguage) {
        for (key in listOf(
            R.string.language_device,
            R.string.language_english,
            R.string.language_finnish,
            R.string.language_swedish,
            R.string.language_german,
            R.string.language_french,
            R.string.language_spanish,
            R.string.language_portuguese_brazil,
        )) {
            val text = string(language, key)
            assertReadable(text)
            val node = radio(text).scrollToIfScrollable().fetchSemanticsNode()
            val density = locale.context.resources.displayMetrics.density
            assertTrue("Radio touch height: $text", node.boundsInRoot.height / density >= 47.5f)
        }
    }

    private fun home() {
        compose.waitUntil(15_000) {
            if (compose.onAllNodesWithText("Skip intro").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Skip intro").performClick()
            }
            compose.onAllNodes(hasContentDescription("Settings")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSettings() {
        home()
        click("Settings")
        waitText("Clock display")
    }

    private fun click(text: String) {
        waitText(text)
        val node =
            compose
                .onAllNodes(
                    (hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction(),
                ).onLast()
        if (generateSequence(node.fetchSemanticsNode().parent) { it.parent }.any {
                SemanticsActions.ScrollBy in
                    it.config
            }
        ) {
            node.scrollToIfScrollable()
        }
        node.assertIsDisplayed().performClick()
    }

    private fun waitText(text: String) {
        compose.waitUntil(15_000) {
            compose
                .onAllNodes(
                    hasText(text) or hasContentDescription(text, substring = true),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun radio(text: String) =
        compose.onNode(
            hasText(text) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )

    private fun waitSelectedDevice() {
        compose.waitUntil(15_000) {
            compose
                .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrElse(SemanticsProperties.Selected) { false } == true
        }
    }

    private fun back() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun string(
        language: AppLanguage,
        id: Int,
    ) = locale.resources(language).getString(id)

    private fun assertPersistedLanguage(language: AppLanguage?) {
        compose.waitUntil(10_000) {
            if (Build.VERSION.SDK_INT >= 33) {
                locale.context
                    .getSystemService(LocaleManager::class.java)
                    .applicationLocales
                    .toLanguageTags() ==
                    language?.tag.orEmpty()
            } else {
                runBlocking { locale.repository.legacyLanguage.first() } == language?.tag
            }
        }
    }

    private fun assertDataUnchanged() {
        assertEquals(seededProgress, runBlocking { locale.repository.progress.first() })
        assertEquals(seededSettings, runBlocking { locale.repository.settings.first() })
    }

    private fun changeExternalLanguage(language: AppLanguage) {
        locale.changeLanguage(language, compose.activityRule.scenario)
    }

    private fun assertReadable(
        text: String,
        scroll: Boolean = true,
        screenshot: String? = null,
    ) {
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true).onLast()
        if (scroll) node.scrollToIfScrollable()
        compose.waitForIdle()
        screenshot?.let(LocalizationScreenshots::capture)
        node.assertIsDisplayed()
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue(
            "Text extends outside screen: $text",
            bounds.left.value >= -1f && bounds.right.value <= locale.context.resources.configuration.screenWidthDp + 1f,
        )
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("No actual text layout: $text", layouts.isNotEmpty())
        for (layout in layouts) {
            assertFalse("Height overflow: $text", layout.didOverflowHeight)
            for (line in 0 until layout.lineCount) {
                assertFalse("Ellipsized line: $text", layout.isLineEllipsized(line))
                assertTrue(
                    "Line overflow: $text; line=$line, left=${layout.getLineLeft(line)}, " +
                        "right=${layout.getLineRight(line)}, size=${layout.size}, " +
                        "paragraphWidth=${layout.multiParagraph.width}, overflowWidth=${layout.didOverflowWidth}, " +
                        "nodeBounds=${node.fetchSemanticsNode().boundsInRoot}",
                    // Center-aligned paragraphs can keep their wider allocation after the node wraps its ink.
                    layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f,
                )
                if (line < layout.lineCount - 1) {
                    val end = layout.getLineEnd(line)
                    assertTrue(
                        "Word split: $text; line=$line, start=${layout.getLineStart(line)}, end=$end, " +
                            "before='${text.substring(maxOf(0, end - 12), end)}', " +
                            "after='${text.substring(end, minOf(text.length, end + 12))}', " +
                            "previousCode=${text.getOrNull(end - 1)?.code}, nextCode=${text.getOrNull(end)?.code}",
                        end !in 1 until text.length || text[end - 1].isWhitespace() || text[end - 1] == '-' ||
                            text[end].isWhitespace(),
                    )
                }
            }
        }
    }
}
