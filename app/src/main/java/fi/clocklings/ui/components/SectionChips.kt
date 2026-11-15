package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import kotlin.math.roundToInt

/** Scrollable section tabs; selection follows the actual section visible in the page. */
@Composable
fun SectionChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val positions = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    var viewport by remember { mutableIntStateOf(0) }
    val reduced = rememberReduceMotion()
    val selected = positions[selectedIndex]
    LaunchedEffect(selectedIndex, selected, viewport, reduced) {
        val (left, width) = selected ?: return@LaunchedEffect
        val target =
            when {
                left < scroll.value -> left
                left + width > scroll.value + viewport -> left + width - viewport
                else -> scroll.value
            }.coerceIn(0, scroll.maxValue)
        if (reduced) scroll.scrollTo(target) else scroll.animateScrollTo(target)
    }
    Row(
        modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it.width }
            .horizontalScroll(scroll)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selectedIndex
            Row(
                Modifier
                    .onGloballyPositioned { positions[index] = it.positionInParent().x.roundToInt() to it.size.width }
                    .minimumInteractiveComponentSize()
                    .clip(SkShapes.Chip)
                    .background(if (on) SkColors.Primary else SkColors.Surface)
                    .selectable(on, role = Role.Tab) { onSelect(index) }
                    .heightIn(min = SkDimens.ChipHeight)
                    .padding(
                        start = if (on) SkDimens.GapOptions else SkDimens.Gap,
                        end = SkDimens.Gap,
                        top = SkDimens.GapS,
                        bottom = SkDimens.GapS,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            ) {
                if (on) SkIcon(SkIcons.Check, SkDimens.StarSmall, tint = SkColors.OnPrimary)
                WordSafeText(label, SkType.Chip.copy(color = if (on) SkColors.OnPrimary else SkColors.Ink))
            }
        }
    }
}
