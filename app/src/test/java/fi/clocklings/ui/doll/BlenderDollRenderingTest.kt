package fi.clocklings.ui.doll

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
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.companion.CompanionPose
import fi.clocklings.ui.companion.captureRenderedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderDollRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun blinkAndWaveKeepTheDressStill() {
        val pose = mutableStateOf(CompanionPose())
        compose.setContent {
            val rig = rememberBlenderDollRig()
            Canvas(Modifier.size(200.dp, 340.dp).testTag("doll")) {
                scale(size.width / 200f, size.height / 340f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.Default, pose.value) }
                }
            }
        }
        val baseline = pixels()
        for (frame in listOf(CompanionPose(armDeg = 30f), CompanionPose(blink = 1f))) {
            compose.runOnIdle { pose.value = frame }
            val changed = pixels()
            assertTrue(
                "Static part at $frame",
                baseline.values.indices.count { baseline.values[it] != changed.values[it] } > 100,
            )
            assertEquals("Dress moved at $frame", baseline.at(100, 195), changed.at(100, 195))
            assertEquals("Nose moved at $frame", baseline.at(100, 106), changed.at(100, 106))
        }
        compose.runOnIdle { pose.value = CompanionPose() }
        assertTrue("Rest pose did not return", baseline.values.contentEquals(pixels().values))
    }

    @Test
    fun everyWardrobeChoiceIsDistinctAndOverallsReplaceTheHem() {
        val outfit = mutableStateOf(Outfit.Default)
        compose.setContent {
            val rig = rememberBlenderDollRig()
            Canvas(Modifier.size(200.dp, 340.dp).testTag("doll")) {
                scale(size.width / 200f, size.height / 340f, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, outfit.value, CompanionPose()) }
                }
            }
        }
        val baseline = pixels()
        val hashes = mutableSetOf(baseline.values.contentHashCode())
        for (item in Wardrobe.itemsFor(CompanionKind.HUMAN).filter { it.star > 0 }) {
            compose.runOnIdle { outfit.value = Outfit.Default.with(item.slot, item.id) }
            val changed = pixels()
            assertTrue(
                "Invisible item ${item.id}",
                baseline.values.indices.count { baseline.values[it] != changed.values[it] } > 100,
            )
            assertTrue("Duplicate artwork ${item.id}", hashes.add(changed.values.contentHashCode()))
        }
        compose.runOnIdle { outfit.value = Outfit.Default.with(Slot.DRESS, "dress-overalls") }
        val overalls = pixels()
        assertTrue("Hem probe misses the default dress", baseline.at(100, 253) != baseline.at(0, 253))
        assertEquals("Yellow dress hem remains between the legs", overalls.at(0, 253), overalls.at(100, 253))
    }

    private fun pixels(): DollPixels {
        val image = compose.captureRenderedImage("doll")
        val map = image.toPixelMap()
        return DollPixels(
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

private data class DollPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
) {
    fun at(
        x: Int,
        y: Int,
    ): Int = values[(y * height / 340) * width + x * width / 200]
}
