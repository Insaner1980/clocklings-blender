package fi.clocklings.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CompanionStyle
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Personalization

internal fun Preferences.readPersonalization(): Personalization =
    Personalization(
        companion = enumValue("companion", CompanionKind.HUMAN),
        styles =
            CompanionKind.entries
                .mapNotNull { kind ->
                    val color = enumValue("companion_${kind.name}_color", CompanionColor.ORIGINAL)
                    val style = CompanionStyle(color = color)
                    if (style == CompanionStyle()) null else kind to style
                }.toMap(),
        clock =
            ClockAppearance(
                theme = enumValue("clock_theme", ClockTheme.RAINBOW),
                custom =
                    CustomClock(
                        palette = enumValue("clock_palette", ClockPalette.BLUE),
                        frame = enumValue("clock_frame", ClockFrame.ROUND),
                        numerals = enumValue("clock_numerals", NumeralStyle.ROUNDED),
                        hands = enumValue("clock_hands", HandStyle.ROUND),
                    ),
            ),
    )

internal fun MutablePreferences.writePersonalization(value: Personalization) {
    this[stringPreferencesKey("companion")] = value.companion.name
    for (kind in CompanionKind.entries) {
        val style = value.styleFor(kind)
        this[stringPreferencesKey("companion_${kind.name}_color")] = style.color.name
    }
    this[stringPreferencesKey("clock_theme")] = value.clock.theme.name
    this[stringPreferencesKey("clock_palette")] = value.clock.custom.palette.name
    this[stringPreferencesKey("clock_frame")] = value.clock.custom.frame.name
    this[stringPreferencesKey("clock_numerals")] = value.clock.custom.numerals.name
    this[stringPreferencesKey("clock_hands")] = value.clock.custom.hands.name
}

private inline fun <reified T : Enum<T>> Preferences.enumValue(
    key: String,
    fallback: T,
): T = enumValues<T>().firstOrNull { it.name == this[stringPreferencesKey(key)] } ?: fallback
