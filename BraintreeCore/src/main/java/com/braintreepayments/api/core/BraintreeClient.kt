package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.ManifestValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

/**
 * Core Braintree class that handles network requests.
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BraintreeClient internal constructor(
    applicationContext: Context,
    integrationType: IntegrationType,
    authorization: Authorization,
    returnUrlScheme: String,
    appLinkReturnUri: Uri?,
    deepLinkFallbackUrlScheme: String? = null,
    sdkComponent: SdkComponent = SdkComponent.create(applicationContext),
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val graphQLClient: BraintreeGraphQLClient = BraintreeGraphQLClient(),
    private val configurationLoader: ConfigurationLoader = ConfigurationLoader.instance,
    private val manifestValidator: ManifestValidator = ManifestValidator(),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val analyticsClient: AnalyticsClient = AnalyticsClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) {

    private val crashReporter: CrashReporter
    private var launchesBrowserSwitchAsNewTask: Boolean = false

    private val braintreeDeepLinkReturnUrlScheme: String =
        "${getAppPackageNameWithoutUnderscores(applicationContext)}.braintree.deeplinkhandler"

    /**
     * @suppress
     */
    constructor (
        context: Context,
        authorization: String,
        returnUrlScheme: String? = null,
        appLinkReturnUri: Uri? = null,
        integrationType: IntegrationType? = null,
        deepLinkFallbackUrlScheme: String? = null,
    ) : this(
        applicationContext = context.applicationContext,
        authorization = Authorization.fromString(authorization),
        returnUrlScheme = returnUrlScheme
            ?: "${getAppPackageNameWithoutUnderscores(context.applicationContext)}.braintree",
        appLinkReturnUri = appLinkReturnUri,
        integrationType = integrationType ?: IntegrationType.CUSTOM,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
    )

    init {
        // TODO: CrashReporter isn't a part of BraintreeClientParams
        //  because it requires a reference to BraintreeClient. This is a design flaw that creates
        //  a circular reference. We should consider if we need CrashReporter anymore since
        //  merchants already have access to Crash statistics via GooglePlay. We also have crash
        //  statistics access via the sdk console
        crashReporter = CrashReporter(this)
        crashReporter.start()

        merchantRepository.let {
            it.applicationContext = applicationContext
            it.integrationType = integrationType
            it.authorization = authorization
            it.returnUrlScheme = returnUrlScheme
            if (appLinkReturnUri != null) {
                it.appLinkReturnUri = appLinkReturnUri
            }
            if (deepLinkFallbackUrlScheme != null) {
                it.deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
            }
        }

        prefetchConfiguration()
    }

    @Suppress("SwallowedException")
    private fun prefetchConfiguration() {
        // This method is called to prefetch the configuration when the BraintreeClient is created.
        // It ensures that the configuration is loaded and ready for use in subsequent requests.
        coroutineScope.launch {
            try {
                val config = getConfiguration()
            } catch (e: IOException) {
                // no op
            }
        }
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback [ConfigurationCallback]
     */
    suspend fun getConfiguration(): Configuration {
        val configResult = configurationLoader.loadConfiguration()
        when (configResult) {
            is ConfigurationLoaderResult.Success -> {
                configResult.timing?.let { sendAnalyticsTimingEvent("/v1/configuration", it) }
                return configResult.configuration
            }

            is ConfigurationLoaderResult.Failure -> throw configResult.error
        }
    }

    /**
     * @suppress
     */
    fun sendAnalyticsEvent(
        eventName: String,
        params: AnalyticsEventParams = AnalyticsEventParams(),
        sendImmediately: Boolean = true,
    ) {
        analyticsClient.sendEvent(eventName, params, sendImmediately)
    }

    /**
     * @suppress
     */
    suspend fun sendGET(url: String): String {
        val configuration = getConfiguration()
        val response = httpClient.get(
            path = url,
            configuration = configuration,
            authorization = merchantRepository.authorization
        )

        sendAnalyticsTimingEvent(url, response.timing)
        return response.body ?: throw IOException("Response body is null")
    }

    /**
     * @suppress
     */
    @JvmOverloads
    suspend fun sendPOST(
        url: String,
        data: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): String {
        val configuration = getConfiguration()
        val response = httpClient.post(
            path = url,
            data = data,
            configuration = configuration,
            authorization = merchantRepository.authorization,
            additionalHeaders = additionalHeaders
        )
        sendAnalyticsTimingEvent(url, response.timing)
        return response.body ?: throw IOException("Response body is null")
    }

    /**
     * @suppress
     */
    suspend fun sendGraphQLPOST(json: JSONObject): String {
        val configuration = getConfiguration()
        val response = graphQLClient.post(
            data = json.toString(),
            configuration = configuration,
            authorization = merchantRepository.authorization
        )

        val query = json.optString(GraphQLConstants.Keys.QUERY)
        val queryDiscardHolder = query.replace(Regex("^[^\\(]*"), "")
        val finalQuery = query.replace(queryDiscardHolder, "")
        val params = AnalyticsEventParams(
            startTime = response.timing.startTime,
            endTime = response.timing.endTime,
            endpoint = finalQuery
        )

        sendAnalyticsEvent(
            eventName = CoreAnalytics.API_REQUEST_LATENCY,
            params = params,
            sendImmediately = false
        )

        return response.body ?: throw IOException("Response body is null")
    }

    /**
     * @suppress
     */
    fun getReturnUrlScheme(): String {
        return if (launchesBrowserSwitchAsNewTask) {
            braintreeDeepLinkReturnUrlScheme
        } else {
            merchantRepository.returnUrlScheme
        }
    }

    /**
     * @suppress
     */
    fun <T> getManifestActivityInfo(klass: Class<T>): ActivityInfo? {
        return manifestValidator.getActivityInfo(merchantRepository.applicationContext, klass)
    }

    /**
     * @suppress
     */
    @Suppress("SwallowedException")
    internal fun reportCrash() {
        coroutineScope.launch {
            try {
                val configuration = getConfiguration()
                analyticsClient.reportCrash(configuration)
            } catch (e: IOException) {
                // no op
            }
        }
    }

    // TODO: Make launches browser switch as new task a property of `BraintreeOptions`
    fun launchesBrowserSwitchAsNewTask(): Boolean {
        return launchesBrowserSwitchAsNewTask
    }

    private fun sendAnalyticsTimingEvent(endpoint: String, timing: HttpResponseTiming) {
        var cleanedPath = endpoint.replace(Regex("/merchants/([A-Za-z0-9]+)/client_api"), "")
        cleanedPath = cleanedPath.replace(
            Regex("payment_methods/.*/three_d_secure"), "payment_methods/three_d_secure"
        )

        sendAnalyticsEvent(
            eventName = CoreAnalytics.API_REQUEST_LATENCY,
            params = AnalyticsEventParams(
                startTime = timing.startTime,
                endTime = timing.endTime,
                endpoint = cleanedPath
            ),
            sendImmediately = false
        )
    }

    companion object {
        private fun getAppPackageNameWithoutUnderscores(context: Context): String {
            return context.applicationContext.packageName.replace("_", "")
        }
    }
}
