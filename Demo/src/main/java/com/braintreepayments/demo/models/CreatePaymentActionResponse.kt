package com.braintreepayments.demo.models

import com.google.gson.annotations.SerializedName

data class CreatePaymentActionResponse(
    @SerializedName("client_token") val clientToken: String? = null,
    @SerializedName("payment_action") val paymentAction: PaymentAction? = null,
) {
    data class PaymentAction(
        @SerializedName("id") val id: String? = null,
        @SerializedName("status") val status: String? = null,
    )
}
