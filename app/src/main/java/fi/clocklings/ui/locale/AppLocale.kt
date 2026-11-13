package fi.clocklings.ui.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.resolveLanguage
import fi.clocklings.ui.components.LoadingScreen
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkType
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalAppLocale = staticCompositionLocalOf<AppLocaleController> { error("Missing app locale") }

fun resourceLanguage(resources: Resources): AppLanguage = resolveLanguage(resources.configuration.locales.tags())

private fun LocaleList.tags(): List<String> = (0 until size()).map { get(it).toLanguageTag() }

private class AndroidAppLocalePlatform(
    private val context: Context,
) : AppLocalePlatform {
    override val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun read(): AppLanguage? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) null else AppLanguage.fromTag(locales[0].toLanguageTag())
        } else {
            null
        }

    override fun write(language: AppLanguage?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language?.tag.orEmpty())
        }
    }
}

/** One resource boundary; lifecycle, saved-state and ViewModel owners remain the activity's. */
@Composable
fun AppLocaleProvider(
    repository: ProgressRepository,
    modifier: Modifier = Modifier,
    controller: AppLocaleController = viewModel(factory = localeViewModelFactory(repository, LocalContext.current)),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val selection by controller.selection.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.refresh() }
    val language =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveLanguage(configuration.locales.tags())
        } else {
            selection.choice ?: resolveLanguage(configuration.locales.tags())
        }
    val localized =
        remember(context, configuration, language) {
            val copy = Configuration(configuration)
            copy.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
            context.createConfigurationContext(copy)
        }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalResources provides localized.resources,
        LocalAppLanguage provides language,
        LocalAppLocale provides controller,
    ) {
        Box(modifier) {
            if (selection.ready) {
                content()
            } else if (selection.failed) {
                ClocklingsTheme {
                    ScreenScaffold(section = Section.HOME) {
                        Text(stringResource(R.string.language_save_error), style = SkType.Body)
                        SkButton(
                            stringResource(R.string.settings_retry),
                            controller::refresh,
                            enabled = !selection.pending,
                        )
                    }
                }
            } else {
                ClocklingsTheme { LoadingScreen() }
            }
        }
    }
}

private fun localeViewModelFactory(
    repository: ProgressRepository,
    context: Context,
) = viewModelFactory {
    initializer { AppLocaleController(repository, AndroidAppLocalePlatform(context.applicationContext)) }
}
