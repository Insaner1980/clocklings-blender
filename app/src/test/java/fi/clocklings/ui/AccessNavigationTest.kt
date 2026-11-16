package fi.clocklings.ui

import androidx.navigation3.runtime.NavKey
import fi.clocklings.Navigator
import fi.clocklings.Screen
import fi.clocklings.domain.AccessPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessNavigationTest {
    @Test
    fun navigationStopsOldSpeechBeforeChangingTheDestination() {
        val stack = mutableListOf<NavKey>(Screen.Home)
        val stoppedAt = mutableListOf<NavKey>()
        val nav = Navigator(stack, beforeNavigate = { stoppedAt += stack.last() }) { AccessPolicy(true) }
        nav.push(Screen.Settings)
        nav.replace(Screen.Levels)
        nav.popTo(Screen.Home)
        nav.push(Screen.Settings)
        nav.pop()
        assertEquals(listOf(Screen.Home, Screen.Settings, Screen.Levels, Screen.Home, Screen.Settings), stoppedAt)
        assertEquals(listOf(Screen.Home), stack)
    }

    @Test
    fun navigationReportsOnlyChangedDestinationsAfterApplyingAccessRules() {
        val stack = mutableListOf<NavKey>(Screen.Home)
        val reported = mutableListOf<Screen>()
        val nav =
            Navigator(stack, onDestinationChanged = {
                assertEquals(stack.last(), it)
                reported += it
            }) { AccessPolicy(fullAccess = false) }
        nav.pop()
        nav.ifCurrent(Screen.Home) { nav.push(Screen.Settings) }
        nav.ifCurrent(Screen.Home) { nav.push(Screen.FreeClock) }
        nav.replace(Screen.Challenge)
        nav.startLevel(4, emptyList(), false)
        nav.replace(Screen.Levels)
        nav.popTo(Screen.Home)
        assertEquals(listOf(Screen.Settings, Screen.Levels, Screen.Home), reported)
    }

    @Test
    fun restoredQueuesKeepOnlyAccessibleLessonsAndCannotStartPaidPractice() {
        val stack = mutableListOf<NavKey>(Screen.Home, Screen.Lesson(listOf("viisarit", "puoli", "tasan"), 4))
        Navigator(stack) { AccessPolicy(fullAccess = false) }
        assertEquals(Screen.Lesson(listOf("viisarit", "tasan")), stack.last())
    }

    @Test
    fun restoredGamesChallengesAndEntirelyRestrictedQueuesFallBackToLists() {
        for ((route, fallback) in listOf(
            Screen.Game(4) to Screen.Levels,
            Screen.Challenge to Screen.Levels,
            Screen.Lesson(listOf("minuutit")) to Screen.LessonList,
        )) {
            val stack = mutableListOf<NavKey>(Screen.Home, route)
            Navigator(stack) { AccessPolicy(fullAccess = false) }
            assertEquals(fallback, stack.last())
        }
    }

    @Test
    fun replayReplacementPushAndPopToAllEnforceCurrentAccess() {
        val stack = mutableListOf<NavKey>(Screen.Home)
        var full = true
        val nav = Navigator(stack) { AccessPolicy(fullAccess = full) }
        nav.push(Screen.Game(3))
        assertEquals(Screen.Game(3), stack.last())
        full = false
        nav.replace(Screen.Game(3, 1))
        assertEquals(Screen.Levels, stack.last())
        nav.push(Screen.Challenge)
        assertEquals(Screen.Levels, stack.last())
        nav.popTo(Screen.Game(3))
        assertEquals(Screen.Levels, stack.last())
        val before = stack.toList()
        nav.startLevel(3, listOf("viisi"), false)
        assertEquals(before, stack)
    }

    @Test
    fun backAfterAccessLossDoesNotReopenRestrictedContent() {
        val stack = mutableListOf<NavKey>(Screen.Home, Screen.Game(2), Screen.Settings)
        var full = true
        val nav = Navigator(stack) { AccessPolicy(full) }
        full = false
        nav.pop()
        assertEquals(Screen.Levels, stack.last())
    }

    @Test
    fun freeAutomaticQueueAndRepeatedPracticeStayAvailable() {
        val stack = mutableListOf<NavKey>(Screen.Home)
        val nav = Navigator(stack) { AccessPolicy(fullAccess = false) }
        nav.startLevel(0, listOf("viisarit", "puoli", "tasan"), false)
        assertEquals(Screen.Lesson(listOf("viisarit", "tasan"), 0), stack.last())
        repeat(20) {
            nav.startLevel(0, emptyList(), true)
            assertEquals(0, (stack.last() as Screen.Game).level)
        }
    }
}
