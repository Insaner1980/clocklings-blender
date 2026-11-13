package fi.clocklings.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Keskitetty teksti, joka ei katkea kesken sanan: fonttia pienennetään niin, että pisin sana
 * mahtuu riville (esim. "Tasatunnit" tai "Sateenkaarihiukset" kapealla näytöllä).
 */
@Composable
fun WordSafeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minScale: Float = MIN_SCALE,
    textAlign: TextAlign = TextAlign.Center,
) {
    BasicText(
        text = text,
        style = style.merge(TextStyle(textAlign = textAlign)),
        modifier = modifier,
        autoSize = WordSafeAutoSize(style.fontSize.value, style.fontSize.value * minScale),
    )
}

/** Suurin fonttikoko, jolla mikään rivinvaihto ei osu sanan keskelle. Ei käytä alikoostamista. */
private data class WordSafeAutoSize(
    val maxSp: Float,
    val minSp: Float,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit = wordSafeFontSize(maxSp, minSp, text.text) { performLayout(constraints, text, it) }
}

/** The largest size from [maxSp] down to [minSp] at which the [layout] of [text] breaks no word. */
private inline fun wordSafeFontSize(
    maxSp: Float,
    minSp: Float,
    text: String,
    layout: (TextUnit) -> TextLayoutResult,
): TextUnit {
    var size = maxSp
    while (size > minSp) {
        val result = layout(size.sp)
        if (!result.didOverflowWidth && !result.breaksInsideWord(text)) return size.sp
        size -= WORD_SAFE_STEP_SP
    }
    return minSp.sp
}

private fun TextLayoutResult.breaksInsideWord(s: String): Boolean =
    (0 until lineCount - 1).any { line ->
        val end = getLineEnd(line)
        end in 1 until s.length && !s[end - 1].isWhitespace() && s[end - 1] != '-' && !s[end].isWhitespace()
    }

/**
 * The height [WordSafeText] gives [text] in [style] at most [maxWidth] px wide, measured without composing it,
 * for example to compare many texts cheaply.
 */
internal fun TextMeasurer.wordSafeHeight(
    text: String,
    style: TextStyle,
    maxWidth: Int,
    minScale: Float = MIN_SCALE,
): Int {
    val constraints = Constraints(maxWidth = maxWidth)
    val maxSp = style.fontSize.value
    val size =
        wordSafeFontSize(maxSp, maxSp * minScale, text) {
            measure(text, style.copy(fontSize = it), constraints = constraints)
        }
    return measure(text, style.copy(fontSize = size), constraints = constraints).size.height
}

private const val WORD_SAFE_STEP_SP = 0.5f
private const val MIN_SCALE = 0.68f
