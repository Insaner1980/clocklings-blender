// Keep the private drawing layers next to the clock canvas that owns their order.
@file:Suppress("TooManyFunctions")

package fi.clocklings.ui.clock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.DayPart
import fi.clocklings.domain.Hand
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.Mark
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.appearance.clockThemeName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Fredoka
import fi.clocklings.ui.theme.Nunito
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import kotlin.math.hypot
import fi.clocklings.ui.clock.ClockSpec as S

/**
 * Kellotaulu. Piirretään yhdellä Canvasilla web-version SVG:n järjestyksessä.
 * Tila (viisarien kulmat ja animaatio) on tämän sisällä; kutsuja ohjaa sitä [command]-käskyillä.
 *
 * @param period 720 (12 h) tai 1440 (24 h: aurinko tai kuu ja vuorokaudenaika näkyvät)
 * @param step raahauksen pyöristys minuutteina
 * @param help yli- ja vaille-puoliskot, minuuttiluvut ja 24 h -tilassa sisäkehän luvut
 * @param mini pieni koristekello ilman apua ja 24 h -osia
 * @param onChange kutsutaan, kun käyttäjä muuttaa aikaa (raahaus tai saavutettavuustoiminto)
 * @param paintRingFrom aloitusnäkymä: sateenkaarirengas maalautuu minuuttiviisarin perässä tästä ajasta alkaen
 * @param onSettle kutsutaan, kun animoitu siirto on kulkenut loppuun
 */
@Composable
fun ClockFace(
    command: ClockCommand,
    modifier: Modifier = Modifier,
    period: Int = ClockTime.HALF_DAY,
    step: Int = 5,
    interactive: Boolean = false,
    help: Boolean = false,
    mini: Boolean = false,
    describeTime: Boolean = true,
    highlight: Hand? = null,
    under: List<Mark> = emptyList(),
    over: List<Mark> = emptyList(),
    onChange: ((Int) -> Unit)? = null,
    paintRingFrom: Int? = null,
    onSettle: (() -> Unit)? = null,
    appearance: ClockAppearance = LocalPersonalization.current.clock,
) {
    val state = rememberClockState(command, period, step, onChange, onSettle)
    val reduceMotion = rememberReduceMotion()

    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    val plainNumbers = appearance.numerals == NumeralStyle.PLAIN
    val styles = remember(density, plainNumbers) { ClockTextStyles(density, plainNumbers) }
    val is24 = period == ClockTime.DAY && !mini

    val pulse = highlightPulse(highlight, reduceMotion)

    val immediateTime = command.immediateMiniTime(mini, interactive, period)
    val time = immediateTime ?: state.value
    val dayPart = timeLanguage(LocalAppLanguage.current).dayPart(time)
    val themeLabel = stringResource(clockThemeName(appearance.theme))
    val description = clockDescription(time, period, describeTime)
    val zoneLabels = clockZoneLabels()
    val addLabel = pluralStringResource(R.plurals.clock_add, step, step)
    val subtractLabel = pluralStringResource(R.plurals.clock_subtract, step, step)
    val currentInteractive by rememberUpdatedState(interactive)
    var focused by remember { mutableStateOf(false) }
    val origin = if (mini) S.MINI_VIEW_ORIGIN else S.VIEW_ORIGIN
    val viewSize = if (mini) S.MINI_VIEW_SIZE else S.VIEW_SIZE

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = description
                stateDescription = themeLabel
                if (interactive) {
                    customActions = clockActions(state, addLabel, subtractLabel) { currentInteractive }
                }
            }.onFocusChanged { focused = it.isFocused }
            .clockKeyboard(state, interactive, focused)
            .clockDrag(state, origin, viewSize) { currentInteractive },
    ) {
        val scale = size.width / viewSize
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
            translate(-origin, -origin)
        }) {
            val raw = immediateTime?.toFloat() ?: state.raw
            drawClockRim(appearance, raw, paintRingFrom, period)
            val showHelp = help && !mini
            if (showHelp) drawHelp(textMeasurer, styles, is24, zoneLabels, raw)
            drawClockDepth(mini)
            under.forEach { drawMark(it, textMeasurer, styles) }
            drawTicks()
            drawNumbers(textMeasurer, styles)
            if (is24) drawDayPart(dayPart, textMeasurer, styles)
            drawHands(raw, highlight, pulse, interactive && !mini, appearance.handsAt(mini))
            drawCoveredNumerals(textMeasurer, styles, raw, showHelp, under)
            drawCircle(SkColors.Ink, S.PIN_RADIUS, Offset(S.CENTER, S.CENTER))
            drawCircle(SkColors.Face, S.PIN_DOT, Offset(S.CENTER, S.CENTER))
            over.forEach { drawMark(it, textMeasurer, styles) }
            if (!mini) drawGlassArc()
        }
    }
}

