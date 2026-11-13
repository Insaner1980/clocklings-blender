package fi.clocklings.ui.intro

/** Startup choreography in milliseconds, shared by rendering and focused tests. */
object IntroTimeline {
    const val MASCOT_IN_MS = 350L
    const val MASCOT_FADE_MS = 150L
    const val MASCOT_JUMP_MS = 600L
    const val TICK_AT_MS = MASCOT_IN_MS + MASCOT_JUMP_MS
    const val TITLE_START_MS = 100L
    const val LETTER_STAGGER_MS = 25L
    const val LETTER_MS = 300L
    const val LETTER_FADE_MS = 100L
    const val GREETING_START_MS = 350L
    const val GREETING_MS = 250L
    const val FADE_START_MS = 1000L
    const val CONTENT_FADE_MS = 100L
    const val BACKGROUND_FADE_MS = 150L
    const val END_MS = FADE_START_MS + CONTENT_FADE_MS + BACKGROUND_FADE_MS
    const val STILL_MS = 400L

    /** The jingle and greeting can finish on Home without holding the startup overlay open. */
    const val GREETING_VOICE_MS = TICK_AT_MS + 1800L

    /** Without motion there is no jingle: the greeting is spoken while the still pose shows it. */
    const val STILL_GREETING_VOICE_MS = 300L
    const val MASCOT_START_SCALE = 0.6f

    fun mascotScale(t: Long): Float =
        MASCOT_START_SCALE + (1f - MASCOT_START_SCALE) * easeOutBack(progress(t, 0L, MASCOT_IN_MS))

    fun mascotAlpha(t: Long): Float = progress(t, 0L, MASCOT_FADE_MS)

    fun letterScale(
        t: Long,
        index: Int,
    ): Float = easeOutBack(progress(t, letterStart(index), LETTER_MS))

    fun letterAlpha(
        t: Long,
        index: Int,
    ): Float = progress(t, letterStart(index), LETTER_FADE_MS)

    fun greetingAlpha(t: Long): Float = progress(t, GREETING_START_MS, GREETING_MS)

    /** Content disappears before the background reveals Home. */
    fun contentAlpha(t: Long): Float = 1f - progress(t, FADE_START_MS, CONTENT_FADE_MS)

    fun overlayAlpha(t: Long): Float = 1f - progress(t, FADE_START_MS + CONTENT_FADE_MS, BACKGROUND_FADE_MS)
}

private fun letterStart(index: Int): Long = IntroTimeline.TITLE_START_MS + index * IntroTimeline.LETTER_STAGGER_MS

internal fun progress(
    t: Long,
    start: Long,
    duration: Long,
): Float = ((t - start).toFloat() / duration).coerceIn(0f, 1f)

internal fun easeOutBack(k: Float): Float {
    val x = k - 1f
    return 1f + BACK_C3 * x * x * x + BACK_C1 * x * x
}

private const val BACK_C1 = 1.70158f
private const val BACK_C3 = BACK_C1 + 1f
