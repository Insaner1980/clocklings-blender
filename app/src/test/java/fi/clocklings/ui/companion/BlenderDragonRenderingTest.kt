package fi.clocklings.ui.companion

import android.graphics.Color
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
    fun neckBandsStayAgainstTheShouldersWhenTheHeadTilts() {
        val pose = mutableStateOf(CompanionPose())
        val outfit = mutableStateOf(Outfit())
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })
        for (angle in listOf(-3f, 3f)) {
            compose.runOnIdle {
                pose.value = CompanionPose(headTiltDeg = angle, partDeg = 18f)
                outfit.value = Outfit()
            }
            val bare = rendering.capture()
            for (item in listOf("dragon-neck-bow", "dragon-neck-cape")) {
                compose.runOnIdle { outfit.value = Outfit().with(Slot.NECK, item) }
                assertNoFloatingNeckBand(bare, rendering.capture(), item, angle)
            }
        }
    }

    private fun assertNoFloatingNeckBand(
        bare: RenderedPixels,
        worn: RenderedPixels,
        item: String,
        angle: Float,
    ) {
        val background = bare.at(0, 0)
        for (y in 147..154) {
            for (x in (84..96) + (144..156)) {
                val towardNeck = if (x < 120) x + 1 else x - 1
                if (bare.at(x, y) == background && bare.at(towardNeck, y) == background) {
                    assertTrue("Floating $item at $x/$y with tilt $angle", worn.at(x, y) == background)
                }
            }
        }
    }

    @Test
    fun closedEyesShowTheLidsInPlaceOfTheIrisAndItsHighlight() {
        val (open, half, closed) = rendering.captureBlinkFrames(Outfit(), 0.45f, 1f)
        for (xs in listOf(75..103, 137..165)) {
            val ys = 96..106
            open.assertUnchanged(half, xs, 98..103) { x, y -> "Iris moves beneath the lid at $x/$y" }
            assertTrue("Eye did not close", open.countDifferencesIn(closed, xs, ys) > 190)
            assertTrue("Missing closed lid", ys.sumOf { y -> xs.count { x -> Color.red(closed.at(x, y)) < 95 } } > 20)
            assertTrue(
                "A white iris highlight remains on the closed eye",
                ys.none { y -> xs.any { x -> Color.green(closed.at(x, y)) > 205 } },
            )
        }
    }

    @Test
    fun pawsOverlapTheHeldEquipmentAndRestOnTheCloud() {
        val outfit = mutableStateOf(Outfit())
        rendering.setContent(outfit = { outfit.value })
        val bare = rendering.capture()
        val equipment =
            listOf(
                Triple("dragon-extra-book", 114..124, 202..213),
                Triple("dragon-extra-shield", 54..68, 198..220),
                Triple("dragon-extra-star-cloud", 112..128, 284..289),
            )
        for ((item, xs, ys) in equipment) {
            compose.runOnIdle { outfit.value = Outfit().with(Slot.EXTRA, item) }
            val worn = rendering.capture()
            assertTrue("Missing $item", bare.countDifferencesIn(worn, xs, ys) > 80)
            for (paw in listOf(85..92, 148..155)) {
                bare.assertUnchanged(worn, paw, 184..194) { x, y -> "$item covers the forepaw at $x/$y" }
            }
            if (item == "dragon-extra-star-cloud") {
                for (foot in listOf(79..91, 147..158)) {
                    bare.assertUnchanged(worn, foot, 273..278) { x, y -> "Cloud covers the foot at $x/$y" }
                }
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
