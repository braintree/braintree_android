package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link VenmoClient#isReadyToPay(Context, VenmoIsReadyToPayCallback)}.
 */
public interface VenmoIsReadyToPayCallback {

    /**
     * @param isReadyToPay true if Venmo is ready; false otherwise.
     * @param error an exception that occurred while checking if Venmo is ready
     */
    void onResult(boolean isReadyToPay, @Nullable Exception error);
}
