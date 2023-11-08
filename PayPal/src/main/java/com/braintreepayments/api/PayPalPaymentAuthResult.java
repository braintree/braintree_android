package com.braintreepayments.api;

/**
 * Result received from the PayPal web flow through {@link PayPalTokenizeCallback}.
 * This result should be passed to
 * {@link PayPalClient#tokenize(PayPalPaymentAuthResult, PayPalTokenizeCallback)}
 * to complete the PayPal payment flow.
 */
public class PayPalPaymentAuthResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    PayPalPaymentAuthResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    PayPalPaymentAuthResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }
}
