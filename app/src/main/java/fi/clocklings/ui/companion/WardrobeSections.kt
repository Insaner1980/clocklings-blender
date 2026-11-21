package fi.clocklings.ui.companion

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.slots
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.SectionChips
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.StarTotalPill
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.slotName
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
internal class WardrobeSections(
    val scroll: ScrollState,
    private val scope: CoroutineScope,
) {
    val headings = mutableStateMapOf<Int, Float>()
    var viewportTop by mutableFloatStateOf(0f)
    private var navigation: Job? = null

    fun selected(count: Int): Int =
        wardrobeSectionIndex(
            scroll.value,
            scroll.maxValue,
            (0 until count).map { headings[it] ?: Float.POSITIVE_INFINITY },
            viewportTop,
        )

    fun select(
        index: Int,
        reduced: Boolean,
    ) {
        val top = headings[index] ?: return
        navigation?.cancel()
        navigation =
            scope.launch {
                val target = wardrobeScrollTarget(scroll.value, scroll.maxValue, top, viewportTop)
                if (reduced) scroll.scrollTo(target) else scroll.animateScrollTo(target)
            }
    }
}

@Composable
internal fun rememberWardrobeSections(kind: CompanionKind): WardrobeSections {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    return remember(kind, scroll, scope) { WardrobeSections(scroll, scope) }
}

@Composable
internal fun WardrobeHeader(
    kind: CompanionKind,
    total: Int,
    access: AccessPolicy,
    navigation: WardrobeSections,
    onBack: () -> Unit,
) {
    val reduced = rememberReduceMotion()
    val colorLabel = if (kind != CompanionKind.HUMAN) listOf(stringResource(R.string.companion_color)) else emptyList()
    val labels = colorLabel + kind.slots.map { stringResource(slotName(it)) }
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        TopBar(stringResource(R.string.doll_title), onBack, trailing = {
            val starInfo = wardrobeStarInfo(kind, total, access)
            StarTotalPill(
                total,
                Modifier.testTag("wardrobe_star_balance").semantics { contentDescription = starInfo },
            )
        })
        SectionChips(labels, navigation.selected(labels.size), { navigation.select(it, reduced) })
    }
}

/**
 * A heading kept close to its own content and set further apart from the section above. The heading
 * shrinks rather than breaks a long word, such as Finnish "Muodonmuutokset".
 */
@Composable
internal fun WardrobeSection(
    title: String,
    onHeadingTop: (Float) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(top = SkDimens.GapS), verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        WordSafeText(
            title,
            SkType.SectionTitle,
            Modifier
                .onGloballyPositioned {
                    onHeadingTop(it.positionInRoot().y)
                }.semantics { heading() },
            textAlign = TextAlign.Start,
        )
        content()
    }
}

/** Top and end rules keep short final sections and pages without a scroll range unambiguous. */
internal fun wardrobeSectionIndex(
    scroll: Int,
    maxScroll: Int,
    headingTops: List<Float>,
    viewportTop: Float,
): Int =
    when {
        maxScroll <= 0 || scroll == 0 -> 0
        scroll >= maxScroll -> headingTops.lastIndex.coerceAtLeast(0)
        else -> headingTops.indexOfLast { it <= viewportTop + 1f }.coerceAtLeast(0)
    }

/** Heading and viewport positions use the same root pixel coordinates. */
internal fun wardrobeScrollTarget(
    scroll: Int,
    maxScroll: Int,
    headingTop: Float,
    viewportTop: Float,
): Int = (scroll + headingTop - viewportTop).roundToInt().coerceIn(0, maxScroll)

@Composable
internal fun WardrobeSaveFailure(retry: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Text(stringResource(R.string.settings_save_error), style = SkType.Body)
        SkButton(
            stringResource(R.string.settings_retry),
            { retry?.invoke() },
            Modifier.fillMaxWidth(),
            kind = ButtonKind.PRIMARY,
        )
    }
}
