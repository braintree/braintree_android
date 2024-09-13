package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalPaymentAuthRequestParams @JvmOverloads internal constructor(
    private val payPalRequest: PayPalRequest,
    var browserSwitchOptions: BrowserSwitchOptions?,
    val approvalUrl: String? = null,
    val clientMetadataId: String? = null,
    val pairingId: String? = null,
    val successUrl: String? = null,
) {

    val intent: PayPalPaymentIntent?
        get() = if (payPalRequest is PayPalCheckoutRequest) payPalRequest.intent else null

    val isBillingAgreement: Boolean
        get() = payPalRequest is PayPalVaultRequest

    val merchantAccountId: String?
        get() = payPalRequest.merchantAccountId
}
