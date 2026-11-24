package fi.clocklings.ui.intro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import fi.clocklings.R
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Greeting
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.LocalClockTime
import fi.clocklings.ui.greetingText
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.sectionBackground
import kotlinx.coroutines.delay
import fi.clocklings.ui.intro.IntroTimeline as T

/** A skippable logo greeting. System reduced motion shows a still pose and finishes without movement. */
@Composable
fun IntroOverlay(
    onTick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val readTime = LocalClockTime.current
    val target = remember { readTime() }
    val elapsed = remember { mutableLongStateOf(0L) }
    val currentOnDone by rememberUpdatedState(onDone)
    IntroTimelineEffect(reduceMotion, onFrame = { elapsed.longValue = it }, onTick = onTick, onDone = onDone)
    val time = { if (reduceMotion) T.FADE_START_MS else elapsed.longValue }
    val title = stringResource(R.string.app_name)
    val greeting = stringResource(greetingText(timeLanguage(LocalAppLanguage.current).greeting(target)))
    val description = stringResource(R.string.intro_description, title, greeting)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = T.overlayAlpha(time()) }
            .sectionBackground(Section.HOME)
            .clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = stringResource(R.string.intro_skip),
                onClick = { currentOnDone() },
            ).semantics { contentDescription = description }
            .safeDrawingPadding()
            .padding(horizontal = SkDimens.Gutter),
        contentAlignment = Alignment.Center,
    ) {
        val mascotSize = min(SkDimens.ClockMax, min(maxWidth, maxHeight * MASCOT_HEIGHT_SHARE))
        Column(
            Modifier
                .widthIn(max = SkDimens.ReadingWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clearAndSetSemantics { }
                .graphicsLayer { alpha = T.contentAlpha(time()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            LogoMascot(
                time = time,
                reducedMotion = reduceMotion,
                modifier =
                    Modifier.size(mascotSize).graphicsLayer {
                        scaleX = T.mascotScale(time())
                        scaleY = scaleX
                        alpha = T.mascotAlpha(time())
                    },
            )
            IntroTitle(title, time)
            Text(
                greeting,
                style = SkType.Tagline,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = T.greetingAlpha(time()) },
            )
        }
    }
}

/** Prevents accessibility and keyboard focus from entering the covered content. */
fun Modifier.coveredByIntro(covered: Boolean): Modifier =
    focusProperties { onEnter = { if (covered) cancelFocusChange() } }
        .focusGroup()
        .then(if (covered) Modifier.clearAndSetSemantics { } else Modifier)

/**
 * Hands the intro's greeting to [onGreeting] for the narrator once the jingle has faded. It lives
 * beside the overlay so the short startup can finish while the jingle and greeting continue on Home.
 */
@Composable
fun IntroGreetingVoiceEffect(
    active: Boolean,
    onGreeting: (Greeting) -> Unit,
) {
    var finished by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    val language = LocalAppLanguage.current
    val currentOnGreeting by rememberUpdatedState(onGreeting)
    LaunchedEffect(active) {
        if (!active) finished = true
        if (finished) return@LaunchedEffect
        val greeting = timeLanguage(language).greeting(ClockTime.now())
        delay(if (reduceMotion) T.STILL_GREETING_VOICE_MS else T.GREETING_VOICE_MS)
        finished = true
        currentOnGreeting(greeting)
    }
}

@Composable
private fun IntroTimelineEffect(
    reduceMotion: Boolean,
    onFrame: (Long) -> Unit,
    onTick: () -> Unit,
    onDone: () -> Unit,
) {
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnTick by rememberUpdatedState(onTick)
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            delay(T.STILL_MS)
        } else {
            val start = withFrameNanos { it }
            var elapsed = 0L
            var ticked = false
            while (elapsed < T.END_MS) {
                elapsed = withFrameNanos { (it - start) / NANOS_PER_MS }
                currentOnFrame(elapsed)
                if (!ticked && elapsed >= T.TICK_AT_MS) {
                    ticked = true
                    currentOnTick()
                }
            }
        }
        currentOnDone()
    }
}

/** Keeps the staggered lettering inside the screen even with 200% system text. */
@Composable
private fun IntroTitle(
    title: String,
    time: () -> Long,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val naturalWidth = title.sumOf { measurer.measure(it.toString(), SkType.Logo).size.width }
        val ratio = (with(density) { maxWidth.toPx() } / naturalWidth).coerceAtMost(1f)
        val style =
            SkType.Logo.copy(
                fontSize = SkType.Logo.fontSize * ratio,
                lineHeight = SkType.Logo.lineHeight * ratio,
            )
        Row {
            title.forEachIndexed { i, c ->
                Text(
                    c.toString(),
                    style = style,
                    color = SkColors.logoColor(i),
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = T.letterScale(time(), i)
                            scaleY = scaleX
                            alpha = T.letterAlpha(time(), i)
                        },
                )
            }
        }
    }
}

private const val MASCOT_HEIGHT_SHARE = 0.45f
private const val NANOS_PER_MS = 1_000_000L
