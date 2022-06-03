package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface PayPalInternalClientCallback {
    void onResult(@Nullable PayPalResponse payPalResponse, @Nullable Exception error);
}
