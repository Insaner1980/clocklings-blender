package fi.clocklings.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

/** Reports the actual scrolling viewport, including the short-window header fallback. */
internal fun Modifier.scrollViewport(
    active: Boolean,
    scroll: ScrollState,
    onTop: ((Float) -> Unit)?,
): Modifier = if (active) onGloballyPositioned { onTop?.invoke(it.positionInRoot().y) }.verticalScroll(scroll) else this

@Composable
internal fun restoredScrollInsetsReady(scrollState: ScrollState): Boolean {
    var ready by remember(scrollState) { mutableStateOf(scrollState.value == 0) }
    if (ready) return true
    val density = LocalDensity.current
    val drawing = WindowInsets.safeDrawing
    val top = drawing.getTop(density)
    val bottom = drawing.getBottom(density)
    val platform =
        ViewCompat
            .getRootWindowInsets(LocalView.current)
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
    val insetsReady = platform == null || (top >= platform.top && bottom >= platform.bottom)
    if (insetsReady) SideEffect { ready = true }
    return insetsReady
}

/** Compare the preview's raw bottom with the scroll edge, including when it has been clipped out. */
@Stable
internal class PreviewScroll(
    private val scroll: ScrollState,
) {
    var viewportTop by mutableFloatStateOf(0f)
    private var previewBottom by mutableFloatStateOf(Float.POSITIVE_INFINITY)

    val showCompact by derivedStateOf { scroll.value > 0 && previewBottom <= viewportTop }

    fun positionPreview(coordinates: LayoutCoordinates) {
        previewBottom = coordinates.positionInRoot().y + coordinates.size.height
    }
}

@Composable
internal fun PinnedPreview(
    visible: Boolean,
    windowHeight: Dp,
    name: String,
    visual: @Composable (Dp) -> Unit,
) {
    val reduced = rememberReduceMotion()
    val height = min(SkDimens.PinnedPreviewSize, windowHeight * SkDimens.PINNED_PREVIEW_MAX_SHARE)
    AnimatedVisibility(
        visible,
        enter = if (reduced) EnterTransition.None else fadeIn(),
        exit = if (reduced) ExitTransition.None else fadeOut(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(max = height)
                .padding(horizontal = SkDimens.GapS)
                .testTag("pinned_preview"),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visual(height)
            WordSafeText(
                name,
                SkType.SmallSoft,
                Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
        }
    }
}
