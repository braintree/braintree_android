package com.braintreepayments.api.uicomponents.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataStoreMock : DataStore<Preferences> {

    private val preferencesFlow = MutableStateFlow<Preferences>(emptyPreferences())
    private val lock = Mutex()

    override val data: Flow<Preferences>
        get() = preferencesFlow

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return lock.withLock {
            val currentPreferences = preferencesFlow.value.toMutablePreferences()
            val updatedPreferences = transform(currentPreferences)
            preferencesFlow.value = updatedPreferences
            updatedPreferences
        }
    }
}
