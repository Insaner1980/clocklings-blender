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
class BlenderUnicornRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "unicorn") { rememberBlenderUnicornRig(it) }

    @Test
    fun headTailAndBlinkAnimateIndependentlyOfTheHooves() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.UNICORN)) { frame, baseline, changed ->
            if (frame.partDeg != 0f) {
                val changedPixels = baseline.countDifferencesIn(changed, 185 until 220, 150 until 230)
                assertTrue("Tail did not move", changedPixels > 100)
            }
            baseline.assertUnchanged(changed, 100 until 115, 265 until 275) { x, y ->
                "Hooves moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun sparkleBootsCoverAllFourHooves() {
        val (baseline, held) =
            rendering.captureOutfitChange(Outfit.default(CompanionKind.UNICORN), Slot.EXTRA, "unicorn-extra-boots")
        assertTrue("Sparkle boots are invisible", baseline.countDifferences(held) > 100)
        for ((x, y) in listOf(82 to 266, 136 to 260, 106 to 272, 167 to 267)) {
            assertTrue("Boot missing at $x/$y", baseline.at(x, y) != held.at(x, y))
        }
    }

    @Test
    fun cloudAndRainbowStayOnTheGroundDuringAHop() {
        val outfit = mutableStateOf(Outfit())
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })
        for ((item, xs) in listOf("unicorn-extra-cloud" to 57..65, "unicorn-extra-rainbow" to 25..60)) {
            compose.runOnIdle {
                outfit.value = Outfit().with(Slot.EXTRA, item)
                pose.value = CompanionPose()
            }
            val rest = rendering.capture()
            assertTrue(xs.any { x -> rest.at(x, 279) != rest.at(0, 0) })
            compose.runOnIdle { pose.value = CompanionPose(lift = CompanionMotion.HOP_HEIGHT, swayDeg = 2f) }
            rest.assertUnchanged(rendering.capture(), xs, 276..283) { x, y -> "$item moved at $x/$y" }
        }
    }

    @Test
    fun theStarWandTiltsWithTheMuzzle() {
        val pose = mutableStateOf(CompanionPose())
        rendering.setContent(outfit = { Outfit().with(Slot.EXTRA, "unicorn-extra-balloon") }, pose = { pose.value })
        val rest = rendering.capture()
        compose.runOnIdle { pose.value = CompanionPose(headTiltDeg = 8f) }
        assertTrue(rest.countDifferencesIn(rendering.capture(), 4..31, 160..191) > 100)
    }

    @Test
    fun walkingLiftsTheForeHoofAndItsBootTogether() {
        val outfit = mutableStateOf(Outfit.default(CompanionKind.UNICORN))
        val pose = mutableStateOf(CompanionPose(walkTimeMs = 1_570L))
        rendering.setContent(outfit = { outfit.value }, pose = { pose.value })

        fun sole(
            pixels: RenderedPixels,
            boot: Boolean,
        ): Int =
            (240..287).last { y ->
                (96..114).any { x ->
                    val rgb = pixels.at(x, y)
                    val red = rgb ushr 16 and 255
                    val green = rgb ushr 8 and 255
                    val blue = rgb and 255
                    if (boot) blue > red + 30 && green > red + 15 else blue > green + 22 && red > green + 10
                }
            }
        for (boot in listOf(false, true)) {
            compose.runOnIdle {
                outfit.value = outfit.value.with(Slot.EXTRA, if (boot) "unicorn-extra-boots" else null)
                pose.value = CompanionPose(walkTimeMs = 1_570L)
            }
            val planted = sole(rendering.capture(), boot)
            compose.runOnIdle { pose.value = CompanionPose(walkTimeMs = 2_230L) }
            val airborne = sole(rendering.capture(), boot)
            assertTrue("The near forefoot should clear its footprint with boot=$boot", airborne <= planted - 7)
        }
    }
}
