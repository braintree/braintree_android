package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface PayPalListener {
    void onPayPalTokenizeError(@NonNull Exception error);
    void onPayPalTokenizeSuccess(@NonNull PayPalAccountNonce payPalAccountNonce);
}
