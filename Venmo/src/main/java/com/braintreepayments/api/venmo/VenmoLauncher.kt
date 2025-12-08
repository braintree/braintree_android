package com.braintreepayments.api.venmo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeException

/**
 * Responsible for launching the Venmo app to authenticate users
 */
class VenmoLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient,
    private val venmoRepository: VenmoRepository,
    lazyAnalyticsClient: Lazy<AnalyticsClient>,
) {

    constructor() : this(
        browserSwitchClient = BrowserSwitchClient(),
        venmoRepository = VenmoRepository.instance,
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

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
        activity: Activity,
        paymentAuthRequest: VenmoPaymentAuthRequest.ReadyToLaunch
    ): VenmoPendingRequest {
        analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_STARTED, analyticsEventParams)
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_FAILED, analyticsEventParams)
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return VenmoPendingRequest.Failure(manifestInvalidError)
        }
        val request = browserSwitchClient.start(
            activity,
            paymentAuthRequest.requestParams.browserSwitchOptions
        )
        return when (request) {
            is BrowserSwitchStartResult.Failure -> {
                analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_FAILED, analyticsEventParams)
                VenmoPendingRequest.Failure(request.error)
            }

            is BrowserSwitchStartResult.Started -> {
                analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED, analyticsEventParams)
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
        analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, analyticsEventParams)
        return when (val browserSwitchResult =
            browserSwitchClient.completeRequest(intent, pendingRequest.pendingRequestString)) {
            is BrowserSwitchFinalResult.Success -> {
                analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_SUCCEEDED, analyticsEventParams)
                VenmoPaymentAuthResult.Success(browserSwitchResult)
            }

            is BrowserSwitchFinalResult.Failure -> {
                analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_FAILED, analyticsEventParams)
                VenmoPaymentAuthResult.Failure(browserSwitchResult.error)
            }

            is BrowserSwitchFinalResult.NoResult -> {
                analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, analyticsEventParams)
                VenmoPaymentAuthResult.NoResult
            }
        }
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    fun showVenmoInGooglePlayStore(activity: Activity) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(
            "https://play.google.com/store/apps/details?id=$VENMO_PACKAGE_NAME"
        )
        activity.startActivity(intent)
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: Activity,
        params: VenmoPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
    }

    private val analyticsEventParams by lazy {
        AnalyticsEventParams(appSwitchUrl = venmoRepository.venmoUrl.toString())
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
