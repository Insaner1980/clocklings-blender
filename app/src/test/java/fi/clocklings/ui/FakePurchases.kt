package fi.clocklings.ui

import android.app.Activity
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.billing.Purchases
import kotlinx.coroutines.flow.MutableStateFlow

/** Test-only entitlement, never included in the application APK. */
class FakePurchases(
    fullAccess: Boolean,
) : Purchases {
    override val state = MutableStateFlow(PurchaseUiState(fullAccess = fullAccess, status = PurchaseStatus.READY))

    override fun buy(activity: Activity) = Unit

    override fun restore() = Unit

    override fun refresh() = Unit
}
