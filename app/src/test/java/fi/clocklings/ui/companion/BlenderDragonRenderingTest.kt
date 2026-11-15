package fi.clocklings.ui.companion

import androidx.compose.runtime.mutableStateOf
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
class BlenderDragonRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "dragon") { rememberBlenderDragonRig(it) }

    @Test
    fun headWingsAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.DRAGON)) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 110 until 130, 205 until 235) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun replacementWingsBothFlapWithoutMovingTheFace() {
        val pose = mutableStateOf(CompanionPose())
        val outfit = Outfit.default(CompanionKind.DRAGON).with(Slot.EXTRA, "dragon-extra-wings")
        rendering.setContent(outfit = { outfit }, pose = { pose.value })
        val rest = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(partDeg = 18f) }
        val flapped = rendering.capture()
        for (horizontalRange in listOf(0 until 70, 170 until 240)) {
            val changed = rest.countDifferencesIn(flapped, horizontalRange, 135 until 235)
            assertTrue("Replacement wing did not flap: $horizontalRange", changed > 100)
        }
        rest.assertUnchanged(flapped, 100 until 140, 85 until 125) { _, _ -> "Wing motion moved the face" }
    }
}