// Passive thumbnails render immediate commands without waiting for the interaction state's effect.
private fun ClockCommand.immediateMiniTime(
    mini: Boolean,
    interactive: Boolean,
    period: Int,
): Int? = if (mini && !interactive && ms == 0) ClockTime.mod(time, period) else null

private fun DrawScope.drawClockRim(
    appearance: ClockAppearance,
    raw: Float,
    paintRingFrom: Int?,
    period: Int,
) {
    val reveal = paintRingFrom?.let { S.ringReveal(raw, it, period) } ?: ClockTime.FULL_TURN
    if (appearance.theme == ClockTheme.RAINBOW) drawRing(reveal) else drawThemedRim(appearance, reveal)
    drawCircle(
        Brush.radialGradient(
            listOf(SkColors.Face, SkColors.FaceEdge),
            center = Offset(S.CENTER, S.CENTER),
            radius = S.FACE_RADIUS,
        ),
        S.FACE_RADIUS,
        Offset(S.CENTER, S.CENTER),
    )
}

/** Arrowheads are noise at thumbnail size, so mini clocks always use round hands. */
private fun ClockAppearance.handsAt(mini: Boolean): HandStyle = if (mini) HandStyle.ROUND else hands

/** A soft inner shadow along the rim; thumbnails stay flat and crisp. */
private fun DrawScope.drawClockDepth(mini: Boolean) {
    if (mini) return
    drawCircle(
        SkColors.FaceShadow,
        S.FACE_RADIUS - SkDimens.CLOCK_INNER_SHADOW_WIDTH / 2,
        Offset(S.CENTER, S.CENTER),
        style = Stroke(SkDimens.CLOCK_INNER_SHADOW_WIDTH),
    )
}

