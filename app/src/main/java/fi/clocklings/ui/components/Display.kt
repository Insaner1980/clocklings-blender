package fi.clocklings.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import fi.clocklings.R
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.resources
import fi.clocklings.ui.spokenTime
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel
import kotlinx.coroutines.delay
import java.util.Locale

enum class PillKind { GOLD, MUTED, OK }

enum class OptionState { NORMAL, CORRECT, WRONG }

/** Small status label, optionally accompanied by an icon. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.GOLD,
    icon: SvgIcon? = null,
    large: Boolean = false,
) {
    val (bg, fg) =
        when (kind) {
            PillKind.GOLD -> SkColors.KouluFill to SkColors.Ink
            PillKind.MUTED -> SkColors.PillMuted to SkColors.UiInkSoft
            PillKind.OK -> SkColors.GoodBg to SkColors.Good
        }
    Row(
        modifier
            .background(bg, SkShapes.Pill)
            .then(
                if (large) {
                    Modifier.heightIn(min = SkDimens.IconButton).padding(horizontal = SkDimens.GapM)
                } else {
                    Modifier.padding(horizontal = SkDimens.PillPadX, vertical = SkDimens.PillPadY)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) SkDimens.GapHalf else SkDimens.GapXS),
    ) {
        icon?.let { SkIcon(it, if (large) SkDimens.PillIconLarge else SkDimens.SmallIcon) }
        // Shrinks rather than splits a word that does not fit (narrow tiles with large text).
        WordSafeText(text, (if (large) SkType.PillLarge else SkType.Pill).copy(color = fg))
    }
}

/** Small uppercase heading. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(Locale.forLanguageTag(LocalAppLanguage.current.tag)),
        style = SkType.Eyebrow,
        modifier = modifier,
    )
}

/** Digital time on a quiet surface. */
@Composable
fun DigitalPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = SkType.Digital,
        modifier =
            modifier
                .background(
                    SkColors.SurfaceContainer,
                    SkShapes.Digital,
                ).padding(horizontal = SkDimens.Gap, vertical = SkDimens.GapHalf),
    )
}

/** The time in words and digits, with a large listen button on the end when speech is available. */
@Composable
fun TimeReadout(
    time: Int,
    h24: Boolean,
    speechAvailable: Boolean,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = resources()
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            Text(timeLabel(LocalAppLanguage.current, time, h24), style = SkType.Words)
            DigitalPill(digitalLabel(res, time, h24))
        }
        if (speechAvailable) {
            RoundIconButton(
                SkIcons.Listen,
                stringResource(R.string.listen),
                { onSpeak(spokenTime(res, time, h24)) },
                large = true,
                tonal = true,
            )
        }
    }
}

/** The outcome of a finished game task. */
enum class DotOutcome { FIRST_TRY, WITH_HINT, SHOWN }

/** Segmented task progress with a readable counter and the existing flying-star anchors. */
@Composable
fun ProgressSegments(
    count: Int,
    current: Int,
    track: Color,
    modifier: Modifier = Modifier,
    currentDone: Boolean = false,
    fillDelayMs: Long = 0L,
    onSegmentCenter: ((Int, Offset) -> Unit)? = null,
    outcomes: List<DotOutcome>? = null,
) {
    val reduced = rememberReduceMotion()
    var filled by remember(current, currentDone) { mutableStateOf(false) }
    LaunchedEffect(current, currentDone, reduced, fillDelayMs) {
        if (currentDone) {
            if (!reduced) delay(fillDelayMs)
            filled = true
        }
    }
    val fraction by animateFloatAsState(
        if (filled) 1f else 0f,
        if (reduced) snap() else tween(SEGMENT_FILL_MS),
        label = "progress fill",
    )
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement =
                Arrangement.spacedBy(
                    if (count <=
                        WIDE_GAP_LIMIT
                    ) {
                        SkDimens.ProgressSegmentGapWide
                    } else {
                        SkDimens.ProgressSegmentGap
                    },
                ),
        ) {
            repeat(count) { k ->
                val completing = k == current && currentDone
                val outcome = outcomes?.getOrNull(k)?.takeIf { k < current || completing }
                val finished = segmentOutcomeColor(outcome)
                val color =
                    when {
                        k < current -> finished
                        k == current -> SkColors.PrimarySoft
                        else -> track
                    }
                val anchor = onSegmentCenter?.let { report -> Modifier.confettiAnchor { report(k, it) } } ?: Modifier
                ProgressSegment(
                    color,
                    finished,
                    if (completing) fraction else 0f,
                    outcome,
                    k + 1,
                    modifier = Modifier.weight(1f).then(anchor),
                )
            }
        }
        Text(stringResource(R.string.task_position, current + 1, count), style = SkType.Counter)
    }
}

@Composable
private fun ProgressSegment(
    color: Color,
    finished: Color,
    fraction: Float,
    outcome: DotOutcome?,
    taskNumber: Int,
    modifier: Modifier = Modifier,
) {
    val segment = modifier.height(SkDimens.ProgressSegmentHeight).outcomeSemantics(outcome, taskNumber)
    if (outcome == DotOutcome.SHOWN) {
        Row(segment, horizontalArrangement = Arrangement.spacedBy(SkDimens.ProgressSegmentGap)) {
            repeat(2) {
                Box(Modifier.weight(1f).fillMaxHeight().background(finished, SkShapes.ProgressSegment))
            }
        }
    } else {
        Box(segment.clip(SkShapes.ProgressSegment).background(color)) {
            if (fraction > 0f) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(finished))
            }
        }
    }
}

@Composable
private fun Modifier.outcomeSemantics(
    outcome: DotOutcome?,
    taskNumber: Int,
): Modifier {
    val label =
        when (outcome) {
            DotOutcome.FIRST_TRY -> R.string.task_outcome_first_try
            DotOutcome.WITH_HINT -> R.string.task_outcome_with_hint
            DotOutcome.SHOWN -> R.string.task_outcome_shown
            null -> return this
        }
    val description = stringResource(label, taskNumber)
    return semantics { contentDescription = description }
}

/** Duration of the segment fill after its flying star arrives. */
private const val SEGMENT_FILL_MS = 150

/** Sparse progress rows use a slightly wider gap. */
private const val WIDE_GAP_LIMIT = 5

enum class FeedbackKind { OK, HINT }

/** Success or retry feedback with readable text. */
@Composable
fun FeedbackBox(
    text: String,
    kind: FeedbackKind,
    modifier: Modifier = Modifier,
) {
    val (bg, style) =
        when (kind) {
            FeedbackKind.OK -> SkColors.GoodBg to SkType.FeedbackOk
            FeedbackKind.HINT -> SkColors.TryBg to SkType.FeedbackHint
        }
    Text(
        text,
        style = style,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(bg, SkShapes.Feedback)
                .padding(horizontal = SkDimens.GapL, vertical = SkDimens.GapLabel),
    )
}

private fun segmentOutcomeColor(outcome: DotOutcome?): Color =
    when (outcome) {
        DotOutcome.FIRST_TRY -> SkColors.Good
        DotOutcome.WITH_HINT -> SkColors.GoldDeep
        DotOutcome.SHOWN -> SkColors.UiInkSoft
        null -> SkColors.Primary
    }
