package fi.clocklings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Lesson
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Levels
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.NextAction
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLocale
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.settings.SettingsScreen
import fi.clocklings.ui.theme.SkDimens
import org.junit.Test
import java.util.Locale

/** Capture the visual follow-up and the corrected Finnish gallery fixtures on the dedicated emulator. */
class VisualFollowUpRound2CaptureTest : ScreenshotReview6Host() {
    @Test
    fun practiceAndRewards() {
        launchHost()
        val lang = captureLanguage
        TaskType.entries.forEach { capturePracticeTask(lang, it) }
        captureRewards(lang)
    }

    private fun capturePracticeTask(
        lang: AppLanguage,
        type: TaskType,
    ) {
        val reading = type == TaskType.READ
        val initial = gameState(type)
        val state = mutableStateOf(initial)
        val prefix = "level-1-${type.name.lowercase(Locale.ROOT)}"
        show(lang, CompanionKind.HUMAN) { GameScreen(state.value, true, {}, {}, {}, {}, {}, {}) }
        val before = captureTask(prefix, type)
        val hint =
            if (reading) {
                Hints.forRead(initial.task.time, 120)
            } else {
                Hints.forSet(initial.task.time, initial.clock.time)
            }
        compose.runOnIdle {
            state.value =
                initial.copy(
                    feedback = GameFeedback.HintText(hint),
                    optionStates = if (reading) mapOf(120 to OptionState.WRONG) else emptyMap(),
                    taskMistakes = 1,
                )
        }
        val hinted = captureTask("$prefix-hint", type)
        compose.runOnIdle {
            state.value =
                initial.copy(
                    feedback = if (reading) GameFeedback.RevealRead(180) else GameFeedback.RevealSet(180),
                    optionStates =
                        if (reading) {
                            mapOf(120 to OptionState.WRONG, 180 to OptionState.CORRECT, 240 to OptionState.WRONG)
                        } else {
                            emptyMap()
                        },
                    clock = ClockCommand(180),
                    showDone = false,
                    showNext = true,
                    interactive = false,
                    busy = true,
                    taskMistakes = 2,
                )
        }
        val revealed = captureTask("$prefix-reveal", type)
        if (reading && cell != "gallery") {
            scrollByPx(-100_000f)
            capture("practice--$prefix-reveal-panel-top")
        }
        compose.runOnIdle { state.value = initial.copy(taskIndex = initial.taskIndex + 1) }
        val next = clockWidth()
        appendMeasure("R2 $cell $type dp=$before,$hinted,$revealed next=$next")
    }

    private fun captureRewards(lang: AppLanguage) {
        val companions =
            if (cell == "gallery") listOf(CompanionKind.CAT) else listOf(CompanionKind.HUMAN, CompanionKind.CAT)
        for (kind in companions) {
            for (count in 1..3) {
                check(Wardrobe.newlyUnlocked(kind, 0, count).size == count)
                val name = "result-$count-stars"
                val file =
                    when {
                        cell == "gallery" -> "screens--$name-p0"
                        kind == CompanionKind.CAT -> "practice--cat--$name"
                        else -> "practice--$name"
                    }
                show(lang, kind) {
                    ResultScreen(
                        AccessPolicy(true),
                        Screen.Result(0, count, 0, 0),
                        Outfit.default(kind),
                        {},
                        {},
                        {},
                        {},
                    )
                }
                compose.mainClock.advanceTimeBy(2_400)
                capture(file)
                val action =
                    compose
                        .onNode(
                            hasText(string(lang, R.string.dress_doll)) and hasClickAction(),
                        ).fetchSemanticsNode()
                val viewport = scrollNode().fetchSemanticsNode().boundsInRoot
                val cardBottom = (action.positionInRoot.y + action.size.height) / density + SkDimens.Gap.value
                val gapBeforeFade = viewport.bottom / density - cardBottom - SkDimens.ScrollFade.value
                appendMeasure("R1 $cell $kind items=$count scroll=${scrollRange()} cardClearanceDp=$gapBeforeFade")
                if (cell != "gallery" && (scrollRange()?.second ?: 0f) > 1f) {
                    scrollToEnd()
                    capture("$file-end")
                }
            }
        }
    }

