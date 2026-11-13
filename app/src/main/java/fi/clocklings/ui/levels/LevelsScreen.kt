package fi.clocklings.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import fi.clocklings.R
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.Levels
import fi.clocklings.domain.Progress
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.PathNode
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.StarRow
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.levelName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun LevelsScreen(
    progress: Progress,
    access: AccessPolicy,
    onPlay: (Int) -> Unit,
    onLesson: (Int) -> Unit,
    onChallenge: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onParent: () -> Unit = {},
) {
    ScreenScaffold(
        modifier,
        section = Section.PLAY,
        topBar = { TopBar(stringResource(R.string.menu_pelaa), onBack) },
    ) {
        val current = progress.stars.indexOfFirst { it < LEVEL_COMPLETE_STARS }
        for (level in Levels.all) {
            val i = level.index
            val title = stringResource(levelName(i))
            val example =
                stringResource(R.string.level_example, timeLanguage(LocalAppLanguage.current).timeWords(level.sample))
            val stars = pluralStringResource(R.plurals.stars_count, progress.stars[i], progress.stars[i])
            val completed = if (progress.stars[i] == LEVEL_COMPLETE_STARS) stringResource(R.string.lesson_done) else ""
            val locked = if (access.level(i)) "" else stringResource(R.string.full_version_label)
            PathNode(
                i,
                title,
                "$title. $example. $stars. $completed $locked",
                { onPlay(i) },
                color = SkColors.LevelCards[i],
                current = i == current && access.level(i),
                enabled = access.level(i),
                // Stars and the lesson shortcut share one row so the text block stays no taller than the node
                // and the trail never runs through the button.
                badge = {
                    if (locked.isNotEmpty()) Text(locked, style = SkType.Small)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                    ) {
                        StarRow(progress.stars[i], stars, size = SkDimens.StarSmall)
                        RoundIconButton(
                            SkIcons.Book,
                            "${stringResource(R.string.watch_lesson)}: $title",
                            { onLesson(i) },
                            enabled = access.level(i),
                        )
                    }
                },
                art = {
                    ClockFace(
                        ClockCommand(level.sample),
                        Modifier.sharedClocklingsElement("level-clock-$i").size(SkDimens.MiniClockLevel),
                        mini = true,
                    )
                },
            )
        }
        ChallengeNode(progress.challengeBest, access.challenge(), onChallenge)
        if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent)
    }
}

@Composable
private fun ChallengeNode(
    best: Int,
    enabled: Boolean,
    onChallenge: () -> Unit,
) {
    val title = stringResource(R.string.challenge_title)
    val description = stringResource(R.string.challenge_desc)
    val record = if (best > 0) stringResource(R.string.record, best) else ""
    val locked = if (enabled) "" else stringResource(R.string.full_version_label)
    PathNode(
        Levels.all.size,
        title,
        "$title. $description. $record $locked",
        onChallenge,
        color = SkColors.ChallengeCard,
        last = true,
        enabled = enabled,
        badge = {
            if (locked.isNotEmpty()) Text(locked, style = SkType.Small)
            if (record.isNotEmpty()) Text(record, style = SkType.Small)
        },
        art = { SkIcon(SkIcons.Star, SkDimens.ChildTarget) },
    )
}

private const val LEVEL_COMPLETE_STARS = 3
