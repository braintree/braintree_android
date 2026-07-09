package com.braintreepayments.api.paypal

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Result of the PayPal web flow received from [PayPalLauncher.handleReturnToApp].
 */
sealed class PayPalPaymentAuthResult {

    /**
     * A successful result that should be passed to [PayPalClient.tokenize] to complete the flow.
     *
     * Three internal construction paths exist:
     * - URL return: wraps a [BrowserSwitchFinalResult.Success] from the normal browser switch flow
     * - Auto-link resolved: carries a [autoLinkNonce] already tokenized by the process-level
     *   foreground trigger ([AppForegroundDetector]); [PayPalClient.tokenize] returns it directly.
     * - Auto-link pending: [autoLinkPending] is true when the App Link return failed but a stored
     *   billing agreement session exists. [PayPalClient.tokenize] will tokenize the BA token
     *   directly with BTGW.
     */
    class Success private constructor(
        internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success?,
        internal val autoLinkNonce: PayPalAccountNonce?,
        internal val autoLinkPending: Boolean
    ) : PayPalPaymentAuthResult() {

        /** URL return path — nonce will be resolved in [PayPalClient.tokenize]. */
        internal constructor(browserSwitchSuccess: BrowserSwitchFinalResult.Success) :
            this(browserSwitchSuccess, null, false)

        /** Auto-link resolved path — nonce already tokenized by the foreground trigger. */
        internal constructor(autoLinkNonce: PayPalAccountNonce) :
            this(null, autoLinkNonce, false)

        /** Auto-link pending path — [PayPalClient.tokenize] will tokenize the stored BA token. */
        internal constructor(autoLinkPending: Boolean) :
            this(null, null, autoLinkPending)
    }

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure internal constructor(val error: Exception) : PayPalPaymentAuthResult()

    /**
     * If no matching result can be found for the [PayPalPendingRequest.Started] passed to
     * [PayPalLauncher.handleReturnToApp]. This is expected if the user closed the
     * browser to cancel the payment flow, or returns to the app without completing the
     * authentication flow.
     */
    data object NoResult : PayPalPaymentAuthResult()
}
