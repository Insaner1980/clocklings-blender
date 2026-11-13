package fi.clocklings.ui.game

import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DOG
import fi.clocklings.domain.CompanionKind.NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleUnlocksTest {
    @Test
    fun `unlocks follow the companion shown`() {
        assertEquals(listOf("dog-head-cap"), visibleUnlocks(0, 1, DOG).map { it.id })
        assertEquals(listOf("cat-head-bow"), visibleUnlocks(0, 1, CAT).map { it.id })
        assertTrue(visibleUnlocks(0, 15, NONE).isEmpty())
    }
}
