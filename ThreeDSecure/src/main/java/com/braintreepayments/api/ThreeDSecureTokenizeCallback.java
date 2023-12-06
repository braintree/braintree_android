package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface ThreeDSecureTokenizeCallback {

    void onThreeDSecureResult(@NonNull ThreeDSecureResult threeDSecureResult);
}
