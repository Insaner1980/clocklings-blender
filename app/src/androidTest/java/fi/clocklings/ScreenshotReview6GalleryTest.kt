package fi.clocklings

import android.app.KeyguardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.LocaleList
import android.provider.Settings.Global
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Item
import fi.clocklings.domain.Mark
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.appearance.LocalCompanionOutfit
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.applyClocklingsSystemBars
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.challenge.ChallengeViewModel
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.ItemArtWindow
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.companion.grayscale
import fi.clocklings.ui.components.ConfettiHost
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.GameViewModel
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeDestination
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.LessonViewModel
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.random.Random
import fi.clocklings.domain.Text as LessonText

/**
 * Device captures for the screenshot review 6 fixes. The cell (width and font scale) is set outside the test with
 * `wm density` and `font_scale`; its name arrives as the instrumentation argument `cell`, which also names the
 * output folder `files/review6/<cell>`. Measurements go to `measure.txt` in that folder.
 */
class ScreenshotReview6GalleryTest : ScreenshotReview6Host() {
    // ---------------------------------------------------------------- A1, A2

    @Test
    fun a1ClockGrids() {
        onlyCells("base")
        launchHost()
        val times = listOf(0 to "12:00", 90 to "1:30", 285 to "4:45", 315 to "5:15", 650 to "10:50", 715 to "11:55")
        for (hands in HandStyle.entries) {
            for (help in listOf(false, true)) {
                show {
                    ClockGrid(times, hands, help)
                }
                captureTag("grid", "a1-clocks-${hands.name}-help${if (help) 1 else 0}")
            }
        }
        // Close-ups of the worst cases: help on, hand on a numeral that sits on a colour boundary.
        for ((time, label) in listOf(0 to "12:00", 180 to "6:00", 300 to "5:00", 55 to "12:55")) {
            show {
                Box(Modifier.testTag("grid").background(SkColors.Bg), contentAlignment = Alignment.Center) {
                    ClockFace(
                        ClockCommand(time),
                        Modifier.size(340.dp),
                        help = true,
                        appearance = ClockAppearance(ClockTheme.CUSTOM, CustomClock(hands = HandStyle.POINTER)),
                    )
                }
            }
            captureTag("grid", "a1-closeup-pointer-help-${label.replace(":", "")}")
        }
    }

    @Test
    fun a1ExploreWithHelp() {
        launchHost()
        for (lang in AppLanguage.entries) {
            if (lang != AppLanguage.ENGLISH && cell !in listOf("narrow", "large", "both")) continue
            show(lang) { FreeClockScreen(false, true, true, {}) }
            capture("a1-explore-${lang.tag}-12h-0300")
            nudgeUntil(listOf("9:45"), 120)
            capture("a1-explore-${lang.tag}-12h-0945")
        }
        if (cell == "base" || cell == "narrow") {
            show { FreeClockScreen(true, true, true, {}) }
            capture("a1-explore-en-24h-start")
            nudgeUntil(listOf("03:00", "3:00"), 300)
            capture("a1-explore-en-24h-0300")
            nudgeUntil(listOf("09:45", "9:45"), 300)
            capture("a1-explore-en-24h-0945")
            nudgeUntil(listOf("21:45"), 300)
            capture("a1-explore-en-24h-2145")
        }
    }

    @Test
    fun a1a2Lessons() {
        launchHost()
        val languages = if (cell == "base" || cell == "narrow") AppLanguage.entries else listOf(AppLanguage.ENGLISH)
        for (lang in languages) {
            lessonPage(lang, "puoli", 0, "a1-lesson-puoli-p1") { it.caption != null }
            lessonPage(lang, "viisarit", 2, "a1-lesson-kaksi-viisaria-p3") { it.under.size == GLOW_COUNT }
            // Mid-count of "Tarkat minuutit" page 1: static states so the capture is deterministic.
            for (count in listOf(3, 5)) {
                show(lang) {
                    LessonScreen(
                        LessonUiState(
                            LessonCatalog.lesson("minuutit", lang),
                            clock = ClockCommand(180 + count, 0, true),
                            under = (1..count).map { Mark.TickGlow(it) },
                            caption = LessonText.Plural(R.plurals.minutes_count, count),
                        ),
                        false,
                        true,
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                    )
                }
                capture("a2-lesson-minuutit-p1-count$count-${lang.tag}")
            }
        }
    }

    @Test
    fun a1LevelsMiniClock() {
        onlyCells("base")
        launchHost()
        show { LevelsScreen(progress.value, AccessPolicy(fullAccess = true), {}, {}, {}, {}) }
        capture("a1-levels-top")
    }

    // ---------------------------------------------------------------- B1, B2, B3, B4

