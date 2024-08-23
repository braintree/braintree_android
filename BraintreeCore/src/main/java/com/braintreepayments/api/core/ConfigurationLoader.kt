package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
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

        val cachedConfig = configurationCache.getConfiguration(authorization, configUrl)
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
                        configurationCache.putConfiguration(configuration, authorization, configUrl)
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
}
