package fi.clocklings.ui.game

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.Screen
import fi.clocklings.audio.Sound
import fi.clocklings.audio.hintVoice
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Hint
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Levels
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.OptionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.random.Random

private const val MAX_TRIES = 2

/** Maps the wrong answers given to one task onto its progress dot. */
internal fun outcomeOf(taskMistakes: Int): DotOutcome =
    when {
        taskMistakes >= MAX_TRIES -> DotOutcome.SHOWN
        taskMistakes > 0 -> DotOutcome.WITH_HINT
        else -> DotOutcome.FIRST_TRY
    }

/** Palaute paneelin alla. Tekstit muodostetaan UI:ssa strings.xml:stä. */
@Immutable
sealed interface GameFeedback {
    data object SaveFailed : GameFeedback

    /** Kehu; Mitä kello on? -tehtävässä mukana oikea aika ([time]). */
    data class Praise(
        val praiseIndex: Int,
        val time: Int?,
    ) : GameFeedback

    data class HintText(
        val hint: Hint,
    ) : GameFeedback

    data class RevealRead(
        val time: Int,
    ) : GameFeedback

    data class RevealSet(
        val time: Int,
    ) : GameFeedback
}

data class GameUiState(
    val level: Int,
    val h24: Boolean,
    val taskIndex: Int,
    val taskCount: Int,
    val task: Task,
    val options: List<Int> = emptyList(),
    val optionStates: Map<Int, OptionState> = emptyMap(),
    val clock: ClockCommand,
    val step: Int,
    val interactive: Boolean = false,
    val showDone: Boolean = false,
    val showNext: Boolean = false,
    val busy: Boolean = false,
    val mistakes: Int = 0,
    val taskMistakes: Int = 0,
    /** One entry per finished task, in order: how that task went (drives the progress dots). */
    val outcomes: List<DotOutcome> = emptyList(),
    val feedback: GameFeedback? = null,
    /** Kasvaa jokaisella onnistumisella: UI ampuu konfetit. */
    val celebrate: Int = 0,
    val finished: Screen.Result? = null,
)

