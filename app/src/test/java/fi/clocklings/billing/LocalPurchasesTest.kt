package fi.clocklings.billing

import android.app.Activity
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.Progress
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

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalPurchasesTest {
    private val gateway = FakeBillingGateway()
    private val store = TestOwnershipStore()

    private fun TestScope.service(verifier: LocalPurchaseVerifier = SignedPurchases.verifier()) =
        LocalPurchases(gateway, verifier, store, backgroundScope)

    private fun TestScope.beginPausedPurchase(): LocalPurchases {
        val service = service()
        service.onForeground()
        runCurrent()
        service.buy(Activity())
        service.onBackground()
        return service
    }

    private fun TestScope.resumeWithOwnedPurchase(): LocalPurchases {
        val service = beginPausedPurchase()
        gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(acknowledged = true)))
        service.onForeground()
        runCurrent()
        assertTrue(service.state.value.fullAccess)
        return service
    }

    @Test fun launchDoesNotGrantButVerifiedPurchaseAndDuplicateUpdatesAcknowledgeOnce() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            assertEquals("4,50 €", service.state.value.localizedPrice)
            service.buy(Activity())
            assertFalse(service.state.value.fullAccess)
            val receipt = SignedPurchases.purchase()
            gateway.owned = StoreResult.Success(listOf(receipt))
            repeat(2) {
                gateway.update(gateway.owned)
                runCurrent()
                assertTrue(service.state.value.fullAccess)
            }
            assertEquals(listOf(receipt.token), gateway.acknowledged)
            assertTrue(requireNotNull(store.read()).acknowledged)
        }

    @Test fun pendingThenCompletedGrantsOnlyAfterFreshValidatedOwnershipQuery() =
        runTest {
            val service = service()
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(wireState = 4)))
            service.onForeground()
            runCurrent()
            assertEquals(PurchaseStatus.PENDING, service.state.value.status)
            assertFalse(service.state.value.fullAccess)
            assertFalse(service.state.value.canPurchase)
            assertTrue(gateway.acknowledged.isEmpty())
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase()))
            gateway.update(gateway.owned)
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(1, gateway.acknowledged.size)
        }

    @Test fun cancellationOnlyChangesOwnershipAfterAFreshVerifiedQuery() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertFalse(service.state.value.fullAccess)
            assertTrue(service.state.value.canPurchase)
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(acknowledged = true)))
            service.restore()
            runCurrent()
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertTrue(service.state.value.fullAccess)
        }

    @Test fun purchaseCancellationWhilePausedSurvivesReturningToTheApp() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            service.buy(Activity())
            service.buy(Activity())
            service.onBackground()
            assertEquals(0, gateway.closed)
            assertEquals(1, gateway.launches)
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertEquals(1, gateway.closed)
            service.onForeground()
            runCurrent()
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertTrue(service.state.value.canPurchase)
            assertFalse(service.state.value.fullAccess)
            assertTrue(gateway.acknowledged.isEmpty())
            service.onBackground()
            assertEquals(2, gateway.closed)
        }

    @Test fun bufferedCancellationReconcilesCompletedOwnershipOnResume() =
        runTest {
            val service = beginPausedPurchase()
            val receipt = SignedPurchases.purchase()
            gateway.owned = StoreResult.Success(listOf(receipt))
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertFalse(service.state.value.canPurchase)
            assertEquals(listOf(receipt.token), gateway.acknowledged)
            assertTrue(requireNotNull(store.read()).acknowledged)
        }

    @Test fun cancellationReconcilesRevokedOwnershipAfterAResumeQuery() =
        runTest {
            val service = resumeWithOwnedPurchase()
            gateway.owned = StoreResult.Success(emptyList())
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertTrue(service.state.value.canPurchase)
            assertNull(store.read())
        }

    @Test fun cancelledRefreshFailurePreservesCachedAccessAndRetriesRevocation() =
        runTest {
            val service = resumeWithOwnedPurchase()
            gateway.owned = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertNotNull(store.read())
            gateway.owned = StoreResult.Success(emptyList())
            advanceTimeBy(2_000)
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertNull(store.read())
        }

    @Test fun purchaseCallbackWhilePausedRequiresFreshMatchingOwnershipOnResume() =
        runTest {
            val service = beginPausedPurchase()
            gateway.update(StoreResult.Success(listOf(SignedPurchases.purchase(token = "callback-token"))))
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(token = "other-token")))
            service.onForeground()
            runCurrent()
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertFalse(service.state.value.fullAccess)
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun purchaseCompletingWhilePausedIsVerifiedAndAcknowledgedAfterResume() =
        runTest {
            val service = beginPausedPurchase()
            val receipt = SignedPurchases.purchase()
            gateway.owned = StoreResult.Success(listOf(receipt))
            gateway.update(gateway.owned)
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(listOf(receipt.token), gateway.acknowledged)
        }

    @Test fun resumeWithoutPurchaseCallbackRefreshesOwnershipAndReleasesTheFlow() =
        runTest {
            val service = beginPausedPurchase()
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase()))
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            service.onBackground()
            assertEquals(1, gateway.closed)
        }

    @Test fun failedPurchaseLaunchDoesNotKeepTheConnectionOpenInBackground() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            gateway.launchResult = StoreResult.Failure(StoreFailure.CANCELLED)
            service.buy(Activity())
            service.onBackground()
            gateway.update(StoreResult.Success(listOf(SignedPurchases.purchase())))
            runCurrent()
            assertEquals(1, gateway.closed)
            assertFalse(service.state.value.fullAccess)
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun cancellationAfterResumeIsNotOverwrittenByAnInFlightOwnershipQuery() =
        runTest {
            val service = beginPausedPurchase()
            val gate = CompletableDeferred<Unit>()
            gateway.queryGate = gate
            service.onForeground()
            runCurrent()
            val queries = gateway.queries
            gateway.update(StoreResult.Failure(StoreFailure.CANCELLED))
            runCurrent()
            assertEquals(queries + 1, gateway.queries)
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertTrue(service.state.value.busy)
            gate.complete(Unit)
            runCurrent()
            assertEquals(PurchaseStatus.CANCELLED, service.state.value.status)
            assertFalse(service.state.value.busy)
            assertFalse(service.state.value.fullAccess)
        }

    @Test fun alreadyOwnedLaunchRestoresInsteadOfConsumingOrChargingAgain() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(acknowledged = true)))
            gateway.launchResult = StoreResult.Failure(StoreFailure.ALREADY_OWNED)
            service.buy(Activity())
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.RESTORED, service.state.value.status)
            assertTrue(gateway.acknowledged.isEmpty())
            assertEquals(1, gateway.launches)
        }

    @Test fun invalidSignedProductIsNeitherGrantedNorAcknowledged() =
        runTest {
            val wrongApp = SignedPurchases.purchase(packageName = "another.application")
            gateway.owned = StoreResult.Success(listOf(wrongApp))
            val service = service()
            service.onForeground()
            runCurrent()
            gateway.update(StoreResult.Success(listOf(wrongApp)))
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertNull(store.read())
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun acknowledgementFailureRetriesAndSurvivesRestartWithoutDuplicateGrant() =
        runTest {
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase()))
            gateway.acknowledgement = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            val first = service()
            first.onForeground()
            runCurrent()
            assertTrue(first.state.value.fullAccess)
            assertEquals(PurchaseStatus.ACKNOWLEDGEMENT_PENDING, first.state.value.status)
            assertFalse(requireNotNull(store.read()).acknowledged)
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(2, gateway.acknowledged.size)
            first.onBackground()
            gateway.acknowledgement = StoreResult.Success(Unit)
            val restarted = service()
            runCurrent()
            assertTrue(restarted.state.value.fullAccess)
            restarted.onForeground()
            runCurrent()
            assertTrue(requireNotNull(store.read()).acknowledged)
            assertEquals(3, gateway.acknowledged.size)
            restarted.refresh()
            runCurrent()
            assertEquals(3, gateway.acknowledged.size)
        }

    @Test fun cachedOwnershipSurvivesOfflineQueryAndProductFailures() =
        runTest {
            seedOwnership()
            gateway.owned = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            gateway.productResult = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            val service = service()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertNotNull(store.read())
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun confirmedEmptyOwnershipRemovesOnlyTheEntitlementAndStaysRemovedOnRestart() =
        runTest {
            seedOwnership()
            val progress = ProgressRepository(InMemoryPreferencesStore())
            val saved = Progress(stars = listOf(3, 2, 1, 0, 0), lessonsDone = setOf("viisarit"))
            progress.updateProgress { saved }
            val service = service()
            service.onForeground()
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertNull(store.read())
            assertEquals(saved, progress.progress.first())
            service.onBackground()
            val restarted = service()
            runCurrent()
            assertFalse(restarted.state.value.fullAccess)
        }

    @Test fun oldValidCallbackCannotRestoreRevokedOwnership() =
        runTest {
            val service = service()
            service.onForeground()
            runCurrent()
            gateway.update(StoreResult.Success(listOf(SignedPurchases.purchase())))
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertNull(store.read())
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test fun restoreOnFreshInstallUsesPlayAndDoesNotCreateLearningAchievements() =
        runTest {
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(acknowledged = true)))
            val progress = ProgressRepository(InMemoryPreferencesStore())
            val service = service()
            service.restore()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.RESTORED, service.state.value.status)
            assertEquals(Progress(), progress.progress.first())
            progress.updateProgress { it.withLevelResult(0, 3) }
            progress.resetProgress()
            val restarted = service()
            runCurrent()
            assertTrue(restarted.state.value.fullAccess)
            assertEquals(Progress(), progress.progress.first())
        }

    @Test fun invalidQueryKeepsPreviouslyVerifiedOwnershipButCannotGrantNewAccess() =
        runTest {
            seedOwnership()
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(packageName = "wrong.application")))
            val service = service()
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(PurchaseStatus.FAILED, service.state.value.status)
            assertNotNull(store.read())
        }

    @Test fun unavailableProductDisablesPurchaseAndRetryRecoversPrice() =
        runTest {
            gateway.productResult = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            val service = service()
            service.onForeground()
            runCurrent()
            service.buy(Activity())
            assertEquals(0, gateway.launches)
            assertFalse(service.state.value.canPurchase)
            gateway.productResult = StoreResult.Success(StoreProduct("€8.25"))
            advanceTimeBy(2_000)
            runCurrent()
            assertTrue(service.state.value.canPurchase)
            assertEquals("€8.25", service.state.value.localizedPrice)
        }

    @Test fun missingKeyBlocksEveryPurchasePathAndDoesNotDeleteCachedEvidence() =
        runTest {
            seedOwnership()
            val service = service(LocalPurchaseVerifier("", "fi.clocklings"))
            service.onForeground()
            service.restore()
            runCurrent()
            service.buy(Activity())
            assertFalse(service.state.value.fullAccess)
            assertFalse(service.state.value.canPurchase)
            assertEquals(0, gateway.queries)
            assertEquals(0, gateway.launches)
            assertNotNull(store.read())
        }

    @Test fun tamperedCacheCannotGrantAccessEvenWithAcknowledgedFlag() =
        runTest {
            store.write(CachedOwnership(PurchaseReceipt("{}", "invalid"), acknowledged = true))
            val service = service()
            runCurrent()
            assertFalse(service.state.value.fullAccess)
        }

    @Test fun failedDurableGrantDoesNotAcknowledgeAndCanRetry() =
        runTest {
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase()))
            store.failWrite = true
            val service = service()
            service.onForeground()
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            assertTrue(gateway.acknowledged.isEmpty())
            store.failWrite = false
            advanceTimeBy(2_000)
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(1, gateway.acknowledged.size)
        }

    @Test fun failedRevocationPersistenceDisablesAccessAndRetriesTheRemoval() =
        runTest {
            seedOwnership()
            store.failWrite = true
            val service = service()
            service.onForeground()
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            store.failWrite = false
            advanceTimeBy(2_000)
            runCurrent()
            assertNull(store.read())
        }

    @Test fun cacheReadFailureRetriesAndForegroundLifecycleClosesTheConnection() =
        runTest {
            seedOwnership()
            store.failRead = true
            gateway.owned = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            val service = service()
            service.onForeground()
            runCurrent()
            assertFalse(service.state.value.loading)
            assertFalse(service.state.value.fullAccess)
            store.failRead = false
            advanceTimeBy(2_000)
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            val queries = gateway.queries
            service.onBackground()
            advanceTimeBy(100_000)
            runCurrent()
            assertEquals(queries, gateway.queries)
            assertEquals(1, gateway.closed)
        }

    private suspend fun seedOwnership() {
        val receipt = requireNotNull(SignedPurchases.verifier().verify(SignedPurchases.purchase()))
        store.write(CachedOwnership(receipt.receipt, acknowledged = true))
    }

    @Test fun accountChangeReconcilesOnlySuccessfulQueriesAndNewSignedOwnership() =
        runTest {
            seedOwnership()
            val service = service()
            gateway.owned = StoreResult.Failure(StoreFailure.UNAVAILABLE)
            service.onForeground()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            gateway.owned = StoreResult.Success(emptyList())
            service.refresh()
            runCurrent()
            assertFalse(service.state.value.fullAccess)
            gateway.owned = StoreResult.Success(listOf(SignedPurchases.purchase(token = "second-account-token")))
            service.restore()
            runCurrent()
            assertTrue(service.state.value.fullAccess)
            assertEquals(listOf("second-account-token"), gateway.acknowledged)
        }
}
