package fi.clocklings.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import fi.clocklings.data.InMemoryPreferencesStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyUsageAnalyticsTest {
    private val store = InMemoryPreferencesStore()
    private val clock = MutableClock("2026-11-15T23:59:59Z")
    private val scheduled = mutableListOf<Boolean>()
    private val requests = mutableListOf<WeeklyBatch>()

    private fun TestScope.analytics(preferences: DataStore<Preferences> = store) =
        WeeklyUsageAnalytics(preferences, backgroundScope, StandardTestDispatcher(testScheduler), scheduled::add, clock)

    private fun failFirstUpdate(): DataStore<Preferences> =
        object : DataStore<Preferences> by store {
            private var failed = false

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                if (!failed) {
                    failed = true
                    throw IOException("Temporary storage failure")
                }
                return store.updateData(transform)
            }
        }

    @Test
    fun persistedConsentRecoversAfterAnInitialStorageFailure() =
        runTest {
            store.edit { it[booleanPreferencesKey("consent")] = true }
            val usage = analytics(failFirstUpdate())
            runCurrent()
            assertFalse(usage.enabled.first())
            assertTrue(scheduled.isEmpty())
            usage.count("game.0.start")

            advanceTimeBy(5_000)
            runCurrent()
            assertTrue(usage.enabled.first())
            assertEquals(listOf(true), scheduled)
            usage.count("lesson.0.start")
            runCurrent()
            clock.set("2026-11-16T12:00:00Z")
            assertTrue(upload(usage))
            assertEquals(mapOf("lesson.0.start" to 1), requests.single().counts)
        }

    @Test
    fun revocationWhileInitializationRetriesOverridesPersistedConsent() =
        runTest {
            store.edit { it[booleanPreferencesKey("consent")] = true }
            val usage = analytics(failFirstUpdate())
            runCurrent()
            usage.setEnabled(false)

            runCurrent()
            assertFalse(usage.enabled.first())
            assertEquals(listOf(false), scheduled)
            val remaining = store.data.first()
            assertTrue(remaining.asMap().isEmpty())
            val restarted = analytics()
            runCurrent()
            assertFalse(restarted.enabled.first())

            advanceTimeBy(5_000)
            runCurrent()
            assertFalse(usage.enabled.first())
            assertEquals(listOf(false), scheduled)
            assertTrue(upload(usage))
            assertTrue(requests.isEmpty())
        }

    @Test
    fun consentChangesStayOrderedWhileInitializationRetries() =
        runTest {
            store.edit { it[booleanPreferencesKey("consent")] = true }
            val usage = analytics(failFirstUpdate())
            runCurrent()

            usage.setEnabled(true)
            usage.setEnabled(false)
            runCurrent()
            assertFalse(usage.enabled.first())
            val remaining = store.data.first()
            assertTrue(remaining.asMap().isEmpty())
            assertEquals(listOf(false), scheduled)

            usage.setEnabled(true)
            runCurrent()
            assertTrue(usage.enabled.first())
            assertEquals(true, store.data.first()[booleanPreferencesKey("consent")])
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(listOf(false, true), scheduled)
        }

    private suspend fun upload(
        usage: WeeklyUsageAnalytics,
        send: suspend (WeeklyBatch) -> UploadOutcome = { UploadOutcome.ACCEPTED },
    ): Boolean =
        usage.uploadClosedWeeks { batch ->
            requests.add(batch)
            send(batch)
        }

    @Test
    fun transportRetriesTimeoutRateLimitAndServerFailure() {
        assertEquals(UploadOutcome.ACCEPTED, UploadOutcome.fromHttpStatus(204))
        assertEquals(UploadOutcome.REJECTED, UploadOutcome.fromHttpStatus(400))
        assertEquals(UploadOutcome.REJECTED, UploadOutcome.fromHttpStatus(410))
        assertEquals(UploadOutcome.RETRY, UploadOutcome.fromHttpStatus(408))
        assertEquals(UploadOutcome.RETRY, UploadOutcome.fromHttpStatus(429))
        assertEquals(UploadOutcome.RETRY, UploadOutcome.fromHttpStatus(503))
    }

    @Test
    fun countsRequireConsentAndSendOnlyWhenTheUtcWeekCloses() =
        runTest {
            val usage = analytics()
            runCurrent()
            assertFalse(usage.enabled.first())
            assertTrue(scheduled.isEmpty())
            usage.count("game.0.start")
            usage.setEnabled(true)
            runCurrent()
            assertTrue(usage.enabled.first())
            assertEquals(listOf(true), scheduled)
            repeat(1_001) { usage.count("game.0.read") }
            usage.count("lesson.0.replay")
            usage.count("game.99.set_reveal")
            usage.count("challenge.timeout")
            usage.count("screen.play_together")
            listOf(
                "game.000.start",
                "game.100.read",
                "game.01.read",
                "game.0.read_extra",
                "screen.home.",
                "profile.child",
            ).forEach(usage::count)
            runCurrent()
            assertTrue(upload(usage))
            assertTrue(requests.isEmpty())

            clock.set("2026-11-16T00:00:00Z")
            assertTrue(upload(usage))
            val batch = requests.single()
            assertEquals("2026-11-09", batch.week)
            assertEquals(
                mapOf(
                    "game.0.read" to 1_000,
                    "lesson.0.replay" to 1,
                    "game.99.set_reveal" to 1,
                    "challenge.timeout" to 1,
                    "screen.play_together" to 1,
                ),
                batch.counts,
            )
            val id = UUID.fromString(batch.id)
            assertEquals(4, id.version())
            assertEquals(2, id.variant())
            assertEquals(id.toString(), batch.id)
        }

    @Test
    fun aRetryKeepsItsBatchAcrossRestartsAndRejectionDiscardsOnlyThatWeek() =
        runTest {
            clock.set("2026-10-12T12:00:00Z")
            val usage = analytics()
            usage.setEnabled(true)
            runCurrent()
            usage.count("game.0.start")
            runCurrent()
            clock.set("2026-10-19T12:00:00Z")
            usage.count("lesson.1.start")
            runCurrent()
            clock.set("2026-11-16T12:00:00Z")
            usage.count("screen.home")
            runCurrent()

            assertFalse(upload(usage) { UploadOutcome.RETRY })
            val firstAttempt = requests.single()
            assertEquals("2026-10-19", firstAttempt.week)
            assertEquals(mapOf("lesson.1.start" to 1), firstAttempt.counts)

            val restarted = analytics()
            runCurrent()
            assertTrue(restarted.enabled.first())
            assertTrue(upload(restarted) { UploadOutcome.REJECTED })
            assertEquals(listOf(firstAttempt, firstAttempt), requests)
            clock.set("2026-11-23T12:00:00Z")
            assertTrue(upload(restarted))
            val current = requests.last()
            assertEquals("2026-11-16", current.week)
            assertEquals(mapOf("screen.home" to 1), current.counts)
            assertNotEquals(firstAttempt.id, current.id)
        }

    @Test
    fun revocationCancelsTheActiveRequestAndClearsAllPendingCounts() =
        runTest {
            val sending = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val usage = analytics()
            usage.setEnabled(true)
            runCurrent()
            usage.count("game.0.start")
            runCurrent()
            clock.set("2026-11-16T12:00:00Z")
            usage.count("screen.home")
            runCurrent()
            val worker =
                async {
                    upload(usage) {
                        sending.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled.complete(Unit)
                        }
                    }
                }
            sending.await()

            usage.setEnabled(false)
            assertFalse(usage.enabled.first())
            usage.count("screen.parent")
            runCurrent()
            cancelled.await()
            assertTrue(worker.isCancelled)
            val remaining = store.data.first()
            assertTrue(remaining.asMap().isEmpty())
            assertEquals(listOf(true, false), scheduled)

            val restarted = analytics()
            runCurrent()
            assertFalse(restarted.enabled.first())
            restarted.setEnabled(true)
            runCurrent()
            restarted.count("challenge.start")
            runCurrent()
            clock.set("2026-11-23T12:00:00Z")
            assertTrue(upload(restarted))
            assertEquals(2, requests.size)
            assertEquals(mapOf("challenge.start" to 1), requests.last().counts)
        }

    private class MutableClock(
        initial: String,
    ) : Clock() {
        private var now = Instant.parse(initial)

        fun set(value: String) {
            now = Instant.parse(value)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)
    }
}
