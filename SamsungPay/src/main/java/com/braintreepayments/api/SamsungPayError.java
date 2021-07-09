package com.braintreepayments.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Collection of Samsung Pay error constants that may be emitted by {@link SamsungPayClient} callbacks.
 */
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

    /**
     * Samsung Pay cannot handle the payment currently.
     */
    int SAMSUNG_PAY_NOT_READY = 0;

    /**
     * Samsung Pay app needs to be updated. See {@link SamsungPayClient#updateSamsungPay(SamsungPayUpdateCallback)}.
     */
    int SAMSUNG_PAY_APP_NEEDS_UPDATE = 1;

    /**
     * Samsung Pay app setup not completed. See {@link SamsungPayClient#activateSamsungPay(SamsungPayActivateCallback)}.
     */
    int SAMSUNG_PAY_SETUP_NOT_COMPLETED = 2;

    /**
     * Samsung Pay user has no supported cards in their wallet.
     */
    int SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET = 3;

    /**
     * Samsung Pay is not supported on the current device.
     */
    int SAMSUNG_PAY_NOT_SUPPORTED = 4;

    /**
     * An unknown Samsung Pay error has occurred.
     */
    int SAMSUNG_PAY_ERROR_UNKNOWN = 5;
}
