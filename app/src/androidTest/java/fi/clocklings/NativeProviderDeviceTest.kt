package fi.clocklings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.SystemClock
import android.provider.Settings.Secure
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/** Requires the test APK's IME and TTS engine selected on a dedicated emulator. */
@RunWith(AndroidJUnit4::class)
class NativeProviderDeviceTest : InputDeviceTest() {
    private var request: Any? = null
    private var originalFlags = 0
    private var trace: InputSessionTrace? = null
    private val expectedIme get() =
        InstrumentationRegistry.getArguments().getString("expectedIme")
            ?: "fi.clocklings.test/fi.clocklings.fixture.AuditInputMethodService"

    @Before
    fun prepare() {
        val automation = instrumentation.uiAutomation
        originalFlags = automation.serviceInfo.flags
        automation.serviceInfo =
            automation.serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        assumeTrue(
            "Dedicated native fixtures must be explicitly selected",
            InstrumentationRegistry.getArguments().getString("nativeProviders") == "true",
        )
        assertEquals(
            expectedIme,
            Secure.getString(environment.context.contentResolver, "default_input_method"),
        )
        assertEquals(
            "fi.clocklings.test",
            Secure.getString(environment.context.contentResolver, "tts_default_synth"),
        )
        waitFor(hasText("Clocklings"))
    }

    @After
    fun restore() {
        var events = emptyList<String>()
        instrumentation.runOnMainSync { events = trace?.finish().orEmpty() }
        events.forEach(::record)
        instrumentation.uiAutomation.serviceInfo =
            instrumentation.uiAutomation.serviceInfo.apply {
                flags = originalFlags
            }
    }

    @Test
    fun nativeCompositionReplacementCommitAndCancellationPersistOnlyTheFinalName() {
        openField()
        startTrace()
        tapIme("Compose")
        expectValue("Ka", TextRange(0, 2))
        tapIme("Replace")
        expectValue("Kati", TextRange(0, 4))
        tapIme("Commit")
        expectValue("Kati", null)
        tapIme("Append")
        expectValue("Katix", TextRange(4, 5))
        tapIme("Cancel")
        expectValue("Kati", null)
        tapIme("Done")
        runBlocking {
            withTimeout(5_000) {
                environment.repository.progress.first { it.nameFor(CompanionKind.HUMAN) == "Kati" }
            }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { fieldText() == "Kati" }
        compose.onNode(hasSetTextAction()).scrollToIfScrollable().assertIsDisplayed()
    }

    @Test
    fun nativeCompositionUsesCurrentRequestAfterReentry() {
        openField()
        startTrace()
        tapIme("Compose")
        expectValue("Ka", TextRange(0, 2))
        tapIme("Commit")
        expectValue("Ka", null)
        val first = checkNotNull(request)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        click(hasText("Choose an outfit") and hasClickAction())
        compose.onNode(hasSetTextAction()).scrollToIfScrollable().performTouchInput { click() }
        chord(KeyEvent.KEYCODE_A)
        tapIme("Compose")
        expectValue("Ka", TextRange(0, 2))
        assertTrue("Reentry must create a different native request", first !== request)
        var stale: TextFieldValue? = null
        instrumentation.runOnMainSync { stale = readField(first, "state") as TextFieldValue }
        record("Retained request=$stale; active request=${nativeValue()}")
        assertEquals("Ka", stale?.text)
        assertEquals(null, stale?.composition)
        instrumentation.runOnMainSync {
            trace?.finish()?.let { events ->
                for (observed in listOf(first, checkNotNull(request))) {
                    assertTrue(
                        "Trace must capture composing commands on both native requests",
                        events.any {
                            it.contains("commands=[SetComposingTextCommand(text='Ka'") &&
                                it.contains("before request=${System.identityHashCode(observed)} ")
                        },
                    )
                }
            }
        }
    }

    @Test
    fun nativeCompositionUndoRedoUsesTheFieldHistory() {
        openField()
        startTrace()
        tapIme("Compose")
        expectValue("Ka", TextRange(0, 2))
        chord(KeyEvent.KEYCODE_Z)
        expectValue("", null)
        chord(KeyEvent.KEYCODE_Z, shift = true)
        expectValue("Ka", null)
        tapIme("Done")
        runBlocking {
            withTimeout(5_000) {
                environment.repository.progress.first { it.nameFor(CompanionKind.HUMAN) == "Ka" }
            }
        }
    }

    @Test
    fun gboardProviderCompositionHasNativeCommandEvidence() {
        assumeTrue("Explicit Gboard diagnostic only", expectedIme.startsWith("com.google.android.inputmethod.latin/"))
        openField()
        startTrace()
        var sawComposition = false
        // Retain the archived NameEditingProbe's C/a/t/Done sequence and composing-span criterion.
        for (text in listOf("c", "ca", "cat")) {
            tapGboardKey(text.last().toString())
            compose.waitUntil(5_000) { fieldText().equals(text, ignoreCase = true) }
            val value = nativeValue()
            record("Gboard provider text=${fieldText()} native=$value")
            assertEquals(text, value.text.lowercase())
            value.composition?.let {
                sawComposition = true
                assertEquals(TextRange(0, text.length), it)
            }
            assertTrue(compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.Focused])
        }
        tapGboardKey("Done")
        compose.waitUntil(5_000) { nativeValue().composition == null }
        runBlocking {
            withTimeout(5_000) {
                environment.repository.progress.first {
                    it.nameFor(CompanionKind.HUMAN).equals("cat", ignoreCase = true)
                }
            }
        }
        assertTrue("Provider keys did not expose a composing span", sawComposition)
    }

