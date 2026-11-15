package fi.clocklings.ui.companion

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderBunnyRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "bunny") { rememberBlenderBunnyRig(it) }

    @Test
    fun headEarsAndBlinkAnimateIndependentlyOfThePaws() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.BUNNY)) { frame, baseline, changed ->
            if (frame.partDeg != 0f) {
                for (side in listOf(0 until 60, 180 until 240)) {
                    val changedPixels = baseline.countDifferencesIn(changed, side, 60 until 230)
                    assertTrue("Ear did not move in $side", changedPixels > 100)
                }
            }
            baseline.assertUnchanged(changed, 110 until 130, 225 until 240) { x, y ->
                "Paws moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun pawsCloseOverTheCarrot() {
        val (baseline, held) =
            rendering.captureOutfitChange(Outfit.default(CompanionKind.BUNNY), Slot.EXTRA, "bunny-extra-carrot")
        assertTrue("Carrot is invisible", baseline.countDifferences(held) > 100)
        for ((x, y) in listOf(110 to 235, 114 to 239, 132 to 235, 128 to 239)) {
            assertTrue("Carrot covers the paw at $x/$y", baseline.at(x, y) == held.at(x, y))
        }
    }

    @Test
    fun modeledTiaraFollowsHeadTiltWhileRearWingsStayWithTheBody() {
        val pose = mutableStateOf(CompanionPose())
        val outfit =
            Outfit
                .default(CompanionKind.BUNNY)
                .with(Slot.HEAD, "bunny-head-tiara")
                .with(Slot.EXTRA, "bunny-extra-wings")
        rendering.setContent(outfit = { outfit }, pose = { pose.value }, color = CompanionColor.PURPLE)
        val baseline = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(headTiltDeg = 12f) }
        val tilted = rendering.capture()

        assertTrue("Tiara did not follow the head", baseline.countDifferencesIn(tilted, 75..165, 10..55) > 20)
        assertTrue("Rear wings moved with the head", baseline.countDifferencesIn(tilted, 10..55, 245..275) == 0)
    }
}
