package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
})
public @interface PaymentMethodType {
    int UNKNOWN = -1;
    int CARD = 0;
    int PAYPAL = 3;
    int VENMO = 2;
    int VISA_CHECKOUT = 7;

    /*
    int GOOGLE_PAY = 5;
    int LOCAL_PAYMENT = 6;
     */
}
