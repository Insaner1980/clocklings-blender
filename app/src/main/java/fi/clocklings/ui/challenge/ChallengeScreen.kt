package fi.clocklings.ui.challenge

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ActionPair
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.ClockFit
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.ReadOptions
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TaskActionFocus
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel

@Composable
fun ChallengeRoute(
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: ChallengeViewModel =
        viewModel(factory = viewModelFactory { initializer { ChallengeViewModel(h24, app.services) } }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    ChallengeScreen(
        state = state,
        h24 = h24,
        help = help,
        onBack = onBack,
        onStart = {
            app.play(Sound.POP)
            if (vm.state.value.saveFailed) vm.retrySave() else vm.start()
        },
        onPick = { value ->
            val before = vm.state.value
            vm.pick(value)
            val after = vm.state.value
            if (after !==
                before
            ) {
                haptic.performHapticFeedback(
                    if (after.score >
                        before.score
                    ) {
                        HapticFeedbackType.Confirm
                    } else {
                        HapticFeedbackType.Reject
                    },
                )
            }
        },
    )
}

@Composable
fun ChallengeScreen(
    state: ChallengeUiState,
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val finalAnchor = remember { mutableStateOf(Offset.Zero) }
    val barAnchor = remember { mutableStateOf(Offset.Zero) }
    RecordCelebrationEffect(state) {
        // A fully clipped score has empty bounds; celebrate from the visible result bar instead.
        finalAnchor.value.takeUnless { it == Offset.Zero } ?: barAnchor.value
    }
    ClockTaskScaffold(
        modifier = modifier,
        clockHeightShare = SkDimens.PLAY_CLOCK_HEIGHT_SHARE,
        section = Section.PLAY,
        topBar = {
            TopBar(
                title = stringResource(R.string.challenge_title),
                onBack = onBack,
                trailing = { Pill(stringResource(R.string.challenge_score, state.score)) },
            )
        },
        // The start action and the result actions stay reachable below a narrow or large-font intro or result
        // without scrolling.
        actions =
            if (state.phase == ChallengePhase.RUN) {
                null
            } else {
                { focus, _ ->
                    Box(Modifier.fillMaxWidth().confettiAnchor { barAnchor.value = it }) {
                        ChallengeActions(state, onStart, onBack, focus)
                    }
                }
            },
        // A run shows all its answers without scrolling where the clock can shrink for them, and the result keeps
        // that size whatever the height its bar takes, so the score and its confetti stay put. The clock resizes
        // into the height the START bar frees instead of jumping.
        clockFit =
            when (state.phase) {
                ChallengePhase.START -> ClockFit.NONE
                ChallengePhase.RUN -> ClockFit.FIT
                ChallengePhase.END -> ClockFit.KEEP
            },
        animateClock = true,
        // Each phase opens at the top, so a run never starts with the clock scrolled away.
        scrollState = rememberSaveable(state.phase, saver = ScrollState.Saver) { ScrollState(0) },
        compactScrollState = rememberSaveable(state.phase, saver = ScrollState.Saver) { ScrollState(0) },
        status = { TimerBar(state) },
        visual = { size -> ChallengeClock(state, h24, help, size) },
    ) { compact ->
        ChallengePanel(
            state,
            h24,
            onPick,
            onAnchorChange = { finalAnchor.value = it },
            compact = compact,
        )
    }
}

/** Whether a run had to measure its answer room itself, as it started before START had prepared it. */
private class MeasuredInRun {
    var rows = false
}

private enum class RunSlot { SHOWN, ROOM }

/**
 * The answers of a run, kept as tall as the tallest answers any question can show: as many of the tallest of
 * all challenge answers as there are answers (see [AnswerRooms]). The panel then keeps one height for the whole
 * run, so the clock fitted above it never resizes and no answer moves between questions.
 */
@Composable
private fun RunOptions(
    state: ChallengeUiState,
    h24: Boolean,
    onPick: (Int) -> Unit,
) {
    val keyFor = rememberAnswerRoomKey(h24)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val measuredInRun = remember { MeasuredInRun() }
    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minHeight = 0)
        val key = keyFor(constraints.maxWidth)
        var room = AnswerRooms.heights(key)
        // Started before START prepared the room, or opened straight into a run: rank by text now the answers START
        // has not ranked yet, compose only the candidates, and keep them until the run ends instead of dropping
        // them on the first pick.
        if (room == null || measuredInRun.rows) {
            val candidates = AnswerRooms.candidates(key) ?: AnswerRooms.rankNow(key, measurer, density)
            val rows = measureAnswerRows(RunSlot.ROOM, candidates, h24, constraints)
            if (room == null) AnswerRooms.storeHeights(key, rows)
            room = rows.sortedDescending()
            measuredInRun.rows = true
        }
        val tallest = room.take(state.options.size)
        val column =
            subcompose(RunSlot.SHOWN) {
                Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
                    for (t in state.options) {
                        RunOption(
                            t,
                            h24,
                            state.optionStates[t] ?: OptionState.NORMAL,
                            onClick = { onPick(t) },
                            enabled = !state.busy,
                        )
                    }
                }
            }.first().measure(loose)
        val reserved = tallest.sum() + SkDimens.GapOptions.roundToPx() * (tallest.size - 1).coerceAtLeast(0)
        layout(column.width, maxOf(column.height, reserved)) { column.place(0, 0) }
    }
}

