package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.util.concurrent.TimeUnit

internal class ConfigurationCache @VisibleForTesting constructor(
        private val sharedPreferences: BraintreeSharedPreferences
    ) {

    fun getConfiguration(cacheKey: String): String? {
        return getConfiguration(cacheKey, System.currentTimeMillis())
    }

    fun getConfiguration(cacheKey: String, currentTimeMillis: Long): String? {
        val timestampKey = "${cacheKey}_timestamp"
        if (sharedPreferences.containsKey(timestampKey)) {
            val timeInCache = currentTimeMillis - sharedPreferences.getLong(timestampKey)
            if (timeInCache < TIME_TO_LIVE) {
                return sharedPreferences.getString(cacheKey, "")
            }
        }
        return null
    }

    fun saveConfiguration(configuration: Configuration, cacheKey: String?) {
        saveConfiguration(configuration, cacheKey, System.currentTimeMillis())
    }

    fun saveConfiguration(
        configuration: Configuration,
        cacheKey: String?,
        currentTimeMillis: Long
    ) {
        val timestampKey = "${cacheKey}_timestamp"
        sharedPreferences.putStringAndLong(
            cacheKey,
            configuration.toJson(),
            timestampKey,
            currentTimeMillis
        )
    }

    companion object {
        private val TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5)

        @Volatile
        private var INSTANCE: ConfigurationCache? = null
        fun getInstance(context: Context): ConfigurationCache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigurationCache(
                    BraintreeSharedPreferences.getInstance(context)
                ).also { INSTANCE = it }
            }
    }
}
