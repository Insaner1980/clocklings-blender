package fi.clocklings.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import fi.clocklings.ui.components.PathRow
import fi.clocklings.ui.components.PathRowStyle
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
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            for (level in Levels.all) {
                LevelRow(level.index, current, progress, access, onPlay, onLesson)
            }
            ChallengeNode(progress.challengeBest, access.challenge(), onChallenge)
        }
        if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LevelRow(
    i: Int,
    current: Int,
    progress: Progress,
    access: AccessPolicy,
    onPlay: (Int) -> Unit,
    onLesson: (Int) -> Unit,
) {
    val level = Levels.all[i]
    val title = stringResource(levelName(i))
    val example =
        stringResource(
            R.string.level_example,
            timeLanguage(LocalAppLanguage.current).timeWords(level.sample),
        )
    val stars = pluralStringResource(R.plurals.stars_count, progress.stars[i], progress.stars[i])
    val completed =
        if (progress.stars[i] ==
            LEVEL_COMPLETE_STARS
        ) {
            stringResource(R.string.lesson_done)
        } else {
            ""
        }
    val enabled = access.level(i)
    val description =
        if (enabled) {
            "$title. $example. $stars. $completed"
        } else {
            "$title. $example. " + stringResource(R.string.full_version_label)
        }
    PathRow(
        title,
        description,
        { onPlay(i) },
        badgeColor = SkColors.LevelCards[i],
        style = if (i == current && access.level(i)) PathRowStyle.CURRENT else PathRowStyle.STEP,
        startLabel = stringResource(R.string.lesson_start),
        locked = !enabled,
        enabled = enabled,
        supporting = {
            StarRow(progress.stars[i], stars, size = SkDimens.StarSmall)
        },
        trailing = {
            RoundIconButton(SkIcons.Book, "${stringResource(R.string.watch_lesson)}: $title", {
                onLesson(i)
            }, enabled = access.level(i), tonal = true)
        },
        leadingAboveAtLargeFont = true,
        leading = {
            ClockFace(
                ClockCommand(level.sample),
                Modifier
                    .padding(
                        SkDimens.GapXS,
                    ).sharedClocklingsElement("level-clock-$i")
                    .size(SkDimens.MiniClockLevel),
                mini = true,
            )
        },
    )
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
    PathRow(
        title,
        "$title. $description. $record $locked",
        onChallenge,
        badgeColor = SkColors.ChallengeCard,
        locked = !enabled,
        enabled = enabled,
        supporting = {
            if (record.isNotEmpty()) Text(record, style = SkType.Small)
        },
        leadingAboveAtLargeFont = true,
        leading = { SkIcon(SkIcons.Star, SkDimens.LargeActionIcon) },
    )
}

private const val LEVEL_COMPLETE_STARS = 3
