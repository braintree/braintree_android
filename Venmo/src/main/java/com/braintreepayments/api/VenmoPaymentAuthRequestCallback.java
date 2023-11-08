package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoResultCallback)}
 */
public interface VenmoPaymentAuthRequestCallback {
    void onPaymentAuthRequest(@Nullable VenmoPaymentAuthRequest venmoPaymentAuthRequest,
                              @Nullable Exception error);

}
