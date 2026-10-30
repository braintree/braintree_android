package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.pendingRequestDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "paypal_saved_payment_method"
)

/**
 * Persists the pending PayPal request across process death, so the return can still be completed. Kept under [key],
 * one per component instance and authorization.
 */
internal class PendingRequestRepository(
    context: Context,
    key: String,
    private val dataStore: DataStore<Preferences> = context.applicationContext.pendingRequestDataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val pendingRequestKey = stringPreferencesKey("pending_request_$key")

    /** Saves the pending request. Returns `false` if the write fails. */
    suspend fun store(pendingRequest: String): Boolean = withContext(dispatcher) {
        try {
            dataStore.edit { it[pendingRequestKey] = pendingRequest }
            true
        } catch (ignored: IOException) {
            false
        }
    }

    /** Returns the stored pending request, or `null` if none is stored or it can't be read. */
    suspend fun get(): String? = withContext(dispatcher) {
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()[pendingRequestKey]
    }

    /** Removes the pending request. A failed write is ignored. */
    suspend fun clear() {
        withContext(dispatcher) {
            try {
                dataStore.edit { it.remove(pendingRequestKey) }
            } catch (ignored: IOException) {
            }
        }
    }
}
