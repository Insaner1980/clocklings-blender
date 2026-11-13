package fi.clocklings

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.theme.SkColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import java.io.File

/** Shared setup and native input assertions for the dedicated emulator tests. */
@OptIn(ExperimentalTestApi::class)
abstract class InputDeviceTest {
    protected val compose = createAndroidComposeRule<MainActivity>()
    internal val environment = LocaleTestEnvironment()
    protected val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var originalProgress: Progress
    private lateinit var originalSettings: Settings
    protected open val requireCutout = false

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    protected open fun initialProgress() = Progress()

    @Before
    fun prepareSyntheticData() {
        runBlocking {
            originalProgress = environment.repository.progress.first()
            originalSettings = environment.repository.settings.first()
            environment.repository.updateProgress { initialProgress() }
            environment.repository.updateSettings { Settings(sound = false) }
        }
        waitFor(hasText("Clocklings"))
        if (requireCutout && InstrumentationRegistry.getArguments().getString("requireCutout") == "true") {
            compose.runOnIdle {
                val cutout = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.displayCutout
                val hasCutout = cutout != null && cutout.safeInsetTop > 0
                assertTrue("Nonempty app-visible cutout required: $cutout", hasCutout)
            }
        }
    }

    @After
    fun restoreSyntheticData() {
        runBlocking {
            environment.repository.updateProgress { originalProgress }
            environment.repository.updateSettings { originalSettings }
        }
    }

    protected fun focusEvidence(
        label: String,
        matcher: SemanticsMatcher,
        inkBorder: Boolean = false,
        directoryName: String = "audit-local-focus",
        move: () -> Unit,
    ) {
        val node = compose.onNode(matcher).scrollToIfScrollable()
        val before = pixels(node.captureToImage())
        move()
        node.assertIsFocused().assertIsDisplayed()
        val image = node.captureToImage()
        val after = pixels(image)
        assertEquals("$label capture width/height must stay stable", before.size, after.size)
        val changed = before.indices.count { before[it] != after[it] }
        val ink = SkColors.Ink.toArgb()
        val extraInk = after.count { it == ink } - before.count { it == ink }
        val directory =
            File(
                checkNotNull(environment.context.getExternalFilesDir(null)),
                directoryName,
            ).apply { mkdirs() }
        val cell = InstrumentationRegistry.getArguments().getString("cell", "unknown")
        File(directory, "$cell-$label.png").outputStream().use {
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        File(directory, "$cell-pixels.txt").appendText("$label: changed=$changed extraInk=$extraInk\n")
        if (label == "reset-row") {
            // Focus darkens the red text; a fade instead washes it toward the background.
            val textPixels = after.count { (it ushr 16 and 0xff) - maxOf(it ushr 8 and 0xff, it and 0xff) >= 120 }
            assertTrue("Focused reset label must remain readable: $textPixels pixels", textPixels > 20)
        }
        assertTrue(
            "$label rendered focus: changed=$changed, extraInk=$extraInk",
            changed > 20 && (!inkBorder || extraInk > 20),
        )
    }

    private fun pixels(image: ImageBitmap): IntArray =
        IntArray(image.width * image.height).also { image.readPixels(it) }

    protected open fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    protected fun focus(matcher: SemanticsMatcher) = compose.focusWithTab(matcher, ::key)

    protected fun activate(matcher: SemanticsMatcher) {
        waitFor(matcher)
        focus(matcher)
        compose.onNode(matcher).assertIsDisplayed()
        key(KeyEvent.KEYCODE_ENTER)
    }

    protected fun click(matcher: SemanticsMatcher) {
        waitFor(matcher)
        compose.onAllNodes(matcher)[0].scrollToIfScrollable().performClick()
    }

    protected fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(10_000) {
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun waitForImeLayout(field: SemanticsNodeInteraction) {
        compose.waitUntil(5_000) {
            val node = field.fetchSemanticsNode()
            val bounds = node.boundsInWindow
            var visible = false
            instrumentation.runOnMainSync {
                val decor = compose.activity.window.decorView
                val insets = ViewCompat.getRootWindowInsets(decor)
                val bottom = insets?.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars())
                visible = bottom != null && bounds.height >= node.size.height - 1 &&
                    bounds.bottom <= decor.height - bottom.bottom
            }
            visible
        }
    }
}

internal fun ComposeTestRule.focusWithTab(
    matcher: SemanticsMatcher,
    key: (Int) -> Unit,
    attempts: Int = 80,
) {
    repeat(attempts) {
        if (onAllNodes(matcher and isFocused()).fetchSemanticsNodes().isNotEmpty()) return
        key(KeyEvent.KEYCODE_TAB)
    }
    error("Keyboard focus did not reach $matcher")
}
