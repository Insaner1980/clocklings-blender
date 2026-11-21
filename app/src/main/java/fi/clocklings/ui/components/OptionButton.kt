package fi.clocklings.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import fi.clocklings.R
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/**
 * Tonal answer with word-safe text, a time pill and explicit feedback icons.
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
    val bg =
        when (state) {
            OptionState.NORMAL -> SkColors.SurfaceContainer
            OptionState.CORRECT -> SkColors.OptCorrectFill
            OptionState.WRONG -> Color.Transparent
        }
    val border =
        when (state) {
            OptionState.NORMAL -> null
            OptionState.CORRECT -> BorderStroke(SkDimens.BorderThin, SkColors.OptCorrectBorder)
            OptionState.WRONG -> BorderStroke(SkDimens.BorderQuiet, SkColors.FieldOutline)
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
    PressSurface(
        onClick = onClick,
        fill = bg,
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
                },
        border = border,
        enabled = enabled,
        dimWhenDisabled = false,
    ) {
        OptionLabel(words, digital, wordsStyle, state)
    }
}

private const val SHAKE_MS = 350

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
    state: OptionState,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labelStyle = if (state == OptionState.WRONG) wordsStyle.copy(color = SkColors.UiInkSoft) else wordsStyle
        val timeFill = if (state == OptionState.WRONG) SkColors.PillMuted else SkColors.Surface
        val compact = maxWidth < SkDimens.OptionCompactWidth
        val start = if (state == OptionState.NORMAL) SkDimens.PanelPadding else SkDimens.GapLabel
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) SkDimens.OptionMinHeightCompact else SkDimens.OptionMinHeight)
                .padding(start = start, end = SkDimens.GapM, top = SkDimens.GapS, bottom = SkDimens.GapS),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapOptions),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state != OptionState.NORMAL) {
                SkIcon(
                    if (state == OptionState.CORRECT) SkIcons.Check else SkIcons.Close,
                    SkDimens.ActionIcon,
                    tint = if (state == OptionState.CORRECT) SkColors.Good else SkColors.UiInkSoft,
                )
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                if (digital != null && optionTimeBelow(maxWidth, LocalDensity.current.fontScale)) {
                    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                        WordSafeText(words, labelStyle, Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        OptionTime(digital, timeFill, Modifier.align(Alignment.End))
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WordSafeText(words, labelStyle, Modifier.weight(1f), textAlign = TextAlign.Start)
                        if (digital != null) OptionTime(digital, timeFill)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionTime(
    digital: String,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .testTag("option_time")
            .widthIn(min = SkDimens.OptionTimeMinWidth)
            .heightIn(min = SkDimens.OptionTimeMinHeight)
            .background(fill, SkShapes.OptDigital)
            .padding(horizontal = SkDimens.OptionTimePadX),
        contentAlignment = Alignment.Center,
    ) { Text(digital, style = SkType.OptDigital, textAlign = TextAlign.Center) }
}

/** Stacks the time at large text when the remaining label area is narrow. */
private fun optionTimeBelow(
    labelWidth: Dp,
    fontScale: Float,
) = labelWidth < SkDimens.GridBreakpoint && fontScale > SkDimens.MAX_ROW_FONT_SCALE

/** Measures the same word area as OptionLabel, including the optional feedback icon. */
internal fun TextMeasurer.optionWordsHeight(
    density: Density,
    words: String,
    digital: String,
    buttonWidth: Int,
    wordsStyle: TextStyle = SkType.OptWords,
    state: OptionState = OptionState.NORMAL,
): Int =
    with(density) {
        val start = if (state == OptionState.NORMAL) SkDimens.PanelPadding else SkDimens.GapLabel
        val icon = if (state == OptionState.NORMAL) 0 else (SkDimens.ActionIcon + SkDimens.GapOptions).roundToPx()
        val label = buttonWidth - start.roundToPx() - SkDimens.GapM.roundToPx() - icon
        val wordsWidth =
            if (optionTimeBelow(label.toDp(), fontScale)) {
                label
            } else {
                val time =
                    maxOf(
                        SkDimens.OptionTimeMinWidth.roundToPx(),
                        measure(digital, SkType.OptDigital).size.width + SkDimens.OptionTimePadX.roundToPx() * 2,
                    )
                label - SkDimens.GapM.roundToPx() - time
            }
        wordSafeHeight(words, wordsStyle, wordsWidth.coerceAtLeast(0))
    }
