package fi.clocklings.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Progress
import fi.clocklings.ui.components.PathRow
import fi.clocklings.ui.components.PathRowStyle
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.StarRow
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.levelName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resolve
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
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
        val ids = Lessons.levelLessons.flatten() + if (h24) listOf(Lessons.DAY_LESSON) else emptyList()
        val current = ids.firstOrNull { access.lesson(it) && it !in progress.lessonsDone }
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            for ((level, chapter) in ids.groupBy(Lessons::levelOf)) {
                val color = level?.let { SkColors.LevelCards[it] } ?: SkColors.ChallengeCard
                val chapterTitle = level?.let { stringResource(levelName(it)) }
                chapter.forEachIndexed { position, id ->
                    LessonListRow(
                        id,
                        ids.indexOf(id) + 1,
                        id == current,
                        position == 0,
                        chapterTitle,
                        color,
                        progress,
                        access,
                        onOpen,
                    )
                }
                if (level != null) {
                    LessonPracticeNode(
                        level,
                        progress.stars[level],
                        access.level(level),
                        { onPlayLevel(level) },
                    )
                }
            }
        }
        if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LessonListRow(
    id: String,
    number: Int,
    current: Boolean,
    isHeading: Boolean,
    chapterTitle: String?,
    color: androidx.compose.ui.graphics.Color,
    progress: Progress,
    access: AccessPolicy,
    onOpen: (String) -> Unit,
) {
    val lesson = LessonCatalog.lesson(id, LocalAppLanguage.current)
    val title = lesson.title.resolve()
    val completed = id in progress.lessonsDone
    val completion = if (completed) stringResource(R.string.lesson_done) else ""
    val locked = if (access.lesson(id)) "" else stringResource(R.string.full_version_label)
    // With no visible chapter headers, the chapter's first stop is its screen reader heading
    // and is announced with the chapter title first.

    val chapterPrefix = chapterTitle?.takeIf { isHeading && it != title }?.let { "$it. " }.orEmpty()
    PathRow(
        title,
        "$chapterPrefix$title. ${lesson.description.resolve()}. $completion $locked",
        { onOpen(id) },
        badgeColor = if (current) SkColors.Primary else color,
        completed = completed,
        style = if (current) PathRowStyle.CURRENT else PathRowStyle.STEP,
        startLabel = stringResource(R.string.lesson_start),
        locked = !access.lesson(id),
        isHeading = isHeading,
        enabled = access.lesson(id),
        leading = {
            Text(
                number.toString(),
                style =
                    if (current) {
                        SkType.LessonNumberCurrent.copy(color = SkColors.OnPrimary)
                    } else {
                        SkType.LessonNumber
                    },
            )
        },
    )
}

@Composable
private fun LessonPracticeNode(
    level: Int,
    stars: Int,
    enabled: Boolean,
    onPlay: () -> Unit,
) {
    val starDescription = pluralStringResource(R.plurals.stars_count, stars, stars)
    val chapterTitle = stringResource(levelName(level))
    val action =
        stringResource(
            when {
                !enabled -> R.string.full_version_label
                stars > 0 -> R.string.play_again
                else -> R.string.lesson_start_game
            },
        )
    PathRow(
        stringResource(R.string.path_practice_title, chapterTitle),
        if (enabled) "$chapterTitle. $action. $starDescription" else "$chapterTitle. $action",
        onPlay,
        style = PathRowStyle.PRACTICE,
        locked = !enabled,
        enabled = enabled,
        supporting = { WordSafeText(action, SkType.SmallSoft, textAlign = TextAlign.Start) },
        trailing = {
            StarRow(
                stars,
                starDescription,
                size = SkDimens.StarSmall,
            )
        },
        leading = { SkIcon(SkIcons.Star, SkDimens.Star) },
    )
}
