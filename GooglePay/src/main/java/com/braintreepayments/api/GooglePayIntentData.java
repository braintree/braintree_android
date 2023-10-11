package com.braintreepayments.api;

import androidx.annotation.NonNull;

import com.google.android.gms.wallet.PaymentDataRequest;

public class GooglePayIntentData {

    private final int googlePayEnvironment;
    private final PaymentDataRequest paymentDataRequest;

    GooglePayIntentData(int googlePayEnvironment, @NonNull PaymentDataRequest paymentDataRequest) {
        this.googlePayEnvironment = googlePayEnvironment;
        this.paymentDataRequest = paymentDataRequest;
    }

    int getGooglePayEnvironment() {
        return googlePayEnvironment;
    }

    PaymentDataRequest getPaymentDataRequest() {
        return paymentDataRequest;
    }
}
