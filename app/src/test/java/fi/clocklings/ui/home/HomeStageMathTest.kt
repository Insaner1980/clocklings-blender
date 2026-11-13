package fi.clocklings.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.theme.SkDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The companion rigs build Android graphics objects, so the aspect ratios need Robolectric.
@RunWith(RobolectricTestRunner::class)
class HomeStageMathTest {
    private val widths = listOf(320.dp, 360.dp, 411.dp, 600.dp)
    private val heights = listOf(200.dp, 320.dp, 360.dp)

    private fun forEveryStage(
        check: (kind: CompanionKind, width: Dp, height: Dp, layout: CompanionStageLayout) -> Unit,
    ) {
        for (kind in CompanionKind.entries) {
            if (kind == CompanionKind.NONE) continue
            val ratio = companionAspectRatio(kind)
            for (width in widths) {
                for (height in heights) check(kind, width, height, companionStageLayout(width, height, height, ratio))
            }
        }
    }

    @Test
    fun hangerSitsAboveTheCompanionAndNeverReachesTheRightColumn() {
        forEveryStage { kind, width, height, layout ->
            val label = "$kind $width x $height"
            val companionTop = height - SkDimens.GapS - layout.companion.height
            val hangerBottom = layout.hanger.y + SkDimens.IconButton
            val hangerRight = layout.hanger.x + SkDimens.IconButton
            // Disjoint from the companion box (above it, with the gap), inside the stage, left of the right column.
            assertTrue(label, hangerBottom <= companionTop - SkDimens.GapS)
            assertTrue(label, layout.hanger.y >= SkDimens.GapS)
            assertTrue(label, layout.hanger.x >= SkDimens.GapS)
            assertTrue(label, hangerRight <= width * (1 - RIGHT_COLUMN_SHARE))
        }
    }

    @Test
    fun companionKeepsItsAspectAndStaysInsideTheStage() {
        forEveryStage { kind, width, height, layout ->
            val label = "$kind $width x $height"
            assertEquals(label, companionAspectRatio(kind), layout.companion.width / layout.companion.height, 0.001f)
            assertTrue(label, layout.companion.width <= width * COMPANION_WIDTH_SHARE + 0.01.dp)
            val reserve = SkDimens.IconButton + SkDimens.GapS * HANGER_RESERVE_GAPS
            assertTrue(label, layout.companion.height <= height - reserve + 0.01.dp)
        }
    }

    @Test
    fun companionGrowsWithTheStageAndLeavesRoomForTheHangerAbove() {
        val ratio = companionAspectRatio(CompanionKind.CAT)
        val short = companionStageLayout(600.dp, 200.dp, 200.dp, ratio).companion
        val tall = companionStageLayout(600.dp, 360.dp, 360.dp, ratio).companion
        assertTrue(tall.height > short.height)
        // The room reserved for the hanger (button plus three gaps) is what limits the height here.
        assertEquals(360.dp - SkDimens.IconButton - SkDimens.GapS * HANGER_RESERVE_GAPS, tall.height)
    }

    @Test
    fun stageWithoutSlackKeepsTheContentAtTheTopAndNoCloudUnderTheHanger() {
        forEveryStage { kind, width, height, layout ->
            val label = "$kind $width x $height"
            assertEquals(label, 0.dp, layout.contentTop)
            val cloudY = layout.leftCloudY ?: return@forEveryStage
            val cloudBottom = cloudY + cloudReach(width * CLOUD_A_SIZE).second
            assertTrue(label, cloudBottom + SkDimens.GapS <= layout.hanger.y)
        }
    }

    @Test
    fun tallStageCentresTheGroupAndKeepsItsShape() {
        for (kind in CompanionKind.entries) {
            if (kind == CompanionKind.NONE) continue
            val ratio = companionAspectRatio(kind)
            for (width in widths) {
                val label = "$kind $width"
                val short = companionStageLayout(width, CONTENT, CONTENT, ratio)
                val tall = companionStageLayout(width, TALL, CONTENT, ratio)
                // Same sizes and positions inside the content box; only the box moves down.
                assertEquals(label, short.companion, tall.companion)
                assertEquals(label, short.hanger, tall.hanger)
                assertTrue(label, tall.contentTop > 0.dp && tall.contentTop + CONTENT <= TALL)
                // The sky above the hanger equals the room below the companion's feet.
                val skyAbove = tall.contentTop + tall.hanger.y
                val roomBelow = TALL - (tall.contentTop + CONTENT - SkDimens.GapS)
                assertEquals(label, skyAbove.value, roomBelow.value, 0.01f)
                // On a phone the left cloud moves into that sky, clear of the stage top and of the hanger;
                // a wider cloud that does not fit is left out.
                val cloudY = tall.leftCloudY
                assertTrue(label, cloudY != null || width > PHONE_WIDTH)
                if (cloudY == null) continue
                val (above, below) = cloudReach(width * CLOUD_A_SIZE)
                assertTrue(label, cloudY - above >= SkDimens.GapS)
                assertTrue(label, cloudY + below + SkDimens.GapS <= skyAbove)
            }
        }
    }

