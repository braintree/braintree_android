package com.braintreepayments.api.sepadirectdebit

/**
 * Result of the SEPA Direct Debit web flow received from [SEPADirectDebitLauncher.handleReturnToAppFromBrowser].
 */
sealed class SEPADirectDebitPaymentAuthResult {

    /**
     * A successful result that should be passed to [SEPADirectDebitClient.tokenize] to complete the flow
     */
    class Success(val paymentAuthInfo: SEPADirectDebitPaymentAuthResultInfo) : SEPADirectDebitPaymentAuthResult()

    /**
     * If no matching result can be found for the [SEPADirectDebitPendingRequest.Started] passed to
     * [SEPADirectDebitLauncher.handleReturnToAppFromBrowser]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    object NoResult : SEPADirectDebitPaymentAuthResult()
}
