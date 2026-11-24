package fi.clocklings.ui.lesson

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.audio.Sound
import fi.clocklings.domain.LessonPage
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ActionPair
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockFit
import fi.clocklings.ui.components.ClockTaskLayout
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.FeedbackKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.ProgressSegments
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TaskActionFocus
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.components.bringFeedbackIntoView
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.praise
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.resolve
import fi.clocklings.ui.resources
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun LessonRoute(
    key: Screen.Lesson,
    speechAvailable: Boolean,
    onLeave: () -> Unit,
    onStartGame: (Int) -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: LessonViewModel =
        viewModel(
            factory = viewModelFactory { initializer { LessonViewModel(key.queue, key.thenLevel, app.services) } },
        ),
) {
    val language = LocalAppLanguage.current
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(language, reduceMotion) {
        vm.setReduceMotion(reduceMotion)
        vm.setLanguage(language)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val leave = {
        if (key.thenLevel != null) onStartGame(key.thenLevel) else onLeave()
    }
    LaunchedEffect(state.finished) { if (state.finished) leave() }
    LessonScreen(
        state = state,
        skipToGame = key.thenLevel != null,
        speechAvailable = speechAvailable,
        onBack = {
            app.play(Sound.POP)
            leave()
        },
        onNext = vm::next,
        onRetrySave = vm::retrySave,
        onPrevious = vm::previous,
        onReplay = vm::replay,
        onPick = { value ->
            val before = vm.state.value
            vm.pick(value)
            val after = vm.state.value
            if (after !==
                before
            ) {
                haptic.performHapticFeedback(
                    if (after.quizDone) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                )
            }
        },
        onSpeak = app::speak,
    )
}

@Composable
fun LessonScreen(
    state: LessonUiState,
    skipToGame: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onPick: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRetrySave: () -> Unit = {},
) {
    val res = resources()
    val anchors =
        rememberCelebrationAnchors(state.celebrate, QUIZ_CONFETTI) {
            state.choiceStates.entries
                .firstOrNull { it.value == OptionState.CORRECT }
                ?.key
        }
    val page = state.lesson.pages.getOrNull(state.page)
    val paragraphs = state.visibleParagraphs.map { it.resolve(res) }
    val caption = state.caption?.resolve(res).orEmpty()
    val playback: @Composable (Boolean) -> Unit = { compact ->
        LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay, compact)
    }
    val scroll = rememberSaveable(state.lesson.id, state.page, saver = ScrollState.Saver) { ScrollState(0) }
    val compactScroll = rememberSaveable(state.lesson.id, state.page, saver = ScrollState.Saver) { ScrollState(0) }
    val tallClockMin =
        when {
            state.isQuiz -> SkDimens.QuizClockMin
            state.firstFiveMinutePage -> SkDimens.FiveMinuteDemoClockMin
            else -> SkDimens.FitClockMin
        }
    ClockTaskScaffold(
        modifier = modifier,
        section = Section.LEARN,
        clockFit = ClockFit.FIT,
        clockTaskKey = state.queueIndex.takeIf { state.isQuiz },
        tallFitMin = tallClockMin,
        scrollState = scroll,
        compactScrollState = compactScroll,
        clockHeightShare = if (state.isQuiz) SkDimens.CLOCK_HEIGHT_SHARE else 1f,
        stackedClockMax = if (state.isQuiz) SkDimens.ClockMax else SkDimens.LessonPageClockMax,
        centerStacked = !state.isQuiz,
        topBar = { compactTitle ->
            TopBar(
                title = state.lesson.title.resolve(),
                backLabel = stringResource(if (skipToGame) R.string.lesson_skip else R.string.back_description),
                onBack = onBack,
                compactTaskTitle = compactTitle,
            )
        },
        actions = { focus, layout ->
            LessonActions(state, onPrevious, onNext, onRetrySave, focus) {
                if (page != null && state.playbackInActions(layout)) playback(true)
            }
        },
        status = { ProgressSegments(state.pageCount, state.page, track = Section.LEARN.track) },
        visual = { LessonClock(state, it) },
    ) { layout ->
        val panelScroll = if (layout == ClockTaskLayout.COMPACT) compactScroll else scroll
        LessonTaskPanel(state, page, paragraphs.size, caption, anchors, onPick, panelScroll, layout) { playback(false) }
    }
}

private val LessonUiState.firstFiveMinutePage: Boolean
    get() = lesson.id == "viisi" && page == 0

private fun LessonUiState.playbackInActions(layout: ClockTaskLayout): Boolean =
    layout == ClockTaskLayout.COMPACT || (firstFiveMinutePage && layout == ClockTaskLayout.TALL)

@Composable
private fun LessonTaskPanel(
    state: LessonUiState,
    page: LessonPage?,
    visibleCount: Int,
    caption: String,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
    scroll: ScrollState,
    layout: ClockTaskLayout,
    playback: @Composable () -> Unit,
) {
    val compact = layout == ClockTaskLayout.COMPACT
    FlatPanel(padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding) {
        if (page != null) {
            key(state.lesson.id, state.page) {
                val animate = !rememberReduceMotion() && !state.demoPlayedInstantly
                LessonExplanation(page, visibleCount, animate, compact)
                LessonCaption(page, caption, animate)
            }
            if (!state.playbackInActions(layout)) playback()
        } else {
            LessonQuiz(state, anchors, onPick, scroll)
        }
    }
}

