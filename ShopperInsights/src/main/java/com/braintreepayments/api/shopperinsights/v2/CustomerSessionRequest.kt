package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Request for creating a customer session.
 *
 * @property hashedEmail Customer email address hashed via SHA256.
 * @property hashedPhoneNumber Customer phone number hashed via SHA256.
 * @property payPalAppInstalled If the PayPal app is installed on the device.
 * @property venmoAppInstalled If the Venmo app is installed on the device.
 * @property purchaseUnits List of purchase units containing the amount and currency code.
 *
 * Warning: This feature is in beta. It's public API may change or be removed in future releases.
 */
@ExperimentalBetaApi
data class CustomerSessionRequest(
    var hashedEmail: String? = null,
    var hashedPhoneNumber: String? = null,
    var payPalAppInstalled: Boolean? = null,
    var venmoAppInstalled: Boolean? = null,
    var purchaseUnits: List<PurchaseUnit>? = null,
)
