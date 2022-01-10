package com.braintreepayments.api;

import com.google.android.gms.wallet.PaymentDataRequest;

public class GooglePayContractInput {

    private final int environment;
    private final PaymentDataRequest paymentDataRequest;

    public GooglePayContractInput(int environment, PaymentDataRequest paymentDataRequest) {
        this.environment = environment;
        this.paymentDataRequest = paymentDataRequest;
    }

    public int getEnvironment() {
        return environment;
    }

    public PaymentDataRequest getPaymentDataRequest() {
        return paymentDataRequest;
    }
}
