package fi.clocklings.ui.lesson

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Choice
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.DemoStep
import fi.clocklings.domain.Hand
import fi.clocklings.domain.Lesson
import fi.clocklings.domain.Mark
import fi.clocklings.domain.Text
import fi.clocklings.ui.clock.ClockCommand
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

/** Mitä Seuraava-nappi tekee ja mikä sen teksti on. */
enum class NextAction { NEXT_PAGE, TRY_IT, NEXT_LESSON, START_GAME, FINISH }

data class LessonUiState(
    val lesson: Lesson,
    val queueIndex: Int = 0,
    val page: Int = 0,
    val clock: ClockCommand = ClockCommand(0),
    val period: Int = ClockTime.HALF_DAY,
    val help: Boolean = false,
    val highlight: Hand? = null,
    val under: List<Mark> = emptyList(),
    val over: List<Mark> = emptyList(),
    val caption: Text? = null,
    val choices: List<Choice> = emptyList(),
    val choiceStates: Map<Int, OptionState> = emptyMap(),
    val showHint: Boolean = false,
    val quizDone: Boolean = false,
    val savePending: Boolean = false,
    val saveFailed: Boolean = false,
    val praiseIndex: Int = 0,
    val celebrate: Int = 0,
    val nextAction: NextAction = NextAction.NEXT_PAGE,
    val finished: Boolean = false,
) {
    val isQuiz: Boolean get() = page >= lesson.pages.size
    val pageCount: Int get() = lesson.pages.size + 1
}

/**
 * Oppituntijono: sivut animaatioineen ja lopussa Kokeile itse -kysymys. Animaatio ajetaan
 * korutiinina, joka perutaan sivun vaihtuessa ja näkymästä poistuttaessa (web: runDemo, demoToken).
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virralla.
 */
@Stable
class LessonViewModel(
    private val queue: List<String>,
    private val thenLevel: Int?,
    private val services: AppServices,
    private val random: Random = Random.Default,
    private var language: AppLanguage = AppLanguage.ENGLISH,
) : ViewModel() {
    init {
        val access = AccessPolicy(services.purchases.state.value.fullAccess)
        require(queue.isNotEmpty() && queue.all(access::lesson)) { "Lesson access required" }
        require(thenLevel == null || access.level(thenLevel)) { "Level access required" }
    }

    private val _state = MutableStateFlow(pageState(0, 0, celebrate = 0))
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var demoJob: Job? = null

    init {
        startDemo()
    }

    private fun pageState(
        queueIndex: Int,
        page: Int,
        celebrate: Int,
    ): LessonUiState {
        val lesson = LessonCatalog.lesson(queue[queueIndex], language)
        val base = LessonUiState(lesson = lesson, queueIndex = queueIndex, page = page, celebrate = celebrate)
        return if (page < lesson.pages.size) {
            val p = lesson.pages[page]
            base.copy(
                clock = ClockCommand(p.time),
                period = p.period,
                help = p.help,
                under = p.under,
                nextAction = if (page == lesson.pages.size - 1) NextAction.TRY_IT else NextAction.NEXT_PAGE,
            )
        } else {
            val q = lesson.quiz
            base.copy(clock = ClockCommand(q.time), period = q.period, choices = q.choices.shuffled(random))
        }
    }

    private fun show(
        queueIndex: Int,
        page: Int,
    ) {
        services.stopSpeech()
        demoJob?.cancel()
        _state.value = pageState(queueIndex, page, _state.value.celebrate)
        startDemo()
    }

    fun setLanguage(value: AppLanguage) {
        if (language == value) return
        language = value
        val current = _state.value
        if (current.isQuiz) {
            // Resource references and numeric choices retain their identity and order.
            _state.value = current.copy(lesson = LessonCatalog.lesson(current.lesson.id, language))
        } else {
            show(current.queueIndex, current.page)
        }
    }

    private fun startDemo() {
        val s = _state.value
        if (s.isQuiz) return
        val steps = s.lesson.pages[s.page].demo
        demoJob =
            viewModelScope.launch {
                for (step in steps) {
                    when (step) {
                        is DemoStep.Wait -> {
                            delay(step.ms)
                        }

                        is DemoStep.Move -> {
                            _state.update {
                                it.copy(
                                    clock = ClockCommand(step.to, step.ms, step.exact),
                                )
                            }
                        }

                        is DemoStep.Highlight -> {
                            _state.update { it.copy(highlight = step.hand) }
                        }

                        is DemoStep.Under -> {
                            _state.update { it.copy(under = step.marks) }
                        }

                        is DemoStep.Over -> {
                            _state.update { it.copy(over = step.marks) }
                        }

                        is DemoStep.Say -> {
                            _state.update { it.copy(caption = step.text) }
                        }
                    }
                }
            }
    }

    fun next() {
        val s = _state.value
        val awaitingAnswer = s.isQuiz && !s.quizDone
        if (s.finished || s.savePending || awaitingAnswer) return
        if (!s.isQuiz) {
            services.play(Sound.POP)
            show(s.queueIndex, s.page + 1)
            return
        }
        // Leaving a finished quiz completes the lesson.
        services.play(Sound.LESSON_COMPLETE)
        val access = AccessPolicy(services.purchases.state.value.fullAccess)
        if (s.queueIndex < queue.size - 1 && access.lesson(queue[s.queueIndex + 1])) {
            show(s.queueIndex + 1, 0)
        } else {
            _state.update { it.copy(finished = true) }
        }
    }

    fun previous() {
        val s = _state.value
        if (s.page == 0 || s.savePending) return
        services.play(Sound.POP)
        show(s.queueIndex, s.page - 1)
    }

    fun replay() {
        val s = _state.value
        if (!s.isQuiz) show(s.queueIndex, s.page)
    }

    fun pick(index: Int) {
        val s = _state.value
        if (!s.isQuiz || s.quizDone || s.choiceStates[index] == OptionState.WRONG) return
        val choice = s.choices[index]
        if (!choice.correct) {
            services.play(Sound.BAD)
            _state.value = s.copy(choiceStates = s.choiceStates + (index to OptionState.WRONG), showHint = true)
            return
        }
        services.play(Sound.OK)
        val more = s.queueIndex < queue.size - 1
        _state.value =
            s.copy(
                choiceStates = s.choiceStates + (index to OptionState.CORRECT),
                quizDone = true,
                savePending = true,
                saveFailed = false,
                showHint = false,
                praiseIndex = random.nextInt(PRAISE_COUNT),
                celebrate = s.celebrate + 1,
                nextAction =
                    when {
                        more -> NextAction.NEXT_LESSON
                        thenLevel != null -> NextAction.START_GAME
                        else -> NextAction.FINISH
                    },
            )
        viewModelScope.launch {
            try {
                services.repository.updateProgress { it.withLessonDone(s.lesson.id) }
                _state.update { it.copy(savePending = false) }
            } catch (_: IOException) {
                _state.update {
                    it.copy(
                        quizDone = false,
                        choiceStates = s.choiceStates,
                        savePending = false,
                        saveFailed = true,
                    )
                }
            }
        }
    }

    private companion object {
        const val PRAISE_COUNT = 7
    }
}
