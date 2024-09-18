package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving result of [PayPalClient.edit].
 */
@ExperimentalBetaApi
fun interface PayPalVaultEditCallback {

    /**
     * @param payPalVaultEditResult a success, failure, or cancel result from the PayPal vault edit
     * flow
     */
    fun onPayPalVaultEditResult(payPalVaultEditResult: PayPalVaultEditResult)
}
