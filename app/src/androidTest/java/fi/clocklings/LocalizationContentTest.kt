package fi.clocklings

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Hint
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Text
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resolve
import fi.clocklings.ui.spokenTime
import fi.clocklings.ui.text
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import androidx.compose.material3.Text as UiText

/** Resolves the actual packaged resources, independently of the emulator's device language. */
@RunWith(AndroidJUnit4::class)
class LocalizationContentTest {
    @get:Rule val compose = createComposeRule()
    private val answers =
        mapOf(
            AppLanguage.ENGLISH to
                listOf(
                    "The short pink hand",
                    "One hour",
                    "Nine o'clock",
                    "Half past five",
                    "Quarter to seven",
                    "Ten past four",
                    "Twenty to nine",
                    "Seven minutes past three",
                    "Seven in the evening: 19:00",
                ),
            AppLanguage.FINNISH to
                listOf(
                    "Lyhyt pinkki viisari",
                    "Yksi tunti",
                    "Tasan yhdeksän",
                    "Puoli kuusi",
                    "Varttia vaille seitsemän",
                    "Kymmentä yli neljä",
                    "Kahtakymmentä vaille yhdeksän",
                    "Seitsemän minuuttia yli kolme",
                    "Seitsemän illalla: 19.00",
                ),
            AppLanguage.SWEDISH to
                listOf(
                    "Den korta rosa visaren",
                    "En timme",
                    "Nio",
                    "Halv sex",
                    "Kvart i sju",
                    "Tio över fyra",
                    "Tjugo i nio",
                    "Sju minuter över tre",
                    "Sju på kvällen: 19.00",
                ),
            AppLanguage.GERMAN to
                listOf(
                    "Der kurze rosa Zeiger",
                    "Eine Stunde",
                    "Neun Uhr",
                    "Halb sechs",
                    "Viertel vor sieben",
                    "Zehn nach vier",
                    "Zwanzig vor neun",
                    "Drei Uhr sieben",
                    "Sieben Uhr abends: 19:00",
                ),
            AppLanguage.FRENCH to
                listOf(
                    "La petite aiguille rose",
                    "Une heure",
                    "Neuf heures",
                    "Cinq heures et demie",
                    "Sept heures moins le quart",
                    "Quatre heures dix",
                    "Neuf heures moins vingt",
                    "Trois heures sept",
                    "Sept heures du soir : 19:00",
                ),
            AppLanguage.SPANISH to
                listOf(
                    "La aguja corta y rosa",
                    "Una hora",
                    "Las nueve",
                    "Las cinco y media",
                    "Las siete menos cuarto",
                    "Las cuatro y diez",
                    "Las nueve menos veinte",
                    "Las tres y siete",
                    "Las siete de la tarde: 19:00",
                ),
            AppLanguage.PORTUGUESE_BRAZIL to
                listOf(
                    "O ponteiro pequeno e rosa",
                    "Uma hora",
                    "Nove horas",
                    "Cinco e meia",
                    "Quinze para as sete",
                    "Quatro e dez",
                    "Vinte para as nove",
                    "Três horas e sete minutos",
                    "Sete da noite: 19:00",
                ),
        )

    @Test
    fun allNineLessonQuizzesMatchIndependentNumericAndLanguageFixtures() {
        val ids = listOf("viisarit", "kierros", "tasan", "puoli", "vartti", "ylivaille", "viisi", "minuutit", "vrk")
        val targets = listOf(240, 180, 540, 330, 405, 250, 520, 187, 1140)
        for (language in AppLanguage.entries) {
            val resources = resources(language.tag)
            val lessons = LessonCatalog.all(language)
            assertEquals(ids, lessons.map { it.id })
            lessons.forEachIndexed { index, lesson ->
                assertEquals("${language.tag}/${lesson.id} time", targets[index], lesson.quiz.time)
                val correct = lesson.quiz.choices.filter { it.correct }
                assertEquals("${language.tag}/${lesson.id} correct count", 1, correct.size)
                assertEquals(answers.getValue(language)[index], correct.single().text.resolve(resources))
                val labels = lesson.quiz.choices.map { it.text.resolve(resources) }
                assertEquals("${language.tag}/${lesson.id} distinct choices", labels.size, labels.toSet().size)
                assertTrue(
                    lesson.quiz.question
                        .resolve(resources)
                        .isNotBlank(),
                )
                assertTrue(
                    lesson.quiz.hint
                        .resolve(resources)
                        .isNotBlank(),
                )
            }
        }
    }

