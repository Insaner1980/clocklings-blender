package fi.clocklings

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Slot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run with animator_duration_scale both 1 and 0 on the dedicated emulator. */
@RunWith(AndroidJUnit4::class)
class BlenderDollMotionDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val scenario = BlenderMotionScenario(compose, CompanionKind.HUMAN, "blender-doll")

    @Test
    fun captureAllWardrobeItemsAtThumbnailSize() {
        scenario.captureAccessories { CompanionColor.ORIGINAL }
    }

    @Test
    fun celebrationAndIdleRespectSystemReducedMotion() {
        scenario.verifyMotion(Slot.DRESS, "dress-overalls", Modifier.size(200.dp, 340.dp))
    }
}
