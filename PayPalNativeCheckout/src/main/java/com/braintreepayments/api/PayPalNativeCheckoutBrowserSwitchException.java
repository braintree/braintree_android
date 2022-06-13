package com.braintreepayments.api;

/**
 * Exception for whenever the browser has returned an 'error' in its response.
 */
public class PayPalNativeCheckoutBrowserSwitchException extends Exception {

    PayPalNativeCheckoutBrowserSwitchException(String detailMessage) {
        super(detailMessage);
    }
}
