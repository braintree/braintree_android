package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.ExperimentalBetaApi

/**
 * Request containing details for the Edit FI flow.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPaypalVaultId PayPal vault ID to edit
 * @property merchantAccountId optional ID of the merchant account; if one is not provided the default will be used
 */
@ExperimentalBetaApi
data class PayPalVaultEditRequest(
    val editPaypalVaultId: String,
    val merchantAccountID: String?,
    val merchantAccountId: String? = null,
    var correlationID: String?
) {
    fun parameters(): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        parameters["edit_paypal_vault_id"] = editPaypalVaultId

        if (correlationID != null) {
            parameters["correlation_id"] = correlationID!!
        }

        val callbackURLScheme = "sdk.ios.braintree"
        val callbackURLHostAndPath = "onetouch/v1"

        parameters["return_url"] = "$callbackURLScheme://$callbackURLHostAndPath/success"
        parameters["cancel_url"] = "$callbackURLScheme://$callbackURLHostAndPath/cancel"

        return parameters
    }
}

const val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"

// Example usage
@OptIn(ExperimentalBetaApi::class)
val request = PayPalVaultEditRequest(
    editPaypalVaultId = "exampleID",
    merchantAccountID = "exampleMerchantAccountID",
    correlationID = "exampleCorrelationID"
)

@OptIn(ExperimentalBetaApi::class)
val params = request.parameters()