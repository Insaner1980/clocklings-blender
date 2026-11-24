package fi.clocklings.ui.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.domain.slots
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.RetryableSave
import fi.clocklings.ui.appearance.companionColorName
import fi.clocklings.ui.appearance.companionName
import fi.clocklings.ui.components.AdaptiveContent
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.LockPill
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.PillKind
import fi.clocklings.ui.components.PinnedPreview
import fi.clocklings.ui.components.PressSurface
import fi.clocklings.ui.components.PreviewScroll
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SelectionBadge
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SwatchTile
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.itemName
import fi.clocklings.ui.observeAccessPolicy
import fi.clocklings.ui.slotName
import fi.clocklings.ui.theme.PersonalizationColors
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/**
 * The selected companion's wardrobe: the big companion and its name, then (for animals) its color,
 * then its items slot by slot. Unlock texts count only this companion's items.
 */
@Composable
fun WardrobeScreen(
    kind: CompanionKind,
    progress: Progress,
    color: CompanionColor,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onParent: () -> Unit = {},
) {
    val app = LocalAppViewModel.current
    val access = app.observeAccessPolicy()
    val nameEditor = key(kind) { rememberTextFieldState(progress.nameFor(kind)) }
    val name = nameEditor.text.toString()
    val nameRetry = rememberNameSave(kind, nameEditor)
    val colorSave = remember(app, kind) { RetryableSave() }
    val itemSaves = remember(app, kind) { kind.slots.associateWith { RetryableSave() } }
    val retry = nameRetry ?: colorSave.retry ?: itemSaves.values.firstNotNullOfOrNull { it.retry }
    val total = progress.totalStars
    val outfit = access.displayedOutfit(kind, progress.outfitFor(kind), total)
    val navigation = rememberWardrobeSections(kind)
    val preview = remember(navigation) { PreviewScroll(navigation.scroll) }
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val failureBar: @Composable () -> Unit = { WardrobeSaveFailure(retry) }
    ScreenScaffold(
        modifier,
        wideContent = true,
        section = Section.COMPANION,
        scrollState = navigation.scroll,
        onViewportTop = {
            navigation.viewportTop = it
            preview.viewportTop = it
        },
        topBar = { WardrobeHeader(kind, total, access, navigation, onBack) },
        bottomBar = failureBar.takeIf { retry != null && !keyboardOpen },
        pinnedContent = { height ->
            PinnedPreview(preview.showCompact, height, name.ifBlank { stringResource(companionName(kind)) }) { size ->
                CompanionView(kind, outfit, Modifier.size(size), color = color, animated = false)
            }
        },
    ) { clockSize ->
        AdaptiveContent(clockSize, visual = {
            WardrobeStage(
                kind,
                outfit,
                color,
                nameEditor,
                nameError = if (nameRetry != null) stringResource(R.string.settings_save_error) else null,
                onPreviewPosition = preview::positionPreview,
            )
        }) {
            if (retry != null && keyboardOpen) failureBar()
            WardrobeIntro(kind, total, access, color, { navigation.headings[0] = it }) { chosen ->
                colorSave.run { app.setColor(kind, chosen, it) }
            }
            WardrobeItems(kind, total, outfit, color, access, navigation) { item ->
                itemSaves.getValue(item.slot).run { app.toggleItem(kind, item, it) }
            }
            if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent)
        }
    }
}

