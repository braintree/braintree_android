package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#requestPayment(FragmentActivity, GooglePayRequest, GooglePayRequestPaymentCallback)}.
 */
public interface GooglePayRequestPaymentCallback {

    /**
     * @param error an exception that occurred while initiating the Google Pay flow
     */
    void onResult(@Nullable Exception error);
}
