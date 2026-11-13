package fi.clocklings.billing

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** Foreground retries are bounded; process-death recovery starts from the signed durable receipt. */
class LocalPurchases(
    private val gateway: BillingGateway,
    private val verifier: LocalPurchaseVerifier,
    store: OwnershipStore,
    private val scope: CoroutineScope,
) : Purchases {
    private val ownership = PurchaseOwnership(store, verifier, gateway)
    private val lock = Mutex()
    private val mutableState = MutableStateFlow(PurchaseUiState(loading = true))
    override val state = mutableState.asStateFlow()
    private var foreground = false
    private var purchaseInProgress = false
    private var pendingUpdate: PurchaseUpdate? = null
    private var cycle: Job? = null
    private var price: String? = null
    private val initialized =
        scope.launch {
            val status =
                try {
                    ownership.load()
                    if (verifier.configured &&
                        ownership.fullAccess
                    ) {
                        PurchaseStatus.OWNED
                    } else {
                        PurchaseStatus.UNAVAILABLE
                    }
                } catch (_: IOException) {
                    PurchaseStatus.FAILED
                }
            publish(status)
        }

    init {
        scope.launch {
            initialized.join()
            gateway.updates.collect { update ->
                if (verifier.configured && (foreground || purchaseInProgress)) {
                    purchaseInProgress = false
                    if (foreground) {
                        handleUpdate(update.result)
                    } else {
                        pendingUpdate = update
                        gateway.close()
                    }
                }
            }
        }
    }

    override fun onForeground() {
        foreground = true
        purchaseInProgress = false
        val update = pendingUpdate
        pendingUpdate = null
        if (update == null) refresh() else handleUpdate(update.result)
    }

    override fun onBackground() {
        foreground = false
        cycle?.cancel()
        // The Play purchase screen pauses our activity before delivering its result.
        if (!purchaseInProgress) gateway.close()
    }

    override fun refresh() = requestRefresh(restoring = false)

    override fun restore() = requestRefresh(restoring = true)

    override fun buy(activity: Activity) {
        if (state.value.canPurchase) {
            cycle?.cancel()
            purchaseInProgress = true
            publish(PurchaseStatus.VERIFYING, busy = true)
            // Query on callback/resume, never trust the launch result as ownership.
            val result = gateway.launch(activity)
            if (result is StoreResult.Failure) {
                purchaseInProgress = false
                handleFailure(result.reason)
            }
        }
    }

    private fun requestRefresh(
        restoring: Boolean,
        expectedToken: String? = null,
        cancelled: Boolean = false,
    ) {
        cycle?.cancel()
        cycle =
            scope.launch {
                initialized.join()
                if (!verifier.configured) {
                    publish(PurchaseStatus.UNAVAILABLE)
                    return@launch
                }
                for (wait in RETRY_DELAYS) {
                    if (wait > 0) {
                        if (!foreground) return@launch
                        delay(wait)
                    }
                    val status = lock.withLock { refreshStore(restoring, expectedToken, cancelled) }
                    if (status !in RETRY_STATUSES) break
                }
            }
    }

    private suspend fun refreshStore(
        restoring: Boolean,
        expectedToken: String?,
        cancelled: Boolean,
    ): PurchaseStatus {
        publish(if (cancelled) PurchaseStatus.CANCELLED else PurchaseStatus.VERIFYING, busy = true)
        val status =
            try {
                ownership.load()
                val owned = gateway.purchases()
                val ownershipStatus =
                    when (owned) {
                        is StoreResult.Success -> ownership.reconcile(owned.value, restoring, expectedToken)
                        is StoreResult.Failure -> PurchaseStatus.FAILED
                    }
                price = (gateway.product() as? StoreResult.Success)?.value?.localizedPrice?.takeIf(String::isNotBlank)
                when {
                    ownershipStatus != PurchaseStatus.NOT_OWNED -> ownershipStatus
                    price == null -> PurchaseStatus.UNAVAILABLE
                    restoring -> PurchaseStatus.NOT_OWNED
                    else -> PurchaseStatus.READY
                }
            } catch (_: IOException) {
                PurchaseStatus.FAILED
            }
        // Verification failures and pending payments must remain actionable after cancellation.
        val visibleStatus =
            if (cancelled && status !in RETRY_STATUSES && status != PurchaseStatus.PENDING) {
                PurchaseStatus.CANCELLED
            } else {
                status
            }
        publish(visibleStatus)
        return status
    }

    private fun handleUpdate(result: StoreResult<List<StorePurchase>>) {
        cycle?.cancel()
        when (result) {
            is StoreResult.Failure -> {
                handleFailure(result.reason)
            }

            is StoreResult.Success -> {
                val relevant = result.value.filter { FullUnlockProduct.ID in it.products }
                val completed = relevant.filter { it.state == StorePurchaseState.PURCHASED }
                val verified = completed.mapNotNull(verifier::verify)
                if (verified.size != completed.size) {
                    publish(PurchaseStatus.FAILED)
                } else {
                    // A fresh query prevents an old, still-valid signed callback from restoring revoked access.
                    requestRefresh(restoring = false, expectedToken = verified.firstOrNull()?.token)
                }
            }
        }
    }

    private fun handleFailure(reason: StoreFailure) {
        when (reason) {
            StoreFailure.CANCELLED -> requestRefresh(restoring = false, cancelled = true)
            StoreFailure.ALREADY_OWNED -> restore()
            else -> requestRefresh(restoring = false)
        }
    }

    private fun publish(
        status: PurchaseStatus,
        busy: Boolean = false,
    ) {
        mutableState.value =
            PurchaseUiState(
                fullAccess = ownership.fullAccess,
                localizedPrice = price,
                busy = busy,
                status = status,
                canPurchase =
                    verifier.configured && price != null && !ownership.fullAccess && !busy &&
                        status in PURCHASE_STATUSES,
            )
    }

    private companion object {
        val RETRY_DELAYS = listOf(0L, 2_000L, 10_000L, 60_000L)
        val RETRY_STATUSES =
            setOf(PurchaseStatus.FAILED, PurchaseStatus.UNAVAILABLE, PurchaseStatus.ACKNOWLEDGEMENT_PENDING)
        val PURCHASE_STATUSES = setOf(PurchaseStatus.READY, PurchaseStatus.NOT_OWNED, PurchaseStatus.CANCELLED)
    }
}
