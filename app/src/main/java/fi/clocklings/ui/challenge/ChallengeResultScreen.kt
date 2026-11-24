package fi.clocklings.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.appearance.LocalCompanionOutfit
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.appearance.PREVIEW_TIME
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.ActionPair
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.StarIcon
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.excludeConfetti
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

private const val RECORD_CONFETTI = 120

@Composable
internal fun ChallengeResultScreen(
    state: ChallengeUiState,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchor = remember { mutableStateOf(Offset.Zero) }
    RecordCelebrationEffect(state) { anchor.value }
    val companion = LocalPersonalization.current.companion
    val celebration = remember(state.runId) { CompanionCelebration(newOutfit = null, jump = true) }
    ScreenScaffold(
        modifier,
        section = Section.PLAY,
        scroll = false,
        topBar = { TopBar(stringResource(R.string.challenge_title), onBack) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().confettiAnchor { anchor.value = it }) {
                ChallengeResultActions(state, onStart, onBack)
            }
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewport = maxHeight
            val short = viewport < SkDimens.CompactTaskHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewport),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SkDimens.Gap),
            ) {
                if (short) ChallengeScore(state)
                val slot = if (short) Modifier else Modifier.weight(1f)
                Box(slot.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (companion == CompanionKind.NONE) {
                        Box(
                            Modifier
                                .size(SkDimens.ChallengeCompanionHeight)
                                .testTag("challenge_result_clock")
                                .clearAndSetSemantics { },
                        ) {
                            ClockFace(ClockCommand(PREVIEW_TIME), Modifier.matchParentSize(), mini = true)
                        }
                    } else {
                        CompanionView(
                            companion,
                            LocalCompanionOutfit.current,
                            Modifier
                                .height(
                                    SkDimens.ChallengeCompanionHeight,
                                ).aspectRatio(companionAspectRatio(companion)),
                            celebration = celebration,
                        )
                    }
                }
                if (!short) ChallengeScore(state)
            }
        }
    }
}

@Composable
private fun ChallengeScore(state: ChallengeUiState) {
    val description =
        if (state.newRecord) {
            stringResource(R.string.challenge_new_record, state.score)
        } else {
            stringResource(R.string.challenge_result, state.score, state.best)
        }
    FlatPanel(
        modifier = Modifier.excludeConfetti(),
        padding = SkDimens.GroupGap,
    ) {
        Column(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapHalf),
        ) {
            Row(
                Modifier
                    .heightIn(min = SkDimens.ChallengeBadgeHeight)
                    .background(if (state.newRecord) SkColors.HelpOnFill else SkColors.PillMuted, SkShapes.Pill)
                    .padding(horizontal = SkDimens.GapM),
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.newRecord) StarIcon(true, size = SkDimens.ChallengeBadgeStar)
                Text(
                    stringResource(if (state.newRecord) R.string.challenge_new_best else R.string.challenge_over),
                    style = SkType.ChallengeBadge,
                    color = if (state.newRecord) SkColors.Ink else SkColors.UiInkSoft,
                )
            }
            Text(state.score.toString(), style = SkType.ScoreHuge, textAlign = TextAlign.Center)
            Text(
                pluralStringResource(R.plurals.challenge_correct_label, state.score),
                style = SkType.SectionTitle,
                textAlign = TextAlign.Center,
            )
            if (!state.newRecord) {
                Text(
                    stringResource(R.string.challenge_best, state.best),
                    style = SkType.SmallSoft,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (state.saveFailed) {
            CompanionTip(
                stringResource(R.string.progress_save_error),
                color = SkColors.HintFill,
                avatarWidth = SkDimens.FeedbackAvatar,
            )
        }
    }
}

@Composable
private fun ChallengeResultActions(
    state: ChallengeUiState,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val back = stringResource(R.string.back_description)
    val playAgain = stringResource(R.string.play_again)
    val again = if (state.saveFailed) stringResource(R.string.settings_retry) else playAgain
    ActionPair(
        back,
        playAgain,
        start = { SkButton(back, onBack, it) },
        end = {
            SkButton(
                again,
                onStart,
                it,
                kind = ButtonKind.PRIMARY,
                enabled = !state.savePending,
            )
        },
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
            withFrameNanos { }
            confetti?.burst(currentAnchor(), RECORD_CONFETTI)
        }
    }
}
