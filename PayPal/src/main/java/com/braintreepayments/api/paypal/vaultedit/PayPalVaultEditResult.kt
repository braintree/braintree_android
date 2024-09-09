package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {

    /**
     * The PayPal vault edit flow completed successfully.
     *
     * @property riskCorrelationId This ID is used to link subsequent retry attempts if payment is declined
     */
    class Success internal constructor(
        val riskCorrelationId: String,
    ) : PayPalVaultEditResult()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure internal constructor(
        val riskCorrelationId: String,
        val error: Exception
    ) : PayPalVaultEditResult()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    class Cancel internal constructor(
        val riskCorrelationIdZ: String
    ) : PayPalVaultEditResult()
}
