package com.braintreepayments.api

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.IntegrationType.Integration

/**
 * Core Braintree class that handles network requests.
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

    /**
     * @suppress
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val authorization: Authorization,

    private val analyticsClient: AnalyticsClient,
    private val httpClient: BraintreeHttpClient,
    private val graphQLClient: BraintreeGraphQLClient,
    private val browserSwitchClient: BrowserSwitchClient,
    private val configurationLoader: ConfigurationLoader,
    private val manifestValidator: ManifestValidator,
    private val returnUrlScheme: String,
    private val braintreeDeepLinkReturnUrlScheme: String,
) {

    private val crashReporter: CrashReporter
    private var launchesBrowserSwitchAsNewTask: Boolean = false

    // NOTE: this constructor is used to make dependency injection easy
    internal constructor(params: BraintreeClientParams) : this(
        applicationContext = params.applicationContext,
        integrationType = params.integrationType,
        sessionId = params.sessionId,
        authorization = params.authorization,
        analyticsClient = params.analyticsClient,
        httpClient = params.httpClient,
        graphQLClient = params.graphQLClient,
        browserSwitchClient = params.browserSwitchClient,
        configurationLoader = params.configurationLoader,
        manifestValidator = params.manifestValidator,
        returnUrlScheme = params.returnUrlScheme,
        braintreeDeepLinkReturnUrlScheme = params.braintreeReturnUrlScheme
    )

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(context: Context, authorization: String) :
            this(BraintreeOptions(context = context, authorization = Authorization.fromString
                (authorization)))

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(options: BraintreeOptions) : this(BraintreeClientParams(options))

    internal constructor(
        context: Context,
        authorization: Authorization,
        sessionId: String?,
        @Integration integrationType: String
    ) : this(
        BraintreeOptions(
            context = context,
            authorization = authorization,
            sessionId = sessionId,
            integrationType = integrationType,
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
    }

    /**
     * Retrieve Braintree configuration.
     *
     * @param callback [ConfigurationCallback]
     */
    open fun getConfiguration(callback: ConfigurationCallback) {
        if (authorization is InvalidAuthorization) {
            callback.onResult(null, createAuthError())
            return
        }
        configurationLoader.loadConfiguration(authorization) { configuration, configError ->
            if (configuration != null) {
                callback.onResult(configuration, null)
            } else {
                callback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendAnalyticsEvent(eventName: String) {
       sendAnalyticsEvent(eventName, null)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendAnalyticsEvent(eventName: String, payPalContextId: String? = null) {
        getConfiguration { configuration, _ ->
            val event = AnalyticsEvent(eventName, payPalContextId)
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
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                httpClient.get(url, configuration, authorization, responseCallback)
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendPOST(url: String, data: String, responseCallback: HttpResponseCallback) {
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                httpClient.post(
                    url,
                    data,
                    configuration,
                    authorization,
                    responseCallback
                )
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendGraphQLPOST(payload: String?, responseCallback: HttpResponseCallback) {
        if (authorization is InvalidAuthorization) {
            responseCallback.onResult(null, createAuthError())
            return
        }
        getConfiguration { configuration, configError ->
            if (configuration != null) {
                graphQLClient.post(
                    payload,
                    configuration,
                    authorization,
                    responseCallback
                )
            } else {
                responseCallback.onResult(null, configError)
            }
        }
    }

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
    fun reportCrash() =
        getConfiguration { configuration, _ ->
            analyticsClient.reportCrash(
                applicationContext,
                configuration,
                sessionId,
                integrationType,
                authorization
            )
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

    private fun createAuthError(): BraintreeException {
        val clientSDKSetupURL =
            "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization"
        val message = "Valid authorization required. See $clientSDKSetupURL for more info."
        return BraintreeException(message)
    }
}
