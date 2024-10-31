package com.braintreepayments.api.core

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import com.braintreepayments.api.sharedutils.HttpResponseTiming
import com.braintreepayments.api.sharedutils.ManifestValidator
import com.braintreepayments.api.sharedutils.Time
import org.json.JSONException
import org.json.JSONObject

/**
 * Core Braintree class that handles network requests.
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BraintreeClient internal constructor(

    /**
     * @suppress
     */
    val applicationContext: Context,

    /**
     * @suppress
     */
    val integrationType: IntegrationType,

    /**
     * @suppress
     */
    val authorization: Authorization,

    private val returnUrlScheme: String,

    /**
     * @suppress
     */
    val appLinkReturnUri: Uri?,

    private val analyticsClient: AnalyticsClient = AnalyticsClient(applicationContext),
    private val httpClient: BraintreeHttpClient = BraintreeHttpClient(),
    private val graphQLClient: BraintreeGraphQLClient = BraintreeGraphQLClient(),
    private val configurationLoader: ConfigurationLoader = ConfigurationLoader(applicationContext, httpClient),
    private val manifestValidator: ManifestValidator = ManifestValidator(),
    private val time: Time = Time(),
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
    ) : this(
        applicationContext = context.applicationContext,
        authorization = Authorization.fromString(authorization),
        returnUrlScheme = returnUrlScheme
            ?: "${getAppPackageNameWithoutUnderscores(context.applicationContext)}.braintree",
        appLinkReturnUri = appLinkReturnUri,
        integrationType = integrationType ?: IntegrationType.CUSTOM,
    )

    init {
        // TODO: CrashReporter isn't a part of BraintreeClientParams
        //  because it requires a reference to BraintreeClient. This is a design flaw that creates
        //  a circular reference. We should consider if we need CrashReporter anymore since
        //  merchants already have access to Crash statistics via GooglePlay. We also have crash
        //  statistics access via the sdk console
        crashReporter = CrashReporter(this)
        crashReporter.start()
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback [ConfigurationCallback]
     */
    fun getConfiguration(callback: ConfigurationCallback) {
        if (authorization is InvalidAuthorization) {
            callback.onResult(null, createAuthError())
            return
        }
        configurationLoader.loadConfiguration(authorization) { configuration, configError, timing ->
            if (configuration != null) {
                callback.onResult(configuration, null)
            } else {
                callback.onResult(null, configError)
            }
            timing?.let { sendAnalyticsTimingEvent("/v1/configuration", it) }
        }
    }

    /**
     * @suppress
     */
    fun sendAnalyticsEvent(
        eventName: String,
        params: AnalyticsEventParams = AnalyticsEventParams()
    ) {
        val timestamp = time.currentTime
        getConfiguration { configuration, _ ->
            val event = AnalyticsEvent(
                name = eventName,
                timestamp = timestamp,
                payPalContextId = params.payPalContextId,
                linkType = params.linkType,
                isVaultRequest = params.isVaultRequest,
                startTime = params.startTime,
                endTime = params.endTime,
                endpoint = params.endpoint,
                experiment = params.experiment,
                paymentMethodsDisplayed = params.paymentMethodsDisplayed
            )
            sendAnalyticsEvent(event, configuration, authorization)
        }
    }

    private fun sendAnalyticsEvent(
        event: AnalyticsEvent,
        configuration: Configuration?,
        authorization: Authorization
    ) {
        configuration?.let {
            analyticsClient.sendEvent(
                it,
                event,
                integrationType,
                authorization
            )
        }
    }

    /**
     * @suppress
     */
    fun sendGET(url: String, responseCallback: HttpResponseCallback) {
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                httpClient.get(url, configuration, authorization) { response, httpError ->
                    response?.let {
                        try {
                            sendAnalyticsTimingEvent(url, response.timing)
                            responseCallback.onResult(it.body, null)
                        } catch (jsonException: JSONException) {
                            responseCallback.onResult(null, jsonException)
                        }
                    } ?: httpError?.let { error ->
                        responseCallback.onResult(null, error)
                    }
                }
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    @JvmOverloads
    fun sendPOST(
        url: String,
        data: String,
        additionalHeaders: Map<String, String> = emptyMap(),
        responseCallback: HttpResponseCallback,
    ) {
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                httpClient.post(
                    path = url,
                    data = data,
                    configuration = configuration,
                    authorization = authorization,
                    additionalHeaders = additionalHeaders
                ) { response, httpError ->
                    response?.let {
                        try {
                            sendAnalyticsTimingEvent(url, it.timing)
                            responseCallback.onResult(it.body, null)
                        } catch (jsonException: JSONException) {
                            responseCallback.onResult(null, jsonException)
                        }
                    } ?: httpError?.let { error ->
                        responseCallback.onResult(null, error)
                    }
                }
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    fun sendGraphQLPOST(json: JSONObject?, responseCallback: HttpResponseCallback) {
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                graphQLClient.post(
                    json?.toString(),
                    configuration,
                    authorization
                ) { response, httpError ->
                    response?.let {
                        try {
                            json?.optString(GraphQLConstants.Keys.QUERY)
                                ?.let { query ->
                                    val queryDiscardHolder = query.replace(Regex("^[^\\(]*"), "")
                                    val finalQuery = query.replace(queryDiscardHolder, "")
                                    val params = AnalyticsEventParams(
                                        startTime = it.timing.startTime,
                                        endTime = it.timing.endTime,
                                        endpoint = finalQuery
                                    )
                                    sendAnalyticsEvent(
                                        CoreAnalytics.apiRequestLatency,
                                        params
                                    )
                                }
                            responseCallback.onResult(it.body, null)
                        } catch (jsonException: JSONException) {
                            responseCallback.onResult(null, jsonException)
                        }
                    } ?: httpError?.let { error ->
                        responseCallback.onResult(null, error)
                    }
                }
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    fun getReturnUrlScheme(): String {
        return if (launchesBrowserSwitchAsNewTask) {
            braintreeDeepLinkReturnUrlScheme
        } else {
            returnUrlScheme
        }
    }

    /**
     * @suppress
     */
    fun <T> isUrlSchemeDeclaredInAndroidManifest(urlScheme: String?, klass: Class<T>?): Boolean {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(
            applicationContext,
            urlScheme,
            klass
        )
    }

    /**
     * @suppress
     */
    fun <T> getManifestActivityInfo(klass: Class<T>?): ActivityInfo? {
        return manifestValidator.getActivityInfo(applicationContext, klass)
    }

    /**
     * @suppress
     */
    internal fun reportCrash() =
        getConfiguration { configuration, _ ->
            analyticsClient.reportCrash(
                applicationContext,
                configuration,
                integrationType,
                authorization
            )
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
            CoreAnalytics.apiRequestLatency,
            AnalyticsEventParams(
                startTime = timing.startTime,
                endTime = timing.endTime,
                endpoint = cleanedPath
            )
        )
    }

    /**
     * Set this property to true to allow the SDK to handle deep links on behalf of the host
     * application for browser switched flows.
     *
     * For web payment flows, this means launching the browser in a task separate from the calling
     * activity.
     *B
     * NOTE: When this property is set to true, all custom url schemes set in [BraintreeClient]
     * constructors will be ignored.
     *
     * @param launchesBrowserSwitchAsNewTask set to true to allow the SDK to capture deep links.
     * This value is false by default.
     */
    fun launchesBrowserSwitchAsNewTask(launchesBrowserSwitchAsNewTask: Boolean) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask
    }

    private fun createAuthError(): BraintreeException {
        val clientSDKSetupURL =
            "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
        val message = "Valid authorization required. See $clientSDKSetupURL for more info."
        return BraintreeException(message)
    }

    companion object {
        private fun getAppPackageNameWithoutUnderscores(context: Context): String {
            return context.applicationContext.packageName.replace("_", "")
        }
    }
}
