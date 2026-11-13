package fi.clocklings.billing

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import fi.clocklings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.purchaseStore by preferencesDataStore(name = "clocklings_purchase")

fun createPlayPurchases(context: Context): Purchases =
    LocalPurchases(
        PlayBillingGateway(context),
        LocalPurchaseVerifier(context.getString(R.string.play_billing_public_key), context.packageName),
        PreferencesOwnershipStore(context.purchaseStore),
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )
