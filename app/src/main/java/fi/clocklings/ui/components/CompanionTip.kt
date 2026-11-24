package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.appearance.LocalCompanionOutfit
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** The companion says [text]. Without a companion the same text shows in a plain feedback box. */
@Composable
fun CompanionTip(
    text: String,
    modifier: Modifier = Modifier,
    kind: FeedbackKind = FeedbackKind.HINT,
    celebrate: Boolean = false,
    color: Color? = null,
    avatarWidth: Dp = SkDimens.TipCompanion,
    animated: Boolean = true,
) {
    CompanionTip(
        modifier,
        celebrationKey = text,
        celebrate = celebrate,
        color = color ?: SkColors.Bubble,
        fallback = { FeedbackBox(text, kind, color = color) },
        avatarWidth = avatarWidth,
        animated = animated,
    ) {
        Text(text, style = SkType.Body)
    }
}

/**
 * The companion presents [content] in a speech bubble whose tail points at its head. The bubble
 * keeps the full remaining width; on narrow screens or with large text the companion moves above it.
 */
@Composable
fun CompanionTip(
    modifier: Modifier = Modifier,
    celebrationKey: String? = null,
    celebrate: Boolean = false,
    color: Color = SkColors.Bubble,
    fallback: @Composable () -> Unit = {},
    avatarWidth: Dp = SkDimens.TipCompanion,
    animated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val companion = LocalPersonalization.current.companion
    if (companion == CompanionKind.NONE) {
        Box(modifier.fillMaxWidth()) { fallback() }
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact =
            maxWidth < SkDimens.GridBreakpoint || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        val celebration =
            remember(celebrationKey, celebrate) {
                if (celebrate) CompanionCelebration(newOutfit = null, jump = true) else null
            }
        val avatar: @Composable () -> Unit = {
            CompanionView(
                companion,
                LocalCompanionOutfit.current,
                Modifier.size(if (compact) SkDimens.TipCompanionCompact else avatarWidth),
                animated = animated,
                celebration = celebration,
            )
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                avatar()
                SpeechBubbleBox(tail = BubbleTail.UP_START, color = color, content = content)
            }
        } else {
            // Align the first line and tail with the head, including short captions.
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.BubbleTail + SkDimens.GapS),
            ) {
                avatar()
                SpeechBubbleBox(
                    Modifier.weight(1f).padding(top = SkDimens.BubbleHeadOffset),
                    tail = BubbleTail.START,
                    color = color,
                    content = content,
                )
            }
        }
    }
}

enum class BubbleTail {
    /** Points left toward the head of a companion beside the first line. */
    START,

    /** Points down and back toward a speaker below and before the bubble. */
    DOWN_START,

    /** Points up toward a speaker above the bubble's start edge. */
    UP_START,
}

/** A single line or short paragraph said by the companion; the tail points down toward it. */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SkType.Body,
    fill: Boolean = true,
    tail: BubbleTail = BubbleTail.DOWN_START,
    color: Color = SkColors.Bubble,
) {
    SpeechBubbleBox(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier), tail, color) {
        WordSafeText(text, style, textAlign = TextAlign.Start)
    }
}

@Composable
fun SpeechBubbleBox(
    modifier: Modifier = Modifier,
    tail: BubbleTail = BubbleTail.DOWN_START,
    color: Color = SkColors.Bubble,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .drawBehind {
                val t = SkDimens.BubbleTail.toPx()
                val path =
                    Path().apply {
                        when (tail) {
                            BubbleTail.START -> {
                                val corner = SkShapes.Bubble.topStart.toPx(size, this@drawBehind)
                                val low = corner + t
                                val high = size.height - corner - t
                                val head =
                                    SkDimens.TipCompanion.toPx() * (1 - HEAD_HEIGHT) - SkDimens.BubbleHeadOffset.toPx()
                                val y = if (low <= high) head.coerceIn(low, high) else size.height / 2
                                moveTo(t, y - t)
                                lineTo(-t, y)
                                lineTo(t, y + t)
                            }

                            BubbleTail.DOWN_START -> {
                                moveTo(t, size.height - t)
                                lineTo(-t * DOWN_TAIL_REACH, size.height + t)
                                lineTo(t * 2, size.height)
                            }

                            BubbleTail.UP_START -> {
                                val x = SkDimens.TipCompanionCompact.toPx() / 2
                                moveTo(x - t, t)
                                lineTo(x, -t)
                                lineTo(x + t, t)
                            }
                        }
                        close()
                    }
                drawPath(path, color)
            }.background(color, SkShapes.Bubble)
            .padding(SkDimens.GapM),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS), content = content)
    }
}

/** Head height above the companion's feet, as a share of its artwork size. */
private const val HEAD_HEIGHT = 0.72f
private const val DOWN_TAIL_REACH = 0.6f
