package fi.clocklings.audio

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.domain.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Each app language resolves every narration line to its own recording, never another language's. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NarrationResourcesTest {
    @Test
    fun everyLanguageHasItsOwnRecordingOfEveryLine() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (sound in Sound.entries.filter { it.narration }) {
            val recordings =
                AppLanguage.entries.associateWith { language ->
                    val configuration = Configuration(context.resources.configuration)
                    configuration.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
                    context
                        .createConfigurationContext(configuration)
                        .resources
                        .openRawResource(checkNotNull(sound.clip))
                        .use { it.readBytes() }
                }
            for ((language, bytes) in recordings) assertTrue("$sound $language", bytes.size > MIN_CLIP_BYTES)
            val distinct = recordings.values.map { it.contentHashCode() }.toSet()
            assertEquals("$sound", AppLanguage.entries.size, distinct.size)
        }
    }

    private companion object {
        const val MIN_CLIP_BYTES = 1_000
    }
}
