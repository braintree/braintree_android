package com.braintreepayments.api;

/**
 * Used to receive notification that the Venmo payment authorization flow completed
 * Once this is invoked, continue the flow by calling
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoTokenizeCallback)}
 */
public interface VenmoLauncherCallback {

    void onVenmoResult(VenmoPaymentAuthResult venmoPaymentAuthResult);
}
