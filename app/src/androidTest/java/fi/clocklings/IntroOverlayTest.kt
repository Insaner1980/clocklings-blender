package fi.clocklings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.intro.IntroOverlay
import fi.clocklings.ui.intro.IntroTimeline
import fi.clocklings.ui.intro.LogoMascot
import fi.clocklings.ui.intro.coveredByIntro
import fi.clocklings.ui.isReduceMotion
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tickPlaysOnceDuringTheGreetingAndTheIntroFinishes() {
        assumeFalse(isReduceMotion(InstrumentationRegistry.getInstrumentation().targetContext))
        var ticks = 0
        var ticksAtDone = -1
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme {
                IntroOverlay(onTick = { ticks++ }, onDone = { ticksAtDone = ticks })
            }
        }
        compose.mainClock.advanceTimeBy(IntroTimeline.TICK_AT_MS / 2)
        compose.runOnIdle { assertEquals(0, ticks) }
        compose.mainClock.advanceTimeBy(IntroTimeline.TICK_AT_MS / 2 + 32L)
        compose.runOnIdle { assertEquals(1, ticks) }
        LocalizationScreenshots.capture("intro", compose.onRoot().captureToImage().asAndroidBitmap())
        // Including frame alignment, these steps allow only 1.3 seconds for startup.
        compose.mainClock.advanceTimeBy(300L)
        compose.runOnIdle { assertEquals(1, ticksAtDone) }
    }

    @Test
    fun mascotPartsMoveAndReducedMotionKeepsTheSameImage() {
        var time by mutableLongStateOf(350L)
        var reduced by mutableStateOf(false)
        compose.setContent {
            ClocklingsTheme { LogoMascot({ time }, reduced, Modifier.size(240.dp)) }
        }
        val first = mascotPixels()
        compose.runOnIdle { time = IntroTimeline.MASCOT_IN_MS + IntroTimeline.MASCOT_JUMP_MS / 2 }
        assertFalse("The waving pose must change the rendered artwork", first.contentEquals(mascotPixels()))
        compose.runOnIdle { reduced = true }
        val still = mascotPixels()
        compose.runOnIdle { time = 890L }
        assertTrue("Reduced motion must keep the same artwork", still.contentEquals(mascotPixels()))
    }

    @Test
    fun reducedMotionFinishesWithoutTicking() {
        org.junit.Assume.assumeTrue(isReduceMotion(InstrumentationRegistry.getInstrumentation().targetContext))
        var ticks = 0
        var done = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme { IntroOverlay(onTick = { ticks++ }, onDone = { done++ }) }
        }
        LocalizationScreenshots.capture("intro-still", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.mainClock.advanceTimeBy(IntroTimeline.STILL_MS + 100L)
        compose.runOnIdle {
            assertEquals(0, ticks)
            assertEquals(1, done)
        }
    }

    private fun mascotPixels(): IntArray {
        val image = compose.onNodeWithTag("logo_mascot").captureToImage().toPixelMap()
        return IntArray(image.width * image.height) { index ->
            image[index % image.width, index / image.width].toArgb()
        }
    }

    @Test
    fun launcherAndSystemSplashUseTheMascotResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue(icon is AdaptiveIconDrawable)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        try {
            icon.setBounds(0, 0, bitmap.width, bitmap.height)
            icon.draw(Canvas(bitmap))
            LocalizationScreenshots.capture("launcher", bitmap)
        } finally {
            bitmap.recycle()
        }
        assertTrue(context.getDrawable(R.drawable.ic_splash_animated) is AnimatedVectorDrawable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val theme = context.resources.newTheme().apply { applyStyle(R.style.Theme_Clocklings, true) }
            val value = android.util.TypedValue()
            assertTrue(theme.resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon, value, true))
            assertEquals(R.drawable.ic_splash_animated, value.resourceId)
            assertTrue(theme.resolveAttribute(android.R.attr.windowSplashScreenIconBackgroundColor, value, true))
            assertEquals(context.getColor(R.color.icon_background), value.data)
        }
    }

    @Test
    fun tapSkipsTheIntro() {
        var done = 0
        // Kello pysäytetään ennen koostamista, jotta intro ei ehdi päättyä itsestään.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme { IntroOverlay(onTick = {}, onDone = { done++ }) }
        }
        compose.onNodeWithContentDescription("Clocklings", substring = true).performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test
    fun contentUnderTheIntroIsHiddenFromAccessibilityUntilItEnds() {
        var covered by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.coveredByIntro(covered)) {
                Button(onClick = {}) { Text("Play") }
            }
        }
        compose.onNodeWithText("Play").assertDoesNotExist()
        covered = false
        compose.onNodeWithText("Play").assertExists()
    }
}
