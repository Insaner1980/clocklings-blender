package fi.clocklings.ui.companion

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderRobotRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyReferenceAccessoryHasVisibleExportedGeometry() {
        val outfit = mutableStateOf(Outfit())
        val rendering = BlenderRigRendering(compose, "robot", 280, 346) { rememberBlenderRobotRig(it) }
        rendering.setContent(outfit = { outfit.value })
        val baseline = rendering.capture()
        val items = Wardrobe.itemsFor(CompanionKind.ROBOT)
        assertEquals(items.map { it.id }.toSet(), RobotAccessoryRegions.keys)
        for (item in items) {
            compose.runOnIdle { outfit.value = Outfit().with(item.slot, item.id) }
            val rendered = rendering.capture()
            assertTrue("Missing rendered accessory ${item.id}", baseline.countDifferences(rendered) > 50)
        }
    }

    @Test
    fun headArmAndBlinkAnimateIndependentlyOfTheChest() {
        val rendering = BlenderRigRendering(compose, "robot") { rememberBlenderRobotRig(it) }
        val frames = listOf(CompanionPose(headTiltDeg = 12f), CompanionPose(armDeg = 95f), CompanionPose(blink = 1f))
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.ROBOT), frames) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 95 until 145, 175 until 200) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }
}
