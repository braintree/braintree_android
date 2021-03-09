package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.SOURCE)
@IntDef({ThreeDSecureShippingMethod.UNSPECIFIED,
        ThreeDSecureShippingMethod.SAME_DAY,
        ThreeDSecureShippingMethod.EXPEDITED,
        ThreeDSecureShippingMethod.PRIORITY,
        ThreeDSecureShippingMethod.GROUND,
        ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY,
        ThreeDSecureShippingMethod.SHIP_TO_STORE})
public @interface ThreeDSecureShippingMethod {
    int UNSPECIFIED = -1;
    int SAME_DAY = 1;
    int EXPEDITED = 2;
    int PRIORITY = 3;
    int GROUND = 4;
    int ELECTRONIC_DELIVERY = 5;
    int SHIP_TO_STORE = 6;
}

