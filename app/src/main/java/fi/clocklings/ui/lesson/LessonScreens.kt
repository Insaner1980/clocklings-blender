package fi.clocklings.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.audio.Sound
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Progress
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ActionPair
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.FeedbackKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.PathNode
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.PillKind
import fi.clocklings.ui.components.ProgressDots
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.StarRow
import fi.clocklings.ui.components.TaskActionFocus
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.rememberCelebrationAnchors
import fi.clocklings.ui.components.taskActionFocus
import fi.clocklings.ui.levelName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.praise
import fi.clocklings.ui.resolve
import fi.clocklings.ui.resources
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** Kellokoulu: oppituntien lista. 24 h -oppitunti näkyy vain 24 h -tilassa. */
@Composable
fun LessonListScreen(
    h24: Boolean,
    progress: Progress,
    access: AccessPolicy,
    onPlayLevel: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onParent: () -> Unit = {},
) {
    ScreenScaffold(
        modifier,
        section = Section.LEARN,
        topBar = { TopBar(stringResource(R.string.koulu_title), onBack) },
    ) {
        val language = LocalAppLanguage.current
        val ids = Lessons.levelLessons.flatten() + if (h24) listOf(Lessons.DAY_LESSON) else emptyList()
        val current = ids.firstOrNull { access.lesson(it) && it !in progress.lessonsDone }
        for ((level, chapter) in ids.groupBy(Lessons::levelOf)) {
            val color = level?.let { SkColors.LevelCards[it] } ?: SkColors.ChallengeCard
            val chapterTitle = level?.let { stringResource(levelName(it)) }
            chapter.forEachIndexed { position, id ->
                val lesson = LessonCatalog.lesson(id, language)
                val title = lesson.title.resolve()
                val completed = id in progress.lessonsDone
                val completion = if (completed) stringResource(R.string.lesson_done) else ""
                val locked = if (access.lesson(id)) "" else stringResource(R.string.full_version_label)
                // With no visible chapter headers, the chapter's first stop is its screen reader heading
                // and is announced with the chapter title first.
                val isHeading = position == 0
                val chapterPrefix = chapterTitle?.takeIf { isHeading && it != title }?.let { "$it. " }.orEmpty()
                PathNode(
                    ids.indexOf(id),
                    title,
                    "$chapterPrefix$title. ${lesson.description.resolve()}. $completion $locked",
                    { onOpen(id) },
                    color = color,
                    completed = completed,
                    current = id == current,
                    first = position == 0,
                    last = level == null,
                    isHeading = isHeading,
                    enabled = access.lesson(id),
                    badge = { if (locked.isNotEmpty()) Text(locked, style = SkType.Small) },
                )
            }
            if (level != null) {
                LessonPracticeNode(
                    ids.indexOf(chapter.last()) + 1,
                    level,
                    progress.stars[level],
                    access.level(level),
                    { onPlayLevel(level) },
                )
            }
        }
        if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent)
    }
}

@Composable
private fun LessonPracticeNode(
    index: Int,
    level: Int,
    stars: Int,
    enabled: Boolean,
    onPlay: () -> Unit,
) {
    val starDescription = pluralStringResource(R.plurals.stars_count, stars, stars)
    val practise = stringResource(R.string.lesson_practise)
    val chapterTitle = stringResource(levelName(level))
    val action =
        stringResource(
            when {
                !enabled -> R.string.full_version_label
                stars > 0 -> R.string.play_again
                else -> R.string.lesson_start_game
            },
        )
    PathNode(
        index,
        practise,
        "$chapterTitle. $practise. $action. $starDescription",
        onPlay,
        color = SkColors.LevelCards[level],
        completed = stars == 3,
        current = false,
        enabled = enabled,
        first = false,
        last = true,
        badge = {
            WordSafeText(action, SkType.Small, textAlign = TextAlign.Start)
            StarRow(stars, starDescription, size = SkDimens.StarSmall)
        },
        art = { SkIcon(SkIcons.Star, SkDimens.ChildTarget) },
    )
}

