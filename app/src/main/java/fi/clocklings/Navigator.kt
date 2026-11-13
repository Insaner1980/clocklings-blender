package fi.clocklings

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import fi.clocklings.domain.AccessPolicy

/** Paluupinon muokkaus yhdessä paikassa. Koti on aina pohjalla. */
@Stable
class Navigator(
    private val stack: MutableList<NavKey>,
    private val beforeNavigate: () -> Unit = {},
    private val access: () -> AccessPolicy,
) {
    init {
        // Restored routes are checked before any restricted ViewModel is constructed.
        stack.indices.forEach { index ->
            stack[index] = (stack[index] as Screen).accessibleDestination(access())
        }
    }

    /** Ignore callbacks from a destination that has already left the top of the stack. */
    fun ifCurrent(
        screen: Screen,
        action: () -> Unit,
    ) {
        if (stack.last() == screen) action()
    }

    fun push(screen: Screen) {
        beforeNavigate()
        stack.add(screen.accessibleDestination(access()))
    }

    fun pop() {
        beforeNavigate()
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack[stack.lastIndex] = (stack.last() as Screen).accessibleDestination(access())
    }

    /** Korvaa päällimmäisen näkymän (esim. peli → tulos), jolloin takaisin vie edelliseen näkymään. */
    fun replace(screen: Screen) {
        beforeNavigate()
        val target = screen.accessibleDestination(access())
        if (stack.size > 1) stack[stack.lastIndex] = target else stack.add(target)
    }

    /** Palaa pinossa näkymään [screen] tai lisää sen kodin päälle. */
    fun popTo(screen: Screen) {
        beforeNavigate()
        val target = screen.accessibleDestination(access())
        val index = stack.indexOfLast { it == target }
        if (index >= 0) {
            while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
        } else {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            stack.add(target)
        }
    }

    /** Tason aloitus: ensin oppitunnit, jos niitä on, muuten suoraan peliin. */
    fun startLevel(
        level: Int,
        lessonQueue: List<String>,
        replaceTop: Boolean,
    ) {
        if (!access().level(level)) return
        val allowedQueue = access().lessonQueue(lessonQueue)
        val target =
            if (allowedQueue.isEmpty()) {
                Screen.Game(level, round = System.nanoTime())
            } else {
                Screen.Lesson(allowedQueue, thenLevel = level)
            }
        if (replaceTop) replace(target) else push(target)
    }
}

internal fun Screen.accessibleDestination(access: AccessPolicy): Screen =
    when (this) {
        is Screen.Game -> {
            if (access.level(level)) this else Screen.Levels
        }

        is Screen.Lesson -> {
            val allowed = access.lessonQueue(queue)
            if (allowed.isEmpty()) {
                Screen.LessonList
            } else {
                copy(
                    queue = allowed,
                    thenLevel = thenLevel?.takeIf(access::level),
                )
            }
        }

        Screen.Challenge -> {
            if (access.challenge()) this else Screen.Levels
        }

        else -> {
            this
        }
    }