@Composable
private fun LessonClock(
    state: LessonUiState,
    size: Dp,
) {
    ClockFace(
        command = state.clock,
        period = state.period,
        help = state.help,
        describeTime = !state.isQuiz || state.quizDone,
        highlight = state.highlight,
        under = state.under,
        over = state.over,
        clockSize = size,
    )
}

private fun nextLabel(action: NextAction): Int =
    when (action) {
        NextAction.NEXT_PAGE -> R.string.next
        NextAction.TRY_IT -> R.string.lesson_try
        NextAction.NEXT_LESSON -> R.string.lesson_next_lesson
        NextAction.START_GAME -> R.string.lesson_start_game
        NextAction.FINISH -> R.string.done
    }

private const val QUIZ_CONFETTI = 60

@Composable
private fun ColumnScope.LessonQuiz(
    state: LessonUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
    scroll: ScrollState,
) {
    val res = resources()
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val narrowLargeText =
        density.fontScale > SkDimens.MAX_ROW_FONT_SCALE &&
            with(density) { window.width.toDp() <= SkDimens.GridBreakpoint }
    val question =
        state.lesson.quiz.question
            .resolve()
    if (narrowLargeText) {
        WordSafeText(question, SkType.TaskTitleLarge, textAlign = TextAlign.Start, preferredMaxLines = 4)
    } else {
        Eyebrow(stringResource(R.string.lesson_try))
        Text(question, style = SkType.Question)
    }
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
        state.choices.forEachIndexed { i, choice ->
            OptionButton(
                words = choice.text.resolve(),
                state = state.choiceStates[i] ?: OptionState.NORMAL,
                enabled = !state.quizDone && state.choiceStates[i] != OptionState.WRONG,
                onClick = { onPick(i) },
                modifier = Modifier.confettiAnchor { anchors[i] = it },
            )
        }
    }
    when {
        state.saveFailed && !state.savePending && !state.showHint -> {
            CompanionTip(
                stringResource(R.string.progress_save_error),
                modifier = Modifier.bringFeedbackIntoView(R.string.progress_save_error.toString(), scroll),
                kind = FeedbackKind.HINT,
            )
        }

        state.quizDone && !state.savePending -> {
            CompanionTip(
                stringResource(R.string.lesson_learned, praise(res, state.praiseIndex), state.lesson.title.resolve()),
                modifier = Modifier.bringFeedbackIntoView(R.string.lesson_learned.toString(), scroll),
                kind = FeedbackKind.OK,
                celebrate = true,
            )
        }

        state.showHint -> {
            CompanionTip(
                state.lesson.quiz.hint
                    .resolve(),
                modifier = Modifier.bringFeedbackIntoView(state.choiceStates.size.toString(), scroll),
                kind = FeedbackKind.HINT,
                color = SkColors.HintFill,
            )
        }
    }
}

@Composable
private fun LessonPlayback(
    speechAvailable: Boolean,
    paragraphs: List<String>,
    caption: String,
    onSpeak: (String) -> Unit,
    onReplay: () -> Unit,
    compact: Boolean,
) {
    Row(
        if (compact) Modifier else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            if (speechAvailable) {
                RoundIconButton(
                    SkIcons.Listen,
                    stringResource(R.string.listen),
                    { onSpeak((paragraphs + caption).filter { it.isNotEmpty() }.joinToString(" ")) },
                    tonal = true,
                )
            }
            RoundIconButton(
                SkIcons.Replay,
                stringResource(R.string.lesson_replay),
                onReplay,
                tonal = true,
            )
        }
    }
}

@Composable
private fun LessonActions(
    state: LessonUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetrySave: () -> Unit,
    focus: TaskActionFocus,
    playback: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        playback()
        Box(Modifier.weight(1f)) { LessonNavigation(state, onPrevious, onNext, onRetrySave, focus) }
    }
}

/** Navigation stays reachable while the lesson itself scrolls. */
@Composable
private fun LessonNavigation(
    state: LessonUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetrySave: () -> Unit,
    focus: TaskActionFocus,
) {
    val retry = state.isQuiz && state.saveFailed
    val next =
        stringResource(
            when {
                retry -> R.string.settings_retry
                state.isQuiz && (!state.quizDone || state.savePending) -> R.string.done
                else -> nextLabel(state.nextAction)
            },
        )
    val nextButton: @Composable (Modifier) -> Unit = {
        SkButton(
            next,
            if (retry) onRetrySave else onNext,
            it.taskActionFocus(focus),
            enabled = !state.savePending && (!state.isQuiz || state.quizDone || retry),
            kind = ButtonKind.PRIMARY,
            trailingIcon = SkIcons.Chevron.takeIf { !state.isQuiz && state.nextAction == NextAction.NEXT_PAGE },
        )
    }
    if (state.page == 0) {
        nextButton(Modifier.fillMaxWidth())
        return
    }
    val previous = stringResource(R.string.lesson_prev)
    ActionPair(
        previous,
        next,
        start = {
            SkButton(
                previous,
                onPrevious,
                it.taskActionFocus(focus),
                enabled = !state.savePending,
            )
        },
        compactStart = {
            RoundIconButton(
                SkIcons.ChevronBack,
                previous,
                onPrevious,
                it.taskActionFocus(focus),
                enabled = !state.savePending,
            )
        },
        end = nextButton,
    )
}
