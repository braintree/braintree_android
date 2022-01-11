package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface BraintreeAuthCallback {
    void onAuthResult(@Nullable String authString, @Nullable Exception error);
}
