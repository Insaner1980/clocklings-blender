package fi.clocklings.ui.clock

internal enum class DragTick { MINUTE, MARK }

/** Only user drag changes call this; programmatic movement and accessibility nudges are silent. */
internal fun dragTick(
    previous: Int,
    next: Int,
): DragTick? =
    when {
        previous == next -> null
        next % FIVE_MINUTES == 0 -> DragTick.MARK
        else -> DragTick.MINUTE
    }

private const val FIVE_MINUTES = 5
