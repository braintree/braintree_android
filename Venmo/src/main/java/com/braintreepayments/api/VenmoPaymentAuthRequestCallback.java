package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoTokenizeCallback)}
 */
public interface VenmoPaymentAuthRequestCallback {
    void onPaymentAuthRequest(@Nullable VenmoPaymentAuthRequest venmoPaymentAuthRequest,
                              @Nullable Exception error);

}
