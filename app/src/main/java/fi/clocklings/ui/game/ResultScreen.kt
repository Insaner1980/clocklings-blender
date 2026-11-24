package fi.clocklings.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.Screen
import fi.clocklings.audio.Sound
import fi.clocklings.audio.companionVoice
import fi.clocklings.audio.resultVoice
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Levels
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.StarIcon
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.components.excludeConfetti
import fi.clocklings.ui.levelName
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Tasotulos: tähdet, uudet vaatteet ja jatkonapit. Kaikki lasketaan avaimesta,
 * joten näkymä on sama, vaikka siihen palattaisiin myöhemmin.
 */
@Composable
fun ResultScreen(
    access: AccessPolicy,
    result: Screen.Result,
    outfit: Outfit,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onDress: () -> Unit,
    onLevels: () -> Unit,
    modifier: Modifier = Modifier,
    onSound: (Sound, Float) -> Unit = { _, _ -> },
    canReplay: Boolean = true,
    canContinue: Boolean = true,
) {
    val stars = result.stars
    val after = result.starsBefore + maxOf(0, stars - result.previousBest)
    val personalization = LocalPersonalization.current
    val kind = personalization.companion
    val color = personalization.styleFor(kind).color
    val newItems = visibleUnlocks(result.starsBefore, after, kind).filter { access.item(it, after) }
    val text = resultMessage(result, newItems.size)

    val confetti = LocalConfetti.current
    val haptic = LocalHapticFeedback.current
    var celebrated by rememberSaveable { mutableStateOf(false) }
    // Nukke juhlii vain ensimmäisellä kerralla, ei palattaessa samaan tulokseen.
    val firstVisit = remember { !celebrated }
    val dollCelebration = rememberDollCelebration(firstVisit, kind, outfit, newItems, stars, onSound)
    val anchor = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        if (celebrated) return@LaunchedEffect
        celebrated = true
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(CONFETTI_DELAY_MS)
        confetti?.burst(anchor.value, RESULT_CONFETTI)
    }

    val hasNext = canContinue && result.level < Levels.all.lastIndex
    ScreenScaffold(
        modifier,
        section = Section.PLAY,
        topBar = { RoundIconButton(SkIcons.Close, stringResource(R.string.back_description), onLevels) },
        bottomBar = { ResultActions(hasNext, canReplay, onNextLevel, onPlayAgain, onLevels) },
    ) {
        ResultLayout(viewportHeight, kind) {
            Box { ResultStars(stars, onSound.takeIf { firstVisit }) { anchor.value = it } }
            Box(Modifier.testTag("result_companion")) {
                if (kind != CompanionKind.NONE) {
                    CompanionView(kind, outfit, Modifier.fillMaxSize(), celebration = dollCelebration)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier.fillMaxWidth().excludeConfetti(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.Gap),
                ) {
                    ResultTitle(stars)
                    Text(text, style = SkType.ResultText, textAlign = TextAlign.Center)
                }
                if (newItems.isNotEmpty()) {
                    Spacer(Modifier.height(SkDimens.Gap))
                    ResultRewards(newItems, outfit, color, onDress)
                }
            }
        }
    }
}

@Composable
private fun ResultLayout(
    viewport: Dp,
    kind: CompanionKind,
    content: @Composable () -> Unit,
) {
    val hasCompanion = kind != CompanionKind.NONE
    val ratio = companionAspectRatio(kind)
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { children, constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val starRow = children[0].measure(loose)
        val details = children[2].measure(loose)
        val gapAbove = SkDimens.GapM.roundToPx()
        val gapBelow = if (hasCompanion) SkDimens.Gap.roundToPx() else 0
        val fade = SkDimens.ScrollFade.roundToPx()
        val available =
            viewport.roundToPx() - SkDimens.GapScreen.roundToPx() - fade -
                starRow.height - details.height - gapAbove - gapBelow
        val artMax = (minOf(SkDimens.DollResult.roundToPx(), width) / ratio).roundToInt()
        val artMin = minOf(artMax, SkDimens.ResultCompanionMinHeight.roundToPx())
        val artHeight = if (hasCompanion) available.coerceIn(artMin, artMax) else 0
        val art = children[1].measure(Constraints.fixed((artHeight * ratio).roundToInt(), artHeight))
        val artTop = starRow.height + gapAbove
        val detailsTop = artTop + art.height + gapBelow
        layout(width, detailsTop + details.height + fade) {
            starRow.placeRelative((width - starRow.width) / 2, 0)
            art.placeRelative((width - art.width) / 2, artTop)
            details.placeRelative((width - details.width) / 2, detailsTop)
        }
    }
}

