package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link GooglePayClient#requestPayment(GooglePayRequest, GooglePayIntentDataCallback)}
 */
public interface GooglePayIntentDataCallback {

    void onGooglePayIntentData(@Nullable GooglePayIntentData googlePayIntentData,
                               @Nullable Exception error);
}
