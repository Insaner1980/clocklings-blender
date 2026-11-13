package fi.clocklings.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class CachedOwnership(
    val receipt: PurchaseReceipt,
    val acknowledged: Boolean,
)

interface OwnershipStore {
    suspend fun read(): CachedOwnership?

    suspend fun write(ownership: CachedOwnership?)
}

/** Separate DataStore file; learning-progress reset never writes to this store. */
class PreferencesOwnershipStore(
    private val store: DataStore<Preferences>,
) : OwnershipStore {
    override suspend fun read(): CachedOwnership? {
        val saved = store.data.first()
        val json = saved[JSON] ?: return null
        val signature = saved[SIGNATURE].orEmpty()
        return CachedOwnership(PurchaseReceipt(json, signature), saved[ACKNOWLEDGED] == true)
    }

    override suspend fun write(ownership: CachedOwnership?) {
        store.edit { saved ->
            if (ownership == null) {
                saved.remove(JSON)
                saved.remove(SIGNATURE)
                saved.remove(ACKNOWLEDGED)
            } else {
                saved[JSON] = ownership.receipt.json
                saved[SIGNATURE] = ownership.receipt.signature
                saved[ACKNOWLEDGED] = ownership.acknowledged
            }
        }
    }

    private companion object {
        val JSON = stringPreferencesKey("verified_receipt")
        val SIGNATURE = stringPreferencesKey("verified_signature")
        val ACKNOWLEDGED = booleanPreferencesKey("acknowledged")
    }
}