    @Test
    fun exploreAndLessons() {
        launchHost()
        val lang = captureLanguage
        captureExplore(lang)
        if (cell != "gallery") {
            show(lang, CompanionKind.HUMAN) { AskScreen(true, true, true, {}) }
            capture("main--05-together-set")
            clickText(lang, R.string.ask_go)
            capture("main--06-together-guess")
            clickText(lang, R.string.ask_reveal)
            capture("main--07-together-reveal")
        }
        captureLessons(lang)
    }

    private fun captureExplore(lang: AppLanguage) {
        if (cell == "gallery") {
            explore(lang, h24 = false, guides = true)
            capture("screens--main-explore-12h-p0")
            explore(lang, h24 = true, guides = true)
            capturePages("screens--main-explore-24h")
            explore(lang, h24 = true, guides = false)
            capture("screens--main-explore-no-guides-p0")
            return
        }
        explore(lang, h24 = false, guides = true)
        capture("main--02-explore-12h")
        val times =
            if (cell == "base" ||
                cell == "combined"
            ) {
                ExploreTimes
            } else {
                ExploreTimes.filter { it.first == 1095 }
            }
        for (guides in listOf(true, false)) {
            explore(lang, h24 = true, guides = guides)
            var previous = ClockTime.start(true)
            for ((time, name) in times) {
                moveExploreClock(previous, time)
                previous = time
                compose.onNodeWithText(timeLanguage(lang).digital(time, h24 = true)).assertExists()
                val file = if (guides) name else name.replace("03-explore-24h", "04-explore-without-guides")
                capture("main--$file")
                if (!guides && time == 18 * 60 + 15) capture("main--04-explore-without-guides")
                if (guides) {
                    val to = textBounds(lang, R.string.clock_zone_vaille)
                    val past = textBounds(lang, R.string.clock_zone_yli)
                    val part =
                        compose
                            .onNodeWithText(
                                timeLanguage(lang).dayPart(time).name,
                            ).fetchSemanticsNode()
                            .boundsInRoot
                    appendMeasure("R3 $cell ${lang.tag} $time to=$to part=$part past=$past")
                }
            }
        }
        if (cell == "base") captureLocalizedExplore()
    }

    private fun captureLocalizedExplore() {
        for (locale in AppLanguage.entries) {
            explore(locale, h24 = true, guides = true)
            moveExploreClock(ClockTime.start(true), 18 * 60 + 15)
            val to = textBounds(locale, R.string.clock_zone_vaille)
            val past = textBounds(locale, R.string.clock_zone_yli)
            val part =
                compose
                    .onNodeWithText(
                        timeLanguage(locale).dayPart(1095).name,
                    ).fetchSemanticsNode()
                    .boundsInRoot
            appendMeasure("R3 locales ${locale.tag} to=$to part=$part past=$past")
            capture("locales--${locale.tag}--03-explore-24h")
        }
    }

    private fun captureLessons(lang: AppLanguage) {
        for ((index, id) in (Lessons.levelLessons.flatten() + Lessons.DAY_LESSON).withIndex()) {
            val number = (index + 1).toString().padStart(2, '0')
            val lesson = LessonCatalog.lesson(id, lang)
            if (cell == "gallery" && lesson.pages.any { it.period == ClockTime.DAY }) {
                captureLessonPages(lang, lesson, number, id)
            }

            val state =
                LessonUiState(
                    lesson = lesson,
                    page = lesson.pages.size,
                    clock = ClockCommand(lesson.quiz.time),
                    period = lesson.quiz.period,
                    choices = lesson.quiz.choices,
                )
            lesson(lang, state)
            capture(if (cell == "gallery") "screens--lesson-$number-$id-quiz-p0" else "lessons--$number-$id-quiz")
            if (index == 0) {
                val wrong = state.choices.indexOfFirst { !it.correct }
                val correct = state.choices.indexOfFirst { it.correct }
                lesson(lang, state.copy(showHint = true, choiceStates = mapOf(wrong to OptionState.WRONG)))
                capture(
                    if (cell ==
                        "gallery"
                    ) {
                        "screens--lesson-01-viisarit-quiz-hint-p0"
                    } else {
                        "lessons--01-viisarit-hint"
                    },
                )
                lesson(
                    lang,
                    state.copy(
                        quizDone = true,
                        choiceStates = mapOf(correct to OptionState.CORRECT),
                        nextAction = NextAction.FINISH,
                    ),
                )
                capture(
                    if (cell ==
                        "gallery"
                    ) {
                        "screens--lesson-01-viisarit-quiz-complete-p0"
                    } else {
                        "lessons--01-viisarit-completed"
                    },
                )
                lesson(lang, state.copy(saveFailed = true))
                compose.onNodeWithText(string(lang, R.string.settings_save_error)).assertExists()
                capture(if (cell == "gallery") "screens--lesson-save-failure-p0" else "lessons--lesson-save-failure")
            }
        }
    }

