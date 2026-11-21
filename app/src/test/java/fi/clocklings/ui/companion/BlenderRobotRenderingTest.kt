package fi.clocklings.ui.companion

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
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
    fun wavingKeepsTheShoulderBroadAndConnected() {
        val rendering = BlenderRigRendering(compose, "robot") { rememberBlenderRobotRig(it) }
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { Outfit() }, pose = { pose.value })
        for (angle in listOf(75f, 95f, 117f)) {
            compose.runOnIdle { pose.value = CompanionPose(armDeg = angle) }
            val frame = rendering.capture()
            val background = frame.at(0, 0)
            for (x in 77..90) {
                assertTrue("Gap at the shoulder at $x with wave $angle", frame.at(x, 160) != background)
            }
            for (y in 151..169) {
                assertTrue("Narrow shoulder at $y with wave $angle", frame.at(78, y) != background)
            }
        }
    }

    @Test
    fun theSmallRobotStaysOnTheFloorWhenItsOwnerJumps() {
        val rendering = BlenderRigRendering(compose, "robot") { rememberBlenderRobotRig(it) }
        val pose = mutableStateOf(CompanionPose())
        val outfit = Outfit().with(Slot.EXTRA, "robot-extra-pet")
        rendering.setContent(outfit = { outfit }, pose = { pose.value })
        val rest = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(lift = 14f, swayDeg = 2f) }
        rest.assertUnchanged(rendering.capture(), 197..222, 237..284) { x, y -> "Pet moved at $x/$y" }
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
