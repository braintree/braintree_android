package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;

import java.util.Collection;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#getTokenizationParameters(GooglePayGetTokenizationParametersCallback)}
 */
public interface GooglePayGetTokenizationParametersCallback {

    /**
     * Called when tokenization parameters for Google Pay are available. Useful for existing Google
     * Wallet or Google Pay integrations, or when full control over the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} and
     * {@link com.google.android.gms.wallet.FullWalletRequest} is required.
     *
     * {@link PaymentMethodTokenizationParameters} should be supplied to the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#setPaymentMethodTokenizationParameters(PaymentMethodTokenizationParameters)}
     * and {@link Collection<Integer>} allowedCardNetworks should be supplied to the
     * {@link com.google.android.gms.wallet.MaskedWalletRequest} via
     * {@link com.google.android.gms.wallet.MaskedWalletRequest.Builder#addAllowedCardNetworks(Collection)}.
     *
     * @param parameters {@link PaymentMethodTokenizationParameters}
     * @param allowedCardNetworks {@link Collection<Integer>} of card networks supported by the current
     *        Braintree merchant account.
     */
    void onResult(@Nullable PaymentMethodTokenizationParameters parameters, @Nullable Collection<Integer> allowedCardNetworks);
}