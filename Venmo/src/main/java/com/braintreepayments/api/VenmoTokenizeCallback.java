package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Used to receive the result of {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoTokenizeCallback)}
 */
public interface VenmoTokenizeCallback {

    void onVenmoResult(@NonNull VenmoResult result);
}
