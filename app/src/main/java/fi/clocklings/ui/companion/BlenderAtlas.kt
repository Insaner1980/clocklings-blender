package fi.clocklings.ui.companion

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import java.lang.ref.WeakReference

/** Share full-resolution atlases between previews without retaining unused bitmaps. */
@Composable
internal fun rememberBlenderAtlas(
    @DrawableRes resource: Int,
): ImageBitmap {
    val resources = LocalResources.current
    return remember(resources, resource) { BlenderAtlas.load(resources, resource) }
}

internal object BlenderAtlas {
    // These fixed drawable-nodpi resources have no density, locale, or theme variants.
    private val images = mutableMapOf<Int, WeakReference<ImageBitmap>>()

    @Synchronized
    fun load(
        resources: Resources,
        @DrawableRes resource: Int,
    ): ImageBitmap {
        images[resource]?.get()?.let { return it }
        val options =
            BitmapFactory.Options().apply {
                inScaled = false
                inMutable = true
                inPremultiplied = false
            }
        val bitmap = checkNotNull(BitmapFactory.decodeResource(resources, resource, options))
        // libwebp and PNG decoding round premultiplied alpha differently on older Android versions.
        // Decode exact straight RGBA, then use Android's ARGB writer (the same rounding as PNG).
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.setPremultiplied(true)
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap.asImageBitmap().also { images[resource] = WeakReference(it) }
    }
}
