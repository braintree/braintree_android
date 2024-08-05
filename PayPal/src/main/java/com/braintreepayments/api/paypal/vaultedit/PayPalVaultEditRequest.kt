package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.ExperimentalBetaApi

/**
 * Request containing details for the Edit FI flow.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPaypalVaultId PayPal vault ID to edit
 * @property merchantAccountId optional ID of the merchant account
 */
@ExperimentalBetaApi
data class PayPalVaultEditRequest(
    val editPaypalVaultId: String,
    val merchantAccountId: String? = null,
)
