package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalPaymentAuthRequestParams @JvmOverloads internal constructor(
    private val payPalRequest: PayPalRequest,
    var browserSwitchOptions: BrowserSwitchOptions?,
    var approvalUrl: String? = null,
    val clientMetadataId: String? = null,
    val contextId: String? = null,
    val successUrl: String? = null,
) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val intent: PayPalPaymentIntent?
        get() = if (payPalRequest is PayPalCheckoutRequest) payPalRequest.intent else null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isVaultRequest: Boolean
        get() = payPalRequest is PayPalVaultRequest

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val merchantAccountId: String?
        get() = payPalRequest.merchantAccountId

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val fundingSource: String
        get() = payPalRequest.getFundingSource().value

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isPurchase: Boolean
        get() = payPalRequest is PayPalCheckoutRequest && payPalRequest.amount.isNotEmpty()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isBillingAgreement: Boolean
        get() = payPalRequest is PayPalCheckoutRequest && payPalRequest.shouldRequestBillingAgreement
}
