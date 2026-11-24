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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** Flat pressable control with ripple, focus indication and optional decorative child semantics. */
@Composable
fun PressSurface(
    onClick: () -> Unit,
    fill: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
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
    val visibleBorder = if (focused && enabled) BorderStroke(SkDimens.Border, SkColors.Ink) else border
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.alpha(if (enabled || !dimWhenDisabled) 1f else SkColors.DISABLED_CONTROL_ALPHA)
            .clip(shape)
            .background(fill)
            .then(if (visibleBorder != null) Modifier.border(visibleBorder, shape) else Modifier)
            .clickable(
                interaction,
                indication = ripple(color = SkColors.Ink),
                enabled = enabled,
                role = role,
                onClick = onClick,
            ).then(if (decorativeContent) Modifier.clearAndSetSemantics { } else Modifier),
        propagateMinConstraints = true,
    ) { content() }
}

enum class ButtonKind { PRIMARY, SECONDARY, TONAL, CONTAINER, DANGER }

/** A pill action whose label may wrap without reducing its touch target. */
@Composable
fun SkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.SECONDARY,
    small: Boolean = false,
    enabled: Boolean = true,
    icon: SvgIcon? = null,
    trailingIcon: SvgIcon? = null,
) {
    val (fill, foreground) =
        when (kind) {
            ButtonKind.PRIMARY -> {
                if (enabled) {
                    SkColors.Primary to SkColors.OnPrimary
                } else {
                    SkColors.PrimaryContainer to SkColors.PrimaryDisabledContent
                }
            }

            ButtonKind.SECONDARY -> {
                SkColors.Surface to SkColors.Ink
            }

            ButtonKind.TONAL -> {
                SkColors.TonalFill to SkColors.Ink
            }

            ButtonKind.CONTAINER -> {
                SkColors.PrimaryContainer to SkColors.Ink
            }

            ButtonKind.DANGER -> {
                SkColors.DangerContainer to SkColors.Danger
            }
        }
    PressSurface(
        onClick,
        fill,
        SkShapes.Button,
        modifier,
        enabled = enabled,
        dimWhenDisabled = kind != ButtonKind.PRIMARY,
    ) {
        Row(
            Modifier
                .heightIn(min = if (small) SkDimens.ButtonHeightSmall else SkDimens.ButtonHeight)
                .padding(horizontal = SkDimens.ButtonPadX, vertical = SkDimens.GapM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapOptions, Alignment.CenterHorizontally),
        ) {
            icon?.let { SkIcon(it, SkDimens.ButtonIcon, tint = foreground) }
            WordSafeText(
                text,
                (if (small) SkType.ButtonSmall else SkType.Button).copy(color = foreground),
                Modifier.weight(1f, fill = false),
            )
            trailingIcon?.let { SkIcon(it, SkDimens.ButtonIcon, tint = foreground) }
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
    compactStart: (@Composable (Modifier) -> Unit)? = null,
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
        // Reserve the same horizontal padding as SkButton.
        val labelWidth = with(density) { (halfWidth - SkDimens.ButtonPadX * 2).toPx() }
        val labels = listOfNotNull(startLabel, endLabel)
        val stack = labels.any { measurer.measure(it, SkType.Button).size.width > labelWidth }
        if (stack && compactStart != null && density.fontScale > SkDimens.MAX_ROW_FONT_SCALE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                compactStart(Modifier)
                if (endLabel != null) endContent(Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            }
        } else if (stack) {
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

/** A pill track with a violet selected segment. */
@Composable
fun SegmentedControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.background(SkColors.Surface, SkShapes.Pill).padding(SkDimens.GapXS).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapXS),
    ) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = SkType.Chip,
                modifier = Modifier.padding(horizontal = SkDimens.GapS),
            )
        }
        options.forEachIndexed { i, option ->
            val on = i == selectedIndex
            PressSurface(
                { onSelect(i) },
                if (on) SkColors.Primary else Color.Transparent,
                SkShapes.Pill,
                Modifier.then(if (label.isEmpty()) Modifier.weight(1f) else Modifier).semantics { selected = on },
                role = Role.RadioButton,
            ) {
                Box(
                    Modifier
                        .heightIn(
                            min = SkDimens.IconButton,
                        ).padding(horizontal = SkDimens.GapS, vertical = SkDimens.GapHalf),
                    contentAlignment = Alignment.Center,
                ) {
                    WordSafeText(option, SkType.Segment.copy(color = if (on) SkColors.OnPrimary else SkColors.Ink))
                }
            }
        }
    }
}

/** A child-sized activity tile with a title and supporting description. */
@Composable
fun MenuTile(
    title: String,
    subtitle: String,
    icon: SvgIcon,
    fill: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactHeight: Boolean = false,
) {
    PressSurface(onClick, fill, SkShapes.Tile, modifier.semantics { contentDescription = "$title. $subtitle" }) {
        Column(
            Modifier
                .heightIn(min = if (compactHeight) SkDimens.MenuTileCompactHeight else SkDimens.MenuTileHeight)
                .padding(horizontal = SkDimens.Gap, vertical = if (compactHeight) SkDimens.GapS else SkDimens.Gap),
            verticalArrangement = if (compactHeight) Arrangement.SpaceBetween else Arrangement.spacedBy(SkDimens.GapM),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                Modifier
                    .size(if (compactHeight) SkDimens.TileIconBadgeCompact else SkDimens.TileIconBadge)
                    .background(SkColors.Surface, SkShapes.Pill),
                contentAlignment = Alignment.Center,
            ) {
                SkIcon(icon, SkDimens.MenuIcon)
            }
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapXS)) {
                WordSafeText(title, SkType.BigButton, textAlign = TextAlign.Start)
                WordSafeText(
                    subtitle,
                    SkType.SmallSoft,
                    Modifier.clearAndSetSemantics { },
                    textAlign = TextAlign.Start,
                    maxLines = if (compactHeight) 2 else Int.MAX_VALUE,
                )
            }
        }
    }
}
