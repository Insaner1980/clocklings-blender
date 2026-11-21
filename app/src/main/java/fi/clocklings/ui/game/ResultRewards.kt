package fi.clocklings.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.companion.ItemArtWindow
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.itemName
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
internal fun ResultRewards(
    newItems: List<Item>,
    outfit: Outfit,
    color: CompanionColor,
    onDress: () -> Unit,
) {
    FlatPanel(padding = SkDimens.Gap) {
        if (newItems.size == 1) {
            val item = newItems.single()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                ItemArtWindow(item, outfit, color, displaySize = SkDimens.RewardArt)
                Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                    WordSafeText(
                        stringResource(itemName(item.id)),
                        SkType.SectionTitle,
                        textAlign = TextAlign.Start,
                    )
                    SkButton(
                        stringResource(R.string.dress_doll),
                        onDress,
                        kind = ButtonKind.CONTAINER,
                        small = true,
                        icon = SkIcons.Outfit,
                    )
                }
            }
        } else {
            ResultRewardItems(newItems, outfit, color)
            SkButton(
                stringResource(R.string.dress_doll),
                onDress,
                Modifier.align(Alignment.CenterHorizontally),
                kind = ButtonKind.CONTAINER,
                small = true,
                icon = SkIcons.Outfit,
            )
        }
    }
}

@Composable
private fun ResultRewardItems(
    items: List<Item>,
    outfit: Outfit,
    color: CompanionColor,
) {
    val names = items.map { stringResource(itemName(it.id)) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widestWord =
        remember(names, measurer, density) {
            names.maxOf { name ->
                name.split(Regex("\\s+")).maxOf { measurer.measure(it, SkType.ItemName, softWrap = false).size.width }
            }
        }
    val spacing = Arrangement.spacedBy(SkDimens.GapM, Alignment.CenterHorizontally)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rowCellPx =
            with(density) {
                val gapPx = SkDimens.GapM.roundToPx()
                // Divide whole pixels so the rounded cells and gaps fit the available width.
                val availablePx = (constraints.maxWidth - gapPx * (items.size - 1)).coerceAtLeast(0)
                minOf(SkDimens.RewardItemWidth.roundToPx(), availablePx / items.size)
            }
        val wrap = widestWord > rowCellPx
        val cellWidth =
            with(density) {
                if (wrap) {
                    maxOf(
                        SkDimens.RewardItemWidth,
                        widestWord.toDp(),
                    ).coerceAtMost(maxWidth)
                } else {
                    rowCellPx.toDp()
                }
            }
        val cells: @Composable () -> Unit = {
            for ((index, item) in items.withIndex()) {
                Column(
                    Modifier.width(cellWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapHalf),
                ) {
                    ItemArtWindow(item, outfit, color, displaySize = SkDimens.ResultRewardArt)
                    WordSafeText(names[index], SkType.ItemName, maxLines = 2)
                }
            }
        }
        if (wrap) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = spacing,
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                maxItemsInEachRow = 2,
            ) { cells() }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = spacing) { cells() }
        }
    }
}
