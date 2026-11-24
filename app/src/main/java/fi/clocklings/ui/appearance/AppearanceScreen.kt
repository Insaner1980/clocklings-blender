package fi.clocklings.ui.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import fi.clocklings.R
import fi.clocklings.audio.companionVoice
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.RetryableSave
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.AdaptiveContent
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.PressSurface
import fi.clocklings.ui.components.PreviewScroll
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SelectionBadge
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SwatchTile
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.components.adaptiveGridColumns
import fi.clocklings.ui.components.bringFeedbackIntoView
import fi.clocklings.ui.observeAccessPolicy
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.PersonalizationColors
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

@Composable
fun AppearanceScreen(
    appearance: Personalization,
    progress: Progress,
    onOutfits: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onParent: () -> Unit = {},
) {
    val app = LocalAppViewModel.current
    val access = app.observeAccessPolicy()
    val save = remember(app) { RetryableSave() }
    val onChange: ((Personalization) -> Personalization) -> Unit = { transform ->
        save.run { app.personalize(it, transform) }
    }
    val pager =
        rememberCompanionPager(appearance.companion, progress.totalStars, access) { chosen ->
            onChange { it.copy(companion = chosen) }
            companionVoice(chosen)?.let(app::play)
        }
    var clocks by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val preview = remember(scroll, clocks) { PreviewScroll(scroll) }
    ScreenScaffold(
        modifier,
        wideContent = true,
        section = Section.COMPANION,
        scrollState = scroll,
        onViewportTop = { preview.viewportTop = it },
        topBar = { AppearanceHeader(clocks, onBack) { clocks = it } },
        pinnedContent = { height ->
            if (clocks) {
                PinnedClockPreview(appearance.clock, preview.showCompact, height, PREVIEW_TIME)
            }
        },
        bottomBar =
            save.retry?.let { retry ->
                {
                    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
                        Text(stringResource(R.string.settings_save_error), style = SkType.Body)
                        SkButton(stringResource(R.string.settings_retry), retry, Modifier.fillMaxWidth())
                    }
                }
            },
    ) { clockSize ->
        if (clocks) {
            ClockChoices(appearance, clockSize, preview, onChange)
        } else {
            // The preview grows with the screen so the page uses its height instead of leaving it empty.
            CompanionChoices(
                appearance,
                progress,
                max(SkDimens.CompanionPreview, clockSize * PREVIEW_GROWTH),
                onOutfits,
                pager,
                access,
                onParent,
                scroll,
            )
        }
    }
}

@Composable
private fun CompanionChoices(
    appearance: Personalization,
    progress: Progress,
    previewHeight: Dp,
    onOutfits: () -> Unit,
    pager: PagerState,
    access: AccessPolicy,
    onParent: () -> Unit,
    scroll: ScrollState,
) {
    val kind = appearance.companion
    val preview = CompanionKind.entries[pager.settledPage]
    val windowHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp()
        }
    val short = windowHeight < SkDimens.CompactTaskHeight
    val visual: @Composable (Dp) -> Unit = { CompanionPager(pager, progress, access, it) }
    val thumbs: @Composable () -> Unit = { CompanionThumbnails(pager, kind, progress, access, short, PREVIEW_TIME) }
    val actions: @Composable () -> Unit = {
        if (!access.companion(preview, progress.totalStars)) {
            Column(
                Modifier.padding(bottom = SkDimens.ScrollFade).bringFeedbackIntoView(preview.name, scroll),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
            ) {
                Text(stringResource(companionRequirement(access)), style = SkType.BodySoft)
                if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent)
            }
        } else if (preview == CompanionKind.NONE) {
            Text(stringResource(R.string.no_companion_hint), style = SkType.BodySoft)
        } else {
            SkButton(
                stringResource(R.string.dress_doll),
                onOutfits,
                Modifier.fillMaxWidth(),
                kind = ButtonKind.PRIMARY,
                icon = SkIcons.Outfit,
                enabled = preview == kind,
            )
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (!short && maxWidth >= companionThumbnailRowWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen)) {
                visual(previewHeight)
                thumbs()
                actions()
            }
        } else {
            AdaptiveContent(previewHeight, visual) {
                thumbs()
                actions()
            }
        }
    }
}

internal fun companionRequirement(access: AccessPolicy): Int =
    if (access.fullAccess) R.string.companion_star_requirement else R.string.companion_access_requirement

@Composable
private fun ClockChoices(
    appearance: Personalization,
    previewSize: Dp,
    preview: PreviewScroll,
    onChange: ((Personalization) -> Personalization) -> Unit,
) {
    AdaptiveContent(previewSize, visual = { size ->
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            ClockBox(size) {
                ClockFace(
                    ClockCommand(PREVIEW_TIME),
                    Modifier.testTag("selected_clock").onGloballyPositioned(preview::positionPreview),
                    appearance = appearance.clock,
                )
            }
            // The preview's state description already names the style, so TalkBack skips this caption.
            Text(
                stringResource(clockThemeName(appearance.clock.theme)),
                style = SkType.SectionTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            )
        }
    }) {
        ChoiceGrid(ClockTheme.entries) { theme, cell ->
            ChoiceCard(
                stringResource(clockThemeName(theme)),
                appearance.clock.theme == theme,
                { onChange { it.copy(clock = it.clock.copy(theme = theme)) } },
                cell.testTag("choose_clock_${theme.name}"),
            ) {
                ClockFace(
                    ClockCommand(PREVIEW_TIME),
                    Modifier.size(SkDimens.ClockThumbnail),
                    mini = true,
                    appearance = appearance.clock.copy(theme = theme),
                )
            }
        }
        if (appearance.clock.theme == ClockTheme.CUSTOM) {
            CustomClockControls(appearance.clock.custom) { transform ->
                onChange { it.copy(clock = it.clock.copy(custom = transform(it.clock.custom))) }
            }
        }
    }
}

