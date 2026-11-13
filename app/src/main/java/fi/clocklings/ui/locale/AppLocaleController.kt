package fi.clocklings.ui.locale

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/** The platform is the only persistent authority on API 33 and newer. */
interface AppLocalePlatform {
    val supported: Boolean

    fun read(): AppLanguage?

    fun write(language: AppLanguage?)
}

data class LanguageSelection(
    val choice: AppLanguage? = null,
    val ready: Boolean = false,
    val pending: Boolean = false,
    val failed: Boolean = false,
)

@Stable
class AppLocaleController(
    private val repository: ProgressRepository,
    private val platform: AppLocalePlatform,
) : ViewModel() {
    private val mutableSelection = MutableStateFlow(LanguageSelection())
    val selection = mutableSelection.asStateFlow()
    private var handoffAttempted = false

    init {
        refresh()
    }

    fun refresh() {
        if (mutableSelection.value.pending) return
        update {
            if (platform.supported) {
                val platformChoice = platform.read()
                if (platformChoice != null || handoffAttempted) {
                    // Android's choice remains usable even when legacy cleanup cannot read the store.
                    handoffAttempted = true
                    mutableSelection.value = mutableSelection.value.copy(choice = platformChoice, ready = true)
                }
                val legacy = repository.legacyLanguageForMigration.first()
                if (!handoffAttempted) {
                    AppLanguage.fromTag(legacy)?.let(platform::write)
                }
                // A failed cleanup must not re-import over a subsequent system selection.
                handoffAttempted = true
                mutableSelection.value = mutableSelection.value.copy(choice = platform.read(), ready = true)
                if (legacy != null) repository.setLegacyLanguage(null)
                platform.read()
            } else {
                AppLanguage.fromTag(repository.legacyLanguage.first())
            }
        }
    }

    fun select(language: AppLanguage?) {
        val current = mutableSelection.value
        if (!current.ready || current.pending || current.choice == language) return
        update {
            if (platform.supported) {
                platform.write(language)
                platform.read()
            } else {
                repository.setLegacyLanguage(language?.tag)
                language
            }
        }
    }

    private fun update(action: suspend () -> AppLanguage?) {
        mutableSelection.value = mutableSelection.value.copy(pending = true, failed = false)
        viewModelScope.launch {
            try {
                mutableSelection.value = LanguageSelection(choice = action(), ready = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                mutableSelection.value = mutableSelection.value.copy(pending = false, failed = true)
            } catch (_: RuntimeException) {
                mutableSelection.value = mutableSelection.value.copy(pending = false, failed = true)
            }
        }
    }
}
