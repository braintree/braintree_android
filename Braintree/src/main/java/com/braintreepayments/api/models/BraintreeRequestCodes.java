package com.braintreepayments.api.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        BraintreeRequestCodes.VISA_CHECKOUT
})
/**
 * Request codes that Braintree uses when communicating between Activities, and Fragments.
 */
public @interface BraintreeRequestCodes {
    int VISA_CHECKOUT = 13592;
}
