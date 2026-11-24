package fi.clocklings.ui.game

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.TaskType
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockFit
import fi.clocklings.ui.components.ClockTaskLayout
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.ConfettiState
import fi.clocklings.ui.components.DigitalPill
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.FeedbackKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.GuidesToggle
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.ProgressSegments
import fi.clocklings.ui.components.ReadOptions
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TaskActionFocus
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.bringFeedbackIntoView
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.levelName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.praise
import fi.clocklings.ui.resources
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.text
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel

@Composable
fun GameRoute(
    key: Screen.Game,
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    onFinish: (Screen.Result) -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: GameViewModel =
        viewModel(
            factory = viewModelFactory { initializer { GameViewModel(key.level, h24, app.services) } },
        ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(state.finished) { state.finished?.let(currentOnFinish) }
    GameScreen(
        state = state,
        help = help,
        onToggleHelp = app::toggleHelp,
        onBack = onBack,
        onPick = { value ->
            val before = vm.state.value
            vm.pick(value)
            val after = vm.state.value
            if (after !==
                before
            ) {
                haptic.performHapticFeedback(
                    if (after.celebrate >
                        before.celebrate
                    ) {
                        HapticFeedbackType.Confirm
                    } else {
                        HapticFeedbackType.Reject
                    },
                )
            }
        },
        onClockChange = vm::onClockChange,
        onDone = {
            val before = vm.state.value
            vm.done()
            val after = vm.state.value
            if (after !== before) {
                haptic.performHapticFeedback(
                    if (after.celebrate > before.celebrate) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                )
            }
        },
        onNext = vm::next,
    )
}

@Composable
fun GameScreen(
    state: GameUiState,
    help: Boolean,
    onToggleHelp: () -> Unit,
    onBack: () -> Unit,
    onPick: (Int) -> Unit,
    onClockChange: (Int) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Edistymispallojen keskipisteet: onnistumisen jälkeen kultainen tähti lentää nykyiseen palloon.
    val dots = remember { mutableStateMapOf<Int, Offset>() }
    val anchors =
        rememberCelebrationAnchors(state.celebrate, SUCCESS_CONFETTI, flyTo = { dots[state.taskIndex] }) {
            if (state.task.type == TaskType.READ) state.task.time else DONE_ANCHOR
        }
    // Leftover confetti and sparkles must not carry over onto the next question.
    val confetti = LocalConfetti.current
    LaunchedEffect(state.taskIndex) { confetti?.clear() }
    val currentDone = state.feedback is GameFeedback.Praise
    val guides: @Composable () -> Unit = { GuidesToggle(help, onToggleHelp) }
    val scroll = rememberScrollState()
    val compactScroll = rememberScrollState()
    ClockTaskScaffold(
        modifier = modifier.testTag("game_task_${state.taskIndex}"),
        clockHeightShare = SkDimens.GAME_CLOCK_HEIGHT_SHARE,
        clockFit = if (state.feedback != null || state.task.type == TaskType.READ) ClockFit.FIT else ClockFit.NONE,
        clockTaskKey = state.taskIndex,
        scrollState = scroll,
        compactScrollState = compactScroll,
        section = Section.PLAY,
        topBar = { compactTitle ->
            TopBar(
                title = stringResource(levelName(state.level)),
                onBack = onBack,
                compactTaskTitle = compactTitle,
                trailing = guides.takeUnless { compactTitle },
            )
        },
        status = {
            ProgressSegments(
                state.taskCount,
                state.taskIndex,
                track = Section.PLAY.track,
                currentDone = currentDone,
                fillDelayMs = ConfettiState.FLIGHT_MS,
                onSegmentCenter = { i, c -> dots[i] = c },
                outcomes = dotOutcomes(state, currentDone),
            )
        },
        largeTextStatusAction = guides,
        visual = { GameClock(state, help, onClockChange, it) },
        actions =
            if (state.showDone || state.showNext) {
                { focus, _ -> GameTaskAction(state, anchors, onDone, onNext, focus) }
            } else {
                null
            },
    ) { layout ->
        val answerScroll = if (layout == ClockTaskLayout.COMPACT) compactScroll else scroll
        GameAnswerPanel(state, anchors, onPick, answerScroll, layout == ClockTaskLayout.TALL)
    }
}

@Composable
private fun GameTaskAction(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onDone: () -> Unit,
    onNext: () -> Unit,
    focus: TaskActionFocus,
) {
    val label =
        when {
            state.showDone -> R.string.check_answer
            state.feedback == GameFeedback.SaveFailed -> R.string.settings_retry
            else -> R.string.next
        }
    val button = Modifier.fillMaxWidth().taskActionFocus(focus)
    SkButton(
        stringResource(label),
        if (state.showDone) onDone else onNext,
        kind = ButtonKind.PRIMARY,
        modifier = if (state.showDone) button.confettiAnchor { anchors[DONE_ANCHOR] = it } else button,
    )
}

@Composable
private fun GameAnswerPanel(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
    scroll: ScrollState,
    tall: Boolean,
) {
    val read = state.task.type == TaskType.READ
    val feedback = state.feedback
    val hintInQuestion = tall && feedback != GameFeedback.SaveFailed
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        FlatPanel(padding = if (maxWidth < SkDimens.GridBreakpoint) SkDimens.Gutter else SkDimens.PanelPadding) {
            if (read) {
                GameOptions(state, anchors, onPick, hintInQuestion, scroll)
            } else {
                SetTarget(state.task.time, state.h24)
            }
            if (!read || !hintInQuestion) {
                GameFeedbackMessage(
                    feedback,
                    state.h24,
                    scroll,
                    scrollToMessage = !read || feedback is GameFeedback.HintText || feedback == GameFeedback.SaveFailed,
                )
            }
        }
    }
}

