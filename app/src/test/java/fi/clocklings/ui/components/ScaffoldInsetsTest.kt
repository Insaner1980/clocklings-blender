package fi.clocklings.ui.components

import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScaffoldInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun restoredContentWaitsInitiallyButSurvivesLaterInsetGrowth() {
        var target = insets(48, 64)
        val insetView =
            object : View(compose.activity) {
                override fun getRootWindowInsets(): WindowInsets = checkNotNull(target.toWindowInsets())
            }
        val viewport = mutableStateOf(411 to 1f)
        val scroll = ScrollState(80)
        var entries = 0
        var disposals = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalView provides insetView,
                LocalDensity provides Density(2f, viewport.value.second),
            ) {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                    ScreenScaffold(scrollState = scroll) {
                        DisposableEffect(Unit) {
                            entries++
                            onDispose { disposals++ }
                        }
                        Text("Retained content")
                        Spacer(Modifier.height(2_000.dp))
                    }
                }
            }
        }
        compose.runOnIdle { compose.activity.addContentView(insetView, ViewGroup.LayoutParams(1, 1)) }
        compose.onNodeWithText("Retained content").assertDoesNotExist()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(insetView, target) }
        compose.onNodeWithText("Retained content").assertExists()
        compose.runOnIdle { assertEquals(80, scroll.value) }
        for ((index, cell) in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f).withIndex()) {
            compose.runOnIdle {
                viewport.value = cell
                target = insets(96 + index * 20, 128 + index * 20)
                // During a bar animation, Compose can receive smaller current insets than the root target.
                ViewCompat.dispatchApplyWindowInsets(insetView, insets(60 + index * 20, 80 + index * 20))
            }
            compose.onNodeWithText("Retained content").assertExists()
            compose.runOnIdle {
                assertEquals("Content must not re-enter at $cell", 1, entries)
                assertEquals("Content must not be disposed at $cell", 0, disposals)
                assertEquals(80, scroll.value)
                ViewCompat.dispatchApplyWindowInsets(insetView, target)
            }
        }
    }

    private fun insets(
        top: Int,
        bottom: Int,
    ): WindowInsetsCompat =
        WindowInsetsCompat
            .Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, top, 0, bottom))
            .build()
}
