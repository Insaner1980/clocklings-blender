package fi.clocklings.analytics

import fi.clocklings.data.InMemoryPreferencesStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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

    private fun TestScope.analytics() =
        WeeklyUsageAnalytics(store, backgroundScope, StandardTestDispatcher(testScheduler), scheduled::add, clock)

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
            usage.count("game.000.start")
            usage.count("profile.child")
            runCurrent()
            assertTrue(upload(usage))
            assertTrue(requests.isEmpty())

            clock.set("2026-11-16T00:00:00Z")
            assertTrue(upload(usage))
            val batch = requests.single()
            assertEquals("2026-11-09", batch.week)
            assertEquals(mapOf("game.0.read" to 1_000, "lesson.0.replay" to 1), batch.counts)
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
