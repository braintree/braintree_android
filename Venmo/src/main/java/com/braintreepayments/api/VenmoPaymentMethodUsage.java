package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Usage type for the tokenized Venmo account.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        VenmoPaymentMethodUsage.UNSPECIFIED,
        VenmoPaymentMethodUsage.SINGLE_USE,
        VenmoPaymentMethodUsage.MULTI_USE
})
public @interface VenmoPaymentMethodUsage {
    int UNSPECIFIED = 0;
    int SINGLE_USE = 1;
    int MULTI_USE = 2;
}
