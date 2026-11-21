package fi.clocklings.ui.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import org.junit.Assert.assertTrue

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

internal class BlenderRigRendering(
    private val compose: ComposeContentTestRule,
    private val tag: String,
    private val width: Int = 240,
    private val height: Int = 300,
    private val rememberRig: @Composable (CompanionColor) -> CompanionRig,
) {
    fun setContent(
        outfit: () -> Outfit,
        pose: () -> CompanionPose = { CompanionPose() },
        color: CompanionColor = CompanionColor.ORIGINAL,
    ) {
        compose.setContent {
            val rig = rememberRig(color)
            Canvas(Modifier.size(width.dp, height.dp).testTag(tag)) {
                scale(size.width / width, size.height / height, Offset.Zero) {
                    with(rig) { draw(color, outfit(), pose()) }
                }
            }
        }
    }

    fun capture(): RenderedPixels {
        val image = compose.captureRenderedImage(tag)
        val map = image.toPixelMap()
        val values = IntArray(image.width * image.height) { map[it % image.width, it / image.width].toArgb() }
        return RenderedPixels(image.width, image.height, width, height, values)
    }

    fun captureBlinkFrames(
        outfit: Outfit,
        vararg blinkFractions: Float,
    ): List<RenderedPixels> {
        val pose = mutableStateOf(CompanionPose())
        setContent(outfit = { outfit }, pose = { pose.value })
        return buildList {
            add(capture())
            for (blink in blinkFractions) {
                compose.runOnIdle { pose.value = CompanionPose(blink = blink) }
                add(capture())
            }
        }
    }

    fun captureGroundedItemHop(
        original: Outfit,
        itemId: String,
    ): Triple<RenderedPixels, RenderedPixels, RenderedPixels> {
        val outfit = mutableStateOf(original)
        val pose = mutableStateOf(CompanionPose())
        setContent(outfit = { outfit.value }, pose = { pose.value })
        val bare = capture()
        compose.runOnIdle { outfit.value = original.with(Slot.EXTRA, itemId) }
        val rest = capture()
        compose.runOnIdle { pose.value = CompanionPose(lift = CompanionMotion.HOP_HEIGHT) }
        return Triple(bare, rest, capture())
    }

    fun assertAnimatedFrames(
        outfit: Outfit,
        frames: List<CompanionPose> = AnimalAnimationFrames,
        assertFrame: (CompanionPose, RenderedPixels, RenderedPixels) -> Unit,
    ) {
        val pose = mutableStateOf(CompanionPose())
        setContent(outfit = { outfit }, pose = { pose.value })
        val baseline = capture()
        for (frame in frames) {
            compose.runOnIdle { pose.value = frame }
            val changed = capture()
            assertTrue("Static part at $frame", baseline.countDifferences(changed) > 100)
            assertFrame(frame, baseline, changed)
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(capture().values))
    }

    fun captureOutfitChange(
        original: Outfit,
        slot: Slot,
        itemId: String,
    ): Pair<RenderedPixels, RenderedPixels> {
        val outfit = mutableStateOf(original)
        setContent(outfit = { outfit.value })
        val baseline = capture()
        compose.runOnIdle { outfit.value = original.with(slot, itemId) }
        return baseline to capture()
    }
}

private val AnimalAnimationFrames =
    listOf(CompanionPose(headTiltDeg = 12f), CompanionPose(partDeg = 18f), CompanionPose(blink = 1f))

internal class RenderedPixels(
    private val width: Int,
    private val height: Int,
    private val gridWidth: Int,
    private val gridHeight: Int,
    val values: IntArray,
) {
    fun at(
        x: Int,
        y: Int,
    ): Int = values[(y * height / gridHeight) * width + x * width / gridWidth]

    fun countDifferences(other: RenderedPixels): Int = values.indices.count { values[it] != other.values[it] }

    fun countDifferencesIn(
        other: RenderedPixels,
        xs: IntRange,
        ys: IntRange,
    ): Int = ys.sumOf { y -> xs.count { x -> at(x, y) != other.at(x, y) } }

    fun assertUnchanged(
        other: RenderedPixels,
        xs: IntRange,
        ys: IntRange,
        message: (Int, Int) -> String,
    ) {
        for (y in ys) {
            for (x in xs) {
                assertTrue(message(x, y), at(x, y) == other.at(x, y))
            }
        }
    }
}
