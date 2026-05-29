package com.braintreepayments.api.paypal

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Result of the PayPal web flow received from [PayPalLauncher.handleReturnToApp].
 */
sealed class PayPalPaymentAuthResult {

    /**
     * A successful result that should be passed to [PayPalClient.tokenize] to complete the flow.
     *
     * Two internal construction paths exist:
     * - URL return: wraps a [BrowserSwitchFinalResult.Success] from the normal browser switch flow
     * - Auto-link: carries a pre-resolved [PayPalAccountNonce] when the App Link return failed
     *   and the SDK tokenized the BA token directly with BTGW
     */
    class Success private constructor(
        internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success?,
        internal val autoLinkNonce: PayPalAccountNonce?
    ) : PayPalPaymentAuthResult() {

        /** URL return path — nonce will be resolved in [PayPalClient.tokenize]. */
        internal constructor(browserSwitchSuccess: BrowserSwitchFinalResult.Success) :
            this(browserSwitchSuccess, null)

        /** Auto-link path — nonce already resolved via [AutoLinkTokenizeUseCase]. */
        internal constructor(autoLinkNonce: PayPalAccountNonce) :
            this(null, autoLinkNonce)
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
