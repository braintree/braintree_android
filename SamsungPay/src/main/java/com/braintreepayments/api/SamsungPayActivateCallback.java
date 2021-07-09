package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link SamsungPayClient#activateSamsungPay(SamsungPayActivateCallback)}.
 */
public interface SamsungPayActivateCallback {

    /**
     * @param error an exception that occurred while navigating to Samsung Pay activate page.
     */
    void onResult(@Nullable Exception error);
}
