package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class ConfigurationService(
    context: Context,
    private val httpClient: BraintreeHttpClient,
    private val backgroundService: ExecutorService = Executors.newFixedThreadPool(1),
    private val configurationCache: ConfigurationCache = ConfigurationCache.getInstance(context),
) {

    fun loadConfiguration(authorization: Authorization, callback: ConfigurationLoaderCallback) {
        backgroundService.submit {
            loadConfigurationInBackground(authorization, callback)
        }
    }

    private fun loadConfigurationInBackground(
        authorization: Authorization,
        callback: ConfigurationLoaderCallback
    ) {
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
            try {
                // TODO: implement retries
                val responseBody = httpClient.get(configUrl, null, authorization)
                val configuration = Configuration.fromJson(responseBody)
                saveConfigurationToCache(configuration, authorization, configUrl)
                // TODO: implement timing
                callback.onResult(configuration, null, null)
            } catch (error: Exception) {
                val errorMessageFormat = "Request for configuration has failed: %s"
                val errorMessage = String.format(errorMessageFormat, error.message)
                val configurationException = ConfigurationException(errorMessage, error)
                callback.onResult(null, configurationException, null)
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