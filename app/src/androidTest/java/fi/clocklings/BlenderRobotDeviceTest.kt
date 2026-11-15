package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderRobotDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.ROBOT) {
    @Test
    fun robotColorAndOutfitSurviveRecreation() {
        openWardrobe()
        selectColor("PURPLE")
        selectItem("Wind-up key")
        verifyOutfitAfterRecreation("PURPLE", "Wind-up key", "purple_key")
    }
}
