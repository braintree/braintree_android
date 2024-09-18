package com.braintreepayments.api.paypal

/**
 * A pending request for the PayPal web-based authentication flow created by invoking
 * [PayPalLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [PayPalLauncher.handleReturnToAppFromBrowser]
 */
sealed class PayPalPendingRequestEditFi {

    /**
     * A pending request was successfully started.
     *
     * @property pendingRequestString - This String should be stored and passed to
     * [PayPalLauncher.handleReturnToAppFromBrowser].
     */
    class Started(val pendingRequestString: String) : PayPalPendingRequestEditFi()

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalPendingRequestEditFi()
}
