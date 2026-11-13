package fi.clocklings.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fi.clocklings.R
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.components.BigMenuButton
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.adaptiveGridColumns
import fi.clocklings.ui.components.hasSideBySideRoom
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import kotlinx.coroutines.delay

/**
 * Home fills the screen: a header row, a companion stage that takes all the free height, and the
 * activity grid at the bottom within thumb reach. Only when the header and grid leave less than
 * [SkDimens.StageMinHeight] does the page scroll. A compact page (narrow screens, large text) scrolls
 * in one column, so there the stage is no taller than its content unless free height is left.
 */
@Composable
fun HomeScreen(
    progress: Progress,
    settings: Settings,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberHomeTime()
    val time = if (settings.h24) now else now % ClockTime.HALF_DAY
    ScreenScaffold(modifier, section = Section.HOME, scroll = false, wideContent = true) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val viewport = maxHeight
            val compact = adaptiveGridColumns(maxWidth, LocalDensity.current.fontScale) == 1
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                FillHeightLayout(
                    viewport = viewport,
                    compact = compact,
                    header = { HomeHeader(progress, onOpen) },
                    stage = { HomeStage(progress, settings, time, compact, onOpen) },
                    footer = { Menu(onOpen) },
                )
            }
        }
    }
}

@Composable
internal fun rememberHomeTime(readTime: () -> Int = ClockTime::now): Int {
    var now by remember { mutableIntStateOf(readTime()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, readTime) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = readTime()
                delay(NOW_TICK_MS)
            }
        }
    }
    return now
}

/**
 * Header, flexible stage and footer: the stage gets whatever height is left in [viewport], but never
 * less than [SkDimens.StageMinHeight]. On a [compact] page the stage has no minimum and may grow to
 * the height its content needs.
 */
@Composable
private fun FillHeightLayout(
    viewport: Dp,
    compact: Boolean,
    header: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Layout(contents = listOf(header, stage, footer)) { (headerM, stageM, footerM), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val gap = SkDimens.Gap.roundToPx()
        val headerP = headerM.map { it.measure(loose) }
        val sideBySide = hasSideBySideRoom(width.toDp(), density.fontScale)
        val bodyWidth = if (sideBySide) (width - gap) / 2 else width
        val footerP = footerM.map { it.measure(Constraints.fixedWidth(bodyWidth)) }
        // The footer tiles' ledges draw below their bounds; reserve that room so scrolling never clips them.
        val headerHeight = headerP.sumOf { it.height } + gap
        val footerHeight = footerP.sumOf { it.height } + SkDimens.BigButtonDepth.roundToPx()
        val fixed = headerHeight + if (sideBySide) 0 else footerHeight + gap
        val stageMin = if (compact) 0 else SkDimens.StageMinHeight.roundToPx()
        val free =
            maxOf(
                with(density) { viewport.roundToPx() } - fixed,
                stageMin,
                if (sideBySide) footerHeight else 0,
            )
        val stageP =
            stageM.map {
                it.measure(
                    Constraints(bodyWidth, bodyWidth, free, if (compact) Constraints.Infinity else free),
                )
            }
        val stageHeight = stageP.maxOfOrNull { it.height } ?: free
        layout(width, fixed + stageHeight) {
            var y = 0
            headerP.forEach {
                it.place(0, y)
                y += it.height
            }
            y += gap
            stageP.forEach { it.place(0, y) }
            y += if (sideBySide) (stageHeight - footerHeight) / 2 else stageHeight + gap
            footerP.forEach { it.place(if (sideBySide) bodyWidth + gap else 0, y) }
        }
    }
}

@Composable
private fun HomeHeader(
    progress: Progress,
    onOpen: (HomeDestination) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        val title = stringResource(R.string.app_name)
        // Shrinks to fit on narrow screens and with large text instead of cutting the name off.
        BasicText(
            buildAnnotatedString {
                title.forEachIndexed { i, letter ->
                    withStyle(SpanStyle(color = SkColors.logoColor(i))) { append(letter) }
                }
            },
            style = SkType.HomeLogo,
            maxLines = 1,
            // The minimum is in dp so that large system text cannot push it past the space it has.
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = with(LocalDensity.current) { SkDimens.LogoMinSize.toSp() },
                    maxFontSize = SkType.HomeLogo.fontSize,
                ),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Pill(
            stringResource(R.string.home_star_total, progress.totalStars, Wardrobe.MAX_STARS),
            icon = SkIcons.StarOn,
            large = true,
        )
        RoundIconButton(
            SkIcons.Settings,
            stringResource(R.string.settings_title),
            { onOpen(HomeDestination.SETTINGS) },
        )
    }
}

@Composable
private fun Menu(onOpen: (HomeDestination) -> Unit) {
    val titles = listOf(R.string.menu_koulu, R.string.menu_vapaa, R.string.menu_pelaa, R.string.menu_kysy)
    val descriptions =
        listOf(R.string.menu_koulu_sub, R.string.menu_vapaa_sub, R.string.menu_pelaa_sub, R.string.menu_kysy_sub)
    val icons = listOf(SkIcons.Book, SkIcons.Clock, SkIcons.Star, SkIcons.Together)
    val fills = listOf(SkColors.KouluFill, SkColors.VapaaFill, SkColors.PelaaFill, SkColors.KysyFill)
    val shadows = listOf(SkColors.KouluShadow, SkColors.VapaaShadow, SkColors.PelaaShadow, SkColors.KysyShadow)
    val destinations = listOf(HomeDestination.KOULU, HomeDestination.VAPAA, HomeDestination.PELAA, HomeDestination.KYSY)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = adaptiveGridColumns(maxWidth, LocalDensity.current.fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            titles.indices.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                    row.forEach { i ->
                        BigMenuButton(
                            stringResource(titles[i]),
                            stringResource(descriptions[i]),
                            icons[i],
                            fills[i],
                            shadows[i],
                            { onOpen(destinations[i]) },
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

private const val NOW_TICK_MS = 10_000L
