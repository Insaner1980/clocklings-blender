// Resource adapters stay together at the domain/UI boundary.
@file:Suppress("TooManyFunctions")

package fi.clocklings.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.BrazilianPortugueseTime
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Greeting
import fi.clocklings.domain.Hint
import fi.clocklings.domain.HourPosition
import fi.clocklings.domain.MinutePosition
import fi.clocklings.domain.Slot
import fi.clocklings.domain.SpanishTime
import fi.clocklings.domain.Text
import fi.clocklings.domain.capitalizeFirst
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.locale.resourceLanguage

/**
 * Resolves domain text references using strings.xml.
 *
 * Android's formatting API requires varargs; Text.Res stores the arguments as a list.
 */
@Suppress("SpreadOperator")
fun Text.resolve(res: Resources): String =
    when (this) {
        is Text.Res -> if (args.isEmpty()) res.getString(id) else res.getString(id, *args.toTypedArray())
        is Text.Plural -> res.getQuantityString(id, count, count)
        is Text.Time -> timeLanguage(resourceLanguage(res)).timeWords(minutes).capitalizeFirst()
        is Text.Plain -> value
    }

@Composable
@ReadOnlyComposable
fun resources(): Resources = LocalResources.current

@Composable
@ReadOnlyComposable
fun Text.resolve(): String = resolve(resources())

/** Capitalized clock phrase, with explicit day context in 24-hour mode. */
fun timeLabel(
    language: AppLanguage,
    t: Int,
    h24: Boolean,
): String = timeLanguage(language).fullWords(t, h24).capitalizeFirst()

/** Numeric time, such as 3:30 or 15:30. */
fun digitalLabel(
    res: Resources,
    t: Int,
    h24: Boolean,
): String {
    val wording = timeLanguage(resourceLanguage(res))
    return if (h24) res.getString(R.string.digital_24, wording.digital(t, true)) else wording.digital(t)
}

/** On-demand narration uses the same time and context as the visible answer. */
fun spokenTime(
    res: Resources,
    t: Int,
    h24: Boolean,
): String {
    val wording = timeLanguage(resourceLanguage(res))
    val phrase =
        when (wording) {
            SpanishTime -> SpanishTime.timeSentence(t, h24)
            BrazilianPortugueseTime -> BrazilianPortugueseTime.timeSentence(t, h24)
            else -> wording.fullWords(t, h24)
        }
    return when {
        h24 && ClockTime.mod(t, ClockTime.DAY) == 0 -> {
            res.getString(R.string.speak_midnight_24, wording.numericWords(t, true))
        }

        h24 && ClockTime.mod(t, ClockTime.DAY) == ClockTime.HALF_DAY -> {
            res.getString(R.string.speak_noon_24, wording.numericWords(t, true))
        }

        h24 -> {
            res.getString(R.string.speak_time_24, phrase, wording.numericWords(t, true))
        }

        else -> {
            res.getString(R.string.speak_time, phrase)
        }
    }
}

fun praise(
    res: Resources,
    index: Int,
): String {
    val all = res.getStringArray(R.array.praise)
    return all[Math.floorMod(index, all.size)]
}

private fun hourText(
    res: Resources,
    p: HourPosition,
): String =
    when (p) {
        is HourPosition.Exact -> res.getString(R.string.hour_pos_exact, p.number)
        is HourPosition.JustPassed -> res.getString(R.string.hour_pos_passed, p.number)
        is HourPosition.Between -> res.getString(R.string.hour_pos_between, p.number, p.next)
        is HourPosition.Almost -> res.getString(R.string.hour_pos_almost, p.next)
    }

private fun minuteText(
    res: Resources,
    p: MinutePosition,
): String =
    when (p) {
        MinutePosition.Top -> res.getString(R.string.minute_pos_top)
        MinutePosition.Bottom -> res.getString(R.string.minute_pos_bottom)
        MinutePosition.QuarterPast -> res.getString(R.string.minute_pos_quarter_past)
        MinutePosition.QuarterTo -> res.getString(R.string.minute_pos_quarter_to)
        is MinutePosition.Number -> res.getString(R.string.minute_pos_number, p.number, p.minutes)
        is MinutePosition.Ticks -> res.getString(R.string.minute_pos_ticks, p.minutes)
    }

private fun withTip(
    main: String,
    tip: String?,
): String = if (tip == null) main else "$main $tip"

