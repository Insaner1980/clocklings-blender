package fi.clocklings.ui.home

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.ui.theme.SkDimens

/**
 * Sizes and positions on the stage. [hanger] is the top left corner of the wardrobe button inside the
 * content box, [contentTop] where that box starts in the stage, and [leftCloudY] the centre height of
 * the left cloud in the stage, or null when the cloud is left out.
 */
internal class CompanionStageLayout(
    val companion: DpSize,
    val hanger: DpOffset,
    val contentTop: Dp,
    val leftCloudY: Dp?,
)

/**
 * The companion stands bottom left of a content box [contentHeight] tall ([SkDimens.GapS] from the
 * edges) with the wardrobe button in the free sky directly above it, centred on it. The companion is
 * as tall as the box allows, keeping room above it for the button and its gaps, and at most
 * [COMPANION_WIDTH_SHARE] of the width, so the button never reaches the right column. On a stage
 * taller than the box, the group from the button to the companion's feet is centred in it.
 */
internal fun companionStageLayout(
    stageWidth: Dp,
    stageHeight: Dp,
    contentHeight: Dp,
    aspectRatio: Float,
): CompanionStageLayout {
    val height =
        min(
            min(contentHeight * COMPANION_HEIGHT_SHARE, stageWidth * COMPANION_WIDTH_SHARE / aspectRatio),
            contentHeight - (SkDimens.IconButton + SkDimens.GapS * HANGER_RESERVE_GAPS),
        )
    val width = height * aspectRatio
    val feet = contentHeight - SkDimens.GapS
    val hangerY = max(SkDimens.GapS, feet - height - SkDimens.GapS - SkDimens.IconButton)
    val contentTop = centredContentTop(stageHeight, contentHeight, hangerY, feet)
    return CompanionStageLayout(
        DpSize(width, height),
        DpOffset(max(SkDimens.GapS, SkDimens.GapS + width / 2 - SkDimens.IconButton / 2), hangerY),
        contentTop,
        skyCloudY(contentTop + hangerY, stageWidth * CLOUD_A_SIZE),
    )
}

/**
 * Where a content box [contentHeight] tall starts in a stage [stageHeight] tall so that its group
 * from [groupTop] to [groupBottom] (inside the box) is centred, without the box leaving the stage.
 */
internal fun centredContentTop(
    stageHeight: Dp,
    contentHeight: Dp,
    groupTop: Dp = 0.dp,
    groupBottom: Dp = contentHeight,
): Dp = ((stageHeight - groupTop - groupBottom) / 2).coerceIn(0.dp, stageHeight - contentHeight)

/**
 * What the scenery behind a stage needs: the content box from [contentTop], [contentHeight] tall, which
 * the ground stands under, and the centre heights of the clouds (null for a cloud left out).
 */
@Immutable
internal data class StageScene(
    val contentTop: Dp,
    val contentHeight: Dp,
    val leftCloudY: Dp?,
    val rightCloudY: Dp?,
)

/**
 * A measured content group [groupHeight] tall on a stage [stageWidth] by [stageHeight]: centred, so the
 * sky above it equals the room below it, with the clouds only in the free sky above it.
 */
internal fun groupStageScene(
    stageWidth: Dp,
    stageHeight: Dp,
    groupHeight: Dp,
): StageScene {
    val top = centredContentTop(stageHeight, min(groupHeight, stageHeight))
    val (left, right) = homeCloudYs(null, stageWidth, top, groupHeight)
    return StageScene(top, groupHeight, left, right)
}

/**
 * Centre heights of the left and right cloud on a stage [stageWidth] wide whose content box starts at
 * [contentTop], or null for a cloud left out. Each moves into the free sky above the content when it fits
 * there (the left one above the hanger of the companion [stage]). Otherwise only the companion stage keeps
 * the right cloud, behind the content; without it a cloud would peek out around the bubble.
 */
internal fun homeCloudYs(
    stage: CompanionStageLayout?,
    stageWidth: Dp,
    contentTop: Dp,
    contentHeight: Dp,
): Pair<Dp?, Dp?> {
    val left = if (stage != null) stage.leftCloudY else skyCloudY(contentTop, stageWidth * CLOUD_A_SIZE)
    val right =
        skyCloudY(contentTop, stageWidth * CLOUD_B_SIZE) ?: stage?.let { contentTop + contentHeight * CLOUD_B_Y }
    return left to right
}

/**
 * Centre height for a cloud [width] wide in the middle of the sky from the stage top to [skyBottom],
 * or null when it does not fit there with at least [SkDimens.GapS] above and below.
 */
internal fun skyCloudY(
    skyBottom: Dp,
    width: Dp,
): Dp? {
    val (above, below) = cloudReach(width)
    val free = skyBottom - above - below
    return if (free < SkDimens.GapS * 2) null else above + free / 2
}

/** How far a cloud [width] wide in the stage scenery reaches above and below its centre. */
internal fun cloudReach(width: Dp): Pair<Dp, Dp> =
    width * CLOUD_PUFF * CLOUD_TOP to width * CLOUD_PUFF * (2 * CLOUD_DROP + 1)

private const val COMPANION_HEIGHT_SHARE = 0.88f
internal const val COMPANION_WIDTH_SHARE = 0.52f

/** Gaps above the stage content, between the hanger and companion, and below the feet. */
internal const val HANGER_RESERVE_GAPS = 3

internal const val CLOUD_A_SIZE = 0.26f
private const val CLOUD_B_Y = 0.3f
internal const val CLOUD_B_SIZE = 0.18f
internal const val CLOUD_PUFF = 0.22f
internal const val CLOUD_TOP = 1.35f
internal const val CLOUD_DROP = 0.35f
