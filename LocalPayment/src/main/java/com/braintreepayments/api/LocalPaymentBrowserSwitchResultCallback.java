package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface LocalPaymentBrowserSwitchResultCallback {
    void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error);
}
