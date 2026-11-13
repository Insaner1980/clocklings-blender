package fi.clocklings.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** One connection, owned by the application lifecycle. No verification or reward logic lives here. */
class PlayBillingGateway(
    context: Context,
) : BillingGateway {
    private val appContext = context.applicationContext
    private val events = Channel<PurchaseUpdate>(Channel.UNLIMITED)
    override val updates = events.receiveAsFlow()
    private val connectionLock = Mutex()
    private var client: BillingClient? = null
    private var offer: Offer? = null

    private fun newClient(): BillingClient =
        BillingClient
            .newBuilder(appContext)
            .setListener { result, purchases ->
                events.trySend(PurchaseUpdate(result.map { purchases.orEmpty().map(Purchase::record) }))
            }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()

    private suspend fun connected(): BillingClient? =
        connectionLock.withLock {
            val current = client ?: newClient().also { client = it }
            if (current.isReady) return@withLock current
            val ready =
                withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        current.startConnection(
                            object : BillingClientStateListener {
                                override fun onBillingSetupFinished(result: BillingResult) {
                                    if (continuation.isActive) {
                                        continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                                    }
                                }

                                override fun onBillingServiceDisconnected() {
                                    // Subsequent operations use the library's automatic service reconnection.
                                    offer = null
                                }
                            },
                        )
                    }
                } ?: false
            current.takeIf { ready && client === current }
        }

    override suspend fun product(): StoreResult<StoreProduct> =
        withContext(Dispatchers.Main.immediate) {
            offer = null
            val current = connected() ?: return@withContext unavailable()
            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product
                                .newBuilder()
                                .setProductId(FullUnlockProduct.ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        ),
                    ).build()
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine<StoreResult<StoreProduct>> { continuation ->
                    current.queryProductDetailsAsync(params) { result, details ->
                        if (!continuation.isActive || client !== current) return@queryProductDetailsAsync
                        val product = details.productDetailsList.singleOrNull { it.productId == FullUnlockProduct.ID }
                        val selected =
                            product?.oneTimePurchaseOfferDetailsList?.singleOrNull {
                                it
                                    .isPermanentBasePurchase()
                            }
                        val token = selected?.offerToken
                        val response =
                            when {
                                result.responseCode != BillingClient.BillingResponseCode.OK -> {
                                    result.failure()
                                }

                                product == null || selected == null || token.isNullOrBlank() -> {
                                    unavailable()
                                }

                                else -> {
                                    offer = Offer(product, token)
                                    StoreResult.Success(StoreProduct(selected.formattedPrice))
                                }
                            }
                        continuation.resume(response)
                    }
                }
            } ?: unavailable()
        }

    override suspend fun purchases(): StoreResult<List<StorePurchase>> =
        withContext(Dispatchers.Main.immediate) {
            val current = connected() ?: return@withContext unavailable()
            val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine<StoreResult<List<StorePurchase>>> { continuation ->
                    current.queryPurchasesAsync(params) { result, purchases ->
                        if (continuation.isActive) continuation.resume(result.map { purchases.map(Purchase::record) })
                    }
                }
            } ?: unavailable()
        }

    override suspend fun acknowledge(token: String): StoreResult<Unit> =
        withContext(Dispatchers.Main.immediate) {
            val current = connected() ?: return@withContext unavailable()
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine<StoreResult<Unit>> { continuation ->
                    current.acknowledgePurchase(params) { result ->
                        if (continuation.isActive) continuation.resume(result.map { })
                    }
                }
            } ?: unavailable()
        }

    override fun launch(activity: Activity): StoreResult<Unit> {
        val current = client
        val selected = offer
        if (current == null || !current.isReady || selected == null) return unavailable()
        val product =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(selected.product)
                .setOfferToken(selected.token)
                .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build()
        return current.launchBillingFlow(activity, params).map { }
    }

    override fun close() {
        offer = null
        client?.endConnection()
        client = null
    }

    private class Offer(
        val product: ProductDetails,
        val token: String,
    )

    private companion object {
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}

private fun ProductDetails.OneTimePurchaseOfferDetails.isPermanentBasePurchase(): Boolean =
    rentalDetails == null && preorderDetails == null && offerId == null && limitedQuantityInfo == null

private fun Purchase.record(): StorePurchase =
    StorePurchase(
        products = products,
        token = purchaseToken,
        state =
            when (purchaseState) {
                Purchase.PurchaseState.PURCHASED -> StorePurchaseState.PURCHASED
                Purchase.PurchaseState.PENDING -> StorePurchaseState.PENDING
                else -> StorePurchaseState.UNSPECIFIED
            },
        originalJson = originalJson,
        signature = signature,
    )

private fun unavailable() = StoreResult.Failure(StoreFailure.UNAVAILABLE)

private inline fun <T> BillingResult.map(value: () -> T): StoreResult<T> =
    if (responseCode == BillingClient.BillingResponseCode.OK) StoreResult.Success(value()) else failure()

private fun BillingResult.failure(): StoreResult.Failure =
    StoreResult.Failure(
        when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> StoreFailure.CANCELLED

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> StoreFailure.ALREADY_OWNED

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            -> StoreFailure.UNAVAILABLE

            else -> StoreFailure.ERROR
        },
    )
