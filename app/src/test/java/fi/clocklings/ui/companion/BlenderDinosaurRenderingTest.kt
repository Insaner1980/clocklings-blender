package fi.clocklings.ui.companion

import android.graphics.Color
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
class BlenderDinosaurRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderDinosaurRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("dinosaur")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(CompanionKind.DINOSAUR), pose.value) }
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
            for (y in 220 until 240) {
                for (x in 135 until 150) {
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
    fun waffleConeRemainsVisibleInFrontOfTheGrippingPaw() {
        val outfit = Outfit().with(Slot.EXTRA, "dinosaur-extra-ice-cream")
        compose.setContent {
            val rig = rememberBlenderDinosaurRig(CompanionColor.BLUE)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("dinosaur")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.BLUE, outfit, CompanionPose()) }
                }
            }
        }
        val held = pixels()
        val visibleCone =
            (195 until 203).sumOf { y ->
                (184 until 192).count { x ->
                    val index = (y * held.height / 300) * held.width + x * held.width / 240
                    val pixel = held.values[index]
                    Color.red(pixel) > 160 && Color.red(pixel) > Color.blue(pixel) + 40
                }
            }
        assertTrue("The gripping paw hides the waffle cone", visibleCone > 40)
    }

    private fun pixels(): DinosaurPixels {
        val image = compose.captureRenderedImage("dinosaur")
        val map = image.toPixelMap()
        return DinosaurPixels(
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

private data class DinosaurPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
)
