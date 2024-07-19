package com.braintreepayments.api.paypal

import com.braintreepayments.api.ExperimentalBetaApi

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