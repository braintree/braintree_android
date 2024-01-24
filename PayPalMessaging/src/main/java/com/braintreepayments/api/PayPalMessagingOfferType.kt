package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType

/**
 * Preferred message offer to display
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 */
enum class PayPalMessagingOfferType {

    /**
     * Pay Later short term installment
     */
    PAY_LATER_SHORT_TERM,

    /**
     * Pay Later long term installments
     */
    PAY_LATER_LONG_TERM,

    /**
     * Pay Later deferred payment
     */
    PAY_LATER_PAY_IN_ONE,

    /**
     * PayPal Credit No Interest
     */
    PAYPAL_CREDIT_NO_INTEREST;

    internal val offerTypeRawValue: PayPalMessageOfferType
        get() = when (this) {
            PAY_LATER_SHORT_TERM -> PayPalMessageOfferType.PAY_LATER_SHORT_TERM
            PAY_LATER_LONG_TERM -> PayPalMessageOfferType.PAY_LATER_LONG_TERM
            PAY_LATER_PAY_IN_ONE -> PayPalMessageOfferType.PAY_LATER_PAY_IN_1
            PAYPAL_CREDIT_NO_INTEREST -> PayPalMessageOfferType.PAYPAL_CREDIT_NO_INTEREST
        }
}