    @Test
    fun resourceAdaptersKeepTimeDigitsNarrationAndContextInTheSameLanguage() {
        for (fixture in adapterFixtures) {
            val resources = resources(fixture.tag)
            assertEquals(fixture.past, Text.Time(205).resolve(resources))
            assertEquals(fixture.to, Text.Time(215).resolve(resources))
            assertEquals(fixture.digital12, digitalLabel(resources, 187, false))
            assertEquals(fixture.digital24, digitalLabel(resources, 930, true))
            assertEquals(fixture.midnight, spokenTime(resources, 0, true))
            assertEquals(fixture.noon, spokenTime(resources, 720, true))
            assertTrue(Hint.HandsRight(0).text(resources).contains(fixture.midnightLabel))
            assertTrue(Hint.HandsRight(720).text(resources).contains(fixture.noonLabel))
            assertEquals("Clocklings", Text.Res(R.string.app_name).resolve(resources))
            assertTrue(Text.Plural(R.plurals.minutes_count, 2).resolve(resources).contains("2"))
        }
    }

    @Test
    fun newLanguageNarrationUsesCompleteSentencesWithHourAndConstructionAgreement() {
        val fixtures =
            mapOf(
                "de" to
                    listOf(
                        Triple(60, false, "Es ist ein Uhr"),
                        Triple(
                            930,
                            true,
                            "Es ist halb vier nachmittags. Im 24-Stunden-Format: fünfzehn Uhr dreißig",
                        ),
                    ),
                "fr" to
                    listOf(
                        Triple(60, false, "Il est une heure"),
                        Triple(750, true, "Il est midi et demi. Au format 24 heures, douze heures trente"),
                    ),
                "es-ES" to
                    listOf(
                        Triple(775, false, "Es la una menos cinco"),
                        Triple(120, false, "Son las dos"),
                        Triple(
                            930,
                            true,
                            "Son las tres y media de la tarde. " +
                                "En formato de 24 horas: las quince horas y treinta minutos",
                        ),
                    ),
                "pt-BR" to
                    listOf(
                        Triple(61, false, "É uma hora e um minuto"),
                        Triple(122, false, "São duas horas e dois minutos"),
                        Triple(215, false, "Faltam vinte e cinco minutos para as quatro"),
                        Triple(750, true, "É meio-dia e meia. No formato de 24 horas: doze horas e trinta minutos"),
                    ),
            )
        for ((tag, examples) in fixtures) {
            for ((time, h24, expected) in examples) {
                assertEquals("$tag/$time/$h24", expected, spokenTime(resources(tag), time, h24))
            }
        }
    }

    @Test
    fun fiveMinuteLessonExplainsBothSidesOfHalfInEachLanguage() {
        val expected =
            mapOf(
                AppLanguage.ENGLISH to
                    "At number 5, say twenty-five past. At number 7, say twenty-five to the next hour.",
                AppLanguage.FINNISH to
                    (
                        "Numeron 5 kohdalla sanomme viittä vaille puoli. " +
                            "Numeron 7 kohdalla sanomme viittä yli puoli. Nimeä seuraava tunti."
                    ),
                AppLanguage.SWEDISH to "Vid 5 säger vi fem i halv. Vid 7 säger vi fem över halv. Nämn nästa timme.",
                AppLanguage.GERMAN to
                    (
                        "Bei der 5 sagen wir fünf vor halb, bei der 7 fünf nach halb. " +
                            "Nach halb nennen wir die nächste Stunde."
                    ),
                AppLanguage.FRENCH to
                    "Au 5, on lit trois heures vingt-cinq. Au 7, quatre heures moins vingt-cinq.",
                AppLanguage.SPANISH to
                    "En el número 5, decimos las tres y veinticinco. En el número 7, las cuatro menos veinticinco.",
                AppLanguage.PORTUGUESE_BRAZIL to
                    "No 5, diga três e vinte e cinco. No 7, diga vinte e cinco para as quatro.",
            )
        for (language in AppLanguage.entries) {
            val page = LessonCatalog.lesson("ylivaille", language).pages.last()
            assertTrue(page.text.map { it.resolve(resources(language.tag)) }.contains(expected.getValue(language)))
        }
    }

