package fi.clocklings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fi.clocklings.ui.applyClocklingsSystemBars

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        applyClocklingsSystemBars()
        super.onCreate(savedInstanceState)
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        setContent { ClocklingsApp(onStartupReady = { ready = true }) }
    }

    override fun onResume() {
        super.onResume()
        (application as ClocklingsApplication).container.resume()
    }

    override fun onPause() {
        super.onPause()
        (application as ClocklingsApplication).container.pause()
    }
}