/** Paljastuksessa aika pienellä alkukirjaimella keskellä lausetta, kuten webissä. */
@Composable
private fun timeText(
    t: Int,
    h24: Boolean,
) = timeLanguage(LocalAppLanguage.current).fullWords(t, h24)

private const val DONE_ANCHOR = -1
private const val SUCCESS_CONFETTI = 45

/**
 * The recorded outcomes plus, during the success pause, the task just solved, so its dot pops in
 * the colour it keeps. The ViewModel records that outcome only when it advances; after that (the
 * last task while the result is saved) it is already in the list and is not added twice.
 */
internal fun dotOutcomes(
    state: GameUiState,
    currentDone: Boolean,
): List<DotOutcome> =
    if (currentDone && state.outcomes.size <= state.taskIndex) {
        state.outcomes + outcomeOf(state.taskMistakes)
    } else {
        state.outcomes
    }

@Composable
private fun GameFeedbackMessage(
    feedback: GameFeedback?,
    h24: Boolean,
    scroll: ScrollState,
    scrollToMessage: Boolean = true,
) {
    val res = resources()
    feedback?.let { fb ->
        val (text, kind) =
            when (fb) {
                GameFeedback.SaveFailed -> {
                    stringResource(R.string.progress_save_error) to FeedbackKind.HINT
                }

                is GameFeedback.Praise -> {
                    val p = praise(res, fb.praiseIndex)
                    (
                        if (fb.time ==
                            null
                        ) {
                            p
                        } else {
                            res.getString(
                                R.string.praise_with_time,
                                p,
                                timeLabel(LocalAppLanguage.current, fb.time, h24),
                            )
                        }
                    ) to
                        FeedbackKind.OK
                }

                is GameFeedback.HintText -> {
                    fb.hint.text(res) to FeedbackKind.HINT
                }

                is GameFeedback.RevealRead -> {
                    res.getString(R.string.reveal_read, timeText(fb.time, h24)) to FeedbackKind.HINT
                }

                is GameFeedback.RevealSet -> {
                    res.getString(R.string.reveal_set, timeText(fb.time, h24)) to FeedbackKind.HINT
                }
            }
        CompanionTip(
            text,
            modifier = if (scrollToMessage) Modifier.bringFeedbackIntoView(text, scroll) else Modifier,
            kind = kind,
            celebrate = feedback is GameFeedback.Praise,
            color = if (kind == FeedbackKind.HINT) SkColors.HintFill else null,
            avatarWidth = SkDimens.FeedbackAvatar,
        )
    }
}

@Composable
private fun ColumnScope.GameOptions(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
    hintInQuestion: Boolean,
    scroll: ScrollState,
) {
    val feedback = state.feedback
    val promptFeedback = feedback.takeIf { hintInQuestion }
    val correctTime =
        when (feedback) {
            is GameFeedback.RevealRead -> feedback.time
            is GameFeedback.Praise -> feedback.time
            else -> null
        }
    ReadOptions(
        promptGap = if (promptFeedback == null) SkDimens.GapM else SkDimens.GapS,
        prompt = {
            if (promptFeedback == null) {
                Text(stringResource(R.string.question_read), style = SkType.Question)
            } else {
                GameFeedbackMessage(promptFeedback, state.h24, scroll, scrollToMessage = correctTime == null)
            }
        },
    ) {
        for (t in state.options) {
            val anchor = Modifier.confettiAnchor { anchors[t] = it }
            val modifier =
                if (t ==
                    correctTime
                ) {
                    anchor.bringFeedbackIntoView("${state.taskIndex}:$feedback", scroll)
                } else {
                    anchor
                }
            OptionButton(
                words = timeLabel(LocalAppLanguage.current, t, state.h24),
                digital = timeLanguage(LocalAppLanguage.current).digital(t, state.h24),
                state = state.optionStates[t] ?: OptionState.NORMAL,
                enabled = !state.busy && state.optionStates[t] != OptionState.WRONG,
                onClick = { onPick(t) },
                modifier = modifier,
            )
        }
    }
}

/** Laita kello -tehtävän tavoite: aika sanoina ja numeroina. */
@Composable
private fun SetTarget(
    time: Int,
    h24: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        Text(stringResource(R.string.question_set), style = SkType.Question)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            Text(timeLabel(LocalAppLanguage.current, time, h24), style = SkType.Words)
            DigitalPill(digitalLabel(resources(), time, h24))
        }
    }
}

@Composable
private fun GameClock(
    state: GameUiState,
    help: Boolean,
    onClockChange: (Int) -> Unit,
    clockSize: Dp,
) {
    // Restore the latest answer on recreation without reapplying each drag update to the live clock.
    val command = remember(state.clock.id) { state.clock }
    ClockFace(
        command = command,
        modifier = Modifier.sharedClocklingsElement("level-clock-${state.level}"),
        period = ClockTime.period(state.h24),
        step = state.step,
        interactive = state.interactive,
        help = help,
        describeTime =
            state.task.type != TaskType.READ || state.feedback is GameFeedback.RevealRead ||
                state.feedback is GameFeedback.Praise,
        onChange = onClockChange,
        clockSize = clockSize,
    )
}
