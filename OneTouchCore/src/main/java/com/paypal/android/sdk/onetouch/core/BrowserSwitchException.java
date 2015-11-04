package com.paypal.android.sdk.onetouch.core;

/**
 * Exception for whenever the Wallet app has returned an 'error' in its response.
 */
class BrowserSwitchException extends Exception {
    public BrowserSwitchException(String detailMessage) {
        super(detailMessage);
    }

    public BrowserSwitchException(Throwable throwable) {
        super(throwable);
    }
}
