package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionPairTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun compactStartLeavesRoomWithoutAnEndAction() {
        val endLabel = mutableStateOf<String?>(null)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        ActionPair(
                            startLabel = "Return to the previous explanation",
                            endLabel = endLabel.value,
                            start = { SkButton("Previous", {}, modifier = it) },
                            compactStart = { SkButton("Back", {}, modifier = it) },
                            end = { SkButton("Next", {}, modifier = it) },
                        )
                    }
                }
            }
        }

        val back = compose.onNodeWithText("Back").assertIsDisplayed()
        val before = back.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Next").assertDoesNotExist()
        compose.runOnIdle { endLabel.value = "Next" }
        compose.onNodeWithText("Next").assertIsDisplayed()
        assertEquals(before, back.fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun changingArrangementPreservesBothSlotsAndUsesTheirLatestCallbacks() {
        val viewport = mutableStateOf(411 to 1f)
        val generation = mutableIntStateOf(0)
        var startGeneration = -1
        var endGeneration = -1
        compose.setContent {
            val (width, fontScale) = viewport.value
            val currentGeneration = generation.intValue
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(width.dp, 640.dp)) {
                        ActionPair(
                            "Back",
                            "Play again",
                            modifier = Modifier.testTag("pair"),
                            start = { modifier ->
                                var clicks by remember { mutableIntStateOf(0) }
                                SkButton(
                                    "Back $clicks",
                                    onClick = {
                                        clicks++
                                        startGeneration = currentGeneration
                                    },
                                    modifier = modifier.testTag("start"),
                                )
                            },
                            end = { modifier ->
                                var clicks by remember { mutableIntStateOf(0) }
                                SkButton(
                                    "Play again $clicks",
                                    onClick = {
                                        clicks++
                                        endGeneration = currentGeneration
                                    },
                                    modifier = modifier.testTag("end"),
                                )
                            },
                        )
                    }
                }
            }
        }

        val cells = listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f, 411 to 1f)
        for ((index, cell) in cells.withIndex()) {
            compose.runOnIdle {
                viewport.value = cell
                generation.intValue = index
            }
            compose.onNodeWithTag("pair").assertExists()
            val start = compose.onNodeWithTag("start").assertIsDisplayed().assertTextEquals("Back $index")
            val end = compose.onNodeWithTag("end").assertIsDisplayed().assertTextEquals("Play again $index")
            assertArrangement(cell, start.fetchSemanticsNode().boundsInRoot, end.fetchSemanticsNode().boundsInRoot)
            start.performClick().assertTextEquals("Back ${index + 1}")
            end.performClick().assertTextEquals("Play again ${index + 1}")
            assertEquals("$cell: current start callback", index, startGeneration)
            assertEquals("$cell: current end callback", index, endGeneration)
        }
    }

    private fun assertArrangement(
        cell: Pair<Int, Float>,
        startBounds: androidx.compose.ui.geometry.Rect,
        endBounds: androidx.compose.ui.geometry.Rect,
    ) {
        // The wider pill padding leaves room for a row only at normal text size.
        if (cell.second == 1f) {
            assertEquals("$cell: same row", startBounds.top, endBounds.top)
            assertTrue("$cell: start before end", startBounds.right <= endBounds.left)
        } else {
            assertTrue(
                "$cell: stacked without overlap: $startBounds / $endBounds",
                startBounds.bottom <= endBounds.top,
            )
        }
    }
}
