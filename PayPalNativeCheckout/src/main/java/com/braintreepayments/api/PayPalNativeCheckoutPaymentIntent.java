package com.braintreepayments.api;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The payment intent in the PayPal Checkout flow
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({PayPalNativeCheckoutPaymentIntent.ORDER, PayPalNativeCheckoutPaymentIntent.SALE, PayPalNativeCheckoutPaymentIntent.AUTHORIZE})
public @interface PayPalNativeCheckoutPaymentIntent {

    /**
     * Payment intent to create an order
     */
    String ORDER = "order";

    /**
     * Payment intent for immediate payment
     */
    String SALE = "sale";

    /**
     * Payment intent to authorize a payment for capture later
     */
    String AUTHORIZE = "authorize";
}
