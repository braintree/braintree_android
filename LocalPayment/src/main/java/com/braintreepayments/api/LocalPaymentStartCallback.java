package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface LocalPaymentStartCallback {
    void onResult(@Nullable LocalPaymentTransaction transaction, @Nullable Exception error);
}
