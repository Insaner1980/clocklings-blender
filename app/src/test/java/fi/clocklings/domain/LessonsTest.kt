package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonsTest {
    @org.junit.Test
    fun `lesson chapters map every lesson to its practice level`() {
        val expected =
            mapOf(
                "viisarit" to 0,
                "kierros" to 0,
                "tasan" to 0,
                "puoli" to 1,
                "vartti" to 2,
                "ylivaille" to 3,
                "viisi" to 3,
                "minuutit" to 4,
                "vrk" to null,
            )
        expected.forEach { (id, level) -> org.junit.Assert.assertEquals(id, level, Lessons.levelOf(id)) }
        org.junit.Assert.assertNull(Lessons.levelOf("unknown"))
    }

    @Test
    fun `quarters and five minute practice follow the approved lesson order`() {
        assertEquals(
            // CPD-OFF: Keep expected lesson mapping independent of production data.
            listOf(
                listOf("viisarit", "kierros", "tasan"),
                listOf("puoli"),
                listOf("vartti"),
                listOf("viisi", "ylivaille"),
                listOf("minuutit"),
            ),
            // CPD-ON
            Lessons.levelLessons,
        )
    }

    @Test
    fun `first play of a level shows its unlearned lessons`() {
        val plan = Lessons.introPlan(level = 0, h24 = false, progress = Progress())
        assertEquals(listOf("viisarit", "kierros", "tasan"), plan.queue)
        assertEquals(setOf("0"), plan.introShown)
    }

    @Test
    fun `learned lessons are skipped`() {
        val progress = Progress(lessonsDone = setOf("kierros"))
        assertEquals(listOf("viisarit", "tasan"), Lessons.introPlan(0, false, progress).queue)
    }

    @Test
    fun `existing lesson ids and intro flags still control the reordered lessons`() {
        val learned = Progress(lessonsDone = setOf("vartti", "ylivaille"), introShown = setOf("2", "h24"))
        assertEquals(IntroPlan(emptyList(), learned.introShown), Lessons.introPlan(2, true, learned))
        assertEquals(IntroPlan(listOf("viisi"), setOf("2", "3", "h24")), Lessons.introPlan(3, true, learned))

        val unfinished = Progress(lessonsDone = setOf("viisi"), introShown = setOf("2"))
        assertEquals(listOf("ylivaille"), Lessons.introPlan(3, false, unfinished).queue)
        val alreadyIntroduced = unfinished.copy(introShown = setOf("2", "3"))
        assertEquals(
            IntroPlan(emptyList(), alreadyIntroduced.introShown),
            Lessons.introPlan(3, false, alreadyIntroduced),
        )
    }

    @Test
    fun `lessons are shown automatically only once per level`() {
        val progress = Progress(introShown = setOf("0"))
        assertTrue(Lessons.introPlan(0, false, progress).queue.isEmpty())
    }

    @Test
    fun `24 h mode adds the day lesson once`() {
        val first = Lessons.introPlan(1, h24 = true, progress = Progress())
        assertEquals(listOf("puoli", "vrk"), first.queue)
        assertEquals(setOf("1", "h24"), first.introShown)
        val again = Lessons.introPlan(2, h24 = true, progress = Progress(introShown = first.introShown))
        assertEquals(listOf("vartti"), again.queue)
    }

    @Test
    fun `24 h lesson is skipped when already learned`() {
        val plan =
            Lessons.introPlan(
                0,
                h24 = true,
                progress = Progress(lessonsDone = setOf("vrk"), introShown = setOf("0")),
            )
        assertTrue(plan.queue.isEmpty())
        assertEquals(setOf("0", "h24"), plan.introShown)
    }

    @Test
    fun `demo builder records steps in order`() {
        val steps =
            demo {
                pause(700)
                highlight(Hand.HOUR)
                move(240, 6000, exact = true)
                under(Mark.NumberGlow(3), Mark.TickGlow(15))
                over(listOf(Mark.Bubble(5, "5")))
                say(Text.Plain("Tasan kolme"))
                say(null)
            }
        assertEquals(
            listOf(
                DemoStep.Wait(700),
                DemoStep.Highlight(Hand.HOUR),
                DemoStep.Move(240, 6000, exact = true),
                DemoStep.Under(listOf(Mark.NumberGlow(3), Mark.TickGlow(15))),
                DemoStep.Over(listOf(Mark.Bubble(5, "5"))),
                DemoStep.Say(Text.Plain("Tasan kolme")),
                DemoStep.Say(null),
            ),
            steps,
        )
    }
}