@Composable
fun LessonRoute(
    key: Screen.Lesson,
    speechAvailable: Boolean,
    onLeave: () -> Unit,
    onStartGame: (Int) -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: LessonViewModel =
        viewModel(
            factory = viewModelFactory { initializer { LessonViewModel(key.queue, key.thenLevel, app.services) } },
        ),
) {
    val language = LocalAppLanguage.current
    LaunchedEffect(language) { vm.setLanguage(language) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val leave = {
        if (key.thenLevel != null) onStartGame(key.thenLevel) else onLeave()
    }
    LaunchedEffect(state.finished) { if (state.finished) leave() }
    LessonScreen(
        state = state,
        skipToGame = key.thenLevel != null,
        speechAvailable = speechAvailable,
        onBack = {
            app.play(Sound.POP)
            leave()
        },
        onNext = vm::next,
        onPrevious = vm::previous,
        onReplay = vm::replay,
        onPick = { value ->
            val before = vm.state.value
            vm.pick(value)
            val after = vm.state.value
            if (after !==
                before
            ) {
                haptic.performHapticFeedback(
                    if (after.quizDone) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                )
            }
        },
        onSpeak = app::speak,
    )
}

@Composable
fun LessonScreen(
    state: LessonUiState,
    skipToGame: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onPick: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = resources()
    val anchors =
        rememberCelebrationAnchors(state.celebrate, QUIZ_CONFETTI) {
            state.choiceStates.entries
                .firstOrNull { it.value == OptionState.CORRECT }
                ?.key
        }
    val page = state.lesson.pages.getOrNull(state.page)
    val paragraphs = page?.text?.map { it.resolve(res) }.orEmpty()
    val caption = state.caption?.resolve(res).orEmpty()
    ClockTaskScaffold(
        modifier = modifier,
        section = Section.LEARN,
        topBar = {
            TopBar(
                title = state.lesson.title.resolve(),
                backLabel = stringResource(if (skipToGame) R.string.lesson_skip else R.string.back_description),
                onBack = onBack,
            )
        },
        actions = { focus, compact ->
            LessonActions(state, onPrevious, onNext, focus, compact) {
                if (page != null) LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay, compact)
            }
        },
        status = { ProgressDots(state.pageCount, state.page) },
        visual = { size ->
            ClockBox(size) {
                ClockFace(
                    command = state.clock,
                    period = state.period,
                    help = state.help,
                    describeTime = !state.isQuiz || state.quizDone,
                    highlight = state.highlight,
                    under = state.under,
                    over = state.over,
                )
            }
        },
    ) { compact ->
        FlatPanel(padding = if (compact) SkDimens.GapS else SkDimens.PanelPadding) {
            if (page != null) {
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) SkDimens.GapS else 10.dp)) {
                    paragraphs.forEach { Text(it, style = SkType.LessonText) }
                }
                if (caption.isNotEmpty()) LessonCaption(caption)
                if (!compact) LessonPlayback(speechAvailable, paragraphs, caption, onSpeak, onReplay, compact)
            } else {
                LessonQuiz(state, anchors, onPick)
            }
        }
    }
}

private fun nextLabel(action: NextAction): Int =
    when (action) {
        NextAction.NEXT_PAGE -> R.string.next
        NextAction.TRY_IT -> R.string.lesson_try
        NextAction.NEXT_LESSON -> R.string.lesson_next_lesson
        NextAction.START_GAME -> R.string.lesson_start_game
        NextAction.FINISH -> R.string.done
    }

@Composable
private fun LessonCaption(caption: String) {
    CompanionTip(caption)
}

private const val QUIZ_CONFETTI = 60

@Composable
private fun ColumnScope.LessonQuiz(
    state: LessonUiState,
    anchors: SnapshotStateMap<Int, Offset>,
    onPick: (Int) -> Unit,
) {
    val res = resources()
    Eyebrow(stringResource(R.string.lesson_try))
    Text(
        state.lesson.quiz.question
            .resolve(),
        style = SkType.Question,
    )
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
        state.choices.forEachIndexed { i, choice ->
            OptionButton(
                words = choice.text.resolve(),
                state = state.choiceStates[i] ?: OptionState.NORMAL,
                enabled = !state.quizDone && state.choiceStates[i] != OptionState.WRONG,
                onClick = { onPick(i) },
                modifier = Modifier.confettiAnchor { anchors[i] = it },
            )
        }
    }
    when {
        state.saveFailed && !state.showHint -> {
            CompanionTip(stringResource(R.string.settings_save_error), kind = FeedbackKind.HINT)
        }

        state.quizDone && !state.savePending -> {
            CompanionTip(
                stringResource(R.string.lesson_learned, praise(res, state.praiseIndex), state.lesson.title.resolve()),
                kind = FeedbackKind.OK,
                celebrate = true,
            )
        }

        state.showHint -> {
            CompanionTip(
                state.lesson.quiz.hint
                    .resolve(),
                kind = FeedbackKind.HINT,
            )
        }
    }
}

@Composable
private fun LessonPlayback(
    speechAvailable: Boolean,
    paragraphs: List<String>,
    caption: String,
    onSpeak: (String) -> Unit,
    onReplay: () -> Unit,
    compact: Boolean,
) {
    Row(
        if (compact) Modifier else Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            if (speechAvailable) {
                RoundIconButton(
                    SkIcons.Listen,
                    stringResource(R.string.listen),
                    { onSpeak((paragraphs + caption).filter { it.isNotEmpty() }.joinToString(" ")) },
                    large = !compact,
                )
            }
            RoundIconButton(
                SkIcons.Replay,
                stringResource(R.string.lesson_replay),
                onReplay,
                large = !compact,
            )
        }
    }
}

@Composable
private fun LessonActions(
    state: LessonUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    focus: TaskActionFocus,
    compact: Boolean,
    playback: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) playback()
        Box(Modifier.weight(1f)) { LessonNavigation(state, onPrevious, onNext, focus) }
    }
}

/** Navigation stays reachable while the lesson itself scrolls. */
@Composable
private fun LessonNavigation(
    state: LessonUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    focus: TaskActionFocus,
) {
    val previous = stringResource(R.string.lesson_prev)
    val next = stringResource(nextLabel(state.nextAction))
    val showNext = !state.savePending && (!state.isQuiz || state.quizDone)
    ActionPair(
        previous,
        next.takeIf { showNext },
        start = {
            SkButton(
                previous,
                onPrevious,
                it.taskActionFocus(focus),
                enabled =
                    state.page > 0 && !state.savePending,
            )
        },
        end = { SkButton(next, onNext, it.taskActionFocus(focus), kind = ButtonKind.PRIMARY) },
    )
}
