package com.braintreepayments.api;

import android.content.Context;

/**
 * Used to receive the result of {@link ThreeDSecureClient#createPaymentAuthRequest(Context, ThreeDSecureRequest, ThreeDSecurePaymentAuthRequestCallback)}  
 */
public interface ThreeDSecurePaymentAuthRequestCallback {

    void onThreeDSecurePaymentAuthRequest(ThreeDSecurePaymentAuthRequest paymentAuthRequest);
}
