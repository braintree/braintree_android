package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface PayPalInternalTokenizeCallback {

    void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error);
}
