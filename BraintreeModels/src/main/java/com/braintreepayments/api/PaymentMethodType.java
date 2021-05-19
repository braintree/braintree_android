package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Enum representing different types of supported Braintree payment methods.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        PaymentMethodType.UNKNOWN,
        PaymentMethodType.CARD,
        PaymentMethodType.PAYPAL,
        PaymentMethodType.VENMO,
        PaymentMethodType.VISA_CHECKOUT,
        PaymentMethodType.GOOGLE_PAY,
        PaymentMethodType.LOCAL_PAYMENT,
        PaymentMethodType.PAYPAL_NATIVE
})
@interface PaymentMethodType {
    int UNKNOWN = -1;
    int CARD = 0;
    int PAYPAL = 1;
    int VENMO = 2;
    int VISA_CHECKOUT = 3;
    int GOOGLE_PAY = 4;
    int LOCAL_PAYMENT = 5;
    int PAYPAL_NATIVE = 6;
}

