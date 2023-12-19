package com.braintreepayments.api;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#isReadyToPay(android.content.Context, GooglePayIsReadyToPayCallback)} and
 * {@link GooglePayClient#isReadyToPay(android.content.Context, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)}.
 */
public interface GooglePayIsReadyToPayCallback {

    /**
     * @param googlePayReadinessResult
     */
    void onGooglePayReadinessResult(GooglePayReadinessResult googlePayReadinessResult);
}
