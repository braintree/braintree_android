package com.braintreepayments.api;

import androidx.annotation.NonNull;

public class PayPalNativeCheckoutRequest {

    private final String amount;

    PayPalNativeCheckoutRequest(@NonNull String amount) {
        this.amount = amount;
    }

    @NonNull
    public String getAmount() {
        return amount;
    }
}
