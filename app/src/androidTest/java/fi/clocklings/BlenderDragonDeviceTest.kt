package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderDragonDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.DRAGON) {
    @Test
    fun dragonColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Knight helmet")
        selectColor("ORANGE")
        selectItem("Sparkle wings")
        verifyOutfitAfterRecreation("ORANGE", "Sparkle wings", "orange_wings")
    }
}
