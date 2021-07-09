package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link SamsungPayClient#isReadyToPay(SamsungPayIsReadyToPayCallback)}.
 */
public interface SamsungPayIsReadyToPayCallback {

    /**
     *
     * @param isReadyToPay true if Samsung Pay is ready; false otherwise.
     * @param error an exception that occurred while checking if Samsung Pay is ready
     */
    void onResult(boolean isReadyToPay, @Nullable Exception error);
}
