package fi.clocklings.ui.companion

import android.graphics.Color
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
    fun bodyRocksOverStationaryTyresAndHubs() {
        val frames = listOf(CompanionPose(partDeg = 18f), CompanionPose(partDeg = -18f))
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.CAR), frames) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 70 until 80, 235 until 255) { x, y ->
                "Wheel hub moved at $x/$y with $frame"
            }
            if (frame.partDeg < 0f) {
                for (y in 263..267) {
                    val rubber = changed.at(202, y)
                    assertTrue("Raised bumper exposed paint or a hole at 202/$y", Color.red(rubber) < 80)
                    assertTrue("Missing far tyre at 202/$y", Color.alpha(rubber) == 255)
                }
            }
        }
    }

    @Test
    fun eyelidsCoverStationaryPupilsAndCatchlights() {
        val frames =
            listOf(
                CompanionPose(blink = 0.5f),
                CompanionPose(blink = CompanionMotion.blink(3_296L)),
                CompanionPose(blink = 1f),
            )
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.CAR), frames) { frame, baseline, changed ->
            if (frame.blink == 0.5f) {
                baseline.assertUnchanged(changed, 122..131, 230..233) { x, y -> "Pupil moved at $x/$y" }
            } else {
                for (xs in listOf(110..140, 199..221)) {
                    for (y in 221..235) {
                        for (x in xs) {
                            val lid = changed.at(x, y)
                            assertTrue("Headlight shines through the closed lid at $x/$y", Color.blue(lid) < 145)
                        }
                    }
                }
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
