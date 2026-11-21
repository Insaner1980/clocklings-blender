package fi.clocklings

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.itemName
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualFollowUpFixesCaptureTest : ScreenshotReview6Host() {
    @Test
    fun rewards() {
        onlyCells("base", "narrow", "large", "combined")
        launchHost()
        val kind = CompanionKind.HUMAN
        val lang = AppLanguage.ENGLISH
        for (count in 1..3) {
            show(lang, kind) {
                ResultScreen(AccessPolicy(true), Screen.Result(0, count, 0, 0), Outfit.default(kind), {}, {}, {}, {})
            }
            compose.mainClock.advanceTimeBy(2_400)
            if (cell == "narrow" || (cell == "base" && count == 3)) captureScreen("result-$count-stars")
            val names = Wardrobe.newlyUnlocked(kind, 0, count).map { string(lang, itemName(it.id)) }
            val tops =
                names.map {
                    compose
                        .onNodeWithText(it)
                        .fetchSemanticsNode()
                        .positionInRoot.y
                }
            appendMeasure("$cell rewards=$count itemTopPx=$tops")
            if (count > 1) {
                val expectedRows = if (cell in listOf("large", "combined") && count == 3) 2 else 1
                assertEquals(
                    "Reward row positions at $cell with $count rewards: $tops",
                    expectedRows,
                    tops.distinct().size,
                )
            }
            val dress = compose.onNode(hasText(string(lang, R.string.dress_doll)) and hasClickAction())
            scrollToEnd()
            dress.assertIsDisplayed()
            if (cell == "combined") captureScreen("result-$count-stars")
        }
    }

    @Test
    fun lessonHint() {
        launchHost()
        for ((lang, expected) in listOf(
            AppLanguage.ENGLISH to "The label under the clock says evening. Add 12 to the hour: 7 + 12 = 19.",
            AppLanguage.FINNISH to "Kellon alla lukee ilta. Lisää tuntiin 12: 7 + 12 = 19.",
        )) {
            val lesson = LessonCatalog.lesson(Lessons.DAY_LESSON, lang)
            val quiz = lesson.quiz
            val state =
                LessonUiState(
                    lesson = lesson,
                    page = lesson.pages.size,
                    clock = ClockCommand(quiz.time),
                    period = quiz.period,
                    choices = quiz.choices,
                    showHint = true,
                    choiceStates = mapOf(quiz.choices.indexOfFirst { !it.correct } to OptionState.WRONG),
                )
            show(lang, CompanionKind.HUMAN) { LessonScreen(state, false, true, {}, {}, {}, {}, {}, {}) }
            scrollToEnd()
            compose.onNodeWithText(expected).assertIsDisplayed()
            if (cell == "base") captureScreen("lesson-09-vrk-hint-${lang.tag}")
        }
    }

    @Test
    fun britishSpelling() {
        launchHost()
        val lang = AppLanguage.ENGLISH
        val kind = CompanionKind.CAT
        show(lang, kind) { WardrobeScreen(kind, progress.value, CompanionColor.ORIGINAL, {}) }
        clickText(lang, R.string.companion_color)
        compose
            .onNode(
                hasText("Colour") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
                useUnmergedTree = true,
            ).assertIsDisplayed()
        if (cell == "base") captureScreen("wardrobe-colour")
        val custom = Personalization(clock = ClockAppearance(ClockTheme.CUSTOM, CustomClock()))
        show(lang, personalization = custom) { AppearanceScreen(custom, progress.value, {}, {}) }
        clickText(lang, R.string.clock_styles_title)
        val hint = compose.onNodeWithText("Mix your favourite colours and shapes.", substring = true)
        hint.performScrollTo().assertIsDisplayed()
        scrollByPx(scrollNode().fetchSemanticsNode().size.height / 5f)
        if (cell == "base") captureScreen("custom-clock-hint")
        val frame = compose.onNodeWithText("Frame colour")
        frame.performScrollTo()
        scrollByPx(scrollNode().fetchSemanticsNode().size.height / 5f)
        frame.assertIsDisplayed()
        if (cell == "base") captureScreen("custom-clock-frame-colour")
    }

    @Test
    fun starTotals() {
        launchHost()
        val kind = CompanionKind.CAT
        for ((stars, label) in listOf(List(5) { 3 } to "15", listOf(3, 3, 1, 0, 0) to "7")) {
            val fixture = Progress(stars = stars)
            val text = "$label / 15"
            val fill = if (label == "15") SkColors.Gold else SkColors.KouluFill
            show(kind = kind) { HomeScreen(fixture, settings.value, {}) }
            compose.onNodeWithText(text).assertIsDisplayed()
            if (cell in listOf("base", "combined")) captureScreen("home-stars-$label")
            show(kind = kind) { WardrobeScreen(kind, fixture, CompanionColor.ORIGINAL, {}) }
            compose.onNodeWithText(text).assertIsDisplayed()
            val pill = compose.onNodeWithTag("wardrobe_star_balance")
            assertTrue(pill.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].isNotEmpty())
            val pixels = pill.captureToImage().toPixelMap()
            assertEquals("Wardrobe star pill fill at $cell with $label stars", fill, pixels[pixels.width / 2, 2])
            if (cell in listOf("base", "combined")) captureScreen("wardrobe-stars-$label")
        }
    }

    private fun captureScreen(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            save(name, bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}
