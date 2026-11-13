package fi.clocklings

import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** Establish the language before the Activity starts and restore it after the Activity closes. */
internal class EnglishLocaleRule : ExternalResource() {
    private val environment = LocaleTestEnvironment()
    private var previousLegacy: String? = null
    private var previousPlatform = ""

    override fun before() {
        runBlocking {
            previousLegacy = environment.repository.legacyLanguage.first()
            environment.repository.setLegacyLanguage(if (Build.VERSION.SDK_INT < 33) "en" else null)
        }
        previousPlatform = environment.setPlatformLanguage("en")
    }

    override fun after() {
        runBlocking { environment.repository.setLegacyLanguage(previousLegacy) }
        environment.setPlatformLanguage(previousPlatform)
    }
}
