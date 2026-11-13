package fi.clocklings.billing

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyPairGenerator
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class LocalPurchaseVerifierTest {
    private val verifier = SignedPurchases.verifier()

    @Test fun completedExpectedPurchaseVerifies() {
        assertNotNull(verifier.verify(SignedPurchases.purchase()))
    }

    @Test fun correctlyEncodedButDifferentPublicKeyCannotVerify() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val wrongKey = LocalPurchaseVerifier(Base64.getEncoder().encodeToString(other.public.encoded), "fi.clocklings")
        assertNull(wrongKey.verify(SignedPurchases.purchase()))
    }

    @Test fun pendingCancelledOtherAppsProductsAndQuantitiesNeverVerify() {
        val invalid =
            listOf(
                SignedPurchases.purchase(wireState = 4),
                SignedPurchases.purchase(wireState = 1),
                SignedPurchases.purchase(wireState = 2),
                SignedPurchases.purchase(packageName = "another.application"),
                SignedPurchases.purchase(product = "another_product"),
                SignedPurchases.purchase(token = ""),
                SignedPurchases.purchase(quantity = 2),
            )
        invalid.forEach { assertNull(verifier.verify(it)) }
    }

    @Test fun missingSignedStateDoesNotUseTheBillingLibraryPurchasedDefault() {
        val json = JSONObject(SignedPurchases.purchase().originalJson).apply { remove("purchaseState") }.toString()
        assertNull(verifier.verify(SignedPurchases.signed(json)))
    }

    @Test fun tamperedReceiptsSignaturesAndCallbackFieldsNeverVerify() {
        val purchase = SignedPurchases.purchase()
        assertNull(
            verifier.verify(
                PurchaseReceipt(purchase.originalJson.replace("test-token", "changed"), purchase.signature),
            ),
        )
        assertNull(verifier.verify(PurchaseReceipt(purchase.originalJson, "invalid")))
        assertNull(
            verifier.verify(
                StorePurchase(
                    purchase.products,
                    "different-token",
                    purchase.state,
                    purchase.originalJson,
                    purchase.signature,
                ),
            ),
        )
        assertNull(
            verifier.verify(
                StorePurchase(
                    listOf("another_product"),
                    purchase.token,
                    purchase.state,
                    purchase.originalJson,
                    purchase.signature,
                ),
            ),
        )
    }

    @Test fun missingOrMalformedConfigurationCannotVerifyOrEnableBilling() {
        for (key in listOf("", "not a public key")) {
            val unconfigured = LocalPurchaseVerifier(key, "fi.clocklings")
            assertFalse(unconfigured.configured)
            assertNull(unconfigured.verify(SignedPurchases.purchase()))
        }
    }

    @Test fun missingAcknowledgementFieldIsNotTreatedAsAlreadyAcknowledged() {
        val json = JSONObject(SignedPurchases.purchase().originalJson).apply { remove("acknowledged") }.toString()
        assertFalse(requireNotNull(verifier.verify(SignedPurchases.signed(json))).acknowledged)
    }
}
