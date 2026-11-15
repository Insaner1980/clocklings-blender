package fi.clocklings.ui.companion

import org.junit.Assert.assertEquals
import org.junit.Test

class WardrobeSectionTest {
    @Test
    fun selectionFollowsTopHeadingsAndShortFinalSection() {
        assertEquals(0, wardrobeSectionIndex(0, 500, listOf(200f, 400f, 600f), 100f))
        assertEquals(1, wardrobeSectionIndex(300, 500, listOf(-100f, 100f, 300f), 100f))
        assertEquals(2, wardrobeSectionIndex(500, 500, listOf(-300f, -100f, 200f), 100f))
        assertEquals(0, wardrobeSectionIndex(0, 0, listOf(200f, 400f, 600f), 100f))
    }

    @Test
    fun tapAlignsAlreadyVisibleHeadingsAndClampsTheLastSection() {
        assertEquals(180, wardrobeScrollTarget(100, 500, 180f, 100f))
        assertEquals(500, wardrobeScrollTarget(100, 500, 900f, 100f))
        assertEquals(0, wardrobeScrollTarget(100, 500, -200f, 100f))
        assertEquals(100, wardrobeScrollTarget(100, 500, 300f, 300f))
    }
}
