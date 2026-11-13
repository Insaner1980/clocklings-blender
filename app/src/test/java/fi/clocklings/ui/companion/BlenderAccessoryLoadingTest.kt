package fi.clocklings.ui.companion

import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlenderAccessoryLoadingTest {
    @Test
    fun defaultOutfitsDoNotLoadUnusedAccessoryAtlases() {
        for (kind in CompanionKind.entries) {
            assertFalse("Unnecessary accessory atlas for $kind", needsBlenderAccessories(kind, Outfit.default(kind)))
        }
    }

    @Test
    fun everyModeledAccessoryLoadsItsAtlas() {
        for (kind in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            for (item in Wardrobe.itemsFor(kind)) {
                val needsAtlas = kind != CompanionKind.HUMAN || item.id in DollAccessoryRegions
                val outfit = Outfit.default(kind).with(item.slot, item.id)
                if (needsAtlas) {
                    assertTrue("Missing atlas for ${item.id}", needsBlenderAccessories(kind, outfit))
                    assertTrue(
                        "Incoming outfit needs its atlas before animation: ${item.id}",
                        needsBlenderAccessories(kind, Outfit.default(kind), outfit),
                    )
                } else {
                    assertFalse("Base atlas already contains ${item.id}", needsBlenderAccessories(kind, outfit))
                }
            }
        }
    }

    @Test
    fun removingAnOptionalAccessoryReleasesTheAtlasRequirement() {
        val item = Wardrobe.itemsFor(CompanionKind.ROBOT).first()
        val dressed = Outfit().with(item.slot, item.id)
        assertTrue(needsBlenderAccessories(CompanionKind.ROBOT, dressed))
        assertFalse(needsBlenderAccessories(CompanionKind.ROBOT, dressed.with(item.slot, null)))
    }
}
