package com.braintreepayments.api.venmo

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistry
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.MerchantRepository

/**
 * Responsible for launching the Venmo app to authenticate users
 */
class VenmoLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient,
    private val venmoRepository: VenmoRepository,
    lazyAnalyticsClient: Lazy<AnalyticsClient>,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {
    /**
     * Used to launch the Venmo flow in a web browser and deliver results to your Activity
     * @param caller Optional ActivityResultCaller parameter. If provided, it will be passed to BrowserSwitchClient
     */
    @JvmOverloads
    constructor(caller: ActivityResultCaller? = null) : this(
        browserSwitchClient = if (caller != null) BrowserSwitchClient(caller) else BrowserSwitchClient(),
        venmoRepository = VenmoRepository.instance,
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    /**
     * Used to launch the Venmo flow in a web browser and deliver results. Use it in Compose flows or where
     * ActivityResultCaller is not available.
     * @param registry ActivityResultRegistry parameter. It will be passed to BrowserSwitchClient.
     */
    constructor(registry: ActivityResultRegistry) : this(
        browserSwitchClient = BrowserSwitchClient(registry),
        venmoRepository = VenmoRepository.instance,
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    @Throws(BrowserSwitchException::class)
    fun restorePendingRequest(pendingRequestString: String) {
        browserSwitchClient.restorePendingRequest(pendingRequestString)
    }

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

    private var startedRequest: Pair<String, Authorization>? = null

    /**
     * Launches the Venmo authentication flow by switching to the Venmo app or a mobile browser, if
     * the Venmo app is not installed on the device.
     *
     * @param activity the ComponentActivity to launch the Venmo flow from
     * @param paymentAuthRequest the result of [VenmoClient.createPaymentAuthRequest]
     * @return [VenmoPendingRequest] a [VenmoPendingRequest.Started] should be stored
     * to complete the flow upon return to app in [VenmoLauncher.handleReturnToApp],
     * or a [VenmoPendingRequest.Failure] with an error if the Venmo flow was unable to be
     * launched in the app or in a browser.
     */
    fun launch(
        activity: ComponentActivity,
        paymentAuthRequest: VenmoPaymentAuthRequest.ReadyToLaunch
    ): VenmoPendingRequest {
        val authorization = paymentAuthRequest.requestParams.authorization
        sendEvent(VenmoAnalytics.APP_SWITCH_STARTED, analyticsEventParams, authorization)
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            sendEvent(VenmoAnalytics.APP_SWITCH_FAILED, analyticsEventParams, authorization)
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return VenmoPendingRequest.Failure(manifestInvalidError)
        }
        val request = browserSwitchClient.start(
            activity,
            paymentAuthRequest.requestParams.browserSwitchOptions,
            true
        )
        return when (request) {
            is BrowserSwitchStartResult.Failure -> {
                sendEvent(VenmoAnalytics.APP_SWITCH_FAILED, analyticsEventParams, authorization)
                VenmoPendingRequest.Failure(request.error)
            }

            is BrowserSwitchStartResult.Started -> {
                authorization?.let { startedRequest = request.pendingRequest to it }
                sendEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED, analyticsEventParams, authorization)
                VenmoPendingRequest.Started(request.pendingRequest)
            }
        }
    }

    /**
     * Captures and delivers the result of a the Venmo authentication flow.
     *
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke [VenmoLauncher.launch].
     *
     * If the Activity used to launch the Venmo flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the [VenmoPendingRequest.Started] stored after successfully
     * invoking [VenmoLauncher.launch]
     * @param intent the intent to return to your application containing a deep link result
     * from the Venmo flow
     * @return a [VenmoPaymentAuthResult.Success] that should be passed to [VenmoClient.tokenize]
     * to complete the Venmo payment flow. Returns [VenmoPaymentAuthResult.NoResult] if the user
     * canceled payment flow, or returned to the app without completing the
     * Venmo authentication flow.
     */
    fun handleReturnToApp(
        pendingRequest: VenmoPendingRequest.Started,
        intent: Intent
    ): VenmoPaymentAuthResult {
        val authorization = startedRequest
            ?.takeIf { it.first == pendingRequest.pendingRequestString }
            ?.second
        sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, analyticsEventParams, authorization)
        return when (val browserSwitchResult =
            browserSwitchClient.completeRequest(intent, pendingRequest.pendingRequestString)) {
            is BrowserSwitchFinalResult.Success -> {
                sendEvent(VenmoAnalytics.HANDLE_RETURN_SUCCEEDED, analyticsEventParams, authorization)
                if (authorization != null) startedRequest = null
                VenmoPaymentAuthResult.Success(browserSwitchResult)
            }

            is BrowserSwitchFinalResult.Failure -> {
                sendEvent(VenmoAnalytics.HANDLE_RETURN_FAILED, analyticsEventParams, authorization)
                if (authorization != null) startedRequest = null
                VenmoPaymentAuthResult.Failure(browserSwitchResult.error)
            }

            is BrowserSwitchFinalResult.NoResult -> {
                sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, analyticsEventParams, authorization)
                VenmoPaymentAuthResult.NoResult
            }
        }
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    fun showVenmoInGooglePlayStore(activity: ComponentActivity) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(
            "https://play.google.com/store/apps/details?id=$VENMO_PACKAGE_NAME"
        )
        activity.startActivity(intent)
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: ComponentActivity,
        params: VenmoPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
    }

    private val analyticsEventParams by lazy {
        AnalyticsEventParams(appSwitchUrl = venmoRepository.venmoUrl.toString())
    }

    private fun sendEvent(
        eventName: String,
        params: AnalyticsEventParams = AnalyticsEventParams(),
        authorization: Authorization?
    ) {
        // shortcut: falls back to the newest client's authorization when the pending request's owner
        // is unknown (e.g. after process death); wrong for apps with several authorizations,
        // fix needs the owner carried across process death without persisting a token
        val resolvedAuthorization = authorization ?: merchantRepository.authorizationOrNull ?: return
        analyticsClient.sendEvent(eventName, resolvedAuthorization, params)
    }

    companion object {
        private const val VENMO_PACKAGE_NAME = "com.venmo"
        private fun createBrowserSwitchError(exception: BrowserSwitchException): Exception {
            return BraintreeException(
                "AndroidManifest.xml is incorrectly configured or another app defines the same " +
                    "browser switch url as this app. See https://developer.paypal.com/" +
                    "braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                    "for the correct configuration: " + exception.message
            )
        }
    }
}