@Composable
private fun ResultActions(
    hasNext: Boolean,
    canReplay: Boolean,
    onNextLevel: () -> Unit,
    onPlayAgain: () -> Unit,
    onLevels: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        if (canReplay) {
            SkButton(
                stringResource(R.string.play_again),
                onPlayAgain,
                Modifier.fillMaxWidth(),
                kind = if (hasNext) ButtonKind.SECONDARY else ButtonKind.PRIMARY,
                icon = SkIcons.Replay,
            )
        }
        if (hasNext) {
            SkButton(
                stringResource(R.string.next_level),
                onNextLevel,
                Modifier.fillMaxWidth(),
                kind = ButtonKind.PRIMARY,
                trailingIcon = SkIcons.Chevron,
            )
        }
        if (!hasNext &&
            !canReplay
        ) {
            SkButton(stringResource(R.string.back_description), onLevels, Modifier.fillMaxWidth())
        }
    }
}

/**
 * The companion's celebration on the first visit, or null when returning to the same result.
 * As it starts, the narrator reads the headline; with new items the unlock sound plays instead and
 * the narrator announces the reward once the new outfit has spun in. The companion speaks last.
 */
@Composable
private fun rememberDollCelebration(
    firstVisit: Boolean,
    kind: CompanionKind,
    outfit: Outfit,
    newItems: List<Item>,
    stars: Int,
    onSound: (Sound, Float) -> Unit,
): CompanionCelebration? {
    val currentOnSound by rememberUpdatedState(onSound)
    LaunchedEffect(kind, firstVisit) {
        if (!firstVisit) return@LaunchedEffect
        delay(DOLL_CELEBRATE_DELAY_MS)
        if (newItems.isNotEmpty()) {
            currentOnSound(Sound.UNLOCK, 1f)
            delay(VOICE_AFTER_UNLOCK_MS)
            currentOnSound(Sound.RESULT_REWARD, 1f)
        } else {
            currentOnSound(resultVoice(stars), 1f)
        }
        delay(COMPANION_AFTER_NARRATION_MS)
        companionVoice(kind)?.let { currentOnSound(it, 1f) }
    }
    return remember(firstVisit) { if (firstVisit) dollCelebration(outfit, newItems, stars) else null }
}

