package fi.clocklings.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Reports the actual scrolling viewport, including the short-window header fallback. */
internal fun Modifier.scrollViewport(
    active: Boolean,
    scroll: ScrollState,
    onTop: ((Float) -> Unit)?,
): Modifier = if (active) onGloballyPositioned { onTop?.invoke(it.positionInRoot().y) }.verticalScroll(scroll) else this
