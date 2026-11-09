package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
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
 * one per component instance and authorization. A request is removed once it's older than [STALE_AFTER_MILLIS], so
 * one left behind by an instance that never came back (the app was swiped away during the edit) doesn't stay on the
 * device.
 */
internal class PendingRequestRepository(
    context: Context,
    key: String,
    private val dataStore: DataStore<Preferences> = context.applicationContext.pendingRequestDataStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    private val pendingRequestKey = stringPreferencesKey("$REQUEST_PREFIX$key")
    private val storedAtKey = longPreferencesKey("$STORED_AT_PREFIX$key")

    /** Saves the pending request. Returns `false` if the write fails. */
    suspend fun store(pendingRequest: String): Boolean = withContext(dispatcher) {
        try {
            dataStore.edit {
                it.removeStale(currentTimeMillis())
                it[pendingRequestKey] = pendingRequest
                it[storedAtKey] = currentTimeMillis()
            }
            true
        } catch (ignored: IOException) {
            false
        }
    }

    /** Returns the stored pending request, or `null` if none is stored, it's stale or it can't be read. */
    suspend fun get(): String? = withContext(dispatcher) {
        val preferences = dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        val storedAt = preferences[storedAtKey]
        val isStale = storedAt == null || currentTimeMillis() - storedAt > STALE_AFTER_MILLIS
        if (isStale) null else preferences[pendingRequestKey]
    }

    /** Removes the pending request. A failed write is ignored. */
    suspend fun clear() {
        withContext(dispatcher) {
            try {
                dataStore.edit {
                    it.remove(pendingRequestKey)
                    it.remove(storedAtKey)
                }
            } catch (ignored: IOException) {
            }
        }
    }

    // An entry without a time is treated as stale.
    private fun MutablePreferences.removeStale(now: Long) {
        asMap().keys
            .filter { it.name.startsWith(REQUEST_PREFIX) }
            .forEach { requestKey ->
                val id = requestKey.name.removePrefix(REQUEST_PREFIX)
                val storedAtKey = longPreferencesKey("$STORED_AT_PREFIX$id")
                val storedAt = this[storedAtKey]
                if (storedAt == null || now - storedAt > STALE_AFTER_MILLIS) {
                    remove(requestKey)
                    remove(storedAtKey)
                }
            }
    }

    internal companion object {
        const val STALE_AFTER_MILLIS = 60 * 60 * 1000L
        private const val REQUEST_PREFIX = "pending_request_"
        private const val STORED_AT_PREFIX = "stored_at_pending_request_"
    }
}