    @Test
    fun nearHalfHintCoversTwentyOneThroughThirtyNineWithoutClaimingExactHalf() {
        val expected =
            mapOf(
                "en" to "Near half past, it is near the halfway point between this hour and the next.",
                "fi" to "Puolen tunnin lähellä viisari on lähellä tämän tunnin ja seuraavan tunnin puoliväliä.",
                "sv" to "Nära halv är visaren nära mitten mellan den här timmen och nästa.",
                "de" to "Zur halben Stunde steht er etwa in der Mitte zwischen dieser und der nächsten Stunde.",
                "fr" to "Vers la demi-heure, elle se trouve à mi-chemin entre cette heure et la suivante.",
                "es-ES" to "Cerca de la media hora, está casi a mitad de camino entre esta hora y la siguiente.",
                "pt-BR" to "Perto da meia hora, ele fica perto do meio do caminho entre uma hora e a seguinte.",
            )
        for ((tag, tip) in expected) {
            for (minute in 20..40) {
                val hint = Hints.forRead(180 + minute, 240 + minute) as Hint.ReadHour
                assertEquals(minute in 21..39, hint.nearHalf)
                val resolved = hint.text(resources(tag))
                if (minute in 21..39) assertTrue(resolved.endsWith(tip)) else assertFalse(resolved.contains(tip))
            }
        }
    }

    @Test
    fun sharedSwedishRegionalResourcesResolveConsistently() {
        val swedish = resources("sv")
        for (tag in listOf("sv-SE", "sv-FI")) {
            val regional = resources(tag)
            assertEquals("Inställningar", regional.getString(R.string.settings_title))
            for (lesson in LessonCatalog.all(AppLanguage.SWEDISH)) {
                assertEquals(lesson.title.resolve(swedish), lesson.title.resolve(regional))
            }
            assertEquals("Fem i halv fyra", Text.Time(205).resolve(regional))
        }
        assertEquals("Settings", resources("ja-JP").getString(R.string.settings_title))
        assertEquals("Twenty-five past three", Text.Time(205).resolve(resources("ja-JP")))
    }

    @Test
    fun appResourceBoundaryResolvesSupportedSecondaryBeforeLoadingUiAndClockWords() {
        val fixtures =
            mapOf(
                "ja-JP,sv-FI" to "sv|Inställningar|Fem i halv fyra",
                "ja-JP,fi-FI" to "fi|Asetukset|Viittä vaille puoli neljä",
                "ja-JP,de-DE" to "de|Einstellungen|Fünf vor halb vier",
                "ja-JP,fr-FR" to "fr|Paramètres|Trois heures vingt-cinq",
                "ja-JP,es-ES" to "es-ES|Ajustes|Las tres y veinticinco",
                "ja-JP,pt-BR" to "pt-BR|Configurações|Três e vinte e cinco",
                "ja-JP" to "en|Settings|Twenty-five past three",
            )
        val contexts = fixtures.keys.associateWith(::localizedContext)
        val tags = mutableStateOf(fixtures.keys.first())
        val repository = inMemoryRepository()
        lateinit var controller: AppLocaleController
        compose.runOnIdle { controller = AppLocaleController(repository, DeviceDefaultPlatform) }
        compose.setContent {
            val upstream = contexts.getValue(tags.value)
            CompositionLocalProvider(
                LocalContext provides upstream,
                LocalConfiguration provides upstream.resources.configuration,
                LocalResources provides upstream.resources,
            ) {
                AppLocaleProvider(repository, controller = controller) {
                    val settings = stringResource(R.string.settings_title)
                    val words = Text.Time(205).resolve()
                    UiText("${LocalAppLanguage.current.tag}|$settings|$words")
                }
            }
        }
        for ((source, expected) in fixtures) {
            compose.runOnIdle { tags.value = source }
            compose.onNodeWithText(expected).assertExists()
        }
    }

