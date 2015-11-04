package com.paypal.android.sdk.onetouch.core;

/**
 * Exception for whenever the Wallet app has returned an 'error' in its response.
 */
class WalletSwitchException extends Exception {
    public WalletSwitchException(String detailMessage) {
        super(detailMessage);
    }

    public WalletSwitchException(Throwable throwable) {
        super(throwable);
    }
}
