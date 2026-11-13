package fi.clocklings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Deterministic access/UI tests. These do not connect to Play or simulate a verified store transaction. */
class MonetizationDeviceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val application
        get() =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.applicationContext as ClocklingsTestApplication

    private fun fixture() =
        object : ExternalResource() {
            private lateinit var originalPurchaseState: PurchaseUiState
            private lateinit var originalProgress: Progress
            private lateinit var originalSettings: Settings

            override fun before() {
                originalPurchaseState = application.testPurchases.state.value
                runBlocking {
                    originalProgress =
                        application.container.repository.progress
                            .first()
                    originalSettings =
                        application.container.repository.settings
                            .first()
                    application.container.repository.updateProgress { Progress() }
                    application.container.repository.updateSettings { Settings(sound = false) }
                }
                application.testPurchases.state.value = PurchaseUiState(fullAccess = false)
            }

            override fun after() {
                application.testPurchases.state.value = originalPurchaseState
                runBlocking {
                    application.container.repository.updateProgress { originalProgress }
                    application.container.repository.updateSettings { originalSettings }
                }
            }
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(fixture()).around(compose)

    @Test fun fixtureRestoresProgressSettingsAndPurchasesAfterAFailedTest() {
        val progress = Progress(stars = listOf(1, 2, 0, 0, 0), lessonsDone = setOf("viisarit"))
        val settings = Settings(sound = false, help = false, h24 = true)
        val purchase = PurchaseUiState(fullAccess = true, status = PurchaseStatus.OWNED)
        runBlocking {
            application.container.repository.updateProgress { progress }
            application.container.repository.updateSettings { settings }
        }
        application.testPurchases.state.value = purchase
        val expectedFailure = AssertionError("Intentional fixture test failure")
        try {
            fixture()
                .apply(
                    object : Statement() {
                        override fun evaluate() {
                            runBlocking {
                                application.container.repository.updateProgress { Progress(stars = List(5) { 3 }) }
                                application.container.repository.updateSettings { Settings(sound = false, help = true) }
                            }
                            application.testPurchases.state.value = PurchaseUiState(fullAccess = false)
                            throw expectedFailure
                        }
                    },
                    Description.EMPTY,
                ).evaluate()
            fail("The fixture must propagate the test failure")
        } catch (failure: AssertionError) {
            assertSame(expectedFailure, failure)
        }
        assertEquals(
            progress,
            runBlocking {
                application.container.repository.progress
                    .first()
            },
        )
        assertEquals(
            settings,
            runBlocking {
                application.container.repository.settings
                    .first()
            },
        )
        assertEquals(purchase, application.testPurchases.state.value)
    }

    @Test
    fun freeContentAdultCancellationUnavailablePurchaseAndLiveAccess() {
        waitForHome()
        clickDescription("Play.")
        compose.onNode(hasContentDescription("Whole hours.", substring = true) and hasClickAction()).assertIsEnabled()
        compose.onNode(hasContentDescription("Half past.", substring = true) and hasClickAction()).assertIsNotEnabled()
        clickText("For parents")
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
        clickText("Cancel")
        compose.onNode(hasContentDescription("Half past.", substring = true) and hasClickAction()).assertIsNotEnabled()
        clickText("For parents")
        val instruction =
            compose
                .onNodeWithText("For adults:", substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString { it.text }
        compose
            .onNode(
                hasSetTextAction(),
            ).scrollToIfScrollable()
            .performTextInput(instruction.filter(Char::isDigit).reversed())
        compose.onNode(hasSetTextAction()).assertIsFocused().performKeyInput { pressKey(Key.Tab) }
        // The shared button's outer keyboard target owns focus; verify activation, not its inner text node.
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithText("Clocklings Full Unlock").scrollToIfScrollable().assertIsDisplayed()
        compose
            .onNode(hasText("Purchases are unavailable. Please try again later.") and hasClickAction())
            .scrollToIfScrollable()
            .assertIsNotEnabled()
        clickText("Restore purchases")
        compose.onNodeWithText("Full version restored.").assertDoesNotExist()
        compose.runOnIdle {
            application.testPurchases.state.value = PurchaseUiState(fullAccess = true, status = PurchaseStatus.RESTORED)
        }
        compose.onNodeWithText("Full version restored.").scrollToIfScrollable().assertIsDisplayed()
        clickDescription("Back")
        compose
            .onNode(hasContentDescription("Half past.", substring = true) and hasClickAction())
            .scrollToIfScrollable()
            .assertIsEnabled()
    }

    @Test fun companionEligibilityAndSavedChoiceRecoverWithoutRestart() {
        waitForHome()
        compose.onNodeWithTag("companion_stage").scrollToIfScrollable().performClick()
        compose.onNodeWithTag("choose_companion_HUMAN").assertIsEnabled().assertIsSelected()
        compose.onNodeWithTag("choose_companion_CAT").assertIsEnabled()
        compose.onNodeWithTag("choose_companion_DOG").assertIsNotEnabled()
        compose.runOnIdle { application.testPurchases.state.value = PurchaseUiState(fullAccess = true) }
        compose.onNodeWithTag("choose_companion_DOG").assertIsNotEnabled()
        runBlocking { application.container.repository.updateProgress { it.withLevelResult(0, 3) } }
        compose
            .onNodeWithTag("choose_companion_DOG")
            .scrollToIfScrollable()
            .assertIsEnabled()
            .performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                application.container.repository.settings
                    .first()
                    .personalization.companion ==
                    CompanionKind.DOG
            }
        }
        compose.runOnIdle { application.testPurchases.state.value = PurchaseUiState(fullAccess = false) }
        compose.onNodeWithTag("choose_companion_HUMAN").scrollToIfScrollable().assertIsSelected()
        assertEquals(
            CompanionKind.DOG,
            runBlocking {
                application.container.repository.settings
                    .first()
                    .personalization.companion
            },
        )
        compose.runOnIdle { application.testPurchases.state.value = PurchaseUiState(fullAccess = true) }
        compose.onNodeWithTag("choose_companion_DOG").scrollToIfScrollable().assertIsSelected()
    }

    @Test fun earnedPaidOutfitRemainsLockedUntilOwnershipUpdates() {
        waitForHome()
        runBlocking { application.container.repository.updateProgress { it.copy(stars = List(5) { 3 }) } }
        compose.onNodeWithTag("companion_stage").scrollToIfScrollable().performClick()
        clickText("Choose an outfit")
        compose.onNode(hasText("Polka-dot dress") and hasClickAction()).scrollToIfScrollable().assertIsEnabled()
        compose.onNode(hasText("Rainbow dress") and hasClickAction()).assertDoesNotExist()
        compose.onNodeWithText("Rainbow dress").scrollToIfScrollable().assertIsDisplayed()
        compose.runOnIdle { application.testPurchases.state.value = PurchaseUiState(fullAccess = true) }
        compose.onNode(hasText("Rainbow dress") and hasClickAction()).scrollToIfScrollable().assertIsEnabled()
        compose.runOnIdle { application.testPurchases.state.value = PurchaseUiState(fullAccess = false) }
        compose.onNode(hasText("Rainbow dress") and hasClickAction()).assertDoesNotExist()
    }

    private fun waitForHome() {
        compose.waitUntil(10_000) {
            skipIntroIfVisible()
            compose
                .onAllNodes(hasContentDescription("Play.", substring = true) and hasClickAction())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun skipIntroIfVisible() {
        if (compose.onAllNodesWithText("Skip intro").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip intro").performClick()
        }
    }

    private fun clickText(text: String) {
        compose
            .onNodeWithText(text)
            .scrollToIfScrollable()
            .assertIsDisplayed()
            .performClick()
    }

    private fun clickDescription(text: String) {
        compose
            .onNode(hasContentDescription(text, substring = true) and hasClickAction())
            .scrollToIfScrollable()
            .assertIsDisplayed()
            .performClick()
    }
}
