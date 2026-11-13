package fi.clocklings.billing

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.data.InMemoryPreferencesStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseRecoveryBoundariesTest {
    @Test fun purchaseDiagnosticsDoNotExposeReceiptOrTokenValues() {
        val receipt = PurchaseReceipt("synthetic-receipt-sentinel", "synthetic-signature-sentinel")
        val token = "synthetic-token-sentinel"
        val purchase =
            StorePurchase(
                listOf(FullUnlockProduct.ID),
                token,
                StorePurchaseState.PURCHASED,
                receipt.json,
                receipt.signature,
            )
        val result = StoreResult.Success(listOf(purchase))
        val diagnostics =
            listOf(
                receipt.toString(),
                CachedOwnership(receipt, false).toString(),
                VerifiedPurchase(receipt, token, false).toString(),
                purchase.toString(),
                result.toString(),
                PurchaseUpdate(result).toString(),
            )
        for (diagnostic in diagnostics) {
            for (sentinel in listOf(receipt.json, receipt.signature, token)) {
                assertFalse(diagnostic.contains(sentinel))
            }
        }
        assertTrue(receipt.toString().contains("redacted"))
    }

    @Test fun distinctVerifiedTokensFailWithoutReplacingThePreviousReceipt() =
        runTest {
            val store = TestOwnershipStore()
            val gateway = FakeBillingGateway()
            val first = SignedPurchases.purchase(acknowledged = true)
            val cached = CachedOwnership(PurchaseReceipt(first.originalJson, first.signature), true)
            store.write(cached)
            val ownership = PurchaseOwnership(store, SignedPurchases.verifier(), gateway)
            ownership.load()
            assertEquals(
                PurchaseStatus.FAILED,
                ownership.reconcile(listOf(first, SignedPurchases.purchase(token = "other-test-token")), false, null),
            )
            assertTrue(ownership.fullAccess)
            assertEquals(cached, store.read())
            assertTrue(gateway.acknowledged.isEmpty())
            assertEquals(PurchaseStatus.OWNED, ownership.reconcile(listOf(first, first), false, null))
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun delayedInitialReadFinishesBeforeAnyOwnershipQueryOrGrant() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val cached = TestOwnershipStore()
            val receipt = SignedPurchases.purchase(acknowledged = true)
            cached.write(CachedOwnership(PurchaseReceipt(receipt.originalJson, receipt.signature), true))
            val store =
                object : OwnershipStore by cached {
                    override suspend fun read(): CachedOwnership? {
                        release.await()
                        return cached.read()
                    }
                }
            val gateway = FakeBillingGateway().apply { owned = StoreResult.Success(listOf(receipt)) }
            val service = LocalPurchases(gateway, SignedPurchases.verifier(), store, backgroundScope)
            service.onForeground()
            service.restore()
            runCurrent()
            assertTrue(service.state.value.loading)
            assertFalse(service.state.value.fullAccess)
            assertEquals(0, gateway.queries)
            release.complete(Unit)
            runCurrent()
            assertFalse(service.state.value.loading)
            assertTrue(service.state.value.fullAccess)
            assertEquals(1, gateway.queries)
            service.onBackground()
        }

    @Test fun suspendedAndFailedFirstWriteCannotGrantOrAcknowledge() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val cached = TestOwnershipStore()
            var blockWrite = true
            val store =
                object : OwnershipStore by cached {
                    override suspend fun write(ownership: CachedOwnership?) {
                        if (blockWrite) release.await()
                        cached.write(ownership)
                    }
                }
            val (service, gateway) = startOwnedPurchase(store)
            assertFalse(service.state.value.fullAccess)
            assertNull(cached.read())
            assertTrue(gateway.acknowledged.isEmpty())
            release.completeExceptionally(IOException("Test first write failure"))
            runCurrent()
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertFalse(service.state.value.fullAccess)
            assertTrue(gateway.acknowledged.isEmpty())
            blockWrite = false
            service.refresh()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertNotNull(cached.read())
            assertEquals(1, gateway.acknowledged.size)
            service.onBackground()
        }

    @Test fun acknowledgementWriteFailureRetainsTheDurableReceiptAcrossRestart() =
        runTest {
            val cached = TestOwnershipStore()
            var writes = 0
            val store =
                object : OwnershipStore by cached {
                    override suspend fun write(ownership: CachedOwnership?) {
                        writes++
                        if (writes == 2) throw IOException("Test acknowledgement write failure")
                        cached.write(ownership)
                    }
                }
            val (service, gateway) = startOwnedPurchase(store)
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            val durable = requireNotNull(cached.read())
            assertFalse(durable.acknowledged)
            assertNotNull(SignedPurchases.verifier().verify(durable.receipt))
            service.onBackground()
            val restarted = LocalPurchases(gateway, SignedPurchases.verifier(), store, backgroundScope)
            runCurrent()
            assertTrue(restarted.state.value.fullAccess)
            restarted.onForeground()
            runCurrent()
            assertTrue(requireNotNull(cached.read()).acknowledged)
            assertEquals(PurchaseStatus.OWNED, restarted.state.value.status)
            assertEquals(2, gateway.acknowledged.size)
            restarted.onBackground()
        }

    private fun TestScope.startOwnedPurchase(store: OwnershipStore): Pair<LocalPurchases, FakeBillingGateway> {
        val gateway = FakeBillingGateway().apply { owned = StoreResult.Success(listOf(SignedPurchases.purchase())) }
        val service = LocalPurchases(gateway, SignedPurchases.verifier(), store, backgroundScope)
        service.onForeground()
        runCurrent()
        return service to gateway
    }

    @Test fun retryableStatesUseOnlyFourAttemptsAndBackgroundCancelsTheWait() =
        runTest {
            val statuses =
                listOf(PurchaseStatus.FAILED, PurchaseStatus.UNAVAILABLE, PurchaseStatus.ACKNOWLEDGEMENT_PENDING)
            for (status in statuses) {
                val gateway =
                    FakeBillingGateway().apply {
                        when (status) {
                            PurchaseStatus.FAILED -> {
                                owned = StoreResult.Failure(StoreFailure.UNAVAILABLE)
                            }

                            PurchaseStatus.UNAVAILABLE -> {
                                productResult = StoreResult.Failure(StoreFailure.UNAVAILABLE)
                            }

                            else -> {
                                owned = StoreResult.Success(listOf(SignedPurchases.purchase()))
                                acknowledgement = StoreResult.Failure(StoreFailure.UNAVAILABLE)
                            }
                        }
                    }
                val service = LocalPurchases(gateway, SignedPurchases.verifier(), TestOwnershipStore(), backgroundScope)
                service.onForeground()
                runCurrent()
                assertEquals(status, service.state.value.status)
                assertEquals(1, gateway.queries)
                for ((index, delay) in listOf(2_000L, 10_000L, 60_000L).withIndex()) {
                    advanceTimeBy(delay - 1)
                    runCurrent()
                    assertEquals(index + 1, gateway.queries)
                    advanceTimeBy(1)
                    runCurrent()
                    assertEquals(index + 2, gateway.queries)
                }
                advanceTimeBy(100_000)
                runCurrent()
                assertEquals(4, gateway.queries)
                service.refresh()
                runCurrent()
                assertEquals(5, gateway.queries)
                service.onBackground()
                advanceTimeBy(100_000)
                runCurrent()
                assertEquals(5, gateway.queries)
            }
        }

    @Test fun partialCacheFlagsCannotGrantAndClearPreservesUnrelatedKeys() =
        runTest {
            val preferences = InMemoryPreferencesStore()
            val store = PreferencesOwnershipStore(preferences)
            val jsonKey = stringPreferencesKey("verified_receipt")
            val signatureKey = stringPreferencesKey("verified_signature")
            val acknowledgedKey = booleanPreferencesKey("acknowledged")
            val unrelated = stringPreferencesKey("unrelated_test_key")
            val purchase = SignedPurchases.purchase()
            preferences.edit {
                it[unrelated] = "preserved"
                it[acknowledgedKey] = true
            }
            assertNull(store.read())
            preferences.edit { it[jsonKey] = purchase.originalJson }
            assertNull(SignedPurchases.verifier().verify(requireNotNull(store.read()).receipt))
            val ownership = PurchaseOwnership(store, SignedPurchases.verifier(), FakeBillingGateway())
            ownership.load()
            assertFalse(ownership.fullAccess)
            preferences.edit {
                it[signatureKey] = purchase.signature
                it.remove(acknowledgedKey)
            }
            val complete = requireNotNull(store.read())
            assertFalse(complete.acknowledged)
            assertNotNull(SignedPurchases.verifier().verify(complete.receipt))
            store.write(complete.copy(acknowledged = true))
            assertTrue(requireNotNull(store.read()).acknowledged)
            store.write(null)
            assertNull(store.read())
            val saved = preferences.data.first()
            assertEquals(setOf(unrelated), saved.asMap().keys)
            assertEquals("preserved", saved[unrelated])
        }
}
