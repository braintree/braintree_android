package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.google.android.gms.wallet.PaymentData;

/**
 * Result returned from the callback used to instantiate {@link GooglePayLauncher} that should be
 * passed to {@link GooglePayClient#tokenize(GooglePayResult, GooglePayOnActivityResultCallback)}
 */
public class GooglePayResult {

    private final PaymentData paymentData;
    private final Exception error;

    GooglePayResult(@Nullable PaymentData paymentData, @Nullable Exception error) {
        this.paymentData = paymentData;
        this.error = error;
    }

    PaymentData getPaymentData() {
        return paymentData;
    }

    Exception getError() {
        return error;
    }
}
