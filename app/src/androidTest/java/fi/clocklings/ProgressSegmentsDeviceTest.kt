package fi.clocklings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.components.DotOutcome
import fi.clocklings.ui.components.ProgressSegments
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProgressSegmentsDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun completedOutcomesAreDescribedAndShownAnswerHasAVisibleGap() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.setContent {
            ClocklingsTheme {
                Box(Modifier.background(Color.White)) {
                    ProgressSegments(
                        count = 5,
                        current = 3,
                        track = SkColors.PlayTrack,
                        outcomes = listOf(DotOutcome.FIRST_TRY, DotOutcome.WITH_HINT, DotOutcome.SHOWN),
                    )
                }
            }
        }

        val firstTry = compose.onNodeWithContentDescription(resources.getString(R.string.task_outcome_first_try, 1))
        val hint = compose.onNodeWithContentDescription(resources.getString(R.string.task_outcome_with_hint, 2))
        val shown = compose.onNodeWithContentDescription(resources.getString(R.string.task_outcome_shown, 3))
        compose.onNodeWithText(resources.getString(R.string.task_position, 4, 5)).assertIsDisplayed()
        val correct = firstTry.captureToImage().toPixelMap()
        val afterHint = hint.captureToImage().toPixelMap()
        val revealed = shown.captureToImage().toPixelMap()
        assertEquals(SkColors.Good, correct[correct.width / 2, correct.height / 2])
        assertEquals(SkColors.GoldDeep, afterHint[afterHint.width / 2, afterHint.height / 2])
        assertEquals(SkColors.UiInkSoft, revealed[revealed.width / 4, revealed.height / 2])
        assertEquals(Color.White, revealed[revealed.width / 2, revealed.height / 2])
        assertEquals(SkColors.UiInkSoft, revealed[revealed.width * 3 / 4, revealed.height / 2])
    }
}
