package fi.clocklings

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.window.SplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.analytics.UsageAnalytics
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.CompanionColor
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
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.LocalClockTime
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeScreen
import fi.clocklings.ui.challenge.ChallengeUiState
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.LoadingScreen
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.game.GameFeedback
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.greetingText
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.intro.IntroOverlay
import fi.clocklings.ui.intro.IntroTimeline
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.lesson.LessonViewModel
import fi.clocklings.ui.lesson.NextAction
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLocale
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.resolve
import fi.clocklings.ui.settings.LanguageScreen
import fi.clocklings.ui.settings.SettingsScreen
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.timeLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Capture the visual follow-up and the Finnish gallery with isolated settings and progress. */
class VisualFollowUpRound2CaptureTest : VisualFollowUpCaptureHost() {
    @Test
    fun practiceAndRewards() {
        launchCaptureHost()
        val lang = captureLanguage
        TaskType.entries.forEach { capturePracticeTask(lang, it) }
        captureRewards(lang)
        if (cell == "gallery") captureMorePracticeAndResults(lang)
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
        if (reading && cell == "gallery") {
            measureOption(lang, "practice-read-reveal", timeLabel(lang, initial.task.time, initial.h24))
        }
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
            if (cell == "gallery") CompanionKind.entries else listOf(CompanionKind.HUMAN, CompanionKind.CAT)
        for (kind in companions) {
            val counts = if (kind == CompanionKind.HUMAN || kind == CompanionKind.CAT) 1..3 else 3..3
            for (count in counts) {
                captureReward(lang, kind, count)
            }
        }
    }

    private fun captureReward(
        lang: AppLanguage,
        kind: CompanionKind,
        count: Int,
    ) {
        val file = rewardFile(kind, count)
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
        val companion = compose.onNodeWithTag("result_companion", useUnmergedTree = true)
        val companionHeight = companion.fetchSemanticsNode().size.height / density
        if (Wardrobe.newlyUnlocked(kind, 0, count).isEmpty()) {
            appendMeasure("R1 $cell $kind items=0 companionHeightDp=$companionHeight")
            return
        }
        val action =
            compose
                .onNode(
                    hasText(string(lang, R.string.dress_doll)) and hasClickAction(),
                ).fetchSemanticsNode()
        val viewport = scrollNode().fetchSemanticsNode().boundsInRoot
        val cardBottom = (action.positionInRoot.y + action.size.height) / density + SkDimens.Gap.value
        val gapBeforeFade = viewport.bottom / density - cardBottom - SkDimens.ScrollFade.value
        val actionClearance = gapBeforeFade + SkDimens.Gap.value
        appendMeasure(
            "R1 $cell $kind items=$count companionHeightDp=$companionHeight " +
                "cardClearanceDp=$gapBeforeFade actionClearanceDp=$actionClearance",
        )
        val measureGalleryClearance = cell == "gallery" && kind == CompanionKind.HUMAN && count >= 2
        if (measureGalleryClearance && density in 2.6f..2.7f && targetContext.resources.configuration.fontScale == 1f) {
            assertTrue("$file outfit button clearance is $actionClearance dp", actionClearance >= -0.5f)
        }
        if (cell != "gallery" && (scrollRange()?.second ?: 0f) > 1f) {
            scrollToEnd()
            capture("$file-end")
        }
    }

    private fun rewardFile(
        kind: CompanionKind,
        count: Int,
    ): String {
        val name = "result-$count-stars"
        return when {
            cell == "gallery" && kind != CompanionKind.HUMAN -> {
                "screens--$name-${kind.name.lowercase(Locale.ROOT)}-p0"
            }

            cell == "gallery" -> {
                "screens--$name-p0"
            }

            kind == CompanionKind.CAT -> {
                "practice--cat--$name"
            }

            else -> {
                "practice--$name"
            }
        }
    }

    @Test
    fun exploreAndLessons() = captureExploreAndLessons()

