package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import fi.clocklings.R
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.sectionBackground

/** A visible, accessible state while the first local read has not returned. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().sectionBackground(Section.HOME).safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Icon navigation with a separate title row when the available space is narrow. */
@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = stringResource(R.string.back_description),
    compactTaskTitle: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val currentTrailing by rememberUpdatedState(trailing)
    val trailingContent = remember { movableContentOf { currentTrailing?.invoke() } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (!compactTaskTitle && maxWidth / LocalDensity.current.fontScale < SkDimens.GridBreakpoint) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                ) {
                    RoundIconButton(SkIcons.Back, backLabel, onBack)
                    trailingContent()
                }
                WordSafeText(
                    title,
                    SkType.Title,
                    Modifier.fillMaxWidth().semantics { heading() },
                    textAlign = TextAlign.Start,
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = SkDimens.ButtonHeightSmall),
                verticalAlignment = if (compactTaskTitle) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                RoundIconButton(SkIcons.Back, backLabel, onBack)
                WordSafeText(
                    title,
                    if (compactTaskTitle) SkType.TaskTitleLarge else SkType.Title,
                    Modifier.weight(1f).semantics { heading() },
                    textAlign = TextAlign.Start,
                    preferredMaxLines = if (compactTaskTitle) 2 else Int.MAX_VALUE,
                )
                trailingContent()
            }
        }
    }
}
