package com.braintreepayments.api;

/**
 * Exception for whenever the browser has returned an 'error' in its response.
 */
public class PayPalBrowserSwitchException extends Exception {

    PayPalBrowserSwitchException(String detailMessage) {
        super(detailMessage);
    }
}
