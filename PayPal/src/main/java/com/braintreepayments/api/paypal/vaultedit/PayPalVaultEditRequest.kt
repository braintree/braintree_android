package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Request containing details for the Edit FI flow.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 *
 * @property editPayPalVaultId PayPal vault ID to edit
 */
@ExperimentalBetaApi
open class PayPalVaultEditRequest
@JvmOverloads constructor(
    open val editPayPalVaultId: String,
    open val hasUserLocationConsent: Boolean
) {
    open val hermesPath: String = "v1/paypal_hermes/generate_edit_fi_url"
}
