package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoTokenizeCallback)}
 */
public interface VenmoPaymentAuthRequestCallback {
    void onVenmoPaymentAuthRequest(@NonNull VenmoPaymentAuthRequest venmoPaymentAuthRequest);

}
