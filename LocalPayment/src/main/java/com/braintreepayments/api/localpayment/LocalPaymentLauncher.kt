package com.braintreepayments.api.localpayment

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.BraintreeException

/**
 * Responsible for launching local payment user authentication in a web browser
 */
class LocalPaymentLauncher internal constructor(private val browserSwitchClient: BrowserSwitchClient) {
    /**
     * Used to launch the local payment flow in a web browser and deliver results to your Activity
     */
    constructor() : this(BrowserSwitchClient())

    /**
     * Launches the local payment flow by switching to a web browser for user authentication.
     *
     * @param activity an Android [ComponentActivity]
     * @param localPaymentAuthRequest the payment auth request created in
     * [LocalPaymentClient.createPaymentAuthRequest]
     * @return [LocalPaymentPendingRequest] a [LocalPaymentPendingRequest.Started] should
     * be stored to complete the flow upon return to app in
     * [LocalPaymentLauncher.handleReturnToAppFromBrowser],
     * or a [LocalPaymentPendingRequest.Failure] with an error if the local payment flow was
     * unable to be launched in a browser.
     */
    fun launch(
        activity: ComponentActivity,
        localPaymentAuthRequest: LocalPaymentAuthRequest.ReadyToLaunch
    ): LocalPaymentPendingRequest {
        val params = localPaymentAuthRequest.requestParams
        val browserSwitchPendingRequest =
            params.browserSwitchOptions?.let { browserSwitchClient.start(activity, it) }
        return when (browserSwitchPendingRequest) {
            is BrowserSwitchStartResult.Started -> {
                LocalPaymentPendingRequest.Started(browserSwitchPendingRequest.pendingRequest)
            }

            is BrowserSwitchStartResult.Failure -> {
                LocalPaymentPendingRequest.Failure(browserSwitchPendingRequest.error)
            }

            null -> LocalPaymentPendingRequest.Failure(BraintreeException("An unexpected error occurred"))
        }
    }

    /**
     * Captures and delivers the result of a the browser-based local payment authentication flow.
     *
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke [LocalPaymentLauncher.launch].
     *
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the [LocalPaymentPendingRequest.Started] stored after successfully
     * invoking [LocalPaymentLauncher.launch]
     * @param intent  the intent to return to your application containing a deep link result
     * from the local payment browser flow
     * @return a [LocalPaymentAuthResult.Success] that should be passed to
     * [LocalPaymentClient.tokenize] to complete the flow, or [LocalPaymentAuthResult.NoResult] if
     * the user closed the browser to cancel the payment flow, or returned to the app without
     * completing the authentication flow.
     */
    fun handleReturnToAppFromBrowser(
        pendingRequest: LocalPaymentPendingRequest.Started,
        intent: Intent
    ): LocalPaymentAuthResult {
        return when (val result =
            browserSwitchClient.completeRequest(intent, pendingRequest.pendingRequestString)) {
            is BrowserSwitchFinalResult.Success -> LocalPaymentAuthResult.Success(result)

            is BrowserSwitchFinalResult.Failure -> LocalPaymentAuthResult.Failure(result.error)

            is BrowserSwitchFinalResult.NoResult -> LocalPaymentAuthResult.NoResult
        }
    }
}
