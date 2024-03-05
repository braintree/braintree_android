package com.braintreepayments.api

import com.google.android.gms.wallet.PaymentMethodTokenizationParameters

/**
 * Callback for receiving result of [GooglePayClient.getTokenizationParameters]
 */
fun interface GooglePayGetTokenizationParametersCallback {
    /**
     * Called when tokenization parameters for Google Pay are available. Useful for existing Google
     * Wallet or Google Pay integrations, or when full control over the
     * [com.google.android.gms.wallet.MaskedWalletRequest] and
     * [com.google.android.gms.wallet.FullWalletRequest] is required.
     *
     *
     * [PaymentMethodTokenizationParameters] should be supplied to the
     * [com.google.android.gms.wallet.MaskedWalletRequest] via
     * [com.google.android.gms.wallet.MaskedWalletRequest.Builder.setPaymentMethodTokenizationParameters]
     * and allowedCardNetworks should be supplied to the
     * [com.google.android.gms.wallet.MaskedWalletRequest]
     * [com.google.android.gms.wallet.MaskedWalletRequest.Builder.addAllowedCardNetworks].
     *
     * @param tokenizationParameters [GooglePayTokenizationParameters]
     */
    fun onTokenizationParametersResult(tokenizationParameters: GooglePayTokenizationParameters?)
}