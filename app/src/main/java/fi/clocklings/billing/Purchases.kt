package fi.clocklings.billing

import android.app.Activity
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
interface Purchases {
    val state: StateFlow<PurchaseUiState>

    fun buy(activity: Activity)

    fun restore()

    fun refresh()

    fun onForeground() = Unit

    fun onBackground() = Unit
}

/** Safe fixture/default for containers that do not provide a configured purchase service. */
class UnconfiguredPurchases : Purchases {
    private val unavailable = MutableStateFlow(PurchaseUiState())
    override val state = unavailable.asStateFlow()

    override fun buy(activity: Activity) = Unit

    override fun restore() = Unit

    override fun refresh() = Unit
}
