package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class CropFitTest {
    @Test
    fun `wide crop grows downwards only`() =
        assertEquals(Rect(66f, 158f, 174f, 266f), Rect(66f, 158f, 174f, 238f).grownTo(Size(76f, 76f)))

    @Test
    fun `tall crop still grows on both sides`() =
        assertEquals(Rect(-50f, 0f, 150f, 200f), Rect(0f, 0f, 100f, 200f).grownTo(Size(100f, 100f)))

    @Test
    fun `crop that already has the canvas proportions stays as it is`() =
        assertEquals(Rect(10f, 20f, 110f, 120f), Rect(10f, 20f, 110f, 120f).grownTo(Size(76f, 76f)))
}
