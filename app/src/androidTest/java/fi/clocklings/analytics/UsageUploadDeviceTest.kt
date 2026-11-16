package fi.clocklings.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class UsageUploadDeviceTest {
    @Test
    fun sendsTheSameSyntheticBatchTwice() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("clocklingsAnalyticsSmoke") == "true")
        val endpoint = URI("https://clocklings-analytics.emmahota.workers.dev/v1/metrics").toURL()
        val previousWeek =
            LocalDate
                .now(Clock.systemUTC())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1)
        val batch =
            WeeklyBatch(
                id = "8ab1565c-787a-427b-bd12-9b5bd06c8368",
                week = previousWeek.toString(),
                counts = mapOf("game.99.start" to 1),
            )
        runBlocking {
            repeat(2) {
                assertEquals(UploadOutcome.ACCEPTED, sendWeeklyBatch(endpoint, network = null, batch))
            }
        }
    }
}
