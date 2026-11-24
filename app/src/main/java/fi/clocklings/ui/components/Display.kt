package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resources
import fi.clocklings.ui.spokenTime
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel

enum class PillKind { GOLD, MUTED, OK, STAR_TOTAL_COMPLETE }

enum class OptionState { NORMAL, CORRECT, WRONG }

@Composable
internal fun LockPill(compact: Boolean = false) {
    Row(
        Modifier
            .heightIn(min = SkDimens.LockPillHeight)
            .background(SkColors.PillMuted, SkShapes.Pill)
            .padding(horizontal = if (compact) SkDimens.LockPillTilePadding else SkDimens.GapOptions),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkIcon(SkIcons.Lock, SkDimens.SmallIcon, tint = SkColors.UiInkSoft)
        WordSafeText(
            stringResource(R.string.full_version_label),
            if (compact) SkType.LockPillTile else SkType.LockPill,
            maxLines = 1,
        )
    }
}

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
            PillKind.STAR_TOTAL_COMPLETE -> SkColors.StarTotalCompleteFill to SkColors.Ink
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

@Composable
fun StarTotalPill(
    totalStars: Int,
    modifier: Modifier = Modifier,
) {
    Pill(
        stringResource(R.string.home_star_total, totalStars, Wardrobe.MAX_STARS),
        modifier,
        kind = if (totalStars == Wardrobe.MAX_STARS) PillKind.STAR_TOTAL_COMPLETE else PillKind.GOLD,
        icon = SkIcons.StarOn,
        large = true,
    )
}

/** Small uppercase heading. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
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
    val words: @Composable () -> Unit = {
        WordSafeText(timeLabel(LocalAppLanguage.current, time, h24), SkType.Words, textAlign = TextAlign.Start)
    }
    val digits: @Composable () -> Unit = { DigitalPill(digitalLabel(res, time, h24)) }
    val listen: @Composable () -> Unit = {
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
    BoxWithConstraints(modifier) {
        val stack =
            speechAvailable && maxWidth <= SkDimens.GridBreakpoint &&
                LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        if (stack) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                words()
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                ) {
                    digits()
                    listen()
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                ) {
                    words()
                    digits()
                }
                listen()
            }
        }
    }
}

/** The outcome of a finished game task. */
enum class DotOutcome { FIRST_TRY, WITH_HINT, SHOWN }

enum class FeedbackKind { OK, HINT }

/** Success or retry feedback with readable text. */
@Composable
fun FeedbackBox(
    text: String,
    kind: FeedbackKind,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val (bg, style) =
        when (kind) {
            FeedbackKind.OK -> SkColors.GoodBg to SkType.FeedbackOk
            FeedbackKind.HINT -> SkColors.HintFill to SkType.Body
        }
    Text(
        text,
        style = style,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(color ?: bg, SkShapes.Feedback)
                .padding(horizontal = SkDimens.GapL, vertical = SkDimens.GapLabel),
    )
}
