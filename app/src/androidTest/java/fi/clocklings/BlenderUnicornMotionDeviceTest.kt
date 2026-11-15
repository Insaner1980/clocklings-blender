package fi.clocklings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Slot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with animator_duration_scale both 1 and 0 on the dedicated emulator. */
@RunWith(AndroidJUnit4::class)
class BlenderUnicornMotionDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val scenario = BlenderMotionScenario(compose, CompanionKind.UNICORN, "blender-unicorn")

    @Test
    fun captureAllColorsAndAccessoriesAtThumbnailSize() {
        scenario.captureAccessories()
    }

    @Test
    fun celebrationAndIdleRespectSystemReducedMotion() {
        scenario.verifyMotion(Slot.HEAD, "unicorn-head-crown")
    }
}
