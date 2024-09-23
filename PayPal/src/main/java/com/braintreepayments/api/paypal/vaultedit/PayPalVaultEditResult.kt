package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {
    /**
     * The PayPal vault edit flow completed successfully.
     */
    class Success internal constructor(val riskCorrelationId: String) : PayPalVaultEditResult()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditResult()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    object Cancel : PayPalVaultEditResult()
}
