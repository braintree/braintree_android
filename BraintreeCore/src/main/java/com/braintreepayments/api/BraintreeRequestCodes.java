package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Request codes that Braintree uses when communicating between Android Activities and Fragments.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        BraintreeRequestCodes.THREE_D_SECURE,
        BraintreeRequestCodes.VENMO,
        BraintreeRequestCodes.PAYPAL,
        BraintreeRequestCodes.VISA_CHECKOUT,
        BraintreeRequestCodes.GOOGLE_PAY,
        BraintreeRequestCodes.SAMSUNG_PAY,
        BraintreeRequestCodes.LOCAL_PAYMENT,
        BraintreeRequestCodes.SEPA,
})
public @interface BraintreeRequestCodes {
    int THREE_D_SECURE = 13487;
    int VENMO = 13488;
    int PAYPAL = 13591;
    int VISA_CHECKOUT = 13592;
    int GOOGLE_PAY = 13593;
    int SAMSUNG_PAY = 13595;
    int LOCAL_PAYMENT = 13596;
    int SEPA = 13597;
}
