package fi.clocklings.ui

import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import fi.clocklings.analytics.UsageAnalytics
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        compose.onNodeWithText("Clocklings Full Version").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(1, back)
    }

    @Test
    fun keyboardGateAndRestorationRequireAnIntentionalAdultAction() {
        val restoration = StateRestorationTester(compose)
        val usage = TestUsage()
        restoration.setContent {
            ClocklingsTheme { ParentScreen(PurchaseUiState(), {}, {}, {}, {}, usage = usage) }
        }
        compose.onNodeWithText("Share usage statistics", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        val first = gateFactors()
        compose.onNode(hasSetTextAction()).performTextInput("0")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Try the new calculation.").assertExists()
        assertNotEquals(first, gateFactors())
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Clocklings Full Version").assertDoesNotExist()
        passGate()
        compose.onNodeWithText("Clocklings Full Version").assertExists()
        compose.onNodeWithText("Share usage statistics", substring = true).assertIsOff()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Clocklings Full Version").assertDoesNotExist()
        compose.onNodeWithText("Share usage statistics", substring = true).assertDoesNotExist()
    }

    @Test
    fun unavailableStoreShowsOneMessageAndOffersRestoreAndRetry() {
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
                    usage = TestUsage(available = false),
                )
            }
        }
        passGate()
        compose.onNodeWithText("Share usage statistics", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Restore purchases").performScrollTo().performClick()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, restores)
        assertEquals(1, retries)
        val unavailable = "Purchases are unavailable. Please try again later."
        compose.onNodeWithText(unavailable).assertExists()
        compose.onNode(hasText(unavailable) and hasClickAction()).assertDoesNotExist()
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
    fun recoveryAndUsageControlsWorkAcrossLanguagesNarrowWidthsAndLargeText() {
        val language = mutableStateOf(AppLanguage.ENGLISH)
        val viewport = mutableStateOf(360 to 1f)
        val state = mutableStateOf(PurchaseUiState(status = PurchaseStatus.FAILED))
        val usage = TestUsage()
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
                            ParentScreen(
                                state.value,
                                { error("purchase") },
                                { restores++ },
                                { retries++ },
                                {},
                                usage = usage,
                            )
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
                passLocalizedGate(resources)
                if (entry == AppLanguage.FINNISH) {
                    compose.onNodeWithText("Clocklings-täysversio").performScrollTo().assertIsDisplayed()
                }
                verifyRecoveryActions(resources)
                verifyOwnedAndPendingStates(resources, state)
                verifyUsageControls(resources)
            }
        }
        assertEquals(28, retries)
        assertEquals(28, restores)
    }

    private fun verifyOwnedAndPendingStates(
        resources: Resources,
        state: MutableState<PurchaseUiState>,
    ) {
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

    private fun passLocalizedGate(resources: Resources) {
        val prefix =
            resources
                .getString(R.string.parent_gate_instruction, 12, 3)
                .substringBefore("12")
                .trim()
        passGate(prefix)
    }

    private fun verifyUsageControls(resources: Resources) {
        compose
            .onNodeWithText(resources.getString(R.string.usage_sharing_description), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        val toggle = compose.onNodeWithText(resources.getString(R.string.usage_sharing_title), substring = true)
        toggle.performScrollTo().assertIsOff().performClick()
        toggle.assertIsOn().performClick()
        toggle.assertIsOff()
    }

    private fun verifyRecoveryActions(resources: Resources) {
        compose
            .onNodeWithText(resources.getString(R.string.purchase_failed))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNode(hasText(resources.getString(R.string.purchase_unavailable)) and hasClickAction())
            .assertDoesNotExist()
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
        val (first, second) = gateFactors(prefix)
        compose.onNode(hasSetTextAction()).performTextInput((first * second).toString())
        compose.onNode(hasSetTextAction()).performImeAction()
    }

    private fun gateFactors(prefix: String = "For adults:"): Pair<Int, Int> {
        val instruction =
            compose
                .onNodeWithText(prefix, substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString { it.text }
        val (first, second) = checkNotNull(Regex("([0-9]{2}) × ([0-9])").find(instruction)).destructured
        return first.toInt() to second.toInt()
    }
}

private class TestUsage(
    override val available: Boolean = true,
) : UsageAnalytics {
    override val enabled = MutableStateFlow(false)

    override fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    override fun count(metric: String) = Unit
}
