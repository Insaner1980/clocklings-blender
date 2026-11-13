package fi.clocklings.ui.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Pixel checks exercise the new raster joints, not only the shared motion calculations. */
@RunWith(RobolectricTestRunner::class)
class BlenderBunnyRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headEarsAndBlinkAnimateIndependentlyOfThePaws() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderBunnyRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("bunny")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(CompanionKind.BUNNY), pose.value) }
                }
            }
        }
        val baseline = pixels()
        for (frame in listOf(
            CompanionPose(headTiltDeg = 12f),
            CompanionPose(partDeg = 18f),
            CompanionPose(blink = 1f),
        )) {
            compose.runOnIdle { pose.value = frame }
            val changed = pixels()
            assertTrue(
                "Static part at $frame",
                baseline.values.indices.count { baseline.values[it] != changed.values[it] } > 100,
            )
            if (frame.partDeg != 0f) {
                for (side in listOf(0 until 60, 180 until 240)) {
                    val changedPixels =
                        (60 until 230).sumOf { y ->
                            side.count { x ->
                                val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                                baseline.values[index] != changed.values[index]
                            }
                        }
                    assertTrue("Ear did not move in $side", changedPixels > 100)
                }
            }
            for (y in 225 until 240) {
                for (x in 110 until 130) {
                    val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                    assertTrue(
                        "Paws moved at $x/$y with $frame",
                        baseline.values[index] == changed.values[index],
                    )
                }
            }
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(pixels().values))
    }

    @Test
    fun pawsCloseOverTheCarrot() {
        val outfit = mutableStateOf(Outfit.default(CompanionKind.BUNNY))
        compose.setContent {
            val rig = rememberBlenderBunnyRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("bunny")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, outfit.value, CompanionPose()) }
                }
            }
        }
        val baseline = pixels()
        compose.runOnIdle { outfit.value = outfit.value.with(Slot.EXTRA, "bunny-extra-carrot") }
        val held = pixels()
        val changedPixels = baseline.values.indices.count { baseline.values[it] != held.values[it] }
        assertTrue("Carrot is invisible", changedPixels > 100)
        for ((x, y) in listOf(110 to 235, 114 to 239, 132 to 235, 128 to 239)) {
            val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
            assertTrue("Carrot covers the paw at $x/$y", baseline.values[index] == held.values[index])
        }
    }

    private fun pixels(): BunnyPixels {
        val image = compose.captureRenderedImage("bunny")
        val map = image.toPixelMap()
        return BunnyPixels(
            image.width,
            image.height,
            IntArray(image.width * image.height) {
                map[
                    it % image.width, it /
                        image.width,
                ].toArgb()
            },
        )
    }

    @Test
    fun modeledTiaraFollowsHeadTiltWhileRearWingsStayWithTheBody() {
        val pose = mutableStateOf(CompanionPose())
        val outfit =
            Outfit
                .default(CompanionKind.BUNNY)
                .with(Slot.HEAD, "bunny-head-tiara")
                .with(Slot.EXTRA, "bunny-extra-wings")
        compose.setContent {
            val rig = rememberBlenderBunnyRig(CompanionColor.PURPLE)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("bunny")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.PURPLE, outfit, pose.value) }
                }
            }
        }
        val baseline = pixels()
        compose.runOnIdle { pose.value = CompanionPose(headTiltDeg = 12f) }
        val tilted = pixels()

        fun changes(
            xs: IntRange,
            ys: IntRange,
        ): Int =
            ys.sumOf { y ->
                xs.count { x ->
                    val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                    baseline.values[index] != tilted.values[index]
                }
            }
        assertTrue("Tiara did not follow the head", changes(75..165, 10..55) > 20)
        assertTrue("Rear wings moved with the head", changes(10..55, 245..275) == 0)
    }
}

private data class BunnyPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
)
