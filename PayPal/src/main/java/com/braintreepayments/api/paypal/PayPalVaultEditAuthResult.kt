package com.braintreepayments.api.paypal

/**
 * Result of the PayPal edit vault flow received from [PayPalLauncher.handleReturnToAppFromBrowser].
 */
sealed class PayPalVaultEditAuthResult {

    /**
     * A successful result that should be passed to [PayPalClient.tokenize] to complete the flow
     */
    class Success internal constructor() : PayPalVaultEditAuthResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditAuthResult()

    /**
     * If no matching result can be found for the [PayPalPendingRequest.Started] passed to
     * [PayPalLauncher.handleReturnToAppFromBrowser]. This is expected if the user closed the
     * browser to cancel the edit vault flow, or returns to the app without completing the
     * authentication flow.
     */
    object NoResult: PayPalVaultEditAuthResult()
}