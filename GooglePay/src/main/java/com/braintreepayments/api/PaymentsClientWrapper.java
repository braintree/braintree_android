package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;

class PaymentsClientWrapper {

    private final WalletOptionsWrapper walletOptionsWrapper;

    PaymentsClientWrapper() {
        this(new WalletOptionsWrapper());
    }

    PaymentsClientWrapper(WalletOptionsWrapper walletOptionsWrapper) {
        this.walletOptionsWrapper = walletOptionsWrapper;
    }

    PaymentsClient getPaymentsClient(FragmentActivity activity, Configuration configuration) {
        return Wallet.getPaymentsClient(activity, walletOptionsWrapper.buildWalletOptions(configuration));
    }
}