/** Vinkin teksti täsmälleen kuten webin hintRead ja hintSet. */
fun Hint.text(res: Resources): String =
    when (this) {
        is Hint.HandsRight -> {
            val dayPart = timeLanguage(resourceLanguage(res)).dayPart(time)
            res.getString(
                R.string.hint_hands_right,
                res.getString(if (dayPart.isSun) R.string.hint_sun else R.string.hint_moon),
                dayPart.name,
            )
        }

        is Hint.ReadHour -> {
            withTip(
                res.getString(R.string.hint_read_hour, hourText(res, position)),
                if (nearHalf) res.getString(R.string.hint_read_hour_half) else null,
            )
        }

        is Hint.ReadMinute -> {
            withTip(
                res.getString(R.string.hint_read_minute, minuteText(res, position)),
                if (alsoHour) res.getString(R.string.hint_read_minute_also_hour) else null,
            )
        }

        is Hint.SetMinute -> {
            res.getString(R.string.hint_set_minute, minuteText(res, position))
        }

        is Hint.SetHour -> {
            withTip(
                res.getString(R.string.hint_set_hour, hourText(res, position)),
                if (turnTip) res.getString(R.string.hint_set_hour_turn) else null,
            )
        }
    }

@StringRes
fun levelName(index: Int): Int =
    when (index) {
        0 -> R.string.level_tasatunnit
        1 -> R.string.level_puolet
        2 -> R.string.level_vartit
        FIVE_MINUTE_LEVEL -> R.string.level_viisi
        else -> R.string.level_minuutit
    }

/** Aloitusnäkymän tervehdyksen teksti. */
@StringRes
fun greetingText(greeting: Greeting): Int =
    when (greeting) {
        Greeting.MORNING -> R.string.intro_greeting_morning
        Greeting.DAY -> R.string.intro_greeting_day
        Greeting.EVENING -> R.string.intro_greeting_evening
        Greeting.NIGHT -> R.string.intro_greeting_night
    }

@StringRes
fun slotName(slot: Slot): Int =
    when (slot) {
        Slot.HAIR -> R.string.slot_hair
        Slot.DRESS -> R.string.slot_dress
        Slot.SHOES -> R.string.slot_shoes
        Slot.HEAD -> R.string.slot_head
        Slot.NECK -> R.string.slot_neck
        Slot.EXTRA -> R.string.slot_extra
        Slot.ROOF -> R.string.slot_roof
        Slot.FRONT -> R.string.slot_front
        Slot.WHEELS -> R.string.slot_wheels
        Slot.MODEL -> R.string.slot_model
    }

