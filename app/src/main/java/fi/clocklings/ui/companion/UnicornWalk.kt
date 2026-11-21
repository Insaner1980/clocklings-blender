@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Rest joints in the Blender camera plane. The near hind, near fore, far hind and far fore lift in turn. */
internal enum class UnicornLeg(
    val hip: Offset,
    val joint: Offset,
    val sole: Offset,
    val beat: Float,
    val fore: Boolean,
) {
    FAR_HIND(Offset(134f, 204f), Offset(140f, 235f), Offset(134f, 269f), .5f, false),
    FAR_FORE(Offset(87f, 204f), Offset(78f, 235f), Offset(78f, 271f), .75f, true),
    NEAR_HIND(Offset(156f, 202f), Offset(168f, 237f), Offset(168f, 275f), 0f, false),
    NEAR_FORE(Offset(109f, 202f), Offset(103f, 236f), Offset(104f, 280f), .25f, true),
}

private const val WALK_MS = 1_560L
private const val SWING = .36f
private const val PI_F = PI.toFloat()

internal class UnicornStride(
    timeMs: Long,
    private val jump: Float = 0f,
) {
    private val phase = (timeMs % WALK_MS).toFloat() / WALK_MS
    private val weight = smooth((timeMs / 430f).coerceIn(0f, 1f)) * (1f - smooth(jump.coerceIn(0f, 1f)))
    val bodyY = .9f * weight * sin(4f * PI_F * phase)
    val nod = 1.4f * weight * sin(4f * PI_F * phase)

    fun leg(leg: UnicornLeg): UnicornLegPose {
        val step = (phase - leg.beat + 1f) % 1f
        val swing = (step / SWING).coerceIn(0f, 1f)
        val arc = if (step < SWING) sin(PI_F * swing).let { it * it * weight } else 0f
        // A companion walks on the spot. Lift the heel, advance the toe and settle onto the same footprint.
        val dx = arc * (13f * cos(PI_F * swing) - 6f) + jump * if (leg.fore) -4f else 3f
        val dy = -arc * (if (leg.fore) 12f else 10f) - bodyY - jump * (if (leg.fore) 10f else 7f)
        val roll = -12f * arc * sin(2f * PI_F * swing) + jump * if (leg.fore) -9f else 6f
        return UnicornLegPose(leg, leg.sole + Offset(dx, dy), roll)
    }
}

/** Two rigid limb segments meet at the carpus or hock. The hoof and its boot share one sole transform. */
internal class UnicornLegPose(
    private val leg: UnicornLeg,
    val sole: Offset,
    val roll: Float,
) {
    private val joint: Offset
    private val upperRotation: Offset
    private val lowerRotation: Offset
    private val hoofRotation = Offset(cos(roll * PI_F / 180f), sin(roll * PI_F / 180f))

    init {
        val upper = leg.joint - leg.hip
        val lower = leg.sole - leg.joint
        val axis = sole - leg.hip
        val length = axis.getDistance()
        val projection = (upper.getDistanceSquared() - lower.getDistanceSquared() + length * length) / (2f * length)
        val bend = sqrt((upper.getDistanceSquared() - projection * projection).coerceAtLeast(0f))
        val direction = axis / length
        val perpendicular = Offset(-direction.y, direction.x)
        joint = leg.hip + direction * projection + perpendicular * (if (leg.fore) bend else -bend)
        upperRotation = rotationBetween(upper, joint - leg.hip)
        lowerRotation = rotationBetween(lower, sole - joint)
    }

    fun skin(point: Offset): Offset {
        val upper = leg.hip + rotate(point - leg.hip, upperRotation)
        val lower = joint + rotate(point - leg.joint, lowerRotation)
        val hoof = sole + rotate(point - leg.sole, hoofRotation)
        val limb = blend(upper, lower, transition(point.y, leg.joint.y - 8f, 16f))
        val foot = blend(limb, hoof, transition(point.y, leg.sole.y - 23f, 10f))
        // Let the painted thigh compress into the flank while the hoof keeps its rigid contact.
        val alongLeg = ((point.y - leg.hip.y) / (leg.sole.y - leg.hip.y)).coerceIn(0f, 1f)
        val compressed = point + (sole - leg.sole) * alongLeg
        val bendWeight = .64f + .36f * transition(point.y, leg.sole.y - 26f, 12f)
        return blend(point, blend(compressed, foot, bendWeight), transition(point.y, 210f, 19f))
    }
}

private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

private fun transition(
    y: Float,
    start: Float,
    span: Float,
): Float = smooth(((y - start) / span).coerceIn(0f, 1f))

private fun blend(
    a: Offset,
    b: Offset,
    t: Float,
): Offset = a + (b - a) * t

private fun rotationBetween(
    a: Offset,
    b: Offset,
): Offset {
    val lengths = a.getDistance() * b.getDistance()
    return Offset((a.x * b.x + a.y * b.y) / lengths, (a.x * b.y - a.y * b.x) / lengths)
}

private fun rotate(
    p: Offset,
    rotation: Offset,
): Offset = Offset(p.x * rotation.x - p.y * rotation.y, p.x * rotation.y + p.y * rotation.x)
