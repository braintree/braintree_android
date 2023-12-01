package com.braintreepayments.demo;

import androidx.annotation.NonNull;

import com.braintreepayments.api.Authorization;
import com.braintreepayments.api.BraintreeClient;

public interface BraintreeAuthorizationCallback {
    void onResult(@NonNull String authString);
}
