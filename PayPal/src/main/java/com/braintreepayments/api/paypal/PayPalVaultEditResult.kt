package com.braintreepayments.api.paypal

import com.braintreepayments.api.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {

    /**
     * The PayPal vault edit flow completed successfully.
     */
    @Suppress("EmptyDefaultConstructor")
    class Success(
        // TODO: add FI details
    ) : PayPalVaultEditResult()

    /**
     * There was an [error] in the PayPal vault edit flow.
     */
    class Failure(val error: Exception) : PayPalVaultEditResult()

    /**
     * The user canceled the PayPal vault edit flow.
     */
    object Cancel : PayPalVaultEditResult()
}
