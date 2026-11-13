package fi.clocklings.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParentScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancellationNeverOpensProductInformationOrStartsBilling() {
        var back = 0
        compose.setContent {
            ClocklingsTheme {
                ParentScreen(PurchaseUiState(), { error("purchase") }, { error("restore") }, {}, { back++ })
            }
        }
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(1, back)
    }

    @Test
    fun keyboardGateAndRestorationRequireAnIntentionalAdultAction() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ClocklingsTheme { ParentScreen(PurchaseUiState(), {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("0000")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Check the order and try again.").assertExists()
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        passGate()
        compose.onNodeWithText("Clocklings Full Unlock").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
    }

    @Test
    fun unavailablePriceDisablesPurchaseAndOffersRestoreAndRetry() {
        var restores = 0
        var retries = 0
        compose.setContent {
            ClocklingsTheme {
                ParentScreen(
                    PurchaseUiState(),
                    { error("purchase") },
                    { restores++ },
                    { retries++ },
                    {},
                )
            }
        }
        passGate()
        compose.onNodeWithText("Restore purchases").performScrollTo().performClick()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, restores)
        assertEquals(1, retries)
        compose
            .onNode(hasText("Purchases are unavailable. Please try again later.") and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun localizedPlayPriceAndOwnershipUpdateWithoutRestart() {
        val state =
            mutableStateOf(
                PurchaseUiState(localizedPrice = "4,50 €", status = PurchaseStatus.READY, canPurchase = true),
            )
        var purchases = 0
        compose.setContent {
            ClocklingsTheme { ParentScreen(state.value, { purchases++ }, {}, {}, {}) }
        }
        passGate()
        compose
            .onNodeWithText("Buy once — 4,50 €")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(1, purchases)
        compose.runOnIdle { state.value = PurchaseUiState(fullAccess = true, status = PurchaseStatus.RESTORED) }
        compose.onNodeWithText("Buy once — 4,50 €").assertDoesNotExist()
        compose.onNodeWithText("Full version restored.").assertExists()
    }

    @Test
    fun recoveryControlsWorkAcrossLanguagesNarrowWidthsAndLargeText() {
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val viewport = mutableStateOf(360 to 1f)
        val state = mutableStateOf(PurchaseUiState(status = PurchaseStatus.FAILED))
        var retries = 0
        var restores = 0
        compose.setContent {
            val context = localizedContext(language.value)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(density.density, viewport.value.second),
            ) {
                ClocklingsTheme {
                    key(language.value, viewport.value) {
                        Box(Modifier.width(viewport.value.first.dp)) {
                            ParentScreen(state.value, { error("purchase") }, { restores++ }, { retries++ }, {})
                        }
                    }
                }
            }
        }
        for (entry in AppLanguage.entries) {
            for (cell in TestViewports) {
                compose.runOnIdle {
                    language.value = entry
                    viewport.value = cell
                    state.value = PurchaseUiState(status = PurchaseStatus.FAILED)
                }
                val resources = localizedContext(entry).resources
                val prefix =
                    resources
                        .getString(R.string.parent_gate_instruction, "GATE_MARKER")
                        .substringBefore("GATE_MARKER")
                        .trim()
                passGate(prefix)
                verifyRecoveryActions(resources)
                compose.runOnIdle { state.value = state.value.copy(fullAccess = true) }
                compose
                    .onNode(hasText(resources.getString(R.string.purchase_unavailable)) and hasClickAction())
                    .assertDoesNotExist()
                compose.runOnIdle { state.value = state.value.copy(status = PurchaseStatus.ACKNOWLEDGEMENT_PENDING) }
                compose
                    .onNodeWithText(resources.getString(R.string.purchase_acknowledgement_pending))
                    .performScrollTo()
                    .assertIsDisplayed()
                compose.runOnIdle { state.value = state.value.copy(busy = true) }
                compose.onNodeWithText(resources.getString(R.string.purchase_retry)).assertIsNotEnabled()
                compose.onNodeWithText(resources.getString(R.string.purchase_restore)).assertIsNotEnabled()
            }
        }
        assertEquals(28, retries)
        assertEquals(28, restores)
    }

    private fun verifyRecoveryActions(resources: Resources) {
        compose
            .onNodeWithText(resources.getString(R.string.purchase_failed))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNode(hasText(resources.getString(R.string.purchase_unavailable)) and hasClickAction())
            .assertIsNotEnabled()
        compose
            .onNodeWithText(resources.getString(R.string.purchase_retry))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose
            .onNodeWithText(resources.getString(R.string.purchase_restore))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
    }

    private fun passGate(prefix: String = "For adults:") {
        val instruction =
            compose
                .onNodeWithText(prefix, substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString { it.text }
        val answer = instruction.filter(Char::isDigit).reversed()
        compose.onNode(hasSetTextAction()).performTextInput(answer)
        compose.onNode(hasSetTextAction()).performImeAction()
    }
}
