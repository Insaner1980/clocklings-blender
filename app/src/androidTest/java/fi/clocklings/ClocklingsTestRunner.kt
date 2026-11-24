package fi.clocklings

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.billing.Purchases
import kotlinx.coroutines.flow.MutableStateFlow

/** Entitlements are controlled only by the test APK, never by a production intent or preference. */
class ClocklingsTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        loader: ClassLoader,
        name: String,
        context: Context,
    ): Application = super.newApplication(loader, ClocklingsTestApplication::class.java.name, context)
}

class ClocklingsTestApplication : ClocklingsApplication() {
    // Existing device tests exercise the full application. Free-access tests explicitly replace this state.
    val testPurchases = DeviceTestPurchases(fullAccess = true)
    internal val galleryStore = GalleryPreferences()

    override fun purchases(): Purchases = testPurchases

    override fun preferencesStore(): DataStore<Preferences> =
        if (InstrumentationRegistry.getArguments().getString("cell") == "gallery") {
            galleryStore
        } else {
            super.preferencesStore()
        }
}

class DeviceTestPurchases(
    fullAccess: Boolean,
) : Purchases {
    override val state = MutableStateFlow(PurchaseUiState(fullAccess = fullAccess))

    override fun buy(activity: Activity) {
        if (state.value.canPurchase) {
            state.value = PurchaseUiState(status = PurchaseStatus.PENDING)
        }
    }

    override fun restore() = Unit

    override fun refresh() = Unit
}