    private fun captureLessonPages(
        lang: AppLanguage,
        lesson: Lesson,
        number: String,
        id: String,
    ) {
        for ((page, data) in lesson.pages.withIndex()) {
            val state =
                LessonUiState(
                    lesson = lesson,
                    page = page,
                    clock = ClockCommand(data.time),
                    period = data.period,
                    help = data.help,
                    under = data.under,
                    nextAction =
                        if (page ==
                            lesson.pages.lastIndex
                        ) {
                            NextAction.TRY_IT
                        } else {
                            NextAction.NEXT_PAGE
                        },
                )
            lesson(lang, state)
            capture("screens--lesson-$number-$id-page-${page + 1}-p0")
        }
    }

    @Test
    fun homeLevelsAndChallenge() {
        launchHost()
        val lang = captureLanguage
        captureHome(lang)
        for (full in listOf(true, false)) {
            show(lang) { LevelsScreen(Progress(), AccessPolicy(full), {}, {}, {}, {}) }
            val file =
                when {
                    cell == "gallery" && full -> "screens--play-levels-p0"
                    cell == "gallery" -> "screens--play-levels-locked-p0"
                    full -> "play--01-levels"
                    else -> "free--01-levels"
                }
            capture(file)
        }
        if (cell == "gallery") {
            for (kind in CompanionKind.entries) {
                show(lang, kind) { HomeScreen(progress.value, settings.value, {}) }
                capture("screens--companion-home-${kind.name.lowercase(Locale.ROOT)}")
            }
        }
        captureChallenges(lang)
    }

    private fun captureHome(lang: AppLanguage) {
        for (locale in if (cell == "gallery") listOf(lang) else listOf(lang, AppLanguage.FINNISH)) {
            show(locale, CompanionKind.HUMAN) { HomeScreen(Progress(stars = List(5) { 3 }), settings.value, {}) }
            val file =
                when {
                    cell == "gallery" -> "screens--main-home-p0"
                    locale == lang -> "main--01-home"
                    else -> "main--fi--01-home"
                }
            capture(file)
            val titleTops =
                homeTitleIds.map {
                    compose
                        .onNodeWithText(
                            string(locale, it),
                            useUnmergedTree = true,
                        ).fetchSemanticsNode()
                        .positionInRoot.y /
                        density
                }
            appendMeasure("R5 $cell ${locale.tag} dp=$titleTops")
            if ((scrollRange()?.second ?: 0f) > 1f && cell != "gallery") {
                scrollToEnd()
                capture("$file-end")
            }
        }
    }

    private fun captureChallenges(lang: AppLanguage) {
        val result = ChallengeUiState(phase = ChallengePhase.END, score = 7, best = 9)
        val start = ChallengeUiState(score = 7, best = 9)
        val running =
            start.copy(
                phase = ChallengePhase.RUN,
                question = 180,
                options = listOf(120, 180, 240),
                clock = ClockCommand(180),
                deadlineNanos = System.nanoTime() + 60_000_000_000L,
            )
        val cases =
            listOf(
                Triple("04-challenge-intro", "challenge-intro", start),
                Triple("05-challenge-playing", "challenge-running", running),
                Triple("06-challenge-result", "challenge-result", result),
                Triple("07-challenge-record", "challenge-record", result.copy(score = 12, best = 12, newRecord = true)),
                Triple("08-challenge-save-failure", "challenge-save-failure", result.copy(saveFailed = true)),
            )
        for ((after, gallery, state) in cases) {
            captureChallenge(lang, after, gallery, state)
        }
        show(lang, CompanionKind.NONE) { ChallengeScreen(result.copy(newRecord = true), false, true, {}, {}, {}) }
        capture(
            if (cell ==
                "gallery"
            ) {
                "screens--challenge-no-companion-p0"
            } else {
                "play--07-challenge-record-no-companion"
            },
        )
        if (cell != "gallery") {
            show(lang, CompanionKind.NONE) { ChallengeScreen(result.copy(saveFailed = true), false, true, {}, {}, {}) }
            capture("play--08-challenge-save-failure-no-companion")
        }
    }

