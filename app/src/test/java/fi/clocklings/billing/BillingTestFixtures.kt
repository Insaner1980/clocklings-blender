package fi.clocklings.billing

import android.app.Activity
import fi.clocklings.data.InMemoryPreferencesStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/** Generated only in the test process. No private key or entitlement override is packaged in the app. */
internal object SignedPurchases {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val publicKey: String = Base64.getEncoder().encodeToString(keys.public.encoded)

    fun verifier() = LocalPurchaseVerifier(publicKey, "fi.clocklings")

    fun purchase(
        token: String = "test-token",
        wireState: Int = 0,
        packageName: String = "fi.clocklings",
        product: String = FullUnlockProduct.ID,
        acknowledged: Boolean = false,
        quantity: Int = 1,
    ): StorePurchase {
        val json =
            JSONObject()
                .put("packageName", packageName)
                .put("productId", product)
                .put("purchaseToken", token)
                .put("purchaseState", wireState)
                .put("acknowledged", acknowledged)
                .put("quantity", quantity)
                .toString()
        return signed(json, if (wireState == 4) StorePurchaseState.PENDING else StorePurchaseState.PURCHASED)
    }

    fun signed(
        json: String,
        state: StorePurchaseState = StorePurchaseState.PURCHASED,
    ): StorePurchase {
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initSign(keys.private)
        signer.update(json.toByteArray(Charsets.UTF_8))
        val signature = Base64.getEncoder().encodeToString(signer.sign())
        val parsed = JSONObject(json)
        return StorePurchase(
            listOf(parsed.optString("productId")),
            parsed.optString("purchaseToken"),
            state,
            json,
            signature,
        )
    }
}

internal class FakeBillingGateway : BillingGateway {
    override val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 16)
    var productResult: StoreResult<StoreProduct> = StoreResult.Success(StoreProduct("4,50 €"))
    var owned: StoreResult<List<StorePurchase>> = StoreResult.Success(emptyList())
    var acknowledgement: StoreResult<Unit> = StoreResult.Success(Unit)
    var launchResult: StoreResult<Unit> = StoreResult.Success(Unit)
    val acknowledged = mutableListOf<String>()
    var queries = 0
    var queryGate: CompletableDeferred<Unit>? = null
    var launches = 0
    var closed = 0

    override suspend fun product() = productResult

    override suspend fun purchases(): StoreResult<List<StorePurchase>> {
        queries++
        queryGate?.await()
        return owned
    }

    override suspend fun acknowledge(token: String): StoreResult<Unit> {
        acknowledged += token
        return acknowledgement
    }

    override fun launch(activity: Activity): StoreResult<Unit> {
        launches++
        return launchResult
    }

    override fun close() {
        closed++
    }

    fun update(result: StoreResult<List<StorePurchase>>) {
        check(updates.tryEmit(PurchaseUpdate(result)))
    }
}

internal class TestOwnershipStore : OwnershipStore {
    private val delegate = PreferencesOwnershipStore(InMemoryPreferencesStore())
    var failRead = false
    var failWrite = false

    override suspend fun read(): CachedOwnership? {
        if (failRead) throw IOException("Test read failure")
        return delegate.read()
    }

    override suspend fun write(ownership: CachedOwnership?) {
        if (failWrite) throw IOException("Test write failure")
        delegate.write(ownership)
    }
}