    @Test
    fun b1b2Game() {
        launchHost()
        val read = Task(180, TaskType.READ)
        val options = listOf(180, 240, 420)

        fun readState(
            feedback: GameFeedback? = null,
            states: Map<Int, OptionState> = emptyMap(),
            mistakes: Int = 0,
        ) = GameUiState(
            level = 0,
            h24 = false,
            taskIndex = 3,
            taskCount = 10,
            task = read,
            options = options,
            optionStates = states,
            clock = ClockCommand(read.time),
            step = 60,
            taskMistakes = mistakes,
            mistakes = mistakes,
            outcomes = listOf(DotOutcome.FIRST_TRY, DotOutcome.WITH_HINT, DotOutcome.SHOWN),
            feedback = feedback,
        )

        for (lang in AppLanguage.entries) {
            if (lang != AppLanguage.ENGLISH && cell != "narrow") continue
            show(lang) { GameScreen(readState(), true, {}, {}, {}, {}, {}, {}) }
            capture("b1b2-game-read-${lang.tag}")
            logFit("b2-read-${lang.tag}")
            val hint = GameFeedback.HintText(Hints.forRead(read.time, 240))
            show(lang) {
                GameScreen(
                    readState(hint, mapOf(240 to OptionState.WRONG), mistakes = 1),
                    true,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
            capture("b1b2-game-read-wrong-${lang.tag}")
            logFit("b2-read-wrong-${lang.tag}")
        }
        // The set task, with the clock share shared with the read task.
        show {
            GameScreen(
                GameUiState(
                    level = 1,
                    h24 = false,
                    taskIndex = 1,
                    taskCount = 10,
                    task = Task(210, TaskType.SET),
                    clock = ClockCommand(0),
                    step = 5,
                    interactive = true,
                    showDone = true,
                    outcomes = listOf(DotOutcome.FIRST_TRY),
                ),
                true,
                {},
                {},
                {},
                {},
                {},
                {},
            )
        }
        capture("b2-game-set-en")
        logFit("b2-set-en")
    }

    @Test
    fun b3Challenge() {
        launchHost()
        val states =
            listOf(
                "start" to ChallengeUiState(best = 9),
                "end-record" to ChallengeUiState(phase = ChallengePhase.END, score = 12, best = 12, newRecord = true),
                "end" to ChallengeUiState(phase = ChallengePhase.END, score = 7, best = 9),
                "end-saving" to
                    ChallengeUiState(phase = ChallengePhase.END, score = 7, best = 9, saveFailed = true),
            )
        for (lang in AppLanguage.entries) {
            for ((name, state) in states) {
                show(lang) { ChallengeScreen(state, false, true, {}, {}, {}) }
                capturePages("b3-challenge-$name-${lang.tag}")
            }
        }
    }

    @Test
    fun b3GameResult() {
        launchHost()
        for (lang in AppLanguage.entries) {
            show(lang) {
                ResultScreen(
                    access = AccessPolicy(fullAccess = true),
                    Screen.Result(0, 3, 0, 0),
                    Outfit.Default,
                    {},
                    {},
                    {},
                    {},
                )
            }
            capture("b3-result-${lang.tag}")
        }
    }

    /**
     * B4, driven by a real [GameViewModel] inside a [ConfettiHost]: a correct answer on task 1, then frames
     * during the success pause and right after the next question appears. Run once with the default animator
     * scale and once with 0 (the shell sets it); the name carries the scale.
     */
    @Test
    fun b4ConfettiDoesNotLinger() {
        onlyCells("base")
        launchHost()
        val scale = Global.getString(targetContext.contentResolver, Global.ANIMATOR_DURATION_SCALE) ?: "unset"
        val tag = "b4-scale-$scale"
        val vm = showGameWithConfetti()
        capture("$tag-0-before")
        val first = vm.state.value.task
        compose.runOnUiThread {
            if (first.type == TaskType.READ) {
                vm.pick(first.time)
            } else {
                vm.onClockChange(first.time)
                vm.done()
            }
        }
        // The test clock only runs frames while the test thread drives it, so step it in real time.
        compose.mainClock.autoAdvance = false
        val shots = listOf(300L, 900L, 1300L, 1700L, 2000L, 2400L)
        var elapsed = 0L
        for (at in shots) {
            while (elapsed < at) {
                Thread.sleep(FRAME_STEP_MS)
                compose.mainClock.advanceTimeBy(FRAME_STEP_MS)
                elapsed += FRAME_STEP_MS
            }
            val index = vm.state.value.taskIndex
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            save("$tag-t${at}ms-task$index", bitmap)
            bitmap.recycle()
        }
        compose.mainClock.autoAdvance = true
        appendMeasure("$tag firstTaskType=${first.type} finalTaskIndex=${vm.state.value.taskIndex}")
    }

    /**
     * M1: the first task is answered wrong once and then right, the second one right at the first try. The dot row
     * is captured during each success pause; the first new dot must pop amber, the second one green.
     */
    @Test
    fun m1DotPopsAfterHint() {
        onlyCells("base")
        launchHost()
        val vm = showGameWithConfetti()
        compose.mainClock.autoAdvance = false
        var elapsed = 0L

        fun step(until: Long) {
            while (elapsed < until) {
                Thread.sleep(FRAME_STEP_MS)
                compose.mainClock.advanceTimeBy(FRAME_STEP_MS)
                elapsed += FRAME_STEP_MS
            }
        }

        fun shoot(name: String) {
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            save(name, bitmap)
            bitmap.recycle()
        }

        fun solve(wrongFirst: Boolean) {
            val task = vm.state.value.task
            compose.runOnUiThread {
                if (task.type == TaskType.READ) {
                    if (wrongFirst) {
                        vm.pick(
                            vm.state.value.options
                                .first { it != task.time },
                        )
                    }
                    vm.pick(task.time)
                } else {
                    if (wrongFirst) {
                        vm.onClockChange((task.time + SET_WRONG_OFFSET) % MINUTES_PER_DAY)
                        vm.done()
                    }
                    vm.onClockChange(task.time)
                    vm.done()
                }
            }
        }
        solve(wrongFirst = true)
        val first = elapsed
        for (at in listOf(100L, 600L, 800L, 1000L, 1300L)) {
            step(first + at)
            shoot("m1-wrong-then-right-t${at}ms")
        }
        step(first + ADVANCE_WAIT_MS)
        solve(wrongFirst = false)
        val second = elapsed
        for (at in listOf(100L, 600L, 800L, 1000L, 1300L)) {
            step(second + at)
            shoot("m1-first-try-t${at}ms")
        }
        compose.mainClock.autoAdvance = true
        appendMeasure("m1 outcomes=${vm.state.value.outcomes} taskIndex=${vm.state.value.taskIndex}")
    }

    // ---------------------------------------------------------------- C1, D1

    @Test
    fun c1LessonList() {
        launchHost()
        for (lang in AppLanguage.entries) {
            show(
                lang,
            ) {
                LessonListScreen(
                    false,
                    progress.value,
                    AccessPolicy(fullAccess = true),
                    {},
                    {},
                    {},
                )
            }
            capture("c1-learn-${lang.tag}-top")
            scrollToEnd()
            capture("c1-learn-${lang.tag}-bottom")
        }
        show {
            LessonListScreen(
                true,
                progress.value,
                AccessPolicy(fullAccess = true),
                {},
                {},
                {},
            )
        }
        scrollToEnd()
        capture("c1-learn-en-24h-bottom")
    }

    @Test
    fun d1Ask() {
        launchHost()
        for (lang in AppLanguage.entries) {
            show(lang) { AskScreen(false, true, true, {}) }
            capture("d1-ask-${lang.tag}-1-set")
            clickText(lang, R.string.ask_go)
            capture("d1-ask-${lang.tag}-2-ask")
            clickText(lang, R.string.ask_reveal)
            Thread.sleep(600)
            capturePages("d1-ask-${lang.tag}-3-revealed")
        }
    }

    // ---------------------------------------------------------------- E1 .. E6

    @Test
    fun e1e3ThumbnailSheets() {
        onlyCells("base")
        launchHost()
        val animals =
            listOf(
                CompanionKind.CAT,
                CompanionKind.DOG,
                CompanionKind.ROBOT,
                CompanionKind.DRAGON,
                CompanionKind.DINOSAUR,
                CompanionKind.BUNNY,
                CompanionKind.UNICORN,
            )
        val slots = listOf(Slot.HEAD, Slot.NECK, Slot.EXTRA)
        for (kind in animals + CompanionKind.HUMAN) {
            val items =
                Wardrobe.itemsFor(kind).filter {
                    if (kind == CompanionKind.HUMAN) it.slot == Slot.EXTRA else it.slot in slots
                }
            for ((page, chunk) in items.chunked(SHEET_PAGE).withIndex()) {
                show(kind = kind) { ThumbnailSheet(kind, chunk) }
                captureTag("sheet", "e-sheet-${kind.name}-p$page")
            }
        }
    }

    @Test
    fun e4e5Wardrobes() {
        launchHost()
        val kinds = listOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.DINOSAUR)
        for (kind in kinds) {
            for (lang in AppLanguage.entries) {
                show(lang, kind) { WardrobeScreen(kind, progress.value, CompanionColor.ORIGINAL, {}) }
                capturePages("e4-wardrobe-${kind.name}-${lang.tag}")
            }
        }
        // E2 (bunny, dragon, cat neck thumbnails) and E6 (swatch rows) in English.
        for (kind in listOf(CompanionKind.BUNNY, CompanionKind.DRAGON, CompanionKind.UNICORN, CompanionKind.DINOSAUR)) {
            show(AppLanguage.ENGLISH, kind) { WardrobeScreen(kind, progress.value, CompanionColor.ORIGINAL, {}) }
            capturePages("e2-wardrobe-${kind.name}-en")
        }
    }

    /** M3: the car wardrobe with unlocked and locked thumbnails (WHEELS and FRONT among them). */
    @Test
    fun m3CarWardrobe() {
        onlyCells("base")
        launchHost()
        show(AppLanguage.ENGLISH, CompanionKind.CAR) {
            WardrobeScreen(CompanionKind.CAR, progress.value, CompanionColor.ORIGINAL, {})
        }
        capturePages("m3-wardrobe-CAR-en")
    }

    @Test
    fun e6Colors() {
        onlyCells("base")
        launchHost()
        for (kind in listOf(CompanionKind.UNICORN, CompanionKind.DRAGON, CompanionKind.DINOSAUR)) {
            show(kind = kind) {
                Row(
                    Modifier.testTag("colors").background(SkColors.Bg).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (color in kind.colorOptions()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompanionView(
                                kind,
                                Outfit.default(kind),
                                Modifier.size(90.dp, 150.dp),
                                color = color,
                                animated = false,
                            )
                            Text(color.name, fontSize = 11.sp)
                        }
                    }
                }
            }
            captureTag("colors", "e6-colors-${kind.name}")
        }
        for ((kind, color) in listOf(
            CompanionKind.UNICORN to CompanionColor.ORIGINAL,
            CompanionKind.UNICORN to CompanionColor.BLUE,
            CompanionKind.DRAGON to CompanionColor.ORANGE,
            CompanionKind.DINOSAUR to CompanionColor.ORANGE,
        )) {
            show(kind = kind) { WardrobeScreen(kind, progress.value, color, {}) }
            capture("e6-swatch-wardrobe-${kind.name}-${color.name}")
        }
    }

