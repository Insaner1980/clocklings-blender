package fi.clocklings

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.companion.BlenderAtlas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Compare Android's actual PNG and WebP decoders, including transparent edge pixels. */
@RunWith(AndroidJUnit4::class)
class BlenderAssetIntegrityDeviceTest {
    @Test
    fun everyPackagedAtlasMatchesItsOriginalPixels() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val context = instrumentation.targetContext
        val originals = assets.list("").orEmpty().filter { "blender" in it && it.endsWith(".png") }
        assertEquals("Every character/color/accessory atlas must be checked", 42, originals.size)
        for (name in originals) {
            val resource = context.resources.getIdentifier(name.removeSuffix(".png"), "drawable", context.packageName)
            assertTrue("Missing Android resource for $name", resource != 0)
            val original = assets.open(name).use { checkNotNull(BitmapFactory.decodeStream(it)) }
            val atlas = BlenderAtlas.load(context.resources, resource)
            val packaged = atlas.asAndroidBitmap()
            try {
                assertEquals("Width changed: $name", original.width, packaged.width)
                assertEquals("Height changed: $name", original.height, packaged.height)
                assertTrue("Android decoded pixels changed: $name", original.sameAs(packaged))
                assertSame(
                    "Visible previews must share their atlas: $name",
                    atlas,
                    BlenderAtlas.load(context.resources, resource),
                )
            } finally {
                original.recycle()
            }
        }
    }
}
