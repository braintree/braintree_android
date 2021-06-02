package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#isReadyToPay(FragmentActivity, GooglePayIsReadyToPayCallback)} and
 * {@link GooglePayClient#isReadyToPay(FragmentActivity, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)}.
 */
public interface GooglePayIsReadyToPayCallback {

    /**
     * @param isReadyToPay true if Google Pay is ready; false otherwise.
     * @param error an exception that occurred while checking if Google Pay is ready
     */
    void onResult(boolean isReadyToPay, Exception error);
}
