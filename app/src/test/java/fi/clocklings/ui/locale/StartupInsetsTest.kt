package fi.clocklings.ui.locale

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import fi.clocklings.R
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.localizedContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StartupInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val repository =
        ProgressRepository(
            object : DataStore<Preferences> {
                override val data = flow<Preferences> { throw IOException("Synthetic startup read failure") }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    error("Startup failure must not write preferences")
            },
        )

    private val platform =
        object : AppLocalePlatform {
            override val supported = true

            override fun read(): AppLanguage? = null

            override fun write(language: AppLanguage?) = error("A failed migration must not change the platform")
        }

    @Test
    fun localeReadErrorAndRetryRespectSystemBarsInEveryLanguageAndDisplaySize() {
        lateinit var controller: AppLocaleController
        compose.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false)
            controller = AppLocaleController(repository, platform)
        }
        val viewport = mutableStateOf(411 to 1f)
        val language = mutableStateOf(AppLanguage.ENGLISH)
        compose.setContent {
            val configuration =
                Configuration(LocalConfiguration.current).apply {
                    setLocale(Locale.forLanguageTag(language.value.tag))
                }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(2f, viewport.value.second),
            ) {
                Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                    AppLocaleProvider(repository, controller = controller) { Text("Ready") }
                }
            }
        }
        try {
            compose.runOnIdle {
                ViewCompat.dispatchApplyWindowInsets(
                    compose.activity.window.decorView,
                    WindowInsetsCompat
                        .Builder()
                        .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 48, 0, 64))
                        .build(),
                )
            }
            for (entry in AppLanguage.entries) {
                for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                    compose.runOnIdle {
                        language.value = entry
                        viewport.value = cell
                    }
                    val context = localizedContext(entry)
                    val error = compose.onNodeWithText(context.getString(R.string.language_save_error))
                    val retry = compose.onNodeWithText(context.getString(R.string.settings_retry))
                    error.assertIsDisplayed()
                    retry.assertIsDisplayed()
                    assertTrue(
                        "$entry / $cell: error intersects status bar",
                        error.fetchSemanticsNode().boundsInRoot.top >= 48f,
                    )
                    assertTrue(
                        "$entry / $cell: Retry intersects navigation bar",
                        retry.fetchSemanticsNode().boundsInRoot.bottom <= 1_216f,
                    )
                }
            }
        } finally {
            controller.viewModelScope.cancel()
        }
    }
}
