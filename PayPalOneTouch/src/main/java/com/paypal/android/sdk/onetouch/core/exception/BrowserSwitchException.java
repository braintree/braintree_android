package com.paypal.android.sdk.onetouch.core.exception;

/**
 * Exception for whenever the browser has returned an 'error' in its response.
 */
public class BrowserSwitchException extends Exception {

    public BrowserSwitchException(String detailMessage) {
        super(detailMessage);
    }

    public BrowserSwitchException(Throwable throwable) {
        super(throwable);
    }
}
