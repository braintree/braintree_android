package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving result for edit internally.
 */
@ExperimentalBetaApi
internal fun interface PayPalInternalClientEditCallback {

    /**
     * @param internalPayPalVaultEditAuthRequestå
     * @param error a success, failure, or cancel result internally
     */
    fun onPayPalVaultEditResult(
        payPalVaultEditAuthRequestParams: PayPalVaultEditAuthRequestParams?,
        error: Exception?
    )
}
