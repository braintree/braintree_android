package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        SamsungPayError.SAMSUNG_PAY_NOT_READY,
        SamsungPayError.SAMSUNG_PAY_APP_NEEDS_UPDATE,
        SamsungPayError.SAMSUNG_PAY_SETUP_NOT_COMPLETED,
        SamsungPayError.SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET,
        SamsungPayError.SAMSUNG_PAY_NOT_SUPPORTED,
        SamsungPayError.SAMSUNG_PAY_ERROR_UNKNOWN
})
public @interface SamsungPayError {

    int SAMSUNG_PAY_NOT_READY = 0;
    int SAMSUNG_PAY_APP_NEEDS_UPDATE = 1;
    int SAMSUNG_PAY_SETUP_NOT_COMPLETED = 2;
    int SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET = 3;
    int SAMSUNG_PAY_NOT_SUPPORTED = 4;
    int SAMSUNG_PAY_ERROR_UNKNOWN = 5;
}
