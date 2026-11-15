package fi.clocklings.ui.companion

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
}
