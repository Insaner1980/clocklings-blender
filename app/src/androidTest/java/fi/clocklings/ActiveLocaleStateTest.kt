package fi.clocklings

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Actual Activity/navigation state; no replacement ViewModels or test-only application hooks. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ActiveLocaleStateTest {
    private val locale = LocaleTestEnvironment()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seed = Progress(introShown = setOf("0", "1", "2", "3", "4", "h24"))
    private val settings = Settings(sound = false, help = false)
    private val state =
        object : ExternalResource() {
            private lateinit var originalProgress: Progress
            private lateinit var originalSettings: Settings
            private var originalLegacy: String? = null
            private var originalPlatform = ""

            override fun before() {
                runBlocking {
                    originalProgress = locale.repository.progress.first()
                    originalSettings = locale.repository.settings.first()
                    originalLegacy = locale.repository.legacyLanguage.first()
                }
                originalPlatform = locale.prepare(seed, settings)
            }

            override fun after() {
                runBlocking {
                    locale.repository.updateProgress { originalProgress }
                    locale.repository.updateSettings { originalSettings }
                    locale.repository.setLegacyLanguage(originalLegacy)
                }
                locale.setPlatformLanguage(originalPlatform)
            }
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(state).around(compose)

    @Test
    fun activePracticeKeepsAnswerIdentitiesAndResultKeepsEarnedProgress() {
        home()
        click("Play")
        waitText("Whole hours")
        click("Whole hours")
        waitText("What time is it?")
        val choices = optionTimes()
        assertEquals(3, choices.size)
        changeLanguage(AppLanguage.FINNISH)
        waitText("Mitä kello on?")
        assertEquals(choices, optionTimes())
        assertHiddenClock(AppLanguage.FINNISH)
        assertEquals(seed, runBlocking { locale.repository.progress.first() })

        // Complete the real ten-question round using only visible choices/feedback.
        repeat(10) { index ->
            waitText(if (index % 2 == 0) "Mitä kello on?" else "Aseta kelloon aika")
            if (index % 2 == 0) {
                finishReadQuestion()
            } else {
                // Whole-hour SET questions deliberately start away from the answer.
                click("Valmis")
                click("Valmis")
                click("Seuraava")
            }
        }
        waitText("Pelaa uudelleen")
        val completed = runBlocking { locale.repository.progress.first() }
        assertTrue(completed.stars[0] > 0)
        assertEquals(seed.withLevelResult(0, completed.stars[0]), completed)
        assertResultStars(AppLanguage.FINNISH, completed.stars[0])
        changeLanguage(AppLanguage.SWEDISH)
        waitText("Spela igen")
        assertResultStars(AppLanguage.SWEDISH, completed.stars[0])
        compose.activityRule.scenario.recreate()
        waitText("Spela igen")
        assertResultStars(AppLanguage.SWEDISH, completed.stars[0])
        assertEquals(completed, runBlocking { locale.repository.progress.first() })
        assertEquals(settings, runBlocking { locale.repository.settings.first() })
        assertNoIntro(AppLanguage.SWEDISH)
    }

    @Test
    fun adjustedPracticeClockSurvivesActivityRecreation() {
        home()
        click("Play")
        click("Whole hours")
        waitText("What time is it?")
        val labels =
            compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().mapNotNull { node ->
                node.config
                    .getOrElse(SemanticsProperties.Text) { emptyList() }
                    .firstOrNull { DIGITAL.matches(it.text) }
                    ?.text
            }
        for (label in labels) {
            click(label)
            if (!shown("What time is it?") || shown("Clock face:")) break
        }
        if (shown("Next")) click("Next")
        waitText("Set the clock to")
        val clock = compose.onNode(hasContentDescription("Clock face:", substring = true))
        val before = clock.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        val addTime =
            clock
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first()
                .action
        compose.runOnIdle { addTime() }
        compose.waitForIdle()
        val adjusted = clock.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertTrue("Clock did not change", before != adjusted)
        compose.activityRule.scenario.recreate()
        waitText("Set the clock to")
        assertEquals(adjusted, clock.fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun unfinishedLessonQuizKeepsChoiceOrderMistakeAndCompletionAcrossLocales() {
        home()
        click("Learn")
        click("Two hands")
        click("Next")
        click("Next")
        click("Try it")
        waitText("Which hand shows the hour?")
        val order = lessonChoiceOrder(AppLanguage.ENGLISH)
        click("The long blue hand")
        waitText("The hour hand is the shorter, pink hand.")
        changeLanguage(AppLanguage.FINNISH)
        waitText("Kumpi viisari näyttää tunnit?")
        assertEquals(order, lessonChoiceOrder(AppLanguage.FINNISH))
        compose.onNode(hasText("Pitkä sininen viisari") and hasClickAction()).assertIsNotEnabled()
        waitText("Tuntiviisari on lyhyempi, pinkki viisari.")
        assertHiddenClock(AppLanguage.FINNISH)
        assertEquals(seed, runBlocking { locale.repository.progress.first() })
        click("Lyhyt pinkki viisari")
        compose.waitUntil(10_000) { "viisarit" in runBlocking { locale.repository.progress.first() }.lessonsDone }
        waitText("Valmis")
        val completed = runBlocking { locale.repository.progress.first() }
        changeLanguage(AppLanguage.SWEDISH)
        waitText("Klar")
        compose.onNode(hasText("Den korta rosa visaren") and hasClickAction()).assertIsNotEnabled()
        assertEquals(completed, runBlocking { locale.repository.progress.first() })
        assertEquals(seed.withLessonDone("viisarit"), completed)
        assertNoIntro(AppLanguage.SWEDISH)
    }

    @Test
    fun activeChallengeEndsAtItsOriginalDeadlineAfterLocaleRecreation() {
        home()
        click("Play")
        waitText("Whole hours")
        click("Minute Challenge")
        waitText("Start!")
        // Drive Compose frames explicitly while the real coroutine timer keeps wall-clock time.
        val started = SystemClock.elapsedRealtime()
        click("Start!")
        compose.mainClock.autoAdvance = false
        try {
            waitText("What time is it?")
            val choices = optionTimes()
            compose.waitUntil(25_000) { SystemClock.elapsedRealtime() - started >= 20_000 }
            changeLanguage(AppLanguage.FINNISH)
            waitText("Mitä kello on?")
            assertEquals(choices, optionTimes())
            assertHiddenClock(AppLanguage.FINNISH)
            // A restarted 60-second run would end around 80 seconds, beyond this bound.
            val remaining = (started + 72_000 - SystemClock.elapsedRealtime()).coerceAtLeast(1)
            compose.waitUntil(remaining) {
                compose.mainClock.advanceTimeByFrame()
                shown("AIKA LOPPUI!")
            }
            val elapsed = SystemClock.elapsedRealtime() - started
            assertTrue("Challenge ended too early: $elapsed ms", elapsed >= 55_000)
            assertTrue("Challenge restarted its minute: $elapsed ms", elapsed < 72_000)
            waitText("Oikeita vastauksia: 0. Paras tuloksesi on 0.")
            assertEquals(seed, runBlocking { locale.repository.progress.first() })
            assertNoIntro(AppLanguage.FINNISH)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    private fun home() {
        compose.waitUntil(15_000) {
            if (shown("Skip intro")) compose.onNodeWithText("Skip intro").performClick()
            shown("Settings")
        }
    }

    private fun finishReadQuestion() {
        val labels =
            compose
                .onAllNodes(hasClickAction())
                .fetchSemanticsNodes()
                .mapNotNull { node ->
                    val texts = node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
                    texts.firstOrNull { DIGITAL.matches(it) }
                }
        for (label in labels) {
            click(label)
            if (!shown("Mitä kello on?") ||
                compose
                    .onAllNodes(hasContentDescription("Kellotaulu:", substring = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            ) {
                break
            }
        }
        if (shown("Seuraava")) click("Seuraava")
    }

    private fun optionTimes(): List<String> =
        compose
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node.config
                    .getOrElse(SemanticsProperties.Text) { emptyList() }
                    .map { it.text }
                    .singleOrNull { DIGITAL.matches(it) }
                    ?.replace(':', '.')
            }

    private fun lessonChoiceOrder(language: AppLanguage): List<Int> {
        val labels =
            listOf(R.string.l_viisarit_c1, R.string.l_viisarit_c2).associateBy {
                locale.resources(language).getString(it)
            }
        return compose
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                node.config
                    .getOrElse(SemanticsProperties.Text) { emptyList() }
                    .firstNotNullOfOrNull { labels[it.text] }
            }.also { assertEquals(2, it.size) }
    }

    private fun assertHiddenClock(language: AppLanguage) {
        val node =
            compose
                .onNodeWithContentDescription(locale.resources(language).getString(R.string.clock_question_description))
                .fetchSemanticsNode()
        assertTrue(node.config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.isEmpty())
        compose.onAllNodesWithText(locale.resources(language).getString(R.string.listen)).assertCountEquals(0)
        assertNoIntro(language)
    }

    private fun assertResultStars(
        language: AppLanguage,
        stars: Int,
    ) {
        val label = locale.resources(language).getQuantityString(R.plurals.result_stars_description, stars, stars)
        compose.onNodeWithContentDescription(label).assertExists()
    }

    private fun assertNoIntro(language: AppLanguage) {
        compose.onAllNodesWithText(locale.resources(language).getString(R.string.intro_skip)).assertCountEquals(0)
    }

    private fun changeLanguage(language: AppLanguage) {
        val previous = compose.activity
        locale.changeLanguage(language, compose.activityRule.scenario)
        compose.waitUntil(15_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            compose.activity !== previous
        }
    }

    private fun click(text: String) {
        waitText(text)
        compose
            .onAllNodes(
                (hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction(),
            )[0]
            .scrollToIfScrollable()
            .performClick()
    }

    private fun shown(text: String) =
        compose
            .onAllNodes(
                hasText(text) or hasContentDescription(text, substring = true),
            ).fetchSemanticsNodes()
            .isNotEmpty()

    private fun waitText(text: String) {
        compose.waitUntil(15_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            shown(text)
        }
    }

    private companion object {
        val DIGITAL = Regex("[0-9]{1,2}[.:][0-9]{2}")
    }
}
