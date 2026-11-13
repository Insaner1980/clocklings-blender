package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessPolicyTest {
    @Test
    fun freeAccessIncludesTheCompleteFirstLevelAndUnlimitedReplays() {
        val access = AccessPolicy(fullAccess = false)
        assertEquals(listOf("viisarit", "kierros", "tasan"), Lessons.levelLessons[0])
        repeat(20) {
            assertTrue(access.level(0))
            Lessons.levelLessons[0].forEach { assertTrue(access.lesson(it)) }
        }
        (1..4).forEach { assertFalse(access.level(it)) }
        assertFalse(access.challenge())
    }

    @Test
    fun fullAccessIncludesEveryExistingLevelLessonAndChallenge() {
        val access = AccessPolicy(fullAccess = true)
        (0..4).forEach { assertTrue(access.level(it)) }
        Lessons.levelLessons.flatten().forEach { assertTrue(access.lesson(it)) }
        assertTrue(access.challenge())
    }

    @Test
    fun automaticOrRestoredQueuesCannotSmuggleRestrictedLessons() {
        val queue = listOf("viisarit", "puoli", "unknown", "tasan", "minuutit")
        assertEquals(listOf("viisarit", "tasan"), AccessPolicy(false).lessonQueue(queue))
        assertEquals(queue - "unknown", AccessPolicy(true).lessonQueue(queue))
        for (full in listOf(false, true)) {
            assertFalse(AccessPolicy(full).level(-1))
            assertFalse(AccessPolicy(full).level(5))
            assertFalse(AccessPolicy(full).lesson("unknown"))
        }
    }
}
