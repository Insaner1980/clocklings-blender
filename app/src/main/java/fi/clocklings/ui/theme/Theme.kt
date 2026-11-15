package fi.clocklings.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

private val SkColorScheme =
    lightColorScheme(
        primary = SkColors.Primary,
        onPrimary = SkColors.OnPrimary,
        primaryContainer = SkColors.PrimaryContainer,
        onPrimaryContainer = SkColors.Ink,
        secondary = SkColors.Minute,
        background = SkColors.Bg,
        onBackground = SkColors.Ink,
        surface = SkColors.Surface,
        onSurface = SkColors.Ink,
        onSurfaceVariant = SkColors.UiInkSoft,
        surfaceVariant = SkColors.SurfaceContainer,
        surfaceContainerHigh = SkColors.Surface,
        error = SkColors.Danger,
        outline = SkColors.Line,
    )

private val SkTypography =
    Typography(
        bodyLarge = SkType.Body,
        bodyMedium = SkType.Body,
        titleLarge = SkType.Title,
        labelLarge = SkType.Button,
    )

/** The application's light theme. */
@Composable
fun ClocklingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkColorScheme, typography = SkTypography, content = content)
}

enum class Section(
    val color: Color,
    val track: Color = SkColors.Line,
) {
    HOME(SkColors.Bg),
    LEARN(SkColors.LearnSurface, SkColors.LearnTrack),
    EXPLORE(SkColors.ExploreSurface),
    PLAY(SkColors.PlaySurface, SkColors.PlayTrack),
    TOGETHER(SkColors.TogetherSurface),
    COMPANION(SkColors.CompanionSurface),
    SETTINGS(SkColors.SettingsBackground),
    ;

    val bottomColor: Color get() = color
}

/** A flat activity tint covers the full window, including the system bars. */
fun Modifier.sectionBackground(section: Section): Modifier = drawBehind { drawRect(section.color) }