@Composable
private fun CustomClockControls(
    custom: CustomClock,
    onChange: ((CustomClock) -> CustomClock) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapL)) {
        CompanionTip(stringResource(R.string.custom_clock_hint), color = SkColors.TryBg)
        ClockColors(custom, onChange)
        ClockOptions(
            R.string.clock_frame,
            ClockFrame.entries,
            custom.frame,
            { value -> onChange { it.copy(frame = value) } },
            {
                when (it) {
                    ClockFrame.ROUND -> R.string.frame_round
                    ClockFrame.SCALLOPED -> R.string.frame_scalloped
                    ClockFrame.BLOCKS -> R.string.frame_blocks
                }
            },
            { custom.copy(frame = it) },
        )
        ClockOptions(
            R.string.clock_numerals,
            NumeralStyle.entries,
            custom.numerals,
            { value -> onChange { it.copy(numerals = value) } },
            { if (it == NumeralStyle.ROUNDED) R.string.numerals_rounded else R.string.numerals_plain },
            { custom.copy(numerals = it) },
        )
        ClockOptions(
            R.string.clock_hands,
            HandStyle.entries,
            custom.hands,
            { value -> onChange { it.copy(hands = value) } },
            { if (it == HandStyle.ROUND) R.string.hands_round else R.string.hands_pointer },
            { custom.copy(hands = it) },
        )
    }
}

@Composable
private fun <T> ClockOptions(
    title: Int,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> Int,
    preview: (T) -> CustomClock,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Text(stringResource(title), style = SkType.SectionTitle, modifier = Modifier.semantics { heading() })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            options.forEach { option ->
                ChoiceCard(stringResource(label(option)), option == selected, { onSelect(option) }, showLabel = false) {
                    ZoomedClock(
                        LocalPersonalization.current.clock.copy(theme = ClockTheme.CUSTOM, custom = preview(option)),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    content: @Composable (T, Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = adaptiveGridColumns(maxWidth, LocalDensity.current.fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            for (row in options.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                    for (option in row) content(option, Modifier.weight(1f))
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    art: @Composable () -> Unit,
) {
    val fill = SkColors.Surface
    PressSurface(
        onClick,
        fill,
        SkShapes.Item,
        modifier.semantics {
            selected = on
            if (!showLabel) contentDescription = title
        },
        border = if (on) BorderStroke(SkDimens.SelectionBorder, SkColors.Primary) else null,
        role = Role.RadioButton,
    ) {
        // Centered in a wide grid cell; the check sits on the preview's corner, as on wardrobe tiles.
        Box(Modifier.padding(SkDimens.GapM), contentAlignment = Alignment.TopCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            ) {
                Box(Modifier.clearAndSetSemantics { }) {
                    art()
                    if (on) SelectionBadge(Modifier.align(Alignment.TopEnd))
                }
                if (showLabel) WordSafeText(title, SkType.ItemName)
            }
        }
    }
}

internal const val PREVIEW_TIME = 610

@Composable
private fun CompanionPager(
    pager: PagerState,
    progress: Progress,
    access: AccessPolicy,
    previewHeight: Dp,
) {
    HorizontalPager(pager, Modifier.fillMaxWidth().testTag("companion_pager"), key = {
        CompanionKind.entries[it].name
    }) { page ->
        val option = CompanionKind.entries[page]
        FlatPanel(Modifier.testTag("companion_preview"), shape = SkShapes.Tile) {
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (option == CompanionKind.NONE) {
                        ClockFace(ClockCommand(PREVIEW_TIME), Modifier.size(previewHeight), mini = true)
                    } else {
                        val greeting =
                            remember(option, pager.settledPage) { CompanionCelebration(newOutfit = null, jump = true) }
                        CompanionView(
                            option,
                            progress.outfitFor(option),
                            Modifier
                                .then(
                                    if (page ==
                                        pager.settledPage
                                    ) {
                                        Modifier.sharedClocklingsElement("companion")
                                    } else {
                                        Modifier
                                    },
                                ).height(previewHeight)
                                .aspectRatio(companionAspectRatio(option)),
                            celebration = greeting,
                            animated = page == pager.settledPage,
                        )
                    }
                    Text(stringResource(companionName(option)), style = SkType.SectionTitle)
                }
                if (!access.companion(option, progress.totalStars)) {
                    SkIcon(SkIcons.Lock, SkDimens.Star, Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

private const val PREVIEW_GROWTH = 1.15f

@Composable
private fun ClockColors(
    custom: CustomClock,
    onChange: ((CustomClock) -> CustomClock) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Text(
            stringResource(R.string.clock_color),
            style = SkType.SectionTitle,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            ClockPalette.entries.forEach { palette ->
                val name =
                    when (palette) {
                        ClockPalette.BLUE -> R.string.color_blue
                        ClockPalette.GREEN -> R.string.color_green
                        ClockPalette.PURPLE -> R.string.color_purple
                        ClockPalette.CORAL -> R.string.color_coral
                    }
                SwatchTile(
                    PersonalizationColors.clock(palette),
                    stringResource(name),
                    custom.palette == palette,
                    { onChange { it.copy(palette = palette) } },
                )
            }
        }
    }
}
