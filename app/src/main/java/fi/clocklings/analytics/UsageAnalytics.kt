package fi.clocklings.analytics

import androidx.annotation.MainThread
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Stable
interface UsageAnalytics {
    val available: Boolean
    val enabled: Flow<Boolean>

    @MainThread
    fun setEnabled(enabled: Boolean)

    @MainThread
    fun count(metric: String)

    data object Disabled : UsageAnalytics {
        override val available = false
        override val enabled = flowOf(false)

        override fun setEnabled(enabled: Boolean) = Unit

        override fun count(metric: String) = Unit
    }
}
