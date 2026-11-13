package fi.clocklings.billing

/** Store-facing state, deliberately separate from learning progress and rewards. */
data class PurchaseUiState(
    val loading: Boolean = false,
    val fullAccess: Boolean = false,
    val localizedPrice: String? = null,
    val busy: Boolean = false,
    val status: PurchaseStatus = PurchaseStatus.UNAVAILABLE,
    val canPurchase: Boolean = false,
)

enum class PurchaseStatus {
    READY,
    UNAVAILABLE,
    PENDING,
    CANCELLED,
    VERIFYING,
    OWNED,
    RESTORED,
    NOT_OWNED,
    FAILED,
    ACKNOWLEDGEMENT_PENDING,
}

object FullUnlockProduct {
    const val ID = "clocklings_full_unlock"
}
