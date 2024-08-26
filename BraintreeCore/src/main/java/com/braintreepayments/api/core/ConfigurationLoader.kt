package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.sharedutils.ThreadScheduler
import org.json.JSONException
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

internal class ConfigurationLoader internal constructor(
    private val httpClient: BraintreeHttpClient,
    private val configurationCache: ConfigurationCache,
    private val threadScheduler: Scheduler = ThreadScheduler(1)
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
        loadConfigurationInBackground(authorization, callback)
    }

    private fun loadConfigurationInBackground(
        authorization: Authorization,
        callback: ConfigurationLoaderCallback
    ) {
        val configUrl = Uri.parse(authorization.configUrl)
            .buildUpon()
            .appendQueryParameter("configVersion", "3")
            .build()
            .toString()

        val cbRef = WeakReference(callback)
        threadScheduler.runOnBackground {
            val (configuration, error, timing) =
                fetchConfigurationFromCache(authorization, configUrl)
                    ?: fetchConfigurationFromNetwork(authorization, configUrl)
            threadScheduler.runOnMain {
                val cb = cbRef.get()
                cb?.onResult(configuration, error, timing)
            }
        }
    }

    private fun fetchConfigurationFromCache(
        authorization: Authorization,
        configUrl: String
    ): Triple<Configuration?, Exception?, HttpResponseTiming?>? =
        configurationCache.getConfiguration(authorization, configUrl)?.let { configuration ->
            // NOTE: timing information is null when configuration comes from cache
            return Triple(configuration, null, null)
        }


    @WorkerThread
    private fun fetchConfigurationFromNetwork(
        authorization: Authorization,
        configUrl: String
    ): Triple<Configuration?, Exception?, HttpResponseTiming?> {
        var configuration: Configuration? = null
        var error: Exception? = null
        var timing: HttpResponseTiming? = null

        val request = InternalHttpRequest(method = HttpMethod.GET, path = configUrl)
        try {
            val response = httpClient.sendRequestSync(
                request = request,
                configuration = null,
                authorization = authorization,
            )

            // capture timing stats
            timing = response.timing

            // parse configuration
            configuration = response.body?.let { Configuration.fromJson(it) }

            // save configuration to cache (if present)
            configuration?.let { configurationCache.putConfiguration(it, authorization, configUrl) }
        } catch (e: Exception) {
            val errorMessageFormat = "Request for configuration has failed: %s"
            val errorMessage = String.format(errorMessageFormat, e.message)
            error = ConfigurationException(errorMessage, e)
        }
        return Triple(configuration, error, timing)
    }
}
