package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface PayPalInternalClientCallback {
    void onResult(@Nullable PayPalPaymentAuthRequestParams paymentAuthRequest, @Nullable Exception error);
}
