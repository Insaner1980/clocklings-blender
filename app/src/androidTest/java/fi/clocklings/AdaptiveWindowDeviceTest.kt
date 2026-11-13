package fi.clocklings

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.resolve
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Runs only on the dedicated Clocklings test AVD; the runner selects real guest display configurations. */
@RunWith(AndroidJUnit4::class)
class AdaptiveWindowDeviceTest : InputDeviceTest() {
    private var mouseDownTime = 0L

    override fun initialProgress() = Progress(lessonsDone = Lessons.levelLessons.flatten().toSet())

    @Test
    fun allScreenFamiliesRemainReachableInTheActualWindow() {
        capture("home")
        open("Explore")
        assertArrangement()
        capture("explore")
        open("Random time")
        back()
        open("Learn")
        capture("lessons")
        open("Two hands")
        assertArrangement()
        assertCompactControlsVisible()
        capture("lesson")
        assertFullyVisibleAction("Next")
        open("Next")
        back()
        back()
        open("Play")
        capture("levels")
        open("Whole hours")
        waitFor(hasTestTag("game_task_0"))
        assertArrangement()
        capture("game")
        back()
        open("Minute Challenge")
        assertArrangement()
        assertCompactControlsVisible()
        capture("challenge")
        assertFullyVisibleAction("Start!")
        back()
        back()
        open("Play Together")
        assertArrangement()
        assertCompactControlsVisible()
        capture("together")
        open("Pass to partner")
        open("Show the answer")
        open("Swap turns")
        back()
        open("My Companion")
        capture("companions")
        open("Clock styles")
        assertArrangement()
        capture("clocks")
        compose.onNodeWithTag("choose_clock_CUSTOM").scrollToIfScrollable().performClick()
        capture("custom-clock")
        open("Companions")
        open("Choose an outfit")
        assertArrangement()
        capture("wardrobe")
        compose.onNode(hasSetTextAction()).scrollToIfScrollable().performTextReplacement("Resize draft ")
        repeat(4) { if (compose.onAllNodes(hasText("Clocklings")).fetchSemanticsNodes().isEmpty()) back() }
        open("Settings")
        capture("settings")
        open("English")
        capture("languages")
    }

    @Test
    fun compactTaskControlsFitInEveryLanguage() {
        val config = compose.activity.resources.configuration
        assumeTrue(config.screenWidthDp >= 1000 && config.screenHeightDp < 480 && config.fontScale == 1f)
        try {
            for (language in AppLanguage.entries) {
                environment.changeLanguage(language, compose.activityRule.scenario)
                compose.waitUntil(10_000) {
                    compose.activity.resources.configuration.locales[0]
                        .language == language.tag.substringBefore('-')
                }
                waitFor(hasText("Clocklings"))
                val res = environment.resources(language)
                open(res.getString(R.string.menu_koulu))
                open(LessonCatalog.lesson(Lessons.levelLessons.first().first(), language).title.resolve(res))
                compose.onNodeWithTag("compact_task_controls").assertIsDisplayed()
                assertCompactControlsVisible()
                capture("${language.tag}-lesson")
                back()
                back()
                open(res.getString(R.string.menu_pelaa))
                open(res.getString(R.string.challenge_title))
                assertCompactControlsVisible()
                capture("${language.tag}-challenge")
                back()
                back()
                open(res.getString(R.string.menu_kysy))
                assertCompactControlsVisible()
                capture("${language.tag}-together")
                back()
            }
        } finally {
            environment.changeLanguage(AppLanguage.ENGLISH, compose.activityRule.scenario)
        }
    }

