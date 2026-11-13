package fi.clocklings.ui.companion

import androidx.compose.runtime.Immutable
import fi.clocklings.domain.Outfit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The companion pose at one moment. [blink] 0 = eyes open, 1 = closed. [swayDeg] is the sway around
 * the feet, [lift] the hop height in the artwork coordinate space, [armDeg] the left arm raise for
 * waving and [turn] the horizontal scale used for the spin (1 = front, -1 = back).
 * [breathe] is the raw breathing sine (-1..1) that renderers multiply by
 * [CompanionMotion.BREATHE_SQUASH], [headTiltDeg] is the head tilt in degrees and [partDeg] the
 * swing of a tail, wings or another part in degrees.
 */
@Immutable
data class CompanionPose(
    val blink: Float = 0f,
    val swayDeg: Float = 0f,
    val lift: Float = 0f,
    val armDeg: Float = 0f,
    val turn: Float = 1f,
    val breathe: Float = 0f,
    val headTiltDeg: Float = 0f,
    val partDeg: Float = 0f,
)

/** One celebration frame: the pose, whether the new outfit is already shown and whether the celebration is over. */
data class CelebrationFrame(
    val pose: CompanionPose,
    val showNewOutfit: Boolean,
    val done: Boolean,
)

/**
 * Companion motion as pure functions of time, so it can be tested on the JVM and drawn without
 * recomposition. Idle: blinking, sway, breathing, head tilt and part swing. Celebration: a spin
 * into the new outfit and two hops while waving.
 */
object CompanionMotion {
    const val SWAY_DEG = 2f
    const val SPIN_MS = 800L
    const val JUMP_MS = 1400L
    const val HOP_HEIGHT = 14f

    const val BREATHE_SQUASH = 0.012f
    const val HEAD_TILT_DEG = 3f
    const val PART_SWING_DEG = 9f

    private const val BLINK_MS = 180L
    private const val SWAY_PERIOD_MS = 3200f
    private const val BREATHE_PERIOD_MS = 3200f
    private const val HEAD_TILT_PERIOD_MS = 4800f
    private const val PART_SWING_PERIOD_MS = 2400f

    /** Blink start times within the repeating cycle: uneven gaps look natural. */
    private val BLINK_STARTS = longArrayOf(3_200L, 7_300L, 9_900L, 14_420L)
    private const val BLINK_CYCLE_MS = 14_600L

    private const val ARM_RAISE_DEG = 95f
    private const val ARM_WIGGLE_DEG = 25f
    private const val ARM_WAVES = 3
    private const val ARM_EASE = 0.15f

    fun blink(nowMs: Long): Float {
        val t = nowMs % BLINK_CYCLE_MS
        val start = BLINK_STARTS.firstOrNull { t >= it && t < it + BLINK_MS } ?: return 0f
        val k = (t - start).toFloat() / BLINK_MS
        return 1f - abs(2f * k - 1f)
    }

    fun sway(nowMs: Long): Float = SWAY_DEG * sin(2f * PI.toFloat() * nowMs / SWAY_PERIOD_MS)

    fun idle(nowMs: Long): CompanionPose =
        CompanionPose(
            blink = blink(nowMs),
            swayDeg = sway(nowMs),
            breathe = sin(2f * PI.toFloat() * (nowMs % BREATHE_PERIOD_MS.toLong()) / BREATHE_PERIOD_MS),
            headTiltDeg = -HEAD_TILT_DEG * raisedCosine(nowMs, HEAD_TILT_PERIOD_MS),
            partDeg = PART_SWING_DEG * raisedCosine(nowMs, PART_SWING_PERIOD_MS),
        )

    /** Smooth 0..1..0 wave that starts at rest: (1 - cos) / 2. The time is wrapped to keep float precision. */
    private fun raisedCosine(
        nowMs: Long,
        periodMs: Float,
    ): Float = (1f - cos(2f * PI.toFloat() * (nowMs % periodMs.toLong()) / periodMs)) / 2f

    fun duration(
        spin: Boolean,
        jump: Boolean,
    ): Long = (if (spin) SPIN_MS else 0L) + (if (jump) JUMP_MS else 0L)

    fun celebration(
        elapsedMs: Long,
        spin: Boolean,
        jump: Boolean,
    ): CelebrationFrame {
        val spinMs = if (spin) SPIN_MS else 0L
        return when {
            elapsedMs >= duration(spin, jump) -> {
                CelebrationFrame(CompanionPose(), showNewOutfit = spin, done = true)
            }

            elapsedMs < spinMs -> {
                val k = elapsedMs.toFloat() / SPIN_MS
                CelebrationFrame(
                    CompanionPose(turn = cos(2f * PI.toFloat() * k)),
                    showNewOutfit = elapsedMs > SPIN_MS / 4,
                    done = false,
                )
            }

            else -> {
                CelebrationFrame(jumpPose((elapsedMs - spinMs).toFloat() / JUMP_MS), showNewOutfit = spin, done = false)
            }
        }
    }

    /**
     * Two hops and a wave; the arm eases up and down at the start and end. Tails and wings flap on
     * every hop.
     */
    private fun jumpPose(k: Float): CompanionPose {
        val twoPiK = 2f * PI.toFloat() * k
        val ease = min(1f, min(k / ARM_EASE, (1f - k) / ARM_EASE))
        return CompanionPose(
            lift = HOP_HEIGHT * abs(sin(twoPiK)),
            armDeg = ease * (ARM_RAISE_DEG + ARM_WIGGLE_DEG * sin(ARM_WAVES * twoPiK)),
            partDeg = 2f * PART_SWING_DEG * abs(sin(twoPiK)),
        )
    }

    /**
     * Whether the celebration's new outfit is already shown: after the first quarter of the spin,
     * or at once when animations are reduced. The celebration starts after [CompanionCelebration.delayMs].
     */
    fun revealsNewOutfit(
        event: CompanionCelebration?,
        elapsedMs: Long,
        reduceMotion: Boolean,
    ): Boolean {
        if (event?.spin != true) return false
        val sinceStart = elapsedMs - event.delayMs
        return reduceMotion || (sinceStart >= 0L && celebration(sinceStart, event.spin, event.jump).showNewOutfit)
    }
}

/**
 * Companion celebration: [newOutfit] (the new clothes on) is revealed with a spin, and [jump]
 * hops twice while waving. Starts [delayMs] after the companion appears.
 */
@Immutable
data class CompanionCelebration(
    val newOutfit: Outfit?,
    val jump: Boolean,
    val delayMs: Long = 0L,
) {
    val spin: Boolean get() = newOutfit != null
}
