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
class BlenderCatRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "cat") { rememberBlenderCatRig(it) }

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.CAT)) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 110 until 130, 205 until 235) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun modeledHatFollowsTheHeadWhileButterflyWingsStayOnTheBody() {
        val pose = mutableStateOf(CompanionPose())
        val outfit = Outfit().with(Slot.HEAD, "cat-head-sailor-hat").with(Slot.EXTRA, "cat-extra-wings")
        rendering.setContent(outfit = { outfit }, pose = { pose.value }, color = CompanionColor.BLUE)
        val rest = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(headTiltDeg = 12f) }
        val tilted = rendering.capture()
        val changedHatPixels = rest.countDifferencesIn(tilted, 50 until 190, 20 until 70)
        assertTrue("Modeled sailor hat did not follow the head", changedHatPixels > 100)
        for (side in listOf(10 until 40, 200 until 230)) {
            rest.assertUnchanged(tilted, side, 185 until 220) { x, y -> "Butterfly wing followed the head at $x/$y" }
        }
    }
}
