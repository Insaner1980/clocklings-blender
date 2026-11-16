package fi.clocklings.ui.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fi.clocklings.R
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.PinnedPreview
import fi.clocklings.ui.components.SegmentedControl
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.theme.SkDimens

@Composable
internal fun AppearanceHeader(
    clocks: Boolean,
    onBack: () -> Unit,
    onTabChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        TopBar(stringResource(R.string.appearance_title), onBack)
        SegmentedControl(
            "",
            listOf(stringResource(R.string.companions_title), stringResource(R.string.clock_styles_title)),
            if (clocks) 1 else 0,
            { onTabChange(it == 1) },
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun PinnedClockPreview(
    clock: ClockAppearance,
    visible: Boolean,
    height: Dp,
    time: Int,
) {
    PinnedPreview(visible, height, stringResource(clockThemeName(clock.theme))) { size ->
        ClockFace(ClockCommand(time), Modifier.size(size), describeTime = false, appearance = clock)
    }
}
