package com.braintreepayments.api.uicomponents.compose

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * In-memory [DataStore] so each test can construct a [PendingRequestRepository] without shared device storage.
 */
internal class InMemoryDataStore : DataStore<Preferences> {

    private val preferencesFlow = MutableStateFlow<Preferences>(emptyPreferences())
    private val lock = Mutex()

    override val data: Flow<Preferences>
        get() = preferencesFlow

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return lock.withLock {
            val updated = transform(preferencesFlow.value)
            preferencesFlow.value = updated
            updated
        }
    }
}

/**
 * Builds a client token whose configuration URL and fingerprint are unique per merchant, so the
 * configuration cache can hold a different configuration per authorization. Encoded without
 * line wrapping so it matches the client token matcher in
 * [com.braintreepayments.api.core.Authorization].
 */
internal fun clientToken(configUrl: String, authorizationFingerprint: String): String {
    val json = JSONObject()
        .put("configUrl", configUrl)
        .put("authorizationFingerprint", authorizationFingerprint)
    return Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
}
