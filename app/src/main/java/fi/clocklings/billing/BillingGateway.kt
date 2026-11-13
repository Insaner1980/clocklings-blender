package fi.clocklings.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** Only store transport. A successful callback is not purchase verification or entitlement. */
interface BillingGateway {
    val updates: Flow<PurchaseUpdate>

    suspend fun product(): StoreResult<StoreProduct>

    suspend fun purchases(): StoreResult<List<StorePurchase>>

    suspend fun acknowledge(token: String): StoreResult<Unit>

    fun launch(activity: Activity): StoreResult<Unit>

    fun close()
}

data class StoreProduct(
    val localizedPrice: String,
)

/** Keep receipt/token values out of automatically generated toString and application logs. */
class StorePurchase(
    val products: List<String>,
    val token: String,
    val state: StorePurchaseState,
    val originalJson: String,
    val signature: String,
)

enum class StorePurchaseState { PURCHASED, PENDING, UNSPECIFIED }

enum class StoreFailure { CANCELLED, ALREADY_OWNED, UNAVAILABLE, ERROR }

sealed interface StoreResult<out T> {
    data class Success<T>(
        val value: T,
    ) : StoreResult<T>

    data class Failure(
        val reason: StoreFailure,
    ) : StoreResult<Nothing>
}

class PurchaseUpdate(
    val result: StoreResult<List<StorePurchase>>,
)
