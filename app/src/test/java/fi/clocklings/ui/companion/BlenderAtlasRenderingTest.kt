package fi.clocklings.ui.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlenderAtlasRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun omittingUnusedAccessoryAtlasesPreservesEveryDefaultCharacterPixel() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        val loadAccessories = mutableStateOf(true)
        compose.setContent {
            val rig = checkNotNull(rememberCompanionRig(kind.value, CompanionColor.ORIGINAL, loadAccessories.value))
            Canvas(Modifier.size(280.dp, 360.dp).testTag("atlas")) {
                scale(size.width / rig.width, size.height / rig.height, Offset.Zero) {
                    with(rig) { draw(CompanionColor.ORIGINAL, Outfit.default(kind.value), CompanionPose()) }
                }
            }
        }
        for (character in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            compose.runOnIdle {
                kind.value = character
                loadAccessories.value = true
            }
            val original = pixels("atlas")
            compose.runOnIdle { loadAccessories.value = false }
            assertArrayEquals("Default appearance changed for $character", original, pixels("atlas"))
        }
    }

    @Test
    fun accessoryAppearsAfterAnEmptyOutfitAndDisappearsWhenRemoved() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        val outfit = mutableStateOf(Outfit.Default)
        compose.setContent {
            CompanionView(kind.value, outfit.value, Modifier.size(280.dp, 360.dp), animated = false)
        }
        for (character in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            val originalOutfit = Outfit.default(character)
            val item =
                Wardrobe.itemsFor(character).first {
                    character != CompanionKind.HUMAN || it.id in DollAccessoryRegions
                }
            compose.runOnIdle {
                kind.value = character
                outfit.value = originalOutfit
            }
            val tag = "companion_${character.name}"
            val original = pixels(tag)
            compose.runOnIdle { outfit.value = originalOutfit.with(item.slot, item.id) }
            assertFalse("Accessory did not appear: ${item.id}", original.contentEquals(pixels(tag)))
            compose.runOnIdle { outfit.value = originalOutfit }
            assertArrayEquals("Accessory did not disappear: ${item.id}", original, pixels(tag))
        }
    }

    private fun pixels(tag: String): IntArray {
        val image = compose.captureRenderedImage(tag)
        val pixels = image.toPixelMap()
        return IntArray(image.width * image.height) { pixels[it % image.width, it / image.width].toArgb() }
    }
}
