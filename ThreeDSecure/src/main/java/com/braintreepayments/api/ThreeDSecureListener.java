package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface ThreeDSecureListener {
    void onThreeDSecureVerificationFailure(@NonNull Exception error);
    void onThreeDSecureVerificationSuccess(@NonNull ThreeDSecureResult threeDSecureResult);
}
