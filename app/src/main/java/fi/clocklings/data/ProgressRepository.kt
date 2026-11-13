package fi.clocklings.data

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException

/**
 * Edistyminen ja asetukset Preferences DataStoressa. Viallinen tai tuntematon arvo
 * korvataan oletuksella, jotta sovellus ei koskaan kaadu tallennukseen.
 *
 * Public Flow references never change; stored values are observed by collecting them.
 */
@Stable
class ProgressRepository(
    private val store: DataStore<Preferences>,
) {
    private val data: Flow<Preferences> =
        flow {
            var hasRead = false
            var retryDelay = READ_RETRY_MS
            emitAll(
                store.data
                    .onEach {
                        hasRead = true
                        retryDelay = READ_RETRY_MS
                    }.retryWhen { error, _ ->
                        if (error !is IOException) return@retryWhen false
                        Log.w(TAG, "Tallennusta ei voitu lukea", error)
                        // Keep startup usable and retain the last known values after a later read failure.
                        if (!hasRead) emit(emptyPreferences())
                        delay(retryDelay)
                        retryDelay = (retryDelay * 2).coerceAtMost(MAX_READ_RETRY_MS)
                        true
                    },
            )
        }

    val progress: Flow<Progress> = data.map { it.toProgress() }.distinctUntilChanged()
    val settings: Flow<Settings> = data.map { it.toSettings() }.distinctUntilChanged()

    // Only API 26–32 persists a language here. Newer Android owns its app locale.
    val legacyLanguage: Flow<String?> = data.map { it[APP_LANGUAGE] }.distinctUntilChanged()

    // Migration must distinguish an unreadable store from an absent saved language.
    internal val legacyLanguageForMigration: Flow<String?> = store.data.map { it[APP_LANGUAGE] }

    suspend fun setLegacyLanguage(tag: String?) {
        store.edit { prefs ->
            if (tag == null) prefs.remove(APP_LANGUAGE) else prefs[APP_LANGUAGE] = tag
        }
    }

    suspend fun updateProgress(transform: (Progress) -> Progress) {
        store.edit { prefs -> prefs.write(transform(prefs.toProgress())) }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[SOUND] = s.sound
            prefs[HELP] = s.help
            prefs[H24] = s.h24
            prefs.writePersonalization(s.personalization)
        }
    }

    /** "Aloita alusta": edistyminen nollataan, asetukset säilyvät. */
    suspend fun resetProgress() {
        store.edit { it.write(Progress()) }
    }

    private fun Preferences.toSettings() =
        Settings(
            sound = this[SOUND] ?: true,
            help = this[HELP] ?: true,
            h24 = this[H24] ?: false,
            personalization = readPersonalization(),
        )

    private fun Preferences.toProgress(): Progress {
        val base =
            Progress(
                stars = STARS.map { (this[it] ?: 0).coerceIn(0, MAX_LEVEL_STARS) },
                challengeBest = (this[BEST] ?: 0).coerceAtLeast(0),
                lessonsDone = this[DONE] ?: emptySet(),
                introShown = this[INTRO] ?: emptySet(),
            )
        return DRESSABLE.fold(base) { progress, kind ->
            val outfit = if (kind == CompanionKind.HUMAN) readHumanOutfit() else readAnimalOutfit(kind)
            progress
                .withOutfit(kind, outfit)
                .withName(kind, this[nameKey(kind)].orEmpty())
        }
    }

    private fun Preferences.readHumanOutfit(): Outfit {
        var outfit = Outfit.Default
        for (slot in CompanionKind.HUMAN.slots) {
            val id = this[outfitKey(CompanionKind.HUMAN, slot)] ?: continue
            val item = ownItem(CompanionKind.HUMAN, slot, id)
            outfit =
                when {
                    item != null -> outfit.with(slot, item.id)
                    slot.optional -> outfit.with(slot, null)
                    else -> outfit
                }
        }
        return outfit
    }

    /**
     * Animals start bare. Before the first save, the single accessory flag of the earlier version
     * (read only) puts on the companion's free item. Any saved outfit key overrides that flag.
     */
    private fun Preferences.readAnimalOutfit(kind: CompanionKind): Outfit {
        val saved = kind.slots.any { outfitKey(kind, it) in this }
        if (!saved) {
            return if (this[legacyAccessoryKey(kind)] == true) {
                Wardrobe.freeItems(kind).fold(Outfit()) { outfit, item -> outfit.with(item.slot, item.id) }
            } else {
                Outfit()
            }
        }
        val current =
            kind.slots.fold(Outfit()) { outfit, slot ->
                val item = this[outfitKey(kind, slot)]?.let { ownItem(kind, slot, it) }
                outfit.with(slot, item?.takeIf { it.slot == slot }?.id)
            }
        // Keep a valid item already in the destination slot. Competing legacy items follow slot order.
        return kind.slots.fold(current) { outfit, slot ->
            val replacement = this[outfitKey(kind, slot)]?.let { ownItem(kind, slot, it) }
            if (replacement != null && outfit[replacement.slot] == null) {
                outfit.with(replacement.slot, replacement.id)
            } else {
                outfit
            }
        }
    }

    private fun ownItem(
        kind: CompanionKind,
        slot: Slot,
        id: String,
    ): Item? {
        val replacement = REPLACED_ITEMS[id]?.takeIf { (sourceSlot, _) -> sourceSlot == slot }
        return Wardrobe
            .itemOrNull(replacement?.second ?: id)
            ?.takeIf { it.companion == kind && (replacement != null || it.slot == slot) }
    }

    private fun MutablePreferences.write(p: Progress) {
        STARS.forEachIndexed { i, key -> this[key] = p.stars[i] }
        this[BEST] = p.challengeBest
        for (kind in DRESSABLE) {
            val outfit = p.outfitFor(kind)
            for (slot in kind.slots) this[outfitKey(kind, slot)] = outfit[slot].orEmpty()
            this[nameKey(kind)] = p.nameFor(kind)
        }
        this[DONE] = p.lessonsDone
        this[INTRO] = p.introShown
    }

    private companion object {
        const val TAG = "ProgressRepository"
        const val READ_RETRY_MS = 1_000L
        const val MAX_READ_RETRY_MS = 30_000L
        val STARS = List(Progress.LEVEL_COUNT) { intPreferencesKey("stars_$it") }
        val BEST = intPreferencesKey("challenge_best")
        val DONE = stringSetPreferencesKey("lessons_done")
        val INTRO = stringSetPreferencesKey("intro_shown")
        val SOUND = booleanPreferencesKey("sound")
        val HELP = booleanPreferencesKey("help")
        val H24 = booleanPreferencesKey("h24")
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        val REPLACED_ITEMS =
            mapOf(
                "extra-laukku" to (Slot.EXTRA to "extra-strawberry-bag"),
                "dress-prinsessa" to (Slot.DRESS to "dress-overalls"),
                "shoes-kimalle" to (Slot.SHOES to "shoes-roller-skates"),
                "robot-neck-tie" to (Slot.NECK to "robot-neck-cape"),
                "robot-neck-spring" to (Slot.NECK to "robot-extra-bubble-pack"),
                "robot-neck-lights" to (Slot.NECK to "robot-extra-wind-up-key"),
                "dinosaur-neck-lei" to (Slot.NECK to "dinosaur-extra-swim-ring"),
                "dinosaur-head-explorer-hat" to (Slot.HEAD to "dinosaur-extra-ice-cream"),
            )

        /** Companions with an outfit and a name: the doll and every animal. */
        val DRESSABLE = CompanionKind.entries.filter { it.slots.isNotEmpty() }

        // The doll keeps the keys of the earlier version; animals are namespaced by kind.
        fun outfitKey(
            kind: CompanionKind,
            slot: Slot,
        ) = stringPreferencesKey(
            if (kind == CompanionKind.HUMAN) {
                "outfit_${slot.name.lowercase()}"
            } else {
                "outfit_${kind.name.lowercase()}_${slot.name.lowercase()}"
            },
        )

        fun nameKey(kind: CompanionKind) =
            stringPreferencesKey(
                if (kind == CompanionKind.HUMAN) "doll_name" else "companion_name_${kind.name.lowercase()}",
            )

        fun legacyAccessoryKey(kind: CompanionKind) = booleanPreferencesKey("companion_${kind.name}_accessory")
    }
}

private const val MAX_LEVEL_STARS = 3
