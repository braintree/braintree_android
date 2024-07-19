package com.braintreepayments.api.paypal

/**
 * Callback for receiving result of [PayPalClient.edit].
 */
fun interface PayPalVaultEditCallback {

    /**
     * @param payPalVaultEditResult a success, failure, or cancel result from the PayPal vault edit
     * flow
     */
    fun onPayPalVaultEditResult(payPalVaultEditResult: PayPalVaultEditResult)
}
