package fi.clocklings

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Optional rendered evidence; ordinary instrumentation runs create no image files. */
object LocalizationScreenshots {
    fun capture(
        name: String,
        rendered: Bitmap? = null,
    ) {
        val prefix =
            InstrumentationRegistry
                .getArguments()
                .getString("screenshotPrefix")
                ?.takeIf { it.isNotBlank() } ?: return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "localization")
        check(directory.isDirectory || directory.mkdirs()) { "Could not create screenshot directory" }
        val filename = "$prefix-$name".replace(Regex("[^A-Za-z0-9_-]"), "_")
        val bitmap =
            rendered ?: checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot unavailable" }
        try {
            File(directory, "$filename.png").outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode screenshot" }
            }
        } finally {
            if (rendered == null) bitmap.recycle()
        }
    }
}
