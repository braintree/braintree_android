package com.braintreepayments.api.paypal

/**
 * Result of the PayPal web flow received from [PayPalLauncher.handleReturnToAppFromBrowser].
 */
sealed class PayPalPaymentAuthResult {

    /**
     * A successful result that should be passed to [PayPalClient.tokenize] to complete the flow
     */
    class Success(val paymentAuthInfo: PayPalPaymentAuthResultInfo) : PayPalPaymentAuthResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure(val error: Exception) : PayPalPaymentAuthResult()

    /**
     * If no matching result can be found for the [PayPalPendingRequest.Started] passed to
     * [PayPalLauncher.handleReturnToAppFromBrowser]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    object NoResult : PayPalPaymentAuthResult()
}