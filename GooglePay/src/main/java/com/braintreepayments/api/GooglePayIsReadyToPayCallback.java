package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#isReadyToPay(android.content.Context, GooglePayIsReadyToPayCallback)} and
 * {@link GooglePayClient#isReadyToPay(android.content.Context, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)}.
 */
public interface GooglePayIsReadyToPayCallback {

    /**
     * @param isReadyToPay true if Google Pay is ready; false otherwise.
     * @param error an exception that occurred while checking if Google Pay is ready
     */
    void onResult(boolean isReadyToPay, @Nullable Exception error);
}