    private fun inMemoryRepository(): ProgressRepository =
        ProgressRepository(
            object : DataStore<Preferences> {
                override val data = MutableStateFlow<Preferences>(emptyPreferences())

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    transform(data.value).also { data.value = it }
            },
        )

    private object DeviceDefaultPlatform : AppLocalePlatform {
        override val supported = false

        override fun read(): AppLanguage? = null

        override fun write(language: AppLanguage?) = error("This read-only fixture must not write a platform locale")
    }

    private fun resources(tags: String): Resources = localizedContext(tags).resources

    private fun localizedContext(tags: String): android.content.Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(*tags.split(',').map(Locale::forLanguageTag).toTypedArray()))
        return context.createConfigurationContext(configuration)
    }

    private val adapterFixtures =
        listOf(
            AdapterFixture(
                "en",
                "Twenty-five past three",
                "Twenty-five to four",
                "3:07",
                "15:30",
                "It is midnight. On a 24-hour clock, zero zero zero.",
                "It is noon. On a 24-hour clock, twelve zero zero.",
                "midnight",
                "noon",
            ),
            AdapterFixture(
                "fi",
                "Viittä vaille puoli neljä",
                "Viittä yli puoli neljä",
                "3.07",
                "15.30",
                "On keskiyö. Numeroina nolla nolla nolla.",
                "On keskipäivä. Numeroina kaksitoista nolla nolla.",
                "keskiyö",
                "keskipäivä",
            ),
            AdapterFixture(
                "sv",
                "Fem i halv fyra",
                "Fem över halv fyra",
                "3.07",
                "15.30",
                "Det är midnatt. Med siffror noll noll noll.",
                "Klockan är tolv på dagen. Med siffror tolv noll noll.",
                "midnatt",
                "middag",
            ),
            AdapterFixture(
                "de",
                "Fünf vor halb vier",
                "Fünf nach halb vier",
                "3:07",
                "15:30",
                "Es ist Mitternacht. Im 24-Stunden-Format: null Uhr.",
                "Es ist Mittag. Im 24-Stunden-Format: zwölf Uhr.",
                "Mitternacht",
                "Mittag",
            ),
            AdapterFixture(
                "fr",
                "Trois heures vingt-cinq",
                "Quatre heures moins vingt-cinq",
                "3:07",
                "15:30",
                "Il est minuit. Au format 24 heures, zéro heure.",
                "Il est midi. Au format 24 heures, douze heures.",
                "minuit",
                "midi",
            ),
            AdapterFixture(
                "es-ES",
                "Las tres y veinticinco",
                "Las cuatro menos veinticinco",
                "3:07",
                "15:30",
                "Es medianoche. En formato de 24 horas: las cero horas.",
                "Es mediodía. En formato de 24 horas: las doce horas.",
                "medianoche",
                "mediodía",
            ),
            AdapterFixture(
                "pt-BR",
                "Três e vinte e cinco",
                "Vinte e cinco para as quatro",
                "3:07",
                "15:30",
                "É meia-noite. No formato de 24 horas: zero hora.",
                "É meio-dia. No formato de 24 horas: doze horas.",
                "meia-noite",
                "meio-dia",
            ),
        )

    private data class AdapterFixture(
        val tag: String,
        val past: String,
        val to: String,
        val digital12: String,
        val digital24: String,
        val midnight: String,
        val noon: String,
        val midnightLabel: String,
        val noonLabel: String,
    )
}
