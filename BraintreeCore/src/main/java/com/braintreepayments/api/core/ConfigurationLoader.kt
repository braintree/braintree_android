package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.sharedutils.ThreadScheduler
import java.lang.ref.WeakReference

internal class ConfigurationLoader internal constructor(
    private val httpClient: BraintreeHttpClient,
    private val configurationCache: ConfigurationCache,
    private val threadScheduler: Scheduler = ThreadScheduler(SERIAL_DISPATCH_QUEUE_POOL_SIZE)
) {
    constructor(context: Context, httpClient: BraintreeHttpClient) : this(
        httpClient, ConfigurationCache.getInstance(context)
    )

    fun loadConfiguration(authorization: Authorization, callback: ConfigurationLoaderCallback) {
        if (authorization is InvalidAuthorization) {
            val invalidAuthError = BraintreeException(authorization.errorMessage)
            callback.onResult(ConfigurationLoaderResponse(error = invalidAuthError))
            return
        }
        loadConfigurationInBackground(authorization, callback)
    }

    private fun loadConfigurationInBackground(
        auth: Authorization,
        callback: ConfigurationLoaderCallback
    ) {
        val url = Uri.parse(auth.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()

        val cbRef = WeakReference(callback)
        threadScheduler.runOnBackground {
            val response = loadConfigFromCache(url, auth) ?: loadConfigFromNetwork(url, auth)
            threadScheduler.runOnMain {
                val cb = cbRef.get()
                cb?.onResult(response)
            }
        }
    }

    private fun loadConfigFromCache(
        configUrl: String,
        authorization: Authorization
    ): ConfigurationLoaderResponse? =
        configurationCache.getConfiguration(authorization, configUrl)?.let { configuration ->
            // NOTE: timing information is null when configuration comes from cache
            return ConfigurationLoaderResponse(configuration = configuration, timing = null)
        }

    @Suppress("TooGenericExceptionCaught")
    @WorkerThread
    private fun loadConfigFromNetwork(
        configUrl: String,
        authorization: Authorization
    ): ConfigurationLoaderResponse {
        val request = InternalHttpRequest(method = HttpMethod.GET, path = configUrl)
        try {
            val response = httpClient.sendRequestSync(
                request = request,
                configuration = null,
                authorization = authorization,
            )

            // parse configuration
            val configuration = response.body?.let { Configuration.fromJson(it) }

            // save configuration to cache (if present)
            configuration?.let { configurationCache.putConfiguration(it, authorization, configUrl) }

            val timing = response.timing
            return ConfigurationLoaderResponse(configuration = configuration, timing = timing)
        } catch (error: Exception) {
            val errorMessage = "Request for configuration has failed: ${error.message}"
            return ConfigurationLoaderResponse(error = ConfigurationException(errorMessage, error))
        }
    }

    companion object {
        // NOTE: a single thread pool makes the ThreadScheduler behave like a serial dispatch queue
        const val SERIAL_DISPATCH_QUEUE_POOL_SIZE = 1
    }
}
