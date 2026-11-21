package fi.clocklings.ui.clock

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
class ClockFaceUpdateTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dayPartNeverErasesTheMinuteHand() {
        val viewport = mutableStateOf(411 to 1f)
        val time = mutableIntStateOf(30)
        val period = mutableIntStateOf(ClockTime.HALF_DAY)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, viewport.value.second)) {
                key(viewport.value, time.intValue, period.intValue) {
                    ClocklingsTheme {
                        ClockFace(
                            ClockCommand(time.intValue),
                            Modifier.size(viewport.value.first.dp).testTag("clock"),
                            period = period.intValue,
                            clockSize = viewport.value.first.dp,
                        )
                    }
                }
            }
        }
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            for (halfPast in listOf(30, 750)) {
                compose.runOnIdle {
                    viewport.value = cell
                    time.intValue = halfPast
                    period.intValue = ClockTime.HALF_DAY
                }
                val bounds = compose.onNodeWithTag("clock").fetchSemanticsNode().boundsInRoot
                val dayPart = timeLanguage(AppLanguage.ENGLISH).dayPart(halfPast).name
                compose.onNodeWithText(dayPart).assertDoesNotExist()
                val baseline = captureClock(view, bounds)
                compose.runOnIdle { period.intValue = ClockTime.DAY }
                val chip = compose.onNodeWithText(dayPart).assertIsDisplayed().fetchSemanticsNode()
                assertTrue(
                    "$cell at $halfPast: the chip must be below the dial",
                    chip.boundsInRoot.top >= bounds.bottom,
                )
                val pixels = captureClock(view, bounds)
                val scale = pixels.width / ClockSpec.VIEW_SIZE
                val x = ((ClockSpec.CENTER - ClockSpec.VIEW_ORIGIN) * scale).roundToInt()
                var maxBlueContrast = 0f
                // Sample the blue minute shaft below the centre cap at half past.
                for (offset in -5..5) {
                    val y = ((ClockSpec.CENTER + 30 + offset - ClockSpec.VIEW_ORIGIN) * scale).roundToInt()
                    val color = pixels[x, y]
                    val expected = baseline[x, y]
                    maxBlueContrast = maxOf(maxBlueContrast, expected.blue - expected.red)
                    assertTrue(
                        "$cell at $halfPast: day part erased the minute hand at $offset: $color",
                        abs(color.red - expected.red) < 0.03f &&
                            abs(color.green - expected.green) < 0.03f &&
                            abs(color.blue - expected.blue) < 0.03f,
                    )
                }
                assertTrue("$cell at $halfPast: baseline must contain the blue minute hand", maxBlueContrast > 0.1f)
            }
        }
    }

    @Test
    fun guided24HourDialHasNoZoneWords() {
        val language = mutableStateOf(AppLanguage.ENGLISH)
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalAppLanguage provides language.value) {
                ClocklingsTheme {
                    ClockFace(
                        ClockCommand(390),
                        Modifier.testTag("clock"),
                        period = ClockTime.DAY,
                        help = true,
                        clockSize = 240.dp,
                    )
                }
            }
        }
        val bounds = compose.onNodeWithTag("clock").fetchSemanticsNode().boundsInRoot
        val english = captureClock(view, bounds)
        // The original Finnish dial never painted the zone words, while the English one did.
        compose.runOnIdle { language.value = AppLanguage.FINNISH }
        val finnish = captureClock(view, bounds)
        val differences =
            (0 until english.width).sumOf { x ->
                (0 until english.height).count { y -> english[x, y] != finnish[x, y] }
            }
        assertEquals("The language must not affect pixels inside the guided 24-hour dial", 0, differences)
    }

    @Test
    fun retainedAnswerUpdatesDoNotSnapTheHandDuringAnActiveDrag() {
        val viewport = mutableStateOf(411 to 1f)
        val command = mutableStateOf(ClockCommand(180))
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(1f, viewport.value.second)) {
                key(viewport.value) {
                    ClocklingsTheme {
                        ClockFace(
                            command.value,
                            Modifier.size(viewport.value.first.dp).testTag("clock"),
                            step = 5,
                            interactive = true,
                            onChange = { command.value = command.value.copy(time = it, ms = 0) },
                        )
                    }
                }
            }
        }
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle {
                viewport.value = cell
                command.value = ClockCommand(180)
            }
            val clock = compose.onNodeWithTag("clock")
            clock.performTouchInput {
                val scale = width / ClockSpec.VIEW_SIZE
                val centre = (ClockSpec.CENTER - ClockSpec.VIEW_ORIGIN) * scale
                down(Offset(centre, centre - 150f * scale))
                // Minute 17 is between the submitted five-minute steps (the answer is 15).
                val angle = Math.toRadians(102.0)
                moveTo(
                    Offset(centre + 150f * scale * sin(angle).toFloat(), centre - 150f * scale * cos(angle).toFloat()),
                )
            }
            compose.runOnIdle { assertEquals(195, command.value.time) }
            val bounds = clock.fetchSemanticsNode().boundsInRoot
            val pixels = captureClock(view, bounds)
            val scale = pixels.width / ClockSpec.VIEW_SIZE
            val centre = (ClockSpec.CENTER - ClockSpec.VIEW_ORIGIN) * scale
            val angle = Math.toRadians(102.0)
            // Sample beyond the shorter hour hand and outside the numeral at radius 110.
            val x = (centre + 130f * scale * sin(angle).toFloat()).roundToInt()
            val y = (centre - 130f * scale * cos(angle).toFloat()).roundToInt()
            val color = pixels[x, y]
            assertTrue(
                "$cell: minute hand jumped from the pointer position to the rounded answer; " +
                    "sample=$color expected=${SkColors.Minute} size=${pixels.width}x${pixels.height}",
                abs(color.red - SkColors.Minute.red) < 0.16f &&
                    abs(color.green - SkColors.Minute.green) < 0.16f &&
                    abs(color.blue - SkColors.Minute.blue) < 0.16f,
            )
            clock.performTouchInput { up() }
            compose.runOnIdle { command.value = ClockCommand(240) }
            val description = clock.fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            assertTrue(description.single().contains(timeLanguage(AppLanguage.ENGLISH).fullWords(240, false)))
        }
    }

    private fun captureClock(
        view: View,
        bounds: Rect,
    ) = compose.runOnIdle {
        // Draw current Compose content synchronously; PixelCopy may retain an older surface.
        val bitmap =
            Bitmap.createBitmap(
                bounds.width.roundToInt(),
                bounds.height.roundToInt(),
                Bitmap.Config.ARGB_8888,
            )
        try {
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left, -bounds.top)
            view.draw(canvas)
            bitmap.asImageBitmap().toPixelMap()
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun disablingInteractivityDuringDragStopsFurtherClockChanges() {
        val interactive = mutableStateOf(true)
        val command = ClockCommand(180)
        var value = 180
        compose.setContent {
            ClocklingsTheme {
                ClockFace(
                    command,
                    Modifier.size(300.dp).testTag("clock"),
                    interactive = interactive.value,
                    onChange = { value = it },
                )
            }
        }
        compose.onNodeWithTag("clock").performTouchInput { down(Offset(center.x, height * 0.15f)) }
        compose.runOnIdle { interactive.value = false }
        val before = value
        compose.onNodeWithTag("clock").performTouchInput {
            moveTo(Offset(width * 0.85f, center.y))
            up()
        }
        compose.runOnIdle { assertEquals("CK-06: disabled clock accepted a continuing drag", before, value) }
    }

    @Test
    fun `immediate mini clock updates its hands without a second draw`() {
        val command = mutableStateOf(ClockCommand(0))
        var draws = 0
        compose.setContent {
            ClocklingsTheme {
                ClockFace(
                    command.value,
                    Modifier.size(200.dp).testTag("clock").drawWithContent {
                        draws++
                        drawContent()
                    },
                    mini = true,
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            draws = 0
            command.value = ClockCommand(15)
        }
        compose.waitForIdle()
        val description =
            compose.onNodeWithTag("clock").fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertTrue(description.single().contains(timeLanguage(AppLanguage.ENGLISH).fullWords(15, h24 = false)))
        assertEquals(1, draws)
        val pixels = compose.onNodeWithTag("clock").captureToImage().toPixelMap()
        val scale = pixels.width / ClockSpec.MINI_VIEW_SIZE
        // At minute 15 the blue shaft points right, clear of the centre pin and numeral 3.
        val x = ((ClockSpec.CENTER + 60f - ClockSpec.MINI_VIEW_ORIGIN) * scale).roundToInt()
        val y = ((ClockSpec.CENTER - ClockSpec.MINI_VIEW_ORIGIN) * scale).roundToInt()
        var handPixels = 0
        for (px in x - 1..x + 1) {
            for (py in y - 1..y + 1) {
                val color = pixels[px, py]
                if (abs(color.red - SkColors.Minute.red) < 0.16f &&
                    abs(color.green - SkColors.Minute.green) < 0.16f &&
                    abs(color.blue - SkColors.Minute.blue) < 0.16f
                ) {
                    handPixels++
                }
            }
        }
        assertTrue("The minute hand was not drawn pointing right after the update", handPixels > 0)
    }
}
