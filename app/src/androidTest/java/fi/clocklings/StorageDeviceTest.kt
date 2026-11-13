package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class StorageDeviceTest {
    protected val context = InstrumentationRegistry.getInstrumentation().targetContext
    protected val compose = createAndroidComposeRule<ComponentActivity>()
    protected val files = TemporaryFolder(context.cacheDir)

    @get:Rule val rules: RuleChain = RuleChain.outerRule(EnglishLocaleRule()).around(files).around(compose)

    protected fun servicesFor(store: DataStore<Preferences>): AppServices =
        object : AppServices by (context.applicationContext as ClocklingsApplication).container {
            override val repository = ProgressRepository(store)
        }
}

internal fun writeGatedPreferences(
    file: File,
    job: Job,
    beforeWrite: suspend () -> Unit,
): DataStore<Preferences> {
    val delegate = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
    return object : DataStore<Preferences> {
        override val data = delegate.data

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            beforeWrite()
            return delegate.updateData(transform)
        }
    }
}

internal fun ComposeTestRule.clickStorageAction(text: String) {
    val matcher = (hasText(text) or hasContentDescription(text, substring = true)) and hasClickAction()
    waitUntil(10_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(matcher)[0].scrollToIfScrollable().performClick()
}
