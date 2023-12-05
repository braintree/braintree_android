package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#isReadyToPay(FragmentActivity, GooglePayIsReadyToPayCallback)} and
 * {@link GooglePayClient#isReadyToPay(FragmentActivity, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)}.
 */
public interface GooglePayIsReadyToPayCallback {

    /**
     * @param googlePayReadinessResult
     */
    void onGooglePayReadinessResult(GooglePayReadinessResult googlePayReadinessResult);
}
