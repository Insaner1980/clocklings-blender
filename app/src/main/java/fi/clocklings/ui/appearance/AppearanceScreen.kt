package fi.clocklings.ui.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import fi.clocklings.ui.components.DepthSurface
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SegmentedControl
import fi.clocklings.ui.components.SelectionBadge
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SwatchTile
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.components.adaptiveGridColumns
import fi.clocklings.ui.observeAccessPolicy
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.PersonalizationColors
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.ledgeColor
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

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
    ScreenScaffold(
        modifier,
        wideContent = true,
        section = Section.COMPANION,
        topBar = { TopBar(stringResource(R.string.appearance_title), onBack) },
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
        SegmentedControl(
            "",
            listOf(stringResource(R.string.companions_title), stringResource(R.string.clock_styles_title)),
            if (clocks) 1 else 0,
            { clocks = it == 1 },
            Modifier.fillMaxWidth(),
        )
        if (clocks) {
            ClockChoices(appearance, clockSize, onChange)
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
) {
    val kind = appearance.companion
    val preview = CompanionKind.entries[pager.settledPage]
    AdaptiveContent(previewHeight, visual = { size -> CompanionPager(pager, progress, size) }) {
        CompanionThumbnails(pager, kind, progress, access)
        if (!access.companion(preview, progress.totalStars)) {
            Text(stringResource(R.string.companion_access_requirement), style = SkType.BodySoft)
            if (!access.fullAccess) SkButton(stringResource(R.string.parent_title), onParent)
        } else if (preview == CompanionKind.NONE) {
            Text(stringResource(R.string.no_companion_hint), style = SkType.BodySoft)
        } else {
            SkButton(
                stringResource(R.string.dress_doll),
                onOutfits,
                Modifier.fillMaxWidth(),
                kind = ButtonKind.PRIMARY,
                icon = SkIcons.Dress,
                enabled = preview == kind,
            )
        }
    }
}

@Composable
private fun ClockChoices(
    appearance: Personalization,
    previewSize: Dp,
    onChange: ((Personalization) -> Personalization) -> Unit,
) {
    AdaptiveContent(previewSize, visual = { size ->
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            ClockBox(size) {
                ClockFace(
                    ClockCommand(PREVIEW_TIME),
                    Modifier.testTag("selected_clock"),
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
                    Modifier.size(SkDimens.PathNode),
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
        CompanionTip(stringResource(R.string.custom_clock_hint))
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
    val fill = if (on) SkColors.ItemOnBg else SkColors.Surface
    DepthSurface(
        onClick,
        fill,
        ledgeColor(fill),
        SkShapes.Item,
        modifier.semantics {
            selected = on
            if (!showLabel) contentDescription = title
        },
        border = if (on) BorderStroke(SkDimens.Border, SkColors.ItemOnBorder) else null,
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

private const val PREVIEW_TIME = 610

@Composable
private fun CompanionPager(
    pager: PagerState,
    progress: Progress,
    previewHeight: Dp,
) {
    HorizontalPager(pager, Modifier.fillMaxWidth().testTag("companion_pager"), key = {
        CompanionKind.entries[it].name
    }) { page ->
        val option = CompanionKind.entries[page]
        FlatPanel(Modifier.testTag("companion_preview")) {
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
        }
    }
}

@Composable
private fun CompanionThumbnails(
    pager: PagerState,
    kind: CompanionKind,
    progress: Progress,
    access: AccessPolicy,
) {
    val scope = rememberCoroutineScope()
    val reduced = rememberReduceMotion()
    val goTo: suspend (Int) -> Unit = { if (reduced) pager.scrollToPage(it) else pager.animateScrollToPage(it) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val count = CompanionKind.entries.size
        // All choices fit on one row when there is room; only very narrow screens scroll.
        val thumb =
            ((maxWidth - SkDimens.GapS * (count - 1)) / count).coerceIn(
                SkDimens.ThumbnailMin,
                SkDimens.CompanionThumbnail,
            )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = SkDimens.GapS),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            CompanionKind.entries.forEach { option ->
                val label = stringResource(companionName(option))
                DepthSurface(
                    { scope.launch { goTo(option.ordinal) } },
                    if (kind == option) SkColors.ItemOnBg else SkColors.Surface,
                    ledgeColor(SkColors.Surface),
                    SkShapes.Item,
                    Modifier
                        .size(thumb)
                        .testTag("choose_companion_${option.name}")
                        .semantics {
                            contentDescription = label
                            selected = kind == option
                        },
                    border = if (kind == option) BorderStroke(SkDimens.BorderThin, SkColors.ItemOnBorder) else null,
                    role = Role.RadioButton,
                    enabled = access.companion(option, progress.totalStars),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(thumb * THUMB_ART).clearAndSetSemantics { }) {
                            if (option == CompanionKind.NONE) {
                                ClockFace(ClockCommand(PREVIEW_TIME), mini = true)
                            } else {
                                CompanionView(
                                    option,
                                    progress.outfitFor(option),
                                    Modifier.matchParentSize(),
                                    animated = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val THUMB_ART = 0.78f
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
