package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface PayPalNativeCheckoutListener {
    void onPayPalSuccess(PayPalNativeCheckoutNonce nonce);
    void onPayPalFailure(@NonNull Exception error);
}
