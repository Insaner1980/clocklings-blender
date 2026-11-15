package fi.clocklings.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import kotlin.math.max
import kotlin.math.min

/** A pressable colour drop: the colour itself is the button, ringed in white with a check when chosen. */
@Composable
fun SwatchTile(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pageColor: Color = SkColors.CompanionSurface,
) {
    PressSurface(
        onClick,
        color,
        SkShapes.Pill,
        modifier.size(SkDimens.ChildTarget).semantics {
            contentDescription = label
            this.selected = selected
        },
        border = if (selected) BorderStroke(SkDimens.SelectionBorder, SkColors.Primary) else null,
        role = Role.RadioButton,
        decorativeContent = true,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(
                            SkDimens.SelectionBorder,
                        ).border(SkDimens.SelectionBorder, pageColor, SkShapes.Pill),
                )
            }
            if (swatchNeedsOutline(color)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(
                            if (selected) SkDimens.SelectionBorder * 2 else 0.dp,
                        ).border(SkDimens.BorderThin, SkColors.SwatchOutline, SkShapes.Pill),
                )
            }
            if (selected) SkIcon(SkIcons.Check, SkDimens.ActionIcon, tint = SkColors.Ink)
        }
    }
}

/** Below this WCAG contrast ratio a swatch blends into the background and gets an outline. */
private const val SWATCH_MIN_CONTRAST = 1.3f

/** The flare term WCAG adds to both relative luminances in a contrast ratio. */
private const val WCAG_LUMINANCE_OFFSET = 0.05f

/** True when [color] is too close to [background] to be told apart without an outline. */
internal fun swatchNeedsOutline(
    color: Color,
    background: Color = SkColors.Bg,
): Boolean {
    val a = color.luminance()
    val b = background.luminance()
    return (max(a, b) + WCAG_LUMINANCE_OFFSET) / (min(a, b) + WCAG_LUMINANCE_OFFSET) < SWATCH_MIN_CONTRAST
}