private fun DrawScope.drawGlassArc() {
    val radius = S.FACE_RADIUS - SkDimens.CLOCK_GLASS_INSET
    drawArc(
        SkColors.Glass,
        SkDimens.CLOCK_GLASS_START,
        SkDimens.CLOCK_GLASS_SWEEP,
        false,
        topLeft = Offset(S.CENTER - radius, S.CENTER - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(SkDimens.CLOCK_GLASS_WIDTH, cap = StrokeCap.Round),
    )
}

/** Finnish and Swedish can refer to the half hour on either side of the face. */
@Composable
private fun clockZoneLabels(): Pair<String, String>? =
    if (LocalAppLanguage.current == AppLanguage.ENGLISH) {
        stringResource(R.string.clock_zone_yli) to stringResource(R.string.clock_zone_vaille)
    } else {
        null
    }

@Composable
private fun clockDescription(
    time: Int,
    period: Int,
    describeTime: Boolean,
): String =
    if (describeTime) {
        stringResource(
            R.string.clock_description,
            timeLanguage(LocalAppLanguage.current).fullWords(
                time,
                h24 =
                    period == ClockTime.DAY,
            ),
        )
    } else {
        stringResource(R.string.clock_question_description)
    }

/** Fonttikoot kellon koordinaatistossa: px-koko = SVG-yksikkö, koska Canvas skaalataan. */
internal class ClockTextStyles(
    private val density: Density,
    plainNumbers: Boolean = false,
) {
    private fun style(
        family: FontFamily,
        weight: FontWeight,
        size: Float,
        color: Color,
    ) = TextStyle(fontFamily = family, fontWeight = weight, fontSize = with(density) { size.toSp() }, color = color)

    val number = style(if (plainNumbers) Nunito else Fredoka, FontWeight.SemiBold, S.NUMBER_FONT, SkColors.Ink)

    /** Outline of a numeral redrawn over the minute hand; the colour is set per draw call. */
    val numberHalo = number.copy(drawStyle = Stroke(width = S.NUMERAL_HALO, join = StrokeJoin.Round))
    val minute = style(Nunito, FontWeight.ExtraBold, S.MINUTE_FONT, SkColors.InkSoft)
    val zone = style(Fredoka, FontWeight.SemiBold, S.ZONE_FONT, SkColors.InkSoft)
    val inner24 = style(Nunito, FontWeight.ExtraBold, S.INNER_24_FONT, SkColors.Hour24)
    val day = style(Fredoka, FontWeight.SemiBold, S.DAY_FONT, SkColors.Ink)
    val bubble = minute.copy(fontSize = with(density) { S.BUBBLE_FONT.toSp() })
}

/** A crescent around the origin, drawn at the sun's centre ([S.CENTER], [S.SUN_CY]). */
private val MoonPath: Path = PathParser().parsePathString("M3 -12A12 12 0 0 0 3 12A7 12 0 0 1 3 -12Z").toPath()

/** Teksti keskitettynä pisteeseen (SVG: text-anchor middle, dominant-baseline central). */
private fun DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    at: Offset,
) {
    val layout = measurer.measure(text, style)
    drawLaidOut(layout, Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

/** Teksti peruslinjalle [baselineY] (SVG:n oletus). */
private fun DrawScope.drawOnBaseline(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    baselineY: Float,
) {
    val layout = measurer.measure(text, style)
    drawLaidOut(layout, Offset(x - layout.size.width / 2f, baselineY - layout.firstBaseline))
}

/**
 * Styles that differ only in paint (the numeral and its halo outline) share one cached paragraph, and a
 * null draw style leaves that paragraph's last Stroke in place, so a filled style passes Fill explicitly.
 */
private fun DrawScope.drawLaidOut(
    layout: TextLayoutResult,
    topLeft: Offset,
) = drawText(layout, topLeft = topLeft, drawStyle = layout.layoutInput.style.drawStyle ?: Fill)

/** Sateenkaarirengas; [revealDeg] alle täyden kierroksen piirtää maalaamattoman osan vaaleana. */
private fun DrawScope.drawRing(revealDeg: Float) {
    val r = S.RING_RADIUS
    val arc = { color: Color, from: Float, sweep: Float ->
        drawArc(
            color = color,
            startAngle = from - ClockTime.FULL_TURN / 4,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(S.CENTER - r, S.CENTER - r),
            size = Size(2 * r, 2 * r),
            style = Stroke(S.RING_WIDTH),
        )
    }
    if (revealDeg >= ClockTime.FULL_TURN) {
        for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
            arc(SkColors.Rainbow[i], i * S.RING_PART_DEG - S.RING_PART_DEG / 2, S.RING_PART_DEG)
        }
        return
    }
    arc(SkColors.Line, 0f, ClockTime.FULL_TURN)
    for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
        for ((from, sweep) in S.ringPieces(i, revealDeg)) arc(SkColors.Rainbow[i], from, sweep)
    }
}

private fun DrawScope.drawHelp(
    measurer: TextMeasurer,
    styles: ClockTextStyles,
    is24: Boolean,
    zoneLabels: Pair<String, String>?,
    raw: Float,
) {
    val r = S.HALF_RADIUS
    val topLeft = Offset(S.CENTER - r, S.CENTER - r)
    val size = Size(2 * r, 2 * r)
    drawArc(SkColors.Yli, -QUARTER_TURN, ClockTime.HALF_TURN, useCenter = true, topLeft = topLeft, size = size)
    drawArc(SkColors.Vaille, QUARTER_TURN, ClockTime.HALF_TURN, useCenter = true, topLeft = topLeft, size = size)
    zoneLabels?.let { (past, to) ->
        val angle = S.zoneLabelAngle(is24)
        val radius = S.zoneLabelRadius(is24)
        drawZoneLabel(measurer, past, styles.zone, S.point(radius, angle), zoneLabelAlpha(angle, raw))
        drawZoneLabel(measurer, to, styles.zone, S.point(radius, -angle), zoneLabelAlpha(-angle, raw))
    }
    for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
        drawCentered(
            measurer,
            (i * ClockTime.MINUTES_PER_NUMBER).toString(),
            styles.minute,
            S.point(
                S.MINUTE_LABEL_RADIUS,
                i * S.DEGREES_PER_HOUR,
            ),
        )
    }
    if (is24) {
        for (n in 1..ClockTime.HOURS_PER_HALF_DAY) {
            drawCentered(
                measurer,
                ((n + ClockTime.HOURS_PER_HALF_DAY) % ClockTime.HOURS_PER_DAY).toString(),
                styles.inner24,
                S.point(
                    S.INNER_24_RADIUS,
                    n * S.DEGREES_PER_HOUR,
                ),
            )
        }
    }
}

