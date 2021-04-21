package com.braintreepayments.api;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Enum representing different types of supported Braintree payment methods.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({
        VenmoPaymentMethodUsage.SINGLE_USE,
        VenmoPaymentMethodUsage.MULTI_USE
})
public @interface VenmoPaymentMethodUsage {
    String SINGLE_USE = "SINGLE_USE";
    String MULTI_USE = "MULTI_USE";
}

