package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.appearance.clockThemeName
import fi.clocklings.ui.appearance.companionName
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.io.File

/** Deterministic, actual Compose renders for visual review at the default test viewport. */
class PersonalizationGalleryTest {
    /** The default outfit, plus the companion's free items when [withFreeItems] is on. */
    private fun wornOutfit(
        kind: CompanionKind,
        withFreeItems: Boolean,
    ): Outfit =
        if (withFreeItems) {
            Wardrobe.freeItems(kind).fold(Outfit.default(kind)) { o, item -> o.with(item.slot, item.id) }
        } else {
            Outfit.default(kind)
        }

    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(compose)

    @Test
    fun companionArtwork() {
        val dressed = mutableStateOf(false)
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.testTag("gallery").background(SkColors.Bg).padding(8.dp)) {
                    for (row in CompanionKind.entries.filter { it != CompanionKind.NONE }.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (kind in row) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CompanionView(
                                        kind,
                                        wornOutfit(kind, dressed.value),
                                        Modifier.size(100.dp, 170.dp),
                                        color = if (dressed.value) CompanionColor.BLUE else CompanionColor.ORIGINAL,
                                        animated = false,
                                    )
                                    Text(stringResource(companionName(kind)), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        save("all_companions")
        compose.runOnIdle { dressed.value = true }
        save("all_accessories")
    }

    @Test
    fun clockArtwork() {
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.testTag("gallery").background(SkColors.Bg).padding(8.dp)) {
                    for (row in ClockTheme.entries.chunked(2)) {
                        Row {
                            for (theme in row) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ClockFace(
                                        ClockCommand(610),
                                        Modifier.size(156.dp),
                                        mini = true,
                                        appearance =
                                            ClockAppearance(
                                                theme,
                                                CustomClock(
                                                    ClockPalette.CORAL,
                                                    ClockFrame.BLOCKS,
                                                    NumeralStyle.PLAIN,
                                                    HandStyle.POINTER,
                                                ),
                                            ),
                                    )
                                    Text(stringResource(clockThemeName(theme)), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        save("all_clocks")
    }

    @Test
    fun clockDepthAtFullAndMiniSize() {
        val theme = mutableStateOf(ClockTheme.CLASSIC)
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.testTag("gallery").background(SkColors.Bg).padding(8.dp)) {
                    Text(stringResource(clockThemeName(theme.value)), fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val appearance = ClockAppearance(theme = theme.value)
                        ClockFace(ClockCommand(610), Modifier.size(210.dp), appearance = appearance)
                        ClockFace(ClockCommand(610), Modifier.size(84.dp), mini = true, appearance = appearance)
                    }
                }
            }
        }
        for (option in ClockTheme.entries) {
            compose.runOnIdle { theme.value = option }
            save("depth_" + option.name)
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onNodeWithTag("gallery").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "personalization").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun resultRespectsEveryCompanionIncludingNone() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        compose.setContent {
            ClocklingsTheme {
                CompositionLocalProvider(LocalPersonalization provides Personalization(companion = kind.value)) {
                    ResultScreen(
                        access = AccessPolicy(fullAccess = true),
                        Screen.Result(0, 3, 0, 0),
                        Outfit.Default,
                        {},
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        for (option in CompanionKind.entries) {
            compose.runOnIdle { kind.value = option }
            if (option == CompanionKind.NONE) {
                compose
                    .onAllNodes(
                        SemanticsMatcher("Companion artwork") {
                            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("companion_")
                        },
                        useUnmergedTree = true,
                    ).assertCountEquals(0)
            } else {
                compose.onNodeWithTag("companion_${option.name}", useUnmergedTree = true).assertExists()
            }
            if (option != CompanionKind.HUMAN) compose.onNodeWithText("Polka-dot dress").assertDoesNotExist()
            compose.onNodeWithText("Play again").assertExists()
            if (option == CompanionKind.HUMAN) LocalizationScreenshots.capture("native-result")
        }
    }
}
