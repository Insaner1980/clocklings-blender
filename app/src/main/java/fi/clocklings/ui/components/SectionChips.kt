package fi.clocklings.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Section
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
    background: Color = Section.COMPANION.color,
) {
    val scroll = rememberScrollState()
    val positions = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    var viewport by remember { mutableIntStateOf(0) }
    val reduced = rememberReduceMotion()
    val selected = positions[selectedIndex]
    val density = LocalDensity.current
    val fade = with(density) { SkDimens.ScrollFade.roundToPx() }
    val maxChipWidth =
        if (viewport > 0) {
            with(density) { (viewport - fade * 2).coerceAtLeast(1).toDp() }
        } else {
            Dp.Unspecified
        }
    LaunchedEffect(selectedIndex, selected, viewport, fade, reduced) {
        val (left, width) = selected ?: return@LaunchedEffect
        val clearance = minOf(fade, ((viewport - width) / 2).coerceAtLeast(0))
        val start = left - clearance
        val end = left + width + clearance
        val target =
            when {
                start < scroll.value -> start
                end > scroll.value + viewport -> end - viewport
                else -> scroll.value
            }.coerceIn(0, scroll.maxValue)
        if (reduced) scroll.scrollTo(target) else scroll.animateScrollTo(target)
    }
    Row(
        modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it.width }
            .sectionChipFades(scroll, fade.toFloat(), background)
            .horizontalScroll(scroll)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            SectionChip(
                label,
                index == selectedIndex,
                { onSelect(index) },
                Modifier
                    .widthIn(max = maxChipWidth)
                    .onGloballyPositioned { positions[index] = it.positionInParent().x.roundToInt() to it.size.width },
            )
        }
    }
}

private fun Modifier.sectionChipFades(
    scroll: ScrollState,
    fade: Float,
    background: Color,
): Modifier =
    drawWithContent {
        drawContent()
        val width = minOf(fade, size.width / 2)
        val transparent = background.copy(alpha = 0f)
        if (scroll.canScrollBackward) {
            drawRect(
                Brush.horizontalGradient(listOf(background, transparent), endX = width),
                size = Size(width, size.height),
            )
        }
        if (scroll.canScrollForward) {
            val start = size.width - width
            drawRect(
                Brush.horizontalGradient(listOf(transparent, background), startX = start, endX = size.width),
                topLeft = Offset(start, 0f),
                size = Size(width, size.height),
            )
        }
    }

@Composable
private fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .clip(SkShapes.Chip)
            .background(if (selected) SkColors.Primary else SkColors.Surface)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = SkDimens.ChipHeight)
            .padding(
                start = if (selected) SkDimens.GapOptions else SkDimens.Gap,
                end = SkDimens.Gap,
                top = SkDimens.GapS,
                bottom = SkDimens.GapS,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        if (selected) SkIcon(SkIcons.Check, SkDimens.StarSmall, tint = SkColors.OnPrimary)
        WordSafeText(label, SkType.Chip.copy(color = if (selected) SkColors.OnPrimary else SkColors.Ink))
    }
}
