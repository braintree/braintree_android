package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving Edit results
 */
@ExperimentalBetaApi
fun interface PayPalVaultEditCallback {

    /**
     * @param payPalVaultEditAuthRequest a success, failure, or cancel result from the PayPal vault edit
     * flow
     */
    fun onPayPalVaultEditResult(payPalVaultEditAuthRequest: PayPalVaultEditAuthRequest)
}
