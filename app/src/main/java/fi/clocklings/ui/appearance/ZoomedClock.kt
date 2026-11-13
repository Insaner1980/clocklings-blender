package fi.clocklings.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes

/**
 * 12:05 keeps both hand tips inside the zoomed top window and clear of the numerals 11 and 1; at 11:05 the
 * hour tip would sit on the window's bottom edge, over the "11".
 */
internal const val ZOOM_TIME = 5

/**
 * A close-up of the top of a full-size clock: the frame, the numbers 11, 12 and 1 and the hand tips
 * are exactly what the custom options change, so the difference between options is visible.
 */
@Composable
internal fun ZoomedClock(appearance: ClockAppearance) {
    Box(
        Modifier
            .size(SkDimens.ItemWindow * ZOOM_WINDOW_ASPECT, SkDimens.ZoomWindowHeight)
            .clip(SkShapes.Caption)
            .background(SkColors.ItemWindow),
    ) {
        ClockFace(
            ClockCommand(ZOOM_TIME),
            Modifier
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .offset(y = SkDimens.ZoomClockShift)
                .requiredSize(SkDimens.ZoomClock),
            describeTime = false,
            appearance = appearance,
        )
    }
}

internal const val ZOOM_WINDOW_ASPECT = 1.2f
