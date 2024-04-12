package com.braintreepayments.demo;

import androidx.annotation.NonNull;

public interface BraintreeAuthorizationCallback {
    void onResult(@NonNull String authString);
}
