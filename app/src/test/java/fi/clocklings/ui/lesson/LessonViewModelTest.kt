package fi.clocklings.ui.lesson

import fi.clocklings.audio.Sound
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Hand
import fi.clocklings.domain.Mark
import fi.clocklings.ui.FakeServices
import fi.clocklings.ui.MainDispatcherRule
import fi.clocklings.ui.components.OptionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val scope = TestScope(mainDispatcher.dispatcher)
    private val services = FakeServices(fullAccess = true)

    private fun vm(
        queue: List<String>,
        thenLevel: Int? = null,
    ) = LessonViewModel(queue, thenLevel, services, Random(5))

    @Test
    fun `page navigation and replay stop the previous narration`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            assertEquals(0, services.speechStops)
            vm.next()
            assertEquals(1, services.speechStops)
            vm.previous()
            assertEquals(2, services.speechStops)
            vm.replay()
            assertEquals(3, services.speechStops)
        }

    @Test
    fun `opens the first page and runs its demo`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            val s = vm.state.value
            assertEquals("viisarit", s.lesson.id)
            assertEquals(0, s.page)
            assertEquals(240, s.clock.time)
            assertNull(s.highlight)
            advanceTimeBy(701)
            assertEquals(Hand.HOUR, vm.state.value.highlight)
        }

    @Test
    fun `next page resets marks and restarts the demo`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            advanceTimeBy(800)
            vm.next()
            runCurrent()
            assertEquals(1, vm.state.value.page)
            assertNull(vm.state.value.highlight)
            advanceTimeBy(701)
            assertEquals(Hand.MINUTE, vm.state.value.highlight)
            vm.previous()
            runCurrent()
            assertEquals(0, vm.state.value.page)
        }

    @Test
    fun `page with initial marks starts with them`() =
        scope.runTest {
            val vm = vm(listOf("puoli"))
            vm.next()
            runCurrent()
            assertTrue(
                vm.state.value.under
                    .first() is Mark.Sector,
            )
            assertEquals(210, vm.state.value.clock.time)
        }

    @Test
    fun `demo moves the clock with an exact animation`() =
        scope.runTest {
            val vm = vm(listOf("kierros"))
            advanceTimeBy(1501)
            assertEquals(240, vm.state.value.clock.time)
            assertEquals(6000, vm.state.value.clock.ms)
            assertTrue(vm.state.value.clock.exact)
        }

    @Test
    fun `quarter example appears after the demo only`() =
        scope.runTest {
            val vm = vm(listOf("vartti"))
            vm.next()
            val text =
                vm.state.value.lesson.pages[1]
                    .text
            assertEquals(text.take(1), vm.state.value.visibleParagraphs)
            advanceTimeBy(3_299)
            assertEquals(195, vm.state.value.clock.time)
            assertEquals(text.take(1), vm.state.value.visibleParagraphs)
            advanceTimeBy(2)
            assertEquals(text, vm.state.value.visibleParagraphs)
        }

    @Test
    fun `replay and reduced motion show the settled example immediately`() =
        scope.runTest {
            val vm = vm(listOf("vartti"))
            vm.next()
            advanceTimeBy(1_000)
            vm.replay()
            assertEquals(195, vm.state.value.clock.time)
            assertEquals(0, vm.state.value.clock.ms)
            assertEquals(
                vm.state.value.lesson.pages[1]
                    .text,
                vm.state.value.visibleParagraphs,
            )

            vm.setReduceMotion(true)
            vm.next()
            assertEquals(225, vm.state.value.clock.time)
            assertEquals(0, vm.state.value.clock.ms)
            assertEquals(
                vm.state.value.lesson.pages[2]
                    .text,
                vm.state.value.visibleParagraphs,
            )
            advanceUntilIdle()
            assertEquals(225, vm.state.value.clock.time)
        }

    @Test
    fun `exact minute quiz highlights the start of the minute count`() =
        scope.runTest {
            val vm = vm(listOf("minuutit"))
            while (!vm.state.value.isQuiz) vm.next()
            assertEquals(187, vm.state.value.clock.time)
            assertEquals(listOf(Mark.NumberGlow(1)), vm.state.value.under)
            vm.setLanguage(AppLanguage.FINNISH)
            assertEquals(listOf(Mark.NumberGlow(1)), vm.state.value.under)
        }

    @Test
    fun `quiz hint after a wrong choice and completion is saved`() =
        scope.runTest {
            val vm = vm(listOf("viisarit"))
            repeat(3) { vm.next() }
            runCurrent()
            val s = vm.state.value
            assertTrue(s.isQuiz)
            val wrong = s.choices.indexOfFirst { !it.correct }
            vm.pick(wrong)
            assertEquals(OptionState.WRONG, vm.state.value.choiceStates[wrong])
            assertTrue(vm.state.value.showHint)
            val right = s.choices.indexOfFirst { it.correct }
            vm.pick(right)
            advanceUntilIdle()
            assertTrue(vm.state.value.quizDone)
            assertEquals(NextAction.FINISH, vm.state.value.nextAction)
            assertEquals(
                setOf("viisarit"),
                services.repository.progress
                    .first()
                    .lessonsDone,
            )
        }

    @Test
    fun `queue moves to the next lesson and ends with the level`() =
        scope.runTest {
            val vm = vm(listOf("puoli", "vrk"), thenLevel = 1)
            repeat(4) { vm.next() }
            vm.pick(
                vm.state.value.choices
                    .indexOfFirst { it.correct },
            )
            assertEquals(NextAction.NEXT_LESSON, vm.state.value.nextAction)
            runCurrent()
            vm.next()
            runCurrent()
            assertEquals("vrk", vm.state.value.lesson.id)
            assertEquals(1440, vm.state.value.period)
            repeat(3) { vm.next() }
            vm.pick(
                vm.state.value.choices
                    .indexOfFirst { it.correct },
            )
            assertEquals(NextAction.START_GAME, vm.state.value.nextAction)
            assertFalse(vm.state.value.finished)
            runCurrent()
            vm.next()
            assertTrue(vm.state.value.finished)
            assertEquals(2, services.sounds.count { it == Sound.LESSON_COMPLETE })
        }

    @Test
    fun `next is ignored on the quiz until it is answered`() =
        scope.runTest {
            val vm = vm(listOf("minuutit"))
            repeat(2) { vm.next() }
            vm.next()
            assertTrue(vm.state.value.isQuiz)
            assertFalse(vm.state.value.finished)
        }

    @Test
    fun `language change restarts only the current demo with its new reference hour`() =
        scope.runTest {
            val vm = vm(listOf("puoli"))
            repeat(2) { vm.next() }
            advanceTimeBy(701)
            assertTrue(Mark.NumberGlow(3) in vm.state.value.under)
            vm.setLanguage(AppLanguage.FINNISH)
            assertEquals(2, vm.state.value.page)
            assertEquals(210, vm.state.value.clock.time)
            assertFalse(Mark.NumberGlow(4) in vm.state.value.under)
            advanceTimeBy(701)
            assertTrue(Mark.NumberGlow(4) in vm.state.value.under)
            assertEquals(0, vm.state.value.celebrate)
        }

    @Test
    fun `language change retains wrong choices completed quiz and queue position`() =
        scope.runTest {
            val vm = vm(listOf("puoli", "vrk"))
            repeat(4) { vm.next() }
            val choices = vm.state.value.choices
            val wrong = choices.indexOfFirst { !it.correct }
            vm.pick(wrong)
            val before = vm.state.value
            vm.setLanguage(AppLanguage.SWEDISH)
            assertEquals(before.choices, vm.state.value.choices)
            assertEquals(before.choiceStates, vm.state.value.choiceStates)
            assertTrue(vm.state.value.showHint)
            val right = choices.indexOfFirst { it.correct }
            vm.pick(right)
            runCurrent()
            val completed = vm.state.value
            vm.setLanguage(AppLanguage.FINNISH)
            vm.pick(right)
            assertEquals(completed, vm.state.value)
            assertEquals(1, vm.state.value.celebrate)
            assertEquals(0, vm.state.value.queueIndex)
            assertEquals(
                setOf("puoli"),
                services.repository.progress
                    .first()
                    .lessonsDone,
            )
        }
}
