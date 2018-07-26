package com.braintreepayments.api.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Request codes that Braintree uses when communicating between {@link android.app.Activity}s, and
 * {@link android.app.Fragment}s.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        BraintreeRequestCodes.THREE_D_SECURE,
        BraintreeRequestCodes.VENMO,
        BraintreeRequestCodes.ANDROID_PAY,
        BraintreeRequestCodes.PAYPAL,
        BraintreeRequestCodes.VISA_CHECKOUT,
        BraintreeRequestCodes.GOOGLE_PAYMENT,
        BraintreeRequestCodes.IDEAL,
        BraintreeRequestCodes.SAMSUNG_PAY
})
public @interface BraintreeRequestCodes {
    int THREE_D_SECURE = 13487;
    int VENMO = 13488;
    int ANDROID_PAY = 13489;
    int PAYPAL = 13591;
    int VISA_CHECKOUT = 13592;
    int GOOGLE_PAYMENT = 13593;
    int IDEAL = 13594;
    int SAMSUNG_PAY = 13595;
}
