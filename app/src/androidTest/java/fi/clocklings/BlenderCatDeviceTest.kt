package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderCatDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.CAT) {
    @Test
    fun catColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        for (label in listOf("Ruffle collar", "Pearl necklace")) selectItem(label)
        compose.clickByLabel("Bobble hat")
        selectColor("PURPLE")
        selectItem("Round glasses")
        verifyOutfitAfterRecreation("PURPLE", "Round glasses", "purple_glasses")
    }
}
