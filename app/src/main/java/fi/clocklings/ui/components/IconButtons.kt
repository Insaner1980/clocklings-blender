package fi.clocklings.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes

@Composable
fun RoundIconButton(
    icon: SvgIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    large: Boolean = false,
    enabled: Boolean = true,
    tonal: Boolean = false,
) {
    PressSurface(
        onClick,
        if (selected == true) {
            SkColors.HelpOnFill
        } else if (tonal || large) {
            SkColors.TonalFill
        } else {
            SkColors.Surface
        },
        SkShapes.Pill,
        modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .size(if (large) SkDimens.ChildTarget else SkDimens.IconButton)
            .semantics {
                this.contentDescription = contentDescription
                if (selected != null) this.selected = selected
            },
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            SkIcon(icon, if (large) SkDimens.LargeActionIcon else SkDimens.ActionIcon)
        }
    }
}

/** Selected state belongs to the containing control's semantics. */
@Composable
fun SelectionBadge(
    modifier: Modifier = Modifier,
    color: Color = SkColors.Primary,
) {
    val reduced = rememberReduceMotion()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        if (visible || reduced) 1f else SkMotion.BADGE_START_SCALE,
        animationSpec = if (reduced) snap() else SkMotion.PopSpring,
        label = "selection",
    )
    Box(
        modifier
            .size(SkDimens.SelectionBadge)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.background(color, SkShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        SkIcon(SkIcons.Check, SkDimens.StarSmall, tint = SkColors.OnPrimary)
    }
}
