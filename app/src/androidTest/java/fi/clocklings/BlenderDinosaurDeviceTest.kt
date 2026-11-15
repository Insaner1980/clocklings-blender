package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderDinosaurDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.DINOSAUR) {
    @Test
    fun dinosaurColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors(listOf("BLUE", "PURPLE", "ORIGINAL"))
        compose.clickByLabel("Party hat")
        selectColor("ORANGE")
        for (label in listOf("Duck swim ring", "Binoculars", "Medal", "Ice cream")) selectItem(label)
        verifyOutfitAfterRecreation("ORANGE", "Ice cream", "orange_ice_cream")
    }
}
