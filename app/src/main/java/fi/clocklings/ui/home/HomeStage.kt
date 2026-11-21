package fi.clocklings.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.R
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.SpeechBubble
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel

/**
 * The companion stands on a soft ground and tells the time in a speech bubble beside the live
 * clock. Tapping the stage opens the companion choices. Without a companion the clock takes the
 * stage alone. The content is at most [SkDimens.HomeStageMaxHeight] tall; on a taller stage it is
 * centred within the rounded scenery; the animated artwork remains unclipped.
 */
@Composable
internal fun HomeStage(
    progress: Progress,
    settings: Settings,
    time: Int,
    compact: Boolean,
    onOpen: (HomeDestination) -> Unit,
    trimSky: Boolean,
) {
    val kind = settings.personalization.companion
    val description = "${stringResource(R.string.menu_nukke)}. ${stringResource(R.string.home_companion_choices)}"
    val words = timeLabel(LocalAppLanguage.current, time, settings.h24)
    var ground by remember(kind) { mutableStateOf<Offset?>(null) }
    if (kind == CompanionKind.NONE || compact) {
        GroupStage(description, onOpen, { ground }) { stageWidth, plannedHeight ->
            if (kind == CompanionKind.NONE) {
                ClockOnlyStage(words, time, stageWidth, plannedHeight, compact)
            } else {
                CompactCompanionStage(kind, progress, words, time, stageWidth, onOpen) { ground = it }
            }
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize().testTag("home_stage")) {
            val density = LocalDensity.current
            var wardrobeHeight by remember { mutableStateOf(SkDimens.WardrobeButtonHeight) }
            val contentHeight = min(maxHeight, SkDimens.HomeStageMaxHeight)
            val skyTrim = if (trimSky) SkDimens.HomeStageSkyTrim else 0.dp
            val stage =
                companionStageLayout(
                    maxWidth,
                    maxHeight,
                    contentHeight,
                    companionAspectRatio(kind),
                    wardrobeHeight,
                    skyTrim,
                )
            val (leftCloudY, rightCloudY) = homeCloudYs(stage, maxWidth, stage.contentTop, contentHeight)
            val scene = StageScene(stage.contentTop, contentHeight, leftCloudY, rightCloudY)
            Scenery({ ground }) { scene }
            StageSurface(
                description,
                onOpen,
                Modifier.offset(y = stage.contentTop).fillMaxWidth().height(contentHeight),
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    CompanionStage(kind, progress, words, time, maxWidth, maxHeight + skyTrim, stage) { ground = it }
                }
            }
            WardrobeButton(
                onOpen,
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(SkDimens.StageInset)
                    .widthIn(max = maxWidth * (1 - RIGHT_COLUMN_SHARE))
                    .onSizeChanged { wardrobeHeight = with(density) { it.height.toDp() } },
            )
        }
    }
}

/**
 * A stage around a measured content group: the clock alone, or the compact companion row below its
 * bubble. The group is centred (see [groupStageScene]); a stage measured without a height limit, as on
 * the scrolling compact page, is exactly as tall as the group, so no empty sky pushes the menu down.
 * [content] gets the stage width and the height it may plan its sizes for.
 */
@Composable
private fun GroupStage(
    description: String,
    onOpen: (HomeDestination) -> Unit,
    ground: () -> Offset?,
    content: @Composable (stageWidth: Dp, plannedHeight: Dp) -> Unit,
) {
    // Set in layout, where the group is measured, and read only while drawing the scenery behind it.
    var scene by remember { mutableStateOf<StageScene?>(null) }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home_stage"), propagateMinConstraints = true) {
        val stageWidth = maxWidth
        val cap = SkDimens.HomeStageMaxHeight
        val planned = if (constraints.hasBoundedHeight) min(maxHeight, cap) else cap
        Layout(
            contents =
                listOf(
                    { Scenery(ground) { scene } },
                    { StageSurface(description, onOpen, Modifier.fillMaxWidth()) { content(stageWidth, planned) } },
                ),
        ) { (sceneryM, surfaceM), stageConstraints ->
            val surface = surfaceM.single().measure(stageConstraints.copy(minHeight = 0))
            val height = stageConstraints.constrainHeight(surface.height)
            val placed = groupStageScene(stageWidth, height.toDp(), surface.height.toDp())
            scene = placed
            val scenery = sceneryM.single().measure(Constraints.fixed(stageConstraints.maxWidth, height))
            layout(stageConstraints.maxWidth, height) {
                scenery.place(0, 0)
                surface.place(0, placed.contentTop.roundToPx())
            }
        }
    }
}

