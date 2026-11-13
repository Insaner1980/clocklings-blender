package fi.clocklings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.billing.CachedOwnership
import fi.clocklings.billing.FullUnlockProduct
import fi.clocklings.billing.LocalPurchaseVerifier
import fi.clocklings.billing.PreferencesOwnershipStore
import fi.clocklings.billing.PurchaseReceipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/** Real Android crypto and disk storage, with an ephemeral test key; never a Play transaction. */
class ReceiptStorageDeviceTest {
    @Test fun signedReceiptAndAcknowledgementSurviveStoreRecreationAndClear() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val file = File(context.cacheDir, "billing-device-test.preferences_pb")
            val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val json =
                JSONObject()
                    .put("packageName", context.packageName)
                    .put("productId", FullUnlockProduct.ID)
                    .put("purchaseState", 0)
                    .put("purchaseToken", "device-test-token")
                    .toString()
            val signature =
                Signature.getInstance("SHA1withRSA").run {
                    initSign(keys.private)
                    update(json.toByteArray(Charsets.UTF_8))
                    Base64.getEncoder().encodeToString(sign())
                }
            val verifier =
                LocalPurchaseVerifier(Base64.getEncoder().encodeToString(keys.public.encoded), context.packageName)
            val cached = CachedOwnership(PurchaseReceipt(json, signature), acknowledged = false)
            try {
                withStore(file) { it.write(cached) }
                withStore(file) {
                    val restored = requireNotNull(it.read())
                    assertEquals(cached, restored)
                    assertNotNull(verifier.verify(restored.receipt))
                    it.write(restored.copy(acknowledged = true))
                }
                withStore(file) {
                    assertTrue(requireNotNull(it.read()).acknowledged)
                    it.write(null)
                }
                withStore(file) { assertNull(it.read()) }
            } finally {
                assertTrue(!file.exists() || file.delete())
            }
        }

    private suspend fun withStore(
        file: File,
        action: suspend (PreferencesOwnershipStore) -> Unit,
    ) {
        val job = Job()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
            action(PreferencesOwnershipStore(store))
        } finally {
            job.cancelAndJoin()
        }
    }
}
