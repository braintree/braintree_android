package com.braintreepayments.api.paypal;

import androidx.annotation.Nullable;

interface PayPalInternalClientCallback {
    void onResult(@Nullable PayPalPaymentAuthRequestParams paymentAuthRequest, @Nullable Exception error);
}