/** A half-face label that fades out while a hand lies over it, so no clipped word peeks out. */
private fun DrawScope.drawZoneLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    center: Offset,
    alpha: Float,
) {
    if (alpha > 0f) {
        drawCentered(measurer, text, style.copy(color = style.color.copy(alpha = alpha)), center)
    }
}

private fun zoneLabelAlpha(
    angle: Float,
    raw: Float,
): Float {
    val nearest =
        minOf(
            ClockTime.angleDiff(angle, ClockTime.hourAngle(raw)),
            ClockTime.angleDiff(angle, ClockTime.minuteAngle(raw)),
        )
    return ((nearest - S.ZONE_HIDE_ANGLE) / (S.ZONE_FADE_ANGLE - S.ZONE_HIDE_ANGLE)).coerceIn(0f, 1f)
}

private fun DrawScope.drawTicks() {
    for (i in 0 until ClockTime.MINUTES_PER_HOUR) {
        val big = i % ClockTime.MINUTES_PER_NUMBER == 0
        drawLine(
            color = if (big) SkColors.Ink else SkColors.Tick,
            start = S.point(if (big) S.TICK_BIG_INNER else S.TICK_SMALL_INNER, i * ClockTime.DEGREES_PER_MINUTE),
            end = S.point(S.TICK_OUTER, i * ClockTime.DEGREES_PER_MINUTE),
            strokeWidth = if (big) S.TICK_BIG_WIDTH else S.TICK_SMALL_WIDTH,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawNumbers(
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    for (n in 1..ClockTime.HOURS_PER_HALF_DAY) drawNumeral(measurer, n, styles.number)
}

private fun DrawScope.drawNumeral(
    measurer: TextMeasurer,
    n: Int,
    style: TextStyle,
) = drawCentered(measurer, n.toString(), style, S.point(S.NUMBER_RADIUS, n * S.DEGREES_PER_HOUR))

/**
 * The minute hand has to reach the minute ticks, so its shaft always crosses the numeral it points at.
 * Redrawing that numeral on top, outlined in the colour behind it, makes the hand pass beneath it.
 */
private fun DrawScope.drawCoveredNumerals(
    measurer: TextMeasurer,
    styles: ClockTextStyles,
    raw: Float,
    help: Boolean,
    under: List<Mark>,
) {
    for (n in S.numeralsUnder(ClockTime.minuteAngle(raw))) {
        val halo = S.numeralHalo(n, help, under)
        if (halo.before == halo.after) {
            drawNumeral(measurer, n, styles.numberHalo.copy(color = halo.before))
        } else {
            // The numeral sits on a colour boundary, so each side of its radial line gets its own halo.
            drawNumeralSide(n, after = false) { drawNumeral(measurer, n, styles.numberHalo.copy(color = halo.before)) }
            drawNumeralSide(n, after = true) { drawNumeral(measurer, n, styles.numberHalo.copy(color = halo.after)) }
        }
        drawNumeral(measurer, n, styles.number)
    }
}

/** Runs [block] clipped to the half of the face on one side of the radial line through numeral [n]. */
private fun DrawScope.drawNumeralSide(
    n: Int,
    after: Boolean,
    block: DrawScope.() -> Unit,
) {
    val angle = n * S.DEGREES_PER_HOUR
    val along = S.point(S.FACE_RADIUS, angle) - Offset(S.CENTER, S.CENTER)
    // Clockwise of the line is the "after" side; the perpendicular of a clockwise unit step is (cos, sin).
    val across = S.point(S.FACE_RADIUS, angle + QUARTER_TURN) - Offset(S.CENTER, S.CENTER)
    val side = if (after) across else -across
    val centre = Offset(S.CENTER, S.CENTER)
    val half =
        Path().apply {
            moveTo((centre - along).x, (centre - along).y)
            lineTo((centre + along).x, (centre + along).y)
            lineTo((centre + along + side).x, (centre + along + side).y)
            lineTo((centre - along + side).x, (centre - along + side).y)
            close()
        }
    clipPath(half, block = block)
}

private fun DrawScope.drawDayPart(
    part: DayPart,
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    if (part.isSun) {
        val c = Offset(S.CENTER, S.SUN_CY)
        drawCircle(SkColors.Sun, S.SUN_RADIUS, c)
        for (i in 0 until SUN_RAY_COUNT) {
            val from = S.point(S.SUN_RAY_FROM, i * SUN_RAY_ANGLE) - Offset(0f, S.CENTER - S.SUN_CY)
            val to = S.point(S.SUN_RAY_TO, i * SUN_RAY_ANGLE) - Offset(0f, S.CENTER - S.SUN_CY)
            drawLine(SkColors.SunRay, from, to, S.SUN_RAY_WIDTH, StrokeCap.Round)
        }
    } else {
        withTransform({ translate(S.CENTER, S.SUN_CY) }) { drawPath(MoonPath, SkColors.Moon) }
    }
    drawOnBaseline(measurer, part.name, styles.day, S.CENTER, S.DAY_TEXT_Y)
}

private fun DrawScope.drawHands(
    raw: Float,
    highlight: Hand?,
    pulse: Float,
    knobs: Boolean,
    handStyle: HandStyle,
) {
    val hourAlpha = if (highlight == Hand.MINUTE) S.DIMMED_ALPHA else 1f
    val minuteAlpha = if (highlight == Hand.HOUR) S.DIMMED_ALPHA else 1f
    val hourWidth = if (highlight == Hand.HOUR) lerp(S.HOUR_WIDTH, S.HOUR_WIDTH_PULSE, pulse) else S.HOUR_WIDTH
    val minuteWidth =
        if (highlight ==
            Hand.MINUTE
        ) {
            lerp(S.MINUTE_WIDTH, S.MINUTE_WIDTH_PULSE, pulse)
        } else {
            S.MINUTE_WIDTH
        }
    val hour =
        HandGeometry(
            S.point(S.HOUR_TAIL, ClockTime.hourAngle(raw) + ClockTime.HALF_TURN),
            S.point(S.HOUR_LENGTH, ClockTime.hourAngle(raw)),
            hourWidth,
            handStyle,
        )
    val minute =
        HandGeometry(
            S.point(S.MINUTE_TAIL, ClockTime.minuteAngle(raw) + ClockTime.HALF_TURN),
            S.point(S.MINUTE_LENGTH, ClockTime.minuteAngle(raw)),
            minuteWidth,
            handStyle,
        )
    drawHandShadow(hour, hourAlpha)
    drawHandShadow(minute, minuteAlpha)
    drawHandShape(hour, SkColors.Face.copy(alpha = hourAlpha), extraWidth = S.HAND_HALO * 2)
    drawHandBody(hour, SkColors.Hour.copy(alpha = hourAlpha))
    drawHandShape(minute, SkColors.Face.copy(alpha = minuteAlpha), extraWidth = S.HAND_HALO * 2)
    drawHandBody(minute, SkColors.Minute.copy(alpha = minuteAlpha))
    // Knobs go on top of both hands so an overlapping hand never hides the other one's grip.
    // Pointed hands use the arrowhead itself as the grip, so they get no knob.
    if (knobs && handStyle == HandStyle.ROUND) {
        drawKnob(hour.tip, S.HOUR_KNOB, SkColors.Hour.copy(alpha = hourAlpha), hourAlpha)
        drawKnob(minute.tip, S.MINUTE_KNOB, SkColors.Minute.copy(alpha = minuteAlpha), minuteAlpha)
    }
}

private fun lerp(
    a: Float,
    b: Float,
    t: Float,
) = a + (b - a) * t

/** One hand from its tail [end] to its [tip]. A pointed hand's shaft stops where the arrowhead starts. */
private class HandGeometry(
    val end: Offset,
    val tip: Offset,
    val width: Float,
    style: HandStyle,
) {
    private val direction: Offset =
        (tip - end).let { d -> d / hypot(d.x, d.y) }
    val head: Path? =
        if (style == HandStyle.POINTER) {
            val base = tip - direction * width * S.POINTER_HEAD_LENGTH
            val normal = Offset(-direction.y, direction.x) * (width * S.POINTER_HEAD_WIDTH / 2)
            Path().apply {
                moveTo(tip.x, tip.y)
                lineTo((base + normal).x, (base + normal).y)
                lineTo((base - normal).x, (base - normal).y)
                close()
            }
        } else {
            null
        }

    /** Where the shaft ends: the tip for round hands, inside the arrowhead base for pointed ones. */
    val shaftEnd: Offset =
        if (head != null) tip - direction * width * (S.POINTER_HEAD_LENGTH - S.POINTER_SHAFT_OVERLAP) else tip
}

private fun DrawScope.drawHandShape(
    hand: HandGeometry,
    color: Color,
    offset: Offset = Offset.Zero,
    extraWidth: Float = 0f,
) {
    drawLine(color, hand.end + offset, hand.shaftEnd + offset, hand.width + extraWidth, StrokeCap.Round)
    hand.head?.let { head ->
        withTransform({ translate(offset.x, offset.y) }) {
            drawPath(head, color)
            // A stroke in the same color rounds the arrowhead corners.
            drawPath(
                head,
                color,
                style = Stroke(S.POINTER_CORNER + extraWidth, join = StrokeJoin.Round),
            )
        }
    }
}

// Pehmeä varjo kahdella kerroksella (web: drop-shadow 0 3px 2px)
private fun DrawScope.drawHandShadow(
    hand: HandGeometry,
    alpha: Float,
) {
    val offset = Offset(0f, SkDimens.CLOCK_HAND_SHADOW_OFFSET)
    val shadow = SkColors.HandShadow.copy(alpha = SkColors.HandShadow.alpha * alpha / 2)
    drawHandShape(hand, shadow, offset, SkDimens.CLOCK_HAND_SHADOW_SPREAD)
    drawHandShape(hand, shadow, offset)
}

private fun DrawScope.drawHandBody(
    hand: HandGeometry,
    color: Color,
) = drawHandShape(hand, color)

private fun DrawScope.drawKnob(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(color, radius, center)
    drawCircle(SkColors.Face.copy(alpha = alpha), radius, center, style = Stroke(S.KNOB_STROKE))
}

private fun DrawScope.drawMark(
    mark: Mark,
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    when (mark) {
        is Mark.Sector -> {
            val color = S.markColor(mark.color)
            val r = S.HALF_RADIUS
            if (mark.toMin - mark.fromMin >= ClockTime.MINUTES_PER_HOUR) {
                drawCircle(color, r, Offset(S.CENTER, S.CENTER))
            } else {
                drawArc(
                    color = color,
                    startAngle = mark.fromMin * 6f - 90f,
                    sweepAngle = (mark.toMin - mark.fromMin) * 6f,
                    useCenter = true,
                    topLeft = Offset(S.CENTER - r, S.CENTER - r),
                    size = Size(2 * r, 2 * r),
                )
            }
        }

        is Mark.NumberGlow -> {
            drawCircle(SkColors.MarkNumber, S.GLOW_NUMBER_RADIUS, S.point(S.NUMBER_RADIUS, mark.n * S.DEGREES_PER_HOUR))
        }

        is Mark.TickGlow -> {
            val at = S.point(S.GLOW_TICK_AT, mark.minute * ClockTime.DEGREES_PER_MINUTE)
            drawCircle(SkColors.MarkTick, S.GLOW_TICK_RADIUS, at)
            drawCircle(SkColors.Hour, S.GLOW_TICK_RADIUS, at, style = Stroke(S.BUBBLE_STROKE))
        }

        is Mark.Bubble -> {
            val at = S.point(S.MINUTE_LABEL_RADIUS, mark.minute * ClockTime.DEGREES_PER_MINUTE)
            drawCircle(SkColors.Face, S.BUBBLE_RADIUS, at)
            drawCircle(SkColors.Minute, S.BUBBLE_RADIUS, at, style = Stroke(S.BUBBLE_STROKE))
            drawCentered(measurer, mark.text, styles.bubble, at)
        }
    }
}

private const val QUARTER_TURN = 90f
private const val SUN_RAY_COUNT = 8
private const val SUN_RAY_ANGLE = 45f

private fun Modifier.clockKeyboard(
    state: ClockState,
    interactive: Boolean,
    focused: Boolean,
): Modifier =
    onKeyEvent { event ->
        if (!interactive || event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val direction =
            when (event.key) {
                Key.DirectionRight, Key.DirectionUp -> 1
                Key.DirectionLeft, Key.DirectionDown -> -1
                else -> return@onKeyEvent false
            }
        state.nudge(direction)
        true
    }.pointerHoverIcon(if (interactive) PointerIcon(android.view.PointerIcon.TYPE_GRAB) else PointerIcon.Default)
        .focusable(enabled = interactive)
        .then(if (focused && interactive) Modifier.border(SkDimens.Border, SkColors.Ink, CircleShape) else Modifier)

private fun Modifier.clockDrag(
    state: ClockState,
    origin: Float,
    viewSize: Float,
    interactive: () -> Boolean,
): Modifier =
    this.pointerInput(state, origin, viewSize) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!interactive()) return@awaitEachGesture
            val scale = size.width / viewSize

            fun local(p: Offset) = Offset(p.x / scale + origin - S.CENTER, p.y / scale + origin - S.CENTER)
            val p = local(down.position)
            val angle = ClockTime.angleOf(p.x, p.y)
            val hand = ClockTime.pickHand(hypot(p.x, p.y), angle, state.raw) ?: return@awaitEachGesture
            down.consume()
            state.startDrag(hand, angle)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (!interactive() || change == null || !change.pressed) break
                val q = local(change.position)
                if (hypot(q.x, q.y) >= S.DRAG_MIN_RADIUS) state.dragTo(ClockTime.angleOf(q.x, q.y))
                change.consume()
            }
            state.endDrag()
        }
    }

