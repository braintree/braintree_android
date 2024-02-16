package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Used to receive the result of {@link VenmoClient#tokenize(VenmoPaymentAuthResultInfo, VenmoTokenizeCallback)}
 */
public interface VenmoTokenizeCallback {

    void onVenmoResult(@NonNull VenmoResult result);
}
