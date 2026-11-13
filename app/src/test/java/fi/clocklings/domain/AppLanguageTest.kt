package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun resolvesFirstSupportedLanguageRatherThanOnlyFirstLocale() {
        assertEquals(AppLanguage.SWEDISH, resolveLanguage(listOf("ja-JP", "sv-FI", "fi-FI")))
        assertEquals(AppLanguage.FINNISH, resolveLanguage(listOf("ja", "fi-FI", "en-US")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(listOf("en-GB", "fi")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(listOf("ja", "it")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(emptyList()))
    }

    @Test
    fun explicitTagsResolveSupportedPrimaryLanguagesAndRejectUnsupportedOnes() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromTag("FI-fi"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-SE"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-FI"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-Latn-SE"))
        assertNull(AppLanguage.fromTag(null))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag("device"))
        assertNull(AppLanguage.fromTag("finnish"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de-DE"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de-AT"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr-FR"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-ES"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-MX"))
        for (tag in listOf("pt", "pt-BR", "pt-PT", "PT-br")) {
            assertEquals(AppLanguage.PORTUGUESE_BRAZIL, AppLanguage.fromTag(tag))
        }
    }

    @Test
    fun newLanguagesResolveInOrderAndRoundTripSavedTags() {
        assertEquals(AppLanguage.GERMAN, resolveLanguage(listOf("de-DE", "sv-FI")))
        assertEquals(AppLanguage.FRENCH, resolveLanguage(listOf("ja", "fr-CA", "en")))
        assertEquals(AppLanguage.SPANISH, resolveLanguage(listOf("es", "fr")))
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, resolveLanguage(listOf("pt-PT", "en")))
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
    }
}
