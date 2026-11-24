package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.Screen
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Task
import fi.clocklings.domain.TaskType
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.ClockFit
import fi.clocklings.ui.components.ClockTaskScaffold
import fi.clocklings.ui.game.GameScreen
import fi.clocklings.ui.game.GameUiState
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeDestination
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.home.centredContentTop
import fi.clocklings.ui.home.companionStageLayout
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pathPracticeRowNamesTheGame() {
        val fullAccess = mutableStateOf(true)
        val context = localizedContext(AppLanguage.FINNISH)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalResources provides context.resources) {
                ClocklingsTheme {
                    LessonListScreen(false, Progress(), AccessPolicy(fullAccess.value), {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("Pelaa: Tasatunnit", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Tasatunnit", useUnmergedTree = true).assertExists()
        compose.runOnIdle { fullAccess.value = false }
        compose.onNodeWithText("Pelaa: Puoli tuntia", useUnmergedTree = true).assertExists()
        val locked = hasContentDescription("Puoli tuntia. Täysversio")
        compose.onNode(locked).assertIsNotEnabled()
        compose.onNode(hasText("Täysversio") and hasAnyAncestor(locked), useUnmergedTree = true).assertExists()
    }

    @Test
    fun setTaskCheckButtonSaysCheck() {
        val context = localizedContext(AppLanguage.FINNISH)
        val state =
            GameUiState(
                level = 0,
                h24 = false,
                taskIndex = 0,
                taskCount = 10,
                task = Task(180, TaskType.SET),
                clock = ClockCommand(0),
                step = 60,
                showDone = true,
            )
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalResources provides context.resources) {
                ClocklingsTheme { GameScreen(state, true, {}, {}, {}, {}, {}, {}) }
            }
        }
        compose.onNode(hasText("Tarkista") and hasClickAction()).assertIsDisplayed()
        compose.onNode(hasText("Valmis") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun taskClockKeepsTheSmallerSizeWhenFeedbackGetsShorter() {
        val controlsHeight = mutableStateOf(100.dp)
        val taskIndex = mutableIntStateOf(0)
        compose.setContent {
            ClocklingsTheme {
                Box(Modifier.requiredSize(411.dp, 914.dp)) {
                    ClockTaskScaffold(
                        section = Section.PLAY,
                        topBar = {},
                        visual = { Box(Modifier.size(it).testTag("measured_clock")) },
                        clockFit = ClockFit.FIT,
                        clockTaskKey = taskIndex.intValue,
                    ) { Box(Modifier.height(controlsHeight.value)) }
                }
            }
        }

        fun clockWidth() =
            compose
                .onNodeWithTag("measured_clock")
                .fetchSemanticsNode()
                .size.width

        val before = clockWidth()
        compose.runOnIdle { controlsHeight.value = 700.dp }
        val hint = clockWidth()
        assertTrue("The tall feedback must shrink the clock", hint < before)
        compose.runOnIdle { controlsHeight.value = 150.dp }
        assertEquals("Shorter feedback must keep the smaller clock", hint, clockWidth())
        compose.runOnIdle { taskIndex.intValue++ }
        assertEquals("The next task must start at full size", before, clockWidth())
    }

    @Test
    fun allHomeDestinationsRemainReachableAtNarrowWidthAndLargeFont() {
        val viewport = mutableStateOf(411 to 1f)
        val companion = mutableStateOf(CompanionKind.HUMAN)
        var opened: HomeDestination? = null
        compose.setContent {
            val base = LocalDensity.current
            val personalization = Personalization(companion = companion.value)
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, viewport.value.second),
                LocalPersonalization provides personalization,
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 800.dp)) {
                        HomeScreen(Progress(), Settings(personalization = personalization), { opened = it })
                    }
                }
            }
        }
        val destinations =
            mapOf(
                "Learn" to HomeDestination.KOULU,
                "Explore" to HomeDestination.VAPAA,
                "Play" to HomeDestination.PELAA,
                "Play Together" to HomeDestination.KYSY,
                "My Companion" to HomeDestination.NUKKE,
                "Settings" to HomeDestination.SETTINGS,
            )
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            for (kind in listOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.NONE)) {
                compose.runOnIdle {
                    viewport.value = cell
                    companion.value = kind
                }
                for ((label, expected) in destinations) {
                    compose
                        .onAllNodes(
                            (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction(),
                        )[0]
                        .scrollIfNeeded()
                        .assertIsDisplayed()
                        .performClick()
                    assertEquals("$cell / $kind / $label", expected, opened)
                }
            }
        }
    }

    @Test
    fun eachLevelAndItsLessonDispatchTheCorrectIndex() {
        val progress = mutableStateOf(Progress())
        var played = -1
        var lesson = -1
        var challenge = false
        compose.setContent {
            ClocklingsTheme {
                LevelsScreen(progress.value, AccessPolicy(fullAccess = true), {
                    played =
                        it
                }, { lesson = it }, { challenge = true }, {})
            }
        }
        val titles = listOf("Whole hours", "Half past", "Quarter hours", "Five-minute steps", "Exact minutes")
        for (stars in listOf(listOf(0, 0, 0, 0, 0), listOf(3, 2, 1, 0, 0), listOf(3, 3, 3, 3, 3))) {
            compose.runOnIdle { progress.value = Progress(stars = stars, challengeBest = 7) }
            for ((index, title) in titles.withIndex()) {
                compose
                    .onNode(
                        hasContentDescription("$title.", substring = true) and hasClickAction(),
                    ).scrollIfNeeded()
                    .performClick()
                assertEquals(index, played)
                compose.onNodeWithContentDescription("Watch the lesson: $title").scrollIfNeeded().performClick()
                assertEquals(index, lesson)
            }
        }
        compose
            .onNode(
                hasContentDescription("Minute Challenge.", substring = true) and hasClickAction(),
            ).scrollIfNeeded()
            .performClick()
        assertEquals(true, challenge)
    }

    @Test
    fun lessonPathHasNoChapterHeadersAndEachChapterEndsWithAPractiseNode() {
        val progress = Progress(stars = listOf(3, 0, 0, 0, 0), lessonsDone = Lessons.levelLessons[0].toSet())
        compose.setContent {
            ClocklingsTheme {
                LessonListScreen(false, progress, AccessPolicy(fullAccess = true), {}, {}, {})
            }
        }
        val levelNames = listOf("Whole hours", "Half past", "Quarter hours", "Five-minute steps", "Exact minutes")
        for (name in levelNames) {
            compose.onAllNodes(isHeading() and hasText(name)).assertCountEquals(0)
        }
        // The level names now identify the practice rows, with one complete description per action.
        levelNames.forEachIndexed { index, name ->
            val action = if (index == 0) "Play again" else "Start playing!"
            compose
                .onNode(hasContentDescription("$name. $action.", substring = true) and hasClickAction())
                .assertExists()
            compose.onNodeWithText("Play: $name", useUnmergedTree = true).assertExists()
        }
        compose.onAllNodesWithText("Play again", useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithText("Start playing!", useUnmergedTree = true).assertCountEquals(levelNames.size - 1)
    }

    @Test
    fun eachLessonChapterExposesOneScreenReaderHeadingOnItsFirstStop() {
        compose.setContent {
            ClocklingsTheme {
                LessonListScreen(true, Progress(), AccessPolicy(fullAccess = true), {}, {}, {})
            }
        }
        // The top bar title is a text heading; chapter headings live on the first stop's description.
        val headings =
            compose.onAllNodes(isHeading()).fetchSemanticsNodes().mapNotNull {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            }
        val chapters =
            listOf(
                "Whole hours. Two hands.",
                "Half past.",
                "Quarter hours.",
                "Five-minute steps.",
                "Exact minutes.",
                "24 hours in a day.",
            )
        assertEquals(headings.toString(), chapters.size, headings.size)
        for (chapter in chapters) {
            assertEquals("$chapter in $headings", 1, headings.count { it.startsWith(chapter) })
        }
    }

    @Test
    fun allFourHomeTilesFitWithoutScrollingOnAShortPhone() {
        val kind = mutableStateOf(CompanionKind.CAT)
        compose.setContent {
            val personalization = Personalization(companion = kind.value)
            CompositionLocalProvider(LocalPersonalization provides personalization) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(360.dp, RESULT_HEIGHT.dp)) {
                        HomeScreen(Progress(), Settings(personalization = personalization), {})
                    }
                }
            }
        }
        for (companion in listOf(CompanionKind.CAT, CompanionKind.NONE)) {
            compose.runOnIdle { kind.value = companion }
            val viewport = compose.onNode(hasScrollAction()).fetchSemanticsNode()
            assertEquals(0f, viewport.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(), 1f)
            for (title in listOf("Learn", "Explore", "Play", "Play Together")) {
                val tile = compose.onNode(hasText(title) and hasClickAction()).assertIsDisplayed().fetchSemanticsNode()
                val bottom = tile.positionInRoot.y + tile.size.height
                assertTrue("$companion / $title: entire tile visible", bottom <= viewport.boundsInRoot.bottom)
            }
        }
    }

    @Test
    fun homeTapTargetFollowsTheContentBoxBelowTheTrimmedSky() {
        val companion = mutableStateOf(CompanionKind.CAT)
        compose.setContent {
            val personalization = Personalization(companion = companion.value)
            CompositionLocalProvider(LocalPersonalization provides personalization) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(411.dp, 914.dp)) {
                        HomeScreen(Progress(), Settings(personalization = personalization), {})
                    }
                }
            }
        }
        for (kind in listOf(CompanionKind.CAT, CompanionKind.HUMAN, CompanionKind.NONE)) {
            compose.runOnIdle { companion.value = kind }
            val slot = compose.onNodeWithTag("home_stage").fetchSemanticsNode().boundsInRoot
            val target = compose.onNodeWithTag("companion_stage").fetchSemanticsNode().boundsInRoot
            with(compose.density) {
                val slotHeight = slot.height.toDp()
                // Without a companion the target is the measured bubble and clock group, at most the content box.
                val contentHeight =
                    if (kind == CompanionKind.NONE) target.height.toDp() else SkDimens.HomeStageMaxHeight
                val contentTop =
                    if (kind == CompanionKind.NONE) {
                        centredContentTop(slotHeight, contentHeight)
                    } else {
                        companionStageLayout(
                            slot.width.toDp(),
                            slotHeight,
                            contentHeight,
                            companionAspectRatio(kind),
                            skyTrim = SkDimens.HomeStageSkyTrim,
                        ).contentTop
                    }
                assertTrue("$kind: the slot is taller than the content", slotHeight > contentHeight)
                assertTrue("$kind: target height", contentHeight <= SkDimens.HomeStageMaxHeight)
                assertEquals("$kind: target height", contentHeight.value, target.height.toDp().value, 1f)
                assertEquals("$kind: target top", contentTop.value, (target.top - slot.top).toDp().value, 1f)
            }
        }
    }

    @Test
    fun compactHomeStageHugsItsContentAndTheClockOnlyGroupIsCentred() {
        val cell = mutableStateOf(Triple(320, 640, 1f))
        val companion = mutableStateOf(CompanionKind.CAT)
        compose.setContent {
            val (width, height, fontScale) = cell.value
            val base = LocalDensity.current
            val personalization = Personalization(companion = companion.value)
            CompositionLocalProvider(
                LocalPersonalization provides personalization,
                LocalDensity provides Density(base.density, fontScale),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(width.dp, height.dp)) {
                        HomeScreen(Progress(), Settings(personalization = personalization), {})
                    }
                }
            }
        }
        val gap = with(compose.density) { SkDimens.GapM.toPx() }

        fun bounds(matcher: SemanticsMatcher) =
            compose
                .onAllNodes(matcher, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .first()
                .boundsInRoot
        val bubble = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
        val clock = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)
        for (c in listOf(Triple(320, 640, 1f), Triple(320, 640, 2f), Triple(411, 914, 2f))) {
            for (kind in listOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.CAR, CompanionKind.NONE)) {
                compose.runOnIdle {
                    cell.value = c
                    companion.value = kind
                }
                val label = "$c $kind"
                val stage = compose.onNodeWithTag("home_stage").fetchSemanticsNode().boundsInRoot
                val target = compose.onNodeWithTag("companion_stage").fetchSemanticsNode().boundsInRoot
                // The group is centred: as much sky above it as ground below it. A compact page scrolls, so
                // there the stage leaves neither unless the screen has height to spare.
                val sky = target.top - stage.top
                val ground = stage.bottom - target.bottom
                assertEquals("$label: sky $sky, ground $ground", sky, ground, 1f)
                if (c.first == 320) assertEquals("$label: no spare height", 0f, sky, 1f)
                val topGap = with(compose.density) { SkDimens.GapS.toPx() }
                assertEquals("$label: bubble has the same top gap", topGap, bounds(bubble).top - target.top, 1f)
                // The row stands right below the bubble: the gap is the column spacing, not empty sky.
                val companionTop =
                    if (kind == CompanionKind.NONE) null else bounds(hasTestTag("companion_$kind")).top
                val rowTop = minOf(bounds(clock).top, companionTop ?: Float.MAX_VALUE)
                val bubbleGap = rowTop - bounds(bubble).bottom
                assertTrue("$label: bubble to row $bubbleGap", bubbleGap <= gap + 1f)
            }
        }
    }

    @Test
    fun togetherPhaseActionsStayVisibleWithoutScrollingAndTheAnswerShowsAfterReveal() {
        val viewport = mutableStateOf(360 to 1f)
        val model = AppViewModel(FakeServices(fullAccess = true))
        compose.setAskContent(model, viewport)

        fun assertActionVisible(label: String) {
            for (cell in TestViewports) {
                compose.runOnIdle { viewport.value = cell }
                compose.onNodeWithText(label).assertIsDisplayed()
            }
            compose.runOnIdle { viewport.value = TestViewports.first() }
        }
        assertActionVisible("Pass to partner")
        compose.onNodeWithText("Pass to partner").performClick()
        assertActionVisible("Show the answer")
        compose.onNodeWithText("Show the answer").performClick()
        // At 360 x 640 dp and 100 % font the revealed answer starts below the fold; it must scroll into view
        // by itself, since the child only pressed the button in the bottom bar.
        compose.onNodeWithText("3:00").assertIsDisplayed()
        // It must also clear the bottom scroll fade, so no digit is half faded out.
        val pill = compose.onNodeWithText("3:00").fetchSemanticsNode().boundsInRoot
        val viewportBox = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
        val fade = with(compose.density) { SkDimens.ScrollFade.toPx() }
        assertTrue("${pill.bottom} vs ${viewportBox.bottom}", pill.bottom <= viewportBox.bottom - fade)
        assertActionVisible("Swap turns")
        compose.onNodeWithText("Swap turns").performClick()
        var scroll = compose.onNode(hasScrollAction()).fetchSemanticsNode()
        assertEquals(0f, scroll.config[SemanticsProperties.VerticalScrollAxisRange].value())
        assertActionVisible("Pass to partner")
        for (cell in TestViewports) {
            compose.runOnIdle { viewport.value = cell }
            compose.onNodeWithText("Pass to partner").performClick()
            compose.onNodeWithText("Show the answer").performClick()
            compose.onNodeWithText("Swap turns").performClick()
            scroll = compose.onNode(hasScrollAction()).fetchSemanticsNode()
            assertEquals("$cell", 0f, scroll.config[SemanticsProperties.VerticalScrollAxisRange].value(), 0f)
        }
    }

    @Test
    fun resultStarsAndActionsMatchTheCompletedLevelAndCompanion() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        val result = mutableStateOf(Screen.Result(0, 1, 0, 0))
        var action = ""
        compose.setContent {
            CompositionLocalProvider(LocalPersonalization provides Personalization(companion = kind.value)) {
                ClocklingsTheme {
                    ResultScreen(
                        access = AccessPolicy(fullAccess = true),
                        result.value,
                        Outfit.default(kind.value),
                        { action = "replay" },
                        { action = "next" },
                        { action = "outfits" },
                        { action = "levels" },
                    )
                }
            }
        }
        for (companion in CompanionKind.entries) {
            for (stars in 1..3) {
                compose.runOnIdle {
                    kind.value = companion
                    result.value = Screen.Result(0, stars, 0, 0)
                }
                val description = if (stars == 1) "1\u00A0star out of three" else "$stars\u00A0stars out of three"
                compose.onNodeWithContentDescription(description).assertExists()
                compose.onNodeWithText("Play again").scrollIfNeeded().performClick()
                assertEquals("replay", action)
                compose.onNodeWithText("Next level").scrollIfNeeded().performClick()
                assertEquals("next", action)
                compose.onNodeWithContentDescription("Back").scrollIfNeeded().performClick()
                assertEquals("levels", action)
            }
        }
        compose.runOnIdle { result.value = Screen.Result(4, 3, 3, 15) }
        compose.onNodeWithText("Next level").assertDoesNotExist()
    }
}

internal fun SemanticsNodeInteraction.scrollIfNeeded(): SemanticsNodeInteraction {
    if (generateSequence(
            fetchSemanticsNode().parent,
        ) { it.parent }.any { SemanticsActions.ScrollBy in it.config }
    ) {
        performScrollTo()
    }
    return this
}

/** The content height of a short phone. */
internal const val RESULT_HEIGHT = 576
