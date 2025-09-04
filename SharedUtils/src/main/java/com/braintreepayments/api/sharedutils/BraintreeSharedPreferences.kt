package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BraintreeSharedPreferences(
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val PREFERENCES_FILE_KEY = "com.braintreepayments.api.SHARED_PREFERENCES"

        @Volatile
        private var INSTANCE: BraintreeSharedPreferences? = null

        @JvmStatic
        fun getInstance(context: Context): BraintreeSharedPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BraintreeSharedPreferences(createSharedPreferencesInstance(context))
                    .also { INSTANCE = it }
            }
        }

        private fun createSharedPreferencesInstance(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
        }
    }

    fun getString(key: String, fallback: String?): String? = sharedPreferences.getString(key, fallback)

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String): Boolean = sharedPreferences.getBoolean(key, false)

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun containsKey(key: String): Boolean = sharedPreferences.contains(key)

    fun getLong(key: String): Long = sharedPreferences.getLong(key, 0)

    fun putStringAndLong(
        stringKey: String,
        stringValue: String,
        longKey: String,
        longValue: Long
    ) {
        sharedPreferences
            .edit()
            .putString(stringKey, stringValue)
            .putLong(longKey, longValue)
            .apply()
    }

    fun clearSharedPreferences() {
        sharedPreferences.edit().clear().apply()
    }
}
