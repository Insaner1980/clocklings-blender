package fi.clocklings.ui.companion

import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderCarRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "car") { rememberBlenderCarRig(it) }

    @Test
    fun bodyRockAndBlinkLeaveWheelHubsStill() {
        val frames = listOf(CompanionPose(partDeg = 18f), CompanionPose(blink = 1f))
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.CAR), frames) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 70 until 80, 235 until 255) { x, y ->
                "Wheel hub moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun sportRimsReplaceBothWheelHubs() {
        val (baseline, held) =
            rendering.captureOutfitChange(Outfit.default(CompanionKind.CAR), Slot.WHEELS, "car-wheels-sport")
        assertTrue("Sport rims are invisible", baseline.countDifferences(held) > 100)
        for ((x, y) in listOf(22 to 228, 75 to 246)) {
            assertTrue("Sport rim missing at $x/$y", baseline.at(x, y) != held.at(x, y))
        }
    }
}
