package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving result of [PayPalClient.edit].
 */
@ExperimentalBetaApi
internal fun interface InternalPayPalVaultEditCallback {

    /**
     * @param internalPayPalVaultEditAuthRequest a success, failure, or cancel result from the PayPal vault edit
     * flow
     */
    fun onPayPalVaultEditResult(internalPayPalVaultEditAuthRequest: InternalPayPalVaultEditAuthRequest)
}

/**
 * Callback for receiving result of [PayPalClient.edit].
 */
@ExperimentalBetaApi
fun interface PayPalVaultEditCallback {

    /**
     * @param internalPayPalVaultEditAuthRequest a success, failure, or cancel result from the PayPal vault edit
     * flow
     */
    fun onPayPalVaultEditResult(payPalVaultEditAuthRequest: PayPalVaultEditAuthRequest)
}