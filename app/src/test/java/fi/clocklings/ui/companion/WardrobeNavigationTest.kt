package fi.clocklings.ui.companion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SectionChips
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WardrobeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tabsAlignVisibleAndOffscreenHeadingsAndFollowResizedViewport() {
        val height = mutableIntStateOf(640)
        val header = mutableIntStateOf(56)
        val labels = listOf("Colors", "Clothes", "Extras")
        lateinit var navigation: WardrobeSections
        compose.setContent {
            navigation = rememberWardrobeSections(CompanionKind.CAT)
            ClocklingsTheme {
                Box(Modifier.requiredSize(320.dp, height.intValue.dp)) {
                    ScreenScaffold(
                        scrollState = navigation.scroll,
                        onViewportTop = { navigation.viewportTop = it },
                        topBar = {
                            Column {
                                Spacer(Modifier.height(header.intValue.dp))
                                SectionChips(labels, navigation.selected(labels.size), { navigation.select(it, false) })
                            }
                        },
                    ) {
                        Spacer(Modifier.height(120.dp))
                        labels.forEachIndexed { index, label ->
                            WardrobeSection(label, { navigation.headings[index] = it }) {
                                Spacer(Modifier.height(if (index == 2) 64.dp else 600.dp))
                            }
                        }
                    }
                }
            }
        }
        assertEquals(0, navigation.selected(labels.size))
        for (index in listOf(0, 1)) {
            compose.onNode(hasText(labels[index]) and hasClickAction()).performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(navigation.viewportTop, navigation.headings.getValue(index), 1f)
            assertEquals(index, navigation.selected(labels.size))
        }
        compose.runOnIdle {
            navigation.select(2, false)
            navigation.select(0, false)
            navigation.select(2, false)
        }
        compose.waitForIdle()
        assertEquals(navigation.scroll.maxValue, navigation.scroll.value)
        assertEquals(2, navigation.selected(labels.size))
        for ((windowHeight, headerHeight) in listOf(300 to 96, 640 to 112)) {
            compose.runOnIdle {
                height.intValue = windowHeight
                header.intValue = headerHeight
            }
            compose.waitForIdle()
            compose.runOnIdle { navigation.select(1, true) }
            compose.waitForIdle()
            assertEquals(navigation.viewportTop, navigation.headings.getValue(1), 1f)
            assertEquals(1, navigation.selected(labels.size))
            assertTrue(navigation.scroll.value > 0)
        }
    }
}
