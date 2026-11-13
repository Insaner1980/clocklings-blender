package fi.clocklings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LocaleReadFailureTest {
    @Test
    fun ioReadFailureUsesDefaultsAndAllowsLegacyLocaleStartup() =
        runBlocking {
            val repository = ProgressRepository(ReadFailureStore(IOException("Synthetic read failure")))
            assertEquals(Progress(), repository.progress.first())
            assertEquals(Settings(), repository.settings.first())
            assertNull(repository.legacyLanguage.first())
            val platform =
                object : AppLocalePlatform {
                    override val supported = false

                    override fun read(): AppLanguage? = error("Legacy startup must not use the platform")

                    override fun write(language: AppLanguage?) = error("Legacy startup must not write the platform")
                }
            val controller = withContext(Dispatchers.Main) { AppLocaleController(repository, platform) }
            try {
                val selection = withTimeout(10_000) { controller.selection.first { !it.pending } }
                assertTrue(selection.ready)
                assertFalse(selection.failed)
                assertNull(selection.choice)
            } finally {
                withContext(Dispatchers.Main) { controller.viewModelScope.cancel() }
            }
        }

    @Test
    fun nonIoReadFailureIsNotHidden() {
        val failure = IllegalStateException("Synthetic non-I/O failure")
        val repository = ProgressRepository(ReadFailureStore(failure))
        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runBlocking { repository.legacyLanguage.first() }
            }
        assertSame(failure, thrown)
    }
}

private class ReadFailureStore(
    private val failure: Exception,
) : DataStore<Preferences> {
    override val data = flow<Preferences> { throw failure }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        error("Read fallback must not overwrite stored preferences")
}
