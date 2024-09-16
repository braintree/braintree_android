package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Request containing details for the Edit FI flow.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPayPalVaultId PayPal vault ID to edit
 * @property merchantAccountId optional ID of the merchant account; if one is not provided the default will be used
 */
@ExperimentalBetaApi
data class PayPalVaultEditRequest(
    val editPayPalVaultId: String,
    val riskCorrelationId: String? = null
) {
    val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"
}