private val ItemNames =
    mapOf(
        "hair-letit" to R.string.item_hair_letit,
        "dress-perus" to R.string.item_dress_perus,
        "shoes-tossut" to R.string.item_shoes_tossut,
        "dress-pilkku" to R.string.item_dress_pilkku,
        "head-rusetti" to R.string.item_head_rusetti,
        "shoes-punaiset" to R.string.item_shoes_punaiset,
        "hair-pitkat" to R.string.item_hair_pitkat,
        "dress-tahti" to R.string.item_dress_tahti,
        "head-kukat" to R.string.item_head_kukat,
        "extra-strawberry-bag" to R.string.item_extra_strawberry_bag,
        "shoes-saappaat" to R.string.item_shoes_saappaat,
        "hair-kiharat" to R.string.item_hair_kiharat,
        "dress-overalls" to R.string.item_dress_overalls,
        "head-kruunu" to R.string.item_head_kruunu,
        "extra-siivet" to R.string.item_extra_siivet,
        "shoes-roller-skates" to R.string.item_shoes_roller_skates,
        "hair-sateenkaari" to R.string.item_hair_sateenkaari,
        "dress-sateenkaari" to R.string.item_dress_sateenkaari,
        // Cat
        "cat-neck-scarf" to R.string.item_cat_neck_scarf,
        "cat-head-bow" to R.string.item_cat_head_bow,
        "cat-extra-yarn" to R.string.item_cat_extra_yarn,
        "cat-neck-bell-collar" to R.string.item_cat_neck_bell_collar,
        "cat-head-bobble-hat" to R.string.item_cat_head_bobble_hat,
        "cat-extra-glasses" to R.string.item_cat_extra_glasses,
        "cat-neck-bow-tie" to R.string.item_cat_neck_bow_tie,
        "cat-head-sailor-hat" to R.string.item_cat_head_sailor_hat,
        "cat-extra-fish-backpack" to R.string.item_cat_extra_fish_backpack,
        "cat-neck-ruffle" to R.string.item_cat_neck_ruffle,
        "cat-head-flower-crown" to R.string.item_cat_head_flower_crown,
        "cat-neck-pearls" to R.string.item_cat_neck_pearls,
        "cat-extra-wand" to R.string.item_cat_extra_wand,
        "cat-neck-cape" to R.string.item_cat_neck_cape,
        "cat-head-crown" to R.string.item_cat_head_crown,
        "cat-extra-wings" to R.string.item_cat_extra_wings,
        // Dog
        "dog-neck-bandana" to R.string.item_dog_neck_bandana,
        "dog-head-cap" to R.string.item_dog_head_cap,
        "dog-extra-ball" to R.string.item_dog_extra_ball,
        "dog-neck-bone-collar" to R.string.item_dog_neck_bone_collar,
        "dog-head-sun-hat" to R.string.item_dog_head_sun_hat,
        "dog-extra-frisbee" to R.string.item_dog_extra_frisbee,
        "dog-neck-bow-tie" to R.string.item_dog_neck_bow_tie,
        "dog-head-party-hat" to R.string.item_dog_head_party_hat,
        "dog-extra-boots" to R.string.item_dog_extra_boots,
        "dog-neck-lei" to R.string.item_dog_neck_lei,
        "dog-head-aviator" to R.string.item_dog_head_aviator,
        "dog-extra-backpack" to R.string.item_dog_extra_backpack,
        "dog-neck-medal" to R.string.item_dog_neck_medal,
        "dog-head-helmet" to R.string.item_dog_head_helmet,
        "dog-extra-raincoat" to R.string.item_dog_extra_raincoat,
        "dog-neck-cape" to R.string.item_dog_neck_cape,
        // Robot
        "robot-head-antenna" to R.string.item_robot_head_antenna,
        "robot-neck-bow-tie" to R.string.item_robot_neck_bow_tie,
        "robot-extra-wrench" to R.string.item_robot_extra_wrench,
        "robot-head-propeller" to R.string.item_robot_head_propeller,
        "robot-neck-cape" to R.string.item_robot_neck_cape,
        "robot-extra-heart-eyes" to R.string.item_robot_extra_heart_eyes,
        "robot-head-headphones" to R.string.item_robot_head_headphones,
        "robot-extra-bubble-pack" to R.string.item_robot_extra_bubble_pack,
        "robot-extra-star-eyes" to R.string.item_robot_extra_star_eyes,
        "robot-head-chef-hat" to R.string.item_robot_head_chef_hat,
        "robot-extra-wind-up-key" to R.string.item_robot_extra_wind_up_key,
        "robot-extra-pet" to R.string.item_robot_extra_pet,
        "robot-head-star-antennas" to R.string.item_robot_head_star_antennas,
        "robot-neck-medal" to R.string.item_robot_neck_medal,
        "robot-head-astronaut" to R.string.item_robot_head_astronaut,
        "robot-extra-jetpack" to R.string.item_robot_extra_jetpack,
        // Dragon
        "dragon-neck-pendant" to R.string.item_dragon_neck_pendant,
        "dragon-head-flower-crown" to R.string.item_dragon_head_flower_crown,
        "dragon-extra-coins" to R.string.item_dragon_extra_coins,
        "dragon-neck-bow" to R.string.item_dragon_neck_bow,
        "dragon-head-wizard-hat" to R.string.item_dragon_head_wizard_hat,
        "dragon-extra-book" to R.string.item_dragon_extra_book,
        "dragon-neck-scarf" to R.string.item_dragon_neck_scarf,
        "dragon-head-tiara" to R.string.item_dragon_head_tiara,
        "dragon-extra-shield" to R.string.item_dragon_extra_shield,
        "dragon-neck-chain" to R.string.item_dragon_neck_chain,
        "dragon-head-knight-helmet" to R.string.item_dragon_head_knight_helmet,
        "dragon-extra-star-cloud" to R.string.item_dragon_extra_star_cloud,
        "dragon-neck-sparkles" to R.string.item_dragon_neck_sparkles,
        "dragon-neck-cape" to R.string.item_dragon_neck_cape,
        "dragon-head-crown" to R.string.item_dragon_head_crown,
        "dragon-extra-wings" to R.string.item_dragon_extra_wings,
        // Dinosaur
        "dinosaur-extra-backpack" to R.string.item_dinosaur_extra_backpack,
        "dinosaur-head-flower" to R.string.item_dinosaur_head_flower,
        "dinosaur-extra-bucket" to R.string.item_dinosaur_extra_bucket,
        "dinosaur-neck-scarf" to R.string.item_dinosaur_neck_scarf,
        "dinosaur-head-cap" to R.string.item_dinosaur_head_cap,
        "dinosaur-extra-balloon" to R.string.item_dinosaur_extra_balloon,
        "dinosaur-neck-bow-tie" to R.string.item_dinosaur_neck_bow_tie,
        "dinosaur-head-party-hat" to R.string.item_dinosaur_head_party_hat,
        "dinosaur-extra-umbrella" to R.string.item_dinosaur_extra_umbrella,
        "dinosaur-extra-swim-ring" to R.string.item_dinosaur_extra_swim_ring,
        "dinosaur-head-hard-hat" to R.string.item_dinosaur_head_hard_hat,
        "dinosaur-extra-kite" to R.string.item_dinosaur_extra_kite,
        "dinosaur-neck-binoculars" to R.string.item_dinosaur_neck_binoculars,
        "dinosaur-neck-medal" to R.string.item_dinosaur_neck_medal,
        "dinosaur-extra-ice-cream" to R.string.item_dinosaur_extra_ice_cream,
        "dinosaur-extra-skateboard" to R.string.item_dinosaur_extra_skateboard,
        // Bunny
        "bunny-neck-ribbon" to R.string.item_bunny_neck_ribbon,
        "bunny-extra-carrot" to R.string.item_bunny_extra_carrot,
        "bunny-head-ear-bow" to R.string.item_bunny_head_ear_bow,
        "bunny-neck-daisies" to R.string.item_bunny_neck_daisies,
        "bunny-extra-basket" to R.string.item_bunny_extra_basket,
        "bunny-head-sun-hat" to R.string.item_bunny_head_sun_hat,
        "bunny-neck-locket" to R.string.item_bunny_neck_locket,
        "bunny-extra-watering-can" to R.string.item_bunny_extra_watering_can,
        "bunny-head-top-hat" to R.string.item_bunny_head_top_hat,
        "bunny-neck-bow-tie" to R.string.item_bunny_neck_bow_tie,
        "bunny-extra-bubbles" to R.string.item_bunny_extra_bubbles,
        "bunny-head-flower-crown" to R.string.item_bunny_head_flower_crown,
        "bunny-extra-bouquet" to R.string.item_bunny_extra_bouquet,
        "bunny-neck-cape" to R.string.item_bunny_neck_cape,
        "bunny-head-tiara" to R.string.item_bunny_head_tiara,
        "bunny-extra-wings" to R.string.item_bunny_extra_wings,
        // Unicorn
        "unicorn-neck-lei" to R.string.item_unicorn_neck_lei,
        "unicorn-head-horn-rings" to R.string.item_unicorn_head_horn_rings,
        "unicorn-extra-cloud" to R.string.item_unicorn_extra_cloud,
        "unicorn-neck-stars" to R.string.item_unicorn_neck_stars,
        "unicorn-head-flower-crown" to R.string.item_unicorn_head_flower_crown,
        "unicorn-extra-balloon" to R.string.item_unicorn_extra_balloon,
        "unicorn-neck-scarf" to R.string.item_unicorn_neck_scarf,
        "unicorn-head-ribbons" to R.string.item_unicorn_head_ribbons,
        "unicorn-extra-boots" to R.string.item_unicorn_extra_boots,
        "unicorn-neck-rainbow-scarf" to R.string.item_unicorn_neck_rainbow_scarf,
        "unicorn-head-butterfly" to R.string.item_unicorn_head_butterfly,
        "unicorn-extra-rainbow" to R.string.item_unicorn_extra_rainbow,
        "unicorn-neck-moon" to R.string.item_unicorn_neck_moon,
        "unicorn-head-crown" to R.string.item_unicorn_head_crown,
        "unicorn-neck-cape" to R.string.item_unicorn_neck_cape,
        "unicorn-extra-wings" to R.string.item_unicorn_extra_wings,
        // Car
        "car-front-plate" to R.string.item_car_front_plate,
        "car-wheels-sport" to R.string.item_car_wheels_sport,
        "car-roof-suitcase" to R.string.item_car_roof_suitcase,
        "car-front-sunglasses" to R.string.item_car_front_sunglasses,
        "car-wheels-offroad" to R.string.item_car_wheels_offroad,
        "car-model-race" to R.string.item_car_model_race,
        "car-roof-surfboard" to R.string.item_car_roof_surfboard,
        "car-front-bull-bar" to R.string.item_car_front_bull_bar,
        "car-wheels-flames" to R.string.item_car_wheels_flames,
        "car-roof-flag" to R.string.item_car_roof_flag,
        "car-model-fire-engine" to R.string.item_car_model_fire_engine,
        "car-front-plow" to R.string.item_car_front_plow,
        "car-roof-taxi" to R.string.item_car_roof_taxi,
        "car-wheels-gold" to R.string.item_car_wheels_gold,
        "car-roof-rally-lights" to R.string.item_car_roof_rally_lights,
        "car-model-rocket" to R.string.item_car_model_rocket,
    )

@StringRes
fun itemName(id: String): Int = ItemNames.getValue(id)

private const val FIVE_MINUTE_LEVEL = 3