    // ---------------------------------------------------------------- F1, H1

    @Test
    fun f1Appearance() {
        launchHost()
        val custom = Personalization(clock = ClockAppearance(ClockTheme.CUSTOM, CustomClock()))
        for (lang in AppLanguage.entries) {
            if (lang != AppLanguage.ENGLISH && cell != "base" && cell != "both") continue
            show(lang, personalization = custom) {
                AppearanceScreen(custom, progress.value, {}, {})
            }
            clickText(lang, R.string.clock_styles_title)
            capturePages("f1-appearance-clocks-${lang.tag}")
        }
    }

    @Test
    fun h1ScrolledUnderTopBar() {
        onlyCells("narrow", "large", "both")
        launchHost()
        val custom = Personalization(clock = ClockAppearance(ClockTheme.CUSTOM, CustomClock()))
        show(personalization = custom) { AppearanceScreen(custom, progress.value, {}, {}) }
        clickText(AppLanguage.ENGLISH, R.string.clock_styles_title)
        titlesUnderTopBar(
            "h1-appearance-clocks",
            listOf(R.string.clock_color, R.string.clock_frame, R.string.clock_numerals, R.string.clock_hands)
                .map { string(AppLanguage.ENGLISH, it) },
        )
        show(kind = CompanionKind.CAT) {
            WardrobeScreen(CompanionKind.CAT, progress.value, CompanionColor.ORIGINAL, {})
        }
        titlesUnderTopBar(
            "h1-wardrobe-cat",
            listOf(R.string.companion_color, R.string.slot_head, R.string.slot_neck, R.string.slot_extra)
                .map { string(AppLanguage.ENGLISH, it) },
        )
    }

    // ---------------------------------------------------------------- G1

    @Test
    fun g1Home() {
        launchHost()
        val titles = homeTitles()
        for (kind in CompanionKind.entries) {
            val personalization = Personalization(companion = kind)
            show(AppLanguage.ENGLISH, kind) { HomeScreen(progress.value, settings.value, {}) }
            capture("g1-home-${kind.name}")
            measureHome(kind.name, titles)
        }
        if (cell == "narrow" || cell == "both") {
            for (lang in listOf(AppLanguage.FINNISH, AppLanguage.SWEDISH)) {
                show(lang, CompanionKind.CAT) { HomeScreen(progress.value, settings.value, {}) }
                capture("g1-home-CAT-${lang.tag}")
            }
        }
    }

    // ---------------------------------------------------------------- Round 2 device validation

    private fun showGameWithConfetti(): GameViewModel {
        lateinit var vm: GameViewModel
        compose.runOnUiThread { vm = GameViewModel(0, false, services, Random(7)) }
        show {
            ConfettiHost {
                val state by vm.state.collectAsStateWithLifecycle()
                GameScreen(state, true, {}, {}, vm::pick, vm::onClockChange, vm::done, vm::next)
            }
        }
        return vm
    }

