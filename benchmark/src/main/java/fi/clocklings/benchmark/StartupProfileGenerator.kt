package fi.clocklings.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()

    @Test
    fun startup() {
        profile.collect(packageName = PACKAGE_NAME, includeInStartupProfile = false) {
            pressHome()
            openHome()
        }
    }
}
