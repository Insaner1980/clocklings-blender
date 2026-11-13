package fi.clocklings.ui.appearance

import fi.clocklings.R
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind

internal fun companionName(kind: CompanionKind): Int =
    when (kind) {
        CompanionKind.HUMAN -> R.string.companion_human
        CompanionKind.CAT -> R.string.companion_cat
        CompanionKind.DOG -> R.string.companion_dog
        CompanionKind.ROBOT -> R.string.companion_robot
        CompanionKind.DRAGON -> R.string.companion_dragon
        CompanionKind.DINOSAUR -> R.string.companion_dinosaur
        CompanionKind.BUNNY -> R.string.companion_bunny
        CompanionKind.UNICORN -> R.string.companion_unicorn
        CompanionKind.CAR -> R.string.companion_car
        CompanionKind.NONE -> R.string.companion_none
    }

internal fun companionColorName(color: CompanionColor): Int =
    when (color) {
        CompanionColor.ORIGINAL -> R.string.color_original
        CompanionColor.BLUE -> R.string.color_blue
        CompanionColor.GREEN -> R.string.color_green
        CompanionColor.PURPLE -> R.string.color_purple
        CompanionColor.ORANGE -> R.string.color_orange
    }

internal fun clockThemeName(theme: ClockTheme): Int =
    when (theme) {
        ClockTheme.CLASSIC -> R.string.theme_classic
        ClockTheme.RAINBOW -> R.string.theme_rainbow
        ClockTheme.SPACE -> R.string.theme_space
        ClockTheme.FOREST -> R.string.theme_forest
        ClockTheme.OCEAN -> R.string.theme_ocean
        ClockTheme.GARDEN -> R.string.theme_garden
        ClockTheme.WORKSHOP -> R.string.theme_workshop
        ClockTheme.CUSTOM -> R.string.theme_custom
    }
