package fi.clocklings.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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

/** Pieni pilleri (web: .pill, .pill.muted, .pill.ok). */
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
            PillKind.GOLD -> SkColors.Pressed to SkColors.Ink
            PillKind.MUTED -> SkColors.PillMuted to SkColors.InkSoft
            PillKind.OK -> SkColors.GoodBg to SkColors.Good
        }
    Row(
        modifier
            .background(bg, SkShapes.Pill)
            .then(
                if (large) {
                    Modifier.heightIn(min = SkDimens.IconButton).padding(horizontal = SkDimens.GapM)
                } else {
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) 6.dp else 4.dp),
    ) {
        icon?.let { SkIcon(it, if (large) SkDimens.PillIconLarge else 14.dp) }
        // Shrinks rather than splits a word that does not fit (narrow tiles with large text).
        WordSafeText(text, (if (large) SkType.PillLarge else SkType.Pill).copy(color = fg))
    }
}

/** Pieni harmaa yläotsikko isoilla kirjaimilla (web: .eyebrow). */
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

/** Numeroaika harmaalla taustalla (web: .digital). */
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
                    SkColors.DigitalBg,
                    SkShapes.Digital,
                ).padding(horizontal = 14.dp, vertical = 4.dp),
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
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            Text(timeLabel(LocalAppLanguage.current, time, h24), style = SkType.Words)
            DigitalPill(digitalLabel(res, time, h24))
        }
        if (speechAvailable) {
            RoundIconButton(
                SkIcons.Listen,
                stringResource(R.string.listen),
                { onSpeak(spokenTime(res, time, h24)) },
                large = true,
            )
        }
    }
}

/** How a finished game task went; [ProgressDots] draws it in place of the rainbow colour. */
enum class DotOutcome { FIRST_TRY, WITH_HINT, SHOWN }

/**
 * Progress dots: finished ones in rainbow colours, the current one ringed (web: .progress).
 * [currentDone] fills the current dot with a pop after [fillDelayMs] (when the flying star lands),
 * and [onDotCenter] reports the dot centres for the star's flight.
 * When [outcomes] is given (games), finished dots show how the task went: green or gold with a
 * check mark, and grey with a ring when the answer was shown. The current dot then pops in its
 * outcome colour, so pass [outcomes] including the task that was just solved.
 */
@Composable
fun ProgressDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
    currentDone: Boolean = false,
    fillDelayMs: Long = 0L,
    onDotCenter: ((Int, Offset) -> Unit)? = null,
    outcomes: List<DotOutcome>? = null,
) {
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS, Alignment.CenterHorizontally),
    ) {
        repeat(count) { k ->
            val anchor = if (onDotCenter != null) Modifier.confettiAnchor { onDotCenter(k, it) } else Modifier
            val dot = Modifier.size(SkDimens.ProgressDot).then(anchor)
            val color = SkColors.Rainbow7[k % SkColors.Rainbow7.size]
            when {
                k < current -> {
                    val outcome = outcomes?.getOrNull(k)
                    if (outcome == null) Box(dot.background(color, CircleShape)) else OutcomeDot(outcome, dot)
                }

                k == current && currentDone -> {
                    // Success is the only path that pops, so a game dot pops in its solved colour.
                    val solved = outcomes?.let { outcomeFill(it.getOrNull(k) ?: DotOutcome.FIRST_TRY) }
                    PoppingDot(solved ?: color, fillDelayMs, dot)
                }

                k == current -> {
                    Box(
                        dot.background(SkColors.Surface, CircleShape).border(3.dp, SkColors.Hour, CircleShape),
                    )
                }

                else -> {
                    Box(dot.background(SkColors.DotEmpty, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun OutcomeDot(
    outcome: DotOutcome,
    modifier: Modifier = Modifier,
) {
    when (outcome) {
        DotOutcome.SHOWN -> {
            Box(
                modifier
                    .background(SkColors.DotEmpty, CircleShape)
                    .border(SkDimens.BorderThin, SkColors.InkSoft, CircleShape),
            )
        }

        DotOutcome.FIRST_TRY, DotOutcome.WITH_HINT -> {
            Box(modifier.background(outcomeFill(outcome), CircleShape), contentAlignment = Alignment.Center) {
                SkIcon(SkIcons.Check, SkDimens.ProgressDotIcon)
            }
        }
    }
}

/** The fill of a solved game dot: green on the first try, gold after a hint. */
private fun outcomeFill(outcome: DotOutcome): Color =
    if (outcome == DotOutcome.WITH_HINT) SkColors.GoldDeep else SkColors.Good

enum class FeedbackKind { OK, HINT }

/** Palautelaatikko: onnistuminen vihreällä, vinkki oranssilla (web: .feedback). */
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

/** Nykyinen pallo täyttyy värillä ja pompahtaa, kun tähti laskeutuu siihen. */
@Composable
private fun PoppingDot(
    color: Color,
    delayMs: Long,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(delayMs)
        scale.animateTo(
            1f,
            keyframes {
                durationMillis = DOT_POP_MS
                0f at 0
                DOT_POP_PEAK at DOT_POP_MS / 2
                1f at DOT_POP_MS
            },
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .border(3.dp, SkColors.Hour, CircleShape),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }.background(color, CircleShape),
        )
    }
}

private const val DOT_POP_MS = 320
private const val DOT_POP_PEAK = 1.5f