    @Test
    fun clockAndNavigationSurviveLiveResizeRotationAndRecreation() {
        open("Explore")
        open("Random time")
        compose.waitForIdle()
        val clock = hasContentDescription("Clock face:", substring = true)
        val expected = compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        val originalSize = currentWindowSize()
        val originalAutoRotation = shell("settings get system accelerometer_rotation").trim()
        val originalUserRotation = shell("settings get system user_rotation").trim()
        try {
            for ((width, height) in listOf(800 to 900, 1200 to 600, 900 to 360, 320 to 800)) {
                shell("wm size ${width}x$height")
                // Legacy configuration dp excludes navigation bars; wm size sets the full window in pixels.
                compose.waitUntil(10_000) {
                    val decor = compose.activity.window.decorView
                    decor.width == width && decor.height == height
                }
                waitFor(clock)
                assertEquals(
                    expected,
                    compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription],
                )
                assertArrangement()
            }
            val before = compose.activity
            shell("settings put system accelerometer_rotation 0")
            shell("settings put system user_rotation 1")
            compose.waitUntil(10_000) { compose.activity !== before }
            waitFor(clock)
            assertEquals(
                expected,
                compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription],
            )
            compose.activityRule.scenario.recreate()
            waitFor(clock)
            assertEquals(
                expected,
                compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription],
            )
            back()
            waitFor(hasText("Clocklings"))
        } finally {
            val rotationSettings =
                listOf("accelerometer_rotation" to originalAutoRotation, "user_rotation" to originalUserRotation)
            for ((setting, value) in rotationSettings) {
                shell(if (value == "null") "settings delete system $setting" else "settings put system $setting $value")
            }
            shell("wm size $originalSize")
        }
    }

    @Test
    fun lessonAndPracticeKeepTheirCurrentContentAcrossWindowChanges() {
        val originalSize = currentWindowSize()
        try {
            open("Learn")
            open("Two hands")
            open("Next")
            val lesson = taskContent()
            for ((width, height) in listOf(1000 to 360, 1000 to 480, 320 to 800, 1000 to 360)) {
                resizeWindowDp(width, height)
                assertEquals("Current lesson page after resize", lesson, taskContent())
            }
            compose.activityRule.scenario.recreate()
            assertEquals("Current lesson page after recreation", lesson, taskContent())
            back()
            back()
            open("Play")
            open("Whole hours")
            waitFor(hasTestTag("game_task_0"))
            val choices = hasText("o'clock", substring = true) and hasClickAction()
            if (compose.onAllNodes(choices).fetchSemanticsNodes().isNotEmpty()) {
                compose.onAllNodes(choices)[0].scrollToIfScrollable().performClick()
            }
            val practice = taskContent()
            resizeWindowDp(320, 800)
            assertEquals("Current practice and answer feedback after resize", practice, taskContent())
            compose.activityRule.scenario.recreate()
            assertEquals("Current practice and answer feedback after recreation", practice, taskContent())
        } finally {
            shell("wm size $originalSize")
        }
    }

    private fun taskContent(): List<String> {
        compose.waitForIdle()
        val text = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)
        return compose
            .onAllNodes(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { it.config[SemanticsProperties.Text].map { value -> value.text } }
            .sorted()
    }

    @Test
    fun togetherKeepsChosenTimeAndTurnAcrossCompactThresholdAndRecreation() {
        val originalSize = currentWindowSize()
        try {
            open("Play Together")
            open("Random time")
            val clock = hasContentDescription("Clock face:", substring = true)
            val chosen = compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            open("Pass to partner")
            for ((height, compact) in listOf(360 to true, 600 to false, 360 to true)) {
                resizeWindowDp(1000, height)
                waitFor(hasText("What time is it?"))
                compose.onNode(hasText("Show the answer") and hasClickAction()).assertExists()
                if (compact) {
                    compose.onNodeWithTag("compact_task_controls").assertIsDisplayed()
                } else {
                    compose.onNodeWithTag("compact_task_controls").assertDoesNotExist()
                }
            }
            compose.activityRule.scenario.recreate()
            waitFor(hasText("What time is it?"))
            compose.onNodeWithTag("compact_task_controls").assertIsDisplayed()
            open("Show the answer")
            assertEquals(
                chosen,
                compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription],
            )
            open("Swap turns")
            waitFor(hasContentDescription("Player 2 sets the time", substring = true))
        } finally {
            shell("wm size $originalSize")
        }
    }

    @Test
    fun scrollPositionSurvivesShortWindowAndActivityRecreation() {
        val originalSize = currentWindowSize()
        try {
            resizeWindowDp(320, 800)
            open("Settings")
            val scrolling = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
            val node = compose.onNode(scrolling)
            node.performTouchInput { swipeUp(durationMillis = 600) }
            compose.waitForIdle()
            val offset = node.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("Settings is scrolled before resizing", offset > 0f)
            for (height in listOf(600, 300)) {
                resizeWindowDp(320, height)
                compose.waitForIdle()
                val restored = node.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
                assertEquals("Scroll retained at height $height", offset, restored, 1f)
            }
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            assertEquals(
                offset,
                node.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value(),
                1f,
            )
        } finally {
            shell("wm size $originalSize")
        }
    }

    @Test
    fun nameDraftAndSelectionSurviveActivityRecreation() {
        open("My Companion")
        open("Choose an outfit")
        val field = compose.onNode(hasSetTextAction()).scrollToIfScrollable()
        field.performTextReplacement("Resize draft ")
        field.performTextInputSelection(TextRange(1, 5))
        compose.activityRule.scenario.recreate()
        waitFor(hasSetTextAction())
        val restored = compose.onNode(hasSetTextAction()).fetchSemanticsNode().config
        assertEquals("Resize draft ", restored[SemanticsProperties.EditableText].text)
        assertEquals(TextRange(1, 5), restored[SemanticsProperties.TextSelectionRange])
    }

    @Test
    fun mouseCanNavigateDragClockAndScrollSettings() {
        val explore = compose.onNode(hasText("Explore") and hasClickAction()).scrollToIfScrollable()
        mouse(MotionEvent.ACTION_DOWN, explore.fetchSemanticsNode().boundsInWindow.center)
        mouse(MotionEvent.ACTION_UP, explore.fetchSemanticsNode().boundsInWindow.center)
        val clock = hasContentDescription("Clock face:", substring = true)
        waitFor(clock)
        val bounds =
            compose
                .onNode(clock)
                .scrollToIfScrollable()
                .fetchSemanticsNode()
                .boundsInWindow
        val before = compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        mouse(MotionEvent.ACTION_DOWN, Offset(bounds.center.x, bounds.center.y - bounds.height * 0.35f))
        repeat(10) { index ->
            val progress = (index + 1) / 10f
            mouse(
                MotionEvent.ACTION_MOVE,
                Offset(
                    bounds.center.x + bounds.width * 0.35f * progress,
                    bounds.center.y - bounds.height * 0.35f * (1 - progress),
                ),
            )
        }
        mouse(MotionEvent.ACTION_UP, Offset(bounds.center.x + bounds.width * 0.35f, bounds.center.y))
        compose.waitForIdle()
        val after = compose.onNode(clock).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertTrue("Mouse moves the clock hand", before != after)
        back()
        open("Settings")
        val scrolling = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        val node = compose.onNode(scrolling)
        val range = node.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        if (range.maxValue() > 0f) {
            mouse(MotionEvent.ACTION_SCROLL, node.fetchSemanticsNode().boundsInWindow.center)
            compose.waitUntil(5_000) {
                node.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f
            }
        }
    }

    private fun mouse(
        action: Int,
        point: Offset,
    ) {
        val now = SystemClock.uptimeMillis()
        if (action == MotionEvent.ACTION_DOWN) mouseDownTime = now
        val properties =
            MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_MOUSE
            }
        val coordinates =
            MotionEvent.PointerCoords().apply {
                x = point.x
                y = point.y
                pressure = if (action == MotionEvent.ACTION_UP) 0f else 1f
                if (action == MotionEvent.ACTION_SCROLL) setAxisValue(MotionEvent.AXIS_VSCROLL, -10f)
            }
        val event =
            MotionEvent.obtain(
                mouseDownTime,
                now,
                action,
                1,
                arrayOf(properties),
                arrayOf(coordinates),
                0,
                if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_SCROLL
                ) {
                    0
                } else {
                    MotionEvent.BUTTON_PRIMARY
                },
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_MOUSE,
                0,
            )
        try {
            assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            instrumentation.waitForIdleSync()
            SystemClock.sleep(40)
        } finally {
            event.recycle()
        }
    }

    private fun assertArrangement() {
        compose.waitForIdle()
        val visual = compose.onNodeWithTag("adaptive_visual").fetchSemanticsNode()
        val controls = compose.onNodeWithTag("adaptive_controls").fetchSemanticsNode()
        val width = compose.activity.resources.configuration.screenWidthDp - 32
        val font = compose.activity.resources.configuration.fontScale
        val short = compose.activity.resources.configuration.screenHeightDp < 480
        val compact =
            short && width >= 720 && width >= 160 + 360 * font + 18 &&
                compose.onAllNodes(hasTestTag("compact_task_controls")).fetchSemanticsNodes().isNotEmpty()
        val horizontal = compact || (width >= 720 && width >= 320 + 360 * font + 18)
        // Position is unclipped, so scrolling cannot turn a below-the-fold region into a false overlap.
        val first = visual.positionInRoot
        val second = controls.positionInRoot
        if (horizontal) {
            assertTrue("Controls beside visual at $width/$font", second.x >= first.x + visual.size.width)
        } else {
            assertTrue("Controls below visual at $width/$font", second.y >= first.y + visual.size.height)
        }
    }

    private fun open(label: String) {
        val exact = (hasText(label) or hasContentDescription(label)) and hasClickAction()
        val action =
            if (compose.onAllNodes(exact).fetchSemanticsNodes().isNotEmpty()) {
                exact
            } else {
                hasContentDescription("$label.", true) and hasClickAction()
            }
        waitFor(action)
        compose
            .onNode(action)
            .scrollToIfScrollable()
            .assertIsDisplayed()
            .performClick()
        compose.waitForIdle()
    }

    private fun assertFullyVisibleAction(label: String) {
        val node = compose.onNode(hasText(label) and hasClickAction()).fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val position = node.positionInRoot
        assertTrue(
            "$label must fit without scrolling: $position / ${node.size} / $root",
            position.y >= root.top && position.y + node.size.height <= root.bottom &&
                position.x >= root.left && position.x + node.size.width <= root.right,
        )
    }

    private fun assertCompactControlsVisible() {
        val pane = hasTestTag("compact_task_controls")
        val panes = compose.onAllNodes(pane).fetchSemanticsNodes()
        if (panes.isEmpty() || compose.activity.resources.configuration.fontScale > 1f) return
        var count = 0
        for (tag in listOf("compact_task_controls", "task_actions")) {
            val region = hasTestTag(tag)
            val viewport = compose.onNode(region).fetchSemanticsNode().boundsInRoot
            val actions = compose.onAllNodes(hasClickAction() and hasAnyAncestor(region)).fetchSemanticsNodes()
            count += actions.size
            for (action in actions) {
                val position = action.positionInRoot
                assertTrue(
                    "Entire control must fit in its pane: $position / ${action.size} / $viewport",
                    position.x >= viewport.left && position.y >= viewport.top &&
                        position.x + action.size.width <= viewport.right &&
                        position.y + action.size.height <= viewport.bottom,
                )
            }
        }
        assertTrue("The task exposes actions", count > 0)
        val clock = compose.onNodeWithTag("task_clock").fetchSemanticsNode()
        assertTrue(
            "Clock uses the available height",
            clock.size.height >= 200 * compose.activity.resources.displayMetrics.density,
        )
    }

    private fun back() {
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            LocalizationScreenshots.capture(name, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun resizeWindowDp(
        widthDp: Int,
        heightDp: Int,
    ) {
        val density = compose.activity.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt()
        val heightPx = (heightDp * density).roundToInt()
        shell("wm size ${widthPx}x$heightPx")
        compose.waitUntil(10_000) {
            val decor = compose.activity.window.decorView
            decor.width == widthPx && decor.height == heightPx
        }
    }

    private fun currentWindowSize(): String =
        shell("wm size")
            .lineSequence()
            .firstOrNull { it.startsWith("Override size:") }
            ?.substringAfter(": ")
            ?.trim() ?: "reset"

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.bufferedReader().readText()
        }
}
