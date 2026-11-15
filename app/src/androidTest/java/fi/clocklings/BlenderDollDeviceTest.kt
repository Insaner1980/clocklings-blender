package fi.clocklings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlenderDollDeviceTest : BlenderWardrobeDeviceTest(CompanionKind.HUMAN, "blender-doll") {
    @Test
    fun dollOutfitSurvivesRecreation() {
        val configuration = compose.activity.resources.configuration
        val arguments = InstrumentationRegistry.getArguments()
        arguments.getString("widthDp")?.let { width ->
            assertEquals("Actual layout width", width.toInt(), configuration.screenWidthDp)
            assertEquals(
                "Actual font scale",
                requireNotNull(arguments.getString("fontScale")).toFloat(),
                configuration.fontScale,
                0.001f,
            )
        }
        openWardrobe(captureScreens = false)
        val stars = compose.onNodeWithTag("wardrobe_star_balance").fetchSemanticsNode().boundsInRoot
        val companion = companionPreview().fetchSemanticsNode().boundsInRoot
        assertTrue("Star balance covers the doll or its crown", stars.bottom <= companion.top)
        compose.onAllNodesWithTag("color_ORIGINAL").assertCountEquals(0)
        for (item in listOf("Golden hair", "Pink curls", "Rainbow hair", "Brown braids")) selectItem(item)
        val selected = listOf("Overalls", "Crown", "Roller skates", "Strawberry bag")
        for (item in selected) selectItem(item)
        compose.activityRule.scenario.recreate()
        for (item in selected + "Brown braids") waitForItem(item)
        companionPreview().scrollToIfScrollable().assertIsDisplayed()
        save("overalls")
    }
}
