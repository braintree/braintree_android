package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Initializes a PayPal Edit Request for the edit funding instrument flow
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPayPalVaultId The `edit_paypal_vault_id` returned from the server side requests,
 * `gateway.payment_method.find("payment_method_token")` or `gateway.customer.find("customer_id")`
 * @property riskCorrelationID Unique id for each transaction used in subsequent retry in case of failure
 */
@ExperimentalBetaApi
data class PayPalVaultErrorHandlingEditRequest(
    override val editPayPalVaultId: String,
    val riskCorrelationId: String,
): PayPalVaultEditAuthRequest(editPayPalVaultId) {
    override val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"
}
