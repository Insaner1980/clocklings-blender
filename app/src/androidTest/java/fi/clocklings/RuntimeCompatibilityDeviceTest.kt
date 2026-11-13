package fi.clocklings

import android.os.Build
import android.os.Bundle
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Load both shipped native dependencies inside the actual application test process. */
@RunWith(AndroidJUnit4::class)
class RuntimeCompatibilityDeviceTest {
    @Test
    fun packagedNativeLibrariesLoadAtTheExpectedRuntimePageSize() {
        val expected = InstrumentationRegistry.getArguments().getString("pageSize")?.toLong()
        val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
        if (expected != null) assertEquals(expected, pageSize)
        System.loadLibrary("androidx.graphics.path")
        System.loadLibrary("datastore_shared_counter")
        InstrumentationRegistry.getInstrumentation().sendStatus(
            2,
            Bundle().apply {
                putString("stream", "API=${Build.VERSION.SDK_INT} pageSize=$pageSize both native libraries loaded\n")
            },
        )
    }
}
