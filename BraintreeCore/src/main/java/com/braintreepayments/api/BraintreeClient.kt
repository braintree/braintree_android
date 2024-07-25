package com.braintreepayments.api

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import org.json.JSONException
import org.json.JSONObject

/**
 * Core Braintree class that handles network requests.
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
open class BraintreeClient @VisibleForTesting internal constructor(

    /**
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val applicationContext: Context,

    /**
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val integrationType: String,

    /**
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val sessionId: String,

    private val authorizationLoader: AuthorizationLoader,
    private val analyticsClient: AnalyticsClient,
    private val httpClient: BraintreeHttpClient,
    private val graphQLClient: BraintreeGraphQLClient,
    private val browserSwitchClient: BrowserSwitchClient,
    private val configurationLoader: ConfigurationLoader,
    private val manifestValidator: ManifestValidator,
    private val returnUrlScheme: String,
    private val braintreeDeepLinkReturnUrlScheme: String,

    /**
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val appLinkReturnUri: Uri?,
) {

    private val crashReporter: CrashReporter
    private var launchesBrowserSwitchAsNewTask: Boolean = false
    private val deviceInspector: DeviceInspector

    // NOTE: this constructor is used to make dependency injection easy
    internal constructor(params: BraintreeClientParams) : this(
        applicationContext = params.applicationContext,
        integrationType = params.integrationType,
        sessionId = params.sessionId,
        authorizationLoader = params.authorizationLoader,
        analyticsClient = params.analyticsClient,
        httpClient = params.httpClient,
        graphQLClient = params.graphQLClient,
        browserSwitchClient = params.browserSwitchClient,
        configurationLoader = params.configurationLoader,
        manifestValidator = params.manifestValidator,
        returnUrlScheme = params.returnUrlScheme,
        braintreeDeepLinkReturnUrlScheme = params.braintreeReturnUrlScheme,
        appLinkReturnUri = params.appLinkReturnUri
    )

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(options: BraintreeOptions) : this(BraintreeClientParams(options))

    /**
     * Create a new instance of [BraintreeClient] using a tokenization key or client token and a
     * custom url scheme.
     *
     *
     * This constructor should only be used for applications with multiple activities and multiple
     * supported return url schemes. This can be helpful for integrations using Drop-in and
     * BraintreeClient to avoid deep linking collisions, since Drop-in uses the same custom url
     * scheme as the default BraintreeClient constructor.
     *
     * @param context         Android Context
     * @param authorization   The tokenization key or client token to use. If an invalid
     * authorization is provided, a [BraintreeException] will be returned via callback.
     * @param returnUrlScheme A custom return url to use for browser and app switching
     * @param appLinkReturnUri A [Uri] containing the Android App Link website associated with your
     * application to be used to return to your app from browser or app switch based payment flows.
     * This Android App Link will only be used for payment flows where `useAppLinkReturn` is set to
     * `true`.
     */
    @JvmOverloads
    constructor (
        context: Context,
        authorization: String,
        returnUrlScheme: String? = null,
        appLinkReturnUri: Uri? = null,
    ) : this(
        BraintreeOptions(
            context = context,
            initialAuthString = authorization,
            returnUrlScheme = returnUrlScheme,
            appLinkReturnUri = appLinkReturnUri
        )
    )

    /**
     * Create a new instance of [BraintreeClient] using a [ClientTokenProvider] and a custom url
     * scheme.
     *
     *
     * This constructor should only be used for applications with multiple activities and multiple
     * supported return url schemes. This can be helpful for integrations using Drop-in and
     * BraintreeClient to avoid deep linking collisions, since Drop-in uses the same custom url
     * scheme as the default BraintreeClient constructor.
     *
     * @param context             Android Context
     * @param clientTokenProvider An implementation of [ClientTokenProvider] that [BraintreeClient]
     * will use to fetch a client token on demand.
     * @param returnUrlScheme     A custom return url to use for browser and app switching
     * @param appLinkReturnUri    A [Uri] containing the Android App Link to use for app switching
     */
    @JvmOverloads
    constructor(
        context: Context,
        clientTokenProvider: ClientTokenProvider,
        returnUrlScheme: String? = null,
        appLinkReturnUri: Uri? = null,
    ) : this(
        BraintreeOptions(
            context = context,
            clientTokenProvider = clientTokenProvider,
            returnUrlScheme = returnUrlScheme,
            appLinkReturnUri = appLinkReturnUri
        )
    )

    init {
        // NEXT MAJOR VERSION: CrashReporter isn't a part of BraintreeClientParams
        // because it requires a reference to BraintreeClient. This is a design flaw that creates
        // a circular reference. We should consider if we need CrashReporter anymore since
        // merchants already have access to Crash statistics via GooglePlay. We also have crash
        // statistics access via the sdk console
        crashReporter = CrashReporter(this)
        crashReporter.start()
        deviceInspector = DeviceInspector()
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback [ConfigurationCallback]
     */
    open fun getConfiguration(callback: ConfigurationCallback) {
        getAuthorization { authorization, authError ->
            if (authorization != null) {
                configurationLoader.loadConfiguration(authorization) { configuration, configError, timing ->
                    if (configuration != null) {
                        callback.onResult(configuration, null)
                    } else {
                        callback.onResult(null, configError)
                    }
                    timing?.let { sendAnalyticsTimingEvent("v1/configuration", it) }
                }
            } else {
                callback.onResult(null, authError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getAuthorization(callback: AuthorizationCallback) {
        authorizationLoader.loadAuthorization(callback)
    }

    /**
     * @suppress
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendAnalyticsEvent(
        eventName: String,
        params: AnalyticsEventParams = AnalyticsEventParams()
    ) {
        getAuthorization { authorization, _ ->
            if (authorization != null) {
                getConfiguration { configuration, _ ->
                    val isVenmoInstalled = deviceInspector.isVenmoInstalled(applicationContext)
                    val event = AnalyticsEvent(
                        eventName,
                        params.payPalContextId,
                        params.linkType,
                        isVenmoInstalled,
                        params.isVaultRequest,
                        params.startTime,
                        params.endTime,
                        params.endpoint
                    )
                    sendAnalyticsEvent(event, configuration, authorization)
                }
            }
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
                sessionId,
                integrationType,
                authorization
            )
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendGET(url: String, responseCallback: HttpResponseCallback) {
        getAuthorization { authorization, authError ->
            if (authorization != null) {
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
            } else {
                responseCallback.onResult(null, authError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmOverloads
    fun sendPOST(
        url: String,
        data: String,
        additionalHeaders: Map<String, String> = emptyMap(),
        responseCallback: HttpResponseCallback,
    ) {
        getAuthorization { authorization, authError ->
            if (authorization != null) {
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
            } else {
                responseCallback.onResult(null, authError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendGraphQLPOST(json: JSONObject?, responseCallback: HttpResponseCallback) {
        getAuthorization { authorization, authError ->
            if (authorization != null) {
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
            } else {
                responseCallback.onResult(null, authError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(BrowserSwitchException::class)
    fun startBrowserSwitch(
        activity: FragmentActivity?,
        browserSwitchOptions: BrowserSwitchOptions?
    ) {
        if (activity != null && browserSwitchOptions != null) {
            browserSwitchClient.start(activity, browserSwitchOptions)
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getBrowserSwitchResult(activity: FragmentActivity): BrowserSwitchResult? =
        browserSwitchClient.getResult(activity)

    /**
     * Deliver a browser switch result from an Activity's pending deep link intent url.
     * If [BraintreeClient.launchesBrowserSwitchAsNewTask] is set to true,
     * use [BraintreeClient.deliverBrowserSwitchResultFromNewTask] instead.
     *
     * @param activity
     * @return [BrowserSwitchResult]
     */
    open fun deliverBrowserSwitchResult(activity: FragmentActivity): BrowserSwitchResult? {
        return browserSwitchClient.deliverResult(activity)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getBrowserSwitchResultFromNewTask(context: Context): BrowserSwitchResult? {
        return browserSwitchClient.getResultFromCache(context)
    }

    /**
     * Deliver pending browser switch result received by [BraintreeDeepLinkActivity] when
     * [BraintreeClient.launchesBrowserSwitchAsNewTask] is set to true.
     *
     * @param context
     * @return [BrowserSwitchResult]
     */
    open fun deliverBrowserSwitchResultFromNewTask(context: Context): BrowserSwitchResult? {
        return browserSwitchClient.deliverResultFromCache(context)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun parseBrowserSwitchResult(context: Context, requestCode: Int, intent: Intent?) =
        browserSwitchClient.parseResult(context, requestCode, intent)

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun clearActiveBrowserSwitchRequests(context: Context) =
        browserSwitchClient.clearActiveRequests(context)

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(BrowserSwitchException::class)
    fun assertCanPerformBrowserSwitch(
        activity: FragmentActivity?,
        @BraintreeRequestCodes requestCode: Int
    ) {
        // url used to see if the application is able to open an https url e.g. web browser
        val url = Uri.parse("https://braintreepayments.com")
        val returnUrlScheme = getReturnUrlScheme()
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(url)
            .appLinkUri(appLinkReturnUri)
            .returnUrlScheme(returnUrlScheme)
            .requestCode(requestCode)
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, browserSwitchOptions)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun <T> getManifestActivityInfo(klass: Class<T>?): ActivityInfo? {
        return manifestValidator.getActivityInfo(applicationContext, klass)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun reportCrash() = authorizationLoader.authorizationFromCache?.let { authorization ->
        getConfiguration { configuration, _ ->
            analyticsClient.reportCrash(
                applicationContext,
                configuration,
                sessionId,
                integrationType,
                authorization
            )
        }
    }

    /**
     * For clients using a [ClientTokenProvider], call this method to invalidate the existing,
     * cached client token. A new client token will be fetched by the SDK when it is needed.
     *
     * For clients not using a [ClientTokenProvider], this method does nothing.
     */
    open fun invalidateClientToken() {
        authorizationLoader.invalidateClientToken()
    }

    // NEXT MAJOR VERSION: Make launches browser switch as new task a property of `BraintreeOptions`
    fun launchesBrowserSwitchAsNewTask(): Boolean {
        return launchesBrowserSwitchAsNewTask
    }

    /**
     * Set this property to true to allow the SDK to handle deep links on behalf of the host
     * application for browser switched flows.
     *
     * For web payment flows, this means launching the browser in a task separate from the calling
     * activity.
     *
     * NOTE: When this property is set to true, all custom url schemes set in [BraintreeClient]
     * constructors will be ignored.
     *
     * @param launchesBrowserSwitchAsNewTask set to true to allow the SDK to capture deep links.
     * This value is false by default.
     */
    open fun launchesBrowserSwitchAsNewTask(launchesBrowserSwitchAsNewTask: Boolean) {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask
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
}
