package com.braintreepayments.api.venmo

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchPendingRequest
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.core.BraintreeException

/**
 * Responsible for launching the Venmo app to authenticate users
 */
class VenmoLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

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
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return VenmoPendingRequest.Failure(manifestInvalidError)
        }
        val request = browserSwitchClient.start(
            activity,
            paymentAuthRequest.requestParams.browserSwitchOptions
        )
        return when (request) {
            is BrowserSwitchPendingRequest.Failure -> VenmoPendingRequest.Failure(request.cause)
            is BrowserSwitchPendingRequest.Started -> VenmoPendingRequest.Started(request)
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
     * closed the browser to cancel the payment flow, or returned to the app without completing the
     * Venmo authentication flow.
     */
    fun handleReturnToApp(
        pendingRequest: VenmoPendingRequest.Started,
        intent: Intent
    ): VenmoPaymentAuthResult {
        return when (val browserSwitchResult =
            browserSwitchClient.parseResult(pendingRequest.request, intent)) {
            is BrowserSwitchResult.Success -> VenmoPaymentAuthResult.Success(
                VenmoPaymentAuthResultInfo(browserSwitchResult.resultInfo)
            )

            is BrowserSwitchResult.NoResult -> VenmoPaymentAuthResult.NoResult
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

    companion object {
        const val VENMO_PACKAGE_NAME = "com.venmo"
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
