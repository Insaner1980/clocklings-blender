package fi.clocklings.ui.appearance

import androidx.compose.runtime.staticCompositionLocalOf
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization

/** Visual preferences shared by every clock and companion, including the intro. */
val LocalPersonalization = staticCompositionLocalOf { Personalization() }
val LocalCompanionOutfit = staticCompositionLocalOf { Outfit.Default }
