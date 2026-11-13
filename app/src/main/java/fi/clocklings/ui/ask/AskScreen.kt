package fi.clocklings.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Levels
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.HelpTopBar
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TaskActionFocus
import fi.clocklings.ui.components.TimeReadout
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import kotlin.random.Random

private enum class AskPhase { SET, ASK, REVEALED }

/** Partners set, answer, reveal, and swap turns without recording a scored result. */
@Composable
fun AskScreen(
    h24: Boolean,
    help: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    var phase by rememberSaveable { mutableStateOf(AskPhase.SET) }
    var setter by rememberSaveable { mutableIntStateOf(1) }
    // Not 12:00, where both hands lie on top of each other.
    var time by rememberSaveable { mutableIntStateOf(ClockTime.start(h24)) }
    var command by remember { mutableStateOf(ClockCommand(time)) }
    val scrollState = rememberScrollState()
    val compactScrollState = rememberScrollState()
    var previousPhase by rememberSaveable { mutableStateOf(phase) }
    val onRandom = {
        time = Levels.randomClockTime(ClockTime.period(h24), time, Random.Default)
        command = ClockCommand(time, RANDOM_MOVE_MS)
        app.play(Sound.POP)
    }
    LaunchedEffect(phase) {
        if (previousPhase != phase && phase == AskPhase.SET) {
            scrollState.scrollTo(0)
            compactScrollState.scrollTo(0)
        }
        previousPhase = phase
    }
    ClockTaskScaffold(
        modifier = modifier,
        scrollState = scrollState,
        compactScrollState = compactScrollState,
        clockHeightShare = SkDimens.PLAY_CLOCK_HEIGHT_SHARE,
        section = Section.TOGETHER,
        topBar = { HelpTopBar(stringResource(R.string.ask_title), help, onBack, app::toggleHelp) },
        actions = { focus, compact ->
            AskActions(
                phase,
                focus,
                compact,
                onRandom,
                onAsk = {
                    app.play(Sound.POP)
                    app.play(Sound.QUESTION_READ)
                    phase = AskPhase.ASK
                },
                onReveal = { phase = AskPhase.REVEALED },
                onSwap = {
                    if (phase == AskPhase.REVEALED) setter = PLAYER_NUMBER_SUM - setter
                    phase = AskPhase.SET
                },
            )
        },
        visual = { size -> AskClock(command, phase, h24, help, size) { time = it } },
    ) { compact ->
        val role = askRoleDescription(phase, setter)
        val padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding
        FlatPanel(Modifier.semantics { contentDescription = role }, padding = padding) {
            if (phase == AskPhase.SET) {
                AskSetup(compact, onRandom)
            } else {
                AskQuestion(phase == AskPhase.REVEALED, time, h24, speechAvailable)
            }
        }
    }
}

@Composable
private fun AskClock(
    command: ClockCommand,
    phase: AskPhase,
    h24: Boolean,
    help: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onChange: (Int) -> Unit,
) {
    ClockBox(size) {
        ClockFace(
            command = command,
            period = ClockTime.period(h24),
            step = STEP,
            interactive = phase == AskPhase.SET,
            help = help,
            describeTime = phase != AskPhase.ASK,
            onChange = onChange,
        )
    }
}

private const val STEP = 5
private const val RANDOM_MOVE_MS = 900
private const val REVEAL_CONFETTI = 40
private const val PLAYER_NUMBER_SUM = 3

@Composable
private fun ColumnScope.AskSetup(
    compact: Boolean,
    onRandom: () -> Unit,
) {
    Text(stringResource(R.string.ask_big_question), style = SkType.Question)
    if (compact) {
        Text(stringResource(R.string.ask_big_text), style = SkType.Body)
    } else {
        CompanionTip(stringResource(R.string.ask_big_text))
    }
    if (!compact) {
        SkButton(
            stringResource(R.string.random_time),
            onRandom,
            Modifier.fillMaxWidth(),
            icon = SkIcons.Shuffle,
        )
    }
}

/**
 * The partner answers; the revealed answer is laid out like the Explore readout. The reveal button sits
 * in the bottom bar, so the answer scrolls itself into view when it may appear below the fold.
 */
@Composable
private fun ColumnScope.AskQuestion(
    revealed: Boolean,
    time: Int,
    h24: Boolean,
    speechAvailable: Boolean,
) {
    val app = LocalAppViewModel.current
    val answer = remember { BringIntoViewRequester() }
    var answerSize by remember { mutableStateOf(IntSize.Zero) }
    val fadePx = with(LocalDensity.current) { SkDimens.ScrollFade.toPx() }
    Text(stringResource(R.string.question_read), style = SkType.Question)
    CompanionTip(stringResource(R.string.ask_small_text))
    if (revealed) {
        TimeReadout(
            time,
            h24,
            speechAvailable,
            app::speak,
            Modifier.bringIntoViewRequester(answer).onSizeChanged { answerSize = it },
        )
    }
    LaunchedEffect(revealed, answerSize) {
        // One fade taller than the readout, so its bottom clears the scaffold's bottom scroll fade.
        if (revealed && answerSize != IntSize.Zero) {
            answer.bringIntoView(Rect(0f, 0f, answerSize.width.toFloat(), answerSize.height + fadePx))
        }
    }
}

/** The single action of the current phase, always reachable in the bottom bar; revealing celebrates. */
@Composable
private fun AskActions(
    phase: AskPhase,
    focus: TaskActionFocus,
    compact: Boolean,
    onRandom: () -> Unit,
    onAsk: () -> Unit,
    onReveal: () -> Unit,
    onSwap: () -> Unit,
) {
    val app = LocalAppViewModel.current
    val confetti = LocalConfetti.current
    val haptic = LocalHapticFeedback.current
    val revealAnchor = remember { mutableStateOf(Offset.Zero) }
    val label =
        when (phase) {
            AskPhase.SET -> R.string.ask_go
            AskPhase.ASK -> R.string.ask_reveal
            AskPhase.REVEALED -> R.string.ask_new
        }
    // Keep the same focus target when its action changes; replacing it can leave focus on a scrolled-away header.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        if (compact && phase == AskPhase.SET) {
            SkButton(stringResource(R.string.random_time), onRandom, Modifier.weight(1f), icon = SkIcons.Shuffle)
        }
        SkButton(
            stringResource(label),
            {
                when (phase) {
                    AskPhase.SET -> {
                        onAsk()
                    }

                    AskPhase.ASK -> {
                        onReveal()
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        app.play(Sound.OK)
                        confetti?.burst(revealAnchor.value, REVEAL_CONFETTI)
                    }

                    AskPhase.REVEALED -> {
                        app.stopSpeech()
                        app.play(Sound.POP)
                        onSwap()
                    }
                }
            },
            Modifier.weight(1f).taskActionFocus(focus).confettiAnchor { revealAnchor.value = it },
            kind = if (phase == AskPhase.REVEALED) ButtonKind.GREEN else ButtonKind.PRIMARY,
            icon = if (phase == AskPhase.SET) SkIcons.Pass else null,
        )
    }
}

@Composable
private fun askRoleDescription(
    phase: AskPhase,
    setter: Int,
): String =
    stringResource(
        if (phase == AskPhase.SET) R.string.ask_big_eyebrow else R.string.ask_small_eyebrow,
        if (phase == AskPhase.SET) setter else PLAYER_NUMBER_SUM - setter,
    )
