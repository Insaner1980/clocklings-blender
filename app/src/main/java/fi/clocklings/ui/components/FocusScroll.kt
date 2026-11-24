package fi.clocklings.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import fi.clocklings.ui.theme.SkDimens

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun rememberFocusScrollSpec(bottom: Dp): BringIntoViewSpec {
    val parent = LocalBringIntoViewSpec.current
    val density = LocalDensity.current
    val topPixels = with(density) { SkDimens.ScrollFadeTop.toPx() }
    val bottomPixels = with(density) { (bottom + SkDimens.ScrollFade).toPx() }
    return remember(parent, topPixels, bottomPixels) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float =
                parent.calculateScrollDistance(
                    offset - topPixels,
                    size,
                    (containerSize - topPixels - bottomPixels).coerceAtLeast(0f),
                )
        }
    }
}

/** Requests once for each new feedback state. A user drag takes priority over the request. */
@Composable
internal fun Modifier.bringFeedbackIntoView(
    feedback: String,
    scrollState: ScrollState? = null,
): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val dragged = scrollState?.interactionSource?.collectIsDraggedAsState()
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(feedback, placed) {
        if (placed && dragged?.value != true) requester.bringIntoView()
    }
    return bringIntoViewRequester(requester).onPlaced { placed = true }
}
