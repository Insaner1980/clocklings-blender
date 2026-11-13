package fi.clocklings.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Keeps a failed write and its retry separate from unrelated writes. */
@Stable
internal class RetryableSave {
    var retry by mutableStateOf<(() -> Unit)?>(null)
        private set
    private var request = 0

    fun run(write: ((Boolean) -> Unit) -> Unit) {
        val current = ++request
        retry = null
        write { saved ->
            if (current == request) retry = if (saved) null else ({ run(write) })
        }
    }
}
