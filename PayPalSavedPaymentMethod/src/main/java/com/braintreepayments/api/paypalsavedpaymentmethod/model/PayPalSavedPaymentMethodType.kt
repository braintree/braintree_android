package com.braintreepayments.api.paypalsavedpaymentmethod.model

/**
 * The kind of funding instrument PayPal will charge.
 */
internal enum class PayPalSavedPaymentMethodType(val rawValue: String) {

    /**
     * A bank account linked to the buyer's PayPal account.
     */
    BANK("BANK"),

    /**
     * A credit or debit card.
     */
    CARD("CARD"),

    /**
     * The buyer's PayPal Credit line.
     */
    PAYPAL_CREDIT("PAYPAL_CREDIT");

    companion object {

        /**
         * Returns `null` when PayPal returns a type this SDK version does not recognize.
         */
        fun fromRawValue(rawValue: String?): PayPalSavedPaymentMethodType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}
