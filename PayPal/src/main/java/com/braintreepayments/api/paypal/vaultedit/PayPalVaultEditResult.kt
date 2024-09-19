package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {
    /**
     * A successful result
     */
    class Success internal constructor(val riskCorrelationId: String) : PayPalVaultEditResult()

    /**
     * The browser switch failed.
     * @property [error] Error detailing the reason for the browser switch failure.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditResult()

    /**
     * If no matching result can be found for the [PayPalPendingRequest.Started] passed to
     * [PayPalLauncher.handleReturnToAppFromBrowser]. This is expected if the user closed the
     * browser to cancel the edit vault flow, or returns to the app without completing the
     * authentication flow.
     */
    object Cancel : PayPalVaultEditResult()
}
