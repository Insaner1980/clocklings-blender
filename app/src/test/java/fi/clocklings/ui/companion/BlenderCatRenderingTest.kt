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
class BlenderCatRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderCatRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("cat")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(CompanionKind.CAT), pose.value) }
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
            for (y in 205 until 235) {
                for (x in 110 until 130) {
                    val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                    assertTrue(
                        "Chest moved at $x/$y with $frame",
                        baseline.values[index] == changed.values[index],
                    )
                }
            }
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(pixels().values))
    }

    @Test
    fun modeledHatFollowsTheHeadWhileButterflyWingsStayOnTheBody() {
        val pose = mutableStateOf(CompanionPose())
        val outfit = Outfit().with(Slot.HEAD, "cat-head-sailor-hat").with(Slot.EXTRA, "cat-extra-wings")
        compose.setContent {
            val rig = rememberBlenderCatRig(CompanionColor.BLUE)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("cat")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.BLUE, outfit, pose.value) }
                }
            }
        }
        val rest = pixels()
        compose.runOnIdle { pose.value = CompanionPose(headTiltDeg = 12f) }
        val tilted = pixels()
        val changedHatPixels =
            (20 until 70).sumOf { y ->
                (50 until 190).count { x ->
                    val index = (y * rest.height / 300) * rest.width + x * rest.width / 240
                    rest.values[index] != tilted.values[index]
                }
            }
        assertTrue("Modeled sailor hat did not follow the head", changedHatPixels > 100)
        for (x in (10 until 40) + (200 until 230)) {
            for (y in 185 until 220) {
                val index = (y * rest.height / 300) * rest.width + x * rest.width / 240
                assertTrue("Butterfly wing followed the head at $x/$y", rest.values[index] == tilted.values[index])
            }
        }
    }

    private fun pixels(): CatPixels {
        val image = compose.captureRenderedImage("cat")
        val map = image.toPixelMap()
        return CatPixels(
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
}

private data class CatPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
)
