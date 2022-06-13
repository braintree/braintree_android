package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface PayPalNativeCheckoutInternalClientCallback {
    void onResult(@Nullable PayPalNativeCheckoutResponse payPalResponse, @Nullable Exception error);
}
