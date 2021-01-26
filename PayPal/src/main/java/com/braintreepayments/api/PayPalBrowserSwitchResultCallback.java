package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface PayPalBrowserSwitchResultCallback {
    void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error);
}
