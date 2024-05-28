package com.braintreepayments.api.paypal;

import androidx.annotation.Nullable;

interface PayPalInternalTokenizeCallback {

    void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error);
}
