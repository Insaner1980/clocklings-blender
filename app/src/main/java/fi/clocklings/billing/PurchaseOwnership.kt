package fi.clocklings.billing

/** Serial caller only. A cache grants offline access; successful Play queries reconcile current ownership. */
internal class PurchaseOwnership(
    private val store: OwnershipStore,
    private val verifier: LocalPurchaseVerifier,
    private val gateway: BillingGateway,
) {
    var fullAccess: Boolean = false
        private set
    private var loaded = false
    private var cached: CachedOwnership? = null
    private var token: String? = null

    suspend fun load() {
        if (!loaded) {
            cached = store.read()
            val verified = cached?.let { verifier.verify(it.receipt) }
            token = verified?.token
            fullAccess = verified != null
            loaded = true
        }
    }

    suspend fun reconcile(
        purchases: List<StorePurchase>,
        restoring: Boolean,
        expectedToken: String?,
    ): PurchaseStatus {
        val candidates = purchases.filter { FullUnlockProduct.ID in it.products }
        val completed = candidates.filter { it.state == StorePurchaseState.PURCHASED }
        val verified = completed.mapNotNull(verifier::verify).distinctBy { it.token }
        val invalid =
            completed.any { verifier.verify(it) == null } ||
                candidates.any { it.state == StorePurchaseState.UNSPECIFIED }
        return when {
            invalid || verified.size > 1 -> {
                PurchaseStatus.FAILED
            }

            expectedToken != null && !fullAccess && verified.none { it.token == expectedToken } -> {
                PurchaseStatus.FAILED
            }

            verified.isNotEmpty() -> {
                grant(verified.single(), restoring)
            }

            else -> {
                // Persist confirmed loss; no educational data belongs to this store.
                fullAccess = false
                token = null
                cached = null
                store.write(null)
                if (candidates.any { it.state == StorePurchaseState.PENDING }) {
                    PurchaseStatus.PENDING
                } else {
                    PurchaseStatus.NOT_OWNED
                }
            }
        }
    }

    private suspend fun grant(
        purchase: VerifiedPurchase,
        restoring: Boolean,
    ): PurchaseStatus {
        val acknowledged = purchase.acknowledged || (token == purchase.token && cached?.acknowledged == true)
        val next = CachedOwnership(purchase.receipt, acknowledged)
        if (cached != next) store.write(next)
        cached = next
        token = purchase.token
        fullAccess = true
        return if (!acknowledged && !acknowledge(purchase)) {
            PurchaseStatus.ACKNOWLEDGEMENT_PENDING
        } else if (restoring) {
            PurchaseStatus.RESTORED
        } else {
            PurchaseStatus.OWNED
        }
    }

    private suspend fun acknowledge(purchase: VerifiedPurchase): Boolean =
        when (gateway.acknowledge(purchase.token)) {
            is StoreResult.Failure -> {
                false
            }

            is StoreResult.Success -> {
                val confirmed = CachedOwnership(purchase.receipt, acknowledged = true)
                store.write(confirmed)
                cached = confirmed
                true
            }
        }
}
