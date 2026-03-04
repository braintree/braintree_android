package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_components")

/**
 * Repository responsible for storing and retrieving the pending request using [DataStore].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayPalPendingRequestRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val pendingRequestKey = stringPreferencesKey("pending_request_key")

    suspend fun storePendingRequest(pendingRequest: String) {
        withContext(dispatcher) {
            dataStore.edit { preferences: MutablePreferences ->
                preferences[pendingRequestKey] = pendingRequest
            }
        }
    }

    suspend fun getPendingRequest(): String? {
        return withContext(dispatcher) {
            dataStore.data.map { preferences: Preferences ->
                preferences[pendingRequestKey]
            }.firstOrNull()
        }
    }

    suspend fun clearPendingRequest() {
        withContext(dispatcher) {
            dataStore.edit { preferences: MutablePreferences ->
                preferences.remove(pendingRequestKey)
            }
        }
    }
}
