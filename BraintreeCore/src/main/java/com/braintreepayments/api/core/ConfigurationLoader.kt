package com.braintreepayments.api.core

import android.net.Uri
import android.util.Base64
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Time
import org.json.JSONException

internal class ConfigurationLoader(
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val configurationCache: ConfigurationCache = ConfigurationCacheProvider().configurationCache,
    private val time: Time = Time(),
    /**
     * TODO: AnalyticsClient must be lazy due to the circular dependency between ConfigurationLoader and AnalyticsClient
     * This should be refactored to remove the circular dependency.
     */
    lazyAnalyticsClient: Lazy<AnalyticsClient> = lazy { AnalyticsClient(httpClient) },
) {
    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

    fun loadConfiguration(callback: ConfigurationLoaderCallback) {
        val authorization = merchantRepository.authorization
        if (authorization is InvalidAuthorization) {
            val clientSDKSetupURL =
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
            val message = "Valid authorization required. See $clientSDKSetupURL for more info."

            // NOTE: timing information is null when configuration comes from cache
            callback.onResult(ConfigurationLoaderResult.Failure(BraintreeException(message)))
            return
        }
        val configUrl = Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()
        val cachedConfig = getCachedConfiguration(authorization, configUrl)

        cachedConfig?.let {
            callback.onResult(ConfigurationLoaderResult.Success(it))
        } ?: run {
            httpClient.get(
                configUrl, null, authorization, HttpClient.RetryStrategy.RETRY_MAX_3_TIMES
            ) { response, httpError ->
                val responseBody = response?.body
                val timing = response?.timing
                if (responseBody != null) {
                    try {
                        val configuration = Configuration.fromJson(responseBody)
                        saveConfigurationToCache(configuration, authorization, configUrl)
                        callback.onResult(ConfigurationLoaderResult.Success(configuration, timing))

                        analyticsClient.sendEvent(
                            eventName = CoreAnalytics.API_REQUEST_LATENCY,
                            analyticsEventParams = AnalyticsEventParams(
                                startTime = timing?.startTime,
                                endTime = timing?.endTime,
                                endpoint = "/v1/configuration"
                            )
                        )
                    } catch (jsonException: JSONException) {
                        callback.onResult(ConfigurationLoaderResult.Failure(jsonException))
                    }
                } else {
                    httpError?.let { error ->
                        val errorMessageFormat = "Request for configuration has failed: %s"
                        val errorMessage = String.format(errorMessageFormat, error.message)
                        val configurationException = ConfigurationException(errorMessage, error)
                        callback.onResult(ConfigurationLoaderResult.Failure(configurationException))
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

        /**
         * Singleton instance of the ConfigurationLoader.
         */
        val instance: ConfigurationLoader by lazy { ConfigurationLoader() }
    }
}
