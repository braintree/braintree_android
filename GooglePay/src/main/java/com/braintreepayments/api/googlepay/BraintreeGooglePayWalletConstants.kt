package com.braintreepayments.api.googlepay

import com.google.android.gms.wallet.WalletConstants

/**
 * Collection of constant values used by the Braintree SDK Google Payment module. Extends upon
 * com.google.android.gms.wallet.WalletConstants.
 */
internal object BraintreeGooglePayWalletConstants {
    /**
     * Card network Elo.
     */
    const val CARD_NETWORK_ELO: Int = WalletConstants.CARD_NETWORK_OTHER + 1
}
