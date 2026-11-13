package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CompanionColorOptionsTest {
    @Test
    fun `dragon swaps purple for orange`() {
        val options = CompanionKind.DRAGON.colorOptions()
        assertFalse(CompanionColor.PURPLE in options)
        assertEquals(
            listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.GREEN, CompanionColor.ORANGE),
            options,
        )
    }

    @Test
    fun `dinosaur swaps green for orange`() {
        val options = CompanionKind.DINOSAUR.colorOptions()
        assertFalse(CompanionColor.GREEN in options)
        assertEquals(
            listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.PURPLE, CompanionColor.ORANGE),
            options,
        )
    }

    @Test
    fun `other kinds keep the original four options`() {
        val others = CompanionKind.entries - CompanionKind.DRAGON - CompanionKind.DINOSAUR
        for (kind in others) {
            assertEquals(
                kind.name,
                listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.GREEN, CompanionColor.PURPLE),
                kind.colorOptions(),
            )
        }
    }

    @Test
    fun `a colour the kind no longer offers reads as original`() {
        val stored =
            Personalization(
                styles =
                    mapOf(
                        CompanionKind.DRAGON to CompanionStyle(CompanionColor.PURPLE),
                        CompanionKind.DINOSAUR to CompanionStyle(CompanionColor.GREEN),
                        CompanionKind.CAT to CompanionStyle(CompanionColor.PURPLE),
                    ),
            )
        assertEquals(CompanionStyle(), stored.styleFor(CompanionKind.DRAGON))
        assertEquals(CompanionStyle(), stored.styleFor(CompanionKind.DINOSAUR))
        assertEquals(CompanionStyle(CompanionColor.PURPLE), stored.styleFor(CompanionKind.CAT))
    }
}
