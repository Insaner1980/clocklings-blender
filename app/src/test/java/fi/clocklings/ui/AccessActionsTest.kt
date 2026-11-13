package fi.clocklings.ui

import fi.clocklings.domain.Progress
import fi.clocklings.ui.challenge.ChallengePhase
import fi.clocklings.ui.challenge.ChallengeViewModel
import fi.clocklings.ui.game.GameViewModel
import fi.clocklings.ui.lesson.LessonViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AccessActionsTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test
    fun directRestrictedActionsCannotStartOrWriteLearningData() =
        runTest {
            val services = FakeServices(fullAccess = false)
            val app = AppViewModel(services)
            runCurrent()
            assertNull(app.startLevel(4))
            assertThrows(IllegalArgumentException::class.java) { GameViewModel(4, false, services) }
            assertThrows(IllegalArgumentException::class.java) { LessonViewModel(listOf("minuutit"), null, services) }
            val challenge = ChallengeViewModel(false, services)
            challenge.start()
            runCurrent()
            assertEquals(ChallengePhase.START, challenge.state.value.phase)
            assertEquals(Progress(), services.repository.progress.first())
        }

    @Test
    fun entitlementChangesAndProgressResetRemainSeparate() =
        runTest {
            val services = FakeServices(fullAccess = false)
            val saved = Progress(stars = listOf(3, 2, 1, 0, 0), lessonsDone = setOf("viisarit", "puoli"))
            services.repository.updateProgress { saved }
            val app = AppViewModel(services)
            runCurrent()
            services.purchases.state.value =
                services.purchases.state.value
                    .copy(fullAccess = true)
            runCurrent()
            assertTrue(app.access.level(4))
            assertEquals(saved, services.repository.progress.first())
            app.resetProgress()
            runCurrent()
            assertEquals(Progress(), services.repository.progress.first())
            assertTrue(services.purchases.state.value.fullAccess)
        }

    @Test
    fun accessLossAllowsTheActiveLessonToFinishButStopsTheRestrictedQueue() =
        runTest {
            val services = FakeServices(fullAccess = true)
            val lesson = LessonViewModel(listOf("viisarit", "puoli"), null, services)
            services.purchases.state.value =
                services.purchases.state.value
                    .copy(fullAccess = false)
            runCurrent()
            assertEquals("viisarit", lesson.state.value.lesson.id)
            assertEquals(0, lesson.state.value.page)
            repeat(lesson.state.value.lesson.pages.size) { lesson.next() }
            val quiz = lesson.state.value
            lesson.pick(quiz.choices.indexOfFirst { it.correct })
            runCurrent()
            lesson.next()
            assertTrue(lesson.state.value.finished)
            assertEquals("viisarit", lesson.state.value.lesson.id)
            val progress = services.repository.progress.first()
            assertEquals(setOf("viisarit"), progress.lessonsDone)
        }
}