    @Test
    fun withoutTheCompanionStageACloudWithoutFreeSkyIsLeftOut() {
        for (width in widths) {
            // No companion, or a compact stage, filling a short stage: no sky above the content.
            val (left, right) = homeCloudYs(null, width, 0.dp, CONTENT)
            assertEquals("$width left", null, left)
            assertEquals("$width right", null, right)
            // A tall stage leaves sky above the centred content, so both clouds move into it.
            val (skyLeft, skyRight) = homeCloudYs(null, width, centredContentTop(TALL, CONTENT), CONTENT)
            assertTrue("$width left in the sky", skyLeft != null || width > PHONE_WIDTH)
            assertTrue("$width right in the sky", skyRight != null)
        }
        // The companion stage keeps the right cloud behind the content when the sky has no room.
        val stage = companionStageLayout(PHONE_WIDTH, CONTENT, CONTENT, companionAspectRatio(CompanionKind.CAT))
        val (left, right) = homeCloudYs(stage, PHONE_WIDTH, stage.contentTop, CONTENT)
        assertEquals(stage.leftCloudY, left)
        assertTrue(right != null && right > 0.dp && right < CONTENT)
    }

    @Test
    fun measuredGroupFillingItsStageHasNoSkyAndNoCloud() {
        // The compact page's stage is exactly as tall as its group; so is a short stage without a companion.
        for (width in widths) {
            for (group in listOf(200.dp, 262.dp, 314.dp)) {
                val scene = groupStageScene(width, group, group)
                assertEquals("$width $group", StageScene(0.dp, group, null, null), scene)
            }
        }
    }

    @Test
    fun measuredGroupOnATallStageIsCentredWithCloudsOnlyInTheFreeSky() {
        for (width in widths) {
            for (group in listOf(200.dp, 284.dp, CONTENT)) {
                val label = "$width $group"
                val scene = groupStageScene(width, TALL, group)
                assertEquals(label, group, scene.contentHeight)
                // The sky above the group equals the ground below it, as on the companion stage.
                val skyAbove = scene.contentTop
                val groundBelow = TALL - scene.contentTop - group
                assertEquals(label, skyAbove.value, groundBelow.value, 0.01f)
                assertCloudsInFreeSky(label, width, scene, skyAbove)
            }
        }
        // A phone with a clock-only group leaves room for both clouds above it.
        val phone = groupStageScene(PHONE_WIDTH, TALL, 284.dp)
        assertTrue(phone.leftCloudY != null && phone.rightCloudY != null)
    }

    @Test
    fun centredContentTopCentresTheBoxAndStaysInsideTheStage() {
        assertEquals(0.dp, centredContentTop(CONTENT, CONTENT))
        assertEquals((TALL - CONTENT) / 2, centredContentTop(TALL, CONTENT))
        // A group near the top of its box can never push the box below the stage bottom.
        assertEquals(TALL - CONTENT, centredContentTop(TALL, CONTENT, groupTop = 0.dp, groupBottom = 10.dp))
    }

    private fun assertCloudsInFreeSky(
        label: String,
        width: androidx.compose.ui.unit.Dp,
        scene: StageScene,
        skyAbove: androidx.compose.ui.unit.Dp,
    ) {
        for ((y, share) in listOf(scene.leftCloudY to CLOUD_A_SIZE, scene.rightCloudY to CLOUD_B_SIZE)) {
            if (y == null) continue
            val (above, below) = cloudReach(width * share)
            assertTrue(label, y - above >= SkDimens.GapS)
            assertTrue(label, y + below + SkDimens.GapS <= skyAbove)
        }
    }

    private companion object {
        val CONTENT = 360.dp
        val TALL = 584.dp
        val PHONE_WIDTH = 411.dp
    }
}
