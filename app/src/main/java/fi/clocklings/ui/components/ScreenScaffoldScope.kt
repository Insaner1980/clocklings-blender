package fi.clocklings.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.Dp

class ScreenScaffoldScope internal constructor(
    columnScope: ColumnScope,
    val viewportHeight: Dp,
) : ColumnScope by columnScope
