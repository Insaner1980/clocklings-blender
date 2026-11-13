package fi.clocklings.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/**
 * "3D-nappi": täytetty pinta, jonka alla on tummempi kiinteä varjo. Painettaessa pinta
 * painuu alas varjon päälle. While pressed it also shrinks to [pressScale]; 1f keeps the size, for a
 * surface whose content must stay where it stands. With [decorativeContent] the content's semantics are
 * dropped: the surface node itself then carries the label, role and state set in [modifier], instead of
 * leaving the label and role on generated child nodes of the accessibility tree.
 */
@Composable
fun DepthSurface(
    onClick: () -> Unit,
    fill: Color,
    shadow: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    depth: Dp = SkDimens.ButtonDepth,
    pressedDepth: Dp = SkDimens.ButtonDepthPressed,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    dimWhenDisabled: Boolean = true,
    role: Role = Role.Button,
    pressScale: Float = SkMotion.PRESS_SCALE,
    decorativeContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val reduced = rememberReduceMotion()
    val scale by animateFloatAsState(
        if (pressed && enabled && !reduced) pressScale else 1f,
        animationSpec = if (reduced) snap() else SkMotion.PressSpring,
        label = "press",
    )
    val shift = if (pressed && enabled) depth - pressedDepth else 0.dp
    val visibleBorder = if (focused && enabled) BorderStroke(SkDimens.Border, SkColors.Ink) else border
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.alpha(if (enabled || !dimWhenDisabled) 1f else DISABLED_ALPHA)
            .offset { IntOffset(0, shift.roundToPx()) }
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                translate(0f, (depth - shift).toPx()) { drawOutline(outline, shadow) }
            }.background(fill, shape)
            .then(if (visibleBorder != null) Modifier.border(visibleBorder, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = role, onClick = onClick)
            // Inside the click semantics, so it clears only the content's and keeps the click, role and label.
            .then(if (decorativeContent) Modifier.clearAndSetSemantics { } else Modifier),
        propagateMinConstraints = true,
    ) { content() }
}

private const val DISABLED_ALPHA = 0.5f

enum class ButtonKind { NORMAL, PRIMARY, GREEN }

/** Tavallinen nappi. */
@Composable
fun SkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.NORMAL,
    small: Boolean = false,
    enabled: Boolean = true,
    icon: SvgIcon? = null,
) {
    val (fill, shadow) =
        when (kind) {
            ButtonKind.NORMAL -> SkColors.BtnFill to SkColors.BtnShadow
            ButtonKind.PRIMARY -> SkColors.BtnPrimaryFill to SkColors.BtnPrimaryShadow
            ButtonKind.GREEN -> SkColors.BtnGreenFill to SkColors.BtnGreenShadow
        }
    DepthSurface(onClick, fill, shadow, SkShapes.Button, modifier, enabled = enabled) {
        Row(
            Modifier
                .heightIn(min = SkDimens.ChildTarget)
                .padding(if (small) PaddingValues(16.dp, 13.dp) else PaddingValues(16.dp, 15.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS, Alignment.CenterHorizontally),
        ) {
            icon?.let { SkIcon(it, SkDimens.ActionIcon) }
            WordSafeText(text, if (small) SkType.ButtonSmall else SkType.Button, Modifier.weight(1f, fill = false))
        }
    }
}

/**
 * Two actions of a bottom bar: side by side when each label fits its half on one line at the current width and
 * font scale, otherwise stacked full width. [start] comes first in both arrangements. Without [endLabel] the
 * row keeps its end half empty, so [start] stays where it is when [end] appears.
 */
@Composable
fun ActionPair(
    startLabel: String,
    endLabel: String?,
    start: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    end: @Composable (Modifier) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val currentStart by rememberUpdatedState(start)
    val currentEnd by rememberUpdatedState(end)
    val startContent = remember { movableContentOf<Modifier> { currentStart(it) } }
    val endContent = remember { movableContentOf<Modifier> { currentEnd(it) } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val halfWidth = (maxWidth - SkDimens.GapM) / 2
        // An SkButton pads its label by a gutter on each side.
        val labelWidth = with(density) { (halfWidth - SkDimens.Gutter * 2).toPx() }
        val labels = listOfNotNull(startLabel, endLabel)
        val stack = labels.any { measurer.measure(it, SkType.Button).size.width > labelWidth }
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                startContent(Modifier.fillMaxWidth())
                if (endLabel != null) endContent(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                startContent(Modifier.weight(1f).fillMaxHeight())
                if (endLabel != null) endContent(Modifier.weight(1f).fillMaxHeight()) else Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Segmented choice: a soft track with the chosen segment as a raised white thumb. */
@Composable
fun SegmentedControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(SkColors.SegmentTrack, SkShapes.Pill)
            .padding(SkDimens.SegmentInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.SegmentInset),
    ) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = SkType.Chip.copy(color = SkColors.InkSoft),
                modifier = Modifier.padding(start = 10.dp, end = 4.dp),
            )
        }
        options.forEachIndexed { i, option ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .then(if (label.isEmpty()) Modifier.weight(1f) else Modifier)
                    .minimumInteractiveComponentSize()
                    .semantics { selected = on }
                    .then(if (on) Modifier.cardLift().background(SkColors.Surface, SkShapes.Pill) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .padding(horizontal = SkDimens.GapS, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { WordSafeText(option, SkType.Segment.copy(color = if (on) SkColors.Ink else SkColors.InkSoft)) }
        }
    }
}

/** A thin soft shadow under the selected segment thumb. */
private fun Modifier.cardLift(): Modifier =
    drawBehind {
        val outline = SkShapes.Pill.createOutline(size, layoutDirection, this)
        translate(0f, SkDimens.SegmentLift.toPx()) { drawOutline(outline, SkColors.SegmentShadow) }
    }

/** A child-sized activity tile; supporting copy remains available to screen readers. */
@Composable
fun BigMenuButton(
    title: String,
    subtitle: String,
    icon: SvgIcon,
    fill: Color,
    shadow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DepthSurface(
        onClick,
        fill,
        shadow,
        SkShapes.Card,
        modifier.semantics { contentDescription = "$title. $subtitle" },
        depth = SkDimens.BigButtonDepth,
        pressedDepth = 2.dp,
    ) {
        Column(
            Modifier.heightIn(min = SkDimens.MenuTileHeight).padding(SkDimens.GapM),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SkIcon(icon, SkDimens.MenuIcon, Modifier.padding(bottom = 4.dp))
            WordSafeText(title, SkType.BigButton)
        }
    }
}
