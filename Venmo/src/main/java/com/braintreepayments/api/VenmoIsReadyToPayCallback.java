package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Callback for receiving result of
 * {@link VenmoClient#isReadyToPay(Context, VenmoIsReadyToPayCallback)}.
 */
public interface VenmoIsReadyToPayCallback {

    /**
     * @param venmoReadinessResult true if Venmo is ready; false otherwise.
     */
    void onVenmoResult(@NonNull VenmoReadinessResult venmoReadinessResult);
}
