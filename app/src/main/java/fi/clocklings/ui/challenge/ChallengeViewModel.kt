package fi.clocklings.ui.challenge

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.domain.Levels
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.components.OptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.random.Random

enum class ChallengePhase { START, RUN, END }

data class ChallengeUiState(
    val phase: ChallengePhase = ChallengePhase.START,
    val score: Int = 0,
    val best: Int = 0,
    val newRecord: Boolean = false,
    val saveFailed: Boolean = false,
    val savePending: Boolean = false,
    val question: Int = 0,
    val options: List<Int> = emptyList(),
    val optionStates: Map<Int, OptionState> = emptyMap(),
    val busy: Boolean = false,
    val clock: ClockCommand = ClockCommand(START_TIME),
    /** Kasvaa jokaisella aloituksella: UI käynnistää ajastinpalkin alusta. */
    val runId: Int = 0,
    val deadlineNanos: Long = 0,
) {
    fun remainingFraction(nowNanos: Long): Float =
        when (phase) {
            ChallengePhase.START -> {
                1f
            }

            ChallengePhase.END -> {
                0f
            }

            ChallengePhase.RUN -> {
                ((deadlineNanos - nowNanos) / (ChallengeViewModel.DURATION_MS * NANOS_PER_MILLISECOND)).coerceIn(0f, 1f)
            }
        }

    companion object {
        /** Aloitusnäkymän kello: varttia vaille yksi (web: hClock.set(45)). */
        const val START_TIME = 45
        private const val NANOS_PER_MILLISECOND = 1_000_000f
    }
}

/**
 * Minuuttihaaste: niin monta Mitä kello on? -kysymystä kuin minuutissa ehtii.
 * Väärästä vastauksesta näytetään oikea, ja peli jatkuu (web: haasteStart, pickH, haasteEnd).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class ChallengeViewModel(
    private val h24: Boolean,
    private val services: AppServices,
    private val random: Random = Random.Default,
    private val nowNanos: () -> Long = System::nanoTime,
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeUiState())
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()

    private var timer: Job? = null
    private var pending: Job? = null

    /** The end fanfare, which waits until the narrator has said that time is up. */
    private var endSound: Job? = null

    init {
        viewModelScope.launch {
            val best =
                services.repository.progress
                    .first()
                    .challengeBest
            _state.update { it.copy(best = maxOf(it.best, best)) }
        }
    }

    fun start() {
        if (!services.purchases.state.value.fullAccess) return
        if (_state.value.savePending) return
        timer?.cancel()
        pending?.cancel()
        endSound?.cancel()
        services.play(Sound.CHALLENGE_GO)
        _state.update {
            it.copy(
                phase = ChallengePhase.RUN,
                score = 0,
                newRecord = false,
                saveFailed = false,
                runId = it.runId + 1,
                deadlineNanos = nowNanos() + DURATION_MS * 1_000_000L,
            )
        }
        nextQuestion()
        timer =
            viewModelScope.launch {
                delay(DURATION_MS)
                end()
            }
    }

    private fun nextQuestion() {
        val previous = _state.value.question.takeIf { _state.value.options.isNotEmpty() }
        val q = Levels.challengeQuestion(h24, previous, random)
        _state.update {
            it.copy(
                question = q.time,
                options = Levels.optionsFor(q.time, q.level, h24, random),
                optionStates = emptyMap(),
                busy = false,
                clock = ClockCommand(q.time, MOVE_MS),
            )
        }
    }

    fun pick(time: Int) {
        val s = _state.value
        if (s.phase != ChallengePhase.RUN || s.busy) return
        if (nowNanos() >= s.deadlineNanos) return
        val correct = time == s.question
        services.play(if (correct) Sound.OK else Sound.BAD)
        _state.value =
            if (correct) {
                s.copy(busy = true, score = s.score + 1, optionStates = mapOf(time to OptionState.CORRECT))
            } else {
                s.copy(busy = true, optionStates = mapOf(time to OptionState.WRONG, s.question to OptionState.CORRECT))
            }
        pending =
            viewModelScope.launch {
                delay(if (correct) NEXT_AFTER_CORRECT_MS else NEXT_AFTER_WRONG_MS)
                if (_state.value.phase == ChallengePhase.RUN) nextQuestion()
            }
    }

    fun retrySave() {
        if (!_state.value.saveFailed || _state.value.savePending) return
        _state.update { it.copy(savePending = true) }
        viewModelScope.launch { end() }
    }

    private suspend fun end() {
        pending?.cancel()
        endSound?.cancel()
        _state.update { it.copy(busy = true, savePending = true) }
        val s = _state.value
        // A retried save ends the same run again: time ran out already, so only the fanfare repeats.
        val timeRanOut = s.phase == ChallengePhase.RUN
        var best = s.best
        var record = s.score > best
        val saved =
            try {
                if (record) {
                    services.repository.updateProgress {
                        best = it.challengeBest
                        record = s.score > best
                        it.withChallengeScore(s.score)
                    }
                }
                true
            } catch (_: IOException) {
                false
            }
        val newRecord = saved && record && s.score > 0
        val fanfare = if (newRecord) Sound.RECORD else Sound.WIN
        if (timeRanOut) {
            services.play(Sound.CHALLENGE_OVER)
            endSound =
                viewModelScope.launch {
                    delay(FANFARE_AFTER_VOICE_MS)
                    services.play(fanfare)
                }
        } else {
            services.play(fanfare)
        }
        _state.update {
            it.copy(
                phase = ChallengePhase.END,
                best = if (saved && record) s.score else best,
                newRecord = newRecord,
                saveFailed = !saved,
                savePending = false,
                busy = true,
            )
        }
    }

    companion object {
        const val DURATION_MS = 60_000L
        private const val MOVE_MS = 350
        private const val NEXT_AFTER_CORRECT_MS = 380L
        private const val NEXT_AFTER_WRONG_MS = 1300L

        /** The narrator's "Time is up!" lasts at most 1.5 s in any language. */
        private const val FANFARE_AFTER_VOICE_MS = 1_500L
    }
}
