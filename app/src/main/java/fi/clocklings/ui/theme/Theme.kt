package fi.clocklings.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private val SkColorScheme =
    lightColorScheme(
        primary = SkColors.Hour,
        onPrimary = SkColors.Surface,
        secondary = SkColors.Minute,
        background = SkColors.Bg,
        onBackground = SkColors.Ink,
        surface = SkColors.Surface,
        onSurface = SkColors.Ink,
        onSurfaceVariant = SkColors.InkSoft,
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
    private val bottom: Color? = null,
) {
    /** Home is a place of its own: a soft sky fading into a warm blush. */
    HOME(SkColors.HomeSky, SkColors.HomeBlush),
    LEARN(SkColors.KouluFill),
    EXPLORE(SkColors.VapaaFill),
    PLAY(SkColors.PelaaFill),
    TOGETHER(SkColors.KysyFill),
    COMPANION(SkColors.NukkeFill),
    SETTINGS(SkColors.SettingsBackground),
    ;

    val bottomColor: Color get() = bottom ?: lerp(color, SkColors.Surface, SkColors.SECTION_TINT)
}

/** Soft activity colors cover the full window, including the system bars. */
fun Modifier.sectionBackground(section: Section): Modifier =
    drawBehind {
        drawRect(Brush.verticalGradient(listOf(section.color, section.bottomColor)))
    }
