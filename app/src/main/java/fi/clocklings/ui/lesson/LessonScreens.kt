package fi.clocklings.ui.lesson

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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.audio.Sound
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ActionPair
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
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
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.praise
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
    LaunchedEffect(language) { vm.setLanguage(language) }
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
) {
    val res = resources()
    val anchors =
        rememberCelebrationAnchors(state.celebrate, QUIZ_CONFETTI) {
            state.choiceStates.entries
                .firstOrNull { it.value == OptionState.CORRECT }
                ?.key
        }
    val page = state.lesson.pages.getOrNull(state.page)
    val paragraphs = page?.text?.map { it.resolve(res) }.orEmpty()
    val caption = state.caption?.resolve(res).orEmpty()
    ClockTaskScaffold(
        modifier = modifier,
        section = Section.LEARN,
        topBar = {
            TopBar(
                title = state.lesson.title.resolve(),
                backLabel = stringResource(if (skipToGame) R.string.lesson_skip else R.string.back_description),
                onBack = onBack,
            )
        },
        actions = { focus, compact ->
            LessonActions(state, onPrevious, onNext, focus, compact) {
                if (page != null) LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay, compact)
            }
        },
        status = { ProgressSegments(state.pageCount, state.page, track = Section.LEARN.track) },
        visual = { size ->
            ClockBox(size) {
                ClockFace(
                    command = state.clock,
                    period = state.period,
                    help = state.help,
                    describeTime = !state.isQuiz || state.quizDone,
                    highlight = state.highlight,
                    under = state.under,
                    over = state.over,
                )
            }
        },
    ) { compact ->
        FlatPanel(padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding) {
            if (page != null) {
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) SkDimens.GapS else SkDimens.GapM)) {
                    paragraphs.forEach { Text(it, style = SkType.LessonText) }
                }
                if (caption.isNotEmpty()) LessonCaption(caption)
                if (!compact) LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay, compact)
            } else {
                LessonQuiz(state, anchors, onPick)
            }
        }
    }
}

private fun nextLabel(action: NextAction): Int =
    when (action) {
        NextAction.NEXT_PAGE -> R.string.next
        NextAction.TRY_IT -> R.string.lesson_try
        NextAction.NEXT_LESSON -> R.string.lesson_next_lesson
        NextAction.START_GAME -> R.string.lesson_start_game
        NextAction.FINISH -> R.string.done
    }

@Composable
private fun LessonCaption(caption: String) {
    CompanionTip(caption)
}

private const val QUIZ_CONFETTI = 60

@Composable
private fun ColumnScope.LessonQuiz(
    state: LessonUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
) {
    val res = resources()
    Eyebrow(stringResource(R.string.lesson_try))
    Text(
        state.lesson.quiz.question
            .resolve(),
        style = SkType.Question,
    )
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
        state.saveFailed && !state.showHint -> {
            CompanionTip(stringResource(R.string.settings_save_error), kind = FeedbackKind.HINT)
        }

        state.quizDone && !state.savePending -> {
            CompanionTip(
                stringResource(R.string.lesson_learned, praise(res, state.praiseIndex), state.lesson.title.resolve()),
                kind = FeedbackKind.OK,
                celebrate = true,
            )
        }

        state.showHint -> {
            CompanionTip(
                state.lesson.quiz.hint
                    .resolve(),
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
    focus: TaskActionFocus,
    compact: Boolean,
    playback: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) playback()
        Box(Modifier.weight(1f)) { LessonNavigation(state, onPrevious, onNext, focus) }
    }
}

/** Navigation stays reachable while the lesson itself scrolls. */
@Composable
private fun LessonNavigation(
    state: LessonUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    focus: TaskActionFocus,
) {
    val previous = stringResource(R.string.lesson_prev)
    val next = stringResource(nextLabel(state.nextAction))
    val showNext = !state.savePending && (!state.isQuiz || state.quizDone)
    ActionPair(
        previous,
        next.takeIf { showNext },
        start = {
            SkButton(
                previous,
                onPrevious,
                it.taskActionFocus(focus),
                enabled =
                    state.page > 0 && !state.savePending,
            )
        },
        end = {
            SkButton(
                next,
                onNext,
                it.taskActionFocus(focus),
                kind = ButtonKind.PRIMARY,
                trailingIcon =
                    SkIcons.Chevron.takeIf {
                        state.nextAction == NextAction.NEXT_PAGE ||
                            state.nextAction == NextAction.NEXT_LESSON
                    },
            )
        },
    )
}
