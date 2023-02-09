package com.braintreepayments.api

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.braintreepayments.api.Configuration.Companion.fromJson
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
        var cachedConfig: Configuration? = null
        var loadFromCacheException: BraintreeSharedPreferencesException? = null
        try {
            cachedConfig = getCachedConfiguration(authorization, configUrl)
        } catch (e: BraintreeSharedPreferencesException) {
            loadFromCacheException = e
        }
        cachedConfig?.let {
            val resultFromCache = ConfigurationLoaderResult(cachedConfig)
            callback.onResult(resultFromCache, null)
        } ?: run {
            val finalLoadFromCacheException = loadFromCacheException
            httpClient[configUrl, null, authorization, HttpClient.RETRY_MAX_3_TIMES, HttpResponseCallback { responseBody, httpError ->
                responseBody?.let {
                    try {
                        val configuration = fromJson(it)
                        var saveToCacheException: BraintreeSharedPreferencesException? = null
                        try {
                            saveConfigurationToCache(configuration, authorization, configUrl)
                        } catch (e: BraintreeSharedPreferencesException) {
                            saveToCacheException = e
                        }
                        val resultFromNetwork = ConfigurationLoaderResult(
                            configuration,
                            finalLoadFromCacheException,
                            saveToCacheException
                        )
                        callback.onResult(resultFromNetwork, null)
                    } catch (jsonException: JSONException) {
                        callback.onResult(null, jsonException)
                    }
                } ?: run {
                    val errorMessageFormat = "Request for configuration has failed: %s"
                    val errorMessage = String.format(errorMessageFormat, httpError!!.message)
                    val configurationException = ConfigurationException(errorMessage, httpError)
                    callback.onResult(null, configurationException)
                }
            }]
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
        val cachedConfigResponse = configurationCache.getConfiguration(cacheKey)
        return try {
            fromJson(cachedConfigResponse)
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        private fun createCacheKey(authorization: Authorization, configUrl: String): String {
            return Base64.encodeToString(
                String.format("%s%s", configUrl, authorization.bearer).toByteArray(), 0
            )
        }
    }
}