package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.sharedutils.ThreadScheduler
import org.json.JSONException
import java.lang.ref.WeakReference

internal class ConfigurationLoader internal constructor(
    private val httpClient: BraintreeHttpClient,
    private val configurationCache: ConfigurationCache,
    private val scheduler: Scheduler = ThreadScheduler()
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

        val callbackRef = WeakReference(callback)
        scheduler.runOnBackground {
            val configUrl = buildConfigurationURL(authorization)
            val cachedConfig = getCachedConfiguration(authorization, configUrl)
            cachedConfig?.let {
                callbackRef.get()?.let {
                    scheduler.runOnMain {
                        callback.onResult(cachedConfig, null, null)
                    }
                }
            } ?: run {
                val request = BraintreeHttpRequest(method = "GET", path = configUrl)
                try {
                    val response = httpClient.sendRequestSync(
                        request = request,
                        configuration = null,
                        authorization = authorization
                    )
                    val configuration = Configuration.fromJson(response.body!!)
                    saveConfigurationToCache(configuration, authorization, configUrl)
                    callbackRef.get()?.let {
                        scheduler.runOnMain {
                            callback.onResult(configuration, null, response.timing)
                        }
                    }

                } catch (error: Exception) {
                    val errorMessageFormat = "Request for configuration has failed: %s"
                    val errorMessage = String.format(errorMessageFormat, error.message)
                    val configurationException = ConfigurationException(errorMessage, error)
                    callbackRef.get()?.let {
                        scheduler.runOnMain {
                            callback.onResult(null, configurationException, null)
                        }
                    }
                }
            }
        }
    }

    private fun buildConfigurationURL(authorization: Authorization) =
        Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()

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
