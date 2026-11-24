package fi.clocklings.ui.lesson

import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.DemoStep
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCatalogTest {
    private val all = LessonCatalog.all(AppLanguage.ENGLISH)

    @Test
    fun fiveMinuteLessonExplainsFullHourInParagraph() {
        for (language in AppLanguage.entries) {
            val page = LessonCatalog.lesson("viisi", language).pages.first()
            val captions = page.demo.filterIsInstance<DemoStep.Say>()
            val texts = captions.map { it.text as Text.Res }
            assertEquals(List(12) { R.string.say_number_minutes }, texts.map { it.id })
            assertEquals(listOf(12, 60), texts.last().args)
        }
    }

    @Test
    fun `five minute explanation reaches each number before naming it`() {
        for (language in AppLanguage.entries) assertFiveMinuteExplanation(language)
    }

    private fun assertFiveMinuteExplanation(language: AppLanguage) {
        val page = LessonCatalog.lesson("viisi", language).pages.first()
        var reachedTime = page.time
        var movingTo = page.time
        var remaining = 0L
        val explained = mutableListOf<Int>()
        for (step in page.demo) {
            when (step) {
                is DemoStep.Move -> {
                    movingTo = step.to
                    remaining = step.ms.toLong()
                }

                is DemoStep.Wait -> {
                    remaining -= step.ms
                    if (remaining <= 0) reachedTime = movingTo
                }

                is DemoStep.Say -> {
                    if (step.text is Text.Res) {
                        val number = step.text.args.first() as Int
                        assertEquals("$language number $number", 180 + number * 5, reachedTime)
                        explained += number
                    }
                }

                else -> {}
            }
        }
        assertEquals((1..12).toList(), explained)
    }

    @Test
    fun `nine lessons in the web order`() {
        assertEquals(
            listOf("viisarit", "kierros", "tasan", "puoli", "vartti", "ylivaille", "viisi", "minuutit", "vrk"),
            all.map { it.id },
        )
    }

    @Test
    fun `the half hour example ends the past and to lesson in every language`() {
        for (language in AppLanguage.entries) {
            val example = LessonCatalog.lesson("ylivaille", language).pages.last()
            assertEquals(listOf(Text.Res(R.string.l_viisi_p3a), Text.Res(R.string.l_viisi_p3b)), example.text)
            assertEquals(listOf(205, 215), example.demo.filterIsInstance<DemoStep.Move>().map { it.to })
            assertEquals(2, LessonCatalog.lesson("viisi", language).pages.size)
        }
    }

    @Test
    fun `every level lesson exists and only the day lesson is 24 h only`() {
        Lessons.levelLessons.flatten().forEach { LessonCatalog.lesson(it) }
        assertEquals(listOf(Lessons.DAY_LESSON), all.filter { it.h24Only }.map { it.id })
    }

    @Test
    fun `each quiz has exactly one correct choice`() {
        for (lesson in all) assertEquals(lesson.id, 1, lesson.quiz.choices.count { it.correct })
    }

    @Test
    fun `day lesson uses the 24 h clock with help`() {
        val vrk = LessonCatalog.lesson(Lessons.DAY_LESSON)
        assertTrue(vrk.pages.all { it.period == ClockTime.DAY && it.help })
        assertEquals(ClockTime.DAY, vrk.quiz.period)
        assertEquals(1140, vrk.quiz.time)
    }

    @Test
    fun `demo moves stay on the clock`() {
        for (lesson in all) {
            for (page in lesson.pages) {
                assertTrue(page.time in 0 until page.period)
                page.demo.filterIsInstance<DemoStep.Move>().forEach {
                    assertTrue(
                        "${lesson.id} ${it.to}",
                        it.to in 0 until page.period,
                    )
                }
            }
        }
    }

    @Test
    fun `minute lesson lights all sixty ticks`() {
        val page = LessonCatalog.lesson("kierros").pages[2]
        val last = page.demo.filterIsInstance<DemoStep.Under>().last()
        assertEquals(60, last.marks.size)
    }

    @Test
    fun `half hour reference follows the language while numeric demos stay identical`() {
        for (language in AppLanguage.entries) {
            val lesson = LessonCatalog.lesson("puoli", language)
            val reference =
                lesson.pages[2]
                    .demo
                    .filterIsInstance<DemoStep.Under>()
                    .single()
            assertEquals(
                listOf(
                    when (language) {
                        AppLanguage.FINNISH, AppLanguage.SWEDISH, AppLanguage.GERMAN -> 4
                        else -> 3
                    },
                ),
                reference.marks.filterIsInstance<fi.clocklings.domain.Mark.NumberGlow>().map { it.n },
            )
            assertEquals(listOf(180, 210, 210, 210), lesson.pages.map { it.time })
            assertEquals(330, lesson.quiz.time)
        }
    }
}