    @Test
    fun homeLevelsAndChallenge() {
        launchCaptureHost()
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
                showWithFixedClock(lang, kind) { HomeScreen(progress.value, settings.value, {}) }
                capture("screens--companion-home-${kind.name.lowercase(Locale.ROOT)}")
            }
        }
        captureChallenges(lang)
    }

    private fun captureChallenges(lang: AppLanguage) {
        val result = ChallengeUiState(phase = ChallengePhase.END, score = 7, best = 9)
        val start = ChallengeUiState(score = 7, best = 9)
        val running =
            start.copy(
                phase = ChallengePhase.RUN,
                score = if (cell == "gallery") 0 else start.score,
                question = 180,
                options = listOf(120, 180, 240),
                clock = ClockCommand(180),
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
        if (cell == "gallery") {
            captureChallenge(
                lang,
                "challenge-wrong-answer",
                "challenge-wrong-answer",
                running.copy(
                    score = start.score,
                    question = 240,
                    clock = ClockCommand(240),
                    optionStates = mapOf(120 to OptionState.WRONG, 240 to OptionState.CORRECT),
                    busy = true,
                ),
            )
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
        val seconds =
            when {
                cell != "gallery" -> 60
                state.busy -> 55
                else -> 59
            }
        val fixture = if (liveTimer) state.copy(deadlineNanos = TimeUnit.SECONDS.toNanos(seconds.toLong())) else state
        if (liveTimer) compose.mainClock.autoAdvance = false
        try {
            show(lang, CompanionKind.CAT) { ChallengeScreen(fixture, false, true, {}, {}, {}, nowNanos = { 0L }) }
            if (liveTimer) {
                advanceFrames(450L)
                if (cell == "gallery") {
                    checkGalleryChallengeTimer(lang, gallery, state, seconds)
                }
            }
            capture(if (cell == "gallery") "screens--$gallery-p0" else "play--$after")
            if (liveTimer) {
                val score = localized.getValue(lang).getString(R.string.challenge_score, state.score)
                compose.onNode(hasText(score) or hasContentDescription(score)).assertExists()
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
            checkChallengeResult(lang, state)
        }
    }

    private fun checkGalleryChallengeTimer(
        lang: AppLanguage,
        gallery: String,
        state: ChallengeUiState,
        seconds: Int,
    ) {
        measureOption(lang, gallery, timeLabel(lang, state.options.last(), false))
        val time = localized.getValue(lang).getString(R.string.challenge_seconds_left, seconds)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, time)).assertIsDisplayed()
        appendMeasure("$gallery remainingSeconds=$seconds")
        val timer = compose.onNodeWithTag("challenge_timer", useUnmergedTree = true).fetchSemanticsNode()
        val row =
            compose
                .onAllNodesWithTag("task_progress_row", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .singleOrNull()
        if (row != null) {
            val timerWidth = timer.size.width / density
            val rowWidth = row.size.width / density
            appendMeasure("$gallery timerWidthDp=$timerWidth rowWidthDp=$rowWidth")
        }
    }

    private fun checkChallengeResult(
        lang: AppLanguage,
        state: ChallengeUiState,
    ) {
        val action = if (state.saveFailed) R.string.settings_retry else R.string.play_again
        val back = textBounds(lang, R.string.back_description)
        val end = textBounds(lang, action)
        appendMeasure("R8 $cell failed=${state.saveFailed} back=$back end=$end")
        if (cell == "gallery" && !state.saveFailed) {
            val number = compose.onNodeWithText(state.score.toString(), useUnmergedTree = true).fetchSemanticsNode()
            val clearance = (back.top - number.positionInRoot.y - number.size.height) / density
            appendMeasure("challenge-result scoreBottomClearanceDp=$clearance")
            assertEquals("Score visible height", number.size.height.toFloat(), number.boundsInRoot.height, 1f)
        }
        if (state.saveFailed) compose.onNodeWithText(string(lang, R.string.progress_save_error)).assertExists()
    }

    @Test
    fun purchaseAndStartup() {
        launchCaptureHost()
        val lang = captureLanguage
        val purchase = mutableStateOf(PurchaseUiState())
        val usage =
            object : UsageAnalytics {
                override val available = true
                override val enabled = MutableStateFlow(false)

                override fun setEnabled(enabled: Boolean) {
                    this.enabled.value = enabled
                }

                override fun count(metric: String) = Unit
            }
        val gateRandom = if (cell == "gallery") Random(0) else Random.Default
        show(lang) {
            ParentScreen(
                purchase.value,
                {},
                {},
                {},
                {},
                usage = if (cell == "gallery") usage else services.usage,
                gateNextInt = gateRandom::nextInt,
            )
        }
        val multiplication = Regex("([0-9]{2}) × ([0-9])")

        fun problem() =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes()
                .flatMap { it.config[SemanticsProperties.Text] }
                .first { multiplication.containsMatchIn(it.text) }
                .text
        if (cell == "gallery") {
            val original = problem()
            capture("screens--parent-gate-p0")
            compose.onNode(hasSetTextAction()).performClick()
            capture("screens--parent-gate-keyboard")
            compose.onNode(hasSetTextAction()).performTextReplacement("0")
            hideKeyboard()
            val submit = compose.onNode(hasText(string(lang, R.string.parent_continue)) and hasClickAction())
            submit.performScrollTo().assertIsEnabled().performClick()
            val error = compose.onNodeWithText(string(lang, R.string.parent_gate_error))
            error.performScrollTo().assertIsDisplayed()
            assertTrue("The gate must generate a new calculation", original != problem())
            capture("screens--parent-gate-error-p0")
        }
        val (first, second) = checkNotNull(multiplication.find(problem())).destructured
        compose.onNode(hasSetTextAction()).performTextReplacement((first.toInt() * second.toInt()).toString())
        compose
            .onNode(hasText(string(lang, R.string.parent_continue)) and hasClickAction())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        captureStoreStates(lang, purchase)
        if (cell == "gallery") {
            compose.runOnIdle { usage.setEnabled(true) }
            val title = compose.onNodeWithText(string(lang, R.string.usage_sharing_title))
            title.performScrollTo().assertIsDisplayed()
            capture("screens--parent-usage-sharing-on")
        }
        captureLocaleReadFailure(lang)
    }

    private fun captureStoreStates(
        lang: AppLanguage,
        purchase: MutableState<PurchaseUiState>,
    ) {
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
    }

    private fun captureLocaleReadFailure(lang: AppLanguage) {
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
        launchCaptureHost()
        val lang = captureLanguage
        val set = gameState(TaskType.SET)
        show(lang) { GameScreen(set, false, {}, {}, {}, {}, {}, {}) }
        capture(if (cell == "gallery") "screens--practice-no-guides-p0" else "practice--practice-no-guides")
        val dayTask =
            gameState(TaskType.READ).copy(
                level = 1,
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
        compose
            .onNode(hasText(string(lang, R.string.settings_reset_action)) and hasClickAction())
            .performScrollTo()
            .performClick()
        capture("screens--settings-reset-dialog")
        show(lang) { LanguageScreen({}) }
        capturePages("screens--settings-language")
    }

    @Test
    fun companionsAndClocks() = captureCompanionsAndClocks()

    private fun captureMorePracticeAndResults(lang: AppLanguage) {
        for (level in 1..4) {
            for (type in TaskType.entries) {
                val setting = type == TaskType.SET
                val step = listOf(60, 30, 15, 5, 1)[level]
                val target = 180 + step
                val state =
                    gameState(type).copy(
                        level = level,
                        task = Task(target, type),
                        clock = ClockCommand(if (setting) Levels.setTaskStart(target) else target),
                        options = if (setting) emptyList() else listOf(target - 60, target, target + 60),
                        step = step,
                    )
                show(lang, CompanionKind.HUMAN) { GameScreen(state, true, {}, {}, {}, {}, {}, {}) }
                capturePages("screens--practice-level-${level + 1}-${type.name.lowercase(Locale.ROOT)}")
            }
        }
        for (type in TaskType.entries) {
            val task = gameState(type)
            val reading = type == TaskType.READ
            val state =
                task.copy(
                    feedback = GameFeedback.Praise(0, task.task.time.takeIf { reading }),
                    optionStates = if (reading) mapOf(task.task.time to OptionState.CORRECT) else emptyMap(),
                    showDone = false,
                    interactive = false,
                    busy = true,
                    clock = ClockCommand(task.task.time),
                )
            show(lang, CompanionKind.HUMAN) { GameScreen(state, true, {}, {}, {}, {}, {}, {}) }
            capturePages("screens--practice-${type.name.lowercase(Locale.ROOT)}-correct")
        }
        capturePracticeSaveFailure(lang)
        captureFinalResults(lang)
    }

    private fun capturePracticeSaveFailure(lang: AppLanguage) {
        val set = gameState(TaskType.SET)
        val failed =
            set.copy(
                taskIndex = set.taskCount - 1,
                outcomes = List(set.taskCount) { DotOutcome.FIRST_TRY },
                feedback = GameFeedback.SaveFailed,
                clock = ClockCommand(set.task.time),
                interactive = false,
                showDone = false,
                showNext = true,
                busy = true,
            )
        show(lang, CompanionKind.HUMAN) { GameScreen(failed, true, {}, {}, {}, {}, {}, {}) }
        capturePages("screens--practice-save-failure")
        // If task generation stops at the draw limit with an odd count, the last task is READ.
        val read = gameState(TaskType.READ)
        val readCount = failed.taskCount - 1
        val failedRead =
            failed.copy(
                task = read.task,
                clock = read.clock,
                options = read.options,
                optionStates = mapOf(read.task.time to OptionState.CORRECT),
                taskIndex = readCount - 1,
                taskCount = readCount,
                outcomes = failed.outcomes.take(readCount),
            )
        show(lang, CompanionKind.HUMAN) { GameScreen(failedRead, true, {}, {}, {}, {}, {}, {}) }
        val correct = timeLabel(lang, read.task.time, read.h24)
        compose.onNode(hasText(correct) and hasClickAction()).performScrollTo().assertIsDisplayed()
        capture("screens--practice-save-failure-correct-option")
    }

    private fun captureFinalResults(lang: AppLanguage) {
        for (fallback in listOf(false, true)) {
            show(lang, CompanionKind.CAT) {
                key(fallback) {
                    ResultScreen(
                        AccessPolicy(true),
                        Screen.Result(4, 3, 3, 0),
                        Outfit.default(CompanionKind.CAT),
                        {},
                        {},
                        {},
                        {},
                        canReplay = !fallback,
                        canContinue = false,
                    )
                }
            }
            compose.mainClock.advanceTimeBy(2_400)
            val stars = localized.getValue(lang).resources.getQuantityString(R.plurals.result_stars_description, 3, 3)
            compose.onNodeWithContentDescription(stars).assertIsDisplayed()
            capturePages("screens--result-${if (fallback) "fallback" else "final"}")
        }
    }

    @Test
    fun startupScreens() = captureStartupScreens()

    @Test
    fun reducedLessonAndReplay() = captureReducedLessonAndReplay()

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
}

abstract class VisualFollowUpCaptureHost : ScreenshotReview6Host() {
    private val galleryPurchases = DeviceTestPurchases(fullAccess = true)
    private val galleryServices by lazy {
        object : AppServices by services {
            override val repository = ProgressRepository(GalleryPreferences())
            override val purchases = galleryPurchases
            override val usage = UsageAnalytics.Disabled
        }
    }

    protected fun launchCaptureHost() {
        launchHost(if (cell.startsWith("gallery")) galleryServices else services)
    }

    protected fun showWithFixedClock(
        lang: AppLanguage,
        kind: CompanionKind? = null,
        content: @Composable () -> Unit,
    ) {
        show(lang, kind) { CompositionLocalProvider(LocalClockTime provides { GALLERY_TIME }, content = content) }
    }

    protected fun textBounds(
        lang: AppLanguage,
        id: Int,
    ) = compose.onNodeWithText(string(lang, id), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    protected fun captureHome(lang: AppLanguage) {
        for (locale in if (cell == "gallery") listOf(lang) else listOf(lang, AppLanguage.FINNISH)) {
            showWithFixedClock(
                locale,
                CompanionKind.HUMAN,
            ) { HomeScreen(Progress(stars = List(5) { 3 }), settings.value, {}) }
            val file =
                when {
                    cell == "gallery" -> "screens--main-home-p0"
                    locale == lang -> "main--01-home"
                    else -> "main--fi--01-home"
                }
            capture(file) { frame ->
                if (cell == "gallery") {
                    assertEquals(
                        "Home background",
                        SkColors.Bg.toArgb(),
                        frame.getPixel(
                            frame.width / 100,
                            frame.height / 3,
                        ),
                    )
                }
            }
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

    protected fun captureExploreAndLessons() {
        launchCaptureHost()
        val lang = captureLanguage
        captureExplore(lang)
        show(lang, CompanionKind.HUMAN) { AskScreen(true, true, true, {}) }
        capture(if (cell == "gallery") "screens--together-1-set-p0" else "main--05-together-set")
        clickText(lang, R.string.ask_go)
        capture(if (cell == "gallery") "screens--together-2-guess-p0" else "main--06-together-guess")
        clickText(lang, R.string.ask_reveal)
        capture(if (cell == "gallery") "screens--together-3-revealed-p0" else "main--07-together-reveal")
        captureLessons(lang)
        if (cell == "gallery") captureLocalizedExplore()
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
            if (cell == "gallery") {
                captureLessonPages(lang, lesson, number, id)
            }

            val state =
                LessonUiState(
                    lesson = lesson,
                    page = lesson.pages.size,
                    clock = ClockCommand(lesson.quiz.time),
                    period = lesson.quiz.period,
                    under = lesson.quiz.under,
                    choices = lesson.quiz.choices,
                )
            lesson(lang, state)
            if (cell == "gallery") {
                val first =
                    state.choices
                        .first()
                        .text
                        .resolve(localized.getValue(lang).resources)
                measureOption(lang, "lesson-$number-$id-quiz", first)
            }
            capture(if (cell == "gallery") "screens--lesson-$number-$id-quiz-p0" else "lessons--$number-$id-quiz")
            if (index == 0) {
                captureFirstLessonQuiz(lang, state)
            }
        }
        if (cell == "gallery") {
            val completed = Progress(lessonsDone = (Lessons.levelLessons.flatten() + Lessons.DAY_LESSON).toSet())
            for ((name, progress, access) in listOf(
                Triple("lesson-path", Progress(), AccessPolicy(true)),
                Triple("lesson-path-locked", Progress(), AccessPolicy(false)),
                Triple("lesson-path-complete", completed, AccessPolicy(true)),
            )) {
                show(lang) { LessonListScreen(true, progress, access, {}, {}, {}) }
                capturePages("screens--$name")
            }
        }
    }

    private fun captureFirstLessonQuiz(
        lang: AppLanguage,
        state: LessonUiState,
    ) {
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
        val answered =
            state.copy(
                quizDone = true,
                choiceStates = mapOf(correct to OptionState.CORRECT),
                nextAction = NextAction.FINISH,
            )
        lesson(lang, answered)
        capture(
            if (cell ==
                "gallery"
            ) {
                "screens--lesson-01-viisarit-quiz-complete-p0"
            } else {
                "lessons--01-viisarit-completed"
            },
        )
        val correctDescription = string(lang, R.string.answer_correct)
        val markedCorrect = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, correctDescription)
        val correctText = state.choices[correct].text.resolve(localized.getValue(lang).resources)
        val correctOption = compose.onNodeWithText(correctText)
        // A failed save restores the choice markings from before the correct pick.
        lesson(lang, state.copy(saveFailed = true, nextAction = answered.nextAction))
        compose.onNodeWithText(string(lang, R.string.progress_save_error)).assertExists()
        correctOption.assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        capture(if (cell == "gallery") "screens--lesson-save-failure-p0" else "lessons--lesson-save-failure")
        if (cell == "gallery") {
            show(lang, CompanionKind.HUMAN) {
                LessonScreen(answered.copy(nextAction = NextAction.START_GAME), true, true, {}, {}, {}, {}, {}, {})
            }
            correctOption.assert(markedCorrect).performScrollTo().assertIsDisplayed()
            capture("screens--lesson-before-game-p0")
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
            val name = "screens--lesson-$number-$id-page-${page + 1}"
            val reduced = fi.clocklings.ui.isReduceMotion(targetContext)
            val fixtureState = mutableStateOf(state.copy(demoFinished = data.demo.isEmpty()))
            show(lang, CompanionKind.HUMAN) {
                LessonScreen(fixtureState.value, false, true, {}, {}, {}, {}, {}, {})
            }
            val clock = compose.onNodeWithTag("task_clock", useUnmergedTree = true)
            val startBounds = clock.fetchSemanticsNode()
            val startTop = startBounds.positionInRoot.y
            val startWidth = startBounds.size.width
            if (data.demo.isNotEmpty() && !reduced) capture("$name-demo-start-p0")
            compose.runOnIdle {
                val vm = LessonViewModel(listOf(id), null, galleryServices, language = lang)
                vm.setReduceMotion(true)
                repeat(page) { vm.next() }
                fixtureState.value = vm.state.value.copy(demoPlayedInstantly = reduced)
            }
            if (data.demo.isNotEmpty() && !reduced) {
                val end = clock.fetchSemanticsNode()
                val topDiff = (end.positionInRoot.y - startTop) / density
                val widthDiff = (end.size.width - startWidth) / density
                appendMeasure("$name demo topDeltaDp=$topDiff widthDeltaDp=$widthDiff")
                assertEquals("$name dial top", 0f, topDiff, 0.5f)
                assertEquals("$name dial width", 0f, widthDiff, 0.5f)
            }
            if (id == "viisi") appendMeasure("$name dialWidthDp=${clock.fetchSemanticsNode().size.width / density}")
            capturePages(name)
        }
    }

    protected fun captureCompanionsAndClocks() {
        onlyCells("gallery")
        launchCaptureHost()
        val lang = captureLanguage
        progress.value = Progress(stars = List(5) { 3 })
        for (kind in CompanionKind.entries) {
            showPicker(lang, kind)
            val name = kind.name.lowercase(Locale.ROOT)
            capture("screens--companion-picker-$name")
            if (kind != CompanionKind.NONE) {
                show(lang, kind) { WardrobeScreen(kind, progress.value, CompanionColor.ORIGINAL, {}) }
                capturePages("screens--wardrobe-$name")
            }
        }
        show(lang, CompanionKind.HUMAN) { WardrobeScreen(CompanionKind.HUMAN, Progress(), CompanionColor.ORIGINAL, {}) }
        compose.onNodeWithContentDescription(string(lang, R.string.doll_name_label)).performScrollTo().performClick()
        hideKeyboard()
        compose.onNodeWithText(string(lang, R.string.doll_name_placeholder)).assertDoesNotExist()
        capture("screens--wardrobe-name-entry")

        for ((case, full, option) in listOf(
            Triple("free", false, CompanionKind.ROBOT),
            Triple("stars", true, CompanionKind.ROBOT),
            Triple("tap", false, CompanionKind.DOG),
        )) {
            compose.runOnIdle {
                galleryPurchases.state.value = PurchaseUiState(fullAccess = full)
                progress.value = Progress()
            }
            showPicker(lang, CompanionKind.CAT)
            val thumbnail = compose.onNodeWithTag("choose_companion_${option.name}")
            thumbnail.performScrollTo().performClick()
            checkLockedPickerActions(lang, full, case)
            assertEquals(
                CompanionKind.CAT,
                runBlocking { galleryServices.repository.settings.first() }.personalization.companion,
            )
            capturePages("screens--companion-picker-locked-$case")
        }
        show(lang, CompanionKind.CAT) { WardrobeScreen(CompanionKind.CAT, Progress(), CompanionColor.ORIGINAL, {}) }
        capturePages("screens--wardrobe-free")

        compose.runOnIdle { galleryPurchases.state.value = PurchaseUiState(fullAccess = true) }
        showPicker(lang, CompanionKind.CAT)
        clickText(lang, R.string.clock_styles_title)
        capturePages("screens--clock-picker")
        for (theme in ClockTheme.entries) {
            val thumbnail = compose.onNodeWithTag("choose_clock_${theme.name}")
            thumbnail.performScrollTo().performClick()
            scrollByPx(-100_000f)
            if (theme == ClockTheme.CUSTOM) {
                capturePages("screens--clock-customize")
            } else {
                capture("screens--clock-style-${theme.name.lowercase(Locale.ROOT)}-p0")
            }
        }
    }

    private fun checkLockedPickerActions(
        lang: AppLanguage,
        full: Boolean,
        case: String,
    ) {
        val requirementId = if (full) R.string.companion_star_requirement else R.string.companion_access_requirement
        val requirement = compose.onNodeWithText(string(lang, requirementId)).assertIsDisplayed()
        val bottom = scrollNode().fetchSemanticsNode().boundsInRoot.bottom
        val parent = compose.onNode(hasText(string(lang, R.string.parent_title)) and hasClickAction())
        if (full) parent.assertDoesNotExist() else parent.assertIsDisplayed()
        val actions = if (full) listOf(requirement) else listOf(requirement, parent)
        val clearance =
            actions.minOf {
                val node = it.fetchSemanticsNode()
                (bottom - node.positionInRoot.y - node.size.height) / density - SkDimens.ScrollFade.value
            }
        appendMeasure("companion-picker-locked-$case actionClearanceDp=$clearance")
    }

    private fun showPicker(
        lang: AppLanguage,
        saved: CompanionKind,
    ) {
        runBlocking {
            galleryServices.repository.updateSettings {
                it.copy(
                    personalization = it.personalization.copy(companion = saved),
                )
            }
        }
        show(lang, saved) {
            val stored by LocalAppViewModel.current.settings.collectAsStateWithLifecycle()
            stored?.let { AppearanceScreen(it.personalization, progress.value, {}, {}) }
        }
    }

    protected fun hideKeyboard() {
        requireNotNull(scenario).onActivity { activity ->
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
    }

    protected fun measureOption(
        lang: AppLanguage,
        label: String,
        text: String,
    ) {
        val option = compose.onNode(hasText(text) and hasClickAction()).fetchSemanticsNode()
        val bottom = scrollNode().fetchSemanticsNode().boundsInRoot.bottom
        val topClearance = (bottom - option.positionInRoot.y) / density - SkDimens.ScrollFade.value
        val bottomClearance = topClearance - option.size.height / density
        appendMeasure("$label ${lang.tag} topClearanceDp=$topClearance bottomClearanceDp=$bottomClearance")
    }

    protected fun captureStartupScreens() {
        onlyCells("gallery")
        launchCaptureHost()
        val lang = captureLanguage
        show(lang) { LoadingScreen() }
        capture("startup--04-loading")
        scenario?.close()
        scenario = null

        val app = targetContext.applicationContext as ClocklingsTestApplication
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        var activity: MainActivity? = null
        var splashFailure: IllegalStateException? = null
        app.galleryStore.readsReady.value = false
        try {
            val options =
                if (Build.VERSION.SDK_INT >= 33) {
                    ActivityOptions.makeBasic().setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON).toBundle()
                } else {
                    null
                }
            targetContext.startActivity(
                Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                options,
            )
            activity =
                checkNotNull(instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as? MainActivity) {
                    "MainActivity did not resume behind the Android splash"
                }
            captureAndroidSplash()
        } catch (error: IllegalStateException) {
            splashFailure = error
        } finally {
            instrumentation.removeMonitor(monitor)
            instrumentation.runOnMainSync { activity?.finish() }
            app.galleryStore.readsReady.value = true
        }
        launchCaptureHost()
        compose.mainClock.autoAdvance = false
        try {
            val greeting = string(lang, greetingText(timeLanguage(lang).greeting(GALLERY_TIME)))
            val name = string(lang, R.string.app_name)
            val intro = localized.getValue(lang).getString(R.string.intro_description, name, greeting)
            showWithFixedClock(lang) { IntroOverlay({}, {}) }
            compose.mainClock.advanceTimeByFrame()
            advanceFrames(IntroTimeline.GREETING_START_MS + IntroTimeline.GREETING_MS)
            compose.onNodeWithContentDescription(intro).assertIsDisplayed()
            shot("startup--02-intro")
        } finally {
            compose.runOnUiThread { screen.value = {} }
            compose.mainClock.autoAdvance = true
        }
        showWithFixedClock(lang) { ClocklingsApp(container = galleryServices, app = LocalAppViewModel.current) }
        compose.waitForIdle()
        capture("startup--03-live-home")
        splashFailure?.let { throw it }
    }

    private fun captureAndroidSplash() {
        val keyguard = checkNotNull(targetContext.getSystemService(KeyguardManager::class.java))
        val expected = targetContext.getColor(R.color.window_background)
        val nightMode = targetContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val night = nightMode == Configuration.UI_MODE_NIGHT_YES
        repeat(20) { attempt ->
            if (!keyguard.isKeyguardLocked && (!night || hasAndroidSplashWindow())) {
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                if (bitmap != null) {
                    try {
                        val y = bitmap.height / 4
                        val background = bitmap.getPixel(bitmap.width / 4, y)
                        val ready = !keyguard.isKeyguardLocked && (night || background == expected)
                        if (ready && matchesSplash(bitmap, y, background)) {
                            save("startup--01-android-splash", bitmap)
                            val hex = Integer.toHexString(background).takeLast(6).uppercase(Locale.ROOT)
                            appendMeasure("android-splash background=#$hex attempts=${attempt + 1}")
                            return
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            Thread.sleep(150)
        }
        error("The Android splash was not visible on the unlocked device")
    }

    private fun hasAndroidSplashWindow(): Boolean {
        val windows = instrumentation.uiAutomation.executeShellCommand("dumpsys window windows")
        return ParcelFileDescriptor.AutoCloseInputStream(windows).bufferedReader().useLines { lines ->
            lines.any { "Splash Screen ${targetContext.packageName}}" in it }
        }
    }

    protected fun captureReducedLessonAndReplay() {
        onlyCells("gallery-reduced", "gallery-replay")
        launchCaptureHost()
        val lang = AppLanguage.FINNISH
        val res = localized.getValue(lang).resources
        val reduced = cell == "gallery-reduced"
        assertEquals(reduced, fi.clocklings.ui.isReduceMotion(targetContext))
        val vm =
            compose.runOnUiThread {
                LessonViewModel(
                    listOf("kierros"),
                    null,
                    galleryServices,
                    language = lang,
                ).apply { setReduceMotion(reduced) }
            }
        show(lang, CompanionKind.HUMAN) {
            val state by vm.state.collectAsStateWithLifecycle()
            LessonScreen(state, false, true, {}, {}, {}, vm::replay, {}, {})
        }
        if (!reduced) {
            compose.onNodeWithContentDescription(string(lang, R.string.lesson_replay)).performClick()
        }
        val state = vm.state.value
        check(state.demoFinished && state.demoPlayedInstantly)
        val page = state.lesson.pages.first()
        page.text.forEach { compose.onNodeWithText(it.resolve(res)).assertIsDisplayed() }
        compose.onNodeWithText(requireNotNull(state.caption).resolve(res)).assertIsDisplayed()
        capture(if (reduced) "lesson-reduced-motion" else "lesson-replay")
        if (reduced) {
            compose.runOnIdle { galleryPurchases.state.value = PurchaseUiState(fullAccess = false) }
            showPicker(lang, CompanionKind.CAT)
            compose.onNodeWithTag("choose_companion_DOG").performScrollTo().performClick()
            checkLockedPickerActions(lang, full = false, case = "reduced")
            capturePages("screens--companion-picker-locked-reduced")
        }
    }

    protected val captureLanguage: AppLanguage
        get() = if (cell == "gallery") AppLanguage.FINNISH else AppLanguage.ENGLISH

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

    private companion object {
        const val GALLERY_TIME = 930

        val homeTitleIds = listOf(R.string.menu_koulu, R.string.menu_vapaa, R.string.menu_pelaa, R.string.menu_kysy)

        val ExploreTimes =
            listOf(
                0 to "03-explore-24h-0000",
                390 to "03-explore-24h-0630",
                930 to "03-explore-24h-1530",
                1095 to "03-explore-24h",
                1140 to "03-explore-24h-1900",
                1425 to "03-explore-24h-2345",
            )
    }
}

private fun matchesSplash(
    bitmap: Bitmap,
    top: Int,
    background: Int,
): Boolean =
    bitmap.getPixel(3 * bitmap.width / 4, top) == background &&
        bitmap.getPixel(bitmap.width / 4, 3 * top) == background &&
        splashEdgesMatch(bitmap, top, background) &&
        hasSplashIcon(bitmap, background)

private fun splashEdgesMatch(
    bitmap: Bitmap,
    top: Int,
    background: Int,
): Boolean =
    listOf(top, bitmap.height / 2, 3 * top).all { row ->
        bitmap.getPixel(2, row) == background && bitmap.getPixel(bitmap.width - 3, row) == background
    }

private fun hasSplashIcon(
    bitmap: Bitmap,
    background: Int,
): Boolean =
    (2 * bitmap.width / 5..3 * bitmap.width / 5 step bitmap.width / 48).any { x ->
        (2 * bitmap.height / 5..3 * bitmap.height / 5 step bitmap.width / 48).any { y ->
            bitmap.getPixel(x, y) != background
        }
    }

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

internal class GalleryPreferences : DataStore<Preferences> {
    val readsReady = MutableStateFlow(true)
    private val preferences = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data =
        flow {
            readsReady.first { it }
            emitAll(preferences)
        }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        mutex.withLock { transform(preferences.value).also { preferences.value = it } }
}
