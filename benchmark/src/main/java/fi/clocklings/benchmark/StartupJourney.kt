package fi.clocklings.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "fi.clocklings.blender"

/** The dedicated English emulator exposes Home only after loading and the welcome overlay finish. */
internal fun MacrobenchmarkScope.openHome() {
    startActivityAndWait()
    check(device.wait(Until.hasObject(By.text("Learn")), 15_000)) {
        "Home was not usable after startup; use the dedicated English test emulator"
    }
}
