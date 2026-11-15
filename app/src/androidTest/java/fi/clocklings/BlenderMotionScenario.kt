package fi.clocklings

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.isReduceMotion
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File

internal class BlenderMotionScenario(
    private val compose: ComposeContentTestRule,
    private val kind: CompanionKind,
    private val directoryName: String,
) {
    fun captureAccessories(colorForRow: (Int) -> CompanionColor = kind.colorOptions()::get) {
        val items = Wardrobe.itemsFor(kind)
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    items.chunked(4).forEachIndexed { row, group ->
                        Row {
                            group.forEach { item ->
                                CompanionView(
                                    kind,
                                    Outfit.default(kind).with(item.slot, item.id),
                                    Modifier.size(88.dp, 110.dp),
                                    color = colorForRow(row),
                                    animated = false,
                                )
                            }
                        }
                    }
                }
            }
        }
        val file = outputFile("accessories.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun verifyMotion(
        slot: Slot,
        itemId: String,
        modifier: Modifier = Modifier.size(240.dp, 300.dp),
    ) {
        val reduced = isReduceMotion(InstrumentationRegistry.getInstrumentation().targetContext)
        val outfit = Outfit.default(kind)
        val next = outfit.with(slot, itemId)
        val frames = mutableListOf<Bitmap>()

        fun pixels(): IntArray {
            val image = compose.onNodeWithTag("companion_${kind.name}", useUnmergedTree = true).captureToImage()
            frames += image.asAndroidBitmap()
            val map = image.toPixelMap()
            return IntArray(map.width * map.height) { map[it % map.width, it / map.width].toArgb() }
        }

        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme {
                CompanionView(
                    kind,
                    outfit,
                    modifier,
                    color = CompanionColor.ORIGINAL,
                    celebration = CompanionCelebration(next, jump = true),
                )
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val first = pixels()
        compose.mainClock.advanceTimeBy(400L)
        val spin = pixels()
        compose.mainClock.advanceTimeBy(750L)
        val jump = pixels()
        compose.mainClock.advanceTimeBy(3_000L)
        val idle = pixels()
        if (reduced) {
            assertTrue("Reduced motion changed the spin frame", first.contentEquals(spin))
            assertTrue("Reduced motion changed the jump frame", first.contentEquals(jump))
            assertTrue("Reduced motion changed the idle frame", first.contentEquals(idle))
        } else {
            assertFalse("Outfit spin did not render", first.contentEquals(spin))
            assertFalse("Jump did not render", spin.contentEquals(jump))
            assertFalse("Idle did not resume", jump.contentEquals(idle))
            saveMotionFrames(frames)
        }
    }

    private fun saveMotionFrames(frames: List<Bitmap>) {
        val strip =
            Bitmap.createBitmap(
                frames.first().width * frames.size,
                frames.first().height,
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(strip)
        frames.forEachIndexed { index, frame -> canvas.drawBitmap(frame, (index * frame.width).toFloat(), 0f, null) }
        outputFile("motion.png").outputStream().use { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }
        strip.recycle()
    }

    private fun outputFile(name: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), directoryName).apply { mkdirs() }
        return File(directory, name)
    }
}