/**
 * Yksi pelikerta: 10 tehtävää, vinkki ensimmäisen virheen jälkeen ja vastaus toisen jälkeen
 * (web: startLevel, renderTask, pickOption, doneBtn, finish).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class GameViewModel(
    private val level: Int,
    private val h24: Boolean,
    private val services: AppServices,
    private val random: Random = Random.Default,
) : ViewModel() {
    init {
        require(AccessPolicy(services.purchases.state.value.fullAccess).level(level)) { "Level access required" }
    }

    private val lv = Levels.all[level]
    private val tasks = Levels.makeTasks(lv, h24, random)
    private var advanceJob: Job? = null

    /** A hint the narrator is about to read once the wrong-answer sound has faded. */
    private var hintVoiceJob: Job? = null

    private val _state = MutableStateFlow(taskState(0, mistakes = 0, celebrate = 0, from = null))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    init {
        services.usage.count("game.$level.start")
        announce(_state.value.task)
    }

    /** The narrator reads each task's instruction, since read and set tasks take turns. */
    private fun announce(task: Task) {
        hintVoiceJob?.cancel()
        val kind = if (task.type == TaskType.READ) "read" else "set"
        services.usage.count("game.$level.$kind")
        services.play(if (task.type == TaskType.READ) Sound.QUESTION_READ else Sound.QUESTION_SET)
    }

    /** Reads which hand [hint] is about once the wrong-answer sound has faded. */
    private fun readHint(hint: Hint) {
        hintVoiceJob?.cancel()
        val voice = hintVoice(hint) ?: return
        hintVoiceJob =
            viewModelScope.launch {
                delay(HINT_VOICE_DELAY_MS)
                services.play(voice)
            }
    }

    private fun taskState(
        index: Int,
        mistakes: Int,
        celebrate: Int,
        from: GameUiState?,
    ): GameUiState {
        val task = tasks[index]
        val base =
            GameUiState(
                level = level,
                h24 = h24,
                taskIndex = index,
                taskCount = tasks.size,
                task = task,
                clock = ClockCommand(task.time, READ_MOVE_MS),
                step = lv.step,
                mistakes = mistakes,
                outcomes = from?.outcomes.orEmpty(),
                celebrate = celebrate,
            )
        return if (task.type == TaskType.READ) {
            base.copy(options = Levels.optionsFor(task.time, lv, h24, random))
        } else {
            val start = Levels.setTaskStart(task.time)
            base.copy(
                clock =
                    ClockCommand(
                        start,
                        if (from ==
                            null
                        ) {
                            0
                        } else {
                            SET_MOVE_MS
                        },
                    ),
                interactive = true,
                showDone = true,
            )
        }
    }

    fun onClockChange(value: Int) {
        if (!_state.value.interactive) return
        _state.update { it.copy(clock = it.clock.copy(time = value, ms = 0)) }
    }

    fun pick(time: Int) {
        val s = _state.value
        if (s.busy || s.task.type != TaskType.READ || s.optionStates[time] == OptionState.WRONG) return
        if (time == s.task.time) {
            success(s.copy(optionStates = s.optionStates + (time to OptionState.CORRECT)))
            return
        }
        services.play(Sound.BAD)
        hintVoiceJob?.cancel()
        val wrong =
            s.copy(
                mistakes = s.mistakes + 1,
                taskMistakes = s.taskMistakes + 1,
                optionStates = s.optionStates + (time to OptionState.WRONG),
            )
        _state.value =
            if (wrong.taskMistakes >= MAX_TRIES) {
                services.usage.count("game.$level.read_reveal")
                wrong.copy(
                    busy = true,
                    optionStates = wrong.optionStates + (s.task.time to OptionState.CORRECT),
                    feedback = GameFeedback.RevealRead(s.task.time),
                    showNext = true,
                )
            } else {
                val hint = Hints.forRead(s.task.time, time)
                readHint(hint)
                wrong.copy(feedback = GameFeedback.HintText(hint))
            }
    }

    fun done() {
        val s = _state.value
        if (s.busy || s.task.type != TaskType.SET) return
        val submittedTime = s.clock.time
        // Viisariasento ratkaisee: 3.30 ja 15.30 näyttävät kellotaululla samalta.
        if (ClockTime.sameHands(submittedTime, s.task.time)) {
            success(s)
            return
        }
        services.play(Sound.BAD)
        hintVoiceJob?.cancel()
        val wrong = s.copy(mistakes = s.mistakes + 1, taskMistakes = s.taskMistakes + 1)
        _state.value =
            if (wrong.taskMistakes >= MAX_TRIES) {
                services.usage.count("game.$level.set_reveal")
                wrong.copy(
                    busy = true,
                    interactive = false,
                    clock = ClockCommand(s.task.time, REVEAL_MS),
                    feedback = GameFeedback.RevealSet(s.task.time),
                    showDone = false,
                    showNext = true,
                )
            } else {
                val hint = Hints.forSet(s.task.time, submittedTime)
                readHint(hint)
                wrong.copy(feedback = GameFeedback.HintText(hint))
            }
    }

    private fun success(s: GameUiState) {
        hintVoiceJob?.cancel()
        services.play(Sound.OK)
        val isSet = s.task.type == TaskType.SET
        val kind = if (isSet) "set" else "read"
        val outcome = if (s.taskMistakes == 0) "first" else "hint"
        services.usage.count("game.$level.${kind}_$outcome")
        _state.value =
            s.copy(
                busy = true,
                feedback = GameFeedback.Praise(random.nextInt(PRAISE_COUNT), if (isSet) null else s.task.time),
                showDone = false,
                interactive = false,
                clock = if (isSet) ClockCommand(s.task.time, SUCCESS_MOVE_MS) else s.clock,
                celebrate = s.celebrate + 1,
            )
        advanceJob =
            viewModelScope.launch {
                delay(SUCCESS_PAUSE_MS)
                advance()
            }
    }

    fun next() {
        if (!_state.value.showNext) return
        services.play(Sound.POP)
        advance()
    }

    private fun advance() {
        advanceJob?.cancel()
        val current = _state.value
        if (current.finished != null) return
        // A failed save is retried through this same path: record the outcome only once per task.
        val s =
            if (current.outcomes.size > current.taskIndex) {
                current
            } else {
                current.copy(outcomes = current.outcomes + outcomeOf(current.taskMistakes))
            }
        val nextIndex = s.taskIndex + 1
        if (nextIndex >= tasks.size) {
            _state.value = s
            finish(s)
        } else {
            _state.value = taskState(nextIndex, s.mistakes, s.celebrate, s)
            announce(_state.value.task)
        }
    }

    private fun finish(s: GameUiState) {
        _state.update { it.copy(busy = true, showNext = false) }
        viewModelScope.launch {
            try {
                val stars = Levels.stars(s.mistakes)
                var previousBest = 0
                var starsBefore = 0
                services.repository.updateProgress {
                    previousBest = it.stars[level]
                    starsBefore = it.totalStars
                    it.withLevelResult(level, stars)
                }
                services.usage.count("game.$level.finish")
                services.play(Sound.WIN)
                _state.update {
                    it.copy(
                        finished =
                            Screen.Result(
                                level = level,
                                stars = stars,
                                previousBest = previousBest,
                                starsBefore = starsBefore,
                            ),
                    )
                }
            } catch (_: IOException) {
                _state.update { it.copy(feedback = GameFeedback.SaveFailed, showNext = true) }
            }
        }
    }

    private companion object {
        const val PRAISE_COUNT = 7
        const val READ_MOVE_MS = 700
        const val SET_MOVE_MS = 500
        const val SUCCESS_MOVE_MS = 300
        const val REVEAL_MS = 1400
        const val SUCCESS_PAUSE_MS = 1500L

        /** The wrong-answer sound has faded by then (it is loud for its first 1.2 s). */
        const val HINT_VOICE_DELAY_MS = 1250L
    }
}
