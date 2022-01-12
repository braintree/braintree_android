package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface GooglePayListener {
    void onGooglePayTokenizeSuccess(@NonNull PaymentMethodNonce paymentMethodNonce);
    void onGooglePayTokenizeError(@NonNull Exception error);
}
