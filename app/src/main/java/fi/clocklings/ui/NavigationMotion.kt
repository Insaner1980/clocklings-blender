package fi.clocklings.ui

import androidx.activity.BackEventCompat
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@Composable
fun Modifier.sharedClocklingsElement(key: String): Modifier {
    val scope = LocalSharedTransitionScope.current
    val reduced = rememberReduceMotion()
    if (scope == null || reduced) return this
    return with(scope) {
        sharedElement(
            rememberSharedContentState(key),
            LocalNavAnimatedContentScope.current,
            boundsTransform = { _, _ -> tween(SkMotion.NAV_DURATION) },
        )
    }
}

fun navigationTransition(
    reduced: Boolean,
    back: Boolean = false,
    predictiveEdge: Int? = null,
): ContentTransform {
    if (reduced) return fadeIn(tween(SkMotion.REDUCED_FADE)) togetherWith fadeOut(tween(SkMotion.REDUCED_FADE))
    val direction =
        when {
            predictiveEdge != null -> if (predictiveEdge == BackEventCompat.EDGE_LEFT) -1 else 1
            back -> -1
            else -> 1
        }
    val enter =
        slideInHorizontally(tween(SkMotion.NAV_DURATION)) { direction * it / SkMotion.NAV_SLIDE_FRACTION } +
            fadeIn(tween(SkMotion.NAV_DURATION))
    val exit =
        slideOutHorizontally(tween(SkMotion.NAV_DURATION)) { -direction * it / SkMotion.NAV_SLIDE_FRACTION } +
            fadeOut(tween(SkMotion.NAV_DURATION))
    return enter togetherWith
        if (predictiveEdge == null) {
            exit
        } else {
            exit +
                scaleOut(tween(SkMotion.NAV_DURATION), targetScale = SkMotion.PREDICTIVE_SCALE)
        }
}
