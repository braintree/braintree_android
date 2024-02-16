package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResultInfo, VenmoTokenizeCallback)}
 */
public interface VenmoPaymentAuthRequestCallback {
    void onVenmoPaymentAuthRequest(@NonNull VenmoPaymentAuthRequest venmoPaymentAuthRequest);

}
