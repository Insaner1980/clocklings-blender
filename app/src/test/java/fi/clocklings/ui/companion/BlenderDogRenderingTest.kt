package fi.clocklings.ui.companion

import androidx.compose.ui.test.junit4.createComposeRule
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
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
}
