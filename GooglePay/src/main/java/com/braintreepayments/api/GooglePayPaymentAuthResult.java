package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.google.android.gms.wallet.PaymentData;

/**
 * Result returned from the callback used to instantiate {@link GooglePayLauncher} that should be
 * passed to {@link GooglePayClient#tokenize(GooglePayPaymentAuthResult, GooglePayTokenizeCallback)}
 */
public class GooglePayPaymentAuthResult {

    private final PaymentData paymentData;
    private final Exception error;

    GooglePayPaymentAuthResult(@Nullable PaymentData paymentData, @Nullable Exception error) {
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
