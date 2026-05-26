package com.braintreepayments.api.core

import android.net.Uri
import android.util.Base64
import org.json.JSONException
import java.io.IOException

internal class ConfigurationLoader(
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val configurationCache: ConfigurationCache = ConfigurationCacheProvider().configurationCache,

    /**
     * TODO: AnalyticsClient must be lazy due to the circular dependency between ConfigurationLoader and AnalyticsClient
     * This should be refactored to remove the circular dependency.
     */
    lazyAnalyticsClient: Lazy<AnalyticsClient> = lazy {
        AnalyticsClient()
    },
) {
    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

    /**
     * Loads configuration using the authorization from [MerchantRepository].
     * Sends analytics on successful API fetch.
     */
    suspend fun loadConfiguration(): ConfigurationLoaderResult {
        return loadConfiguration(merchantRepository.authorization, sendAnalytics = true)
    }

    /**
     * Loads configuration for a standalone [authorization], without sending analytics.
     * Used by [Configuration.fetch] to support merchant-facing configuration queries
     * outside the normal SDK lifecycle.
     */
    suspend fun loadConfiguration(authorization: Authorization): ConfigurationLoaderResult {
        return loadConfiguration(authorization, sendAnalytics = false)
    }

    private suspend fun loadConfiguration(
        authorization: Authorization,
        sendAnalytics: Boolean
    ): ConfigurationLoaderResult {
        if (authorization is InvalidAuthorization) {
            val clientSDKSetupURL =
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
            val message = "Valid authorization required. See $clientSDKSetupURL for more info."

            return ConfigurationLoaderResult.Failure(BraintreeException(message))
        }
        val configUrl = Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()

        val cachedConfig = getCachedConfiguration(authorization, configUrl)
        if (cachedConfig != null) {
            return ConfigurationLoaderResult.Success(cachedConfig)
        }

        return executeConfigurationApi(configUrl, authorization, sendAnalytics)
    }

    private suspend fun executeConfigurationApi(
        configUrl: String,
        authorization: Authorization,
        sendAnalytics: Boolean
    ): ConfigurationLoaderResult {
        try {
            val response = httpClient.get(
                path = configUrl,
                configuration = null,
                authorization = authorization
            )

            val responseBody = response.body ?: run {
                return ConfigurationLoaderResult.Failure(
                    ConfigurationException("Configuration responseBody is null")
                )
            }

            val timing = response.timing
            try {
                val configuration = Configuration.fromJson(responseBody)
                saveConfigurationToCache(configuration, authorization, configUrl)
                if (sendAnalytics) {
                    analyticsClient.sendEvent(
                        eventName = CoreAnalytics.API_REQUEST_LATENCY,
                        analyticsEventParams = AnalyticsEventParams(
                            startTime = timing.startTime,
                            endTime = timing.endTime,
                            endpoint = "/v1/configuration"
                        ),
                        sendImmediately = false
                    )
                }
                return ConfigurationLoaderResult.Success(configuration, timing)
            } catch (jsonException: JSONException) {
                return ConfigurationLoaderResult.Failure(jsonException)
            }
        } catch (e: IOException) {
            val errorMessageFormat = "Request for configuration has failed: %s"
            val errorMessage = String.format(errorMessageFormat, e.message)
            val configurationException = ConfigurationException(errorMessage, e)
            return ConfigurationLoaderResult.Failure(configurationException)
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

        /**
         * Singleton instance of the ConfigurationLoader.
         */
        val instance: ConfigurationLoader by lazy { ConfigurationLoader() }
    }
}
