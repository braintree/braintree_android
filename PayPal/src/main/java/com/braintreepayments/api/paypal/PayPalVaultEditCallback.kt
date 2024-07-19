package com.braintreepayments.api.paypal

import com.braintreepayments.api.ExperimentalBetaApi

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
