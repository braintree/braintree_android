package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Initializes a PayPal Edit Request for the edit funding instrument flow
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property hasUserLocationConsent is a required parameter that informs the SDK if your application
 * has obtained consent from the user to collect location data in compliance with
 * [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk
 * Management.
 * @see [User Data policies for the Google Play Developer Program ](https://support.google.com/googleplay/android-developer/answer/10144311.personal-sensitive)
 *
 * @see [Examples of prominent in-app disclosures](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en.Prominent%20in-app%20disclosure)
 *
 * @property editPayPalVaultId The `edit_paypal_vault_id` returned from the server side requests,
 * `gateway.payment_method.find("payment_method_token")` or `gateway.customer.find("customer_id")`
 * @property riskCorrelationID Unique id for each transaction used in subsequent retry in case of
 * failure
 */
@ExperimentalBetaApi
data class PayPalVaultErrorHandlingEditRequest
@JvmOverloads constructor(
    override val hasUserLocationConsent: Boolean,
    override val editPayPalVaultId: String,
    val riskCorrelationId: String,
) : PayPalVaultEditRequest(hasUserLocationConsent, editPayPalVaultId) {
    override val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"
}
