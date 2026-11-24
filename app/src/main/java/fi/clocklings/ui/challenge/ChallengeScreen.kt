package fi.clocklings.ui.challenge

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.stateDescription
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
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockFit
import fi.clocklings.ui.components.ClockTaskLayout
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.ReadOptions
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.bringFeedbackIntoView
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

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
    nowNanos: () -> Long = System::nanoTime,
) {
    if (state.phase == ChallengePhase.END) {
        ChallengeResultScreen(state, onStart, onBack, modifier)
        return
    }
    val running = state.phase == ChallengePhase.RUN
    val scorePill: (@Composable () -> Unit)? =
        if (running) ({ ChallengeScorePill(state.score) }) else null
    val scroll = rememberSaveable(state.phase, state.question, saver = ScrollState.Saver) { ScrollState(0) }
    val compactScroll = rememberSaveable(state.phase, state.question, saver = ScrollState.Saver) { ScrollState(0) }
    ClockTaskScaffold(
        modifier = modifier,
        clockHeightShare = SkDimens.PLAY_CLOCK_HEIGHT_SHARE,
        section = Section.PLAY,
        topBar = { compactTitle ->
            TopBar(
                title = stringResource(R.string.challenge_title),
                onBack = onBack,
                compactTaskTitle = compactTitle,
                trailing = scorePill.takeUnless { compactTitle },
            )
        },
        actions =
            if (running) {
                null
            } else {
                { focus, _ ->
                    SkButton(
                        stringResource(R.string.challenge_go),
                        onStart,
                        Modifier.fillMaxWidth().taskActionFocus(focus),
                        kind = ButtonKind.PRIMARY,
                    )
                }
            },
        clockFit = if (running) ClockFit.FIT else ClockFit.NONE,
        clockTaskKey = state.runId.takeIf { running },
        animateClock = true,
        scrollState = scroll,
        compactScrollState = compactScroll,
        status = { TimerBar(state, nowNanos) },
        largeTextStatusAction = if (running) ({ ChallengeScorePill(state.score, compact = true) }) else null,
        visual = { size -> ChallengeClock(state, h24, help, size) },
    ) { layout ->
        val compact = layout == ClockTaskLayout.COMPACT
        ChallengePanel(state, h24, onPick, compact, if (compact) compactScroll else scroll)
    }
}

@Composable
private fun ChallengeScorePill(
    score: Int,
    compact: Boolean = false,
) {
    val label = stringResource(R.string.challenge_score, score)
    if (compact) {
        Pill(
            score.toString(),
            Modifier.clearAndSetSemantics { contentDescription = label },
            icon = SkIcons.Check,
        )
    } else {
        Pill(label)
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
    scroll: ScrollState,
) {
    val keyFor = rememberAnswerRoomKey(h24)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val measuredInRun = remember { MeasuredInRun() }
    val showCorrect = OptionState.WRONG in state.optionStates.values
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
                            modifier =
                                if (showCorrect && t == state.question) {
                                    Modifier.bringFeedbackIntoView(state.question.toString(), scroll)
                                } else {
                                    Modifier
                                },
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

/** Sateenkaaren väreillä täyttyvä ajastinpalkki, joka tyhjenee minuutissa (web: .timer). */
@Composable
private fun TimerBar(
    state: ChallengeUiState,
    nowNanos: () -> Long,
) {
    val label = stringResource(R.string.challenge_time_remaining)
    val currentNowNanos by rememberUpdatedState(nowNanos)
    // Ajastin on tietoa eikä koristetta, joten se lasketaan ruutujen ajasta (ei animaatioasetuksen mukaan).
    var fraction by remember(
        state.runId,
        state.phase,
    ) { mutableFloatStateOf(state.remainingFraction(nowNanos())) }
    LaunchedEffect(state.runId, state.phase) {
        if (state.phase != ChallengePhase.RUN) return@LaunchedEffect
        while (fraction > 0f) {
            withFrameNanos { fraction = state.remainingFraction(currentNowNanos()) }
        }
    }
    val seconds = ceil(fraction * TimeUnit.MILLISECONDS.toSeconds(ChallengeViewModel.DURATION_MS)).toInt()
    val time = stringResource(R.string.challenge_seconds_left, seconds)
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = label
            if (state.phase == ChallengePhase.RUN) stateDescription = time
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        },
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(SkDimens.TimerHeight)
                .testTag("challenge_timer")
                .clip(SkShapes.Timer)
                .background(SkColors.DotEmpty),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Brush.horizontalGradient(SkColors.Rainbow7)),
            )
        }
        if (state.phase == ChallengePhase.RUN) Text(time, style = SkType.Small)
    }
}

@Composable
private fun ChallengePanel(
    state: ChallengeUiState,
    h24: Boolean,
    onPick: (Int) -> Unit,
    compact: Boolean,
    scroll: ScrollState,
) {
    FlatPanel(
        padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding,
    ) {
        if (state.phase == ChallengePhase.START) {
            Text(stringResource(R.string.challenge_desc), style = SkType.Question)
            Text(stringResource(R.string.challenge_intro), style = SkType.Body)
            val best = stringResource(R.string.challenge_best, state.best)
            // The panel is as wide as a run's answers, so their room is prepared here, before Start.
            Box(Modifier.fillMaxWidth()) {
                Eyebrow(if (state.best > 0) best else stringResource(R.string.challenge_no_best))
                AnswerRoomPreparation(h24, Modifier.matchParentSize())
            }
        } else if (state.phase == ChallengePhase.RUN) {
            ReadOptions { RunOptions(state, h24, onPick, scroll) }
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
        clockSize = clockSize,
    )
}
