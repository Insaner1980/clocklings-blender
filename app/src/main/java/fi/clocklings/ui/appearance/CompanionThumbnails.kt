package fi.clocklings.ui.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import fi.clocklings.R
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.components.PressSurface
import fi.clocklings.ui.components.SelectionBadge
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import kotlinx.coroutines.launch

internal fun companionThumbnailRowWidth(): Dp =
    SkDimens.CompanionThumbnail * CompanionKind.entries.size + SkDimens.GapS * (CompanionKind.entries.size - 1)

@Composable
internal fun CompanionThumbnails(
    pager: PagerState,
    kind: CompanionKind,
    progress: Progress,
    access: AccessPolicy,
    short: Boolean,
    previewTime: Int,
) {
    val scope = rememberCoroutineScope()
    val reduced = rememberReduceMotion()
    val goTo: suspend (Int) -> Unit = { if (reduced) pager.scrollToPage(it) else pager.animateScrollToPage(it) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val count = CompanionKind.entries.size
        val perRow = if (short || maxWidth >= companionThumbnailRowWidth()) count else (count + 1) / 2
        val thumb =
            ((maxWidth - SkDimens.GapS * (perRow - 1)) / perRow)
                .coerceIn(SkDimens.ThumbnailMin, SkDimens.CompanionThumbnail)
        val choices: @Composable () -> Unit = {
            CompanionKind.entries.forEach { option ->
                val locked = !access.companion(option, progress.totalStars)
                CompanionThumbnail(
                    option,
                    thumb,
                    selected = kind == option,
                    shown = pager.currentPage == option.ordinal,
                    lockLabel = if (locked) stringResource(companionRequirement(access)) else null,
                    progress = progress,
                    previewTime = previewTime,
                    onClick = { scope.launch { goTo(option.ordinal) } },
                )
            }
        }
        if (short) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = SkDimens.GapS),
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            ) { choices() }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = perRow,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) { choices() }
        }
    }
}

@Composable
private fun CompanionThumbnail(
    option: CompanionKind,
    thumb: Dp,
    selected: Boolean,
    shown: Boolean,
    lockLabel: String?,
    progress: Progress,
    previewTime: Int,
    onClick: () -> Unit,
) {
    val label = stringResource(companionName(option))
    PressSurface(
        onClick,
        SkColors.Surface,
        SkShapes.Pill,
        Modifier
            .size(thumb)
            .testTag("choose_companion_${option.name}")
            .semantics {
                contentDescription = label
                this.selected = selected
                if (lockLabel != null) stateDescription = lockLabel
            },
        border =
            if (shown) {
                BorderStroke(SkDimens.SelectionBorder, SkColors.Primary)
            } else {
                null
            },
        role = Role.RadioButton,
    ) {
        Box(Modifier.size(thumb), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(thumb * THUMB_ART)
                    .alpha(if (lockLabel == null) 1f else SkColors.DISABLED_CONTROL_ALPHA)
                    .clearAndSetSemantics { },
            ) {
                if (option == CompanionKind.NONE) {
                    ClockFace(ClockCommand(previewTime), mini = true)
                } else {
                    CompanionView(
                        option,
                        progress.outfitFor(option),
                        Modifier.matchParentSize(),
                        animated = false,
                    )
                }
            }
            if (lockLabel != null) {
                SkIcon(
                    SkIcons.Lock,
                    SkDimens.StarSmall,
                    Modifier.align(Alignment.TopEnd).padding(SkDimens.GapHalf).clearAndSetSemantics { },
                )
            }
            if (selected) SelectionBadge(Modifier.align(Alignment.BottomCenter))
        }
    }
}

private const val THUMB_ART = 0.78f
