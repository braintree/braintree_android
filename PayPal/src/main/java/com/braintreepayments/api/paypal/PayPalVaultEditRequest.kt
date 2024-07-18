package com.braintreepayments.api.paypal

import com.braintreepayments.api.ExperimentalBetaApi

/**
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
@ExperimentalBetaApi
data class PayPalVaultEditRequest(
    val encryptedBillingAgreementID: String
)
