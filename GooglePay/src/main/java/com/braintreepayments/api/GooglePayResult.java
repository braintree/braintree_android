package com.braintreepayments.api;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.PaymentData;

public class GooglePayResult {

    private final BraintreeException error;
    private final PaymentData paymentData;

    public GooglePayResult(PaymentData paymentData, BraintreeException error) {
        this.error = error;
        this.paymentData = paymentData;
    }

    public BraintreeException getError() {
        return error;
    }

    public PaymentData getPaymentData() {
        return paymentData;
    }
}
