package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.google.android.gms.wallet.PaymentData;

class GooglePayResult {

    private final PaymentData paymentData;
    private final Exception error;

    GooglePayResult(@Nullable PaymentData paymentData, @Nullable Exception error) {
        this.paymentData = paymentData;
        this.error = error;
    }

    public PaymentData getPaymentData() {
        return paymentData;
    }

    public Exception getError() {
        return error;
    }
}
