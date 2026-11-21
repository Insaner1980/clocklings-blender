package fi.clocklings.analytics

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.Network
import android.os.Build
import androidx.datastore.preferences.preferencesDataStore
import fi.clocklings.BuildConfig
import fi.clocklings.ClocklingsApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume

private const val UPLOAD_JOB_ID = 529
private const val NETWORK_TIMEOUT_MS = 10_000
private val Context.usageDataStore by preferencesDataStore(name = "weekly_usage")

internal fun createUsageAnalytics(context: Context): UsageAnalytics {
    if (BuildConfig.ANALYTICS_ENDPOINT.isEmpty()) return UsageAnalytics.Disabled
    val dispatcher = Dispatchers.Main.immediate
    return WeeklyUsageAnalytics(
        store = context.usageDataStore,
        scope = CoroutineScope(SupervisorJob() + dispatcher),
        dispatcher = dispatcher,
        schedule = { enabled -> scheduleUpload(context, enabled) },
    )
}

private fun scheduleUpload(
    context: Context,
    enabled: Boolean,
) {
    val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
    if (!enabled) {
        scheduler.cancel(UPLOAD_JOB_ID)
    } else if (scheduler.getPendingJob(UPLOAD_JOB_ID) == null) {
        val info =
            JobInfo
                .Builder(UPLOAD_JOB_ID, ComponentName(context, UsageUploadService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(TimeUnit.DAYS.toMillis(1))
                .build()
        scheduler.schedule(info)
    }
}

class UsageUploadService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val usage = (application as? ClocklingsApplication)?.usage as? WeeklyUsageAnalytics ?: return false
        val endpoint = URI("${BuildConfig.ANALYTICS_ENDPOINT}/v1/metrics").toURL()
        val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) params.network else null
        running =
            scope.launch {
                val sent = usage.uploadClosedWeeks { sendWeeklyBatch(endpoint, network, it) }
                running = null
                jobFinished(params, !sent)
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

internal suspend fun sendWeeklyBatch(
    endpoint: URL,
    network: Network?,
    batch: WeeklyBatch,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): UploadOutcome =
    withContext(ioDispatcher) {
        suspendCancellableCoroutine { call ->
            val connection = (network?.openConnection(endpoint) ?: endpoint.openConnection()) as HttpsURLConnection
            call.invokeOnCancellation { connection.disconnect() }
            try {
                val payload =
                    JSONObject()
                        .put("id", batch.id)
                        .put("week", batch.week)
                        .put("counts", JSONObject(batch.counts))
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.connectTimeout = NETWORK_TIMEOUT_MS
                connection.readTimeout = NETWORK_TIMEOUT_MS
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Clocklings")
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(payload.size)
                if (!call.isActive) return@suspendCancellableCoroutine
                connection.outputStream.use {
                    if (call.isActive) it.write(payload)
                }
                if (!call.isActive) return@suspendCancellableCoroutine
                call.resume(UploadOutcome.fromHttpStatus(connection.responseCode))
            } catch (_: IOException) {
                call.resume(UploadOutcome.RETRY)
            } finally {
                connection.disconnect()
            }
        }
    }
