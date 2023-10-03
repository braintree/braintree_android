package com.braintreepayments.api

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONException

internal class ConfigurationLoader internal constructor(
    private val httpClient: BraintreeHttpClient,
    private val configurationCache: ConfigurationCache
) {
    constructor(context: Context, httpClient: BraintreeHttpClient) : this(
        httpClient, ConfigurationCache.getInstance(context)
    )

    fun loadConfiguration(authorization: Authorization, callback: ConfigurationLoaderCallback) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(null, BraintreeException(message))
            return
        }
        val configUrl = Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()
        val cachedConfig = getCachedConfiguration(authorization, configUrl)

        cachedConfig?.let {
            callback.onResult(cachedConfig, null)
        } ?: run {
            httpClient.get(configUrl, null, authorization, HttpClient.RETRY_MAX_3_TIMES,
                object : HttpResponseCallback {
                    override fun onResult(responseBody: String?, httpError: Exception?) {
                        responseBody?.let {
                            try {
                                val configuration = Configuration.fromJson(it)
                                saveConfigurationToCache(configuration, authorization, configUrl)
                                callback.onResult(configuration, null)
                            } catch (jsonException: JSONException) {
                                callback.onResult(null, jsonException)
                            }
                        } ?: httpError?.let { error ->
                            val errorMessageFormat = "Request for configuration has failed: %s"
                            val errorMessage = String.format(errorMessageFormat, error.message)
                            val configurationException = ConfigurationException(errorMessage, error)
                            callback.onResult(null, configurationException)
                        }
                    }
                })
        }
    }

    private fun saveConfigurationToCache(
        configuration: Configuration,
        authorization: Authorization,
        configUrl: String
    ) {
        val cacheKey = createCacheKey(authorization, configUrl)
        configurationCache.saveConfiguration(configuration, cacheKey)
    }

    private fun getCachedConfiguration(authorization: Authorization, configUrl: String): Configuration? {
        val cacheKey = createCacheKey(authorization, configUrl)
        val cachedConfigResponse = configurationCache.getConfiguration(cacheKey)
        return try {
            Configuration.fromJson(cachedConfigResponse)
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        private fun createCacheKey(authorization: Authorization, configUrl: String): String {
            return Base64.encodeToString("$configUrl${authorization.bearer}".toByteArray(), 0)
        }
    }
}
