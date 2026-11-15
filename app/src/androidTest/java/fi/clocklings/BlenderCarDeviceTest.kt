package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderCarDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.CAR) {
    @Test
    fun carColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Taxi sign")
        selectColor("PURPLE")
        selectItem("Sport rims")
        verifyOutfitAfterRecreation("PURPLE", "Sport rims", "purple_rims")
    }
}