/** Tähti ponnahtaa esiin pyörähtäen. [onPop] plays its sound as it pops, also without motion. */
@Composable
private fun PoppingStar(
    on: Boolean,
    delayMs: Int,
    onPop: (() -> Unit)?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val k = remember { Animatable(if (on && !reduceMotion) 0f else 1f) }
    val currentOnPop by rememberUpdatedState(onPop)
    LaunchedEffect(on) {
        if (!on) return@LaunchedEffect
        delay(delayMs.toLong())
        currentOnPop?.invoke()
        if (!reduceMotion) {
            k.animateTo(
                1f,
                keyframes {
                    durationMillis = POP_MS
                    0f at 0
                    POP_OVERSHOOT at (POP_MS * POP_PEAK_FRACTION).toInt()
                    1f at POP_MS
                },
            )
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        StarIcon(false, size = size)
        if (on) {
            StarIcon(
                true,
                Modifier.graphicsLayer {
                    val scale = if (reduceMotion) 1f else k.value
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (scale < 1f) -POP_ROTATION * (1f - scale) else 0f
                },
                size = size,
            )
        }
    }
}

/** The items of [companion], the one shown on the result, that this result unlocked; none without a companion. */
internal fun visibleUnlocks(
    before: Int,
    after: Int,
    companion: CompanionKind,
): List<Item> = Wardrobe.newlyUnlocked(companion, before, after)

private const val MAX_STARS = 3
private const val POP_MS = 500
private const val CONFETTI_DELAY_MS = 250L
private const val RESULT_CONFETTI = 120

/** Nukke alkaa juhlia, kun kolmas tähti on ponnahtanut esiin. */
private const val DOLL_CELEBRATE_DELAY_MS = 1_100L

/** The narrator announces the reward after the spin into the new outfit, as the unlock sound fades. */
private const val VOICE_AFTER_UNLOCK_MS = 1_200L

/** The companion speaks once the narrator's line (at most 1.9 s in any language) has ended. */
private const val COMPANION_AFTER_NARRATION_MS = 2_000L

/** Each star sounds higher: root, major third and fifth as SoundPool playback rates. */
private val STAR_RATES = floatArrayOf(1f, 1.26f, 1.498f)

private const val POP_OVERSHOOT = 1.2f
private const val POP_PEAK_FRACTION = 0.7f
private const val POP_ROTATION = 40f

@Composable
private fun ResultTitle(stars: Int) {
    Text(
        stringResource(
            when (stars) {
                MAX_STARS -> R.string.result_title_3
                2 -> R.string.result_title_2
                else -> R.string.result_title_1
            },
        ),
        style = SkType.ResultTitle,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun resultMessage(
    result: Screen.Result,
    newItemCount: Int,
): String {
    val stars = result.stars
    val name = stringResource(levelName(result.level))
    return buildList {
        add(pluralStringResource(R.plurals.result_text, stars, name, stars))
        if (result.previousBest > stars) {
            add(pluralStringResource(R.plurals.result_previous_best, result.previousBest, result.previousBest))
        }
        when {
            newItemCount == 1 -> add(stringResource(R.string.result_new_item))
            newItemCount > 1 -> add(stringResource(R.string.result_new_items))
            stars < MAX_STARS -> add(stringResource(R.string.result_play_more))
        }
    }.joinToString(" ")
}

/**
 * Kolme tähteä, jotka ponnahtavat esiin vuorotellen. [onCenter] antaa konfetin lähtöpisteen.
 * With [onSound] each earned star plays the star sound one step higher than the previous one.
 */
@Composable
private fun ResultStars(
    stars: Int,
    onSound: ((Sound, Float) -> Unit)?,
    onCenter: (Offset) -> Unit,
) {
    val description = pluralStringResource(R.plurals.result_stars_description, stars, stars)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description }.confettiAnchor(onCenter),
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapOptions),
    ) {
        repeat(MAX_STARS) { k ->
            PoppingStar(
                on = k < stars,
                size = if (k == 1) SkDimens.StarResultMiddle else SkDimens.StarResultSide,
                modifier = Modifier.offset(y = if (k == 1) -SkDimens.GapS else 0.dp),
                delayMs = STAR_DELAY_MS + k * STAR_STAGGER_MS,
                onPop = onSound?.let { play -> { play(Sound.STAR, STAR_RATES[k]) } },
            )
        }
    }
}

/** Nuken juhlinta: uudet vaatteet pyörähdyksellä ja kolmesta tähdestä hyppy vilkuttaen. */
private fun dollCelebration(
    outfit: Outfit,
    newItems: List<Item>,
    stars: Int,
): CompanionCelebration =
    CompanionCelebration(
        newOutfit = newItems.takeIf { it.isNotEmpty() }?.fold(outfit) { o, item -> o.with(item.slot, item.id) },
        jump = stars == MAX_STARS,
        delayMs = DOLL_CELEBRATE_DELAY_MS,
    )

private const val STAR_DELAY_MS = 150
private const val STAR_STAGGER_MS = 250
