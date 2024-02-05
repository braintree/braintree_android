package com.braintreepayments.api

import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * Responsible for launching a SEPA mandate in a web browser
 */
class SEPADirectDebitLauncher internal constructor(private val browserSwitchClient: BrowserSwitchClient) {
    /**
     * Used to launch the SEPA mandate in a web browser and deliver results to your Activity
     */
    constructor() : this(BrowserSwitchClient())

    /**
     * Launches the SEPA mandate by switching to a web browser for user authentication
     *
     * @param activity       an Android [FragmentActivity]
     * @param paymentAuthRequest the result of the SEPA mandate received from invoking
     * [SEPADirectDebitClient.createPaymentAuthRequest]
     * @return [SEPADirectDebitPendingRequest] a [SEPADirectDebitPendingRequest.Started]
     * should be stored to complete the flow upon return to app in
     * [SEPADirectDebitLauncher.handleReturnToAppFromBrowser],
     * or a [SEPADirectDebitPendingRequest.Failure] with an error if the SEPA flow was unable
     * to be launched in a browser.
     */
    fun launch(
        activity: ComponentActivity,
        paymentAuthRequest: SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
    ): SEPADirectDebitPendingRequest {
        val params = paymentAuthRequest.requestParams
        val browserSwitchPendingRequest =
            browserSwitchClient.start(activity, params.browserSwitchOptions)
        return when (browserSwitchPendingRequest) {
            is BrowserSwitchPendingRequest.Started ->
                SEPADirectDebitPendingRequest.Started(browserSwitchPendingRequest)
            is BrowserSwitchPendingRequest.Failure ->
                SEPADirectDebitPendingRequest.Failure(browserSwitchPendingRequest.cause)
        }
    }

    /**
     * Captures and delivers the result of the browser-based SEPA mandate flow.
     *
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke [SEPADirectDebitLauncher.launch].
     *
     * If the Activity used to launch the SEPA mandate is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the [SEPADirectDebitPendingRequest.Started] stored after successfully
     * invoking [SEPADirectDebitLauncher.launch]
     * @param intent  the intent to return to your application containing a deep link result from
     * the SEPA mandate flow
     * @return a [SEPADirectDebitPaymentAuthResult] that should be passed to
     * [SEPADirectDebitClient.tokenize]
     * to complete the flow.
     */
    fun handleReturnToAppFromBrowser(
        pendingRequest: SEPADirectDebitPendingRequest.Started,
        intent: Intent
    ): SEPADirectDebitPaymentAuthResult? {
        val result = browserSwitchClient.parseResult(pendingRequest.request, intent)
        return result?.let { SEPADirectDebitPaymentAuthResult(it) }
    }
}
