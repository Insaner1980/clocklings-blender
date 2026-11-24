package fi.clocklings.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.sectionBackground

/**
 * Näkymän runko: reunasta reunaan, turva-alueet huomioiden, 16 dp:n sivumarginaali.
 * [topBar] stays pinned in normal-height windows; short windows scroll it with the content.
 * [bottomBar] stays outside the scrolling area so task actions remain available.
 * Under pinned chrome, scrolled content fades out under the bar
 * instead of being cut off. With [scroll] false the content gets the exact remaining height and
 * arranges its own overflow. [onViewportTop] reports the visible scroll edge below any top fade.
 * The clock size is capped by the content width and [clockHeightShare] of its height.
 * [clockFit] can also cap it so that the whole content fits without scrolling (see [ClockFit]). With
 * [animateClock] size changes are animated. A new [scrollState] opens the content at the top.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    section: Section = Section.HOME,
    topBar: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    clockHeightShare: Float = SkDimens.CLOCK_HEIGHT_SHARE,
    clockFit: ClockFit = ClockFit.NONE,
    fitMin: Dp = SkDimens.FitClockMin,
    animateClock: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    wideContent: Boolean = false,
    centerContent: Boolean = false,
    onViewportTop: ((Float) -> Unit)? = null,
    pinnedContent: (@Composable (windowHeight: Dp) -> Unit)? = null,
    stackedClock: (@Composable (Dp) -> Unit)? = null,
    stackedStatus: (@Composable () -> Unit)? = null,
    stackedClockMax: Dp = SkDimens.ClockMax,
    clockTaskKey: Int? = null,
    content: @Composable ScreenScaffoldScope.(clockSize: Dp) -> Unit,
) {
    // Zero initial Compose insets would clamp a restored offset before the system bars arrive.
    if (!restoredScrollInsetsReady(scrollState)) return
    val contentWidth = if (wideContent) SkDimens.WideContentWidth else SkDimens.ReadingWidth
    ScaffoldFrame(
        section,
        contentWidth,
        scroll && stackedClock == null,
        scrollState,
        onViewportTop,
        topBar,
        pinnedContent,
        bottomBar,
        modifier,
    ) { width, height, scrollChrome ->
        if (stackedClock != null) {
            StackedClockBody(
                section,
                scrollState,
                bottomBar != null,
                clockHeightShare,
                clockFit,
                fitMin,
                animateClock,
                centerContent,
                stackedClock,
                stackedStatus,
                stackedClockMax,
                clockTaskKey,
                content,
            )
        } else {
            ScaffoldBody(
                onViewportTop = onViewportTop,
                availableWidth = width,
                viewportHeight = height,
                wideContent = wideContent,
                centerContent = centerContent,
                scrollChrome = scrollChrome,
                scrolling = scroll && !scrollChrome,
                scrollState = scrollState,
                section = section,
                hasBottomBar = bottomBar != null,
                clockHeightShare = clockHeightShare,
                clockFit = clockFit,
                fitMin = fitMin,
                animateClock = animateClock,
                clockTaskKey = clockTaskKey,
                content = content,
            )
        }
    }
}

@Composable
private fun ScaffoldFrame(
    section: Section,
    contentWidth: Dp,
    allowScrollingChrome: Boolean,
    scrollState: ScrollState,
    onViewportTop: ((Float) -> Unit)?,
    topBar: (@Composable () -> Unit)?,
    pinnedContent: (@Composable (Dp) -> Unit)?,
    bottomBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (width: Dp, height: Dp, scrollChrome: Boolean) -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .sectionBackground(section)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .imePadding(),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val windowHeight = maxHeight
                // Let the header scroll when fixed bars would crowd out the task or the open IME.
                val scrollChrome = allowScrollingChrome && windowHeight < SkDimens.PinnedChromeMinHeight
                val pinnedHeader = pinnedContent.takeUnless { scrollChrome }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (scrollChrome && pinnedContent != null) {
                        Box(Modifier.widthIn(max = contentWidth).padding(horizontal = SkDimens.Gutter)) {
                            pinnedContent(windowHeight)
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().scrollViewport(scrollChrome, scrollState, onViewportTop),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ScaffoldTopBar(section, contentWidth, windowHeight, topBar, pinnedHeader)
                        BoxWithConstraints(
                            Modifier
                                .widthIn(max = contentWidth)
                                .fillMaxWidth()
                                .then(if (scrollChrome) Modifier else Modifier.weight(1f)),
                        ) {
                            content(
                                maxWidth - SkDimens.Gutter * 2,
                                if (scrollChrome) windowHeight else maxHeight,
                                scrollChrome,
                            )
                        }
                    }
                }
            }
            ScaffoldBottomBar(section, bottomBar, contentWidth)
        }
    }
}

@Composable
private fun ScaffoldTopBar(
    section: Section,
    contentWidth: Dp,
    windowHeight: Dp,
    topBar: (@Composable () -> Unit)?,
    pinnedContent: (@Composable (Dp) -> Unit)?,
) {
    if (topBar == null && pinnedContent == null) return
    Box(
        Modifier.fillMaxWidth().background(section.color),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = contentWidth)
                .fillMaxWidth()
                .padding(horizontal = SkDimens.Gutter, vertical = SkDimens.GapS),
        ) {
            topBar?.invoke()
            pinnedContent?.invoke(windowHeight)
        }
    }
}

@Composable
private fun ScaffoldBody(
    onViewportTop: ((Float) -> Unit)?,
    availableWidth: Dp,
    viewportHeight: Dp,
    wideContent: Boolean,
    centerContent: Boolean,
    scrollChrome: Boolean,
    scrolling: Boolean,
    scrollState: ScrollState,
    section: Section,
    hasBottomBar: Boolean,
    clockHeightShare: Float,
    clockFit: ClockFit,
    fitMin: Dp,
    animateClock: Boolean,
    clockTaskKey: Int?,
    content: @Composable ScreenScaffoldScope.(Dp) -> Unit,
) {
    var restHeight by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val sideBySide = wideContent && hasSideBySideRoom(availableWidth, density.fontScale)
    val clockWidth = if (sideBySide) visualPaneWidth(availableWidth) else availableWidth
    val height =
        if (sideBySide && !scrollChrome) {
            viewportHeight - SkDimens.IconButton
        } else {
            viewportHeight * clockHeightShare
        }
    val compactClock = sideBySide || scrollChrome
    val clockHeight = if (compactClock) max(SkDimens.FitClockMin, height) else max(0.dp, height)
    val base = min(min(clockWidth, SkDimens.ClockMax), clockHeight)
    val fit = if (compactClock) ClockFit.NONE else clockFit
    val rest = restHeight?.let { with(density) { it.toDp() } }
    val clockSize =
        min(clockWidth, scaffoldClockSize(base, viewportHeight, fit, rest, animateClock, fitMin, clockTaskKey))
    val bottomInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
    val bottom = with(density) { bottomInsets.getBottom(this).toDp() }
    Box(Modifier.fillMaxSize()) {
        ScaffoldColumn(
            onViewportTop = onViewportTop,
            scrollState = scrollState,
            scrolling = scrolling,
            scrollChrome = scrollChrome,
            centerContent = centerContent && sideBySide,
            bottomInset = if (hasBottomBar) 0.dp else bottom,
            bottomPadding = if (hasBottomBar) SkDimens.GapM else bottom + SkDimens.GapS,
            measure = fit == ClockFit.FIT,
            clockSize = clockSize,
            onRestHeight = { restHeight = it },
        ) {
            content(ScreenScaffoldScope(this, viewportHeight), clockSize)
        }
        ScaffoldFades(section, scrollState, scrolling, hasBottomBar, bottom)
    }
}

@Composable
private fun StackedClockBody(
    section: Section,
    scrollState: ScrollState,
    hasBottomBar: Boolean,
    clockHeightShare: Float,
    clockFit: ClockFit,
    fitMin: Dp,
    animateClock: Boolean,
    centerContent: Boolean,
    clock: @Composable (Dp) -> Unit,
    status: (@Composable () -> Unit)?,
    clockMax: Dp,
    clockTaskKey: Int?,
    content: @Composable ScreenScaffoldScope.(Dp) -> Unit,
) {
    val density = LocalDensity.current
    val bottomInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
    val bottom = if (hasBottomBar) 0.dp else with(density) { bottomInsets.getBottom(this).toDp() }
    val bottomPadding = if (hasBottomBar) SkDimens.GapM else bottom + SkDimens.GapS
    Column(Modifier.fillMaxSize().padding(top = SkDimens.GapScreen)) {
        if (status != null) {
            Box(Modifier.padding(horizontal = SkDimens.Gutter).padding(bottom = SkDimens.GapScreen)) { status() }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            val base = min(min(maxWidth - SkDimens.Gutter * 2, clockMax), viewport * clockHeightShare)
            var panelHeight by remember(clockTaskKey) { mutableStateOf<Int?>(null) }
            var visualExtra by remember { mutableIntStateOf(0) }
            val rest = panelHeight?.let { with(density) { (it + visualExtra).toDp() } }
            val size = scaffoldClockSize(base, viewport, clockFit, rest, animateClock, fitMin, clockTaskKey)
            val topSpace = if (centerContent && rest != null) max(0.dp, (viewport - size - rest) / 2) else 0.dp
            Column(Modifier.fillMaxSize().padding(top = topSpace)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SkDimens.Gutter)
                        .testTag("adaptive_visual")
                        .measureRestHeight(size) { visualExtra = it },
                ) {
                    clock(size)
                }
                Box(Modifier.weight(1f).fillMaxWidth().testTag("adaptive_controls")) {
                    ScaffoldColumn(
                        onViewportTop = null,
                        scrollState = scrollState,
                        scrolling = true,
                        scrollChrome = false,
                        centerContent = false,
                        bottomInset = bottom,
                        bottomPadding = bottomPadding,
                        measure = clockFit == ClockFit.FIT || centerContent,
                        clockSize = 0.dp,
                        onRestHeight = { panelHeight = it },
                        topPadding = SkDimens.Gap,
                    ) { content(ScreenScaffoldScope(this, viewport), size) }
                    ScaffoldFades(section, scrollState, true, hasBottomBar, bottom)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ScaffoldColumn(
    onViewportTop: ((Float) -> Unit)?,
    scrollState: ScrollState,
    scrolling: Boolean,
    scrollChrome: Boolean,
    centerContent: Boolean,
    bottomInset: Dp,
    bottomPadding: Dp,
    measure: Boolean,
    clockSize: Dp,
    onRestHeight: (Int) -> Unit,
    topPadding: Dp = if (scrollChrome) 0.dp else SkDimens.GapScreen,
    content: @Composable ColumnScope.() -> Unit,
) {
    val nestedFocusScroll = LocalBringIntoViewSpec.current
    val topFade = with(LocalDensity.current) { SkDimens.ScrollFadeTop.toPx() }
    CompositionLocalProvider(LocalBringIntoViewSpec provides rememberFocusScrollSpec(bottomInset)) {
        Column(
            Modifier
                .fillMaxSize()
                .then(
                    if (scrolling) {
                        Modifier
                            .onGloballyPositioned {
                                onViewportTop?.invoke(it.positionInRoot().y + topFade)
                            }.verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                ).then(if (measure) Modifier.measureRestHeight(clockSize, onRestHeight) else Modifier)
                .padding(
                    start = SkDimens.Gutter,
                    end = SkDimens.Gutter,
                    top = topPadding,
                    bottom = bottomPadding,
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    SkDimens.GapScreen,
                    if (centerContent && scrolling) Alignment.CenterVertically else Alignment.Top,
                ),
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides nestedFocusScroll) { content() }
        }
    }
}

@Composable
private fun BoxScope.ScaffoldFades(
    section: Section,
    scrollState: ScrollState,
    scrolling: Boolean,
    hasBottomBar: Boolean,
    bottom: Dp,
) {
    if (scrolling && scrollState.canScrollBackward) ScaffoldTopFade(section)
    if (scrolling && scrollState.canScrollForward) {
        ScaffoldBottomFade(section, if (hasBottomBar) 0.dp else bottom)
    }
}

@Composable
private fun BoxScope.ScaffoldBottomFade(
    section: Section,
    bottom: Dp = 0.dp,
) {
    // Content still below fades out above the system navigation bar instead of running
    // under its buttons, as it does under the top bar.
    val fade = SkDimens.ScrollFade
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(bottom + fade)
            .background(
                Brush.verticalGradient(
                    0f to section.bottomColor.copy(alpha = 0f),
                    fade / (bottom + fade) to section.bottomColor,
                ),
            ),
    )
}

@Composable
private fun ScaffoldBottomBar(
    section: Section,
    content: (@Composable () -> Unit)?,
    contentWidth: Dp,
) {
    if (content == null) return
    Box(
        Modifier
            .widthIn(max = min(contentWidth, SkDimens.ReadingWidth))
            .fillMaxWidth()
            .background(section.bottomColor)
            .padding(horizontal = SkDimens.Gutter)
            .padding(
                top = SkDimens.GapS,
                bottom =
                    WindowInsets.safeDrawing
                        .exclude(WindowInsets.ime)
                        .asPaddingValues()
                        .calculateBottomPadding() +
                        SkDimens.GapS,
            ),
    ) { content() }
}

@Composable
private fun ScaffoldTopFade(section: Section) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SkDimens.ScrollFadeTop)
            .background(
                Brush.verticalGradient(
                    0f to section.color,
                    SkDimens.SCROLL_FADE_TOP_SOLID to section.color,
                    1f to section.color.copy(alpha = 0f),
                ),
            ),
    )
}
