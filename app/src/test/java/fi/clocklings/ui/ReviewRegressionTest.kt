package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.AppServices
import fi.clocklings.R
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ReviewRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val viewport = mutableStateOf(411 to 1f)
    private val language = mutableStateOf(AppLanguage.ENGLISH)
    private val store = ReviewWriteStore()
    private val repository = ProgressRepository(store)
    private val services =
        object : AppServices by FakeServices(fullAccess = true) {
            override val repository = this@ReviewRegressionTest.repository
        }
    private val model = AppViewModel(services)

    @Test
    fun failedNameDraftKeepsItsRetryAfterSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                WardrobeScreen(CompanionKind.CAT, progress ?: Progress(), CompanionColor.ORIGINAL, {})
            }
        }
        store.fail = true
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Draft")
        compose.onNodeWithText(retryText()).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
            assertEquals(errorText(), nameError())
        }
        compose
            .onNodeWithContentDescription(localizedContext(language.value).getString(R.string.doll_name_label))
            .scrollIfNeeded()
            .assertTextEquals("Draft")
        assertEquals("", model.progress.value!!.nameFor(CompanionKind.CAT))
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle { assertEquals("Draft", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        compose.onNodeWithText(retryText()).assertDoesNotExist()
        assertEquals(null, nameError())
    }

    @Test
    fun failedNameRemainsRetryableAfterAnotherChangeSucceeds() {
        compose.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                WardrobeScreen(CompanionKind.CAT, progress ?: Progress(), CompanionColor.ORIGINAL, {})
            }
        }
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Mia")
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.runOnIdle { assertEquals("", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        // A successful colour change must not hide the still-unsaved name or its retry action.
        store.fail = false
        compose.runOnIdle { model.setColor(CompanionKind.CAT, CompanionColor.BLUE) }
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle { assertEquals("Mia", model.progress.value!!.nameFor(CompanionKind.CAT)) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText(retryText()).assertDoesNotExist()
    }

    @Test
    fun appearanceSaveFailureIsVisibleAndRetrySavesTheChoice() {
        showAppearanceScreen()
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText("Clock styles").performClick()
        compose.onNodeWithTag("choose_clock_CUSTOM").scrollIfNeeded().performClick()
        store.fail = false
        compose.runOnIdle { model.setName(CompanionKind.CAT, "Mia") }
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.runOnIdle {
            val theme = requireNotNull(model.settings.value).personalization.clock.theme
            assertEquals(ClockTheme.CUSTOM, theme)
        }
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.onNodeWithText(retryText()).assertDoesNotExist()
    }

    @Test
    fun wardrobeColorAndItemFailuresKeepTheirOwnRetries() {
        val item = Wardrobe.itemsFor(CompanionKind.CAT).first { it.star == 0 }
        val headItem = Wardrobe.itemsFor(CompanionKind.CAT).first { it.star == 1 }
        compose.setContent {
            Viewport {
                val progress by model.progress.collectAsStateWithLifecycle()
                val settings by model.settings.collectAsStateWithLifecycle()
                WardrobeScreen(
                    CompanionKind.CAT,
                    progress ?: Progress(),
                    (settings ?: Settings()).personalization.styleFor(CompanionKind.CAT).color,
                    {},
                )
            }
        }
        compose.runOnIdle { model.updateProgress { it.withLevelResult(0, 3) } }
        store.fail = true
        compose.onNodeWithTag("color_BLUE").scrollIfNeeded().performClick()
        val itemText = localizedContext(language.value).getString(itemName(item.id))
        compose.onNodeWithText(itemText).scrollIfNeeded().performClick()
        val headText = localizedContext(language.value).getString(itemName(headItem.id))
        compose.onNodeWithText(headText).scrollIfNeeded().performClick()
        assertEquals(null, nameError())
        store.fail = false
        compose.onNodeWithContentDescription("Companion name").scrollIfNeeded().performTextReplacement("Mia")
        forEachCell {
            compose.onNodeWithText(errorText()).assertIsDisplayed()
            compose.onNodeWithText(retryText()).assertIsDisplayed()
        }
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            val color = requireNotNull(model.settings.value).personalization.styleFor(CompanionKind.CAT).color
            assertEquals(CompanionColor.BLUE, color)
            assertEquals(false, requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT).isOn(item))
        }
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            val outfit = requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT)
            assertEquals(true, outfit.isOn(headItem))
            assertEquals(false, outfit.isOn(item))
        }
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.onNodeWithText(retryText()).performClick()
        compose.runOnIdle {
            assertEquals(true, requireNotNull(model.progress.value).outfitFor(CompanionKind.CAT).isOn(item))
        }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        store.fail = true
        compose.runOnIdle { model.startLevel(0) }
        compose.onNodeWithText(errorText()).assertDoesNotExist()
    }

    @Test
    fun companionChoiceCanRetryWithoutMovingToAnotherPage() {
        showAppearanceScreen()
        store.fail = true
        compose.onNodeWithTag("choose_companion_CAT").scrollIfNeeded().performClick()
        compose.onNodeWithText(errorText()).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(CompanionKind.HUMAN, requireNotNull(model.settings.value).personalization.companion)
        }
        store.fail = false
        compose.onNodeWithText(retryText()).performClick()
        compose.onNodeWithText(errorText()).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(CompanionKind.CAT, requireNotNull(model.settings.value).personalization.companion)
        }
    }

    @Test
    fun lessonHintTakesPriorityOverAnEarlierSaveFailure() {
        compose.setContent {
            Viewport {
                val lesson = LessonCatalog.lesson("viisarit", language.value)
                LessonScreen(
                    LessonUiState(
                        lesson = lesson,
                        page = lesson.pages.size,
                        choices = lesson.quiz.choices,
                        saveFailed = true,
                        showHint = true,
                    ),
                    false,
                    false,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        forEachCell {
            val lesson = LessonCatalog.lesson("viisarit", language.value)
            val hint = lesson.quiz.hint.resolve(localizedContext(language.value).resources)
            compose.onNodeWithText(hint).scrollIfNeeded().assertIsDisplayed()
            compose.onNodeWithText(errorText()).assertDoesNotExist()
        }
    }

    private fun showAppearanceScreen() {
        compose.setContent {
            Viewport {
                val settings by model.settings.collectAsStateWithLifecycle()
                AppearanceScreen((settings ?: Settings()).personalization, Progress(), {}, {})
            }
        }
    }

    private fun forEachCell(assertion: () -> Unit) {
        for (selectedLanguage in listOf(AppLanguage.ENGLISH, AppLanguage.FINNISH, AppLanguage.SWEDISH)) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    language.value = selectedLanguage
                    viewport.value = cell
                }
                assertion()
            }
        }
    }

    private fun errorText() = localizedContext(language.value).getString(R.string.settings_save_error)

    private fun nameError() =
        compose
            .onNode(hasSetTextAction())
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Error)

    private fun retryText() = localizedContext(language.value).getString(R.string.settings_retry)

    @Composable
    private fun Viewport(content: @Composable () -> Unit) {
        val context = localizedContext(language.value)
        CompositionLocalProvider(
            LocalAppViewModel provides model,
            LocalContext provides context,
            LocalResources provides context.resources,
            LocalAppLanguage provides language.value,
            LocalDensity provides Density(1f, viewport.value.second),
        ) {
            ClocklingsTheme {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) { content() }
            }
        }
    }
}

private class ReviewWriteStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    override val data = delegate.data
    var fail = false

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (fail) throw IOException("Synthetic write failure")
        return delegate.updateData(transform)
    }
}
