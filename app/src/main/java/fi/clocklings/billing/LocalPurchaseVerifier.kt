package fi.clocklings.billing

import com.android.billingclient.api.Purchase
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Signed evidence of a completed purchase, not evidence that it has never been revoked. */
data class PurchaseReceipt(
    val json: String,
    val signature: String,
) {
    override fun toString(): String = "PurchaseReceipt(redacted)"
}

class VerifiedPurchase(
    val receipt: PurchaseReceipt,
    val token: String,
    val acknowledged: Boolean,
)

/** The key is the public licensing key for this application, never an APK signing key. */
class LocalPurchaseVerifier(
    publicKey: String,
    private val packageName: String,
) {
    private val key =
        try {
            KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.filterNot(Char::isWhitespace))),
            )
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    val configured: Boolean get() = key != null

    fun verify(purchase: StorePurchase): VerifiedPurchase? {
        val verified = verify(PurchaseReceipt(purchase.originalJson, purchase.signature))
        return verified?.takeIf {
            purchase.state == StorePurchaseState.PURCHASED &&
                purchase.products == listOf(FullUnlockProduct.ID) && purchase.token == it.token
        }
    }

    fun verify(receipt: PurchaseReceipt): VerifiedPurchase? =
        try {
            if (validSignature(receipt)) completedPurchase(receipt) else null
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JSONException) {
            null
        }

    @Suppress("kotlin:S4790")
    private fun validSignature(receipt: PurchaseReceipt): Boolean {
        val publicKey = key ?: return false
        // Play's purchase-data protocol uses RSA PKCS#1 v1.5 with SHA-1, not an app-selected hash.
        // Protocol reference: TrivialDrive Security.java in Google's billing sample:
        // https://dl.google.com/android/repository/play_billing_r05.zip
        // nosemgrep: kotlin.lang.security.use-of-sha1.use-of-sha1
        val verifier = Signature.getInstance("SHA1withRSA")
        verifier.initVerify(publicKey)
        verifier.update(receipt.json.toByteArray(Charsets.UTF_8))
        return verifier.verify(Base64.getDecoder().decode(receipt.signature))
    }

    private fun completedPurchase(receipt: PurchaseReceipt): VerifiedPurchase? {
        // Check the signed wire value explicitly: Billing's accessor has a permissive default.
        val signed = JSONObject(receipt.json)
        if (signed.getInt("purchaseState") != COMPLETED_WIRE_STATE) return null
        val purchase = Purchase(receipt.json, receipt.signature)
        val expected = purchase.packageName == packageName && purchase.products == listOf(FullUnlockProduct.ID)
        return if (expected && purchase.purchaseToken.isNotBlank() && purchase.quantity == 1) {
            VerifiedPurchase(receipt, purchase.purchaseToken, signed.optBoolean("acknowledged", false))
        } else {
            null
        }
    }

    private companion object {
        const val COMPLETED_WIRE_STATE = 0
    }
}
