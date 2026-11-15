package fi.clocklings.ui.companion

import android.graphics.Color
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
    fun headTailAndBlinkAnimateIndependentlyOfTheChest() {
        rendering.assertAnimatedFrames(Outfit.default(CompanionKind.DINOSAUR)) { frame, baseline, changed ->
            baseline.assertUnchanged(changed, 135 until 150, 220 until 240) { x, y ->
                "Chest moved at $x/$y with $frame"
            }
        }
    }

    @Test
    fun waffleConeRemainsVisibleInFrontOfTheGrippingPaw() {
        val outfit = Outfit().with(Slot.EXTRA, "dinosaur-extra-ice-cream")
        rendering.setContent(outfit = { outfit }, color = CompanionColor.BLUE)
        val held = rendering.capture()
        val visibleCone =
            (195 until 203).sumOf { y ->
                (184 until 192).count { x ->
                    val pixel = held.at(x, y)
                    Color.red(pixel) > 160 && Color.red(pixel) > Color.blue(pixel) + 40
                }
            }
        assertTrue("The gripping paw hides the waffle cone", visibleCone > 40)
    }
}
