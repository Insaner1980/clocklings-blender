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
class BlenderCatRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "cat") { rememberBlenderCatRig(it) }

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.CAT)) { frame, baseline, changed ->
            if (frame.partDeg > 0f) {
                baseline.assertUnchanged(changed, 181..188, 263..269) { x, y -> "The rear paw moved at $x/$y" }
            }
            baseline.assertUnchanged(changed, 110 until 130, 205 until 235) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun eyelidsCoverStationaryCatchlightsAndHoldThroughTheRecordedBlink() {
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { Outfit.default(CompanionKind.CAT) }, pose = { pose.value })
        val open = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(blink = .2f) }
        val partial = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(blink = CompanionMotion.blink(3296L)) }
        val closed = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(blink = CompanionMotion.blink(3273L)) }
        assertTrue("The lids did not hold closed", closed.values.contentEquals(rendering.capture().values))
        for (center in listOf(90, 152)) {
            val highlights =
                (center - 7..center + 2).flatMap { x -> (99..109).map { y -> x to y } }.filter { (x, y) ->
                    Color.green(open.at(x, y)) > 235 && Color.blue(open.at(x, y)) > 233
                }
            assertTrue("Missing catchlight near $center", highlights.size > 4)
            for ((x, y) in highlights) {
                assertTrue("Catchlight shifted at $x/$y", open.at(x, y) == partial.at(x, y))
                assertTrue("Catchlight visible through the lid at $x/$y", Color.blue(closed.at(x, y)) < 200)
            }
            val paintedLid = (112..115).any { y -> Color.green(closed.at(center, y)) < 110 }
            assertTrue("Missing painted lid near $center", paintedLid)
        }
    }

    @Test
    fun backpackStrapsPassBehindBothForelegsAtRestAndDuringAHop() {
        val bare = Outfit.default(CompanionKind.CAT)
        val backpack = bare.with(Slot.EXTRA, "cat-extra-fish-backpack")
        val outfit = mutableStateOf(bare)
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })
        for (lift in listOf(0f, CompanionMotion.HOP_HEIGHT)) {
            compose.runOnIdle {
                pose.value = CompanionPose(lift = lift)
                outfit.value = bare
            }
            val without = rendering.capture()
            compose.runOnIdle { outfit.value = backpack }
            val worn = rendering.capture()
            val dy = lift.toInt()
            for (leg in listOf(78..96, 149..163)) {
                without.assertUnchanged(worn, leg, (182 - dy)..(199 - dy)) { x, y ->
                    "Webbing crossed the foreleg at $x/$y"
                }
            }
            for (shoulder in listOf(99..110, 130..141)) {
                val visible = without.countDifferencesIn(worn, shoulder, (169 - dy)..(186 - dy))
                assertTrue("Missing shoulder strap at $shoulder", visible > 35)
            }
        }
    }

    @Test
    fun frontPawsTuckDuringTheHop() {
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { Outfit.default(CompanionKind.CAT) }, pose = { pose.value })
        val rest = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(lift = CompanionMotion.HOP_HEIGHT) }
        val apex = rendering.capture()
        for (xs in listOf(90..107, 136..151)) {
            val tucked =
                xs.sumOf { x ->
                    (277..280).count { y ->
                        val liftedY = y - CompanionMotion.HOP_HEIGHT.toInt()
                        rest.at(x, y) != rest.at(0, 0) && apex.at(x, liftedY) == apex.at(0, 0)
                    }
                }
            assertTrue("The front paw only translated at $xs", tucked > 20)
        }
    }

    @Test
    fun yarnBallAndLooseStrandStayOnTheFloorWhenTheCatJumps() {
        val (bare, rest, jump) = rendering.captureGroundedItemHop(Outfit.default(CompanionKind.CAT), "cat-extra-yarn")
        assertTrue("Yarn ball missing", bare.countDifferencesIn(rest, 59..77, 254..270) > 200)
        assertTrue("Loose strand missing", bare.countDifferencesIn(rest, 98..120, 282..288) > 10)

        assertTrue("Cat did not jump", rest.countDifferencesIn(jump, 90..155, 90..145) > 100)
        rest.assertUnchanged(jump, 59..77, 254..270) { x, y -> "Yarn ball moved at $x/$y" }
        rest.assertUnchanged(jump, 98..120, 285..288) { x, y -> "Loose strand moved at $x/$y" }
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
