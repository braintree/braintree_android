package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject

/**
 * The funding instrument details for a vaulted PayPal payment method.
 *
 * @property paymentMethods The funding instruments returned for the buyer's PayPal account. Entries without a type or
 * label are dropped, so an empty list means there is no instrument to display.
 * @property payer The buyer's PayPal account, when PayPal returns one with an email.
 */
internal data class PayPalSavedPaymentMethodSummary(
    val paymentMethods: List<PayPalSavedPaymentMethod>,
    val payer: PayPalPayer?
) {

    companion object {
        private const val PAYMENT_METHODS_KEY = "paymentMethods"
        private const val PAYER_KEY = "payer"

        /**
         * Parses the `paypalFundingInstrumentDetails` field of a `PaypalFundingInstrumentDetails` response.
         * Returns `null` when the field is not an object.
         */
        fun fromJson(json: JSONObject?): PayPalSavedPaymentMethodSummary? {
            if (json == null) return null

            val paymentMethods = json.optJSONArray(PAYMENT_METHODS_KEY)?.let { array ->
                (0 until array.length()).mapNotNull { PayPalSavedPaymentMethod.fromJson(array.optJSONObject(it)) }
            }.orEmpty()

            return PayPalSavedPaymentMethodSummary(
                paymentMethods = paymentMethods,
                payer = PayPalPayer.fromJson(json.optJSONObject(PAYER_KEY))
            )
        }
    }
}
