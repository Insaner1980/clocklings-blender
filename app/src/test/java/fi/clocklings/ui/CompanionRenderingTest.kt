package fi.clocklings.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.rememberCompanionRig
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompanionRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyCompanionAndWearableProducesVisibleDistinctArtwork() {
        val kind = mutableStateOf(CompanionKind.CAT)
        val outfit = mutableStateOf(Outfit.default(CompanionKind.CAT))
        compose.setContent {
            ClocklingsTheme {
                CompanionView(kind.value, outfit.value, Modifier.size(180.dp, 240.dp), animated = false)
            }
        }
        for (companion in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            compose.runOnIdle {
                kind.value = companion
                outfit.value = Outfit.default(companion)
            }
            val baseline = pixels(companion)
            assertTrue("Empty companion: $companion", baseline.any { it != 0 })
            for (item in Wardrobe.itemsFor(companion)) {
                compose.runOnIdle { outfit.value = Outfit.default(companion).with(item.slot, item.id) }
                val dressed = pixels(companion)
                if (Outfit.default(companion).isOn(item)) {
                    assertTrue("Default item changed: ${item.id}", baseline.contentEquals(dressed))
                } else {
                    assertTrue("Invisible item: ${item.id}", !baseline.contentEquals(dressed))
                }
            }
        }
    }

    @Test
    fun everyAccessoryThumbnailZoomsAndKeepsItsEstablishedBounds() {
        val kind = mutableStateOf(CompanionKind.CAT)
        var selectedRig: CompanionRig? = null
        compose.setContent { selectedRig = rememberCompanionRig(kind.value, CompanionColor.ORIGINAL) }
        val accessories = Wardrobe.items.filter { it.slot == Slot.EXTRA }
        assertTrue("No accessories found", accessories.isNotEmpty())
        val unzoomed = mutableListOf<String>()
        val outside = mutableListOf<String>()
        val small = mutableListOf<String>()
        // Preserve the fitted thumbnail padding around artwork that reaches the original grid edges.
        val extendedCrops =
            mapOf(
                "bunny-extra-bubbles" to Rect(110f, 163f, 247f, 293f),
                "dinosaur-extra-balloon" to Rect(24f, -3f, 256f, 216f),
                "dinosaur-extra-skateboard" to Rect(10f, 80f, 230f, 314f),
                "unicorn-extra-balloon" to Rect(-3f, 112f, 120f, 232f),
                "unicorn-extra-cloud" to Rect(44f, 142f, 204f, 300f),
            )
        for (item in accessories) {
            compose.runOnIdle { kind.value = item.companion }
            val rig = compose.runOnIdle { checkNotNull(selectedRig) }
            val crop = rig.crop(item)
            val full = rig.full(Outfit.default(item.companion).with(item.slot, item.id))
            if (crop == full) unzoomed += item.id
            val outsideHorizontally = crop.left < full.left || crop.right > full.right
            val outsideVertically = crop.top < full.top || crop.bottom > full.bottom
            if (item.id in extendedCrops) {
                assertEquals(item.id, extendedCrops.getValue(item.id), crop)
            } else if (outsideHorizontally || outsideVertically) {
                outside += item.id
            }
            if (crop.width < MIN_CROP || crop.height < MIN_CROP) small += item.id
        }
        assertTrue("Accessory crops equal to the whole grid: $unzoomed", unzoomed.isEmpty())
        assertTrue("Accessory crops outside the grid: $outside", outside.isEmpty())
        assertTrue("Accessory crops under $MIN_CROP units: $small", small.isEmpty())
    }

    @Test
    fun eachCompanionColorChangesTheRenderedBody() {
        val kind = mutableStateOf(CompanionKind.CAT)
        val color = mutableStateOf(CompanionColor.ORIGINAL)
        compose.setContent {
            ClocklingsTheme {
                CompanionView(
                    kind.value,
                    Outfit.default(kind.value),
                    Modifier.size(180.dp, 240.dp),
                    color = color.value,
                    animated = false,
                )
            }
        }
        for (companion in CompanionKind.entries.filter { it != CompanionKind.NONE && it != CompanionKind.HUMAN }) {
            compose.runOnIdle {
                kind.value = companion
                color.value = CompanionColor.ORIGINAL
            }
            val baseline = pixels(companion)
            for (shade in companion.colorOptions().filter { it != CompanionColor.ORIGINAL }) {
                compose.runOnIdle { color.value = shade }
                assertTrue("Unchanged color: $companion / $shade", !baseline.contentEquals(pixels(companion)))
            }
        }
    }

    private companion object {
        /** Smallest thumbnail crop, so the companion stays recognisable next to its item. */
        const val MIN_CROP = 120f
    }

    private fun pixels(kind: CompanionKind): IntArray {
        val image = compose.onNodeWithTag("companion_${kind.name}", useUnmergedTree = true).captureToImage()
        val pixels = image.toPixelMap()
        return IntArray(image.width * image.height) { index ->
            pixels[index % image.width, index / image.width].toArgb()
        }
    }
}
