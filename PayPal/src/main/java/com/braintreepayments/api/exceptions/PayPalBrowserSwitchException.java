package com.braintreepayments.api.exceptions;

/**
 * Exception for whenever the browser has returned an 'error' in its response.
 */
public class PayPalBrowserSwitchException extends Exception {

    public PayPalBrowserSwitchException(String detailMessage) {
        super(detailMessage);
    }
}