    private fun captureChallenge(
        lang: AppLanguage,
        after: String,
        gallery: String,
        state: ChallengeUiState,
    ) {
        val liveTimer = state.phase == ChallengePhase.RUN
        if (liveTimer) compose.mainClock.autoAdvance = false
        try {
            show(lang, CompanionKind.CAT) { ChallengeScreen(state, false, true, {}, {}, {}) }
            if (liveTimer) advanceFrames(450L)
            capture(if (cell == "gallery") "screens--$gallery-p0" else "play--$after")
            if (liveTimer) {
                val score = localized.getValue(lang).getString(R.string.challenge_score, state.score)
                compose.onNodeWithText(score).assertExists()
            }
        } finally {
            if (liveTimer) {
                compose.runOnUiThread { screen.value = {} }
                compose.mainClock.advanceTimeBy(450L)
                compose.mainClock.autoAdvance = true
            }
        }
        if (state.phase == ChallengePhase.START) {
            val score = localized.getValue(lang).getString(R.string.challenge_score, state.score)
            compose.onNodeWithText(score).assertDoesNotExist()
        }
        if (state.phase == ChallengePhase.END && !state.newRecord) {
            val action = if (state.saveFailed) R.string.settings_retry else R.string.play_again
            val back = textBounds(lang, R.string.back_description)
            val end = textBounds(lang, action)
            appendMeasure("R8 $cell failed=${state.saveFailed} back=$back end=$end")
            if (state.saveFailed) compose.onNodeWithText(string(lang, R.string.settings_save_error)).assertExists()
        }
    }

