package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderBunnyDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.BUNNY) {
    @Test
    fun bunnyColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Magician's top hat")
        selectColor("PURPLE")
        selectItem("Carrot")
        verifyOutfitAfterRecreation("PURPLE", "Carrot", "purple_carrot")
    }
}
