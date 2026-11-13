package fi.clocklings.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Debug builds always own the full unlock so every paid feature can be reviewed without Play Billing. */
@Suppress("UNUSED_PARAMETER")
fun createAppPurchases(context: Context): Purchases = DebugFullAccessPurchases()

private class DebugFullAccessPurchases : Purchases {
    override val state =
        MutableStateFlow(PurchaseUiState(fullAccess = true, status = PurchaseStatus.OWNED)).asStateFlow()

    override fun buy(activity: Activity) = Unit

    override fun restore() = Unit

    override fun refresh() = Unit
}
