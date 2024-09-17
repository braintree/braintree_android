package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Result of the SEPA Direct Debit web flow received from [SEPADirectDebitLauncher.handleReturnToApp].
 */
sealed class SEPADirectDebitPaymentAuthResult {

    /**
     * A successful result that should be passed to [SEPADirectDebitClient.tokenize] to complete the flow
     */
    class Success internal constructor(
        internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success
    ) : SEPADirectDebitPaymentAuthResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure internal constructor(val error: Exception) : SEPADirectDebitPaymentAuthResult()

    /**
     * If no matching result can be found for the [SEPADirectDebitPendingRequest.Started] passed to
     * [SEPADirectDebitLauncher.handleReturnToApp]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    data object NoResult : SEPADirectDebitPaymentAuthResult()
}
