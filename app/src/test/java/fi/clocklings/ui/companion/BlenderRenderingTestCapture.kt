package fi.clocklings.ui.companion

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/** Robolectric can return an empty window before the first native Canvas frame is drawn. */
internal fun ComposeContentTestRule.captureRenderedImage(tag: String): ImageBitmap {
    var rendered: ImageBitmap? = null
    waitUntil(5_000) {
        val image = onNodeWithTag(tag).captureToImage()
        val pixels = image.toPixelMap()
        val background = pixels[0, 0]
        val painted =
            (1..7).any { y ->
                (1..7).any { x -> pixels[x * (image.width - 1) / 8, y * (image.height - 1) / 8] != background }
            }
        if (painted) {
            rendered = image
        } else {
            runOnIdle {
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .forEach { it.window.decorView.invalidate() }
            }
            mainClock.advanceTimeByFrame()
        }
        painted
    }
    return checkNotNull(rendered)
}
