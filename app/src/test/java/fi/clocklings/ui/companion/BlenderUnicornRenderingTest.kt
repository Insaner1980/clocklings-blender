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
class BlenderUnicornRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheHooves() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderUnicornRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("unicorn")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(CompanionKind.UNICORN), pose.value) }
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
                val changedPixels =
                    (150 until 230).sumOf { y ->
                        (185 until 220).count { x ->
                            val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                            baseline.values[index] != changed.values[index]
                        }
                    }
                assertTrue("Tail did not move", changedPixels > 100)
            }
            for (y in 265 until 275) {
                for (x in 100 until 115) {
                    val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
                    assertTrue(
                        "Hooves moved at $x/$y with $frame",
                        baseline.values[index] == changed.values[index],
                    )
                }
            }
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(pixels().values))
    }

    @Test
    fun sparkleBootsCoverAllFourHooves() {
        val outfit = mutableStateOf(Outfit.default(CompanionKind.UNICORN))
        compose.setContent {
            val rig = rememberBlenderUnicornRig(CompanionColor.ORIGINAL)
            Canvas(Modifier.size(240.dp, 300.dp).testTag("unicorn")) {
                scale(size.width / 240f, size.height / 300f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, outfit.value, CompanionPose()) }
                }
            }
        }
        val baseline = pixels()
        compose.runOnIdle { outfit.value = outfit.value.with(Slot.EXTRA, "unicorn-extra-boots") }
        val held = pixels()
        val changedPixels = baseline.values.indices.count { baseline.values[it] != held.values[it] }
        assertTrue("Sparkle boots are invisible", changedPixels > 100)
        for ((x, y) in listOf(82 to 266, 136 to 260, 106 to 272, 167 to 267)) {
            val index = (y * baseline.height / 300) * baseline.width + x * baseline.width / 240
            assertTrue("Boot missing at $x/$y", baseline.values[index] != held.values[index])
        }
    }

    private fun pixels(): UnicornPixels {
        val image = compose.captureRenderedImage("unicorn")
        val map = image.toPixelMap()
        return UnicornPixels(
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

private data class UnicornPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
)
