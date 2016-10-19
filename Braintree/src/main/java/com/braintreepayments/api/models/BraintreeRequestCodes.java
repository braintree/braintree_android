package com.braintreepayments.api.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        BraintreeRequestCodes.VISA_CHECKOUT
})
public @interface BraintreeRequestCodes {
    int VISA_CHECKOUT = 13592;
}
