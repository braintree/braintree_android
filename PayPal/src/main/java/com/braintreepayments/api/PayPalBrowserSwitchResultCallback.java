package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.PayPalAccountNonce;

public interface PayPalBrowserSwitchResultCallback {
    void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error);
}