    @androidx.compose.runtime.Composable
    private fun ClockGrid(
        times: List<Pair<Int, String>>,
        hands: HandStyle,
        help: Boolean,
    ) {
        Column(Modifier.testTag("grid").background(SkColors.Bg).padding(4.dp)) {
            for (row in times.chunked(2)) {
                Row {
                    for ((time, label) in row) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ClockFace(
                                ClockCommand(time),
                                Modifier.size(190.dp),
                                help = help,
                                appearance = ClockAppearance(ClockTheme.CUSTOM, CustomClock(hands = hands)),
                            )
                            Text("$label ${hands.name} help=$help", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ThumbnailSheet(
        kind: CompanionKind,
        items: List<Item>,
    ) {
        val outfit = Outfit.default(kind)
        Column(
            Modifier.testTag("sheet").background(SkColors.Bg).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (row in items.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (item in row) {
                        Column(Modifier.size(width = 168.dp, height = 100.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ItemArtWindow(item, outfit, CompanionColor.ORIGINAL)
                                ItemArtWindow(
                                    item,
                                    outfit,
                                    CompanionColor.ORIGINAL,
                                    Modifier.alpha(LOCKED_ALPHA).grayscale(),
                                )
                            }
                            Text(
                                item.id.removePrefix("${kind.name.lowercase()}-"),
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

class ScreenshotReview6Round2Test : ScreenshotReview6Host() {
    /** R2-1: the wardrobe colour swatch row of the dragon and the dinosaur in every language. */
    @Test
    fun r2Swatches() {
        onlyCells("base")
        launchHost()
        for (kind in listOf(CompanionKind.DRAGON, CompanionKind.DINOSAUR)) {
            for (lang in AppLanguage.entries) {
                show(lang, kind) { WardrobeScreen(kind, progress.value, CompanionColor.ORANGE, {}) }
                capture("r2-swatch-${kind.name}-${lang.tag}")
                dumpTree("r2-swatch-${kind.name}-${lang.tag}")
            }
        }
    }

    /**
     * R2-2: Minuuttihaaste START, then the real Start tap, RUN while the clock grows and once settled, and the
     * accessibility dump of the top bar. The test clock is stepped by hand because the RUN timer never idles.
     */
    @Test
    fun r2ChallengeRun() = challengeRun(scrolled = false)

    /** R2-2: as [r2ChallengeRun], but the START page is scrolled to its end before Start is tapped. */
    @Test
    fun r2ChallengeRunScrolled() = challengeRun(scrolled = true)

    private fun challengeRun(scrolled: Boolean) {
        val prefix = if (scrolled) "r2-challenge-scrolled" else "r2-challenge"
        launchHost()
        for (lang in AppLanguage.entries) {
            lateinit var vm: ChallengeViewModel
            compose.runOnUiThread { vm = ChallengeViewModel(false, services) }
            show(lang) {
                val state by vm.state.collectAsStateWithLifecycle()
                ChallengeScreen(state, false, true, {}, vm::start, vm::pick)
            }
            if (scrolled) capturePages("$prefix-start-${lang.tag}") else capture("$prefix-start-${lang.tag}-p0")
            if (!scrolled) dumpTree("$prefix-start-${lang.tag}")
            appendMeasure("$prefix start ${lang.tag} scroll=${scrollRange()}")
            compose.mainClock.autoAdvance = false
            compose.onNode(hasText(string(lang, R.string.challenge_go)) and hasClickAction()).performClick()
            var elapsed = 0L
            for (at in listOf(RESIZE_MID_MS, RESIZE_SETTLED_MS, RESIZE_LATE_MS)) {
                while (elapsed < at) {
                    Thread.sleep(FRAME_STEP_MS)
                    compose.mainClock.advanceTimeBy(FRAME_STEP_MS)
                    elapsed += FRAME_STEP_MS
                }
                val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                save("$prefix-run-${lang.tag}-t${at}ms", bitmap)
                bitmap.recycle()
            }
            if (!scrolled) dumpTree("$prefix-run-${lang.tag}")
            appendMeasure("$prefix run ${lang.tag} phase=${vm.state.value.phase} scroll=${scrollRange()}")
            compose.runOnUiThread { screen.value = {} }
            compose.mainClock.advanceTimeBy(RESIZE_MID_MS)
            compose.mainClock.autoAdvance = true
        }
    }

    /** R2-2: the END phase of Minuuttihaaste with its buttons, scrolled in pages, and the accessibility dump. */
    @Test
    fun r2ChallengeEnd() {
        launchHost()
        for (lang in AppLanguage.entries) {
            val end = ChallengeUiState(phase = ChallengePhase.END, score = 7, best = 9)
            show(lang) { ChallengeScreen(end, false, true, {}, {}, {}) }
            appendMeasure("r2 challenge-end ${lang.tag} scroll=${scrollRange()}")
            capturePages("r2-challenge-end-${lang.tag}")
            dumpTree("r2-challenge-end-${lang.tag}")
        }
    }

    /** R2-2: the Game screen top bar back button in every language. */
    @Test
    fun r2GameBack() {
        onlyCells("base")
        launchHost()
        val read = Task(180, TaskType.READ)
        val state =
            GameUiState(
                level = 0,
                h24 = false,
                taskIndex = 3,
                taskCount = 10,
                task = read,
                options = listOf(180, 240, 420),
                clock = ClockCommand(read.time),
                step = 60,
            )
        for (lang in AppLanguage.entries) {
            show(lang) { GameScreen(state, true, {}, {}, {}, {}, {}, {}) }
            capture("r2-game-${lang.tag}")
            dumpTree("r2-game-${lang.tag}")
        }
    }

    /** R2-2: heading nodes of the Opi lesson path, from the compose tree and from the accessibility tree. */
    @Test
    fun r2LessonHeadings() {
        onlyCells("base")
        launchHost()
        for (lang in AppLanguage.entries) {
            for (h24 in listOf(false, true)) {
                if (h24 && lang != AppLanguage.ENGLISH) continue
                show(
                    lang,
                ) {
                    LessonListScreen(
                        h24,
                        progress.value,
                        AccessPolicy(fullAccess = true),
                        {},
                        {},
                        {},
                    )
                }
                measureLessonHeadings(lang, h24)
                dumpPages("r2-lessons-${lang.tag}-h24-$h24")
            }
        }
    }

    /** R2-3: 24 h Tutki with help on at 03:00, 09:45 and 21:45 in every language. */
    @Test
    fun r2Explore24() {
        onlyCells("base", "narrow")
        launchHost()
        for (lang in AppLanguage.entries) {
            show(lang) { FreeClockScreen(true, true, true, {}) }
            for (time in listOf("03:00", "09:45", "21:45")) {
                nudgeUntil(timeSpellings(time), EXPLORE_STEPS)
                capture("r2-explore24-${lang.tag}-${time.replace(":", "")}")
            }
        }
    }

    /** R2-4: taps on empty sky, on bare ground and on the content group of the Home stage. */
    @Test
    fun r2HomeTaps() {
        onlyCells("base")
        launchHost()
        val opened = mutableListOf<HomeDestination>()
        for (kind in CompanionKind.entries) {
            show(AppLanguage.ENGLISH, kind) { HomeScreen(progress.value, settings.value, { opened += it }) }
            val stage = compose.onNodeWithTag("home_stage")
            val content = compose.onNodeWithTag("companion_stage")
            val stageBounds = stage.fetchSemanticsNode().boundsInRoot
            val contentBounds = content.fetchSemanticsNode().boundsInRoot
            val sky = (contentBounds.top - stageBounds.top) / density
            val ground = (stageBounds.bottom - contentBounds.bottom) / density
            appendMeasure("r2 home ${kind.name} skyAboveContentDp=${sky.fmt()} groundBelowContentDp=${ground.fmt()}")
            val inset = TAP_INSET_DP * density
            if (sky > TAP_FREE_MIN_DP) tap(kind, "sky", stage, Offset(stageBounds.width * TAP_X, inset), opened)
            if (ground > TAP_FREE_MIN_DP) {
                tap(kind, "ground", stage, Offset(stageBounds.width * TAP_X, stageBounds.height - inset), opened)
            }
            tap(kind, "content", content, Offset(contentBounds.width / 2, contentBounds.height / 2), opened)
        }
    }

    // ---------------------------------------------------------------- R2 device-fix wave

    /** R2 dev: Minuuttihaaste START, RUN once settled, END and Play again in the cell's languages. */
    @Test
    fun r2devChallenge() = challengeFlow(scrolledStart = false)

    /** R2 dev: as [r2devChallenge], but START is scrolled to its end before Start is tapped. */
    @Test
    fun r2devChallengeScrolled() {
        onlyCells("both", "s360")
        challengeFlow(scrolledStart = true)
    }

    private fun challengeFlow(scrolledStart: Boolean) {
        val tag = if (scrolledStart) "r2dev-scrolled" else "r2dev"
        launchHost()
        for ((lang, h24) in challengeCombos()) {
            val name = "$tag-${lang.tag}-h24-$h24"
            challengeOverride.value = null
            lateinit var vm: ChallengeViewModel
            compose.runOnUiThread { vm = ChallengeViewModel(h24, services) }
            show(lang) {
                val state by vm.state.collectAsStateWithLifecycle()
                val again = {
                    challengeOverride.value = null
                    vm.start()
                }
                ChallengeScreen(challengeOverride.value ?: state, h24, true, {}, again, vm::pick)
            }
            if (scrolledStart) scrollToEnd()
            capture("$name-start")
            measureChallenge("$name START")
            compose.mainClock.autoAdvance = false
            val tapped = System.nanoTime()
            compose.onNode(hasText(string(lang, R.string.challenge_go)) and hasClickAction()).performClick()
            advanceFrames(RESIZE_SETTLED_MS) { elapsed, wall ->
                if (elapsed <= COLD_FRAMES_MS) appendMeasure("$name frame t=${elapsed}ms wallMs=$wall")
            }
            appendMeasure("$name tapToSettledWallMs=${(System.nanoTime() - tapped) / NANOS_PER_MS}")
            measureChallenge("$name RUN")
            shot("$name-run")
            compose.runOnUiThread {
                val end = vm.state.value.copy(phase = ChallengePhase.END, score = 7, best = 9, busy = true)
                challengeOverride.value = end
            }
            advanceFrames(RESIZE_SETTLED_MS)
            measureChallenge("$name END")
            shot("$name-end-p0")
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            scrollToEnd()
            measureChallenge("$name END-scrolled")
            capture("$name-end-p1")
            compose.mainClock.autoAdvance = false
            compose.onNode(hasText(string(lang, R.string.play_again)) and hasClickAction()).performClick()
            advanceFrames(RESIZE_SETTLED_MS)
            measureChallenge("$name AGAIN")
            shot("$name-again")
            compose.runOnUiThread { screen.value = {} }
            advanceFrames(RESIZE_MID_MS)
            compose.mainClock.autoAdvance = true
        }
    }

    /** R2 dev: with animator scale 0 (set outside), START to RUN frame by frame at 360 x 640 dp in Finnish. */
    @Test
    fun r2devReducedMotion() {
        onlyCells("s360")
        launchHost()
        val scale = Global.getFloat(targetContext.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f)
        appendMeasure("r2dev animator_duration_scale=$scale")
        challengeOverride.value = null
        lateinit var vm: ChallengeViewModel
        compose.runOnUiThread { vm = ChallengeViewModel(false, services) }
        show(AppLanguage.FINNISH) {
            val state by vm.state.collectAsStateWithLifecycle()
            ChallengeScreen(state, false, true, {}, vm::start, vm::pick)
        }
        measureChallenge("r2dev-rm START")
        compose.mainClock.autoAdvance = false
        compose.onNode(hasText(string(AppLanguage.FINNISH, R.string.challenge_go)) and hasClickAction()).performClick()
        shot("r2dev-rm-t0")
        measureChallenge("r2dev-rm t=0ms")
        val saveAt = listOf(FRAME_MS, 3 * FRAME_MS, 6 * FRAME_MS)
        advanceFrames(REDUCED_MOTION_MS) { elapsed, wall ->
            appendMeasure("r2dev-rm t=${elapsed}ms phase=${vm.state.value.phase} clockDp=${clockDp()} wallMs=$wall")
            if (elapsed in saveAt) shot("r2dev-rm-t${elapsed}ms")
        }
        measureChallenge("r2dev-rm settled")
        compose.runOnUiThread { screen.value = {} }
        advanceFrames(RESIZE_MID_MS)
        compose.mainClock.autoAdvance = true
    }

    /** R2 dev: a pressed companion on Home keeps its feet on the ground; NONE shows no cloud by the bubble. */
    @Test
    fun r2devHome() {
        launchHost()
        if (cell == "base") {
            show(AppLanguage.ENGLISH, CompanionKind.CAT) { HomeScreen(progress.value, settings.value, {}) }
            compose.waitForIdle()
            Thread.sleep(SETTLE_MS)
            shot("r2dev-home-CAT-rest")
            val stage = compose.onNodeWithTag("companion_stage")
            appendMeasure("r2dev CAT rest bounds=${stage.fetchSemanticsNode().boundsInRoot}")
            stage.performTouchInput { down(center) }
            Thread.sleep(HOLD_MS)
            shot("r2dev-home-CAT-pressed")
            appendMeasure("r2dev CAT pressed bounds=${stage.fetchSemanticsNode().boundsInRoot}")
            stage.performTouchInput { cancel() }
            compose.waitForIdle()
            Thread.sleep(SETTLE_MS)
            shot("r2dev-home-CAT-released")
        }
        show(AppLanguage.ENGLISH, CompanionKind.NONE) { HomeScreen(progress.value, settings.value, {}) }
        capture("r2dev-home-NONE")
    }

    /** R2 dev: accessibility dump of the Levels (Play) stops. */
    @Test
    fun r2devLevelsDump() {
        onlyCells("base")
        launchHost()
        for (lang in AppLanguage.entries) {
            show(
                lang,
            ) { LevelsScreen(progress.value, AccessPolicy(fullAccess = true), {}, {}, {}, {}) }
            dumpPages("r2dev-levels-${lang.tag}")
        }
    }

    private fun measureLessonHeadings(
        lang: AppLanguage,
        h24: Boolean,
    ) {
        val headings =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                .fetchSemanticsNodes()
        for (node in headings) {
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
            val text = node.config.getOrNull(SemanticsProperties.Text)
            appendMeasure("r2 headings ${lang.tag} h24=$h24 desc=$description text=$text")
        }
        appendMeasure("r2 headings ${lang.tag} h24=$h24 count=${headings.size}")
    }
}

class ScreenshotReview6Round3Test : ScreenshotReview6Host() {
    // ---------------------------------------------------------------- Round 3 device validation

    /** Top of the system navigation bar in dp: the lowest line a bottom bar button can use. */
    private fun navTopDp(): Float {
        var top = 0
        scenario!!.onActivity { activity ->
            val decor = activity.window.decorView
            val inset = decor.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
            top = decor.height - inset
        }
        return top / density
    }

    private fun SemanticsNode.span() =
        "top=${(boundsInRoot.top / density).fmt()} bottom=${(boundsInRoot.bottom / density).fmt()}"

    /** Clock, score, record text and bar buttons of the Minuuttihaaste END (or RUN) against the viewport and bar. */
    private fun r3Measure(
        label: String,
        lang: AppLanguage,
        score: Int,
        record: Boolean,
    ) {
        val nav = navTopDp()
        val viewport = compose.onAllNodes(verticalScrollMatcher).fetchSemanticsNodes().firstOrNull()
        appendMeasure(
            "r3 $label clockDp=${clockDp()} navTopDp=${nav.fmt()} " +
                "viewport=${viewport?.span()} scroll=${scrollRange()}",
        )
        for (node in compose.onAllNodes(hasText(score.toString()), useUnmergedTree = true).fetchSemanticsNodes()) {
            appendMeasure("r3 $label score ${node.span()} centerPx=${node.boundsInRoot.center}")
        }
        if (record) {
            val headline = string(lang, R.string.challenge_new_best)
            for (node in compose
                .onAllNodes(
                    hasText(headline, ignoreCase = true),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()) {
                appendMeasure("r3 $label recordHeadline ${node.span()}")
            }
            val text = localized.getValue(lang).getString(R.string.challenge_new_record, score)
            for (node in compose
                .onAllNodes(
                    SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text)),
                ).fetchSemanticsNodes()) {
                appendMeasure("r3 $label recordText ${node.span()}")
            }
        }
        for (id in listOf(R.string.back_description, R.string.play_again)) {
            for (node in compose.onAllNodes(hasText(string(lang, id)) and hasClickAction()).fetchSemanticsNodes()) {
                val inside = node.boundsInRoot.bottom / density <= nav + 0.5f
                appendMeasure("r3 $label button=\"${string(lang, id)}\" ${node.span()} aboveNavBar=$inside")
            }
        }
    }

    /** R3-2: Minuuttihaaste START, RUN and END (normal and new record) with confetti in a real run. */
    @Test
    fun r3Challenge() {
        launchHost()
        val languages = if (cell == "base") listOf(AppLanguage.FINNISH) else AppLanguage.entries
        for (lang in languages) {
            for (record in listOf(false, true)) {
                val name = "r3-${lang.tag}-${if (record) "record" else "normal"}"
                val score = if (record) R3_RECORD_SCORE else R3_SCORE
                challengeOverride.value = null
                lateinit var vm: ChallengeViewModel
                compose.runOnUiThread { vm = ChallengeViewModel(false, services) }
                show(lang) {
                    ConfettiHost {
                        val state by vm.state.collectAsStateWithLifecycle()
                        val again = {
                            challengeOverride.value = null
                            vm.start()
                        }
                        ChallengeScreen(challengeOverride.value ?: state, false, true, {}, again, vm::pick)
                    }
                }
                capture("$name-start")
                compose.mainClock.autoAdvance = false
                compose.onNode(hasText(string(lang, R.string.challenge_go)) and hasClickAction()).performClick()
                advanceFrames(RESIZE_SETTLED_MS)
                r3Measure("$name RUN", lang, score, record)
                shot("$name-run")
                compose.runOnUiThread {
                    val end =
                        vm.state.value.copy(
                            phase = ChallengePhase.END,
                            score = score,
                            best = if (record) score else R3_BEST,
                            newRecord = record,
                        )
                    challengeOverride.value = end
                }
                advanceFrames(R3_CONFETTI_MS)
                shot("$name-end-t${R3_CONFETTI_MS}ms")
                advanceFrames(RESIZE_SETTLED_MS)
                r3Measure("$name END", lang, score, record)
                shot("$name-end")
                compose.mainClock.autoAdvance = true
                compose.waitForIdle()
                scrollToEnd()
                r3Measure("$name END-scrolled", lang, score, record)
                capture("$name-end-scrolled")
                compose.mainClock.autoAdvance = false
                compose.runOnUiThread { screen.value = {} }
                advanceFrames(RESIZE_MID_MS)
                compose.mainClock.autoAdvance = true
            }
        }
    }

    /** R3-5: with bold text (set outside) questions 1 to 3 of a 24 h run in Finnish: fit and a steady clock. */
    @Test
    fun r3Bold() {
        onlyCells("s360")
        launchHost()
        appendMeasure("r3 bold fontWeightAdjustment=${targetContext.resources.configuration.fontWeightAdjustment}")
        challengeOverride.value = null
        lateinit var vm: ChallengeViewModel
        compose.runOnUiThread { vm = ChallengeViewModel(true, services) }
        show(AppLanguage.FINNISH) {
            val state by vm.state.collectAsStateWithLifecycle()
            ChallengeScreen(state, true, true, {}, vm::start, vm::pick)
        }
        compose.mainClock.autoAdvance = false
        compose.onNode(hasText(string(AppLanguage.FINNISH, R.string.challenge_go)) and hasClickAction()).performClick()
        advanceFrames(RESIZE_SETTLED_MS)
        for (question in 1..R3_QUESTIONS) {
            measureChallenge("r3-bold q$question")
            shot("r3-bold-q$question")
            val shown = vm.state.value.question
            compose.runOnUiThread { vm.pick(shown) }
            var waited = 0L
            while (waited < ADVANCE_WAIT_MS && (vm.state.value.question == shown || vm.state.value.busy)) {
                advanceFrames(FRAME_MS)
                waited += FRAME_MS
            }
            advanceFrames(RESIZE_SETTLED_MS)
        }
        compose.runOnUiThread { screen.value = {} }
        advanceFrames(RESIZE_MID_MS)
        compose.mainClock.autoAdvance = true
    }

    /** R3-3: Home for the kinds the cell calls for, with the tiles and the navigation bar line. */
    @Test
    fun r3Home() {
        launchHost()
        val titles = homeTitles()
        val kinds =
            when (cell) {
                "narrow", "both", "s360" -> CompanionKind.entries
                "large" -> listOf(CompanionKind.NONE, CompanionKind.CAT)
                else -> listOf(CompanionKind.NONE)
            }
        for (kind in kinds) {
            show(AppLanguage.ENGLISH, kind) { HomeScreen(progress.value, settings.value, {}) }
            capture("r3-home-${kind.name}")
            measureHome("r3 ${kind.name}", titles)
            appendMeasure("r3 home ${kind.name} navTopDp=${navTopDp().fmt()}")
        }
    }

    /** R3-4: the lesson screen navigation buttons (shared ActionPair) on the first two pages of "puoli". */
    @Test
    fun r3Lessons() {
        launchHost()
        for (lang in listOf(AppLanguage.FINNISH, AppLanguage.ENGLISH)) {
            for (page in 0..1) {
                lessonPage(lang, "puoli", page, "r3-lesson-puoli-p${page + 1}") { page > 0 || it.caption != null }
                for (node in compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()) {
                    val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                    appendMeasure("r3 lesson ${lang.tag} p${page + 1} \"$text\" ${node.span()} nav=${navTopDp().fmt()}")
                }
            }
        }
    }

    /** R3-6 and R3-7: Opi and Play paths, captured and dumped to find the current stop state description. */
    @Test
    fun r3Paths() {
        launchHost()
        dumpTree("r3-warmup")
        for (lang in listOf(AppLanguage.FINNISH, AppLanguage.ENGLISH)) {
            show(
                lang,
            ) {
                LessonListScreen(
                    false,
                    progress.value,
                    AccessPolicy(fullAccess = true),
                    {},
                    {},
                    {},
                )
            }
            capture("r3-opi-${lang.tag}-p0")
            dumpTree("r3-opi-${lang.tag}")
            show(
                lang,
            ) { LevelsScreen(progress.value, AccessPolicy(fullAccess = true), {}, {}, {}, {}) }
            capture("r3-play-${lang.tag}-p0")
            dumpTree("r3-play-${lang.tag}")
        }
    }

    // ---------------------------------------------------------------- hosting and helpers
}

abstract class ScreenshotReview6Host {
    @get:Rule val compose = createEmptyComposeRule()

    protected val instrumentation = InstrumentationRegistry.getInstrumentation()
    protected val targetContext = instrumentation.targetContext
    protected val services = (targetContext.applicationContext as ClocklingsApplication).container
    protected val cell = InstrumentationRegistry.getArguments().getString("cell") ?: "base"
    protected val density = targetContext.resources.displayMetrics.density

    protected val language = mutableStateOf(AppLanguage.ENGLISH)
    protected val settings = mutableStateOf(Settings())
    protected val progress = mutableStateOf(Progress(stars = listOf(3, 3, 1, 0, 0)))
    protected val screen = mutableStateOf<@androidx.compose.runtime.Composable () -> Unit>({})
    protected var scenario: ActivityScenario<ComponentActivity>? = null

    protected val localized =
        AppLanguage.entries.associateWith {
            targetContext.createConfigurationContext(
                Configuration(targetContext.resources.configuration).apply {
                    setLocales(LocaleList.forLanguageTags(it.tag))
                },
            )
        }

    /** The capture harness runs only when asked for with `-e cell <name>`. */
    @Before
    fun onlyOnRequest() = assumeTrue(InstrumentationRegistry.getArguments().containsKey("cell"))

    protected fun homeTitles(): List<String> =
        listOf(R.string.menu_koulu, R.string.menu_vapaa, R.string.menu_pelaa, R.string.menu_kysy)
            .map { localized.getValue(AppLanguage.ENGLISH).getString(it) }

    @After
    fun closeHost() {
        scenario?.close()
    }

    protected val challengeOverride = mutableStateOf<ChallengeUiState?>(null)

    /** The language and clock mode pairs checked in each cell of the R2 device-fix wave. */
    protected fun challengeCombos(): List<Pair<AppLanguage, Boolean>> =
        when (cell) {
            "s360" -> listOf(FI_12, EN_12, SV_12, FI_24)
            "both" -> listOf(FI_12, EN_12, SV_12)
            "base" -> listOf(FI_12, FI_24)
            "narrow" -> listOf(FI_12)
            else -> emptyList()
        }

    /** Steps the paused test clock frame by frame; [onFrame] gets the elapsed time and the wall time of the frame. */
    protected fun advanceFrames(
        totalMs: Long,
        onFrame: (Long, Long) -> Unit = { _, _ -> },
    ) {
        var elapsed = 0L
        while (elapsed < totalMs) {
            val began = System.nanoTime()
            compose.mainClock.advanceTimeBy(FRAME_MS)
            val wall = (System.nanoTime() - began) / NANOS_PER_MS
            Thread.sleep(FRAME_MS)
            elapsed += FRAME_MS
            onFrame(elapsed, wall)
        }
    }

    protected fun shot(name: String) {
        val bitmap = screenshot()
        try {
            save(name, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun challengeClockNode() =
        compose
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "challenge_clock"))
            .fetchSemanticsNodes()
            .singleOrNull()

    protected fun clockDp(): String = challengeClockNode()?.let { (it.size.width / density).fmt() } ?: "none"

    /** The clock width and the unclipped geometry of its controls against the viewport. */
    protected fun measureChallenge(label: String) {
        val clock = challengeClockNode()
        val clockBottom = clock?.let { it.positionInRoot.y + it.size.height } ?: 0f
        val clockTop = clock?.let { (it.positionInRoot.y / density).fmt() } ?: "none"
        val rootHeight =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .size.height
        val viewport =
            compose
                .onAllNodes(verticalScrollMatcher)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.boundsInRoot
        appendMeasure(
            "$label clockDp=${clockDp()} clockTopDp=$clockTop" +
                " viewportBottomDp=${((viewport?.bottom ?: rootHeight.toFloat()) / density).fmt()}" +
                " rootHeightDp=${(rootHeight / density).fmt()} scroll=${scrollRange()}",
        )
        val below =
            compose
                .onAllNodes(hasClickAction())
                .fetchSemanticsNodes()
                .filter { it.positionInRoot.y >= clockBottom - 1f }
                .sortedBy { it.positionInRoot.y }
        for (node in below) {
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
            val top = node.positionInRoot.y
            val bottom = top + node.size.height
            val inside = viewport != null && top >= viewport.top && bottom <= viewport.bottom
            appendMeasure(
                "$label   node=\"$text\" topDp=${(top / density).fmt()} bottomDp=${(bottom / density).fmt()}" +
                    " fullyInViewport=$inside",
            )
        }
    }

    protected fun tap(
        kind: CompanionKind,
        label: String,
        node: SemanticsNodeInteraction,
        at: Offset,
        opened: List<HomeDestination>,
    ) {
        val before = opened.size
        node.performTouchInput { click(at) }
        compose.waitForIdle()
        appendMeasure("r2 tap ${kind.name} $label at=$at opened=${opened.drop(before)}")
    }

    /** The digital spellings of a 24 h time in the three languages: 03:00, 3:00, 03.00 and 3.00. */
    protected fun timeSpellings(time: String): List<String> =
        listOf(time, time.trimStart('0'), time.replace(':', '.'), time.replace(':', '.').trimStart('0'))

    /** Walks the accessibility tree that uiautomator dumps and writes one line per node. */
    protected fun dumpTree(name: String) {
        compose.waitForIdle()
        Thread.sleep(DUMP_SETTLE_MS)
        val out = StringBuilder()
        val root = instrumentation.uiAutomation.rootInActiveWindow
        if (root == null) {
            out.append("no root window\n")
        } else {
            fun walk(
                node: AccessibilityNodeInfo,
                depth: Int,
            ) {
                node.refresh()
                val bounds = Rect().also { node.getBoundsInScreen(it) }
                out
                    .append(" ".repeat(depth))
                    .append(node.className)
                    .append(" text=")
                    .append(node.text)
                    .append(" desc=")
                    .append(node.contentDescription)
                    .append(" state=")
                    .append(node.stateDescription)
                    .append(" heading=")
                    .append(node.isHeading)
                    .append(" clickable=")
                    .append(node.isClickable)
                    .append(" visible=")
                    .append(node.isVisibleToUser)
                    .append(" bounds=")
                    .append(bounds.toShortString())
                    .append('\n')
                for (index in 0 until node.childCount) node.getChild(index)?.let { walk(it, depth + 1) }
            }
            walk(root, 0)
        }
        val safe = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        File(outDir(), "dump-$safe.txt").writeText(out.toString())
        File(outDir(), "semantics-$safe.txt").writeText(compose.onRoot().printToString(Int.MAX_VALUE))
    }

    protected fun onlyCells(vararg cells: String) = assumeTrue("Not for cell $cell", cell in cells)

    protected fun launchHost(hostServices: AppServices = services) {
        val factory = viewModelFactory { initializer { AppViewModel(hostServices) } }
        scenario =
            if (cell.startsWith("gallery")) {
                ActivityScenario.launch(Intent(targetContext, GalleryHostActivity::class.java))
            } else {
                ActivityScenario.launch(ComponentActivity::class.java)
            }
        scenario!!.onActivity { activity ->
            activity.applyClocklingsSystemBars()
            activity.setContent {
                val app = viewModel<AppViewModel>(factory = factory)
                AppLocaleProvider(hostServices.repository) {
                    val context = localized.getValue(language.value)
                    val personalization = settings.value.personalization
                    CompositionLocalProvider(
                        LocalAppViewModel provides app,
                        LocalContext provides context,
                        LocalResources provides context.resources,
                        LocalConfiguration provides context.resources.configuration,
                        LocalAppLanguage provides language.value,
                        LocalPersonalization provides personalization,
                        LocalCompanionOutfit provides progress.value.outfitFor(personalization.companion),
                    ) {
                        ClocklingsTheme {
                            key(language.value, screen.value) { screen.value() }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    protected fun show(
        lang: AppLanguage = AppLanguage.ENGLISH,
        kind: CompanionKind? = null,
        personalization: Personalization? = null,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.runOnIdle {
            val base = personalization ?: settings.value.personalization
            settings.value =
                Settings(help = true, personalization = if (kind != null) base.copy(companion = kind) else base)
            language.value = lang
            screen.value = content
        }
        compose.waitForIdle()
    }

    protected fun string(
        lang: AppLanguage,
        id: Int,
    ): String = localized.getValue(lang).getString(id)

    protected fun clickText(
        lang: AppLanguage,
        id: Int,
    ) {
        compose.onAllNodes(hasText(string(lang, id)) and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
    }

    protected fun outDir(): File =
        File(
            checkNotNull(targetContext.getExternalFilesDir(null)),
            "review6/${InstrumentationRegistry.getArguments().getString("output") ?: cell}",
        ).apply { mkdirs() }

    protected fun save(
        name: String,
        bitmap: Bitmap,
    ) {
        val file = File(outDir(), "${name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.png")
        // Invalidate any earlier capture before changing its inset. Publish the image only when both writes succeed.
        Files.deleteIfExists(file.toPath())
        file.writeAtomically { pending ->
            pending.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            if (cell == "gallery" && android.os.Build.VERSION.SDK_INT >= 30) {
                var bottom: Int? = null
                scenario?.onActivity { activity ->
                    val insets = checkNotNull(activity.window.decorView.rootWindowInsets)
                    bottom = insets.getInsets(WindowInsets.Type.navigationBars() or WindowInsets.Type.ime()).bottom
                }
                bottom?.let { inset ->
                    val metadata = File(file.parentFile, "app-area.tsv")
                    metadata.writeAtomically { updated ->
                        val previous = if (metadata.exists()) metadata.readText() else ""
                        updated.writeText("$previous${file.name}\t$inset\n")
                    }
                }
            }
        }
    }

    private fun File.writeAtomically(write: (File) -> Unit) {
        // Keep the ordinary external-file permissions so adb can pull the published capture.
        val pending = File.createTempFile(name, ".tmp", parentFile)
        try {
            write(pending)
            Files.move(pending.toPath(), toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(pending.toPath())
        }
    }

    protected fun capture(
        name: String,
        verify: (Bitmap) -> Unit = {},
    ) {
        compose.waitForIdle()
        Thread.sleep(SETTLE_MS)
        compose.waitForIdle()
        var bitmap = screenshot()
        // The first frame after the activity starts can still be blank; take it again.
        repeat(BLANK_RETRIES) {
            if (!isBlank(bitmap)) return@repeat
            bitmap.recycle()
            Thread.sleep(SETTLE_MS)
            bitmap = screenshot()
        }
        try {
            verify(bitmap)
            save(name, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun screenshot(): Bitmap {
        val keyguard = targetContext.getSystemService(KeyguardManager::class.java)
        val gallery = cell.startsWith("gallery")
        check(!gallery || keyguard?.isKeyguardLocked == false) {
            "Unlock the test device before taking a gallery screenshot"
        }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot unavailable" }
        if (gallery && keyguard?.isKeyguardLocked != false) {
            bitmap.recycle()
            error("The test device locked during the gallery screenshot")
        }
        return bitmap
    }

    protected fun isBlank(bitmap: Bitmap): Boolean {
        val first = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        for (x in 1 until BLANK_GRID) {
            for (y in 1 until BLANK_GRID) {
                if (bitmap.getPixel(bitmap.width * x / BLANK_GRID, bitmap.height * y / BLANK_GRID) !=
                    first
                ) {
                    return false
                }
            }
        }
        return true
    }

    protected fun captureTag(
        tag: String,
        name: String,
    ) {
        compose.waitForIdle()
        Thread.sleep(SETTLE_MS)
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        save(name, bitmap)
    }

    protected fun appendMeasure(line: String) {
        File(outDir(), "measure.txt").appendText("$line\n")
    }

    protected val verticalScrollMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    protected fun scrollNode() = compose.onAllNodes(verticalScrollMatcher).onFirst()

    protected fun scrollRange(): Pair<Float, Float>? {
        val node = compose.onAllNodes(verticalScrollMatcher).fetchSemanticsNodes().firstOrNull() ?: return null
        val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
        return range.value() to range.maxValue()
    }

    protected fun scrollByPx(px: Float) {
        scrollNode().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
        compose.waitForIdle()
    }

    protected fun scrollToEnd() {
        if (scrollRange() != null) scrollByPx(SCROLL_FAR)
    }

    protected fun capturePages(name: String) {
        for (page in 0 until MAX_PAGES) {
            capture("$name-p$page")
            val (value, max) = scrollRange() ?: return
            if (value >= max - 1f) return
            scrollByPx(scrollNode().fetchSemanticsNode().size.height * PAGE_SHARE)
        }
    }

    /** B2: records whether the screen content can scroll at all (fits without scrolling when not). */
    protected fun logFit(label: String) {
        val range = scrollRange()
        val fits = range == null || range.second <= 1f
        appendMeasure("$label fitsWithoutScrolling=$fits scrollMaxPx=${range?.second}")
    }

    protected fun nudgeUntil(
        texts: List<String>,
        maxSteps: Int,
    ) {
        val actions =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
                .onFirst()
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]

        fun shown() = texts.any { compose.onAllNodes(hasText(it)).fetchSemanticsNodes().isNotEmpty() }
        var steps = 0
        while (!shown() && steps++ < maxSteps) compose.runOnIdle { actions.first().action() }
        compose.waitForIdle()
        appendMeasure("nudge $texts reached=${shown()} steps=$steps")
    }

    protected fun lessonPage(
        lang: AppLanguage,
        id: String,
        page: Int,
        name: String,
        settled: (LessonUiState) -> Boolean,
    ) {
        lateinit var vm: LessonViewModel
        compose.runOnUiThread {
            vm = LessonViewModel(listOf(id), null, services, language = lang)
            repeat(page) { vm.next() }
        }
        show(lang) {
            val state by vm.state.collectAsStateWithLifecycle()
            LessonScreen(state, false, true, {}, {}, {}, {}, {}, {})
        }
        compose.waitUntil(LESSON_TIMEOUT_MS) { settled(vm.state.value) }
        Thread.sleep(LESSON_SETTLE_MS)
        capture("$name-${lang.tag}")
    }

    protected fun measureHome(
        label: String,
        titles: List<String>,
    ) {
        val stage = compose.onAllNodes(hasTestTag("home_stage")).fetchSemanticsNodes().firstOrNull()
        val tiles =
            titles.map { title ->
                compose.onAllNodes(hasText(title) and hasClickAction()).fetchSemanticsNodes().firstOrNull()
            }
        val container = compose.onAllNodes(verticalScrollMatcher).fetchSemanticsNodes().firstOrNull()
        val content = compose.onAllNodes(hasTestTag("companion_stage")).fetchSemanticsNodes().firstOrNull()
        val skyDp = content?.let { c -> stage?.let { (c.boundsInRoot.top - it.boundsInRoot.top) / density } }
        val groundDp = content?.let { c -> stage?.let { (it.boundsInRoot.bottom - c.boundsInRoot.bottom) / density } }
        val stageBottom = stage?.boundsInRoot?.bottom?.div(density)
        val tileTop = tiles.filterNotNull().minOfOrNull { it.boundsInRoot.top }?.div(density)
        val tileBottom = tiles.filterNotNull().maxOfOrNull { it.boundsInRoot.bottom }?.div(density)
        val viewportBottom = container?.boundsInRoot?.bottom?.div(density)
        val scrolls =
            container
                ?.config
                ?.get(SemanticsProperties.VerticalScrollAxisRange)
                ?.maxValue
                ?.invoke()
        appendMeasure(
            "g1 $label skyDp=${skyDp?.fmt()} groundBelowContentDp=${groundDp?.fmt()} " +
                "stageBottomDp=${stageBottom?.fmt()} tileTopDp=${tileTop?.fmt()} " +
                "gapDp=${if (stageBottom != null && tileTop != null) (tileTop - stageBottom).fmt() else "n/a"} " +
                "tilesFound=${tiles.count { it != null }} tilesBottomDp=${tileBottom?.fmt()} " +
                "viewportBottomDp=${viewportBottom?.fmt()} scrollMaxPx=$scrolls",
        )
    }

    protected fun Float.fmt() = "%.1f".format(this)

    protected fun hasTestTag(tag: String) = SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag)

    /** H1: puts each given section title at three depths under the top of the scroll area. */
    protected fun titlesUnderTopBar(
        name: String,
        titles: List<String>,
    ) {
        val containerTop = scrollNode().fetchSemanticsNode().positionInRoot.y
        for (title in titles) {
            for (depthDp in listOf(2, 12, 22)) {
                val top =
                    compose
                        .onAllNodes(hasText(title))
                        .onFirst()
                        .fetchSemanticsNode()
                        .positionInRoot.y
                val before = scrollRange()
                scrollByPx(top - containerTop - depthDp * density)
                appendMeasure(
                    "h1 $title depth=$depthDp titleTop=$top containerTop=$containerTop " +
                        "scroll $before -> ${scrollRange()}",
                )
                capture("$name-${title.replace(' ', '_').take(MAX_TITLE)}-at${depthDp}dp")
            }
        }
    }

    protected fun dumpPages(name: String) {
        for (page in 0 until MAX_PAGES) {
            dumpTree("$name-p$page")
            val range = scrollRange()
            if (range == null || range.first >= range.second - 1f) break
            scrollByPx(scrollNode().fetchSemanticsNode().size.height * PAGE_SHARE)
        }
    }
}

private const val GLOW_COUNT = 12
private const val SHEET_PAGE = 10
private const val SETTLE_MS = 450L
private const val BLANK_RETRIES = 4
private const val FRAME_STEP_MS = 50L
private const val SET_WRONG_OFFSET = 60
private const val MINUTES_PER_DAY = 1440
private const val ADVANCE_WAIT_MS = 2000L
private const val BLANK_GRID = 12
private const val SCROLL_FAR = 50_000f
private const val MAX_PAGES = 12
private const val PAGE_SHARE = 0.8f
private const val LESSON_TIMEOUT_MS = 30_000L
private const val LESSON_SETTLE_MS = 700L
private const val NANOS_PER_MS = 1_000_000L
private const val LOCKED_ALPHA = 0.55f
private const val MAX_TITLE = 14
private const val DUMP_SETTLE_MS = 1500L
private const val RESIZE_MID_MS = 100L
private const val RESIZE_SETTLED_MS = 450L
private const val RESIZE_LATE_MS = 900L
private const val EXPLORE_STEPS = 400
private const val TAP_FREE_MIN_DP = 40f
private const val TAP_INSET_DP = 12f
private const val TAP_X = 0.1f
private const val FRAME_MS = 16L
private const val COLD_FRAMES_MS = 160L
private const val REDUCED_MOTION_MS = 400L
private const val HOLD_MS = 800L
private const val R3_SCORE = 17
private const val R3_RECORD_SCORE = 31
private const val R3_BEST = 25
private const val R3_CONFETTI_MS = 160L
private const val R3_QUESTIONS = 3
private val FI_12 = AppLanguage.FINNISH to false
private val FI_24 = AppLanguage.FINNISH to true
private val EN_12 = AppLanguage.ENGLISH to false
private val SV_12 = AppLanguage.SWEDISH to false
