package fi.clocklings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.ui.FakePurchases

/**
 * The application for local Robolectric tests. Progress and settings live in memory: on the Windows
 * JVM the preferences file's atomic replace intermittently fails with AccessDeniedException, which
 * lost writes and made app flow tests time out. Each test gets a fresh application and a fresh store.
 */
class TestClocklingsApplication : ClocklingsApplication() {
    override fun purchases() = FakePurchases(fullAccess = true)

    override fun preferencesStore(): DataStore<Preferences> = InMemoryPreferencesStore()
}
