package fi.clocklings.domain

import fi.clocklings.domain.CompanionKind.HUMAN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeTest {
    @Test
    fun `eighteen items with three free and one per star`() {
        val items = Wardrobe.itemsFor(HUMAN)
        assertEquals(18, items.size)
        assertEquals(3, items.count { it.star == 0 })
        assertEquals(
            (1..15).toList(),
            items
                .filter { it.star > 0 }
                .map { it.star }
                .sorted(),
        )
        assertEquals(
            18,
            items
                .map { it.id }
                .toSet()
                .size,
        )
    }

    @Test
    fun `only the doll's hair, dress and shoes are required`() {
        assertEquals(setOf(Slot.HAIR, Slot.DRESS, Slot.SHOES), Slot.entries.filterNot { it.optional }.toSet())
    }

    @Test
    fun `default outfit uses the free items`() {
        val o = Outfit.Default
        assertEquals("hair-letit", o[Slot.HAIR])
        assertEquals("dress-perus", o[Slot.DRESS])
        assertEquals("shoes-tossut", o[Slot.SHOES])
        assertNull(o[Slot.HEAD])
        assertNull(o[Slot.EXTRA])
    }

    @Test
    fun `newly unlocked items between star totals`() {
        assertEquals(
            listOf("shoes-punaiset", "hair-pitkat"),
            Wardrobe.newlyUnlocked(HUMAN, before = 2, after = 4).map { it.id },
        )
        assertTrue(Wardrobe.newlyUnlocked(HUMAN, 5, 5).isEmpty())
    }

    @Test
    fun `items left counts locked items`() {
        assertEquals(15, Wardrobe.lockedCount(HUMAN, 0))
        assertEquals(0, Wardrobe.lockedCount(HUMAN, 15))
    }

    @Test
    fun `toggling an optional item takes it off`() {
        val crown = Wardrobe.item("head-kruunu")
        val on = Wardrobe.toggle(Outfit.Default, crown)
        assertEquals("head-kruunu", on[Slot.HEAD])
        assertNull(Wardrobe.toggle(on, crown)[Slot.HEAD])
    }

    @Test
    fun `toggling a required item keeps it on`() {
        val dress = Wardrobe.item("dress-perus")
        assertEquals("dress-perus", Wardrobe.toggle(Outfit.Default, dress)[Slot.DRESS])
        val star = Wardrobe.item("dress-tahti")
        assertEquals("dress-tahti", Wardrobe.toggle(Outfit.Default, star)[Slot.DRESS])
    }

    @Test
    fun `outfit slot access`() {
        val o = Outfit.Default.with(Slot.EXTRA, "extra-siivet")
        assertEquals("extra-siivet", o[Slot.EXTRA])
        assertEquals("shoes-tossut", o[Slot.SHOES])
        assertFalse(o.isOn(Wardrobe.item("extra-strawberry-bag")))
        assertTrue(o.isOn(Wardrobe.item("extra-siivet")))
    }

    @Test
    fun `preview keeps required clothes and only the previewed item`() {
        val outfit =
            Outfit(
                mapOf(
                    Slot.HAIR to "hair-letit",
                    Slot.DRESS to "dress-perus",
                    Slot.SHOES to "shoes-tossut",
                    Slot.HEAD to "head-rusetti",
                ),
            )
        val item = Wardrobe.item("extra-strawberry-bag")
        val preview = outfit.previewFor(item)
        assertEquals(setOf(Slot.HAIR, Slot.DRESS, Slot.SHOES, Slot.EXTRA), preview.items.keys)
        assertEquals("extra-strawberry-bag", preview[Slot.EXTRA])
    }

    @Test
    fun `animal preview contains only the item`() {
        val item = Wardrobe.item("cat-neck-scarf")
        val preview = Outfit(mapOf(Slot.EXTRA to "cat-extra-yarn")).previewFor(item)
        assertEquals(mapOf(Slot.NECK to "cat-neck-scarf"), preview.items)
    }

    @Test
    fun `preview replaces a required slot with the previewed item`() {
        val preview = Outfit.Default.previewFor(Wardrobe.item("dress-tahti"))
        assertEquals("dress-tahti", preview[Slot.DRESS])
        assertEquals(setOf(Slot.HAIR, Slot.DRESS, Slot.SHOES), preview.items.keys)
    }

    @Test
    fun `items by slot keep the web order`() {
        assertEquals(
            listOf("hair-letit", "hair-pitkat", "hair-kiharat", "hair-sateenkaari"),
            Wardrobe.itemsFor(HUMAN, Slot.HAIR).map { it.id },
        )
    }
}
