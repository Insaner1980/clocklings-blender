package fi.clocklings.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Järjestelmän "Poista animaatiot" -asetus (animaatioiden kesto 0). Toimii API 17:stä alkaen. */
fun isReduceMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember(context) { mutableStateOf(isReduceMotion(context)) }
    DisposableEffect(context) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = isReduceMotion(context)
                }
            }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

/** Shared motion values; callers use an instant change or REDUCED_FADE when motion is disabled. */
object SkMotion {
    const val BADGE_START_SCALE = 0.7f
    val PressSpring = spring<Float>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
    val PopSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    const val PRESS_SCALE = 0.96f
    const val NAV_DURATION = 300
    const val RESIZE_DURATION = 300
    const val REDUCED_FADE = 80
    const val NAV_SLIDE_FRACTION = 4
    const val PREDICTIVE_SCALE = 0.92f
    const val PULSE_DURATION = 1400
    const val PULSE_SCALE = 1.04f
}
