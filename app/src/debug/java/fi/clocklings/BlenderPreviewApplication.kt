package fi.clocklings

import android.content.SharedPreferences
import android.util.Log
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Lessons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

/** First-launch demo data belongs only to this separate debug application. */
class BlenderPreviewApplication : ClocklingsApplication() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                initializeBlenderPreview(container.repository, getSharedPreferences("blender_preview", MODE_PRIVATE))
            } catch (error: IOException) {
                Log.w("BlenderPreview", "Preview setup could not be saved; normal app startup remains available", error)
            }
        }
    }
}

/** Preserve subsequent learning progress, outfits and settings instead of resetting them on launch. */
internal suspend fun initializeBlenderPreview(
    repository: ProgressRepository,
    preferences: SharedPreferences,
) {
    if (preferences.getBoolean("initialized", false)) return
    repository.updateProgress { it.copy(stars = List(Lessons.levelLessons.size) { PREVIEW_STARS_PER_LEVEL }) }
    repository.updateSettings { it.copy(personalization = it.personalization.copy(companion = CompanionKind.ROBOT)) }
    preferences.edit().putBoolean("initialized", true).apply()
}

private const val PREVIEW_STARS_PER_LEVEL = 3
