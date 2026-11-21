package fi.clocklings.ui.doll

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.companion.BlenderRigRendering
import fi.clocklings.ui.companion.CompanionPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderDollRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "doll", 200, 340) { rememberBlenderDollRig() }

    @Test
    fun blinkAndWaveKeepTheDressStill() {
        val frames = listOf(CompanionPose(armDeg = 30f), CompanionPose(blink = 1f))
        rendering.assertAnimatedFrames(Outfit.Default, frames) { frame, baseline, changed ->
            assertEquals("Dress moved at $frame", baseline.at(100, 195), changed.at(100, 195))
            assertEquals("Nose moved at $frame", baseline.at(100, 106), changed.at(100, 106))
        }
    }

    @Test
    fun blinkingCoversTheIrisAndClosesWithTwoLashes() {
        val (open, half, closed) = rendering.captureBlinkFrames(Outfit.Default, 0.45f, 1f)
        for (center in listOf(77, 123)) {
            open.assertUnchanged(half, center - 3..center + 3, 92..98) { x, y -> "Iris moved at $x/$y" }
            val changed = open.countDifferencesIn(closed, center - 9..center + 9, 82..102)
            assertTrue("Iris still visible at $center", changed > 190)
            val hasLash = (95..98).any { y -> android.graphics.Color.red(closed.at(center, y)) < 145 }
            assertTrue("Missing closed lash at $center", hasLash)
        }
    }

    @Test
    fun wingsExtendPastBothShouldersAndSkatesCastAShadowAtTheWheels() {
        val outfit = mutableStateOf(Outfit.Default)
        rendering.setContent(outfit = { outfit.value })
        val plain = rendering.capture()
        compose.runOnIdle { outfit.value = Outfit.Default.with(Slot.EXTRA, "extra-siivet") }
        val wings = rendering.capture()
        for (xs in listOf(12..25, 175..188)) {
            assertTrue("Upper wing hidden at $xs", plain.countDifferencesIn(wings, xs, 138..154) > 100)
        }
        compose.runOnIdle { outfit.value = Outfit.Default.with(Slot.SHOES, "shoes-roller-skates") }
        val skates = rendering.capture()
        assertTrue("Missing shadow between the wheels", plain.at(100, 331) != skates.at(100, 331))
    }

    @Test
    fun strawberryStrapContinuesOverTheShoulderToTheBag() {
        val outfit = Outfit.Default.with(Slot.EXTRA, "extra-strawberry-bag")
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { outfit }, pose = { pose.value })
        for (armAngle in listOf(0f, 95f)) {
            compose.runOnIdle { pose.value = CompanionPose(armDeg = armAngle) }
            val image = rendering.capture()
            for (y in 140..206) {
                val strapWidth =
                    (91..138).count { x ->
                        val pixel = image.at(x, y)
                        val red = pixel ushr 16 and 255
                        val green = pixel ushr 8 and 255
                        val blue = pixel and 255
                        red > blue + 32 && blue > green + 18
                    }
                assertTrue("Missing pink strap at height $y with arm at $armAngle", strapWidth >= 2)
            }
        }
    }

    @Test
    fun everyWardrobeChoiceIsDistinctAndOverallsReplaceTheHem() {
        val outfit = mutableStateOf(Outfit.Default)
        rendering.setContent(outfit = { outfit.value })
        val baseline = rendering.capture()
        val hashes = mutableSetOf(baseline.values.contentHashCode())
        for (item in Wardrobe.itemsFor(CompanionKind.HUMAN).filter { it.star > 0 }) {
            compose.runOnIdle { outfit.value = Outfit.Default.with(item.slot, item.id) }
            val changed = rendering.capture()
            assertTrue("Invisible item ${item.id}", baseline.countDifferences(changed) > 100)
            assertTrue("Duplicate artwork ${item.id}", hashes.add(changed.values.contentHashCode()))
        }
        compose.runOnIdle { outfit.value = Outfit.Default.with(Slot.DRESS, "dress-overalls") }
        val overalls = rendering.capture()
        assertTrue("Hem probe misses the default dress", baseline.at(100, 253) != baseline.at(0, 253))
        assertEquals("Yellow dress hem remains between the legs", overalls.at(0, 253), overalls.at(100, 253))
    }
}
