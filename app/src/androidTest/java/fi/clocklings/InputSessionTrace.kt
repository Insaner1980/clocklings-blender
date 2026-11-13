package fi.clocklings

import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.text.input.EditCommand
import androidx.lifecycle.LifecycleOwner
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

/** Opt-in, test-only observation of the exact cached Compose 1.12.1 legacy input implementation. */
internal class InputSessionTrace(
    private val window: Window,
) {
    private val events = mutableListOf<String>()
    private val callbacks = IdentityHashMap<Any, Any>()
    private var firstRequest: Any? = null
    private val originalCallback = window.callback

    init {
        window.callback =
            object : Window.Callback by originalCallback {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    sample(
                        "window before key=${event.keyCode} action=${event.action} " +
                            "meta=${event.metaState} flags=${event.flags}",
                    )
                    val handled = originalCallback.dispatchKeyEvent(event)
                    sample("window after key=${event.keyCode} handled=$handled")
                    return handled
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    originalCallback.onWindowFocusChanged(hasFocus)
                    sample("window focus=$hasFocus")
                }
            }
    }

    fun sample(stage: String) {
        val request = currentRequest()
        if (request == null) {
            events += "${SystemClock.uptimeMillis()} $stage no active input request"
        } else {
            if (firstRequest == null) firstRequest = request
            if (!callbacks.containsKey(request)) observeCommands(request)
            events +=
                "${SystemClock.uptimeMillis()} $stage ${describe(request)} ${context()}"
        }
    }

    fun finish(): List<String> {
        window.callback = originalCallback
        callbacks.forEach { (request, callback) ->
            request.javaClass
                .getDeclaredField("onEditCommand")
                .apply { isAccessible = true }
                .set(request, callback)
        }
        return events.toList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeCommands(request: Any) {
        val member = request.javaClass.getDeclaredField("onEditCommand").apply { isAccessible = true }
        val original = member.get(request) as (List<EditCommand>) -> Unit
        callbacks[request] = original
        val traced: (List<EditCommand>) -> Unit = { commands ->
            events += "${SystemClock.uptimeMillis()} commands=$commands before ${describe(request)} ${context()}"
            original(commands)
            events += "${SystemClock.uptimeMillis()} commands after ${describe(request)} ${context()}"
        }
        member.set(request, traced)
    }

    private fun context(): String =
        "active=${currentRequest()?.let(System::identityHashCode)} " +
            "windowFocus=${window.decorView.hasWindowFocus()} " +
            "lifecycle=${(window.context as? LifecycleOwner)?.lifecycle?.currentState} " +
            "retained=${firstRequest?.let(::describe)}"

    private fun describe(request: Any): String {
        val manager = field(request, "textFieldSelectionManager")!!
        val undo = field(manager, "undoManager")!!
        val state = field(request, "legacyTextFieldState")!!
        val processor = field(state, "processor")!!
        val buffer = processor.javaClass.getMethod("toTextFieldValue").invoke(processor)
        val connections = field(request, "ics") as List<*>
        val focused = state.javaClass.getMethod("getHasFocus").invoke(state)
        return "request=${System.identityHashCode(request)} state=${field(request, "state")} " +
            "processor=$buffer connections=${connections.size} focused=$focused " +
            "undo=${stack(undo, "undoStack")} redo=${stack(undo, "redoStack")} " +
            "force=${field(undo, "forceNextSnapshot")}"
    }

    private fun stack(
        manager: Any,
        name: String,
    ): List<Any?> {
        val values = mutableListOf<Any?>()
        var entry = field(manager, name)
        while (entry != null) {
            values += field(entry, "value")
            entry = field(entry, "next")
        }
        return values
    }

    private fun currentRequest(): Any? {
        val mutex = field(composeView(window.decorView), "textInputSessionMutex") as AtomicReference<*>
        val session = mutex.get()?.let { field(it, "value") } ?: return null
        val method = field(session, "methodSessionMutex") as AtomicReference<*>
        return method.get()?.let { field(field(it, "value")!!, "request") }
    }

    private fun field(
        value: Any,
        name: String,
    ): Any? =
        value.javaClass
            .getDeclaredField(name)
            .apply { isAccessible = true }
            .get(value)

    private fun composeView(view: View): View {
        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") return view
        var result: View? = null
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                result = runCatching { composeView(view.getChildAt(index)) }.getOrNull()
                if (result != null) break
            }
        }
        return checkNotNull(result)
    }
}
