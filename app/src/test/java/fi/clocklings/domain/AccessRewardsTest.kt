package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessRewardsTest {
    @Test fun onlyHumanCatAndNoneAreFreeRegardlessOfSavedStars() {
        val free = AccessPolicy(false)
        for (stars in 0..Wardrobe.MAX_STARS) {
            assertEquals(
                listOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.NONE),
                CompanionKind.entries.filter { free.companion(it, stars) },
            )
        }
    }

    @Test fun allOtherCompanionsBecomeEligibleTogetherAtThreeEarnedStars() {
        val full = AccessPolicy(true)
        for (kind in CompanionKind.entries - setOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.NONE)) {
            assertFalse(full.companion(kind, 2))
            assertTrue(full.companion(kind, 3))
        }
    }

    @Test fun freeSelectionIsExactlyTheTenApprovedItemsAndStillRequiresEarnedStars() {
        val free = AccessPolicy(false)
        assertEquals(
            setOf(
                "hair-letit",
                "dress-perus",
                "shoes-tossut",
                "dress-pilkku",
                "head-rusetti",
                "shoes-punaiset",
                "cat-neck-scarf",
                "cat-head-bow",
                "cat-extra-yarn",
                "cat-neck-bell-collar",
            ),
            Wardrobe.items
                .filter(free::itemContent)
                .map { it.id }
                .toSet(),
        )
        for (item in Wardrobe.items) {
            for (stars in 0..Wardrobe.MAX_STARS) {
                assertEquals(free.itemContent(item) && stars >= item.star, free.item(item, stars))
                val full = AccessPolicy(true)
                assertEquals(stars >= item.star && full.companion(item.companion, stars), full.item(item, stars))
            }
        }
    }

    @Test fun displayFallbackRetainsSavedChoicesAndRestoresThemWithAccess() {
        val saved = Outfit.Default.with(Slot.DRESS, "dress-sateenkaari").with(Slot.HEAD, "head-kruunu")
        assertEquals(Outfit.Default, AccessPolicy(false).displayedOutfit(CompanionKind.HUMAN, saved, 15))
        assertEquals(saved, AccessPolicy(true).displayedOutfit(CompanionKind.HUMAN, saved, 15))
        assertEquals(CompanionKind.HUMAN, AccessPolicy(false).displayedCompanion(CompanionKind.DRAGON, 15))
        assertEquals(CompanionKind.DRAGON, AccessPolicy(true).displayedCompanion(CompanionKind.DRAGON, 15))
        for (full in listOf(false, true)) {
            assertEquals(CompanionKind.NONE, AccessPolicy(full).displayedCompanion(CompanionKind.NONE, 0))
            assertTrue(AccessPolicy(full).lesson(Lessons.DAY_LESSON))
        }
    }
}
