package fi.clocklings.ui.theme

import androidx.compose.ui.graphics.Color
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind

/** Companion swatches and clock artwork pigments; text and hands use SkColors. */
object PersonalizationColors {
    val Cat = Color(0xFFEFA04C)
    val Dog = Color(0xFFCE8B50)
    val Robot = Color(0xFF5DB6BA)
    val Dragon = Color(0xFFA77BDD)
    val Dinosaur = Color(0xFF7DBE74)
    val Bunny = Color(0xFFF2CFA2)
    val Unicorn = Color(0xFFF8F1F3)
    val Car = Color(0xFFF07A2E)
    val Blue = Color(0xFF86BDED)
    val Green = Color(0xFF94C9A1)
    val Purple = Color(0xFFC5A1DD)
    val Orange = Color(0xFFF4B26A)

    // Per-kind color options where a generic option would look like the kind's own Original color.
    val RobotBlue = Color(0xFF5B95DA)
    val Space = Color(0xFF444777)
    val SpaceLight = Color(0xFFA6A6DE)
    val Forest = Color(0xFF547B57)
    val ForestLight = Color(0xFFB8D6A0)
    val Ocean = Color(0xFF267E9A)
    val OceanLight = Color(0xFF98DADB)
    val Garden = Color(0xFFAC5681)
    val GardenLight = Color(0xFFF4B9D4)
    val Workshop = Color(0xFFAA7243)
    val WorkshopLight = Color(0xFFE8BB77)
    val Classic = Color(0xFF455260)
    val Coral = Color(0xFFB95760)
    val CustomBlue = Color(0xFF386EAA)
    val CustomPurple = Color(0xFF79599D)

    fun companion(
        kind: CompanionKind,
        color: CompanionColor,
    ): Color =
        when (color) {
            CompanionColor.BLUE -> {
                if (kind == CompanionKind.ROBOT) RobotBlue else Blue
            }

            CompanionColor.GREEN -> {
                Green
            }

            CompanionColor.PURPLE -> {
                Purple
            }

            CompanionColor.ORANGE -> {
                Orange
            }

            CompanionColor.ORIGINAL -> {
                originalCompanion(kind)
            }
        }

    private fun originalCompanion(kind: CompanionKind): Color =
        when (kind) {
            CompanionKind.CAT -> Cat
            CompanionKind.DOG -> Dog
            CompanionKind.ROBOT -> Robot
            CompanionKind.DRAGON -> Dragon
            CompanionKind.BUNNY -> Bunny
            CompanionKind.UNICORN -> Unicorn
            CompanionKind.CAR -> Car
            else -> Dinosaur
        }

    fun clock(palette: ClockPalette): Color =
        when (palette) {
            ClockPalette.BLUE -> CustomBlue
            ClockPalette.GREEN -> Forest
            ClockPalette.PURPLE -> CustomPurple
            ClockPalette.CORAL -> Coral
        }
}
