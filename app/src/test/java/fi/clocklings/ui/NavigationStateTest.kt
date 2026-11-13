package fi.clocklings.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import fi.clocklings.Navigator
import fi.clocklings.Screen
import fi.clocklings.ScreenSavedState
import fi.clocklings.domain.AccessPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyDestinationAndPayloadSurvivesTheActualBackStackSaver() {
        val expected =
            listOf(
                Screen.Home,
                Screen.Settings,
                Screen.Language,
                Screen.Parent,
                Screen.FreeClock,
                Screen.LessonList,
                Screen.Lesson(listOf("viisarit", "kierros", "tasan"), thenLevel = 2),
                Screen.Levels,
                Screen.Game(level = 3, round = 8123456789L),
                Screen.Result(level = 4, stars = 2, previousBest = 1, starsBefore = 7),
                Screen.Challenge,
                Screen.Ask,
                Screen.Wardrobe,
                Screen.Personalize,
            )
        val restoration = StateRestorationTester(compose)
        lateinit var stack: MutableList<NavKey>
        restoration.setContent { stack = rememberNavBackStack(ScreenSavedState, *expected.toTypedArray()) }
        compose.runOnIdle { assertEquals(expected, stack.toList()) }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            assertEquals(expected, stack.toList())
            expected.zip(stack).forEach { (before, after) -> assertEquals(before::class, after::class) }
        }
    }

    @Test
    fun popReplaceAndPopToRetainTheIntendedParents() {
        val stack = mutableListOf<NavKey>(Screen.Home)
        val nav = Navigator(stack) { AccessPolicy(fullAccess = true) }
        nav.pop()
        assertEquals(listOf(Screen.Home), stack)
        nav.replace(Screen.Settings)
        assertEquals(listOf(Screen.Home, Screen.Settings), stack)
        nav.push(Screen.Language)
        nav.replace(Screen.Parent)
        assertEquals(listOf(Screen.Home, Screen.Settings, Screen.Parent), stack)
        nav.pop()
        nav.push(Screen.Levels)
        nav.push(Screen.Settings)
        nav.push(Screen.Language)
        nav.popTo(Screen.Settings)
        assertEquals(listOf(Screen.Home, Screen.Settings, Screen.Levels, Screen.Settings), stack)
        nav.pop()
        assertEquals(listOf(Screen.Home, Screen.Settings, Screen.Levels), stack)
        nav.popTo(Screen.Wardrobe)
        assertEquals(listOf(Screen.Home, Screen.Wardrobe), stack)
        nav.pop()
        assertEquals(listOf(Screen.Home), stack)
    }

    @Test
    fun lessonContinuationFinishAndReplayReplaceOnlyTheirOutgoingEntry() {
        val stack = mutableListOf<NavKey>(Screen.Home, Screen.Levels)
        val nav = Navigator(stack) { AccessPolicy(fullAccess = true) }
        nav.startLevel(1, listOf("puoli"), replaceTop = false)
        val lesson = Screen.Lesson(listOf("puoli"), thenLevel = 1)
        assertEquals(listOf(Screen.Home, Screen.Levels, lesson), stack)
        nav.ifCurrent(lesson) { nav.replace(Screen.Game(1, round = 1)) }
        val game = stack.last() as Screen.Game
        val result = Screen.Result(1, 2, 1, 4)
        repeat(2) { nav.ifCurrent(game) { nav.replace(result) } }
        assertEquals(listOf(Screen.Home, Screen.Levels, result), stack)
        nav.startLevel(1, emptyList(), replaceTop = true)
        val replay = stack.last() as Screen.Game
        assertNotEquals(game, replay)
        assertEquals(1, replay.level)
        assertEquals(3, stack.size)
        nav.pop()
        assertEquals(listOf(Screen.Home, Screen.Levels), stack)
    }
}
