package com.braintreepayments.api;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class IntegrationType {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            IntegrationType.CUSTOM,
            IntegrationType.DROP_IN
    })
    @interface Integration {}
    static final String CUSTOM = "custom";
    static final String DROP_IN = "dropin";
}
