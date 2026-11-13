package fi.clocklings

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Vaalea ulkoasu aina: tummat kuvakkeet tila- ja navigointipalkissa myös tummassa tilassa.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { ClocklingsApp() }
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
