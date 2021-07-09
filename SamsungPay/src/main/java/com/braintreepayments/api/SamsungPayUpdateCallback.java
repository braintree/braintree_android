package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link SamsungPayClient#updateSamsungPay(SamsungPayUpdateCallback)}.
 */
public interface SamsungPayUpdateCallback {

    /**
     * @param error an exception that occurred while navigating to Samsung Pay update page.
     */
    void onResult(@Nullable Exception error);
}
