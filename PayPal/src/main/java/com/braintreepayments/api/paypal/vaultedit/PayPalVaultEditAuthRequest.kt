package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Request containing details for the Edit FI flow.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPayPalVaultId PayPal vault ID to edit
 * @property riskCorrelationId optional ID;
 */
@ExperimentalBetaApi
data class PayPalVaultEditAuthRequest(
    val editPayPalVaultId: String,
    val riskCorrelationId: String? = null
) {
    val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"
}



