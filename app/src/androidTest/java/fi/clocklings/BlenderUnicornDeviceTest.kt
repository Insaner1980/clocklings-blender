package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderUnicornDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.UNICORN) {
    @Test
    fun unicornColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Horn rings")
        selectColor("PURPLE")
        selectItem("Sparkle boots")
        verifyOutfitAfterRecreation("PURPLE", "Sparkle boots", "purple_boots")
    }
}
