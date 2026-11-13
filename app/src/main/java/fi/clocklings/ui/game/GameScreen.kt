package fi.clocklings.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import fi.clocklings.ui.components.AdaptiveContent
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.ConfettiState
import fi.clocklings.ui.components.DigitalPill
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.FeedbackKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.HelpToggle
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.ProgressDots
import fi.clocklings.ui.components.ReadOptions
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.levelName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.praise
import fi.clocklings.ui.resources
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.text
import fi.clocklings.ui.theme.Section
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
    ScreenScaffold(
        modifier.testTag("game_task_${state.taskIndex}"),
        wideContent = true,
        centerContent = true,
        clockHeightShare = SkDimens.GAME_CLOCK_HEIGHT_SHARE,
        section = Section.PLAY,
        topBar = {
            TopBar(
                title = stringResource(levelName(state.level)),
                onBack = onBack,
                trailing = { HelpToggle(help, onToggleHelp) },
            )
        },
    ) { clockSize ->
        val currentDone = state.feedback is GameFeedback.Praise
        ProgressDots(
            state.taskCount,
            state.taskIndex,
            currentDone = currentDone,
            fillDelayMs = ConfettiState.FLIGHT_MS,
            onDotCenter = { i, c -> dots[i] = c },
            outcomes = dotOutcomes(state, currentDone),
        )
        AdaptiveContent(clockSize, controlsAlignment = Alignment.CenterVertically, visual = { size ->
            GameClock(state, help, onClockChange, size)
        }) {
            GameAnswerPanel(state, anchors, onPick, onDone, onNext)
        }
    }
}

@Composable
private fun GameAnswerPanel(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
    onDone: () -> Unit,
    onNext: () -> Unit,
) {
    FlatPanel {
        if (state.task.type == TaskType.READ) {
            GameOptions(state, anchors, onPick)
        } else {
            SetTarget(state.task.time, state.h24)
        }
        if (state.showDone) {
            SkButton(
                stringResource(R.string.done),
                onDone,
                kind = ButtonKind.PRIMARY,
                modifier = Modifier.fillMaxWidth().confettiAnchor { anchors[DONE_ANCHOR] = it },
            )
        }
        GameFeedbackMessage(state.feedback, state.h24)
        if (state.showNext) {
            val label =
                if (state.feedback ==
                    GameFeedback.SaveFailed
                ) {
                    R.string.settings_retry
                } else {
                    R.string.next
                }
            SkButton(
                stringResource(label),
                onNext,
                kind = ButtonKind.GREEN,
                modifier = Modifier.fillMaxWidth(),
            )
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
) {
    val res = resources()
    feedback?.let { fb ->
        val (text, kind) =
            when (fb) {
                GameFeedback.SaveFailed -> {
                    stringResource(R.string.settings_save_error) to FeedbackKind.HINT
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
        CompanionTip(text, kind = kind, celebrate = feedback is GameFeedback.Praise)
    }
}

@Composable
private fun ColumnScope.GameOptions(
    state: GameUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
) {
    ReadOptions {
        for (t in state.options) {
            OptionButton(
                words = timeLabel(LocalAppLanguage.current, t, state.h24),
                digital = timeLanguage(LocalAppLanguage.current).digital(t, state.h24),
                state = state.optionStates[t] ?: OptionState.NORMAL,
                enabled = !state.busy && state.optionStates[t] != OptionState.WRONG,
                onClick = { onPick(t) },
                modifier = Modifier.confettiAnchor { anchors[t] = it },
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
    ClockBox(clockSize) {
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
        )
    }
}
