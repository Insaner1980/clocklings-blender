package fi.clocklings.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.AppServices
import fi.clocklings.ClocklingsApplication
import fi.clocklings.R
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ScreenshotReviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun personalizationSaveErrorsStayAboveTheirRetryActionsInEveryLanguageAndDisplaySize() {
        val app = ApplicationProvider.getApplicationContext<ClocklingsApplication>()
        val store =
            object : DataStore<Preferences> {
                override val data = MutableStateFlow(emptyPreferences())

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    throw IOException("Synthetic personalization write failure")
            }
        val services =
            object : AppServices by FakeServices(fullAccess = true) {
                override val repository = ProgressRepository(store)
            }
        val model = AppViewModel(services)
        val wardrobe = mutableStateOf(false)
        val language = mutableStateOf("en")
        val viewport = mutableStateOf(411 to 1f)
        setLocalizedContent(model, language, viewport) {
            key(wardrobe.value) {
                if (wardrobe.value) {
                    WardrobeScreen(CompanionKind.CAT, Progress(), CompanionColor.ORIGINAL, {})
                } else {
                    AppearanceScreen(Personalization(), Progress(), {}, {})
                }
            }
        }
        try {
            assertPersonalizationSaveErrorLayouts(app, wardrobe, language, viewport)
        } finally {
            model.viewModelScope.cancel()
        }
    }

    private fun setLocalizedContent(
        model: AppViewModel,
        language: MutableState<String>,
        viewport: MutableState<Pair<Int, Float>>,
        content: @Composable () -> Unit,
    ) {
        val app = ApplicationProvider.getApplicationContext<ClocklingsApplication>()
        compose.setContent {
            val context =
                app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply {
                        setLocale(Locale.forLanguageTag(language.value))
                    },
                )
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(2f, viewport.value.second),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        content()
                    }
                }
            }
        }
    }

    private fun assertPersonalizationSaveErrorLayouts(
        app: ClocklingsApplication,
        wardrobe: MutableState<Boolean>,
        language: MutableState<String>,
        viewport: MutableState<Pair<Int, Float>>,
    ) {
        for (showWardrobe in listOf(false, true)) {
            compose.runOnIdle {
                wardrobe.value = showWardrobe
                language.value = "en"
                viewport.value = 411 to 1f
            }
            if (showWardrobe) {
                compose
                    .onNodeWithContentDescription("Companion name")
                    .performScrollTo()
                    .performTextReplacement("Draft")
            } else {
                compose.onNodeWithText("Clock styles").performClick()
                compose.onNodeWithTag("choose_clock_SPACE").performScrollTo().performClick()
            }
            compose.waitUntil(10_000) {
                compose
                    .onAllNodesWithText(app.getString(R.string.settings_save_error))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            for (tag in AppLanguage.entries.map { it.tag }) {
                for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                    compose.runOnIdle {
                        language.value = tag
                        viewport.value = cell
                    }
                    val context =
                        app.createConfigurationContext(
                            Configuration(app.resources.configuration).apply {
                                setLocale(Locale.forLanguageTag(tag))
                            },
                        )
                    val error = compose.onNodeWithText(context.getString(R.string.settings_save_error))
                    val retry = compose.onNodeWithText(context.getString(R.string.settings_retry))
                    error.assertIsDisplayed()
                    retry.assertIsDisplayed()
                    val errorBottom = error.fetchSemanticsNode().boundsInRoot.bottom
                    val retryTop = retry.fetchSemanticsNode().boundsInRoot.top
                    assertTrue("$showWardrobe / $tag / $cell: error overlaps Retry", errorBottom <= retryTop)
                }
            }
        }
    }

    @Test
    fun olderCompanionSaveDoesNotReplaceTheLaterSettledChoice() {
        val pending = Channel<CompletableDeferred<Unit>>(Channel.UNLIMITED)
        val store =
            CompanionSaveStore {
                val release = CompletableDeferred<Unit>()
                pending.send(release)
                release.await()
            }
        val services =
            object : AppServices by FakeServices(fullAccess = true) {
                override val repository = ProgressRepository(store)
            }
        val model = AppViewModel(services)
        compose.setContent {
            val settings by model.settings.collectAsState()
            CompositionLocalProvider(LocalAppViewModel provides model) {
                ClocklingsTheme {
                    AppearanceScreen(
                        settings?.personalization ?: Personalization(),
                        Progress(stars = listOf(3, 0, 0, 0, 0)),
                        {},
                        {},
                    )
                }
            }
        }
        try {
            compose.onNodeWithTag("choose_companion_CAT").performScrollTo().performClick()
            compose.waitUntil(10_000) { store.writes == 1 }
            val first = pending.tryReceive().getOrThrow()
            compose.onNodeWithTag("choose_companion_DOG").performScrollTo().performClick()
            compose.waitUntil(10_000) { store.writes == 2 }
            compose.runOnIdle { first.complete(Unit) }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals("An old committed selection must not enqueue a new pager selection", 2, store.writes)
                pending.tryReceive().getOrThrow().complete(Unit)
            }
            compose.waitUntil(10_000) {
                model.settings.value
                    ?.personalization
                    ?.companion == CompanionKind.DOG
            }
            compose.runOnIdle { assertEquals(2, store.writes) }
        } finally {
            model.viewModelScope.cancel()
            pending.cancel()
        }
    }

    @Test
    fun companionRetryKeepsThePreviewAfterSwitchingTabsAcrossDisplaySizes() {
        var reject = true
        val store =
            CompanionSaveStore {
                if (reject) throw IOException("Synthetic companion write failure")
            }
        val services =
            object : AppServices by FakeServices(fullAccess = true) {
                override val repository = ProgressRepository(store)
            }
        val model = AppViewModel(services)
        val viewport = mutableStateOf(411 to 1f)
        setLocalizedContent(model, mutableStateOf("en"), viewport) {
            val settings by model.settings.collectAsState()
            AppearanceScreen(
                settings?.personalization ?: Personalization(),
                Progress(stars = listOf(3, 0, 0, 0, 0)),
                {},
                {},
            )
        }
        try {
            for ((index, cell) in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f).withIndex()) {
                compose.runOnIdle {
                    viewport.value = cell
                    reject = true
                }
                val kind = if (index % 2 == 0) CompanionKind.CAT else CompanionKind.DOG
                val name = if (kind == CompanionKind.CAT) "Cat" else "Dog"
                compose.onNodeWithTag("choose_companion_${kind.name}").performScrollTo().performClick()
                compose.onNodeWithText("Try again").assertIsDisplayed()
                compose.onNodeWithText("Clock styles").performScrollTo().performClick()
                compose.onNodeWithText("Companions").performClick()
                compose.runOnIdle { reject = false }
                compose.onNodeWithText("Try again").performClick()
                compose.waitUntil(10_000) {
                    model.settings.value
                        ?.personalization
                        ?.companion == kind
                }
                compose.onNodeWithTag("choose_companion_${kind.name}").assertIsSelected()
                compose
                    .onNode(hasText(name) and hasAnyAncestor(hasTestTag("companion_pager")))
                    .performScrollTo()
                    .assertIsDisplayed()
                compose.runOnIdle {
                    assertEquals("$cell: retry must not trigger another write", (index + 1) * 2, store.writes)
                }
            }
        } finally {
            model.viewModelScope.cancel()
        }
    }

    private class CompanionSaveStore(
        private val beforeWrite: suspend () -> Unit,
    ) : DataStore<Preferences> {
        override val data = MutableStateFlow(preferencesOf(intPreferencesKey("stars_0") to 3))
        private val mutex = Mutex()
        var writes = 0
            private set

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            writes++
            return mutex.withLock {
                beforeWrite()
                transform(data.value).also { data.value = it }
            }
        }
    }

    @Test
    fun lockedItemDescriptionsUseTheThresholdQuantityInEveryLanguageAndDisplaySize() {
        val model = AppViewModel(FakeServices(fullAccess = true))
        val language = mutableStateOf("en")
        val viewport = mutableStateOf(411 to 1f)
        setLocalizedContent(model, language, viewport) {
            WardrobeScreen(CompanionKind.CAT, Progress(), CompanionColor.ORIGINAL, {})
        }
        val descriptions =
            mapOf(
                "en" to
                    listOf("Locked. Opens at 1\u00a0star. You have 0.", "Locked. Opens at 2\u00a0stars. You have 0."),
                "fi" to
                    listOf(
                        "Lukittu. Avautuu, kun tähtiä on 1. Sinulla on 0.",
                        "Lukittu. Avautuu, kun tähtiä on 2. Sinulla on 0.",
                    ),
                "sv" to
                    listOf(
                        "Låst. Låses upp vid 1\u00a0stjärna. Du har 0.",
                        "Låst. Låses upp vid 2\u00a0stjärnor. Du har 0.",
                    ),
            )
        for ((tag, expected) in descriptions) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    language.value = tag
                    viewport.value = cell
                }
                for (description in expected) {
                    compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun wardrobeNamesKeepWholeWordsAtOneSizePerGrid() {
        val context = finnishContext()
        val model = AppViewModel(FakeServices(fullAccess = true))
        val kind = mutableStateOf(CompanionKind.DOG)
        compose.setContent {
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(2.625f, 1f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize((1080f / 2.625f).dp, 800.dp)) {
                        WardrobeScreen(kind.value, Progress(stars = listOf(3, 0, 0, 0, 0)), CompanionColor.ORIGINAL, {})
                    }
                }
            }
        }
        for (companion in listOf(CompanionKind.DOG, CompanionKind.ROBOT, CompanionKind.CAT)) {
            compose.runOnIdle { kind.value = companion }
            for (items in Wardrobe.itemsFor(companion).groupBy { it.slot }.values) {
                val sizes =
                    items.map { item ->
                        val text = context.getString(itemName(item.id))
                        assertWholeWords(text).layoutInput.style.fontSize
                    }
                assertEquals("Different name sizes in ${items.first().slot}", 1, sizes.distinct().size)
            }
        }
    }

    @Test
    fun wardrobeSectionTitlesKeepWholeWordsAtNarrowWidthAndDoubleText() {
        val context = finnishContext()
        val model = AppViewModel(FakeServices(fullAccess = true))
        val kind = mutableStateOf(CompanionKind.CAR)
        compose.setContent {
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(3.375f, 2f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        WardrobeScreen(kind.value, Progress(), CompanionColor.ORIGINAL, {})
                    }
                }
            }
        }
        for (companion in CompanionKind.entries.filter { it.slots.isNotEmpty() }) {
            compose.runOnIdle { kind.value = companion }
            for (slot in companion.slots) assertWholeWords(context.getString(slotName(slot)))
        }
    }

    @Test
    fun longAnswerWordsStayWholeAtNarrowWidthAndDoubleText() {
        val words = "Twenty-one minutes to four in the afternoon"
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3.375f, 2f)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(256.dp, 640.dp)) {
                        OptionButton(words, {}, digital = "15:39")
                    }
                }
            }
        }
        assertWholeWords(words)
    }

    private fun assertWholeWords(
        text: String,
        index: Int = 0,
    ): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onAllNodesWithText(text, useUnmergedTree = true)[index]
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line)
            assertTrue(
                "Word split in $text",
                end !in 1 until text.length || text[end - 1].isWhitespace() || text[end - 1] == '-' ||
                    text[end].isWhitespace(),
            )
        }
        return layout
    }

    @Test
    fun eachLessonChapterStartsItsOwnPracticeLevel() {
        var opened = -1
        val context = finnishContext()
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalAppLanguage provides AppLanguage.FINNISH,
                LocalDensity provides Density(3.375f, 2f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        LessonListScreen(true, Progress(), fi.clocklings.domain.AccessPolicy(fullAccess = true), {
                            opened =
                                it
                        }, {}, {})
                    }
                }
            }
        }
        val names = listOf("Tasatunnit", "Puoli tuntia", "Vartit", "Viiden minuutin välein", "Tarkat minuutit")
        names.forEachIndexed { level, name ->
            assertWholeWords("Aloita pelaaminen!", level)
            compose
                .onNode(
                    hasContentDescription("$name. Harjoittele. Aloita pelaaminen!", substring = true) and
                        hasClickAction(),
                ).performScrollTo()
                .assertIsDisplayed()
                .performClick()
            assertEquals(level, opened)
        }
    }

    private fun finnishContext() =
        ApplicationProvider.getApplicationContext<ClocklingsApplication>().let { app ->
            app.createConfigurationContext(
                Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag("fi")) },
            )
        }

    @Test
    fun lessonNavigationRemainsVisibleWithoutScrollingAtNarrowWidthAndLargeText() {
        var next = 0
        val viewport = mutableStateOf(320 to 1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, viewport.value.second)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        LessonScreen(
                            LessonUiState(LessonCatalog.lesson("minuutit")),
                            false,
                            true,
                            {},
                            { next++ },
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        for (cell in listOf(320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            compose.onNodeWithText("Previous").assertIsDisplayed()
            compose.onNodeWithText("Next").assertIsDisplayed().performClick()
        }
        assertEquals(3, next)
    }

    @Test
    fun answerTimeChipsKeepTheSameEndEdgeWithDifferentWordLengths() {
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.requiredSize(320.dp, 600.dp)) {
                    OptionButton("One", {}, digital = "1:00")
                    OptionButton("Twenty-five minutes past eleven", {}, digital = "11:25")
                }
            }
        }
        val first = compose.onNodeWithText("1:00", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("11:25", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(first.right, second.right, 1f)
    }
}
