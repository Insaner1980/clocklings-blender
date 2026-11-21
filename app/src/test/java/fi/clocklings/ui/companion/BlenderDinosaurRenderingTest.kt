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
class BlenderDinosaurRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "dinosaur") { rememberBlenderDinosaurRig(it) }

    @Test
    fun bothLidsCoverTheStationaryReflectionsAndCloseOnPaintedArcs() {
        val (open, closing, closed) = rendering.captureBlinkFrames(Outfit(), .35f, 1f)
        open.assertUnchanged(closing, 119..121, 84..87) { x, y -> "Near eye reflection moved at $x/$y" }
        open.assertUnchanged(closing, 160..162, 72..75) { x, y -> "Far eye reflection moved at $x/$y" }

        for ((xs, ys) in listOf(117..125 to 94..97, 162..168 to 80..83)) {
            assertTrue(
                "Painted eyelid missing at $xs/$ys",
                ys.any { y -> xs.any { x -> Color.green(closed.at(x, y)) < 100 } },
            )
        }
        for ((xs, ys) in listOf(110..130 to 79..103, 158..171 to 68..85)) {
            assertTrue(
                "Eye reflection remains at $xs/$ys",
                ys.none { y -> xs.any { x -> Color.red(closed.at(x, y)) > 210 && Color.blue(closed.at(x, y)) > 180 } },
            )
        }
    }

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.DINOSAUR)) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 135 until 150, 220 until 240) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
            if (frame.partDeg != 0f) {
                baseline.assertUnchanged(changed, 68..70, 202..204) { x, y ->
                    "Tail root plate swung into the back at $x/$y"
                }
                val tipRows =
                    (226..257).filter { y ->
                        val pixel = changed.at(11, y)
                        Color.green(pixel) > Color.red(pixel) + 15
                    }
                assertTrue("Tail tip lost its reach or thickness", tipRows.size >= 6)
                assertTrue("Tail tip did not lift gently", tipRows.first() in 234..241)
            }
        }
    }

    @Test
    fun onePawGripsTheWaffleCone() {
        val outfit = Outfit().with(Slot.EXTRA, "dinosaur-extra-ice-cream")
        rendering.setContent(outfit = { outfit }, color = CompanionColor.BLUE)
        val held = rendering.capture()
        val visibleCone =
            (190 until 198).sumOf { y ->
                (184 until 192).count { x ->
                    val pixel = held.at(x, y)
                    Color.red(pixel) > 160 && Color.red(pixel) > Color.blue(pixel) + 40
                }
            }
        assertTrue("The gripping paw hides the waffle cone", visibleCone > 40)
        for (y in 188..200) {
            for (x in 201..210) {
                val pixel = held.at(x, y)
                assertTrue(
                    "A second blue paw appears beyond the cone at $x/$y",
                    Color.blue(pixel) <= Color.red(pixel) + 25,
                )
            }
        }
    }

    @Test
    fun farBackpackStrapReachesTheUndersideOfTheChin() {
        val (bare, worn) = rendering.captureOutfitChange(Outfit(), Slot.EXTRA, "dinosaur-extra-backpack")
        assertTrue("The shoulder strap stops below the chin", bare.countDifferencesIn(worn, 153..157, 143..145) >= 10)
    }

    @Test
    fun farShoulderLeavesTheChestClearAndThePawWrapsAroundTheHandle() {
        val umbrella = Outfit().with(Slot.EXTRA, "dinosaur-extra-umbrella")
        rendering.setContent(outfit = { umbrella }, color = CompanionColor.BLUE)
        val held = rendering.capture()
        for ((x, y) in listOf(162 to 169, 166 to 177, 169 to 184)) {
            val chest = held.at(x, y)
            assertTrue("The far arm covers the cream chest at $x/$y", Color.red(chest) > Color.blue(chest) + 40)
        }
        for ((x, y) in listOf(206 to 180, 200 to 198, 209 to 199)) {
            val handle = held.at(x, y)
            assertTrue("The gold handle is missing at $x/$y", Color.red(handle) > Color.blue(handle) + 65)
        }
        val palm = held.at(204, 187)
        assertTrue("The shaft crosses the face of the blue paw", Color.blue(palm) > Color.red(palm) + 40)
    }

    @Test
    fun umbrellaClearsTheFaceWhileTheHeadTilts() {
        val bare = Outfit()
        val outfit = mutableStateOf(bare)
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })
        for (angle in listOf(0f, -6f)) {
            compose.runOnIdle {
                outfit.value = bare
                pose.value = CompanionPose(headTiltDeg = angle)
            }
            val face = rendering.capture()
            compose.runOnIdle { outfit.value = bare.with(Slot.EXTRA, "dinosaur-extra-umbrella") }
            val withUmbrella = rendering.capture()
            face.assertUnchanged(withUmbrella, 78..218, 62..135) { x, y -> "The umbrella crosses the face at $x/$y" }
            assertTrue(
                "The shaft is missing beside the muzzle",
                face.countDifferencesIn(withUmbrella, 223..239, 92..120) > 35,
            )
        }
    }

    @Test
    fun skateboardAndCupStayOnTheGroundWhenTheDinosaurJumps() {
        val bare = Outfit.default(CompanionKind.DINOSAUR)
        val outfit = mutableStateOf(bare)
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })
        val without = rendering.capture()
        for (item in listOf("dinosaur-extra-skateboard", "dinosaur-extra-bucket")) {
            compose.runOnIdle {
                outfit.value = bare.with(Slot.EXTRA, item)
                pose.value = CompanionPose()
            }
            val rest = rendering.capture()
            val board = item.endsWith("skateboard")
            if (board) {
                without.assertUnchanged(rest, 105..119, 278..280) { x, y -> "Deck covered the near toes at $x/$y" }
            }
            val xs = if (board) 58..68 else 205..218
            val ys = if (board) 289..297 else 253..272
            assertTrue("Missing $item", without.countDifferencesIn(rest, xs, ys) > 40)
            compose.runOnIdle { pose.value = CompanionPose(lift = CompanionMotion.HOP_HEIGHT) }
            val jump = rendering.capture()
            rest.assertUnchanged(jump, xs, ys) { x, y -> "$item moved at $x/$y" }
        }
    }
}
