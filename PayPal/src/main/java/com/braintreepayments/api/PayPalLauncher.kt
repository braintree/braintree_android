package com.braintreepayments.api

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting

/**
 * Responsible for launching PayPal user authentication in a web browser
 */
class PayPalLauncher @VisibleForTesting internal constructor(private val browserSwitchClient: BrowserSwitchClient) {
    /**
     * Used to launch the PayPal flow in a web browser and deliver results to your Activity
     */
    constructor() : this(BrowserSwitchClient())

    /**
     * Launches the PayPal flow by switching to a web browser for user authentication
     *
     * @param activity the Android Activity from which you will launch the web browser
     * @param paymentAuthRequest a [PayPalPaymentAuthRequest.ReadyToLaunch] received from
     * calling [PayPalClient.createPaymentAuthRequest]
     * @return [PayPalPendingRequest] a [PayPalPendingRequest.Started] should be stored
     * to complete the flow upon return to app in
     * [PayPalLauncher.handleReturnToAppFromBrowser],
     * or a [PayPalPendingRequest.Failure] with an error if the PayPal flow was unable to be
     * launched in a browser.
     */
    fun launch(
        activity: ComponentActivity,
        paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
    ): PayPalPendingRequest {
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return PayPalPendingRequest.Failure(manifestInvalidError)
        }
        val request = browserSwitchClient.start(
            activity,
            paymentAuthRequest.requestParams.browserSwitchOptions
        )
        return when (request) {
            is BrowserSwitchPendingRequest.Failure -> PayPalPendingRequest.Failure(request.cause)
            is BrowserSwitchPendingRequest.Started -> PayPalPendingRequest.Started(request)
        }
    }

    /**
     * Captures and delivers the result of a the browser-based PayPal authentication flow.
     *
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke
     * [PayPalLauncher.launch].
     *
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the [PayPalPendingRequest.Started] stored after successfully
     * invoking [PayPalLauncher.launch]
     * @param intent         the intent to return to your application containing a deep link result
     * from the PayPal browser flow
     * @return a [PayPalPaymentAuthResult] that should be passed to
     * [PayPalClient.tokenize] to complete
     * the PayPal payment flow. Returns null if the user closed the browser to cancel the payment
     * flow, or returned to the app without completing the PayPal authentication flow.
     */
    fun handleReturnToAppFromBrowser(
        pendingRequest: PayPalPendingRequest.Started, intent: Intent
    ): PayPalPaymentAuthResult? {
        browserSwitchClient.parseResult(pendingRequest.request, intent)?.let {
           return PayPalPaymentAuthResult(it)
        }
        return null
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: ComponentActivity,
        params: PayPalPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
    }

    companion object {
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
