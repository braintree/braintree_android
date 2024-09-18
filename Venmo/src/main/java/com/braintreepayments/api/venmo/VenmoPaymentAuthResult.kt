package com.braintreepayments.api.venmo

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Result of the Venmo flow received from [VenmoLauncher.handleReturnToApp].
 */
sealed class VenmoPaymentAuthResult {

    /**
     * A successful result that should be passed to [VenmoClient.tokenize] to complete the flow
     */
    class Success internal constructor(
        internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success
    ) : VenmoPaymentAuthResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure internal constructor(val error: Exception) : VenmoPaymentAuthResult()

    /**
     * If no matching result can be found for the [VenmoPendingRequest.Started] passed to
     * [VenmoLauncher.handleReturnToApp]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    data object NoResult : VenmoPaymentAuthResult()
}
