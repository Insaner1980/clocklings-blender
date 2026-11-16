package fi.clocklings.ui.game

import fi.clocklings.AppServices
import fi.clocklings.Screen
import fi.clocklings.analytics.UsageAnalytics
import fi.clocklings.audio.Sound
import fi.clocklings.domain.Hint
import fi.clocklings.domain.Levels
import fi.clocklings.domain.TaskType
import fi.clocklings.ui.FakeServices
import fi.clocklings.ui.MainDispatcherRule
import fi.clocklings.ui.components.DotOutcome
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val scope = TestScope(mainDispatcher.dispatcher)
    private val services = FakeServices(fullAccess = true)

    @Test
    fun recreatedClockUsesTheSameTimeAsTheSubmittedAnswer() =
        scope.runTest {
            val vm = game()
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1_600)
            runCurrent()
            assertEquals(TaskType.SET, vm.state.value.task.type)
            val target = vm.state.value.task.time
            vm.onClockChange(target)
            assertEquals(target, vm.state.value.clock.time)
            vm.done()
            assertEquals(0, vm.state.value.mistakes)
        }

    @Test
    fun repeatedAnswerCannotAdvanceBefore1500MillisecondsOrSkipATask() =
        scope.runTest {
            val vm = game()
            val answer = vm.state.value.task.time
            vm.pick(answer)
            vm.pick(answer)
            runCurrent()
            advanceTimeBy(1499)
            vm.pick(answer)
            assertEquals(0, vm.state.value.taskIndex)
            assertEquals(1, vm.state.value.celebrate)
            assertEquals(listOf(Sound.QUESTION_READ, Sound.OK), services.sounds)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(1, vm.state.value.outcomes.size)
        }

    @Test
    fun analyticsCountEachShownTaskAndOutcomeOnce() =
        scope.runTest {
            val metrics = mutableListOf<String>()
            val analytics =
                object : UsageAnalytics by UsageAnalytics.Disabled {
                    override fun count(metric: String) {
                        metrics.add(metric)
                    }
                }
            val instrumented =
                object : AppServices by services {
                    override val usage = analytics
                }
            val vm = GameViewModel(0, false, instrumented, Random(11))
            val answer = vm.state.value.task.time
            vm.pick(answer)
            vm.pick(answer)
            advanceTimeBy(1_600)
            val target = vm.state.value.task.time
            vm.onClockChange((target + 60) % 720)
            repeat(3) { vm.done() }
            assertEquals(
                listOf("game.0.start", "game.0.read", "game.0.read_first", "game.0.set", "game.0.set_reveal"),
                metrics,
            )
            vm.next()
            vm.next()
            assertEquals("game.0.read", metrics.last())
            assertEquals(6, metrics.size)
        }

    private fun game(
        level: Int = 0,
        h24: Boolean = false,
    ) = GameViewModel(level, h24, services, Random(11))

    private fun GameViewModel.revealReadAnswer() {
        val initial = state.value
        initial.options.filter { it != initial.task.time }.forEach(::pick)
    }

    private fun TestScope.revealSetAnswer(vm: GameViewModel): Int {
        vm.pick(vm.state.value.task.time)
        advanceTimeBy(1600)
        val target = vm.state.value.task.time
        vm.onClockChange((target + 60) % 720)
        vm.done()
        vm.done()
        return target
    }

    private fun TestScope.finishPerfectRound(vm: GameViewModel) {
        repeat(10) {
            val task = vm.state.value.task
            if (task.type == TaskType.READ) {
                vm.pick(task.time)
            } else {
                vm.onClockChange(task.time)
                vm.done()
            }
            advanceTimeBy(1600)
        }
        advanceUntilIdle()
    }

    @Test
    fun `starts with a read task showing three options and the clock at the answer`() =
        scope.runTest {
            val vm = game()
            val s = vm.state.value
            assertEquals(0, s.taskIndex)
            assertEquals(TaskType.READ, s.task.type)
            assertEquals(3, s.options.size)
            assertTrue(s.task.time in s.options)
            assertEquals(s.task.time, s.clock.time)
            assertFalse(s.interactive)
        }

    @Test
    fun `correct answer praises and moves on after a pause`() =
        scope.runTest {
            val vm = game()
            val answer = vm.state.value.task.time
            vm.pick(answer)
            val s = vm.state.value
            assertEquals(OptionState.CORRECT, s.optionStates[answer])
            assertTrue(s.feedback is GameFeedback.Praise)
            assertEquals(1, s.celebrate)
            assertEquals(listOf(Sound.QUESTION_READ, Sound.OK), services.sounds)
            advanceTimeBy(1600)
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(TaskType.SET, vm.state.value.task.type)
            assertTrue(vm.state.value.interactive)
            assertEquals(Sound.QUESTION_SET, services.sounds.last())
        }

    @Test
    fun `the narrator reads a hand hint once the wrong-answer sound has faded`() =
        scope.runTest {
            val vm = game(level = 2)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            vm.onClockChange((target + 30) % 720)
            vm.done()
            assertTrue((vm.state.value.feedback as GameFeedback.HintText).hint is Hint.SetMinute)
            advanceTimeBy(1249)
            assertEquals(Sound.BAD, services.sounds.last())
            advanceTimeBy(2)
            assertEquals(Sound.HINT_MINUTE, services.sounds.last())
        }

    @Test
    fun `an answer before the hint is read cancels the stale hint`() =
        scope.runTest {
            val vm = game()
            vm.revealReadAnswer()
            advanceTimeBy(5_000)
            assertTrue(services.sounds.none { it == Sound.HINT_HOUR || it == Sound.HINT_MINUTE })
        }

    @Test
    fun `first mistake gives a hint and second reveals the answer`() =
        scope.runTest {
            val vm = game()
            val s0 = vm.state.value
            val wrong = s0.options.filter { it != s0.task.time }
            vm.pick(wrong[0])
            assertTrue(vm.state.value.feedback is GameFeedback.HintText)
            assertEquals(OptionState.WRONG, vm.state.value.optionStates[wrong[0]])
            vm.pick(wrong[0])
            assertEquals(1, vm.state.value.mistakes)
            vm.pick(wrong[1])
            val s = vm.state.value
            assertEquals(2, s.mistakes)
            assertEquals(GameFeedback.RevealRead(s0.task.time), s.feedback)
            assertEquals(OptionState.CORRECT, s.optionStates[s0.task.time])
            assertTrue(s.showNext)
            vm.pick(s0.task.time)
            assertEquals(2, vm.state.value.mistakes)
            vm.next()
            assertEquals(1, vm.state.value.taskIndex)
        }

    @Test
    fun `set task accepts the right hand position and hints otherwise`() =
        scope.runTest {
            val vm = game(level = 2)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            assertEquals(Levels.setTaskStart(target), vm.state.value.clock.time)
            assertTrue(vm.state.value.showDone)
            vm.onClockChange((target + 30) % 720)
            vm.done()
            val hint = (vm.state.value.feedback as GameFeedback.HintText).hint
            assertTrue(hint is Hint.SetMinute)
            vm.onClockChange(target)
            vm.done()
            assertTrue(vm.state.value.feedback is GameFeedback.Praise)
            assertEquals(target, vm.state.value.clock.time)
            assertFalse(vm.state.value.interactive)
        }

    @Test
    fun `24 h set task accepts the same hands in the other day half`() =
        scope.runTest {
            val vm = game(level = 1, h24 = true)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val target = vm.state.value.task.time
            vm.onClockChange((target + 720) % 1440)
            vm.done()
            assertTrue(vm.state.value.feedback is GameFeedback.Praise)
        }

    @Test
    fun `second set mistake shows the answer on the clock`() =
        scope.runTest {
            val vm = game(level = 0)
            val target = revealSetAnswer(vm)
            val s = vm.state.value
            assertEquals(GameFeedback.RevealSet(target), s.feedback)
            assertEquals(target, s.clock.time)
            assertEquals(1400, s.clock.ms)
            assertFalse(s.showDone)
            assertTrue(s.showNext)
        }

    @Test
    fun `finishing ten tasks records stars and reports the result`() =
        scope.runTest {
            val vm = game(level = 0)
            finishPerfectRound(vm)
            val result = vm.state.value.finished
            assertNotNull(result)
            assertEquals(Screen.Result(level = 0, stars = 3, previousBest = 0, starsBefore = 0), result)
            assertEquals(
                3,
                services.repository.progress
                    .first()
                    .stars[0],
            )
            assertEquals(Sound.WIN, services.sounds.last())
        }

    @Test
    fun `correct first answer records FIRST_TRY`() =
        scope.runTest {
            val vm = game()
            assertTrue(
                vm.state.value.outcomes
                    .isEmpty(),
            )
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(listOf(DotOutcome.FIRST_TRY), vm.state.value.outcomes)
        }

    @Test
    fun `one wrong answer then right records WITH_HINT`() =
        scope.runTest {
            val vm = game()
            val s0 = vm.state.value
            vm.pick(s0.options.first { it != s0.task.time })
            vm.pick(s0.task.time)
            advanceTimeBy(1600)
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(listOf(DotOutcome.WITH_HINT), vm.state.value.outcomes)
        }

    @Test
    fun `two wrong answers then next records SHOWN`() =
        scope.runTest {
            val vm = game()
            vm.revealReadAnswer()
            vm.next()
            assertEquals(1, vm.state.value.taskIndex)
            assertEquals(listOf(DotOutcome.SHOWN), vm.state.value.outcomes)
        }

    @Test
    fun `set task shown after two mistakes records SHOWN`() =
        scope.runTest {
            val vm = game(level = 0)
            revealSetAnswer(vm)
            vm.next()
            assertEquals(2, vm.state.value.taskIndex)
            assertEquals(listOf(DotOutcome.FIRST_TRY, DotOutcome.SHOWN), vm.state.value.outcomes)
        }

    @Test
    fun `outcomes carry through to the final task and stay one per task`() =
        scope.runTest {
            val vm = game(level = 0)
            finishPerfectRound(vm)
            assertEquals(List(10) { DotOutcome.FIRST_TRY }, vm.state.value.outcomes)
            // The last dot stays popped while the result is saved; its outcome is not added a second time.
            assertTrue(vm.state.value.feedback is GameFeedback.Praise)
            assertEquals(List(10) { DotOutcome.FIRST_TRY }, dotOutcomes(vm.state.value, currentDone = true))
        }

    @Test
    fun `set task solved after one mistake records WITH_HINT and its dot pops gold`() =
        scope.runTest {
            val vm = game(level = 0)
            vm.pick(vm.state.value.task.time)
            advanceTimeBy(1600)
            val s1 = vm.state.value
            assertEquals(TaskType.SET, s1.task.type)
            vm.onClockChange((s1.task.time + 60) % 720)
            vm.done()
            vm.onClockChange(s1.task.time)
            vm.done()
            val praised = vm.state.value
            assertTrue(praised.feedback is GameFeedback.Praise)
            // During the success pause the current dot already pops in its outcome colour.
            assertEquals(listOf(DotOutcome.FIRST_TRY), praised.outcomes)
            assertEquals(listOf(DotOutcome.FIRST_TRY, DotOutcome.WITH_HINT), dotOutcomes(praised, currentDone = true))
            advanceTimeBy(1600)
            assertEquals(2, vm.state.value.taskIndex)
            assertEquals(listOf(DotOutcome.FIRST_TRY, DotOutcome.WITH_HINT), vm.state.value.outcomes)
        }

    @Test
    fun `outcomeOf maps mistakes`() {
        assertEquals(DotOutcome.FIRST_TRY, outcomeOf(0))
        assertEquals(DotOutcome.WITH_HINT, outcomeOf(1))
        assertEquals(DotOutcome.SHOWN, outcomeOf(2))
    }

    @Test
    fun `no result before the last task`() =
        scope.runTest {
            val vm = game()
            assertNull(vm.state.value.finished)
        }
}
