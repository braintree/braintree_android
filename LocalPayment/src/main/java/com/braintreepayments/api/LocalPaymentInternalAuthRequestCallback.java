package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest, LocalPaymentInternalAuthRequestCallback)}.
 */
public interface LocalPaymentInternalAuthRequestCallback {

    /**
     * @param localPaymentAuthRequestParams {@link LocalPaymentAuthRequestParams}
     * @param error an exception that occurred while initiating a Local Payment
     */
    void onResult(@Nullable LocalPaymentAuthRequestParams localPaymentAuthRequestParams, @Nullable Exception error);
}