/** The sky and ground behind the stage; only a backdrop, as the content box alone opens the companion choices. */
@Composable
private fun Scenery(
    ground: () -> Offset?,
    scene: () -> StageScene?,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(SkShapes.Stage)
            .background(SkColors.HomeSky)
            .drawBehind {
                val layout = scene() ?: return@drawBehind
                drawSkyAndGround(
                    layout.contentTop.toPx(),
                    layout.contentHeight.toPx(),
                    layout.leftCloudY?.toPx(),
                    layout.rightCloudY?.toPx(),
                    ground()?.minus(origin),
                )
            },
    )
}

/** The content box, which opens the companion choices. */
@Composable
private fun StageSurface(
    description: String,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .testTag("companion_stage")
            .semantics { contentDescription = description }
            .then(if (focused) Modifier.border(SkDimens.Border, SkColors.Ink, SkShapes.Stage) else Modifier)
            .clickable(interaction, indication = null, role = Role.Button) { onOpen(HomeDestination.NUKKE) },
        propagateMinConstraints = true,
    ) {
        Box(Modifier.matchParentSize().clip(SkShapes.Stage).indication(interaction, ripple(color = SkColors.Ink)))
        content()
    }
}

/**
 * Two clouds at the given centre heights (a cloud without a height is left out) and a gentle
 * hill for the companion and the clock to stand on. The hill's curved top stays at the bottom of the
 * content box, which starts at [contentTop]; below it the ground reaches down past the stage, behind
 * the menu.
 */
private fun DrawScope.drawSkyAndGround(
    contentTop: Float,
    contentHeight: Float,
    leftCloudY: Float?,
    rightCloudY: Float?,
    ground: Offset?,
) {
    if (leftCloudY != null) drawCloud(Offset(size.width * CLOUD_A_X, leftCloudY), size.width * CLOUD_A_SIZE)
    if (rightCloudY != null) drawCloud(Offset(size.width * CLOUD_B_X, rightCloudY), size.width * CLOUD_B_SIZE)
    val groundHeight = contentHeight * GROUND_HEIGHT
    val groundWidth = size.width * (1 + 2 * GROUND_OVERHANG)
    val groundTop =
        ground?.let {
            groundHorizon(it, size.width / 2, groundWidth / 2, groundHeight, SkDimens.StageGroundOverlap.toPx())
        } ?: (contentTop + contentHeight - groundHeight)
    drawRoundRect(
        SkColors.HomeGround,
        Offset(-size.width * GROUND_OVERHANG, groundTop),
        Size(groundWidth, maxOf(size.height - groundTop + groundHeight, groundHeight * 2)),
        CornerRadius(groundWidth / 2, groundHeight),
    )
}

/**
 * Without a companion the live clock takes the stage, with the time in words above it. The clock is
 * sized for a stage [stageHeight] tall; the group is only as tall as the bubble and the clock.
 */
@Composable
private fun ClockOnlyStage(
    words: String,
    time: Int,
    stageWidth: Dp,
    stageHeight: Dp,
    compact: Boolean,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = if (compact) SkDimens.GapS else 0.dp, bottom = SkDimens.GapM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        SpeechBubble(
            words,
            Modifier.widthIn(max = stageWidth).padding(end = SkDimens.Gap),
            style = SkType.NowWords,
            fill = false,
            color = SkColors.Surface,
        )
        StageClock(time, min(stageWidth, stageHeight * NONE_CLOCK_SHARE))
    }
}

/**
 * Narrow screens and large text: the bubble gets the full width on top, with the companion, the
 * wardrobe button and the clock side by side right below it. The row is as tall as its tallest item,
 * so no empty sky opens between the bubble and the companion.
 */
