package fi.clocklings.domain

import fi.clocklings.domain.CompanionKind.BUNNY
import fi.clocklings.domain.CompanionKind.CAR
import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DINOSAUR
import fi.clocklings.domain.CompanionKind.DOG
import fi.clocklings.domain.CompanionKind.DRAGON
import fi.clocklings.domain.CompanionKind.HUMAN
import fi.clocklings.domain.CompanionKind.NONE
import fi.clocklings.domain.CompanionKind.ROBOT
import fi.clocklings.domain.CompanionKind.UNICORN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionItemsTest {
    private val animals = listOf(CAT, DOG, ROBOT, DRAGON, DINOSAUR, BUNNY, UNICORN)
    private val wardrobes = animals + CAR

    @Test
    fun `every companion but the doll has one free item and one item per star`() =
        wardrobes.forEach { kind ->
            val items = Wardrobe.itemsFor(kind)
            assertEquals(16, items.size)
            assertEquals(1, items.count { it.star == 0 })
            assertEquals((1..15).toList(), items.filter { it.star > 0 }.map { it.star }.sorted())
        }

    @Test
    fun `items use only their companion's slots and match their ids`() =
        wardrobes.forEach { kind ->
            Wardrobe.itemsFor(kind).forEach {
                assertTrue(it.slot in kind.slots)
                assertEquals(it.slot.name.lowercase(), it.id.split("-")[1])
                assertTrue(it.id.startsWith(kind.name.lowercase() + "-"))
                assertEquals(kind, it.companion)
            }
        }

    @Test
    fun `ids are unique across all companions`() =
        assertEquals(
            Wardrobe.items.size,
            Wardrobe.items
                .map { it.id }
                .toSet()
                .size,
        )

    @Test
    fun `newly unlocked is per companion`() {
        assertEquals(listOf("cat-head-bow", "cat-extra-yarn"), Wardrobe.newlyUnlocked(CAT, 0, 2).map { it.id })
        assertEquals(listOf("robot-neck-bow-tie"), Wardrobe.newlyUnlocked(ROBOT, 0, 1).map { it.id })
        assertTrue(Wardrobe.newlyUnlocked(NONE, 0, 15).isEmpty())
    }

    @Test
    fun `locked count is per companion`() {
        assertEquals(15, Wardrobe.lockedCount(DOG, 0))
        assertEquals(0, Wardrobe.lockedCount(DOG, 15))
    }

    @Test
    fun `slots per companion`() {
        assertEquals(listOf(Slot.HAIR, Slot.DRESS, Slot.SHOES, Slot.HEAD, Slot.EXTRA), HUMAN.slots)
        assertTrue(NONE.slots.isEmpty())
        animals.forEach { assertEquals(listOf(Slot.HEAD, Slot.NECK, Slot.EXTRA), it.slots) }
        assertEquals(listOf(Slot.ROOF, Slot.FRONT, Slot.WHEELS, Slot.MODEL), CAR.slots)
    }

    @Test
    fun `every car slot has items and the car has no head`() {
        CAR.slots.forEach { assertTrue(it.name, Wardrobe.itemsFor(CAR, it).isNotEmpty()) }
        assertTrue(Wardrobe.itemsFor(CAR, Slot.HEAD).isEmpty())
        assertTrue(CAR.slots.all { it.optional })
    }

    @Test
    fun `each animal has its free item and default outfit is empty`() {
        val free =
            mapOf(
                CAT to "cat-neck-scarf",
                DOG to "dog-neck-bandana",
                ROBOT to "robot-head-antenna",
                DRAGON to "dragon-neck-pendant",
                DINOSAUR to "dinosaur-extra-backpack",
                BUNNY to "bunny-neck-ribbon",
                UNICORN to "unicorn-neck-lei",
                CAR to "car-front-plate",
            )
        free.forEach { (kind, id) -> assertEquals(listOf(id), Wardrobe.freeItems(kind).map { it.id }) }
        wardrobes.forEach { assertEquals(Outfit(), Outfit.default(it)) }
        assertEquals(Outfit.Default, Outfit.default(HUMAN))
        assertEquals(Outfit(), Outfit.default(NONE))
    }

    @Test
    fun `item lookup by id`() {
        assertEquals(CAT, Wardrobe.item("cat-head-bow").companion)
        assertNull(Wardrobe.itemOrNull("no-such-item"))
    }

    @Test
    fun `animal outfit toggles through optional slots`() {
        val bow = Wardrobe.item("cat-head-bow")
        val on = Wardrobe.toggle(Outfit(), bow)
        assertTrue(on.isOn(bow))
        assertEquals(Outfit(), Wardrobe.toggle(on, bow))
    }
}