@Composable
private fun highlightPulse(
    highlight: Hand?,
    reduceMotion: Boolean,
): Float =
    if (highlight != null && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "korostus")
        transition
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        tween(S.PULSE_HALF_MS, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                label = "korostus",
            ).value
    } else {
        0f
    }

private fun clockActions(
    state: ClockState,
    addLabel: String,
    subtractLabel: String,
    interactive: () -> Boolean,
) = listOf(
    CustomAccessibilityAction(addLabel) {
        if (interactive()) {
            state.nudge(1)
            true
        } else {
            false
        }
    },
    CustomAccessibilityAction(subtractLabel) {
        if (interactive()) {
            state.nudge(-1)
            true
        } else {
            false
        }
    },
)

@Composable
private fun rememberClockState(
    command: ClockCommand,
    period: Int,
    step: Int,
    onChange: ((Int) -> Unit)?,
    onSettle: (() -> Unit)?,
): ClockState {
    val scope = rememberCoroutineScope()
    val state = remember { ClockState(command.time, period, scope) }
    val reduceMotion = rememberReduceMotion()
    val haptic = LocalHapticFeedback.current
    SideEffect {
        state.step = step
        state.reduceMotion = reduceMotion
        state.onChange = onChange
        state.onSettle = onSettle
        state.onDragStep = { tick ->
            haptic.performHapticFeedback(
                if (tick ==
                    DragTick.MARK
                ) {
                    HapticFeedbackType.SegmentTick
                } else {
                    HapticFeedbackType.SegmentFrequentTick
                },
            )
        }
    }
    LaunchedEffect(period) { state.updatePeriod(period) }
    LaunchedEffect(command.id) { state.apply(command) }

    return state
}
