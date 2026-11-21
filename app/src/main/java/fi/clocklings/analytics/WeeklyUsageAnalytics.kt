package fi.clocklings.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

internal data class WeeklyBatch(
    val id: String,
    val week: String,
    val counts: Map<String, Int>,
)

internal enum class UploadOutcome {
    ACCEPTED,
    REJECTED,
    RETRY,
    ;

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429

        fun fromHttpStatus(status: Int): UploadOutcome =
            when (status) {
                HttpURLConnection.HTTP_NO_CONTENT -> ACCEPTED
                HttpURLConnection.HTTP_CLIENT_TIMEOUT, HTTP_TOO_MANY_REQUESTS -> RETRY
                in HttpURLConnection.HTTP_BAD_REQUEST until HttpURLConnection.HTTP_INTERNAL_ERROR -> REJECTED
                else -> RETRY
            }
    }
}

internal class WeeklyUsageAnalytics(
    private val store: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val schedule: (Boolean) -> Unit,
    private val clock: Clock = Clock.systemUTC(),
) : UsageAnalytics {
    private val consent = MutableStateFlow(false)
    override val available = true
    override val enabled = consent.asStateFlow()

    private val writes = Mutex()
    private var consentRevision = 0L
    private var activeUpload: Job? = null
    private val initialized =
        scope.launch {
            while (true) {
                try {
                    val persisted =
                        store.edit {
                            if (it[CONSENT] == true) it.prune(currentWeek()) else it.clear()
                        }
                    if (consentRevision == 0L && persisted[CONSENT] == true) {
                        consent.value = true
                        schedule(true)
                    }
                    return@launch
                } catch (_: IOException) {
                    // Keep reporting disabled until persisted consent can be read.
                    delay(STORAGE_RETRY_MS)
                }
            }
        }
    private var lastConsentWrite: Job? = null

    override fun setEnabled(enabled: Boolean) {
        val revision = ++consentRevision
        if (!enabled) {
            consent.value = false
            activeUpload?.cancel()
            schedule(false)
        }
        val previousWrite = lastConsentWrite
        lastConsentWrite =
            scope.launch {
                previousWrite?.join()
                writes.withLock {
                    persistConsent(enabled)
                    if (revision == consentRevision && enabled) {
                        consent.value = true
                        schedule(true)
                    }
                }
            }
    }

    private suspend fun persistConsent(enabled: Boolean) {
        while (true) {
            try {
                store.edit { if (enabled) it[CONSENT] = true else it.clear() }
                return
            } catch (_: IOException) {
                // Apply revocation durably before any subsequent grant can take effect.
                delay(STORAGE_RETRY_MS)
            }
        }
    }

    override fun count(metric: String) {
        if (!consent.value || !isValidMetric(metric)) return
        val revision = consentRevision
        val current = currentWeek()
        scope.launch {
            writes.withLock {
                if (!consent.value || revision != consentRevision) return@withLock
                try {
                    store.edit { prefs ->
                        prefs.prune(currentWeek())
                        val week = current.toString()
                        if (week <= prefs[CLOSED_THROUGH].orEmpty()) return@edit
                        val key = intPreferencesKey("$week/$metric")
                        prefs[key] = ((prefs[key] ?: 0) + 1).coerceAtMost(MAX_COUNT)
                    }
                } catch (_: IOException) {
                    // The optional counter must never block an exercise.
                }
            }
        }
    }

    // The dispatcher confines activeUpload and consent changes to the same thread.
    @Suppress("kotlin:S6311")
    suspend fun uploadClosedWeeks(send: suspend (WeeklyBatch) -> UploadOutcome): Boolean =
        withContext(dispatcher) {
            initialized.join()
            if (activeUpload != null) return@withContext false
            activeUpload = coroutineContext.job
            try {
                uploadPendingBatches(send)
            } catch (_: IOException) {
                false
            } finally {
                activeUpload = null
            }
        }

    private suspend fun uploadPendingBatches(send: suspend (WeeklyBatch) -> UploadOutcome): Boolean {
        var outcome: UploadOutcome?
        do {
            val batch = writes.withLock { if (consent.value) nextBatch() else null }
            outcome = if (batch != null && consent.value) send(batch) else null
            if (batch != null && outcome != null && outcome != UploadOutcome.RETRY) {
                writes.withLock {
                    store.edit { prefs ->
                        prefs
                            .asMap()
                            .keys
                            .filter { it.name.startsWith("${batch.week}/") }
                            .forEach { prefs.remove(it) }
                    }
                }
            }
        } while (outcome != null && outcome != UploadOutcome.RETRY)
        return outcome != UploadOutcome.RETRY
    }

    private suspend fun nextBatch(): WeeklyBatch? {
        val current = currentWeek()
        var batch: WeeklyBatch? = null
        store.edit { prefs ->
            prefs.prune(current)
            val week =
                prefs
                    .asMap()
                    .keys
                    .mapNotNull { it.name.weekOrNull() }
                    .filter { it < current.toString() }
                    .minOrNull() ?: return@edit
            val counts =
                prefs
                    .asMap()
                    .mapNotNull { (key, value) ->
                        if (key.name.weekOrNull() == week && value is Int) {
                            key.name.substringAfter('/') to value
                        } else {
                            null
                        }
                    }.toMap()
            val key = stringPreferencesKey("$week/id")
            val id = prefs[key] ?: UUID.randomUUID().toString()
            prefs[key] = id
            prefs[CLOSED_THROUGH] = maxOf(prefs[CLOSED_THROUGH].orEmpty(), week)
            batch = WeeklyBatch(id, week, counts)
        }
        return batch
    }

    private fun currentWeek(): LocalDate = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun MutablePreferences.prune(current: LocalDate) {
        val earliest = current.minusWeeks(RETENTION_WEEKS).toString()
        asMap()
            .keys
            .filter { (it.name.weekOrNull() ?: earliest) < earliest }
            .forEach { remove(it) }
    }

    private fun String.weekOrNull(): String? = substringBefore('/').takeIf { '/' in this }

    private companion object {
        val CONSENT = booleanPreferencesKey("consent")
        val CLOSED_THROUGH = stringPreferencesKey("closed_through")
        const val RETENTION_WEEKS = 4L
        const val MAX_COUNT = 1_000
        const val STORAGE_RETRY_MS = 5_000L
        const val INDEXED_METRIC_PARTS = 3

        val LEVEL_INDEX = Regex("0|[1-9][0-9]?")
        val SESSION_ACTIONS = setOf("start", "finish")
        val GAME_ACTIONS =
            SESSION_ACTIONS +
                setOf(
                    "read",
                    "set",
                    "read_first",
                    "read_hint",
                    "read_reveal",
                    "set_first",
                    "set_hint",
                    "set_reveal",
                )
        val LESSON_ACTIONS = SESSION_ACTIONS + setOf("wrong", "correct", "replay")
        val LEVEL_ACTIONS = mapOf("game" to GAME_ACTIONS, "lesson" to LESSON_ACTIONS)
        val CHALLENGE_ACTIONS = SESSION_ACTIONS + setOf("correct", "wrong", "timeout")
        val SCREENS =
            setOf(
                "home",
                "lessons",
                "levels",
                "free_clock",
                "play_together",
                "wardrobe",
                "personalize",
                "settings",
                "parent",
                "challenge",
                "result",
            )

        fun isValidMetric(metric: String): Boolean {
            val parts = metric.split('.', limit = 4)
            val levelActions = LEVEL_ACTIONS[parts.first()]
            if (levelActions != null) {
                return parts.size == INDEXED_METRIC_PARTS && LEVEL_INDEX.matches(parts[1]) && parts[2] in levelActions
            }
            return when (parts.first()) {
                "challenge" -> parts.size == 2 && parts[1] in CHALLENGE_ACTIONS
                "screen" -> parts.size == 2 && parts[1] in SCREENS
                else -> false
            }
        }
    }
}