    @Test
    fun purchaseAndStartup() {
        launchHost()
        val lang = captureLanguage
        val purchase = mutableStateOf(PurchaseUiState())
        show(lang) { ParentScreen(purchase.value, {}, {}, {}, {}, usage = services.usage) }
        val digits = Regex("[0-9](?: [0-9]){3}")
        val text =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.Text] }
                .first { digits.containsMatchIn(it.text) }
        val reversedCode = checkNotNull(digits.find(text.text)).value.filter(Char::isDigit).reversed()
        compose.onNode(hasSetTextAction()).performTextReplacement(reversedCode)
        clickText(lang, R.string.parent_continue)
        for (status in PurchaseStatus.entries) {
            val purchasable =
                status == PurchaseStatus.READY || status == PurchaseStatus.CANCELLED ||
                    status == PurchaseStatus.NOT_OWNED
            val price = if (lang == AppLanguage.FINNISH) "6,99 €" else "€6.99"
            compose.runOnIdle {
                purchase.value =
                    PurchaseUiState(
                        status = status,
                        localizedPrice = price,
                        fullAccess = status == PurchaseStatus.OWNED || status == PurchaseStatus.RESTORED,
                        canPurchase = purchasable,
                        busy = status == PurchaseStatus.VERIFYING,
                    )
            }
            val file = "parent-store-${status.name.lowercase(Locale.ROOT)}"
            val buy = localized.getValue(lang).getString(R.string.purchase_buy, price)
            if (purchasable) {
                compose.onNode(hasText(buy) and hasClickAction()).assertIsEnabled()
            } else if (status != PurchaseStatus.VERIFYING) {
                compose.onNodeWithText(buy).assertDoesNotExist()
            }
            scrollByPx(-100_000f)
            if (cell == "gallery") capturePages("screens--$file") else capture("parent--$file-p0")
        }

        show(lang) {
            val locale =
                remember {
                    AppLocaleController(
                        services.repository,
                        object : AppLocalePlatform {
                            override val supported = true

                            override fun read(): AppLanguage = error("Startup locale read failure fixture")

                            override fun write(language: AppLanguage?) = Unit
                        },
                    )
                }
            AppLocaleProvider(services.repository, controller = locale) {}
        }
        compose.onNodeWithText(string(lang, R.string.language_save_error)).assertIsDisplayed()
        compose.onNode(hasClickAction() and hasText(string(lang, R.string.settings_retry))).assertIsDisplayed()
        capture("startup--05-locale-retry")
    }

    @Test
    fun settingsAndPracticeFixtures() {
        launchHost()
        val lang = captureLanguage
        val set = gameState(TaskType.SET)
        show(lang) { GameScreen(set, false, {}, {}, {}, {}, {}, {}) }
        capture(if (cell == "gallery") "screens--practice-no-guides-p0" else "practice--practice-no-guides")
        val dayTask =
            gameState(TaskType.READ).copy(
                h24 = true,
                task = Task(930, TaskType.READ),
                options = listOf(870, 930, 990),
                clock = ClockCommand(930),
                step = 5,
            )
        show(lang) { GameScreen(dayTask, true, {}, {}, {}, {}, {}, {}) }
        capture(if (cell == "gallery") "screens--practice-24h-p0" else "practice--24h-1530")
        if (cell != "gallery") return
        for (speech in listOf(false, true)) {
            show(lang) {
                val controller =
                    remember {
                        AppLocaleController(
                            services.repository,
                            object : AppLocalePlatform {
                                override val supported = true

                                override fun read() = AppLanguage.FINNISH

                                override fun write(language: AppLanguage?) = Unit
                            },
                        )
                    }
                CompositionLocalProvider(LocalAppLocale provides controller) {
                    SettingsScreen(Settings(h24 = speech, sound = !speech), speech, {}, {}, onParent = {})
                }
            }
            compose.onNodeWithText("Suomi").assertExists()
            compose.onNodeWithText(string(lang, R.string.parent_title)).assertExists()
            capture(if (speech) "screens--settings-speech-available" else "screens--settings-main-p0")
        }
    }

    private val captureLanguage: AppLanguage
        get() = if (cell == "gallery") AppLanguage.FINNISH else AppLanguage.ENGLISH

    private fun gameState(type: TaskType): GameUiState {
        val target = 180
        val setting = type == TaskType.SET
        return GameUiState(
            level = 0,
            h24 = false,
            taskIndex = if (setting) 1 else 0,
            taskCount = Levels.TASK_COUNT,
            task = Task(target, type),
            options = if (setting) emptyList() else listOf(120, target, 240),
            clock = ClockCommand(if (setting) Levels.setTaskStart(target) else target),
            step = 60,
            interactive = setting,
            showDone = setting,
        )
    }

    private fun captureTask(
        file: String,
        type: TaskType,
    ): Float {
        capture(
            if (cell == "gallery") {
                val gallery =
                    if (file.endsWith("-hint") || file.endsWith("-reveal")) {
                        file.removePrefix("level-1-")
                    } else {
                        file
                    }
                "screens--practice-$gallery-p0"
            } else {
                "practice--$file"
            },
        )
        if (type == TaskType.READ) {
            // Even if the panel scrolls at large font sizes, it still contains all the answers.
            check(compose.onAllNodesWithTag("option_time", useUnmergedTree = true).fetchSemanticsNodes().size == 3)
        }
        return clockWidth()
    }

    private fun clockWidth(): Float =
        compose
            .onNodeWithTag("task_clock", useUnmergedTree = true)
            .fetchSemanticsNode()
            .size.width / density

    private fun explore(
        lang: AppLanguage,
        h24: Boolean,
        guides: Boolean,
    ) {
        show(lang, CompanionKind.HUMAN) { FreeClockScreen(h24, guides, true, {}) }
    }

    private fun moveExploreClock(
        from: Int,
        to: Int,
    ) {
        val forward = Math.floorMod(to - from, ClockTime.DAY)
        val delta = if (forward > ClockTime.HALF_DAY) forward - ClockTime.DAY else forward
        if (delta == 0) return
        val actions =
            compose
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions), useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            repeat(kotlin.math.abs(delta) / 5) {
                check(actions[if (delta > 0) 0 else 1].action())
            }
        }
        compose.waitForIdle()
    }

    private fun lesson(
        lang: AppLanguage,
        state: LessonUiState,
    ) {
        show(lang, CompanionKind.HUMAN) { LessonScreen(state, false, true, {}, {}, {}, {}, {}, {}) }
    }

    private fun textBounds(
        lang: AppLanguage,
        id: Int,
    ) = compose.onNodeWithText(string(lang, id), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private companion object {
        val ExploreTimes =
            listOf(
                0 to "03-explore-24h-0000",
                390 to "03-explore-24h-0630",
                930 to "03-explore-24h-1530",
                1095 to "03-explore-24h",
                1140 to "03-explore-24h-1900",
                1425 to "03-explore-24h-2345",
            )
        val homeTitleIds = listOf(R.string.menu_koulu, R.string.menu_vapaa, R.string.menu_pelaa, R.string.menu_kysy)
    }
}
