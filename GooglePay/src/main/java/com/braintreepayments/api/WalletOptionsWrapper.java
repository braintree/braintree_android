package com.braintreepayments.api;

import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

class WalletOptionsWrapper {

    Wallet.WalletOptions buildWalletOptions(Configuration configuration) {
        return new Wallet.WalletOptions.Builder()
                .setEnvironment(getGooglePayEnvironment(configuration))
                .build();
    }

   private int getGooglePayEnvironment(Configuration configuration) {
        if ("production".equals(configuration.getGooglePayEnvironment())) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }
}