@Composable
private fun WardrobeItems(
    kind: CompanionKind,
    total: Int,
    outfit: Outfit,
    color: CompanionColor,
    access: AccessPolicy,
    navigation: WardrobeSections,
    onItemClick: (Item) -> Unit,
) {
    val app = LocalAppViewModel.current
    val firstItemSection = if (kind == CompanionKind.HUMAN) 0 else 1
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val (columns, nameStyle) = rememberItemGridLayout(Wardrobe.itemsFor(kind), maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen)) {
            for ((index, slot) in kind.slots.withIndex()) {
                val section = firstItemSection + index
                WardrobeSection(stringResource(slotName(slot)), { navigation.headings[section] = it }) {
                    ItemGrid(Wardrobe.itemsFor(kind, slot), columns) { item, cell ->
                        if (access.item(item, total)) {
                            val on = outfit.isOn(item)
                            ItemTile(item, outfit, color, cell, nameStyle = nameStyle, on = on, onClick = {
                                app.play(if (on) Sound.POP else Sound.EQUIP)
                                onItemClick(item)
                            })
                        } else {
                            LockedTile(item, outfit, color, total, cell, nameStyle, !access.itemContent(item))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WardrobeStage(
    kind: CompanionKind,
    outfit: Outfit,
    color: CompanionColor,
    nameEditor: TextFieldState,
    nameError: String?,
    onPreviewPosition: (LayoutCoordinates) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(SkColors.Surface, SkShapes.Tile)
            .padding(SkDimens.Gap),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            CompanionView(
                kind,
                outfit,
                Modifier
                    .onGloballyPositioned(onPreviewPosition)
                    .width(SkDimens.DollBigMax)
                    .aspectRatio(companionAspectRatio(kind)),
                color = color,
            )
            NameField(
                nameEditor,
                Modifier.semantics { if (nameError != null) error(nameError) },
            )
        }
    }
}

/**
 * One column count and item name size for every grid of the wardrobe, so all its sections match.
 * Three columns, two on narrow screens and with large text; a column is dropped only when the longest
 * word of any name, such as "Firefighter", does not fit even at the minimum size, so a name never
 * splits mid-word. The names then shrink as little as that column count needs.
 */
@Composable
private fun rememberItemGridLayout(
    items: List<Item>,
    width: Dp,
): Pair<Int, TextStyle> {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val names = items.map { stringResource(itemName(it.id)) }
    return remember(names, width, density) {
        val words = names.flatMap { it.split(WordBreak) }

        fun longestWordWidth(fontSize: Float) =
            words.maxOfOrNull { measurer.measure(it, SkType.ItemName.copy(fontSize = fontSize.sp)).size.width } ?: 0

        fun textWidth(count: Int) =
            with(density) {
                (width.roundToPx() - SkDimens.GapM.roundToPx() * (count - 1)) / count - SkDimens.GapS.roundToPx() * 2
            }
        val narrow = width < NarrowGridWidth || density.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        val minimumSize = SkType.ItemNameMinSize.value
        val columns =
            ((if (narrow) NARROW_GRID_COLUMNS else GRID_COLUMNS) downTo 1)
                .firstOrNull { longestWordWidth(minimumSize) <= textWidth(it) } ?: 1
        var fontSize = SkType.ItemName.fontSize.value
        while (fontSize > minimumSize && longestWordWidth(fontSize) > textWidth(columns)) {
            fontSize = (fontSize - SkType.ItemNameStep.value).coerceAtLeast(minimumSize)
        }
        columns to SkType.ItemName.copy(fontSize = fontSize.sp)
    }
}

/** Rows of [columns] equally wide and tall tiles. Empty places in the last row stay empty. */
@Composable
private fun ItemGrid(
    options: List<Item>,
    columns: Int,
    tile: @Composable (Item, Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        for (row in options.chunked(columns)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                modifier = Modifier.height(IntrinsicSize.Max),
            ) {
                row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Item previews are raised only when they can be selected. */
@Composable
fun ItemTile(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    nameStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        FlatPanel(modifier, padding = SkDimens.GapS) { ItemTileContent(item, outfit, color, on, nameStyle) }
    } else {
        val fill = SkColors.Surface
        PressSurface(
            onClick,
            fill,
            SkShapes.Item,
            modifier.semantics { selected = on },
            border = if (on) BorderStroke(SkDimens.SelectionBorder, SkColors.Primary) else null,
        ) {
            Box(Modifier.padding(SkDimens.GapS), contentAlignment = Alignment.TopCenter) {
                ItemTileContent(item, outfit, color, on, nameStyle)
            }
        }
    }
}

/** Centered like a locked tile; the check sits on the preview's corner, where a locked tile has its lock. */
@Composable
private fun ItemTileContent(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    on: Boolean,
    nameStyle: TextStyle?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        Box(Modifier.width(SkDimens.ItemWindow).height(SkDimens.SelectionBadge)) {
            if (on) SelectionBadge(Modifier.align(Alignment.TopEnd))
        }
        ItemArtWindow(item, outfit, color)

        val name = stringResource(itemName(item.id))
        if (nameStyle == null) {
            WordSafeText(name, SkType.ItemName)
        } else {
            WordSafeText(name, nameStyle, minScale = 1f)
        }
    }
}

/**
 * A locked item shows a faded, colourless preview of itself, so the child can see what the next stars
 * will bring, with a lock and progress toward its star threshold.
 */
@Composable
private fun LockedTile(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    total: Int,
    modifier: Modifier = Modifier,
    nameStyle: TextStyle = SkType.ItemName,
    requiresFullVersion: Boolean = false,
) {
    val needsStars = !requiresFullVersion && total < item.star
    val description =
        if (needsStars) pluralStringResource(R.plurals.item_star_need_description, item.star, item.star) else ""
    Column(
        modifier
            .background(SkColors.ItemLockedBg, SkShapes.Item)
            .semantics(mergeDescendants = true) { if (needsStars) contentDescription = description }
            .padding(TilePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        Box {
            ItemArtWindow(item, outfit, color, Modifier.graphicsLayer { alpha = LOCKED_ALPHA }.grayscale())
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(SkDimens.SelectionBadge)
                    .background(SkColors.Surface, SkShapes.Pill),
                contentAlignment = Alignment.Center,
            ) { SkIcon(SkIcons.Lock, SkDimens.StarSmall) }
        }
        // The name takes the spare height, so the progress pills of one row stay level when a
        // neighbouring name takes two lines.
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            WordSafeText(stringResource(itemName(item.id)), nameStyle, minScale = 1f)
        }
        if (needsStars) {
            Pill(
                stringResource(R.string.item_star_need, item.star),
                modifier = Modifier.clearAndSetSemantics { },
                kind = PillKind.MUTED,
                icon = SkIcons.StarOn,
            )
        }
        if (requiresFullVersion) LockPill(compact = true)
    }
}

private const val LOCKED_ALPHA = 0.55f

/** The existing locked-item frame preserves its thumbnail and caption clearance. */
private val TilePadding =
    PaddingValues(start = SkDimens.GapS, end = SkDimens.GapS, top = SkDimens.GapOptions, bottom = SkDimens.GapM)

/** Narrow wardrobes use two columns before testing long item names. */
private val NarrowGridWidth = 360.dp
private val WordBreak = Regex("(?<=-)|\\s+")

private const val GRID_COLUMNS = 3
private const val NARROW_GRID_COLUMNS = 2

@Composable
private fun CompanionColors(
    kind: CompanionKind,
    color: CompanionColor,
    onColorChange: (CompanionColor) -> Unit,
) {
    val app = LocalAppViewModel.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        kind.colorOptions().forEach { option ->
            SwatchTile(
                PersonalizationColors.companion(kind, option),
                stringResource(companionColorName(option)),
                option == color,
                {
                    onColorChange(option)
                    app.play(Sound.POP)
                },
                Modifier.testTag("color_${option.name}"),
            )
        }
    }
}

@Composable
private fun WardrobeIntro(
    kind: CompanionKind,
    total: Int,
    access: AccessPolicy,
    color: CompanionColor,
    onHeadingTop: (Float) -> Unit,
    onColorChange: (CompanionColor) -> Unit,
) {
    WardrobeHints(kind, total)
    if (!access.fullAccess) Text(stringResource(R.string.wardrobe_full_requirement), style = SkType.SmallSoft)
    if (kind != CompanionKind.HUMAN) {
        WardrobeSection(stringResource(R.string.companion_color), onHeadingTop) {
            CompanionColors(kind, color, onColorChange)
        }
    }
}