@Composable
private fun CompactCompanionStage(
    kind: CompanionKind,
    progress: Progress,
    words: String,
    time: Int,
    stageWidth: Dp,
    onOpen: (HomeDestination) -> Unit,
    onGroundPosition: (Offset) -> Unit,
) {
    val clockSize = min(stageWidth * CLOCK_WIDTH_SHARE, SkDimens.HomeStageMaxHeight)
    // The row never grows past the stage, so the companion leaves room for the button and the clock.
    val companionMaxWidth = max(stageWidth - SkDimens.GapS * 2 - clockSize - SkDimens.GapM, 0.dp)
    Column(
        Modifier.fillMaxWidth().padding(SkDimens.GapS),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        SpeechBubble(words, Modifier.padding(end = SkDimens.Gap), style = SkType.NowWords, color = SkColors.Surface)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CompanionView(
                kind,
                progress.outfitFor(kind),
                Modifier
                    .sharedClocklingsElement("companion")
                    .widthIn(max = companionMaxWidth)
                    .heightIn(max = SkDimens.HomeStageMaxHeight)
                    .aspectRatio(companionAspectRatio(kind), matchHeightConstraintsFirst = true),
                onGroundPosition = onGroundPosition,
            )
            StageClock(time, clockSize)
        }
        WardrobeButton(onOpen, Modifier.padding(start = SkDimens.GapS, bottom = SkDimens.GapS))
    }
}

/**
 * The companion stands on the left with the wardrobe button in the sky directly above it; the speech
 * bubble and the live clock stand to the right of both.
 */
@Composable
private fun BoxScope.CompanionStage(
    kind: CompanionKind,
    progress: Progress,
    words: String,
    time: Int,
    stageWidth: Dp,
    stageHeight: Dp,
    stage: CompanionStageLayout,
    onGroundPosition: (Offset) -> Unit,
) {
    val greeting = remember(kind) { CompanionCelebration(newOutfit = null, jump = true) }
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(start = SkDimens.GapS, bottom = stage.companionBottom)
            .sharedClocklingsElement("companion")
            .size(stage.companion),
    ) {
        CompanionView(
            kind,
            progress.outfitFor(kind),
            Modifier.fillMaxSize(),
            celebration = greeting,
            onGroundPosition = onGroundPosition,
        )
    }
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(stageWidth * RIGHT_COLUMN_SHARE)
            .padding(top = SkDimens.GapS, bottom = SkDimens.GapM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.Bottom),
    ) {
        SpeechBubble(
            words,
            Modifier.padding(end = SkDimens.Gap),
            style = SkType.NowWords,
            fill = false,
            color = SkColors.Surface,
        )
        StageClock(time, min(stageWidth * CLOCK_WIDTH_SHARE, stageHeight * CLOCK_HEIGHT_SHARE))
    }
}

@Composable
private fun StageClock(
    time: Int,
    size: Dp,
) {
    ClockFace(
        remember(time) { ClockCommand(time) },
        Modifier.sharedClocklingsElement("clock").size(size),
        mini = true,
    )
}

/** A soft three-puff cloud centred on [center], [width] wide. */
private fun DrawScope.drawCloud(
    center: Offset,
    width: Float,
) {
    val puff = width * CLOUD_PUFF
    drawCircle(SkColors.Cloud, puff, center + Offset(-width * CLOUD_SIDE, puff * CLOUD_DROP))
    drawCircle(SkColors.Cloud, puff * CLOUD_TOP, center)
    drawCircle(SkColors.Cloud, puff, center + Offset(width * CLOUD_SIDE, puff * CLOUD_DROP))
    drawRoundRect(
        SkColors.Cloud,
        topLeft = center + Offset(-width / 2, 0f),
        size = Size(width, puff * 2 * CLOUD_DROP + puff),
        cornerRadius = CornerRadius(puff),
    )
}

private const val GROUND_HEIGHT = 0.22f
private const val GROUND_OVERHANG = 0.15f
internal const val RIGHT_COLUMN_SHARE = 0.46f
private const val CLOCK_WIDTH_SHARE = 0.4f
private const val CLOCK_HEIGHT_SHARE = 0.52f
private const val NONE_CLOCK_SHARE = 0.6f
private const val CLOUD_A_X = 0.22f
private const val CLOUD_B_X = 0.7f
private const val CLOUD_SIDE = 0.24f
