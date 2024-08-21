package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpMethod
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
            // NOTE: timing information is null when configuration comes from cache
            callback.onResult(null, BraintreeException(message), null)
            return
        }
        val configUrl = Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()
        val cachedConfig = getCachedConfiguration(authorization, configUrl)

        cachedConfig?.let {
            callback.onResult(cachedConfig, null, null)
        } ?: run {
            val request = InternalHttpRequest(method = HttpMethod.GET, path = configUrl)
            httpClient.sendRequest(
                request = request,
                authorization = authorization
            ) { response, httpError ->
                val responseBody = response?.body
                val timing = response?.timing
                if (responseBody != null) {
                    try {
                        val configuration = Configuration.fromJson(responseBody)
                        saveConfigurationToCache(configuration, authorization, configUrl)
                        callback.onResult(configuration, null, timing)
                    } catch (jsonException: JSONException) {
                        callback.onResult(null, jsonException, null)
                    }
                } else {
                    httpError?.let { error ->
                        val errorMessageFormat = "Request for configuration has failed: %s"
                        val errorMessage = String.format(errorMessageFormat, error.message)
                        val configurationException = ConfigurationException(errorMessage, error)
                        callback.onResult(null, configurationException, null)
                    }
                }
            }
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

    private fun getCachedConfiguration(
        authorization: Authorization,
        configUrl: String
    ): Configuration? {
        val cacheKey = createCacheKey(authorization, configUrl)
        val cachedConfigResponse = configurationCache.getConfiguration(cacheKey) ?: return null
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
