package fi.clocklings.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Wardrobe
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ItemNamesTest {
    @Test
    fun `every wardrobe item has a distinct name resource`() {
        val ids = Wardrobe.items.map { itemName(it.id) }
        assertEquals(Wardrobe.items.size, ids.toSet().size)
    }

    @Test
    fun `names are unique within each companion in every language`() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        // A known name per language proves the locale really switched instead of falling back to English.
        val known =
            mapOf(
                "en" to "Head flower",
                "fi" to "Kukka päässä",
                "sv" to "Blomma på huvudet",
                "de" to "Blume auf dem Kopf",
                "fr" to "Fleur sur la tête",
                "es-ES" to "Flor para la cabeza",
                "pt-BR" to "Flor na cabeça",
            )
        assertEquals(AppLanguage.entries.map { it.tag }.toSet(), known.keys)
        for ((language, flower) in known) {
            val localized =
                app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) },
                )
            assertEquals(language, flower, localized.getString(itemName("dinosaur-head-flower")))
            for ((companion, items) in Wardrobe.items.groupBy { it.companion }) {
                val names = items.map { localized.getString(itemName(it.id)) }
                assertEquals(
                    "Duplicate names for $companion in $language: $names",
                    names.size,
                    names.toSet().size,
                )
            }
        }
    }
}
