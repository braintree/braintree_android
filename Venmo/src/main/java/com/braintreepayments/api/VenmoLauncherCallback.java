package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Used to receive notification that the Venmo payment authorization flow completed
 * Once this is invoked, continue the flow by calling
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResultInfo, VenmoTokenizeCallback)}
 */
public interface VenmoLauncherCallback {

    void onVenmoPaymentAuthResult(@NonNull VenmoPaymentAuthResultInfo venmoPaymentAuthResultInfo);
}
