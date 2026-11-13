package fi.clocklings.ui.challenge

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Levels
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.optionWordsHeight
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.timeLabel

/**
 * Everything the height of a challenge answer row depends on. [fontWeightAdjustment] is the system Bold text
 * setting (0 before Android 12), which makes the same text wider.
 */
internal data class AnswerRoomKey(
    val language: AppLanguage,
    val h24: Boolean,
    val density: Float,
    val fontScale: Float,
    val fontWeightAdjustment: Int,
    val width: Int,
)

/**
 * The [AnswerRoomKey] of the current screen for an answer width, which only layout knows. Every user of the room
 * builds its key here, so the keys always agree.
 */
@Composable
internal fun rememberAnswerRoomKey(h24: Boolean): (width: Int) -> AnswerRoomKey {
    val language = LocalAppLanguage.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val weight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) configuration.fontWeightAdjustment else 0
    return remember(language, h24, density, weight) {
        { width -> AnswerRoomKey(language, h24, density.density, density.fontScale, weight, width) }
    }
}

/**
 * The room a run keeps for its answers, shared by the process for the last 16 configurations so that a recent
 * visit or recreated screen can reuse its measurements. Per [AnswerRoomKey] it holds the text estimates, then the
 * [ROOM_CANDIDATES] answers whose text lays out tallest, then their real row heights, tallest first. The
 * candidates and heights are snapshot state, so a layout that reads them measures again when they arrive. All of
 * it is read and written only in layout and effects, on the main thread.
 */
internal object AnswerRooms {
    private val recentKeys = LinkedHashSet<AnswerRoomKey>()
    private val estimates = HashMap<AnswerRoomKey, MutableMap<Int, Int>>()
    private val candidates = mutableStateMapOf<AnswerRoomKey, List<Int>>()
    private val heights = mutableStateMapOf<AnswerRoomKey, List<Int>>()

    /** Text estimates made so far; tests read it to see that no answer is estimated twice. */
    var estimatesMade = 0
        private set

    fun candidates(key: AnswerRoomKey): List<Int>? = candidates[key]

    fun heights(key: AnswerRoomKey): List<Int>? = heights[key]

    fun storeHeights(
        key: AnswerRoomKey,
        rows: List<Int>,
    ) {
        retain(key)
        heights[key] = rows.sortedDescending()
    }

    /** Ranks every challenge answer not estimated yet at once, without composing any, and returns the candidates. */
    fun rankNow(
        key: AnswerRoomKey,
        measurer: TextMeasurer,
        density: Density,
    ): List<Int> {
        val known = estimatesFor(key)
        for (t in Levels.challengeTimes(key.h24)) {
            if (t !in known) known[t] = measurer.estimate(key, density, t)
        }
        return rank(key, known)
    }

    /**
     * Ranks every challenge answer, starting a frame later and spending at most about [RANK_FRAME_BUDGET_NS] per
     * frame, so the screen keeps drawing meanwhile. If it is cancelled, the estimates made so far stay for
     * [rankNow].
     */
    suspend fun rankInSteps(
        key: AnswerRoomKey,
        measurer: TextMeasurer,
        density: Density,
    ) {
        val known = estimatesFor(key)
        withFrameNanos { }
        var frameStart = System.nanoTime()
        for (t in Levels.challengeTimes(key.h24)) {
            if (t in known) continue
            known[t] = measurer.estimate(key, density, t)
            if (System.nanoTime() - frameStart >= RANK_FRAME_BUDGET_NS) {
                withFrameNanos { }
                frameStart = System.nanoTime()
            }
        }
        rank(key, known)
    }

    private fun estimatesFor(key: AnswerRoomKey): MutableMap<Int, Int> {
        retain(key)
        return estimates.getOrPut(key) { HashMap() }
    }

    private fun retain(key: AnswerRoomKey) {
        recentKeys.remove(key)
        recentKeys.add(key)
        if (recentKeys.size > MAX_CACHED_ROOMS) {
            val oldest = recentKeys.first()
            recentKeys.remove(oldest)
            estimates.remove(oldest)
            candidates.remove(oldest)
            heights.remove(oldest)
        }
    }

    private fun rank(
        key: AnswerRoomKey,
        known: Map<Int, Int>,
    ): List<Int> =
        known.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(ROOM_CANDIDATES)
            .map { it.key }
            .also {
                retain(key)
                candidates[key] = it
            }

    private fun TextMeasurer.estimate(
        key: AnswerRoomKey,
        density: Density,
        t: Int,
    ): Int {
        estimatesMade++
        return optionWordsHeight(
            density,
            timeLabel(key.language, t, key.h24),
            timeLanguage(key.language).digital(t, key.h24),
            key.width,
        )
    }

    /** Forgets everything; for tests that need a first visit. */
    fun clear() {
        recentKeys.clear()
        estimates.clear()
        candidates.clear()
        heights.clear()
        estimatesMade = 0
    }
}

private const val MAX_CACHED_ROOMS = 16

/**
 * Answers ranked tallest by their text that are measured as real rows. A few more than a question shows, as the
 * ranking only sees the words; the reservation test proves the real tallest rows are always among them.
 */
internal const val ROOM_CANDIDATES = 6

/** Time the START screen spends ranking answers per frame. */
private const val RANK_FRAME_BUDGET_NS = 4_000_000L

/** Counts the answer rows composed and disposed; only tests provide it, to keep the cost of a run bounded. */
internal class AnswerRowCounter {
    var composed = 0
    var disposed = 0
}

internal val LocalAnswerRowCounter = staticCompositionLocalOf<AnswerRowCounter?> { null }

/** Counts this answer row in a provided [LocalAnswerRowCounter]. */
@Composable
internal fun CountAnswerRow() {
    val counter = LocalAnswerRowCounter.current ?: return
    DisposableEffect(counter) {
        counter.composed++
        onDispose { counter.disposed++ }
    }
}

/**
 * Measures [times] as real answer rows as wide as [constraints] allow, at any height, in [slot] without placing
 * them. Their text is cleared so it never matches a shown answer.
 */
internal fun SubcomposeMeasureScope.measureAnswerRows(
    slot: Any,
    times: List<Int>,
    h24: Boolean,
    constraints: Constraints,
): List<Int> =
    subcompose(slot) {
        for (t in times) {
            RunOption(t, h24, OptionState.NORMAL, {}, Modifier.clearAndSetSemantics { }, enabled = false)
        }
    }.map { it.measure(Constraints(maxWidth = constraints.maxWidth)).height }

/**
 * Prepares a run's answer room while the START screen shows, so the Start tap measures nothing: it ranks every
 * answer by its text within a small budget per frame, then measures the [ROOM_CANDIDATES] tallest as real rows
 * and drops them again before the run. It draws nothing; [modifier] must give it the width of a run's answers.
 */
@Composable
internal fun AnswerRoomPreparation(
    h24: Boolean,
    modifier: Modifier = Modifier,
) {
    val keyFor = rememberAnswerRoomKey(h24)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    var width by remember { mutableIntStateOf(0) }
    val key = keyFor(width)
    LaunchedEffect(key) {
        if (width > 0 && AnswerRooms.candidates(key) == null) AnswerRooms.rankInSteps(key, measurer, density)
    }
    SubcomposeLayout(modifier) { constraints ->
        width = constraints.maxWidth
        val measuring = keyFor(constraints.maxWidth)
        val candidates = AnswerRooms.candidates(measuring)
        if (candidates != null && AnswerRooms.heights(measuring) == null) {
            AnswerRooms.storeHeights(measuring, measureAnswerRows(PreparedRows, candidates, h24, constraints))
        }
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

private object PreparedRows
