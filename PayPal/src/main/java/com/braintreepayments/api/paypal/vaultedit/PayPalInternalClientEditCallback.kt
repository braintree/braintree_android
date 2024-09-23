package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving result for edit internally.
 */
@ExperimentalBetaApi
internal fun interface PayPalInternalClientEditCallback {

    fun onPayPalVaultEditResult(
        payPalVaultEditAuthRequestParams: PayPalVaultEditAuthRequestParams?,
        error: Exception?
    )
}
