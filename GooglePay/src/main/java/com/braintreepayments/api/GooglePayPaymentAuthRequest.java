package com.braintreepayments.api;

import androidx.annotation.NonNull;

import com.google.android.gms.wallet.PaymentDataRequest;

/**
 * Used to request Google Pay payment authorization via
 * {@link GooglePayLauncher#launch(GooglePayPaymentAuthRequest)}
 */
public class GooglePayPaymentAuthRequest {

    private final int googlePayEnvironment;
    private final PaymentDataRequest paymentDataRequest;

    GooglePayPaymentAuthRequest(int googlePayEnvironment, @NonNull PaymentDataRequest paymentDataRequest) {
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