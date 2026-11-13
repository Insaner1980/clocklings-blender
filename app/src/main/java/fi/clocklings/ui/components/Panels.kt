package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fi.clocklings.R
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes

/** Informational content stays flat; depth is reserved for pressable controls. */
@Composable
fun FlatPanel(
    modifier: Modifier = Modifier,
    padding: Dp = SkDimens.PanelPadding,
    color: Color = SkColors.FlatSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(color, SkShapes.Card).padding(padding),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        content = content,
    )
}

/** Kello keskitettynä annettuun kokoon. */
@Composable
fun ClockBox(
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size)) { content() }
    }
}

/** Apu päällä / Apu pois -siru yläpalkkiin. */
@Composable
fun HelpToggle(
    help: Boolean,
    onToggle: () -> Unit,
) {
    RoundIconButton(
        if (help) SkIcons.HelpOn else SkIcons.Help,
        stringResource(if (help) R.string.help_on else R.string.help_off),
        onToggle,
        selected = help,
    )
}

@Composable
fun HelpTopBar(
    title: String,
    help: Boolean,
    onBack: () -> Unit,
    onToggleHelp: () -> Unit,
) {
    TopBar(
        title = title,
        onBack = onBack,
        trailing = { HelpToggle(help, onToggleHelp) },
    )
}
