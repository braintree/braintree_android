package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface PayPalInternalClientCallback {
    void onResult(@Nullable PayPalPaymentAuthRequest payPalPaymentAuthRequest, @Nullable Exception error);
}
