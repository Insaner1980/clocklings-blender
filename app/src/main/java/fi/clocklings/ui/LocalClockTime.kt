package fi.clocklings.ui

import androidx.compose.runtime.staticCompositionLocalOf
import fi.clocklings.domain.ClockTime

internal val LocalClockTime = staticCompositionLocalOf<() -> Int> { ClockTime::now }
