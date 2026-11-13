package fi.clocklings

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnglishContentTest {
    private fun englishResources() =
        InstrumentationRegistry.getInstrumentation().targetContext.let { context ->
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocales(LocaleList.forLanguageTags("en"))
            context.createConfigurationContext(configuration).resources
        }

    @Test
    fun lessonAnswersMatchTheirClocksInEnglish() {
        val res = englishResources()
        val examples =
            listOf(
                Triple("tasan", 540, "Nine o'clock"),
                Triple("puoli", 330, "Half past five"),
                Triple("vartti", 405, "Quarter to seven"),
                Triple("ylivaille", 250, "Ten past four"),
                Triple("viisi", 520, "Twenty to nine"),
                Triple("minuutit", 187, "Seven minutes past three"),
                Triple("vrk", 1140, "Seven in the evening: 19:00"),
            )
        for ((id, time, answer) in examples) {
            val quiz = LessonCatalog.lesson(id).quiz
            assertEquals(id, time, quiz.time)
            assertEquals(
                id,
                answer,
                quiz.choices
                    .single { it.correct }
                    .text
                    .resolve(res),
            )
            assertEquals(
                id,
                quiz.choices.size,
                quiz.choices
                    .map { it.text.resolve(res) }
                    .distinct()
                    .size,
            )
        }
    }

    @Test
    fun halfHourExplanationNamesTheHourAlreadyPassed() {
        val res = englishResources()
        val half = LessonCatalog.lesson("puoli")
        assertEquals(210, half.pages[2].time)
        assertTrue(res.getString(R.string.l_puoli_p3a).contains("half past three"))
        assertEquals("Half past three is 3:30.", res.getString(R.string.l_puoli_say_digits))
    }
}
