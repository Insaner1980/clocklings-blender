package fi.clocklings.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.ledgeColor

/**
 * Vastausvaihtoehto: sanat vasemmalla ja numeroaika oikealla (web: .opt).
 * Väärä vastaus tärähtää ja himmenee.
 */
@Composable
fun OptionButton(
    words: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    digital: String? = null,
    state: OptionState = OptionState.NORMAL,
    enabled: Boolean = true,
    wordsStyle: TextStyle = SkType.OptWords,
) {
    val result =
        when (state) {
            OptionState.NORMAL -> null
            OptionState.CORRECT -> stringResource(R.string.answer_correct)
            OptionState.WRONG -> stringResource(R.string.answer_incorrect)
        }
    val (bg, border, shadow) =
        when (state) {
            OptionState.NORMAL -> Triple(SkColors.Surface, SkColors.Line, ledgeColor(SkColors.Surface))
            OptionState.CORRECT -> Triple(SkColors.GoodBg, SkColors.OptCorrectBorder, SkColors.OptCorrectShadow)
            OptionState.WRONG -> Triple(SkColors.TryBg, SkColors.OptWrongBorder, SkColors.OptWrongShadow)
        }
    val shake = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(state) {
        if (state == OptionState.WRONG && !reduceMotion) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = SHAKE_MS
                    0f at 0
                    -SHAKE_LEFT_START at (SHAKE_MS * SHAKE_FIRST_FRACTION).toInt()
                    SHAKE_RIGHT at (SHAKE_MS * SHAKE_SECOND_FRACTION).toInt()
                    -SHAKE_LEFT_END at (SHAKE_MS * SHAKE_THIRD_FRACTION).toInt()
                    0f at SHAKE_MS
                },
            )
        }
    }
    DepthSurface(
        onClick = onClick,
        fill = bg,
        shadow = shadow,
        shape = SkShapes.Option,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    if (result != null) {
                        stateDescription = result
                        liveRegion = LiveRegionMode.Polite
                    }
                }.graphicsLayer {
                    translationX = shake.value * density
                    alpha = if (state == OptionState.WRONG) WRONG_ALPHA else 1f
                },
        depth = SkDimens.OptionDepth,
        pressedDepth = 1.dp,
        border = BorderStroke(SkDimens.Border, border),
        enabled = enabled,
        dimWhenDisabled = false,
    ) {
        OptionLabel(words, digital, wordsStyle)
    }
}

private const val SHAKE_MS = 350
private const val WRONG_ALPHA = 0.75f

private const val SHAKE_LEFT_START = 7f
private const val SHAKE_RIGHT = 6f
private const val SHAKE_LEFT_END = 4f
private const val SHAKE_FIRST_FRACTION = 0.2f
private const val SHAKE_SECOND_FRACTION = 0.45f
private const val SHAKE_THIRD_FRACTION = 0.7f

@Composable
private fun OptionLabel(
    words: String,
    digital: String?,
    wordsStyle: TextStyle,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = SkDimens.GapM, vertical = SkDimens.GapS),
    ) {
        if (digital != null && optionTimeBelow(maxWidth, LocalDensity.current.fontScale)) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                WordSafeText(words, wordsStyle, Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                OptionTime(digital, Modifier.align(Alignment.End))
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WordSafeText(words, wordsStyle, Modifier.weight(1f), textAlign = TextAlign.Start)
                if (digital != null) OptionTime(digital)
            }
        }
    }
}

@Composable
private fun OptionTime(
    digital: String,
    modifier: Modifier = Modifier,
) {
    Text(
        digital,
        style = SkType.OptDigital,
        modifier =
            modifier
                .background(SkColors.OptDigitalBg, SkShapes.OptDigital)
                .padding(horizontal = SkDimens.OptionTimePadX, vertical = SkDimens.OptionTimePadY),
    )
}

/** Whether [OptionLabel] puts the time below the words, in a label [labelWidth] wide. */
private fun optionTimeBelow(
    labelWidth: Dp,
    fontScale: Float,
) = labelWidth < SkDimens.GridBreakpoint && fontScale > SkDimens.MAX_ROW_FONT_SCALE

/**
 * The height of an option's words in an [OptionButton] [buttonWidth] px wide, laid out as [OptionLabel] does,
 * but measured without composing anything: for ranking many answers by height. The button is as tall as its
 * words unless the time pill beside them is taller.
 */
internal fun TextMeasurer.optionWordsHeight(
    density: Density,
    words: String,
    digital: String,
    buttonWidth: Int,
    wordsStyle: TextStyle = SkType.OptWords,
): Int =
    with(density) {
        val label = buttonWidth - SkDimens.GapM.roundToPx() * 2
        val wordsWidth =
            if (optionTimeBelow(label.toDp(), fontScale)) {
                label
            } else {
                val time = measure(digital, SkType.OptDigital).size.width + SkDimens.OptionTimePadX.roundToPx() * 2
                label - SkDimens.GapM.roundToPx() - time
            }
        wordSafeHeight(words, wordsStyle, wordsWidth.coerceAtLeast(0))
    }
