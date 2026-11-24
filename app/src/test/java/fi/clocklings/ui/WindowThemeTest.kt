package fi.clocklings.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 36])
class WindowThemeTest {
    @Test
    @Config(sdk = [28])
    fun preAndroidTenThemesKeepTheSharedWindowAndSplashResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val window = ContextThemeWrapper(context, R.style.Theme_Clocklings)
        val windowAttributes = window.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        try {
            assertEquals(R.color.window_background, windowAttributes.getResourceId(0, 0))
        } finally {
            windowAttributes.recycle()
        }

        val splash = ContextThemeWrapper(context, R.style.Theme_Clocklings_Starting)
        val splashAttributes =
            splash.obtainStyledAttributes(intArrayOf(androidx.core.splashscreen.R.attr.postSplashScreenTheme))
        try {
            assertEquals(R.style.Theme_Clocklings, splashAttributes.getResourceId(0, 0))
        } finally {
            splashAttributes.recycle()
        }
    }

    @Test
    @Config(sdk = [36, 37])
    fun startingThemeRequestsLightSplashBackgroundInNightMode() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_YES }
        val night = application.createConfigurationContext(configuration)
        val attributes =
            ContextThemeWrapper(night, R.style.Theme_Clocklings_Starting)
                .obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
        try {
            assertEquals(0xFFFFF5FA.toInt(), attributes.getColor(0, 0))
        } finally {
            attributes.recycle()
        }
    }

    @Test
    fun startingAndMainThemesKeepClocklingsColorsInSystemDarkMode() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        for (nightMode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val configuration = Configuration().apply { uiMode = nightMode }
            val context = application.createConfigurationContext(configuration)
            val expandedDarkTheme = Build.VERSION.SDK_INT >= 36 && nightMode == Configuration.UI_MODE_NIGHT_YES
            for (theme in listOf(R.style.Theme_Clocklings_Starting, R.style.Theme_Clocklings)) {
                val themeName = context.resources.getResourceEntryName(theme)
                val attributes =
                    ContextThemeWrapper(context, theme).obtainStyledAttributes(
                        intArrayOf(android.R.attr.isLightTheme, android.R.attr.forceDarkAllowed),
                    )
                try {
                    assertFalse("$themeName must disable Force Dark", attributes.getBoolean(1, true))
                    assertEquals(
                        "$themeName must opt out of expanded dark theme in night mode",
                        !expandedDarkTheme,
                        attributes.getBoolean(0, true),
                    )
                } finally {
                    attributes.recycle()
                }
            }
        }
    }
}