@Composable
internal fun RunOption(
    t: Int,
    h24: Boolean,
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CountAnswerRow()
    val language = LocalAppLanguage.current
    OptionButton(
        words = timeLabel(language, t, h24),
        onClick = onClick,
        modifier = modifier,
        digital = timeLanguage(language).digital(t, h24),
        state = state,
        enabled = enabled,
    )
}

@Composable
private fun RecordCelebrationEffect(
    state: ChallengeUiState,
    anchor: () -> Offset,
) {
    val confetti = LocalConfetti.current
    val currentAnchor by rememberUpdatedState(anchor)
    var celebratedRun by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(state.phase, state.newRecord, state.runId) {
        if (state.phase == ChallengePhase.END && state.newRecord && celebratedRun != state.runId) {
            celebratedRun = state.runId
            confetti?.burst(currentAnchor(), RECORD_CONFETTI)
        }
    }
}

/** Sateenkaaren väreillä täyttyvä ajastinpalkki, joka tyhjenee minuutissa (web: .timer). */
@Composable
private fun TimerBar(state: ChallengeUiState) {
    val label = stringResource(R.string.challenge_time_remaining)
    // Ajastin on tietoa eikä koristetta, joten se lasketaan ruutujen ajasta (ei animaatioasetuksen mukaan).
    var fraction by remember(
        state.runId,
        state.phase,
    ) { mutableFloatStateOf(state.remainingFraction(System.nanoTime())) }
    LaunchedEffect(state.runId, state.phase) {
        if (state.phase != ChallengePhase.RUN) return@LaunchedEffect
        while (fraction > 0f) {
            withFrameNanos { fraction = state.remainingFraction(System.nanoTime()) }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(SkDimens.TimerHeight)
            .clip(SkShapes.Timer)
            .background(SkColors.DotEmpty)
            .clearAndSetSemantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(SkColors.Rainbow7)),
        )
    }
}

private const val RECORD_CONFETTI = 120

@Composable
private fun ColumnScope.ChallengeResult(
    state: ChallengeUiState,
    onAnchorChange: (Offset) -> Unit,
) {
    Eyebrow(stringResource(if (state.newRecord) R.string.challenge_new_best else R.string.challenge_over))
    Text(
        state.score.toString(),
        style = SkType.BigNumber,
        modifier = Modifier.confettiAnchor(onAnchorChange),
    )
    Text(
        if (state.newRecord) {
            stringResource(R.string.challenge_new_record, state.score)
        } else {
            stringResource(R.string.challenge_result, state.score, state.best)
        },
        style = SkType.ResultText,
    )
    if (state.saveFailed) Text(stringResource(R.string.settings_save_error), style = SkType.Body)
}

/** Start, or Back and Play again (Retry after a failed save), in the current layout's action area. */
@Composable
private fun ChallengeActions(
    state: ChallengeUiState,
    onStart: () -> Unit,
    onBack: () -> Unit,
    focus: TaskActionFocus,
) {
    if (state.phase == ChallengePhase.START) {
        SkButton(
            stringResource(R.string.challenge_go),
            onStart,
            Modifier.fillMaxWidth().taskActionFocus(focus),
            kind = ButtonKind.PRIMARY,
        )
        return
    }
    val back = stringResource(R.string.back_description)
    val again = stringResource(if (state.saveFailed) R.string.settings_retry else R.string.play_again)
    ActionPair(
        back,
        again,
        start = { SkButton(back, onBack, it.taskActionFocus(focus)) },
        end = {
            SkButton(
                again,
                onStart,
                it.taskActionFocus(focus),
                kind = ButtonKind.PRIMARY,
                enabled = !state.savePending,
            )
        },
    )
}

@Composable
private fun ChallengePanel(
    state: ChallengeUiState,
    h24: Boolean,
    onPick: (Int) -> Unit,
    onAnchorChange: (Offset) -> Unit,
    compact: Boolean,
) {
    FlatPanel(padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding) {
        when (state.phase) {
            ChallengePhase.START -> {
                Text(stringResource(R.string.challenge_desc), style = SkType.Question)
                Text(stringResource(R.string.challenge_intro), style = SkType.Body)
                val best = stringResource(R.string.challenge_best, state.best)
                // The panel is as wide as a run's answers, so their room is prepared here, before Start.
                Box(Modifier.fillMaxWidth()) {
                    Eyebrow(if (state.best > 0) best else stringResource(R.string.challenge_no_best))
                    AnswerRoomPreparation(h24, Modifier.matchParentSize())
                }
            }

            ChallengePhase.RUN -> {
                ReadOptions { RunOptions(state, h24, onPick) }
            }

            ChallengePhase.END -> {
                ChallengeResult(state, onAnchorChange = onAnchorChange)
            }
        }
    }
}

@Composable
private fun ChallengeClock(
    state: ChallengeUiState,
    h24: Boolean,
    help: Boolean,
    clockSize: Dp,
) {
    ClockBox(clockSize) {
        ClockFace(
            command = state.clock,
            period = ClockTime.period(h24),
            help = help,
            describeTime = state.phase != ChallengePhase.RUN || state.busy,
            modifier =
                Modifier.testTag("challenge_clock").then(
                    if (state.phase == ChallengePhase.START) {
                        Modifier.alpha(SkDimens.INACTIVE_CLOCK_ALPHA)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}
