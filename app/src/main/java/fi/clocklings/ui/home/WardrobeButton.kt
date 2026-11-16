package fi.clocklings.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fi.clocklings.R
import fi.clocklings.ui.components.PressSurface
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** A labelled wardrobe action, measured independently from the character artwork. */
@Composable
internal fun WardrobeButton(
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    PressSurface({ onOpen(HomeDestination.ASUT) }, SkColors.Surface, SkShapes.Pill, modifier) {
        Row(
            Modifier
                .heightIn(min = SkDimens.WardrobeButtonHeight)
                .padding(start = SkDimens.GapLabel, end = SkDimens.GapL, top = SkDimens.GapS, bottom = SkDimens.GapS),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapOptions),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkIcon(SkIcons.Outfit, SkDimens.ButtonIcon)
            WordSafeText(stringResource(R.string.doll_title), SkType.ButtonSmall, Modifier.weight(1f, fill = false))
        }
    }
}