    private fun tapGboardKey(label: String) {
        // Verified key IDs in the explicitly selected Gboard 18.5.6 English layout.
        val keyId =
            when (label.lowercase()) {
                "c" -> "B03"
                "a" -> "C01"
                "t" -> "D05"
                "done" -> "key_pos_ime_action"
                else -> error("Unsupported diagnostic key: $label")
            }
        var target: AccessibilityNodeInfo? = null
        val previousBounds = Rect()
        var stableSince = SystemClock.uptimeMillis()
        val screen = Rect()
        instrumentation.runOnMainSync {
            compose.activity.window.decorView.display
                .getRectSize(screen)
        }
        compose.waitUntil(5_000) {
            // A suggestion can have the same label; it is not the requested keyboard key.
            target =
                instrumentation.uiAutomation.windows
                    .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                    ?.root
                    ?.findAccessibilityNodeInfosByViewId("com.google.android.inputmethod.latin:id/$keyId")
                    ?.singleOrNull()
            val bounds = Rect()
            target?.getBoundsInScreen(bounds)
            if (bounds != previousBounds) {
                previousBounds.set(bounds)
                stableSince = SystemClock.uptimeMillis()
            }
            target?.isEnabled == true && target?.isVisibleToUser == true && !bounds.isEmpty &&
                screen.contains(bounds) && SystemClock.uptimeMillis() - stableSince >= 300
        }
        val node = checkNotNull(target)
        assertEquals(expectedIme.substringBefore('/'), node.packageName.toString())
        record("Gboard key label=$label id=${node.viewIdResourceName} bounds=$previousBounds")
        val bounds = Rect().also(node::getBoundsInScreen)
        instrumentation.runOnMainSync { trace?.sample("before Gboard touch=$label bounds=$bounds") }
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event =
                MotionEvent.obtain(
                    now,
                    SystemClock.uptimeMillis(),
                    action,
                    bounds.exactCenterX(),
                    bounds.exactCenterY(),
                    0,
                )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun nativeSynthesisStallCancelsOnBackgroundAndNextRequestCompletes() {
        val events = CopyOnWriteArrayList<String>()
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    intent.getStringExtra("event")?.let(events::add)
                }
            }
        val context = environment.context
        val filter = IntentFilter("fi.clocklings.audit.SPEECH_EVENT")
        // Speech events come from the separate test APK's TTS service.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        val services = (environment.context.applicationContext as ClocklingsApplication).container
        try {
            click(hasContentDescription("Settings") and hasClickAction())
            services.refreshSpeech()
            compose.waitUntil(10_000) { services.speechAvailable.value }
            record(
                "native TTS resumed owners=" +
                    services.javaClass
                        .getDeclaredField("resumedActivities")
                        .apply { isAccessible = true }
                        .get(services),
            )
            services.speak("Clocklings stalled request")
            compose.waitUntil(5_000) { "started" in events }
            // Scroll on the same route so navigation cannot cancel the request first.
            val languageRow = compose.onNode(hasContentDescription("Language:", substring = true))
            compose.waitUntil(5_000) { languageRow.scrollToIfScrollable().isDisplayed() }
            assertFalse("released" in events)
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.waitUntil(5_000) { "released" in events }
            assertTrue("stopped" in events)
            assertFalse("timeout" in events)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            services.refreshSpeech()
            compose.waitUntil(10_000) { services.speechAvailable.value }
            assertFalse("completed" in events)
            services.speak("Clocklings recovery request")
            compose.waitUntil(5_000) { "completed" in events }
            assertEquals(1, events.count { it == "started" })
            record("native TTS events: $events")
        } finally {
            record("native TTS final events: $events")
            services.stopSpeech()
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun controlledImeTwoUndoTwoRedo() {
        openField()
        useBareFieldIfRequested()
        val clipboard = compose.runOnIdle { compose.activity.getSystemService(ClipboardManager::class.java) }
        val previous = compose.runOnIdle { clipboard.primaryClip }
        try {
            compose.waitUntil(5_000) {
                instrumentation.uiAutomation.windows.any {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                        it.root?.packageName == expectedIme.substringBefore('/')
                }
            }
            compose.onNode(hasSetTextAction()).scrollToIfScrollable()
            compose.waitForIdle()
            startTrace()
            instrumentation.sendStringSync("CutCopy")
            try {
                compose.waitUntil(5_000) { fieldText() == "CutCopy" }
            } finally {
                record("typed state: ${nativeValue()}")
            }
            chord(KeyEvent.KEYCODE_A)
            assertEquals(TextRange(0, 7), nativeValue().selection)
            chord(KeyEvent.KEYCODE_C)
            compose.waitUntil(5_000) {
                var copied = false
                instrumentation.runOnMainSync {
                    copied = clipboard.primaryClip
                        ?.getItemAt(0)
                        ?.text
                        ?.toString() == "CutCopy"
                }
                copied
            }
            val codes = listOf(KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_Z)
            for (index in 0..5) {
                chord(codes.getOrElse(index) { KeyEvent.KEYCODE_Z }, shift = index >= 4)
                val expected = if (index % 2 == 0) "" else "CutCopy"
                try {
                    compose.waitUntil(5_000) { fieldText() == expected }
                } finally {
                    record("history step=$index expected='$expected' actual='${fieldText()}' native=${nativeValue()}")
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                restoreClipboard(clipboard, previous)
                if (Build.VERSION.SDK_INT >= 28) {
                    assertEquals("Clipboard presence must be restored", previous != null, clipboard.hasPrimaryClip())
                    record("Clipboard restored: before=${previous != null}, after=${clipboard.hasPrimaryClip()}")
                }
            }
        }
    }

    private fun openField() {
        click(hasContentDescription("My Companion") and hasClickAction())
        click(hasText("Choose an outfit") and hasClickAction())
        compose.onNode(hasSetTextAction()).scrollToIfScrollable().performTouchInput { click() }
    }

    private fun useBareFieldIfRequested() {
        if (InstrumentationRegistry.getArguments().getString("historyBaseline") == "true") {
            instrumentation.runOnMainSync {
                compose.activity.setContent {
                    val value = remember { mutableStateOf("") }
                    val view = LocalView.current
                    val intercept = InstrumentationRegistry.getArguments().getString("historyPreIme") == "true"
                    BasicTextField(
                        value.value,
                        { value.value = it },
                        modifier =
                            Modifier.onPreInterceptKeyBeforeSoftKeyboard { event ->
                                val native = event.nativeKeyEvent
                                if (intercept && native.isCtrlPressed && native.keyCode == KeyEvent.KEYCODE_Z) {
                                    trace?.sample(
                                        "preIME before key=${native.keyCode} " +
                                            "action=${native.action} meta=${native.metaState}",
                                    )
                                    val handled = view.dispatchKeyEvent(native)
                                    trace?.sample("preIME after handled=$handled")
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                    )
                }
            }
            compose.onNode(hasSetTextAction()).performTouchInput { click() }
        }
    }

    private fun expectValue(
        text: String,
        composition: TextRange?,
    ) {
        try {
            compose.waitUntil(5_000) {
                val value = nativeValue()
                value.text == text && value.composition == composition && fieldText() == text
            }
        } finally {
            record("expected text='$text' composition=$composition; native field: ${nativeValue()}")
        }
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        assertTrue(compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsProperties.Focused])
        val bounds = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInWindow
        instrumentation.runOnMainSync {
            val visible = Rect()
            val location = IntArray(2)
            val decor = compose.activity.window.decorView
            decor.getWindowVisibleDisplayFrame(visible)
            decor.getLocationOnScreen(location)
            assertTrue(
                "Field below visible IME boundary: $bounds / $visible",
                bounds.bottom + location[1] <= visible.bottom + 1,
            )
        }
    }

    private fun nativeValue(): TextFieldValue {
        var value: TextFieldValue? = null
        var replaced = false
        instrumentation.runOnMainSync {
            val mutex = readField(composeView(compose.activity.window.decorView), "textInputSessionMutex")
            val session = readField(checkNotNull((mutex as AtomicReference<*>).get()), "value")
            val methodMutex = readField(session, "methodSessionMutex") as AtomicReference<*>
            val active = readField(readField(checkNotNull(methodMutex.get()), "value"), "request")
            replaced = request != null && request !== active
            request = active
            value = readField(checkNotNull(request), "state") as TextFieldValue
            trace?.sample("observe")
        }
        if (replaced) record("Observed active native input request replacement")
        return checkNotNull(value)
    }

    private fun readField(
        value: Any,
        name: String,
    ): Any =
        checkNotNull(
            value.javaClass
                .getDeclaredField(name)
                .apply { isAccessible = true }
                .get(value),
        )

    private fun composeView(view: View): View {
        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") return view
        var found: View? = null
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                found = runCatching { composeView(view.getChildAt(index)) }.getOrNull()
                if (found != null) break
            }
        }
        return checkNotNull(found) { "No Compose input view" }
    }

    private fun tapIme(label: String) {
        var target: AccessibilityNodeInfo? = null
        val previousBounds = Rect()
        var stableSince = SystemClock.uptimeMillis()
        val screen = Rect()
        instrumentation.runOnMainSync {
            compose.activity.window.decorView.display
                .getRectSize(screen)
        }
        try {
            compose.waitUntil(5_000) {
                target =
                    instrumentation.uiAutomation.windows
                        .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                        ?.root
                        ?.findAccessibilityNodeInfosByText(
                            label,
                        )?.firstOrNull {
                            it.text?.toString().equals(label, ignoreCase = true) && it.isEnabled && it.isVisibleToUser
                        }
                val bounds = Rect()
                target?.getBoundsInScreen(bounds)
                if (bounds != previousBounds) {
                    previousBounds.set(bounds)
                    stableSince = SystemClock.uptimeMillis()
                }
                // Accessibility visibility can be true while the IME is still animating below the screen.
                target != null && !bounds.isEmpty && screen.contains(bounds) &&
                    SystemClock.uptimeMillis() - stableSince >= 300
            }
        } finally {
            record("IME readiness label=$label bounds=$previousBounds screen=$screen")
            captureImeIfRequested(label)
        }
        val node = checkNotNull(target)
        assertEquals("fi.clocklings.test", node.packageName.toString())
        // Attach to a replacement session before dispatch, not after its first native command.
        instrumentation.runOnMainSync { trace?.sample("before IME label=$label") }
        if (InstrumentationRegistry.getArguments().getString("imeTouch") == "true") {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            record("IME touch label=$label bounds=$bounds")
            AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}"),
            ).use { it.readBytes() }
        } else {
            assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        compose.waitForIdle()
    }

