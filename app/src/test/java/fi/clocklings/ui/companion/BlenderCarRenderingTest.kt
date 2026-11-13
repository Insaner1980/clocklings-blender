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
class BlenderCarRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun bodyRockAndBlinkLeaveWheelHubsStill() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderCarRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("car")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(CompanionKind.CAR), pose.value) }
                }
            }
        }
        val baseline = pixels()
        for (frame in listOf(
            CompanionPose(partDeg = 18f),
            CompanionPose(blink = 1f),
        )) {
            compose.runOnIdle { pose.value = frame }
            val changed = pixels()
            assertTrue(
                "Static part at $frame",
                baseline.values.indices.count { baseline.values[it] != changed.values[it] } > 100,
            )
            for (y in 235 until 255) {
                for (x in 70 until 80) {
                    val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                    assertTrue(
                        "Wheel hub moved at $x/$y with $frame",
                        baseline.values[index] == changed.values[index],
                    )
                }
            }
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(pixels().values))
    }

    @Test
    fun sportRimsReplaceBothWheelHubs() {
        val outfit = mutableStateOf(Outfit.default(CompanionKind.CAR))
        compose.setContent {
            val rig = rememberBlenderCarRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("car")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, outfit.value, CompanionPose()) }
                }
            }
        }
        val baseline = pixels()
        compose.runOnIdle { outfit.value = outfit.value.with(Slot.WHEELS, "car-wheels-sport") }
        val held = pixels()
        val changedPixels = baseline.values.indices.count { baseline.values[it] != held.values[it] }
        assertTrue("Sport rims are invisible", changedPixels > 100)
        for ((x, y) in listOf(22 to 228, 75 to 246)) {
            val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
            assertTrue("Sport rim missing at $x/$y", baseline.values[index] != held.values[index])
        }
    }

    private fun pixels(): CarPixels {
        val image = compose.captureRenderedImage("car")
        val map = image.toPixelMap()
        return CarPixels(
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

private data class CarPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
)
