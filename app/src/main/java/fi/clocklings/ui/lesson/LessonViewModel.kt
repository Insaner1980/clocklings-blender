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
    val demoFinished: Boolean = true,
    val demoPlayedInstantly: Boolean = false,
) {
    val isQuiz: Boolean get() = page >= lesson.pages.size
    internal val awaitingAnswer: Boolean get() = isQuiz && !quizDone
    val pageCount: Int get() = lesson.pages.size + 1
    val visibleParagraphs: List<Text>
        get() {
            val paragraphs =
                lesson.pages
                    .getOrNull(page)
                    ?.text
                    .orEmpty()
            return if (!demoFinished && paragraphs.size > 1) paragraphs.dropLast(1) else paragraphs
        }
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

    private val _state =
        MutableStateFlow(lessonPageState(LessonCatalog.lesson(queue.first(), language), 0, 0, 0, random))
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var demoJob: Job? = null
    private var reduceMotion = false

    init {
        services.countLesson(_state.value.lesson.id, "start")
        startDemo()
    }

    private fun show(
        queueIndex: Int,
        page: Int,
        immediate: Boolean = reduceMotion,
    ) {
        services.stopSpeech()
        demoJob?.cancel()
        val lesson = LessonCatalog.lesson(queue[queueIndex], language)
        _state.value = lessonPageState(lesson, queueIndex, page, _state.value.celebrate, random)
        startDemo(immediate)
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

    fun setReduceMotion(value: Boolean) {
        if (reduceMotion == value) return
        reduceMotion = value
        if (value && !_state.value.demoFinished) {
            demoJob?.cancel()
            startDemo()
        }
    }

    private fun startDemo(immediate: Boolean = reduceMotion) {
        val s = _state.value
        if (s.isQuiz) return
        val steps = s.lesson.pages[s.page].demo
        if (immediate) {
            val settled = steps.fold(s) { state, step -> state.withDemoStep(step, animate = false) }
            _state.value = settled.copy(demoFinished = true, demoPlayedInstantly = true)
            return
        }
        demoJob =
            viewModelScope.launch {
                for (step in steps) {
                    if (step is DemoStep.Wait) {
                        delay(step.ms)
                    } else {
                        _state.update { it.withDemoStep(step, animate = true) }
                    }
                }
                _state.update { it.copy(demoFinished = true) }
            }
    }

    fun next() {
        val s = _state.value
        if (s.finished || s.savePending || s.awaitingAnswer) return
        if (!s.isQuiz) {
            services.play(Sound.POP)
            show(s.queueIndex, s.page + 1)
            return
        }
        // Leaving a finished quiz completes the lesson.
        services.countLesson(_state.value.lesson.id, "finish")
        services.play(Sound.LESSON_COMPLETE)
        val access = AccessPolicy(services.purchases.state.value.fullAccess)
        if (s.queueIndex < queue.size - 1 && access.lesson(queue[s.queueIndex + 1])) {
            show(s.queueIndex + 1, 0)
            services.countLesson(_state.value.lesson.id, "start")
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
        if (!s.isQuiz) {
            services.countLesson(_state.value.lesson.id, "replay")
            show(s.queueIndex, s.page, immediate = true)
        }
    }

    fun pick(index: Int) {
        val s = _state.value
        if (!s.awaitingAnswer || s.savePending || s.choiceStates[index] == OptionState.WRONG) return
        val choice = s.choices[index]
        if (!choice.correct) {
            services.countLesson(_state.value.lesson.id, "wrong")
            services.play(Sound.BAD)
            _state.value = s.copy(choiceStates = s.choiceStates + (index to OptionState.WRONG), showHint = true)
            return
        }
        saveCorrectChoice(s, index)
    }

    fun retrySave() {
        val s = _state.value
        if (!s.isQuiz || !s.saveFailed || s.savePending) return
        saveCorrectChoice(s, s.choices.indexOfFirst { it.correct })
    }

    private fun saveCorrectChoice(
        s: LessonUiState,
        index: Int,
    ) {
        if (!s.saveFailed) services.countLesson(_state.value.lesson.id, "correct")
        services.play(Sound.OK)
        val more = s.queueIndex < queue.size - 1
        _state.value =
            s.copy(
                choiceStates = s.choiceStates + (index to OptionState.CORRECT),
                quizDone = true,
                savePending = true,
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
                _state.update { it.copy(savePending = false, saveFailed = false) }
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

private fun AppServices.countLesson(
    lessonId: String,
    event: String,
) {
    val index = LessonCatalog.all(AppLanguage.ENGLISH).indexOfFirst { it.id == lessonId }
    usage.count("lesson.$index.$event")
}

private fun lessonPageState(
    lesson: Lesson,
    queueIndex: Int,
    page: Int,
    celebrate: Int,
    random: Random,
): LessonUiState {
    val base = LessonUiState(lesson = lesson, queueIndex = queueIndex, page = page, celebrate = celebrate)
    return if (page < lesson.pages.size) {
        val p = lesson.pages[page]
        base.copy(
            clock = ClockCommand(p.time),
            period = p.period,
            help = p.help,
            under = p.under,
            demoFinished = false,
            nextAction = if (page == lesson.pages.size - 1) NextAction.TRY_IT else NextAction.NEXT_PAGE,
        )
    } else {
        val q = lesson.quiz
        base.copy(
            clock = ClockCommand(q.time),
            period = q.period,
            under = q.under,
            choices = q.choices.shuffled(random),
        )
    }
}

private fun LessonUiState.withDemoStep(
    step: DemoStep,
    animate: Boolean,
): LessonUiState =
    when (step) {
        is DemoStep.Wait -> this
        is DemoStep.Move -> copy(clock = ClockCommand(step.to, if (animate) step.ms else 0, step.exact))
        is DemoStep.Highlight -> copy(highlight = step.hand)
        is DemoStep.Under -> copy(under = step.marks)
        is DemoStep.Over -> copy(over = step.marks)
        is DemoStep.Say -> copy(caption = step.text)
    }
