package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Used to receive the result of {@link ThreeDSecureClient#tokenize(ThreeDSecurePaymentAuthResult, ThreeDSecureTokenizeCallback)}
 */
public interface ThreeDSecureTokenizeCallback {

    void onThreeDSecureResult(@NonNull ThreeDSecureResult threeDSecureResult);
}
