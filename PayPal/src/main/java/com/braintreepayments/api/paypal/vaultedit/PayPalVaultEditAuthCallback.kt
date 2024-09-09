package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving result of [PayPalClient.createEditAuthRequest].
 */
@ExperimentalBetaApi
fun interface PayPalVaultEditAuthCallback {

    /**
     * @param payPalVaultEditRequest a request used to launch the PayPal web authentication flow
     */
    fun onPayPalVaultEditAuthRequest(payPalVaultEditRequest: PayPalVaultEditAuthRequest)
}
