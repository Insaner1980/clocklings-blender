package fi.clocklings.ui.theme

import androidx.compose.ui.unit.dp

/** Välit ja koot. Kosketusalueet ovat vähintään 48 dp. */
object SkDimens {
    const val MAX_ROW_FONT_SCALE = 1.3f

    val ButtonHeight = 64.dp
    val ButtonHeightSmall = 56.dp
    val ButtonPadX = 24.dp
    val ButtonIcon = 22.dp
    val OptionMinHeight = 64.dp
    val OptionMinHeightCompact = 56.dp
    val OptionTimeMinHeight = 40.dp
    val OptionTimeMinWidth = 64.dp
    val RowMinHeight = 72.dp
    val PracticeRowMinHeight = 64.dp
    val PracticeIndent = 28.dp
    val RowBadge = 48.dp
    val RowBadgeCurrent = 56.dp
    val RowStateIcon = 32.dp
    val IconBadge = 40.dp
    val TileIconBadge = 48.dp
    val TileIconBadgeCompact = 52.dp
    val ProgressSegmentHeight = 8.dp
    val ProgressSegmentGap = 4.dp
    val ProgressSegmentGapWide = 6.dp
    val ChipHeight = 40.dp
    val WardrobeButtonHeight = 48.dp
    val StageInset = 16.dp
    val SelectionBorder = 3.dp
    val StarResultSide = 64.dp
    val StarResultMiddle = 80.dp
    val GroupGap = 24.dp
    val GapXS = 4.dp
    val GapTiny = 2.dp
    val GapHalf = 6.dp
    val GapLabel = 14.dp
    val RewardArt = 88.dp
    val ResultRewardArt = 72.dp
    val RewardItemWidth = 104.dp
    val OptionCompactWidth = 300.dp
    val PillPadX = 10.dp
    val PillPadY = 3.dp
    val SmallIcon = 14.dp

    val Gutter = 16.dp
    val ReadingWidth = 640.dp
    val LocaleErrorMascot = 160.dp
    val WideContentWidth = 1120.dp
    val SideBySideWidth = 720.dp
    val VisualPaneWidth = 400.dp
    val ControlsMinWidth = 360.dp
    val ClockMax = 400.dp
    val PinnedChromeMinHeight = 360.dp
    val CompactTaskHeight = 480.dp
    val GapS = 8.dp
    val GapM = 12.dp

    /** Näkymän osien väli: tiivis, jotta kello ja kolme vastausta mahtuvat 360×640 dp:n näytölle. */
    val GapScreen = 12.dp
    val GapOptions = 10.dp
    val Gap = 16.dp
    val GapL = 18.dp

    val PanelPadding = 20.dp
    val BorderThin = 2.dp
    val BorderQuiet = 1.5.dp
    val LockPillHeight = 28.dp
    val LockPillTilePadding = 7.dp
    val Border = 3.dp

    /** Padding inside the time pill beside an answer's words. */
    val OptionTimePadX = 12.dp

    val IconButton = 48.dp
    val ActionIcon = 24.dp
    val ChildTarget = 64.dp
    val ClockThumbnail = 96.dp

    val SelectionBadge = 24.dp
    val TipCompanion = 72.dp
    val TipCompanionCompact = 48.dp
    val BubbleTail = 10.dp
    val LegendSwatchShort = 22.dp
    val LegendSwatchLong = 34.dp
    val LegendSwatchHeight = 8.dp
    val StageMinHeight = 200.dp
    val TallHomeStageViewport = 800.dp
    val HomeStageSkyTrim = 32.dp
    val ShortHomeStageHeight = 144.dp
    val LogoMinSize = 14.dp
    val PillIconLarge = 20.dp
    val CompanionThumbnail = 64.dp
    val MenuTileHeight = 160.dp
    val MenuTileCompactHeight = 132.dp
    val Divider = 1.dp
    val SettingsGroupPadding = 16.dp
    val SettingsLineGap = 2.dp
    val SettingsCardInset = 4.dp
    val SettingsRowHeight = 56.dp
    val GridBreakpoint = 320.dp
    val SwatchRing = 4.dp
    val ItemWindow = 76.dp

    /**
     * The zoom window shows face units x about 111 to 289 and y 8 to 126 of the 468 unit clock canvas (240 dp, so
     * 0.513 dp per unit): the rim, the numerals 11, 12 and 1 and both hand tips, but not the tops of 10 and 2
     * (about y 130). The shift moves the canvas up by (8 + 34) units; the height is the 118 unit span.
     */
    val ZoomWindowHeight = 60.5.dp
    val ZoomClock = 240.dp
    val ZoomClockShift = (-21.5).dp
    val ThumbnailMin = 48.dp
    const val CLOCK_HEIGHT_SHARE = 0.42f
    const val PLAY_CLOCK_HEIGHT_SHARE = 0.52f

    /**
     * Smallest clock a screen that fits its clock to the content (the challenge run) may shrink it to: it keeps
     * the clock readable while the first answer stays within reach at large text.
     */
    val FitClockMin = 160.dp
    val FeedbackClockMin = 220.dp
    val QuizClockMin = 200.dp
    val LessonPageClockMax = 340.dp
    val FiveMinuteDemoClockMin = 272.dp
    val FeedbackAvatar = 40.dp
    val ChallengeCompanionHeight = 190.dp
    val ChallengeBadgeHeight = 36.dp
    val ChallengeBadgeStar = 18.dp
    val DayPartHeight = 40.dp
    val DayPartIcon = 22.dp
    val TallTaskHeight = 760.dp

    /** Clock share on the read and set game screen only; Ask and Challenge keep [PLAY_CLOCK_HEIGHT_SHARE]. */
    const val GAME_CLOCK_HEIGHT_SHARE = 0.58f

    /** Opacity of the preset clock before the minute challenge starts, so it does not read as a question. */
    const val INACTIVE_CLOCK_ALPHA = 0.45f

    val HomeStageMaxHeight = 360.dp
    val StageGroundOverlap = 4.dp
    val BubbleHeadOffset = 4.dp
    val PinnedPreviewSize = 88.dp
    const val PINNED_PREVIEW_MAX_SHARE = 0.25f
    val ScrollFade = 16.dp

    /** Taller, opaque at its start: text under the top bar is hidden or reduced rather than half-faded. */
    val ScrollFadeTop = 28.dp

    /** Share of [ScrollFadeTop], from the top, that stays fully opaque before the fade begins. */
    const val SCROLL_FADE_TOP_SOLID = 0.45f

    const val CLOCK_INNER_SHADOW_WIDTH = 4f
    const val CLOCK_GLASS_INSET = 6f
    const val CLOCK_GLASS_WIDTH = 3f
    const val CLOCK_GLASS_START = 208f
    const val CLOCK_GLASS_SWEEP = 44f
    const val CLOCK_HAND_SHADOW_OFFSET = 3f
    const val CLOCK_HAND_SHADOW_SPREAD = 3f

    val Star = 28.dp
    val StarSmall = 18.dp
    val MenuIcon = 28.dp
    val LargeActionIcon = 32.dp

    val MiniClockLevel = 56.dp
    val DollResult = 128.dp
    val ResultCompanionMinHeight = 96.dp
    val DollBigMax = 220.dp
    val CompanionPreview = 245.dp
    val NameFieldMaxWidth = 280.dp
    val FieldPaddingHorizontal = 14.dp
    val FieldPaddingVertical = 10.dp
    val TimerHeight = 16.dp
}
