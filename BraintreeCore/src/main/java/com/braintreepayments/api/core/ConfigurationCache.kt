package com.braintreepayments.api.core

import android.content.Context
import android.util.Base64
import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import com.braintreepayments.api.sharedutils.Timestamper
import org.json.JSONException
import java.util.concurrent.TimeUnit

internal class ConfigurationCache(
    private val sharedPreferences: BraintreeSharedPreferences,
    private val timestamper: Timestamper = Timestamper()
) {

    fun getConfiguration(authorization: Authorization, configUrl: String): Configuration? {
        val cacheKey = createCacheKey(authorization, configUrl)
        val timestampKey = "${cacheKey}_timestamp"

        var configurationAsString: String? = null
        if (sharedPreferences.containsKey(timestampKey)) {
            val timeInCache = timestamper.now - sharedPreferences.getLong(timestampKey)
            if (timeInCache < TIME_TO_LIVE) {
                configurationAsString = sharedPreferences.getString(cacheKey, "")
            }
        }

        return try {
            configurationAsString?.let { Configuration.fromJson(it) }
        } catch (e: JSONException) {
            null
        }
    }

    fun putConfiguration(
        configuration: Configuration,
        authorization: Authorization,
        configUrl: String
    ) {
        val cacheKey = createCacheKey(authorization, configUrl)
        val timestampKey = "${cacheKey}_timestamp"
        sharedPreferences.putStringAndLong(
            cacheKey,
            configuration.toJson(),
            timestampKey,
            timestamper.now
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

        private fun createCacheKey(authorization: Authorization, configUrl: String): String {
            return Base64.encodeToString("$configUrl${authorization.bearer}".toByteArray(), 0)
        }
    }
}
