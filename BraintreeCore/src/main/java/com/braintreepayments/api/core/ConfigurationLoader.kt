package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.annotation.WorkerThread
import com.braintreepayments.api.sharedutils.HttpResponse
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
        val callbackRef = WeakReference(callback)
        scheduler.runOnBackground {
            val configUrl = buildConfigURL(authorization)
            val cachedConfig = loadConfigurationFromCache(authorization, configUrl)
            if (cachedConfig == null) {
                try {
                    val configResponse = loadConfigurationFromNetwork(configUrl, authorization)
                    val configuration = Configuration.fromJson(configResponse.body!!)
                    saveConfigurationToCache(configuration, authorization, configUrl)
                    scheduler.runOnMain {
                        val cb = callbackRef.get()
                        cb?.onResult(configuration, null, configResponse.timing)
                    }
                } catch (error: Exception) {
                    val errorMessageFormat = "Request for configuration has failed: %s"
                    val errorMessage = String.format(errorMessageFormat, error.message)
                    val configurationException = ConfigurationException(errorMessage, error)
                    scheduler.runOnMain {
                        val cb = callbackRef.get()
                        cb?.onResult(null, configurationException, null)
                    }
                }
            } else {
                scheduler.runOnMain {
                    val cb = callbackRef.get()
                    cb?.onResult(cachedConfig, null, null)
                }
            }
        }
    }

    @WorkerThread
    private fun loadConfigurationFromNetwork(
        configUrl: String,
        authorization: Authorization
    ): HttpResponse {
        val request = BraintreeHttpRequest(method = "GET", path = configUrl)
        return httpClient.sendRequestSync(
            request = request,
            configuration = null,
            authorization = authorization
        )
    }

    private fun buildConfigURL(authorization: Authorization) =
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

    private fun loadConfigurationFromCache(
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
