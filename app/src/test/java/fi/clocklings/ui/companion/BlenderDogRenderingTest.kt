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
class BlenderDogRenderingTest {
    @get:Rule val compose = createComposeRule()

    private val rendering = BlenderRigRendering(compose, "dog") { rememberBlenderDogRig(it) }

    @Test
    fun headEarTailAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.DOG)) { frame, baseline, changed ->
            if (frame.partDeg > 0f) {
                assertTrue(
                    "The tip should leave its resting footprint as the tail wags",
                    baseline.countDifferencesIn(changed, 184..197, 175..185) > 35,
                )
                baseline.assertUnchanged(changed, 176..183, 262..268) { x, y -> "The rear paw moved at $x/$y" }
                for (y in 224..242) {
                    for (x in 192..225) {
                        val rest = baseline.at(x, y)
                        val wag = changed.at(x, y)
                        if (Color.alpha(rest) > 220 && Color.red(rest) - Color.blue(rest) > 65) {
                            assertTrue(
                                "The tail root separates from the haunch at $x/$y",
                                Color.alpha(wag) > 190 && Color.red(wag) - Color.blue(wag) > 40,
                            )
                        }
                    }
                }
            }
            if (frame.blink == 1f) {
                for (startX in listOf(80, 158)) {
                    val visible = baseline.countDifferencesIn(changed, startX until startX + 4, 98 until 106)
                    assertTrue("Lop ear hides the outer eye at $startX", visible >= 24)
                }
            }
            baseline.assertUnchanged(changed, 110 until 130, 205 until 235) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun tiltedJawOverlapsTheNeck() {
        rendering.setContent(
            outfit = { Outfit() },
            pose = { CompanionPose(headTiltDeg = -CompanionMotion.HEAD_TILT_DEG) },
        )
        val tilted = rendering.capture()
        for ((x, y) in listOf(151 to 156, 149 to 157, 147 to 158, 145 to 159)) {
            val pixel = tilted.at(x, y)
            assertTrue(
                "Background shows under the jaw at $x/$y",
                Color.alpha(pixel) > 200 && Color.red(pixel) - Color.blue(pixel) > 45,
            )
        }
    }

    @Test
    fun capeDrapesOverTheRaincoatAndOpensAtTheChest() {
        val cape = Outfit().with(Slot.NECK, "dog-neck-cape")
        val coat = Outfit().with(Slot.EXTRA, "dog-extra-raincoat")
        val outfit = mutableStateOf(cape)
        rendering.setContent(outfit = { outfit.value })
        val velvet = rendering.capture()
        compose.runOnIdle { outfit.value = coat }
        val yellow = rendering.capture()
        compose.runOnIdle { outfit.value = coat.with(Slot.NECK, "dog-neck-cape") }
        val both = rendering.capture()

        var visibleVelvet = 0
        for (y in 178..199) {
            for (x in 83..109) {
                val pixel = velvet.at(x, y)
                if (Color.red(pixel) > 2 * Color.green(pixel) && both.at(x, y) == pixel && yellow.at(x, y) != pixel) {
                    visibleVelvet++
                }
            }
        }
        assertTrue("The slicker covers the velvet shoulder", visibleVelvet > 100)
        yellow.assertUnchanged(both, 118..122, 197..205) { x, y -> "Cape blocks the coat opening at $x/$y" }
    }

    @Test
    fun raincoatFitsAroundBothUpperForelegs() {
        val outfit = mutableStateOf(Outfit())
        rendering.setContent(outfit = { outfit.value }, color = CompanionColor.BLUE)
        val bare = rendering.capture()
        compose.runOnIdle { outfit.value = Outfit().with(Slot.EXTRA, "dog-extra-raincoat") }
        val worn = rendering.capture()

        for (leg in listOf(82..91, 148..157)) {
            bare.assertUnchanged(worn, leg, 189..217) { x, y -> "Fabric crosses the upper foreleg at $x/$y" }
        }
        assertTrue("Missing fabric between the legs", bare.countDifferencesIn(worn, 116..124, 192..215) == 216)
        for (side in listOf(63..68, 172..177)) {
            assertTrue("Missing fabric beside the shoulder", bare.countDifferencesIn(worn, side, 202..212) > 30)
        }
    }

    @Test
    fun outerwearLeavesBothLowerForelegsVisible() {
        val outfit = mutableStateOf(Outfit())
        rendering.setContent(outfit = { outfit.value })
        val bare = rendering.capture()
        for ((slot, item) in listOf(Slot.NECK to "dog-neck-cape", Slot.EXTRA to "dog-extra-raincoat")) {
            compose.runOnIdle { outfit.value = Outfit().with(slot, item) }
            val worn = rendering.capture()
            assertTrue("Missing garment at the shoulders", bare.countDifferencesIn(worn, 74..166, 173..196) > 200)
            for (leg in listOf(81..96, 144..159)) {
                bare.assertUnchanged(worn, leg, 243..270) { x, y -> "$item covers the foreleg at $x/$y" }
            }
        }
    }
}
