package fi.clocklings.ui.freeclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Levels
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.AdaptiveContent
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.HelpTopBar
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TimeReadout
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import kotlin.random.Random

/** Vapaa kello: raahattava kello, aika sanoina ja numeroina, Kuuntele ja Arvo aika. */
@Composable
fun FreeClockScreen(
    h24: Boolean,
    help: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val period = ClockTime.period(h24)
    var time by rememberSaveable { mutableIntStateOf(ClockTime.start(h24)) }
    var previousH24 by rememberSaveable { mutableStateOf(h24) }
    var command by remember { mutableStateOf(ClockCommand(time)) }
    // 24 h -tilassa aloitetaan iltapäivästä (15.00) eikä yöstä (3.00), kuten webissä.
    LaunchedEffect(h24) {
        if (previousH24 == h24) return@LaunchedEffect
        previousH24 = h24
        val adjusted = if (h24 && time < ClockTime.HALF_DAY) time + ClockTime.HALF_DAY else ClockTime.mod(time, period)
        if (adjusted != time) {
            time = adjusted
            command = ClockCommand(adjusted)
        }
    }
    ScreenScaffold(
        modifier,
        wideContent = true,
        centerContent = true,
        section = Section.EXPLORE,
        bottomBar = {
            SkButton(stringResource(R.string.random_time), {
                val t = Levels.randomClockTime(period, time, Random.Default)
                time = t
                command = ClockCommand(t, RANDOM_MOVE_MS)
                app.play(Sound.POP)
            }, Modifier.fillMaxWidth(), kind = ButtonKind.PRIMARY, icon = SkIcons.Shuffle)
        },
        topBar = {
            HelpTopBar(
                title = stringResource(R.string.free_title),
                help = help,
                onBack = onBack,
                onToggleHelp = app::toggleHelp,
            )
        },
    ) { clockSize ->
        AdaptiveContent(clockSize, controlsAlignment = Alignment.CenterVertically, visual = { size ->
            ClockFace(
                command = command,
                modifier = Modifier.sharedClocklingsElement("clock"),
                period = period,
                step = STEP,
                interactive = true,
                help = help,
                onChange = { time = it },
                clockSize = size,
            )
        }) {
            ExplorePanel(time, h24, speechAvailable)
        }
    }
}

/** The time in words and digits, a listen button beside it, a random-time button and the hand legend. */
@Composable
private fun ExplorePanel(
    time: Int,
    h24: Boolean,
    speechAvailable: Boolean,
) {
    val app = LocalAppViewModel.current
    FlatPanel {
        TimeReadout(time, h24, speechAvailable, app::speak)
        HandLegend()
    }
}

/** The companion points out which hand is which, with a coloured sample of each hand. */
@Composable
private fun HandLegend() {
    val hour = stringResource(R.string.legend_hour)
    val minute = stringResource(R.string.legend_minute)
    val drag = stringResource(R.string.legend_drag)
    CompanionTip(
        fallback = {
            FlatPanel(color = SkColors.Bubble) { LegendRows(hour, minute, drag) }
        },
    ) {
        LegendRows(hour, minute, drag)
    }
}

@Composable
private fun ColumnScope.LegendRows(
    hour: String,
    minute: String,
    drag: String,
) {
    LegendRow(SkColors.Hour, SkDimens.LegendSwatchShort, hour)
    LegendRow(SkColors.Minute, SkDimens.LegendSwatchLong, minute)
    Text(drag, style = SkType.SmallSoft.copy(color = SkColors.InkSoft))
}

@Composable
private fun LegendRow(
    color: Color,
    length: Dp,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Box(Modifier.width(SkDimens.LegendSwatchLong), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.size(length, SkDimens.LegendSwatchHeight).background(color, SkShapes.Pill))
        }
        Text(text, style = SkType.Small, modifier = Modifier.weight(1f))
    }
}

private const val STEP = 5
private const val RANDOM_MOVE_MS = 900
