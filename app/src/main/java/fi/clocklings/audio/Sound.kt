package fi.clocklings.audio

import fi.clocklings.R
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Greeting
import fi.clocklings.domain.Hint

/**
 * A sound effect. A sound with a [clip] plays a recorded file from res/raw; the others are
 * synthesized by [ToneSynth]. A [voice] is spoken (a companion or the narrator): only one voice
 * plays at a time. A [narration] clip has a recording per app language in the locale-qualified
 * raw folders (English in res/raw).
 */
enum class Sound(
    val clip: Int? = null,
    val voice: Boolean = false,
    val narration: Boolean = false,
) {
    OK(R.raw.correct),
    BAD(R.raw.wrong),
    WIN(R.raw.success),
    POP,
    INTRO(R.raw.clocklings),
    STAR(R.raw.star),
    UNLOCK(R.raw.unlock),
    RECORD(R.raw.record),
    LESSON_COMPLETE(R.raw.lesson_complete),
    EQUIP(R.raw.equip),
    HUMAN(R.raw.companion_human, voice = true),
    CAT(R.raw.companion_cat, voice = true),
    DOG(R.raw.companion_dog, voice = true),
    ROBOT(R.raw.companion_robot, voice = true),
    DRAGON(R.raw.companion_dragon, voice = true),
    DINOSAUR(R.raw.companion_dinosaur, voice = true),
    BUNNY(R.raw.companion_bunny, voice = true),
    UNICORN(R.raw.companion_unicorn, voice = true),
    CAR(R.raw.companion_car, voice = true),
    QUESTION_READ(R.raw.voice_question_read, voice = true, narration = true),
    QUESTION_SET(R.raw.voice_question_set, voice = true, narration = true),
    HINT_HOUR(R.raw.voice_hint_hour, voice = true, narration = true),
    HINT_MINUTE(R.raw.voice_hint_minute, voice = true, narration = true),
    RESULT_3(R.raw.voice_result_3, voice = true, narration = true),
    RESULT_2(R.raw.voice_result_2, voice = true, narration = true),
    RESULT_1(R.raw.voice_result_1, voice = true, narration = true),
    RESULT_REWARD(R.raw.voice_result_reward, voice = true, narration = true),
    GREETING_MORNING(R.raw.voice_greeting_morning, voice = true, narration = true),
    GREETING_DAY(R.raw.voice_greeting_day, voice = true, narration = true),
    GREETING_EVENING(R.raw.voice_greeting_evening, voice = true, narration = true),
    GREETING_NIGHT(R.raw.voice_greeting_night, voice = true, narration = true),
    CHALLENGE_GO(R.raw.voice_challenge_go, voice = true, narration = true),
    CHALLENGE_OVER(R.raw.voice_challenge_over, voice = true, narration = true),
}

/** The voice of [kind], or null without a companion. */
fun companionVoice(kind: CompanionKind): Sound? =
    when (kind) {
        CompanionKind.HUMAN -> Sound.HUMAN
        CompanionKind.CAT -> Sound.CAT
        CompanionKind.DOG -> Sound.DOG
        CompanionKind.ROBOT -> Sound.ROBOT
        CompanionKind.DRAGON -> Sound.DRAGON
        CompanionKind.DINOSAUR -> Sound.DINOSAUR
        CompanionKind.BUNNY -> Sound.BUNNY
        CompanionKind.UNICORN -> Sound.UNICORN
        CompanionKind.CAR -> Sound.CAR
        CompanionKind.NONE -> null
    }

/** The narrator's line for the intro greeting. */
fun greetingVoice(greeting: Greeting): Sound =
    when (greeting) {
        Greeting.MORNING -> Sound.GREETING_MORNING
        Greeting.DAY -> Sound.GREETING_DAY
        Greeting.EVENING -> Sound.GREETING_EVENING
        Greeting.NIGHT -> Sound.GREETING_NIGHT
    }

/** The narrator's line for the result headline of [stars] stars (the same thresholds as the headline). */
fun resultVoice(stars: Int): Sound =
    when {
        stars >= MAX_STARS -> Sound.RESULT_3
        stars == 2 -> Sound.RESULT_2
        else -> Sound.RESULT_1
    }

/** Which hand a game hint is about, or null for a hint about neither hand alone. */
fun hintVoice(hint: Hint): Sound? =
    when (hint) {
        is Hint.ReadHour, is Hint.SetHour -> Sound.HINT_HOUR
        is Hint.ReadMinute, is Hint.SetMinute -> Sound.HINT_MINUTE
        is Hint.HandsRight -> null
    }

private const val MAX_STARS = 3
