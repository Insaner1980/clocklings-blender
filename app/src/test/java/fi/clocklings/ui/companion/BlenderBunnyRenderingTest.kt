package fi.clocklings.ui.companion

import android.graphics.Color
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
                for ((y, temple) in listOf(69 to 62..77, 81 to 171..187, 135 to 176..191)) {
                    for (x in temple) {
                        assertTrue("Resting ear separated at $x/$y", baseline.at(x, y) != baseline.at(0, 0))
                        assertTrue("Swinging ear separated at $x/$y", changed.at(x, y) != changed.at(0, 0))
                    }
                }
            }
            baseline.assertUnchanged(changed, 110 until 130, 225 until 240) { x, y ->
                "Paws moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun closingTheLidsOccludesTheCatchlightsInPlace() {
        val (baseline, partial, closed) = rendering.captureBlinkFrames(Outfit.default(CompanionKind.BUNNY), .20f, .94f)

        for (xs in listOf(81..95, 144..156)) {
            val highlightPixels =
                xs.flatMap { x -> (102..110).map { y -> x to y } }.filter { (x, y) ->
                    Color.blue(baseline.at(x, y)) > 233 && Color.green(baseline.at(x, y)) > 235
                }
            assertTrue("Open eye has no catchlight in $xs", highlightPixels.size > 4)
            for ((x, y) in highlightPixels) {
                assertTrue("Catchlight shifted at $x/$y", partial.at(x, y) == baseline.at(x, y))
                assertTrue("Catchlight remained on the closed lid at $x/$y", Color.blue(closed.at(x, y)) < 225)
            }
        }
        baseline.assertUnchanged(closed, 70..90, 130..140) { x, y -> "Blink moved the cheek at $x/$y" }
    }

    @Test
    fun hindFeetTuckInsideTheirRestSilhouetteAtTheApex() {
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { Outfit.default(CompanionKind.BUNNY) }, pose = { pose.value })
        val baseline = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(lift = CompanionMotion.HOP_HEIGHT) }
        val apex = rendering.capture()
        for (xs in listOf(52..71, 167..184)) {
            val changed =
                xs.sumOf { x ->
                    (241..272).count { y ->
                        val liftedY = y - CompanionMotion.HOP_HEIGHT.toInt()
                        baseline.at(x, y) != baseline.at(0, 0) && apex.at(x, liftedY) == apex.at(0, 0)
                    }
                }
            assertTrue("Hind foot only translated in $xs", changed > 15)
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
    fun heldToolsMeetThePawOutlineAndTheWandStarStaysVisible() {
        val outfit = mutableStateOf(Outfit.default(CompanionKind.BUNNY))
        rendering.setContent(outfit = { outfit.value })
        val bare = rendering.capture()
        for (id in listOf("watering-can", "bubbles", "carrot", "bouquet")) {
            compose.runOnIdle { outfit.value = outfit.value.with(Slot.EXTRA, "bunny-extra-$id") }
            val held = rendering.capture()
            bare.assertUnchanged(held, 140..153, 220..224) { x, y -> "$id crosses the wrist at $x/$y" }
            bare.assertUnchanged(held, 141..148, 225..232) { x, y -> "$id covers the paw at $x/$y" }
            assertTrue("$id is invisible", bare.countDifferences(held) > 100)
            if (id == "bouquet") {
                assertTrue("Star hidden behind the arm", bare.countDifferencesIn(held, 167..191, 195..217) > 170)
                for ((x, y) in listOf(170 to 215, 171 to 215, 172 to 214)) {
                    val joint = held.at(x, y)
                    assertTrue("Gap below the star at $x/$y", Color.blue(joint) > Color.green(joint) + 15)
                }
            }
        }
    }

    @Test
    fun basketAndItsContactShadowStayOnTheFloorDuringAHop() {
        val (bare, rest, apex) =
            rendering.captureGroundedItemHop(Outfit.default(CompanionKind.BUNNY), "bunny-extra-basket")
        assertTrue("Wicker basket is missing", bare.countDifferencesIn(rest, 205..221, 250..274) > 100)
        assertTrue("Basket shadow is missing", bare.countDifferencesIn(rest, 210..222, 281..283) > 12)

        assertTrue("Bunny did not hop", rest.countDifferencesIn(apex, 80..160, 80..150) > 100)
        rest.assertUnchanged(apex, 208..230, 211..285) { x, y -> "Grounded basket moved at $x/$y" }
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
