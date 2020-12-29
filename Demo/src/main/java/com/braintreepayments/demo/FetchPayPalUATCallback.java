package com.braintreepayments.demo;

import androidx.annotation.Nullable;

public interface FetchPayPalUATCallback {
    void onResult(@Nullable String payPalUAT, @Nullable Exception error);
}
