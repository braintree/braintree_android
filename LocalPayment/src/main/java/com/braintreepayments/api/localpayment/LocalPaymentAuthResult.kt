package com.braintreepayments.api.localpayment

/**
 * Result of the local payment web flow received from [LocalPaymentLauncher.handleReturnToAppFromBrowser].
 */
sealed class LocalPaymentAuthResult {

    /**
     * A successful result that should be passed to [LocalPaymentClient.tokenize] to complete the flow
     */
    class Success(val paymentAuthInfo: LocalPaymentAuthResultInfo) : LocalPaymentAuthResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure(val error: Exception) : LocalPaymentAuthResult()

    /**
     * If no matching result can be found for the [LocalPaymentPendingRequest.Started] passed to
     * [LocalPaymentLauncher.handleReturnToAppFromBrowser]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    object NoResult : LocalPaymentAuthResult()
}
