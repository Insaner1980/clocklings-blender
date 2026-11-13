package fi.clocklings.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Levels
import java.util.Locale

/** The application context with its resources in [language], for screens composed in that language. */
internal fun localizedContext(language: AppLanguage): Context =
    ApplicationProvider.getApplicationContext<Application>().let { app ->
        app.createConfigurationContext(
            Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) },
        )
    }

/** The [count] challenge answers with the longest wording in [language]: long, but not always the tallest. */
internal fun longAnswers(
    language: AppLanguage,
    h24: Boolean,
    count: Int,
): List<Int> = Levels.challengeTimes(h24).sortedByDescending { timeLabel(language, it, h24).length }.take(count)
