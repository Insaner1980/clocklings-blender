package fi.clocklings.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.clocklings.R
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import kotlinx.coroutines.delay

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

private fun segmentOutcomeColor(outcome: DotOutcome?): Color =
    when (outcome) {
        DotOutcome.FIRST_TRY -> SkColors.Good
        DotOutcome.WITH_HINT -> SkColors.GoldDeep
        DotOutcome.SHOWN -> SkColors.UiInkSoft
        null -> SkColors.Primary
    }