    private fun captureImeIfRequested(label: String) {
        if (label == "Append" && InstrumentationRegistry.getArguments().getString("inputScreenshot") == "true") {
            File(environment.context.cacheDir, "audit-input.png").outputStream().use {
                instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun chord(
        code: Int,
        shift: Boolean = false,
    ) {
        instrumentation.runOnMainSync { trace?.sample("before key=$code shift=$shift") }
        if (InstrumentationRegistry.getArguments().getString("historyShell") == "true") {
            val modifiers = if (shift) "113 59" else "113"
            AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("input keycombination $modifiers $code"),
            ).use { it.readBytes() }
            compose.waitForIdle()
            instrumentation.runOnMainSync { trace?.sample("after key=$code shift=$shift") }
            return
        }
        val meta = KeyEvent.META_CTRL_ON or if (shift) KeyEvent.META_SHIFT_ON else 0
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            instrumentation.sendKeySync(KeyEvent(0, SystemClock.uptimeMillis(), action, code, 0, meta))
        }
        compose.waitForIdle()
        instrumentation.runOnMainSync { trace?.sample("after key=$code shift=$shift") }
    }

    private fun startTrace() {
        if (InstrumentationRegistry.getArguments().getString("inputTrace") == "true") {
            instrumentation.runOnMainSync {
                trace = InputSessionTrace(compose.activity.window)
                trace?.sample("start")
            }
        }
    }

    private fun fieldText(): String =
        compose
            .onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    private fun record(message: String) {
        instrumentation.sendStatus(2, Bundle().apply { putString("stream", "$message\n") })
    }
}

internal fun restoreClipboard(
    clipboard: ClipboardManager,
    previous: ClipData?,
) {
    when {
        previous != null -> clipboard.setPrimaryClip(previous)
        Build.VERSION.SDK_INT >= 28 -> clipboard.clearPrimaryClip()
        else -> clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}
