package com.braintreepayments.api;

import com.google.android.gms.wallet.WalletConstants;

/**
 * Collection of constant values used by the Braintree SDK Google Payment module. Extends upon
 * com.google.android.gms.wallet.WalletConstants.
 */
class BraintreeWalletConstants {

    /**
     * Card network Elo.
     */
    public static final int CARD_NETWORK_ELO = WalletConstants.CARD_NETWORK_OTHER + 1;
}
